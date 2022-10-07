/*
 * LifeDots
 *
 * Copyright (C) 2017-2018 Raphael Mack http://www.raphael-mack.de
 * Copyright (C) 2018 Bc. Ondrej Janitor
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
package com.mdiqentw.lifedots.helpers

import android.annotation.SuppressLint
import android.content.AsyncQueryHandler
import android.content.ContentUris
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import androidx.preference.PreferenceManager
import com.mdiqentw.lifedots.MVApplication
import com.mdiqentw.lifedots.db.Contract
import com.mdiqentw.lifedots.db.LDContentProvider
import com.mdiqentw.lifedots.model.DiaryActivity
import com.mdiqentw.lifedots.model.conditions.AlphabeticalCondition
import com.mdiqentw.lifedots.model.conditions.Condition
import com.mdiqentw.lifedots.model.conditions.GlobalOccurrenceCondition
import com.mdiqentw.lifedots.model.conditions.RecentOccurrenceCondition
import com.mdiqentw.lifedots.ui.settings.SettingsActivity
import java.util.*
import kotlin.math.min

/**
 * provide a smooth interface to an OO abstraction of the data for our diary.
 */
class ActivityHelper private constructor() : AsyncQueryHandler(MVApplication.appContext!!.contentResolver) {
    /* list of all activities, not including deleted ones */
    internal var activities: MutableList<DiaryActivity>

    /* unsortedActivities is not allowed to be modified */
    private val unsortedActivities: MutableList<DiaryActivity>
    private var mCurrentActivity: DiaryActivity? = null
    val currentActivityStartTime: Date
    var currentDiaryUri: Uri? = null
        private set
    /* @NonNull */  var currentNote: String? = null
    private lateinit var conditions: Array<Condition>

    //    private DetailViewModel viewModel;
    private val mHandler: Handler = object : Handler(Looper.getMainLooper()) {
        /*
         * handleMessage() defines the operations to perform when
         * the Handler receives a new Message to process.
         */
        override fun handleMessage(inputMessage: Message) {
            // so far we only have one message here so no need to look at the details
            // just assume that at least one Condition evaluation is finished and we check
            // whether all are done
            var allDone = true
            for (c in conditions) {
                if (c.isActive) {
                    allDone = false
                    break
                }
            }
            if (allDone) {
                reorderActivites()
            }
        }
    }

    /* to be used only in the UI thread, consider getActivitiesCopy() */
    fun getActivities(): List<DiaryActivity> {
        return activities
    }

    /* get a list of the activities as non-modifable copy, not guaranteed to be up to date */
    fun getUnsortedActivities(): List<DiaryActivity> {
        val result: MutableList<DiaryActivity> = ArrayList(unsortedActivities.size)
        synchronized(this) {
            if (unsortedActivities.isEmpty()) {
                /* activities not yet loaded, so it doesn't make sense yet to read the activities */
                try {
                    Thread.sleep(50)
                } catch (e: InterruptedException) {
                    /* intended empty */
                }
            }
            result.addAll(unsortedActivities)
        }
        return result
    }

    interface DataChangedListener {
        /**
         * Called when the data has changed and no further specification is possible.
         * => everything needs to be refreshed!
         */
        fun onActivityDataChanged()

        /**
         * Called when the data of one activity was changed.
         */
        fun onActivityDataChanged(activity: DiaryActivity)

        /**
         * Called on addition of an activity.
         */
        fun onActivityAdded(activity: DiaryActivity)

        /**
         * Called on removal of an activity.
         */
        fun onActivityRemoved(activity: DiaryActivity)

        /**
         * Called on change of the current activity.
         */
        fun onActivityChanged()

        /**
         * Called on change of the activity order due to likelyhood.
         */
        fun onActivityOrderChanged()
    }

    private val mDataChangeListeners: MutableList<DataChangedListener>
    fun registerDataChangeListener(listener: DataChangedListener) {
        mDataChangeListeners.add(listener)
    }

    fun unregisterDataChangeListener(listener: DataChangedListener) {
        mDataChangeListeners.remove(listener)
    }

