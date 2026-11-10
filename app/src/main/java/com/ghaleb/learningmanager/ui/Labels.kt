package com.ghaleb.learningmanager.ui

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/** Always shown on the subjects screen, even before their first session. Others appear once they have one. */
val KNOWN_SUBJECTS = listOf("ai", "math", "human-systems", "astrophysics", "electronics")

fun subjectName(slug: String): String = if (slug == "ai") "AI" else topicWords(slug)

/** "ohms-law-voltage-budget" -> "Ohms law voltage budget" */
fun topicWords(slug: String): String = slug.split('-').joinToString(" ").replaceFirstChar { it.uppercase() }

val shortDate: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM")

fun recency(last: LocalDate?, today: LocalDate): String {
    if (last == null) return "not started yet"
    val days = ChronoUnit.DAYS.between(last, today)
    return when {
        days <= 0 -> "today"
        days == 1L -> "yesterday"
        days < 7 -> "$days days ago"
        days < 14 -> "last week"
        days < 60 -> "${days / 7} weeks ago"
        else -> "a while ago"
    }
}
