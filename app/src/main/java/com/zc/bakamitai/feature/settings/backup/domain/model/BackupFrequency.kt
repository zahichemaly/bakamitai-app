package com.zc.bakamitai.feature.settings.backup.domain.model

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

/**
 * How often automatic backups run. Saved by name: never rename a constant.
 *
 * @property interval the time between two automatic backups; `null` for [OFF].
 */
enum class BackupFrequency(val interval: Duration?) {
    OFF(interval = null),
    EVERY_6_HOURS(interval = 6.hours),
    EVERY_12_HOURS(interval = 12.hours),
    DAILY(interval = 1.days),
    WEEKLY(interval = 7.days),
}
