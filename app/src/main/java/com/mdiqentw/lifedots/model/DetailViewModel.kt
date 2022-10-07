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
package com.mdiqentw.lifedots.model

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mdiqentw.lifedots.db.Contract

/* the viewmodel for the details of a diary entry */
class DetailViewModel : ViewModel() {
    @JvmField
    val mNote: MutableLiveData<String> = MutableLiveData()
    val mDuration: MutableLiveData<String> = MutableLiveData()

    //    public MutableLiveData<String> mAvgDuration;
    //    public MutableLiveData<String> mStartOfLast;
    @JvmField
    val mTotalToday: MutableLiveData<String> = MutableLiveData()

    @JvmField
    val mTotalWeek: MutableLiveData<String> = MutableLiveData()

    @JvmField
    val mTotalMonth: MutableLiveData<String> = MutableLiveData()
    val mCurrentActivity: MutableLiveData<DiaryActivity?> = MutableLiveData()

    /* TODO: note and starttime from ActivityHelper to here, or even use directly the ContentProvider
     * register a listener to get updates directly from the ContentProvider */
    val mDiaryEntryId: MutableLiveData<Long> = MutableLiveData()

    fun note(): LiveData<String> {
        return mNote
    }

    fun duration(): LiveData<String> {
        return mDuration
    }

    fun currentActivity(): LiveData<DiaryActivity?> {
        return mCurrentActivity
    }

    // TODO: this is not fully correct until the entry is stored in the DB and the ID is updated...
    var currentDiaryUri: Uri?
        get() = if (mCurrentActivity.value == null) {
            null
        } else {
            // TODO: this is not fully correct until the entry is stored in the DB and the ID is updated...
            Uri.withAppendedPath(
                Contract.Diary.CONTENT_URI,
                (mDiaryEntryId.value!!).toString()
            )
        }
        set(currentDiaryUri) {
            if (currentDiaryUri != null) {
                mDiaryEntryId.value = currentDiaryUri.lastPathSegment!!.toLong()
            }
        }
}