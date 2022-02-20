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

package com.mdiqentw.lifedots.model;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import android.net.Uri;

import com.mdiqentw.lifedots.db.Contract;

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

/* the viewmodel for the details of a diary entry */
public class DetailViewModel extends ViewModel {
    public final MutableLiveData<String> mNote;
    public final MutableLiveData<String> mDuration;
//    public MutableLiveData<String> mAvgDuration;
//    public MutableLiveData<String> mStartOfLast;
    public final MutableLiveData<String> mTotalToday;
    public final MutableLiveData<String> mTotalWeek;
    public final MutableLiveData<String> mTotalMonth;

    public final MutableLiveData<DiaryActivity> mCurrentActivity;
    /* TODO: note and starttime from ActivityHelper to here, or even use directly the ContentProvider
     * register a listener to get updates directly from the ContentProvider */

    public final MutableLiveData<Long> mDiaryEntryId;

    public DetailViewModel()
    {
        mNote = new MutableLiveData<>();
        mDuration = new MutableLiveData<>();
//        mAvgDuration = new MutableLiveData<>();
//        mStartOfLast = new MutableLiveData<>();
        mTotalToday = new MutableLiveData<>();
        mTotalWeek = new MutableLiveData<>();
        mTotalMonth = new MutableLiveData<>();
        mCurrentActivity = new MutableLiveData<>();
        mDiaryEntryId = new MutableLiveData<>();
    }

    public LiveData<String> note() {
        return mNote;
    }

    public LiveData<String> duration() {
        return mDuration;
    }

    public LiveData<DiaryActivity> currentActivity() {
        return mCurrentActivity;
    }

    @Nullable
    public Uri getCurrentDiaryUri(){
        if(mCurrentActivity.getValue() == null){
            return null;
        } else {
            // TODO: this is not fully correct until the entry is stored in the DB and the ID is updated...
            return Uri.withAppendedPath(Contract.Diary.CONTENT_URI,
                    Long.toString(mDiaryEntryId.getValue()));
        }
    }

    public void setCurrentDiaryUri(Uri currentDiaryUri) {
        if(currentDiaryUri != null) {
            mDiaryEntryId.setValue(Long.parseLong(currentDiaryUri.getLastPathSegment()));
        }
    }
}
