/*
 * LifeDots
 *
 * Copyright (C) 2020 Xilin Jia https://github.com/XilinJia
  ~ Copyright (C) 2017-2018 Raphael Mack http://www.raphael-mack.de
 * Copyright (C) 2018 Bc. Ondrej Janitor
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

package com.mdiqentw.lifedots.helpers;

import android.annotation.SuppressLint;
import android.content.AsyncQueryHandler;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import com.mdiqentw.lifedots.MVApplication;
import com.mdiqentw.lifedots.db.Contract;
import com.mdiqentw.lifedots.db.LDContentProvider;
import com.mdiqentw.lifedots.model.DiaryActivity;
import com.mdiqentw.lifedots.model.conditions.AlphabeticalCondition;
import com.mdiqentw.lifedots.model.conditions.Condition;
import com.mdiqentw.lifedots.model.conditions.GlobalOccurrenceCondition;
import com.mdiqentw.lifedots.model.conditions.RecentOccurrenceCondition;
import com.mdiqentw.lifedots.ui.settings.SettingsActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/*
 * LifeDots
 *
 * Copyright (C) 2020 Xilin Jia https://github.com/XilinJia
 * Copyright (C) 2017-2018 Raphael Mack http://www.raphael-mack.de
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
 * provide a smooth interface to an OO abstraction of the data for our diary.
 */
public class ActivityHelper extends AsyncQueryHandler{
    private static final String TAG = ActivityHelper.class.getName();

    private static final int QUERY_ALL_ACTIVITIES = 0;
    private static final int UPDATE_CLOSE_ACTIVITY = 1;
    private static final int INSERT_NEW_DIARY_ENTRY = 2;
    private static final int UPDATE_ACTIVITY = 3;
    private static final int INSERT_NEW_ACTIVITY = 4;
    private static final int UPDATE_DELETE_ACTIVITY = 5;
    private static final int QUERY_CURRENT_ACTIVITY = 6;
    private static final int DELETE_LAST_DIARY_ENTRY = 7;
    private static final int REOPEN_LAST_DIARY_ENTRY = 8;
    private static final int UNDELETE_ACTIVITY = 9;

    private static final String[] DIARY_PROJ = new String[] {
            Contract.Diary.ACT_ID,
            Contract.Diary.START,
            Contract.Diary.END,
            Contract.Diary.TABLE_NAME + "." + Contract.Diary._ID,
            Contract.Diary.NOTE
    };
    private static final String[] ACTIVITIES_PROJ = new String[] {
            Contract.DiaryActivity._ID,
            Contract.DiaryActivity.NAME,
            Contract.DiaryActivity.COLOR
    };
    private static final String SELECTION = Contract.DiaryActivity._DELETED + "=0";

    public static final ActivityHelper helper = new ActivityHelper();

    /* list of all activities, not including deleted ones */
    private List<DiaryActivity> activities;
    /* unsortedActivities is not allowed to be modified */
    private final List<DiaryActivity> unsortedActivities;

