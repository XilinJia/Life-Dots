/*
 * LifeDots
 *
 * Copyright (C) 2018 Raphael Mack http://www.raphael-mack.de
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
package com.mdiqentw.lifedots.model.conditions

import android.database.sqlite.SQLiteQueryBuilder
import android.util.Log
import com.mdiqentw.lifedots.db.Contract
import com.mdiqentw.lifedots.helpers.ActivityHelper
import com.mdiqentw.lifedots.helpers.ActivityHelper.DataChangedListener
import com.mdiqentw.lifedots.model.DiaryActivity
import com.mdiqentw.lifedots.ui.settings.SettingsActivity
import kotlin.math.max

/**
 * Model the likelihood of the activities based on its predecessors in the diary
 */
open class GlobalOccurrenceCondition(helper: ActivityHelper) : Condition(), DataChangedListener {
    init {
        helper.registerDataChangeListener(this)
    }

    protected open fun selectionString(): String? {
        return "D." + Contract.Diary._DELETED + " = 0 " +
                "AND D." + Contract.Diary.ACT_ID + " = A." + Contract.DiaryActivity._ID +
                " AND A. " + Contract.DiaryActivity._DELETED + " = 0 "
    }

    protected open fun setWeight() {
        weight = sharedPreferences.getString(SettingsActivity.KEY_PREF_COND_OCCURRENCE, "0")!!.toDouble()
    }

    override fun doEvaluation() {
        setWeight()
        val all = ActivityHelper.helper.getUnsortedActivities()
        val result = ArrayList<Likelihood>(all.size)
        if (weight > 0.000001) {
            val qBuilder = SQLiteQueryBuilder()
            val db = mOpenHelper.readableDatabase
            qBuilder.tables = Contract.Diary.TABLE_NAME + " D, " + Contract.DiaryActivity.TABLE_NAME + " A"
            val c = qBuilder.query(
                db, arrayOf("D." + Contract.Diary.ACT_ID, "COUNT(D." + Contract.Diary.ACT_ID + ")"),
                selectionString(),
                null,
                "D." + Contract.Diary.ACT_ID,
                null,
                null
            )
            c.moveToFirst()
            var total: Long = 0
            var max: Long = 0
            while (!c.isAfterLast) {
                val a = ActivityHelper.helper.activityWithId(c.getInt(0))
                if (a == null) {
                    Log.i("doEvaluation", String.format("ID: %d links to no activity %d", c.getInt(0), c.getInt(1)))
                } else {
                    total += c.getInt(1)
                    max = max(max, c.getInt(1).toLong())
                    result.add(Likelihood(a, c.getInt(1).toDouble()))
                }
                c.moveToNext()
            }
            c.close()
            for (l in result) {
                l.likelihood = l.likelihood / max * weight
            }
        }
        setResult(result)
    }

    /**
     * Called when the data has changed and no further specification is possible.
     * => everything needs to be refreshed!
     */
    override fun onActivityDataChanged() {}

    /**
     * Called when the data of one activity was changed.
     *
     * @param activity
     */
    override fun onActivityDataChanged(activity: DiaryActivity) {}

    /**
     * Called on addition of an activity.
     *
     * @param activity
     */
    override fun onActivityAdded(activity: DiaryActivity) {
        refresh()
    }

    /**
     * Called on removale of an activity.
     *
     * @param activity
     */
    override fun onActivityRemoved(activity: DiaryActivity) {
        refresh()
    }

    /**
     * Called on change of the current activity.
     */
    override fun onActivityChanged() {
        refresh()
    }
}