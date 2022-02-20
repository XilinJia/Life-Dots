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

package com.mdiqentw.lifedots.model.conditions;

import com.mdiqentw.lifedots.db.Contract;
import com.mdiqentw.lifedots.helpers.ActivityHelper;
import com.mdiqentw.lifedots.ui.settings.SettingsActivity;

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

/**
 * Model the likelihood of the activities based on its predecessors in the diary
 */

public class RecentOccurrenceCondition extends GlobalOccurrenceCondition {
    public static final long MS_Per_Day = 1000 * 60 * 60 * 24;

    public RecentOccurrenceCondition(ActivityHelper helper){
        super(helper);
        helper.registerDataChangeListener(this);
    }

    @Override
    protected String selectionString() {
        long threeMonthsAgo = System.currentTimeMillis() - 90 * MS_Per_Day;

        return  "D." + Contract.Diary._DELETED + " = 0 " +
                "AND D." + Contract.Diary.ACT_ID + " = A." + Contract.DiaryActivity._ID +
                " AND A. " + Contract.DiaryActivity._DELETED + " = 0 AND D." +
                Contract.Diary.START + " >= " + threeMonthsAgo;
    }

    @Override
    protected void setWeight() {
        weight = Double.parseDouble(sharedPreferences.getString(SettingsActivity.KEY_PREF_COND_RECENCY, "20"));
    }

}