    /* reload all the activities from the database */
    fun reloadAll() {
        val resolver = MVApplication.appContext!!.contentResolver
        val client = resolver.acquireContentProviderClient(Contract.AUTHORITY)
        val provider = client!!.localContentProvider as LDContentProvider
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            client.close()
        } else {
            client.release()
        }
        provider.resetDatabase()
        startQuery(
            QUERY_ALL_ACTIVITIES, null, Contract.DiaryActivity.CONTENT_URI,
            ACTIVITIES_PROJ, SELECTION, null,
            null
        )
    }

    /* start the query to read the current activity
     * will trigger the update of currentActivity and send notifications afterwards */
    fun readCurrentActivity() {
        startQuery(
            QUERY_CURRENT_ACTIVITY, null, Contract.Diary.CONTENT_URI,
            DIARY_PROJ, Contract.Diary.START + " = (SELECT MAX("
                    + Contract.Diary.START + ") FROM "
                    + Contract.Diary.TABLE_NAME + " WHERE " + SELECTION + ")", null,
            Contract.Diary.START + " DESC"
        )
    }

    @SuppressLint("Range")
    override fun onQueryComplete(
        token: Int, cookie: Any?,
        cursor: Cursor
    ) {
        if (cursor.moveToFirst()) {
            when (token) {
                QUERY_ALL_ACTIVITIES -> {
                    synchronized(this) {
                        activities.clear()
                        unsortedActivities.clear()
                        while (!cursor.isAfterLast) {
                            val act = DiaryActivity(
                                cursor.getInt(cursor.getColumnIndex(Contract.DiaryActivity._ID)),
                                cursor.getString(cursor.getColumnIndex(Contract.DiaryActivity.NAME)),
                                cursor.getInt(cursor.getColumnIndex(Contract.DiaryActivity.COLOR))
                            )
                            /* TODO: optimize by keeping a map with id as key and the DiaryActivities */activities.add(
                                act
                            )
                            unsortedActivities.add(act)
                            cursor.moveToNext()
                        }
                    }
                    readCurrentActivity()
                    for (listener in mDataChangeListeners) {
                        listener.onActivityDataChanged()
                    }
                }

                QUERY_CURRENT_ACTIVITY -> {
                    if (!cursor.isNull(cursor.getColumnIndex(Contract.Diary.END))) {
                        /* no current activity */
                        currentNote = ""
                        currentDiaryUri = null
                        currentActivityStartTime.time = cursor.getLong(cursor.getColumnIndex(Contract.Diary.END))
                    } else {
                        mCurrentActivity =
                            activityWithId(cursor.getInt(cursor.getColumnIndex(Contract.Diary.ACT_ID)))
                        currentActivityStartTime.time = cursor.getLong(cursor.getColumnIndex(Contract.Diary.START))
                        currentNote = cursor.getString(cursor.getColumnIndex(Contract.Diary.NOTE))
                        currentDiaryUri = Uri.withAppendedPath(
                            Contract.Diary.CONTENT_URI,
                            cursor.getLong(cursor.getColumnIndex(Contract.Diary._ID)).toString()
                        )
                    }
                    //                showCurrentActivityNotification();
                    for (listener in mDataChangeListeners) {
                        listener.onActivityChanged()
                    }
                }

                UNDELETE_ACTIVITY -> {
                    if (cookie != null) {
                        val act = cookie as DiaryActivity
                        act.mColor = cursor.getInt(cursor.getColumnIndex(Contract.DiaryActivity.COLOR))
                        act.mName = cursor.getString(cursor.getColumnIndex(Contract.DiaryActivity.NAME))
                        act.mId = cursor.getInt(cursor.getColumnIndex(Contract.DiaryActivity._ID))
                        for (listener in mDataChangeListeners) {
                            // notify about the (re-)added activity
                            listener.onActivityAdded(act)
                        }
                    }
                }
            }
        }
        cursor.close()
    }// activity terminated, so we have to notify here...

    //            showCurrentActivityNotification();
    /* update the current diary entry to "finish" it
       * in theory there should be only one entry with end = NULL in the diary table
       * but who knows? -> Let's update all. */
    var currentActivity: DiaryActivity?
        get() = mCurrentActivity
        set(activity) {
            /* update the current diary entry to "finish" it
         * in theory there should be only one entry with end = NULL in the diary table
         * but who knows? -> Let's update all. */
            if (mCurrentActivity !== activity) {
                val values = ContentValues()
                val timestamp = System.currentTimeMillis()
                values.put(Contract.Diary.END, timestamp)
                startUpdate(
                    UPDATE_CLOSE_ACTIVITY, timestamp, Contract.Diary.CONTENT_URI,
                    values, Contract.Diary.END + " is NULL", null
                )
                mCurrentActivity = activity
                currentDiaryUri = null
                currentActivityStartTime.time = timestamp
                currentNote = ""
                if (mCurrentActivity == null) {
                    // activity terminated, so we have to notify here...
                    for (listener in mDataChangeListeners) {
                        listener.onActivityChanged()
                    }
                }
                LocationHelper.helper.updateLocation(false)
                //            showCurrentActivityNotification();
            }
        }

    /* undo the last activity selection by deleteing all open entries
     *
     * */
    fun undoLastActivitySelection() {
        if (mCurrentActivity != null) {
            startDelete(
                DELETE_LAST_DIARY_ENTRY, null,
                Contract.Diary.CONTENT_URI,
                Contract.Diary.END + " is NULL",
                null
            )
        }
    }

    override fun onUpdateComplete(token: Int, cookie: Any?, result: Int) {
        if (token == UPDATE_CLOSE_ACTIVITY) {
            if (mCurrentActivity != null) {
                /* create a new diary entry */
                val values = ContentValues()
                values.put(Contract.Diary.ACT_ID, mCurrentActivity!!.mId)
                values.put(Contract.Diary.START, cookie as Long)
                startInsert(
                    INSERT_NEW_DIARY_ENTRY, cookie, Contract.Diary.CONTENT_URI,
                    values
                )
            }
        } else if (token == UPDATE_ACTIVITY) {
            for (listener in mDataChangeListeners) {
                listener.onActivityDataChanged(cookie as DiaryActivity)
            }
        } else if (token == REOPEN_LAST_DIARY_ENTRY) {
            mCurrentActivity = null
            readCurrentActivity()
        } else if (token == UNDELETE_ACTIVITY) {
            val act = cookie as DiaryActivity
            startQuery(
                UNDELETE_ACTIVITY, cookie,
                Contract.DiaryActivity.CONTENT_URI,
                ACTIVITIES_PROJ, Contract.DiaryActivity._ID + " = " + act.mId,
                null,
                null
            )
        }
    }

    override fun onDeleteComplete(token: Int, cookie: Any, result: Int) {
        if (token == DELETE_LAST_DIARY_ENTRY) {
            val values = ContentValues()
            values.putNull(Contract.Diary.END)
            startUpdate(
                REOPEN_LAST_DIARY_ENTRY, null,
                Contract.Diary.CONTENT_URI,
                values,
                Contract.Diary.END + "=(SELECT MAX(" + Contract.Diary.END + ") FROM " + Contract.Diary.TABLE_NAME + " )",
                null
            )
        }
    }

    override fun onInsertComplete(token: Int, cookie: Any, uri: Uri) {
        if (token == INSERT_NEW_DIARY_ENTRY) {
            currentDiaryUri = uri
            for (listener in mDataChangeListeners) {
                listener.onActivityChanged()
            }
        } else if (token == INSERT_NEW_ACTIVITY) {
            val act = cookie as DiaryActivity
            act.mId = uri.lastPathSegment!!.toInt()
            synchronized(this) {
                activities.add(act)
                unsortedActivities.add(act)
            }
            for (listener in mDataChangeListeners) {
                listener.onActivityAdded(act)
            }
            if (PreferenceManager
                    .getDefaultSharedPreferences(MVApplication.appContext!!)
                    .getBoolean(SettingsActivity.KEY_PREF_AUTO_SELECT, true)
            ) {
                currentActivity = act
            }
        }
    }

    fun updateActivity(act: DiaryActivity) {
        startUpdate(
            UPDATE_ACTIVITY,
            act,
            ContentUris.withAppendedId(Contract.DiaryActivity.CONTENT_URI, act.mId.toLong()),
            contentFor(act),
            null,
            null
        )
        for (listener in mDataChangeListeners) {
            listener.onActivityDataChanged(act)
        }
    }

    /* undelete an activity with given ID */
    fun undeleteActivity(id: Int, name: String): DiaryActivity {
        val result = DiaryActivity(id, name, 0)
        val values = ContentValues()
        values.put(Contract.Diary._DELETED, 0)
        startUpdate(
            UNDELETE_ACTIVITY, result, Contract.Diary.CONTENT_URI,
            values, Contract.Diary._ID + " = " + id, null
        )
        activities.add(result)
        unsortedActivities.add(result)
        return result
    }

    /* inserts a new activity and sets it as the current one if configured in the preferences */
    fun insertActivity(act: DiaryActivity) {
        startInsert(
            INSERT_NEW_ACTIVITY,
            act,
            Contract.DiaryActivity.CONTENT_URI,
            contentFor(act)
        )
    }

    fun deleteActivity(act: DiaryActivity) {
        if (act === mCurrentActivity) {
            currentActivity = null
        }
        val values = ContentValues()
        values.put(Contract.DiaryActivity._DELETED, "1")
        startUpdate(
            UPDATE_DELETE_ACTIVITY,
            act,
            ContentUris.withAppendedId(Contract.DiaryActivity.CONTENT_URI, act.mId.toLong()),
            values,
            null,  /* entry selected via URI */
            null
        )
        synchronized(this) {
            if (activities.remove(act)) {
                unsortedActivities.remove(act)
            } else {
                Log.e(TAG, "removal of activity $act failed")
            }
        }
        for (listener in mDataChangeListeners) {
            listener.onActivityRemoved(act)
        }
    }

    fun activityWithId(id: Int): DiaryActivity? {
        /* TODO improve performance by storing the DiaryActivities in a map or Hashtable instead of a list */
        synchronized(this) {
            if (unsortedActivities.isEmpty()) {
                /* activities not yet loaded, so it doesn't make sense yet to read the activities */
                try {
                    Thread.sleep(50)
                } catch (e: InterruptedException) {
                    /* intended empty */
                }
            }
            for (a in activities) {
                if (a.mId == id) {
                    return a
                }
            }
        }
        return null
    }

    /* reevaluate ALL conditions, very heavy operation, do not trigger without need */
    fun evaluateAllConditions() {
        for (c in conditions) {
            c.refresh()
        }
    }

    private var likeliActivites = HashMap<DiaryActivity, Double>(1)

    /* Access only allowed via ActivityHelper.helper singleton */
    init {
        mDataChangeListeners = ArrayList(3)
        activities = ArrayList(50)
        unsortedActivities = ArrayList(50)
        conditions = arrayOf(
            AlphabeticalCondition(this),
            GlobalOccurrenceCondition(this),
            RecentOccurrenceCondition(this)
        )
        reloadAll()
        LocationHelper.helper.updateLocation(false)
        currentActivityStartTime = Date()
    }

    fun likelihoodFor(a: DiaryActivity): Double {
        return if (likeliActivites.containsKey(a)) {
            likeliActivites[a]!!
        } else 0.0
    }

    fun reorderActivites() {
        synchronized(this) {
            val `as`: List<DiaryActivity> = activities
            likeliActivites = HashMap(`as`.size)
            for (a in `as`) {
                likeliActivites[a] = 0.0
            }

            // reevaluate the conditions
            for (c in conditions) {
                val s = c.likelihoods()
                for (l in s) {
                    if (!likeliActivites.containsKey(l.activity)) {
                        Log.e(
                            TAG,
                            String.format(
                                "Activity %s not in likeliActivites %s",
                                l.activity,
                                `as`.contains(l.activity)
                            )
                        )
                    } else {
                        val lv = likeliActivites[l.activity]
                        if (lv == null) {
                            Log.e(
                                TAG,
                                String.format(
                                    "Activity %s has no likelyhood in Condition %s",
                                    l.activity,
                                    c.javaClass.simpleName
                                )
                            )
                        } else {
                            likeliActivites[l.activity] = lv + l.likelihood
                        }
                    }
                }
            }
            val list: MutableList<DiaryActivity> = ArrayList(likeliActivites.keys)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Collections.sort(list, Collections.reverseOrder(Comparator.comparing { o: DiaryActivity ->
                        likeliActivites[o]!!
                }))
            }
            activities = list
        }
        for (listener in mDataChangeListeners) {
            listener.onActivityOrderChanged()
        }

