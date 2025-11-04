package com.chatting.ui.activitys

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.chatting.ui.model.Country
import com.chatting.ui.theme.MyComposeApplicationTheme
import com.chatting.ui.utils.CountryList

class CountrySelectionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            MyComposeApplicationTheme {
                val allCountries = remember { CountryList.countries.sortedBy { it.name } }
                CountrySelectionScreen(
                    countries = allCountries,
                    onCountrySelected = { country ->
                        val resultIntent = Intent().apply {
                            putExtra("country_emoji", country.emoji)
                            putExtra("country_name", country.name)
                            putExtra("country_code", country.code)
                        }
                        setResult(Activity.RESULT_OK, resultIntent)
                        finish()
                    },
                    onClose = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountrySelectionScreen(
    countries: List<Country>,
    onCountrySelected: (Country) -> Unit,
    onClose: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredCountries = remember(searchQuery, countries) {
        if (searchQuery.isBlank()) {
            countries
        } else {
            countries.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.code.contains(searchQuery)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Selecione um País", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Fechar")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            // Campo de busca agora faz parte do conteúdo principal, evitando sobreposição.
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(24.dp)),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF0F0F0),
                    unfocusedContainerColor = Color(0xFFF0F0F0),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                placeholder = { Text("Buscar por nome ou código", color = Color.Gray) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color.Gray)
                },
                singleLine = true
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Lógica de agrupamento simplificada, sem duplicação de código.
                val grouped = filteredCountries.groupBy { it.name.first().uppercaseChar() }
                grouped.keys.sorted().forEach { initial ->
                    item(key = "header_$initial") {
                        ListHeader(text = initial.toString())
                    }
                    items(grouped.getValue(initial), key = { "country_${it.code}_${it.name}" }) { country ->
                        CountryRowItem(country = country, onClick = { onCountrySelected(country) })
                    }
                }
            }
        }
    }
}


@Composable
fun ListHeader(text: String) {
    Text(
        text = text,
        style = TextStyle(
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        ),
        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
    )
}

@Composable
fun CountryRowItem(
    country: Country,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = country.emoji, fontSize = 28.sp)
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = country.name,
            modifier = Modifier.weight(1f),
            style = TextStyle(fontSize = 16.sp)
        )
        Text(
            text = country.code,
            style = TextStyle(fontSize = 16.sp, color = Color.Gray)
        )
    }
}