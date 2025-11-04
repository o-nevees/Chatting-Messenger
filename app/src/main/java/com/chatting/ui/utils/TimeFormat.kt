package com.chatting.ui.utils

import android.text.format.DateUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object TimeFormat {

    @JvmStatic
    fun getFormattedTimestamp(timestamp: Long?): String {
        if (timestamp == null || timestamp == 0L) {
            return ""
        }

        val messageDate = Date(timestamp)
        val todayCalendar = Calendar.getInstance()
        val messageCalendar = Calendar.getInstance()
        messageCalendar.time = messageDate

        val today = todayCalendar.get(Calendar.DAY_OF_YEAR)
        val yearToday = todayCalendar.get(Calendar.YEAR)
        val messageDay = messageCalendar.get(Calendar.DAY_OF_YEAR)
        val messageYear = messageCalendar.get(Calendar.YEAR)

        return when {
            today == messageDay && yearToday == messageYear -> {
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(messageDate)
            }
            today - 1 == messageDay && yearToday == messageYear -> {
                "Ontem"
            }
            else -> {
                SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(messageDate)
            }
        }
    }

    /**
     * Formata um timestamp para uma string legível para cabeçalhos de data no chat.
     * Retorna "Hoje", "Ontem" ou a data completa (ex: "25 de setembro de 2025").
     */
    @JvmStatic
    fun getFormattedDateHeader(timestamp: Long?): String {
        if (timestamp == null) return "Data desconhecida"

        return when {
            DateUtils.isToday(timestamp) -> "Hoje"
            DateUtils.isToday(timestamp + DateUtils.DAY_IN_MILLIS) -> "Ontem"
            else -> SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale.getDefault()).format(Date(timestamp))
        }
    }
}