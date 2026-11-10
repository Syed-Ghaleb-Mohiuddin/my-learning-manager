package com.ghaleb.learningmanager.streak

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/** Every date in the app is a Dubai date, whatever timezone the phone is set to. */
object DubaiTime {
    val ZONE: ZoneId = ZoneId.of("Asia/Dubai")

    fun now(clock: Clock = Clock.systemUTC()): ZonedDateTime = ZonedDateTime.now(clock.withZone(ZONE))

    fun today(clock: Clock = Clock.systemUTC()): LocalDate = now(clock).toLocalDate()

    /** Emits the Dubai time now and then every minute, so open screens roll over at midnight and moods move. */
    fun ticker(): Flow<ZonedDateTime> = flow {
        while (true) {
            emit(now())
            delay(60_000)
        }
    }
}
