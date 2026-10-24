/*
 * Copyright (c) 2026. Seedim <hello@seelim.dev>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package dev.seelim.velocitywol.config

import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DownTimeEvaluatorTest {

    private val utcZone = ZoneId.of("UTC")

    private fun utcTime(hour: Int, minute: Int): ZonedDateTime {
        return ZonedDateTime.of(2026, 10, 24, hour, minute, 0, 0, utcZone)
    }

    @Test
    fun `returns false when downtime is disabled even during downtime hours`() {
        val config = DowntimeConfig(
            enabled = false,
            timezone = "UTC",
            startTime = "00:00",
            endTime = "08:00",
        )

        val duringHours = utcTime(4, 0)
        assertFalse(DowntimeEvaluator.isDowntime(config, duringHours))
    }

    @Test
    fun `evaluates same-day dowtime window correctly`() {
        val config = DowntimeConfig(
            enabled = true,
            timezone = "UTC",
            startTime = "02:00",
            endTime = "06:00",
        )

        assertFalse(DowntimeEvaluator.isDowntime(config, utcTime(1, 59)))
        assertTrue(DowntimeEvaluator.isDowntime(config, utcTime(2, 0)))
        assertTrue(DowntimeEvaluator.isDowntime(config, utcTime(4, 30)))
        assertFalse(DowntimeEvaluator.isDowntime(config, utcTime(6, 0)))
        assertFalse(DowntimeEvaluator.isDowntime(config, utcTime(7, 0)))
    }

    @Test
    fun `evaluates overnight downtime window spanning midnight correctly`() {
        val config = DowntimeConfig(
            enabled = true,
            timezone = "UTC",
            startTime = "22:00",
            endTime = "06:00",
        )

        assertFalse(DowntimeEvaluator.isDowntime(config, utcTime(21, 59)))
        assertTrue(DowntimeEvaluator.isDowntime(config, utcTime(22, 0)))
        assertTrue(DowntimeEvaluator.isDowntime(config, utcTime(23, 45)))
        assertTrue(DowntimeEvaluator.isDowntime(config, utcTime(0, 0)))
        assertTrue(DowntimeEvaluator.isDowntime(config, utcTime(5, 30)))
        assertFalse(DowntimeEvaluator.isDowntime(config, utcTime(6, 0)))
        assertFalse(DowntimeEvaluator.isDowntime(config, utcTime(14, 0)))
    }

    @Test
    fun `correctly converts timezones when evaluating schedule`() {
        val config = DowntimeConfig(
            enabled = true,
            timezone = "Europe/Berlin",
            startTime = "02:00",
            endTime = "06:00",
        )

        val berlinDowntime = ZonedDateTime.of(2026, 10, 24, 0, 30, 0, 0, ZoneId.of("UTC"))
        assertTrue(DowntimeEvaluator.isDowntime(config, berlinDowntime))

        val berlinDaytime = ZonedDateTime.of(2026, 10, 24, 12, 0, 0, 0, ZoneId.of("UTC"))
        assertFalse(DowntimeEvaluator.isDowntime(config, berlinDaytime))
    }

    @Test
    fun `handles invalid timezone gracefully by falling back to UTC without throwing`() {
        val config = DowntimeConfig(
            enabled = true,
            timezone = "Invalid/Timezone",
            startTime = "02:00",
            endTime = "06:00",
        )

        assertTrue(DowntimeEvaluator.isDowntime(config, utcTime(3, 0)))
        assertFalse(DowntimeEvaluator.isDowntime(config, utcTime(10, 0)))
    }
}