package io.github.naharaoss.skpd.utils

import androidx.room.TypeConverter
import kotlin.time.Instant

class RoomTypeConverters {
    @TypeConverter
    fun fromTimestamp(value: Long?) = value?.let { Instant.fromEpochMilliseconds(it) }

    @TypeConverter
    fun toTimestamp(value: Instant?) = value?.toEpochMilliseconds()
}