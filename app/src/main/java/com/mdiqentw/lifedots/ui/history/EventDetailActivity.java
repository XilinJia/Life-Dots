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
package com.mdiqentw.lifedots.ui.history;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.AsyncQueryHandler;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.DialogFragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.mdiqentw.lifedots.MVApplication;
import com.mdiqentw.lifedots.R;
import com.mdiqentw.lifedots.databinding.ActivityEventDetailContentBinding;
import com.mdiqentw.lifedots.db.Contract;
import com.mdiqentw.lifedots.helpers.ActivityHelper;
import com.mdiqentw.lifedots.ui.generic.BaseActivity;
import com.mdiqentw.lifedots.ui.generic.DetailRecyclerViewAdapter;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Objects;

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

/*
 * HistoryDetailActivity to show details of and modify diary entries
 *
 * */
public class EventDetailActivity extends BaseActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String[] PROJECTION_IMG = new String[] {
            Contract.DiaryImage.URI,
            Contract.DiaryImage._ID
    };

    private static final int READ_ALL = 1;
    private static final int UPDATE_ENTRY = 2;
    private static final int UPDATE_PRE = 3;
    private static final int UPDATE_SUCC = 4;

    ActivityEventDetailContentBinding binding;

    private DetailRecyclerViewAdapter detailAdapter;

    private final boolean[] mUpdatePending = new boolean[UPDATE_SUCC + 1];
//    private static final int OVERLAP_CHECK = 5;

    private final String[] ENTRY_PROJ = new String[]{
            Contract.DiaryActivity.TABLE_NAME + "." + Contract.DiaryActivity.NAME,
            Contract.DiaryActivity.TABLE_NAME + "." + Contract.DiaryActivity.COLOR,
            Contract.Diary.TABLE_NAME + "." + Contract.Diary.ACT_ID,
            Contract.Diary.TABLE_NAME + "." + Contract.Diary._ID,
            Contract.Diary.NOTE,
            Contract.Diary.START,
            Contract.Diary.END};

    private static final String DIRAY_ENTRY_ID_KEY = "ENTRY_ID";
    private static final String UPDATE_VALUE_KEY = "UPDATE_VALUE";
    private static final String ADJUST_ADJACENT_KEY = "ADJUST_ADJACENT";

    final String dateFormatString = MVApplication.getAppContext().getResources().getString(R.string.date_format);
    final String timeFormatString = MVApplication.getAppContext().getResources().getString(R.string.time_format);
//    private final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(MVApplication.getAppContext());

    /* the id of the currently displayed diary entry */
    private long diaryEntryID;
