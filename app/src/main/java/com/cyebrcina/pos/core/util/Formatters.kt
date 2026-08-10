package com.cyebrcina.pos.core.util

import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// Fire Hut Pizza & Wraps is a UK business — GBP, not USD.
private val currencyFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale.UK)
private val dateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy · h:mm a").withZone(ZoneId.systemDefault())
private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a").withZone(ZoneId.systemDefault())

fun Double.asCurrency(): String = currencyFormat.format(this)

fun Instant.asDateTime(): String = dateTimeFormatter.format(this)

fun Instant.asTime(): String = timeFormatter.format(this)

/** Parses the ISO-8601 timestamps the Fire Hut API returns (Prisma/JS `Date.toISOString()`). */
fun String.toInstantOrNull(): Instant? = runCatching { Instant.parse(this) }.getOrNull()
