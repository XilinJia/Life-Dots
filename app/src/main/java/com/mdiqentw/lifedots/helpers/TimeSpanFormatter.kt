/*
 * LifeDots
 *
 * Copyright (C) 2020 Xilin Jia https://github.com/XilinJia
  ~ Copyright (C) 2017-2018 Raphael Mack http://www.raphael-mack.de
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.mdiqentw.lifedots.helpers

import androidx.preference.PreferenceManager
import com.mdiqentw.lifedots.MVApplication
import com.mdiqentw.lifedots.R
import com.mdiqentw.lifedots.ui.settings.SettingsActivity
import java.text.DecimalFormat
import java.util.*

/*
 * LifeDots
 *
 * Copyright (C) 2020 Xilin Jia https://github.com/XilinJia
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
object TimeSpanFormatter {

    @JvmStatic
    fun fuzzyFormat(start: Date, end: Date): String {
        val res = MVApplication.getAppContext().resources
        val delta = (end.time - start.time + 500) / 1000

        return when {
            delta <= 3 -> {
                res.getString(R.string.just_now)
            }
            delta <= 35 -> {
                res.getString(R.string.few_seconds)
            }
            delta <= 90 -> {
                val `val` = ((delta + 8) / 15).toInt() * 15
                res.getQuantityString(R.plurals.seconds_short, `val`, `val`)
            }
            delta <= 90 * 60 -> {
                val `val` = ((delta + 30) / 60).toInt()
                res.getQuantityString(R.plurals.minutes_short, `val`, `val`)
            }
            delta <= 90 * 60 * 60 -> {
                val `val` = ((delta + 30 * 60) / 3600).toInt()
                res.getQuantityString(R.plurals.hours_short, `val`, `val`)
            }
            else -> {
                val `val` = ((delta + 12 * 60 * 60) / 3600 / 24).toInt()
                res.getQuantityString(R.plurals.days, `val`, `val`)
            }
        }
    }

    /**
     * duration in millis
     */
    @JvmStatic
    fun format(duration: Long): String {
        if (duration < 1E4) {
            return (duration / 100f).toString() + "%"
        }
        val res = MVApplication.getAppContext().resources
        val delta = duration / 1000
        val displayFormat = PreferenceManager
                .getDefaultSharedPreferences(MVApplication.getAppContext())
                .getString(SettingsActivity.KEY_PREF_DURATION_FORMAT, "dynamic")
        var result = ""
        val sec = (delta % 60).toInt()
        var min = ((delta - sec) / 60 % 60).toInt()
        var hours = ((delta - 60 * min - sec) / 60 / 60 % 24).toInt()
        var days = ((delta - 3600 * hours - 60 * min - sec) / 60 / 60 / 24).toInt()
        val df = DecimalFormat("00")
        when (displayFormat) {
            "hour_min" -> {
                if (days > 0) {
                    hours += days * 24
                }
                min += (sec + 30) / 60
                result = hours.toString() + "h " + df.format(min.toLong()) + "'"
            }
            "nodays" -> {
                if (days > 0) {
                    hours += days * 24
                    days = 0
                }
                if (days > 0) {
                    result = days.toString() + "d "
                }
                if (hours > 0 || days > 0) {
                    result += (hours.toString() + "h "
                            + df.format(min.toLong()) + "' ")
                }
                if (min > 0 && hours == 0 && days == 0) {
                    result += "$min' "
                }
                if (min > 0 || hours > 0 || days > 0) {
                    result += df.format(sec.toLong()) + "''"
                } else {
                    result = res.getQuantityString(R.plurals.seconds_short, sec, sec)
                }
            }
            "precise" -> {
                if (days > 0) {
                    result = days.toString() + "d "
                }
                if (hours > 0 || days > 0) {
                    result += (hours.toString() + "h "
                            + df.format(min.toLong()) + "' ")
                }
                if (min > 0 && hours == 0 && days == 0) {
                    result += "$min' "
                }
                if (min > 0 || hours > 0 || days > 0) {
                    result += df.format(sec.toLong()) + "''"
                } else {
                    result = res.getQuantityString(R.plurals.seconds_short, sec, sec)
                }
            }
            "dynamic" -> result = when {
                days >= 9 -> {
                    (days + (hours + 12) / 24).toString() + "d"
                }
                days >= 1 -> {
                    (days.toString() + "d "
                            + (hours + (min + 30) / 60) + "h")
                }
                hours >= 1 -> {
                    (hours.toString() + "h "
                            + df.format((min + (sec + 30) / 60).toLong()) + "'")
                }
                min >= 1 -> {
                    (min.toString() + "' "
                            + df.format(sec.toLong()) + "''")
                }
                else -> {
                    res.getQuantityString(R.plurals.seconds_short, sec, sec)
                }
            }
            else -> result = when {
                days >= 9 -> {
                    (days + (hours + 12) / 24).toString() + "d"
                }
                days >= 1 -> {
                    (days.toString() + "d "
                            + (hours + (min + 30) / 60) + "h")
                }
                hours >= 1 -> {
                    (hours.toString() + "h "
                            + df.format((min + (sec + 30) / 60).toLong()) + "'")
                }
                min >= 1 -> {
                    (min.toString() + "' "
                            + df.format(sec.toLong()) + "''")
                }
                else -> {
                    res.getQuantityString(R.plurals.seconds_short, sec, sec)
                }
            }
        }
        return result
    }
}