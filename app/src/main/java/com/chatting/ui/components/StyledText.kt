package com.chatting.ui.components

import android.widget.Toast
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import com.chatting.ui.theme.PrimaryLight
import java.util.regex.Pattern

enum class TextContentType {
    PLAIN,
    LAST_MESSAGE_PREVIEW,
    CHAT_BUBBLE_MESSAGE
}

@Composable
fun StyledText(
    text: String,
    modifier: Modifier = Modifier,
    contentType: TextContentType = TextContentType.PLAIN,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    textAlign: TextAlign? = null
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val clipboardManager = LocalClipboardManager.current

    val linkStyle = SpanStyle(
        color = PrimaryLight,
        textDecoration = TextDecoration.Underline
    )
    val hashtagStyle = SpanStyle(
        color = MaterialTheme.colorScheme.secondary,
        fontStyle = FontStyle.Italic
    )
    val mentionStyle = SpanStyle(
        color = MaterialTheme.colorScheme.tertiary,
        fontWeight = FontWeight.Bold
    )
    val monospaceStyle = SpanStyle(
        fontFamily = FontFamily.Monospace,
        background = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    )

    val annotatedString = remember(text, contentType) {
        buildAnnotatedString {
            if (contentType == TextContentType.PLAIN) {
                append(text)
                return@buildAnnotatedString
            }

            val isClickable = contentType == TextContentType.CHAT_BUBBLE_MESSAGE

            val codeBlockPattern = Pattern.compile("```(.*?)```", Pattern.DOTALL)
            val inlineCodePattern = Pattern.compile("`(.*?)`")

            val formattingPatterns = mapOf(
                Pattern.compile("(https?://\\S+|www\\.\\S+)") to "URL",
                Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}") to "EMAIL",
                // Regex para números de telefone (simplificado para cobrir formatos comuns)
                Pattern.compile("\\+?\\d[\\d\\s-]{8,}\\d") to "PHONE",
                Pattern.compile("@[a-zA-Z0-9_-]+") to "MENTION",
                Pattern.compile("#[a-zA-Z0-9_-]+") to "HASHTAG",
                Pattern.compile("\\*(.*?)\\*") to "BOLD",
                Pattern.compile("_(.*?)_") to "ITALIC",
                Pattern.compile("~(.*?)~") to "STRIKETHROUGH"
            )

            val matches = mutableListOf<Triple<Int, Int, String>>()
            
            var matcher = codeBlockPattern.matcher(text)
            while (matcher.find()) {
                matches.add(Triple(matcher.start(), matcher.end(), "CODE_BLOCK"))
            }

            matcher = inlineCodePattern.matcher(text)
            while (matcher.find()) {
                val isInsideCodeBlock = matches.any { it.third == "CODE_BLOCK" && matcher.start() >= it.first && matcher.end() <= it.second }
                if (!isInsideCodeBlock) {
                    matches.add(Triple(matcher.start(), matcher.end(), "INLINE_CODE"))
                }
            }

            formattingPatterns.forEach { (pattern, tag) ->
                matcher = pattern.matcher(text)
                while (matcher.find()) {
                    val isInsideAnyCode = matches.any { 
                        (it.third == "CODE_BLOCK" || it.third == "INLINE_CODE") && 
                        matcher.start() >= it.first && matcher.end() <= it.second 
                    }
                    if (!isInsideAnyCode) {
                        matches.add(Triple(matcher.start(), matcher.end(), tag))
                    }
                }
            }

            matches.sortBy { it.first }

            var lastIndex = 0
            matches.forEach { (start, end, tag) ->
                if (start > lastIndex) {
                    append(text.substring(lastIndex, start))
                }

                val content = text.substring(start, end)
                val strippedContent: String
                val style: SpanStyle?

                when (tag) {
                    "URL", "EMAIL", "PHONE" -> {
                        strippedContent = content
                        style = linkStyle
                        if (isClickable) pushStringAnnotation(tag = tag, annotation = content)
                    }
                    "MENTION" -> { strippedContent = content; style = mentionStyle }
                    "HASHTAG" -> { strippedContent = content; style = hashtagStyle }
                    "BOLD" -> { strippedContent = content.removeSurrounding("*"); style = SpanStyle(fontWeight = FontWeight.Bold) }
                    "ITALIC" -> { strippedContent = content.removeSurrounding("_"); style = SpanStyle(fontStyle = FontStyle.Italic) }
                    "STRIKETHROUGH" -> { strippedContent = content.removeSurrounding("~"); style = SpanStyle(textDecoration = TextDecoration.LineThrough) }
                    "INLINE_CODE", "CODE_BLOCK" -> {
                        strippedContent = if (tag == "INLINE_CODE") content.removeSurrounding("`") else content.removeSurrounding("```")
                        style = monospaceStyle
                        if (isClickable) pushStringAnnotation(tag = "COPYABLE", annotation = strippedContent)
                    }
                    else -> { strippedContent = content; style = null }
                }
                
                if (style != null) pushStyle(style)
                append(strippedContent)
                if (style != null) pop()
                if (isClickable && (tag == "URL" || tag == "EMAIL" || tag == "PHONE")) pop()

                lastIndex = end
            }

            if (lastIndex < text.length) {
                append(text.substring(lastIndex))
            }
        }
    }

    if (contentType == TextContentType.CHAT_BUBBLE_MESSAGE) {
        ClickableText(
            text = annotatedString,
            modifier = modifier,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = color,
                fontSize = fontSize,
                fontWeight = fontWeight,
                textAlign = textAlign ?: TextAlign.Unspecified
            ),
            maxLines = maxLines,
            overflow = overflow,
            onClick = { offset ->
                val clickedAnnotations = annotatedString.getStringAnnotations(start = offset, end = offset)

                // Prioriza copiar texto monoespaçado
                clickedAnnotations.firstOrNull { it.tag == "COPYABLE" }?.let { annotation ->
                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(annotation.item))
                    Toast.makeText(context, "Texto copiado!", Toast.LENGTH_SHORT).show()
                    return@ClickableText
                }

                // Se não for um texto copiável, verifica se é um link
                clickedAnnotations.firstOrNull()?.let { annotation ->
                    try {
                        var uri = annotation.item
                        when (annotation.tag) {
                            "EMAIL" -> if (!uri.startsWith("mailto:")) uri = "mailto:$uri"
                            "PHONE" -> if (!uri.startsWith("tel:")) uri = "tel:$uri"
                            "URL" -> if (!uri.startsWith("http://") && !uri.startsWith("https://")) uri = "http://$uri"
                        }
                        uriHandler.openUri(uri)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        )
    } else {
        Text(
            text = annotatedString,
            modifier = modifier,
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            maxLines = maxLines,
            overflow = overflow,
            textAlign = textAlign
        )
    }
}