//    private int actId;

    private Calendar start, storedStart;
    private Calendar end, storedEnd;

    private ContentValues updateValues = new ContentValues();
    private boolean mIsCurrent;

    public static class TimePickerFragment extends DialogFragment{
        private int hour, minute;
        private TimePickerDialog.OnTimeSetListener listener;

        public void setData(TimePickerDialog.OnTimeSetListener listener,
                           int hour, int minute){
            this.hour = hour;
            this.minute = minute;
            this.listener = listener;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current time as the default values for the picker
            // Create a new instance of TimePickerDialog and return it
            return new TimePickerDialog(getActivity(), listener, hour, minute,
                    DateFormat.is24HourFormat(getActivity()));
        }
    }

    public static class DatePickerFragment extends DialogFragment{
        private int year, month, day;
        private DatePickerDialog.OnDateSetListener listener;

        public void setData(DatePickerDialog.OnDateSetListener listener,
                            int year, int mount, int day){
            this.year = year;
            this.month = mount;
            this.day = day;
            this.listener = listener;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current time as the default values for the picker
            // Create a new instance of TimePickerDialog and return it
            return new DatePickerDialog(getActivity(), listener, year, month, day);
        }
    }

    @SuppressLint("Range")
    public void doTokenReadAll(Cursor cursor) {
        if(cursor.moveToFirst()) {
            start = Calendar.getInstance();
            storedStart = Calendar.getInstance();
            start.setTimeInMillis(cursor.getLong(cursor.getColumnIndex(Contract.Diary.START)));
            storedStart.setTimeInMillis(cursor.getLong(cursor.getColumnIndex(Contract.Diary.START)));
            end = Calendar.getInstance();
            storedEnd = Calendar.getInstance();
            long endMillis = cursor.getLong(cursor.getColumnIndex(Contract.Diary.END));
            storedEnd.setTimeInMillis(endMillis);
            if (endMillis != 0) {
                end.setTimeInMillis(endMillis);
                mIsCurrent = false;
            } else {
                mIsCurrent = true;
            }

            if(!updateValues.containsKey(Contract.Diary.NOTE)) {
                binding.editActivityNote.setText(cursor.getString(cursor.getColumnIndex(Contract.Diary.NOTE)));
            }
//            actId = cursor.getInt(cursor.getColumnIndex(Contract.Diary.ACT_ID));
//            System.out.println("Contract.DiaryActivity: " + cursor.getString(cursor.getColumnIndex(Contract.DiaryActivity.NAME)) +
//                    " " + actId);
            binding.row.name.setText(
                    cursor.getString(cursor.getColumnIndex(Contract.DiaryActivity.NAME)));
//            binding.activityCard.setOnLongClickListener((view) -> {
//                Intent i = new Intent(EventDetailActivity.this, EditActivity.class);
//                i.putExtra("activityID", actId);
//                startActivity(i);
//                return true;
//            });
            binding.row.background.setBackgroundColor(cursor.getInt(cursor.getColumnIndex(Contract.DiaryActivity.COLOR)));

            if(diaryEntryID == -1){
                diaryEntryID = cursor.getLong(cursor.getColumnIndex(Contract.Diary._ID));
            }
            overrideUpdates();
        }
    }

    public void doUpdateTokens(int token) {
        if(token == UPDATE_ENTRY){
            mUpdatePending[UPDATE_ENTRY] = false;
        }
        if(token == UPDATE_SUCC){
            mUpdatePending[UPDATE_SUCC] = false;
        }
        if(token == UPDATE_PRE){
            mUpdatePending[UPDATE_PRE] = false;
        }
        int i;
        for(i = 0; i < mUpdatePending.length; i++){
            if(mUpdatePending[i]){
                break;
            }
        }
        if(i >= mUpdatePending.length) {
            if(mIsCurrent) {
                ActivityHelper.helper.readCurrentActivity();
            }
            finish();
        }
    }

    private static class QHandler extends AsyncQueryHandler {
        final EventDetailActivity act;

        private QHandler(EventDetailActivity act){
            super(MVApplication.getAppContext().getContentResolver());
            this.act = new WeakReference<>(act).get();
        }

        @SuppressLint("Range")
        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            if (cursor != null) {
                if(token == READ_ALL) act.doTokenReadAll(cursor);
                cursor.close();
            }
        }

        @Override
        protected void onUpdateComplete(int token, Object cookie, int result) {
            super.onUpdateComplete(token, cookie, result);
            act.doUpdateTokens(token);
        }
    }

    // override the UI by the values in updateValues
    private void overrideUpdates() {
        if(updateValues.containsKey(Contract.Diary.NOTE)) {
            binding.editActivityNote.setText((CharSequence) updateValues.get(Contract.Diary.NOTE));
        }
        if(updateValues.containsKey(Contract.Diary.START)) {
            start.setTimeInMillis(updateValues.getAsLong(Contract.Diary.START));
        }
        if(updateValues.containsKey(Contract.Diary.END)) {
            end.setTimeInMillis(updateValues.getAsLong(Contract.Diary.END));
        }
        updateDateTimes();
    }

    private void updateDateTimes() {
        binding.dateStart.setText(DateFormat.format(dateFormatString, start));
        binding.timeStart.setText(DateFormat.format(timeFormatString, start));
        binding.dateEnd.setText(DateFormat.format(dateFormatString, end));
        binding.timeEnd.setText(DateFormat.format(timeFormatString, end));
        checkConstraints();
    }

    private final QHandler mQHandler = new QHandler(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_event_detail_content);
        setContent(binding.getRoot());
        binding.setActivity(this);

        Intent i = getIntent();
        diaryEntryID = i.getIntExtra("diaryEntryID", -1);

        binding.editActivityNote.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // empty
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // empty
            }

            @Override
            public void afterTextChanged(Editable s) {
                String ss = s.toString();
                updateValues.put(Contract.Diary.NOTE, ss);
            }
        });

        start = Calendar.getInstance();
        end = Calendar.getInstance();

        RecyclerView.LayoutManager layoutMan = new StaggeredGridLayoutManager(3, StaggeredGridLayoutManager.VERTICAL);
        binding.pictureRecycler.setLayoutManager(layoutMan);
        detailAdapter = new DetailRecyclerViewAdapter(this,null);
        binding.pictureRecycler.setAdapter(detailAdapter);

        LoaderManager.getInstance(this).restartLoader(0, null, this);

        if(savedInstanceState != null) {
            updateValues = savedInstanceState.getParcelable(UPDATE_VALUE_KEY);
            diaryEntryID = savedInstanceState.getLong(DIRAY_ENTRY_ID_KEY);
            binding.adjustAdjacent.setChecked(savedInstanceState.getBoolean(ADJUST_ADJACENT_KEY));
            overrideUpdates();
        }
        Arrays.fill(mUpdatePending, false);
        if(diaryEntryID == -1) {
            mQHandler.startQuery(READ_ALL,
                    null,
                    Contract.Diary.CONTENT_URI,
                    ENTRY_PROJ,
                    Contract.Diary.TABLE_NAME + "." + Contract.Diary._ID
                            + " = (SELECT MAX(" + Contract.Diary._ID + ") FROM " + Contract.Diary.TABLE_NAME + ")",
                    null,
                    null);
        }else {
            mQHandler.startQuery(READ_ALL,
                    null,
                    Contract.Diary.CONTENT_URI,
                    ENTRY_PROJ,
                    Contract.Diary.TABLE_NAME + "." + Contract.Diary._ID + "=?",
                    new String[]{Long.toString(diaryEntryID)},
                    null);
        }

        mDrawerToggle.setDrawerIndicatorEnabled(false);
        Objects.requireNonNull(getSupportActionBar()).setHomeAsUpIndicator(R.drawable.ic_close_cancel);
    }

    @Override
    public void onResume(){
        mNavigationView.getMenu().findItem(R.id.nav_diary).setChecked(true);
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(ADJUST_ADJACENT_KEY, binding.adjustAdjacent.isChecked());
        outState.putLong(DIRAY_ENTRY_ID_KEY, diaryEntryID);
        outState.putParcelable(UPDATE_VALUE_KEY, updateValues);
        // call superclass to save any view hierarchy
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.edit_diary_entry_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int mid = item.getItemId();
//        if (mid == R.id.action_edit_delete) {
//            /* TODO: DELETE diary entry */
//            Toast.makeText(this, R.string.delete_event_entry_msg, Toast.LENGTH_SHORT).show();
//            finish();
//        } else
        if (mid == android.R.id.home) {
            /* cancel edit */
            finish();
        } else if (mid == R.id.action_edit_done) {
            /* finish edit and save */
            if(checkConstraints()) {
                if (updateValues.size() > 0) {
                    mQHandler.startUpdate(UPDATE_ENTRY, null,
                            ContentUris.withAppendedId(Contract.Diary.CONTENT_URI, diaryEntryID),
                            updateValues, null, null);
                    mUpdatePending[UPDATE_ENTRY] = true;

                    if (binding.adjustAdjacent.isChecked()) {
                        if (updateValues.containsKey(Contract.Diary.START)) {
                            // update also the predecessor
                            ContentValues updateEndTime = new ContentValues();
                            updateEndTime.put(Contract.Diary.END, updateValues.getAsString(Contract.Diary.START));
                            mQHandler.startUpdate(UPDATE_PRE, null,
                                    Contract.Diary.CONTENT_URI,
                                    updateEndTime,
                                    Contract.Diary.END + "=?",
                                    new String[]{Long.toString(storedStart.getTimeInMillis())});
                            mUpdatePending[UPDATE_PRE] = true;

                        }
                        if (updateValues.containsKey(Contract.Diary.END)) {
                            // update also the successor
                            ContentValues updateStartTime = new ContentValues();
                            updateStartTime.put(Contract.Diary.START, updateValues.getAsString(Contract.Diary.END));
                            mQHandler.startUpdate(UPDATE_SUCC, null,
                                    Contract.Diary.CONTENT_URI,
                                    updateStartTime,
                                    Contract.Diary.START + "=?",
                                    new String[]{Long.toString(storedEnd.getTimeInMillis())});
                            mUpdatePending[UPDATE_SUCC] = true;
                        }
                    }
                } else finish();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean checkConstraints(){
        boolean result = true;
        if(end.getTimeInMillis() != 0 && !end.after(start)){
            result = false;
            binding.timeError.setText(R.string.constraint_positive_duration);
        }

        checkForOverlap();
        // TODO
        // end >= start + 1000
        // no overlap OR adjust adjacent (but still no oerlap with the next next and last last

        if(!result) {
            // TODO: make animation here, and do so only if it is not already visibile
            binding.timeError.setVisibility(View.VISIBLE);
        }else{
            binding.timeError.setVisibility(View.GONE);
        }
        return result;
    }

    private void checkForOverlap() {
/*        mQHandler.startQuery(OVERLAP_CHECK,
                null,
                Contract.Diary.CONTENT_URI,
                new String[]{
                        Contract.Diary._ID
                },
                Contract.Diary.TABLE_NAME + "." + Contract.Diary._ID + "=?",
                new String[]{Long.toString(start.getTimeInMillis()), Long.toString(end.getTimeInMillis())},
                null);
                */
    }

    /**
     * Instantiate and return a new Loader for the given ID.
     *
     * @param id   The ID whose loader is to be created.
     * @param args Any arguments supplied by the caller.
     * @return Return a new Loader instance that is ready to start loading.
     */
    // Called when a new Loader needs to be created
    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return new CursorLoader(this, Contract.DiaryImage.CONTENT_URI,
                PROJECTION_IMG,
                Contract.DiaryImage.TABLE_NAME + "." + Contract.DiaryImage.DIARY_ID + "=? AND "
                        + Contract.DiaryImage._DELETED + "=0",
                new String[]{Long.toString(diaryEntryID)},
                Contract.DiaryImage.SORT_ORDER_DEFAULT);
    }

    // Called when a previously created loader has finished loading
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
        // Swap the new cursor in
        detailAdapter.swapCursor(data);
    }

    // Called when a previously created loader is reset, making the data unavailable
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed.  We need to make sure we are no
        // longer using it.
        detailAdapter.swapCursor(null);
    }

    public void showStartTimePickerDialog(View v) {
        TimePickerFragment newFragment = new TimePickerFragment();
        newFragment.setData((view, hourOfDay, minute) -> {
            start.set(Calendar.HOUR_OF_DAY, hourOfDay);
            start.set(Calendar.MINUTE, minute);
            start.set(Calendar.SECOND, 0);
            start.set(Calendar.MILLISECOND, 0);

            Long newStart = start.getTimeInMillis();
            updateValues.put(Contract.Diary.START, newStart);
            updateDateTimes();
        }
                , start.get(Calendar.HOUR_OF_DAY), start.get(Calendar.MINUTE));
        newFragment.show(getSupportFragmentManager(), "startTimePicker");
    }

    public void showEndTimePickerDialog(View v) {
        TimePickerFragment newFragment = new TimePickerFragment();
        newFragment.setData((view, hourOfDay, minute) -> {
            end.set(Calendar.HOUR_OF_DAY, hourOfDay);
            end.set(Calendar.MINUTE, minute);
            end.set(Calendar.SECOND, 0);
            end.set(Calendar.MILLISECOND, 0);

            Long newEnd = end.getTimeInMillis();
            updateValues.put(Contract.Diary.END, newEnd);
            updateDateTimes();
        }
                , end.get(Calendar.HOUR_OF_DAY), end.get(Calendar.MINUTE));
        newFragment.show(getSupportFragmentManager(), "endTimePicker");
    }

    public void showStartDatePickerDialog(View v) {
        DatePickerFragment newFragment = new DatePickerFragment();
        newFragment.setData((view, year, month, dayOfMonth) -> {
                    start.set(Calendar.YEAR, year);
                    start.set(Calendar.MONTH, month);
                    start.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    Long newStart = start.getTimeInMillis();
                    updateValues.put(Contract.Diary.START, newStart);
                    updateDateTimes();
                }
                , start.get(Calendar.YEAR), start.get(Calendar.MONTH), start.get(Calendar.DAY_OF_MONTH));
        newFragment.show(getSupportFragmentManager(), "startDatePicker");
    }

    public void showEndDatePickerDialog(View v) {
        DatePickerFragment newFragment = new DatePickerFragment();
        newFragment.setData((view, year, month, dayOfMonth) -> {
            end.set(Calendar.YEAR, year);
            end.set(Calendar.MONTH, month);
            end.set(Calendar.DAY_OF_MONTH, dayOfMonth);

            Long newEnd = end.getTimeInMillis();
            updateValues.put(Contract.Diary.END, newEnd);
            updateDateTimes();
        }
                , end.get(Calendar.YEAR), end.get(Calendar.MONTH), end.get(Calendar.DAY_OF_MONTH));
        newFragment.show(getSupportFragmentManager(), "endDatePicker");
    }
}