//        updateNotification();
    }

    /*
     * collect results from all Conditions (if all are finished)
     * can be called from any Thread
     */
    fun conditionEvaluationFinished() {
        val completeMessage = mHandler.obtainMessage()
        completeMessage.sendToTarget()
    }

    /* perform cyclic actions like update of timing on current activity and checking time based Conditions */
    fun cyclicUpdate() {
        // TODO add a service like RefreshService, to call this with configurable cycle time
    }

    companion object {
        private val TAG = ActivityHelper::class.java.name
        private const val QUERY_ALL_ACTIVITIES = 0
        private const val UPDATE_CLOSE_ACTIVITY = 1
        private const val INSERT_NEW_DIARY_ENTRY = 2
        private const val UPDATE_ACTIVITY = 3
        private const val INSERT_NEW_ACTIVITY = 4
        private const val UPDATE_DELETE_ACTIVITY = 5
        private const val QUERY_CURRENT_ACTIVITY = 6
        private const val DELETE_LAST_DIARY_ENTRY = 7
        private const val REOPEN_LAST_DIARY_ENTRY = 8
        private const val UNDELETE_ACTIVITY = 9
        private val DIARY_PROJ = arrayOf(
            Contract.Diary.ACT_ID,
            Contract.Diary.START,
            Contract.Diary.END,
            Contract.Diary.TABLE_NAME + "." + Contract.Diary._ID,
            Contract.Diary.NOTE
        )
        private val ACTIVITIES_PROJ = arrayOf(
            Contract.DiaryActivity._ID,
            Contract.DiaryActivity.NAME,
            Contract.DiaryActivity.COLOR
        )
        private const val SELECTION = Contract.DiaryActivity._DELETED + "=0"
        @JvmField
        val helper = ActivityHelper()
        @JvmStatic
        fun sortedActivities(query: String): ArrayList<DiaryActivity> {
            val filtered = ArrayList<DiaryActivity>(helper.activities.size)
            val filteredDist = ArrayList<Int>(helper.activities.size)
            for (a in helper.activities) {
                val dist = searchDistance(query, a.mName)
                var pos = 0
                // search where to enter it
                for (i in filteredDist) {
                    if (dist > i) {
                        pos++
                    } else {
                        break
                    }
                }
                filteredDist.add(pos, dist)
                filtered.add(pos, a)
            }
            return filtered
        }

        private fun contentFor(act: DiaryActivity): ContentValues {
            val result = ContentValues()
            result.put(Contract.DiaryActivity.NAME, act.mName)
            result.put(Contract.DiaryActivity.COLOR, act.mColor)
            return result
        }

        /* calculate the "search" distance between search string and model
     * Code based on Levensthein distance from https://en.wikibooks.org/wiki/Algorithm_Implementation/Strings/Levenshtein_distance#Java
     */
        fun searchDistance(inSearch: CharSequence, inModel: CharSequence): Int {
            val search = inSearch.toString().lowercase(Locale.getDefault()) // s0
            val model = inModel.toString().lowercase(Locale.getDefault()) // s1
            var result: Int
            val len0 = search.length + 1
            val len1 = model.length + 1


            // the array of distances
            var cost = IntArray(len0)
            var newcost = IntArray(len0)

            // initial cost of skipping prefix in String s0
            for (i in 0 until len0) cost[i] = i

            // dynamically computing the array of distances

            // transformation cost for each letter in s1
            for (j in 1 until len1) {
                // initial cost of skipping prefix in String s1
                newcost[0] = j

                // transformation cost for each letter in s0
                for (i in 1 until len0) {
                    // matching current letters in both strings
                    val match = if (search[i - 1] == model[j - 1]) 0 else 1

                    // computing cost for each transformation
                    val cost_replace = cost[i - 1] + match
                    val cost_insert = cost[i] + 1
                    val cost_delete = newcost[i - 1] + 1

                    // keep minimum cost
                    newcost[i] = min(min(cost_insert, cost_delete), cost_replace)
                }

                // swap cost/newcost arrays
                val swap = cost
                cost = newcost
                newcost = swap
            }

            // the distance is the cost for transforming all letters in both strings
            result = cost[len0 - 1]

            // we want to give some preference for true substrings and character occurrences
            if (model.contains(search)) {
                result -= 30
            }
            if (model.startsWith(search)) {
                result -= 10
            }
            for (element in search) {
                val idx = model.indexOf(element)
                if (idx < 0) {
                    result += 4
                }
            }
            return result
        }
    }
}