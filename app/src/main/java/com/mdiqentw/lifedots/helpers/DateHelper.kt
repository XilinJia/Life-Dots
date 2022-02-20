/*
 * LifeDots
 *
 * Copyright (C) 2018 Raphael Mack http://www.raphael-mack.de
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

import com.mdiqentw.lifedots.MVApplication
import com.mdiqentw.lifedots.R
import java.lang.RuntimeException
import java.text.SimpleDateFormat
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
object DateHelper {
    const val FULLDAY = 3324
    const val FULLWEEK = 3407
    const val FULLMONTH = 2530
    const val DAY_IN_MS = (1000 * 60 * 60 * 24).toLong()

    /* Get the start of the time span from timeRef
     * possible range values are the field constants of Calender, e. g. Calendar.MONTH
     *
     * Current supported granularity down to DAY, so MILLISECOND, SECOND, MINUTE and HOUR_OF_DAY are always set to 0
     *
     * timeRef is in millis since epoch
     * */
    fun startOf(field: Int, timeRef: Long): Calendar {
        val result = Calendar.getInstance()
        result.timeInMillis = timeRef
        result[Calendar.HOUR_OF_DAY] = 0 // ! clear would not reset the hour of day !
        result.clear(Calendar.MINUTE)
        result.clear(Calendar.SECOND)
        result.clear(Calendar.MILLISECOND)
        when (field) {
            Calendar.DAY_OF_MONTH, Calendar.DAY_OF_WEEK, Calendar.DAY_OF_WEEK_IN_MONTH, Calendar.DAY_OF_YEAR -> {}
            Calendar.WEEK_OF_YEAR -> result[Calendar.DAY_OF_WEEK] = result.firstDayOfWeek
            Calendar.MONTH -> result[Calendar.DAY_OF_MONTH] = 1
            Calendar.YEAR -> result[Calendar.DAY_OF_YEAR] = 1
            else -> throw RuntimeException("date field not supported: $field")
        }
        return result
    }

    @JvmStatic
    fun dateFormat(field: Int): SimpleDateFormat {
        val res = MVApplication.getAppContext().resources
        val result: SimpleDateFormat = when (field) {
            Calendar.DAY_OF_MONTH, Calendar.DAY_OF_WEEK, Calendar.DAY_OF_WEEK_IN_MONTH, Calendar.DAY_OF_YEAR -> SimpleDateFormat(res.getString(R.string.day_format))
            Calendar.WEEK_OF_YEAR -> SimpleDateFormat(res.getString(R.string.week_format))
            Calendar.MONTH -> SimpleDateFormat(res.getString(R.string.month_format))
            Calendar.YEAR -> SimpleDateFormat(res.getString(R.string.year_format))
            else -> throw RuntimeException("date field not supported: $field")
        }
        return result
    }
}