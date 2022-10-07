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

import com.mdiqentw.lifedots.helpers.ActivityHelper
import com.mdiqentw.lifedots.helpers.ActivityHelper.DataChangedListener
import com.mdiqentw.lifedots.model.DiaryActivity
import com.mdiqentw.lifedots.ui.settings.SettingsActivity
import java.util.*

/**
 * Model the likelihood of the activities based on the alphabetical sorting of their names
 */
class AlphabeticalCondition(helper: ActivityHelper) : Condition(), DataChangedListener {
    init {
        helper.registerDataChangeListener(this)
    }

    override fun doEvaluation() {
        weight = sharedPreferences.getString(SettingsActivity.KEY_PREF_COND_ALPHA, "5")!!.toDouble()
        val result = ArrayList<Likelihood>(ActivityHelper.helper.getUnsortedActivities().size)
        if (weight > 0.001) {
            val sort = ArrayList(ActivityHelper.helper.getUnsortedActivities())
            Collections.sort(sort) { o1: DiaryActivity?, o2: DiaryActivity? ->
                if (o1 === o2) {
                    return@sort 0
                } else if (o1 == null) {
                    return@sort -1
                } else if (o2 == null) {
                    return@sort 1
                } else {
                    return@sort o2.mName.compareTo(o1.mName)
                }
            }
            val step = weight / sort.size
            for ((no, a) in sort.withIndex()) {
                result.add(Likelihood(a!!, step * no))
            }
        }
        setResult(result)
    }

    /**
     * Called when the data has changed and no further specification is possible.
     * => everything needs to be refreshed!
     *
     */
    override fun onActivityDataChanged() {
        refresh()
    }

    /**
     * Called when the data of one activity was changed.
     *
     * @param activity
     */
    override fun onActivityDataChanged(activity: DiaryActivity) {
        refresh()
    }

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
        // no influence on likelhood for this Condition
    }
}