    private DiaryActivity mCurrentActivity = null;
    final Date mCurrentActivityStartTime;
    private @Nullable Uri mCurrentDiaryUri;
    private /* @NonNull */ String mCurrentNote;
    private final Condition[] conditions;

//    private DetailViewModel viewModel;

    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        /*
         * handleMessage() defines the operations to perform when
         * the Handler receives a new Message to process.
         */
        @Override
        public void handleMessage(Message inputMessage) {
            // so far we only have one message here so no need to look at the details
            // just assume that at least one Condition evaluation is finished and we check
            // whether all are done
            boolean allDone = true;
            for(Condition c:conditions){
                if(c.isActive()){
                    allDone = false;
                    break;
                }
            }
            if(allDone) {
                reorderActivites();
            }
        }

    };

    /* to be used only in the UI thread, consider getActivitiesCopy() */
    public List<DiaryActivity> getActivities() {
        return activities;
    }

    /* get a list of the activities as non-modifable copy, not guaranteed to be up to date */
    public List<DiaryActivity> getUnsortedActivities() {
        List<DiaryActivity> result = new ArrayList<>(unsortedActivities.size());
        synchronized (this){
            if(unsortedActivities.isEmpty()){
                /* activities not yet loaded, so it doesn't make sense yet to read the activities */
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    /* intended empty */
                }
            }

            result.addAll(unsortedActivities);
        }
        return result;
    }

    public static ArrayList<DiaryActivity> sortedActivities(String query) {
        ArrayList<DiaryActivity> filtered = new ArrayList<>(ActivityHelper.helper.activities.size());
        ArrayList<Integer> filteredDist = new ArrayList<>(ActivityHelper.helper.activities.size());
        for(DiaryActivity a : ActivityHelper.helper.activities){
            int dist = ActivityHelper.searchDistance(query, a.getName());
            int pos = 0;
            // search where to enter it
            for(Integer i : filteredDist){
                if(dist > i){
                    pos++;
                }else{
                    break;
                }
            }
            filteredDist.add(pos, dist);
            filtered.add(pos, a);
        }
        return filtered;
    }

    public interface DataChangedListener{
        /**
         * Called when the data has changed and no further specification is possible.
         * => everything needs to be refreshed!
         */
        void onActivityDataChanged();

        /**
         * Called when the data of one activity was changed.
         */
        void onActivityDataChanged(DiaryActivity activity);

        /**
         * Called on addition of an activity.
         */
        void onActivityAdded(DiaryActivity activity);

        /**
         * Called on removal of an activity.
         */
        void onActivityRemoved(DiaryActivity activity);

        /**
         * Called on change of the current activity.
         */
        void onActivityChanged();

        /**
         * Called on change of the activity order due to likelyhood.
         */
        void onActivityOrderChanged();

    }
    private final List<DataChangedListener> mDataChangeListeners;

    public void registerDataChangeListener(DataChangedListener listener){
        mDataChangeListeners.add(listener);
    }

    public void unregisterDataChangeListener(DataChangedListener listener){
        mDataChangeListeners.remove(listener);
    }

    /* Access only allowed via ActivityHelper.helper singleton */
    private ActivityHelper(){
        super(MVApplication.getAppContext().getContentResolver());
        mDataChangeListeners = new ArrayList<>(3);
        activities = new ArrayList<>(50);
        unsortedActivities = new ArrayList<>(50);

        conditions = new Condition[]{
                new AlphabeticalCondition(this),
                new GlobalOccurrenceCondition(this),
                new RecentOccurrenceCondition(this),
        };
        reloadAll();

        LocationHelper.helper.updateLocation();
        mCurrentActivityStartTime = new Date();
    }

    /* reload all the activities from the database */
    public void reloadAll(){
        ContentResolver resolver = MVApplication.getAppContext().getContentResolver();
        ContentProviderClient client = resolver.acquireContentProviderClient(Contract.AUTHORITY);
        LDContentProvider provider = (LDContentProvider) client.getLocalContentProvider();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            client.close();
        } else {
            client.release();
        }
        provider.resetDatabase();

        startQuery(QUERY_ALL_ACTIVITIES, null, Contract.DiaryActivity.CONTENT_URI,
                ACTIVITIES_PROJ, SELECTION, null,
                null);
    }

    /* start the query to read the current activity
     * will trigger the update of currentActivity and send notifications afterwards */
    public void readCurrentActivity() {
        startQuery(QUERY_CURRENT_ACTIVITY, null, Contract.Diary.CONTENT_URI,
                DIARY_PROJ, Contract.Diary.START + " = (SELECT MAX("
                + Contract.Diary.START + ") FROM "
                + Contract.Diary.TABLE_NAME + " WHERE " + SELECTION +")"
                , null,
                Contract.Diary.START + " DESC");
    }

    @SuppressLint("Range")
    @Override
    protected void onQueryComplete(int token, Object cookie,
                                   Cursor cursor) {
        if ((cursor != null) && cursor.moveToFirst()) {
            if (token == QUERY_ALL_ACTIVITIES) {
                synchronized (this) {
                    activities.clear();
                    unsortedActivities.clear();
                    while (!cursor.isAfterLast()) {
                        DiaryActivity act = new DiaryActivity(cursor.getInt(cursor.getColumnIndex(Contract.DiaryActivity._ID)),
                                cursor.getString(cursor.getColumnIndex(Contract.DiaryActivity.NAME)),
                                cursor.getInt(cursor.getColumnIndex(Contract.DiaryActivity.COLOR)));
                        /* TODO: optimize by keeping a map with id as key and the DiaryActivities */
                        activities.add(act);
                        unsortedActivities.add(act);
                        cursor.moveToNext();
                    }
                }
                readCurrentActivity();
                for (DataChangedListener listener : mDataChangeListeners) {
                    listener.onActivityDataChanged();
                }
            } else if (token == QUERY_CURRENT_ACTIVITY) {
                if (!cursor.isNull(cursor.getColumnIndex(Contract.Diary.END))) {
                    /* no current activity */
                    mCurrentNote = "";
                    mCurrentDiaryUri = null;
                    mCurrentActivityStartTime.setTime(cursor.getLong(cursor.getColumnIndex(Contract.Diary.END)));
                } else {
                    mCurrentActivity = activityWithId(cursor.getInt(cursor.getColumnIndex(Contract.Diary.ACT_ID)));
                    mCurrentActivityStartTime.setTime(cursor.getLong(cursor.getColumnIndex(Contract.Diary.START)));
                    mCurrentNote = cursor.getString(cursor.getColumnIndex(Contract.Diary.NOTE));
                    mCurrentDiaryUri = Uri.withAppendedPath(Contract.Diary.CONTENT_URI,
                                        Long.toString(cursor.getLong(cursor.getColumnIndex(Contract.Diary._ID))));

                }
//                showCurrentActivityNotification();

                for (DataChangedListener listener : mDataChangeListeners) {
                    listener.onActivityChanged();
                }
            } else if (token == UNDELETE_ACTIVITY){

                DiaryActivity act = (DiaryActivity)cookie;
                act.setColor(cursor.getInt(cursor.getColumnIndex(Contract.DiaryActivity.COLOR)));
                act.setName(cursor.getString(cursor.getColumnIndex(Contract.DiaryActivity.NAME)));
                act.setId(cursor.getInt(cursor.getColumnIndex(Contract.DiaryActivity._ID)));

                for(DataChangedListener listener : mDataChangeListeners) {
                    // notify about the (re-)added activity
                    listener.onActivityAdded(act);
                }

            }
        }
        if (cursor != null) {
            cursor.close();
        }
    }

    public DiaryActivity getCurrentActivity(){
        return mCurrentActivity;
    }
    public Date getCurrentActivityStartTime() { return mCurrentActivityStartTime;}
    public String getCurrentNote() { return mCurrentNote;}
    public void setCurrentNote(String str) { mCurrentNote = str;}

    public void setCurrentActivity(@Nullable DiaryActivity activity){
        /* update the current diary entry to "finish" it
         * in theory there should be only one entry with end = NULL in the diary table
         * but who knows? -> Let's update all. */
        if(mCurrentActivity != activity) {
            ContentValues values = new ContentValues();
            Long timestamp = System.currentTimeMillis();
            values.put(Contract.Diary.END, timestamp);

            startUpdate(UPDATE_CLOSE_ACTIVITY, timestamp, Contract.Diary.CONTENT_URI,
                    values, Contract.Diary.END + " is NULL", null);

            mCurrentActivity = activity;
            mCurrentDiaryUri = null;
            mCurrentActivityStartTime.setTime(timestamp);
            mCurrentNote = "";
            if(mCurrentActivity == null){
                // activity terminated, so we have to notify here...
                for(DataChangedListener listener : mDataChangeListeners) {
                    listener.onActivityChanged();
                }
            }
            LocationHelper.helper.updateLocation();
//            showCurrentActivityNotification();
        }
    }

    /* undo the last activity selection by deleteing all open entries
     *
     * */
    public void undoLastActivitySelection() {
        if(mCurrentActivity != null) {
            startDelete(DELETE_LAST_DIARY_ENTRY, null,
                    Contract.Diary.CONTENT_URI,
                    Contract.Diary.END + " is NULL",
                    null);
        }
    }

    @Override
    protected void onUpdateComplete(int token, Object cookie, int result) {
        if(token == UPDATE_CLOSE_ACTIVITY) {
            if(mCurrentActivity != null) {
                /* create a new diary entry */
                ContentValues values = new ContentValues();

                values.put(Contract.Diary.ACT_ID, mCurrentActivity.getId());
                values.put(Contract.Diary.START, (Long)cookie);

                startInsert(INSERT_NEW_DIARY_ENTRY, cookie, Contract.Diary.CONTENT_URI,
                        values);
            }
        }else if(token == UPDATE_ACTIVITY){
            for(DataChangedListener listener : mDataChangeListeners) {
                listener.onActivityDataChanged((DiaryActivity)cookie);
            }
        }else if(token == REOPEN_LAST_DIARY_ENTRY){
            mCurrentActivity = null;
            readCurrentActivity();
        }else if(token == UNDELETE_ACTIVITY){
            DiaryActivity act = (DiaryActivity)cookie;

            startQuery(UNDELETE_ACTIVITY, cookie,
                    Contract.DiaryActivity.CONTENT_URI,
                    ACTIVITIES_PROJ, Contract.DiaryActivity._ID + " = " + act.getId(),
                    null,
                    null);
        }
    }


    @Override
    protected void onDeleteComplete(int token, Object cookie, int result) {
        if(token == DELETE_LAST_DIARY_ENTRY){
            ContentValues values = new ContentValues();
            values.putNull(Contract.Diary.END);

            startUpdate(REOPEN_LAST_DIARY_ENTRY, null,
                    Contract.Diary.CONTENT_URI,
                    values,
                    Contract.Diary.END + "=(SELECT MAX(" + Contract.Diary.END + ") FROM " + Contract.Diary.TABLE_NAME + " )",
                    null
            );
        }
    }

    @Override
    protected void onInsertComplete(int token, Object cookie, Uri uri) {
        if (token == INSERT_NEW_DIARY_ENTRY) {
            mCurrentDiaryUri = uri;
            for(DataChangedListener listener : mDataChangeListeners) {
                listener.onActivityChanged();
            }

        } else if (token == INSERT_NEW_ACTIVITY) {

            DiaryActivity act = (DiaryActivity)cookie;
            act.setId(Integer.parseInt(uri.getLastPathSegment()));
            synchronized (this) {
                activities.add(act);
                unsortedActivities.add(act);
            }
            for(DataChangedListener listener : mDataChangeListeners) {
                listener.onActivityAdded(act);
            }
            if(PreferenceManager
                    .getDefaultSharedPreferences(MVApplication.getAppContext())
                    .getBoolean(SettingsActivity.KEY_PREF_AUTO_SELECT, true)){
                setCurrentActivity(act);
            }
        }
    }

    public void updateActivity(DiaryActivity act) {
        startUpdate(UPDATE_ACTIVITY,
                act,
                ContentUris.withAppendedId(Contract.DiaryActivity.CONTENT_URI, act.getId()),
                contentFor(act),
                null,
                null);

        for(DataChangedListener listener : mDataChangeListeners) {
            listener.onActivityDataChanged(act);
        }
    }

    /* undelete an activity with given ID */
    public DiaryActivity undeleteActivity(int id, String name){
        DiaryActivity result = new DiaryActivity(id, name, 0);
        ContentValues values = new ContentValues();
        values.put(Contract.Diary._DELETED, 0);

        startUpdate(UNDELETE_ACTIVITY, result, Contract.Diary.CONTENT_URI,
                values, Contract.Diary._ID + " = " + id, null);

        activities.add(result);
        unsortedActivities.add(result);
        return result;
    }

    /* inserts a new activity and sets it as the current one if configured in the preferences */
    public void insertActivity(DiaryActivity act){
        startInsert(INSERT_NEW_ACTIVITY,
                act,
                Contract.DiaryActivity.CONTENT_URI,
                contentFor(act));
    }

    public void deleteActivity(DiaryActivity act) {
        if(act == mCurrentActivity){
            setCurrentActivity(null);
        }
        ContentValues values = new ContentValues();
        values.put(Contract.DiaryActivity._DELETED, "1");

        startUpdate(UPDATE_DELETE_ACTIVITY,
                act,
                ContentUris.withAppendedId(Contract.DiaryActivity.CONTENT_URI, act.getId()),
                values,
                null, /* entry selected via URI */
                null);
        synchronized (this) {
            if (activities.remove(act)) {
                unsortedActivities.remove(act);
            } else {
                Log.e(TAG, "removal of activity " + act + " failed");
            }
        }
        for(DataChangedListener listener : mDataChangeListeners) {
            listener.onActivityRemoved(act);
        }
    }

    @Nullable
    public DiaryActivity activityWithId(int id){
        /* TODO improve performance by storing the DiaryActivities in a map or Hashtable instead of a list */
        synchronized (this) {
            if(unsortedActivities.isEmpty()){
                /* activities not yet loaded, so it doesn't make sense yet to read the activities */
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    /* intended empty */
                }
            }
            for (DiaryActivity a : activities) {
//                System.out.println("activity: " + a.getName());
                if (a.getId() == id) {
                    return a;
                }
            }
        }
        return null;
    }

    private static ContentValues contentFor(DiaryActivity act){
        ContentValues result = new ContentValues();
        result.put(Contract.DiaryActivity.NAME, act.getName());
        result.put(Contract.DiaryActivity.COLOR, act.getColor());
        return result;
    }

    public @Nullable Uri getCurrentDiaryUri(){
        return mCurrentDiaryUri;
    }

    /* calculate the "search" distance between search string and model
     * Code based on Levensthein distance from https://en.wikibooks.org/wiki/Algorithm_Implementation/Strings/Levenshtein_distance#Java
     */
    public static int searchDistance(CharSequence inSearch, CharSequence inModel) {
        String search = inSearch.toString().toLowerCase(Locale.getDefault()); // s0
        String model = inModel.toString().toLowerCase(Locale.getDefault());   // s1
        int result;
        int len0 = search.length() + 1;
        int len1 = model.length() + 1;


        // the array of distances
        int[] cost = new int[len0];
        int[] newcost = new int[len0];

        // initial cost of skipping prefix in String s0
        for (int i = 0; i < len0; i++) cost[i] = i;

        // dynamically computing the array of distances

        // transformation cost for each letter in s1
        for (int j = 1; j < len1; j++) {
            // initial cost of skipping prefix in String s1
            newcost[0] = j;

            // transformation cost for each letter in s0
            for(int i = 1; i < len0; i++) {
                // matching current letters in both strings
                int match = (search.charAt(i - 1) == model.charAt(j - 1)) ? 0 : 1;

                // computing cost for each transformation
                int cost_replace = cost[i - 1] + match;
                int cost_insert  = cost[i] + 1;
                int cost_delete  = newcost[i - 1] + 1;

                // keep minimum cost
                newcost[i] = Math.min(Math.min(cost_insert, cost_delete), cost_replace);
            }

            // swap cost/newcost arrays
            int[] swap = cost; cost = newcost; newcost = swap;
        }

        // the distance is the cost for transforming all letters in both strings
        result = cost[len0 - 1];

        // we want to give some preference for true substrings and character occurrences
        if(model.contains(search)){
            result = result - 30;
        }
        if(model.startsWith(search)){
            result = result - 10;
        }
        for(int i = 0; i < search.length(); i++){
            int idx = model.indexOf(search.charAt(i));
            if(idx < 0){
                result = result + 4;
            }
        }
        return result;
    }

    /* reevaluate ALL conditions, very heavy operation, do not trigger without need */
    public void evaluateAllConditions() {
        for (Condition c : conditions) {
            c.refresh();
        }
    }

    private HashMap<DiaryActivity, Double> likeliActivites = new HashMap<>(1);
    public double likelihoodFor(DiaryActivity a){
        if(likeliActivites.containsKey(a)){
            //noinspection ConstantConditions
            return likeliActivites.get(a);
        }
        return 0.0;
    }

    public void reorderActivites() {
        synchronized (this) {
            List<DiaryActivity> as = activities;
            likeliActivites = new HashMap<>(as.size());

            for (DiaryActivity a : as) {
                likeliActivites.put(a, 0.0);
            }

            // reevaluate the conditions
            for (Condition c : conditions) {
                List<Condition.Likelihood> s = c.likelihoods();
                for (Condition.Likelihood l : s) {
                    if (!likeliActivites.containsKey(l.activity)) {
                        Log.e(TAG, String.format("Activity %s not in likeliActivites %s", l.activity, as.contains(l.activity)));
                    } else {
                        Double lv = likeliActivites.get(l.activity);
                        if (lv == null) {
                            Log.e(TAG, String.format("Activity %s has no likelyhood in Condition %s", l.activity, c.getClass().getSimpleName()));
                        } else {
                            likeliActivites.put(l.activity, lv + l.likelihood);
                        }
                    }
                }
            }

            List<DiaryActivity> list = new ArrayList<>(likeliActivites.keySet());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Collections.sort(list, Collections.reverseOrder(Comparator.comparing(o -> Objects.requireNonNull(likeliActivites.get(o)))));
            }
            activities = list;

            /* is one of the conditions currently evaluating? */
//            boolean reorderingInProgress = false;
        }

        for(DataChangedListener listener : mDataChangeListeners) {
            listener.onActivityOrderChanged();
        }

//        updateNotification();
    }

    /*
     * collect results from all Conditions (if all are finished)
     * can be called from any Thread
     */
    public void conditionEvaluationFinished() {
        Message completeMessage =
                mHandler.obtainMessage();
        completeMessage.sendToTarget();
    }

    /* perform cyclic actions like update of timing on current activity and checking time based Conditions */
    public void cyclicUpdate(){
        // TODO add a service like RefreshService, to call this with configurable cycle time
    }
}
