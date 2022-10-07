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
package com.mdiqentw.lifedots.ui.history

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.DatePickerDialog.OnDateSetListener
import android.app.Dialog
import android.app.TimePickerDialog
import android.app.TimePickerDialog.OnTimeSetListener
import android.content.AsyncQueryHandler
import android.content.ContentUris
import android.content.ContentValues
import android.database.Cursor
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateFormat
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.DatePicker
import android.widget.TimePicker
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.mdiqentw.lifedots.MVApplication
import com.mdiqentw.lifedots.R
import com.mdiqentw.lifedots.databinding.ActivityEventDetailContentBinding
import com.mdiqentw.lifedots.db.Contract
import com.mdiqentw.lifedots.helpers.ActivityHelper
import com.mdiqentw.lifedots.ui.generic.BaseActivity
import com.mdiqentw.lifedots.ui.generic.DetailRecyclerViewAdapter
import java.lang.ref.WeakReference
import java.util.*

/*
 * HistoryDetailActivity to show details of and modify diary entries
 *
 * */
class EventDetailActivity : BaseActivity(), LoaderManager.LoaderCallbacks<Cursor> {
    lateinit var binding: ActivityEventDetailContentBinding
    private var detailAdapter: DetailRecyclerViewAdapter? = null
    private val mUpdatePending = BooleanArray(UPDATE_SUCC + 1)

    //    private static final int OVERLAP_CHECK = 5;
    private val ENTRY_PROJ = arrayOf(
        Contract.DiaryActivity.TABLE_NAME + "." + Contract.DiaryActivity.NAME,
        Contract.DiaryActivity.TABLE_NAME + "." + Contract.DiaryActivity.COLOR,
        Contract.Diary.TABLE_NAME + "." + Contract.Diary.ACT_ID,
        Contract.Diary.TABLE_NAME + "." + Contract.Diary._ID,
        Contract.Diary.NOTE,
        Contract.Diary.START,
        Contract.Diary.END
    )
    val dateFormatString = MVApplication.appContext!!.resources.getString(R.string.date_format)
    val timeFormatString = MVApplication.appContext!!.resources.getString(R.string.time_format)

    //    private final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(MVApplication.getAppContext());
    /* the id of the currently displayed diary entry */
    private var diaryEntryID: Long = 0

    //    private int actId;
    private var start: Calendar? = null
    private var storedStart: Calendar? = null
    private var end: Calendar? = null
    private var storedEnd: Calendar? = null
    private var updateValues: ContentValues? = ContentValues()
    private var mIsCurrent = false

    class TimePickerFragment : DialogFragment() {
        private var hour = 0
        private var minute = 0
        private var listener: OnTimeSetListener? = null
        fun setData(
            listener: OnTimeSetListener?,
            hour: Int, minute: Int
        ) {
            this.hour = hour
            this.minute = minute
            this.listener = listener
        }

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            // Use the current time as the default values for the picker
            // Create a new instance of TimePickerDialog and return it
            return TimePickerDialog(
                activity, listener, hour, minute,
                DateFormat.is24HourFormat(activity)
            )
        }
    }

    class DatePickerFragment : DialogFragment() {
        private var year = 0
        private var month = 0
        private var day = 0
        private var listener: OnDateSetListener? = null
        fun setData(
            listener: OnDateSetListener?,
            year: Int, mount: Int, day: Int
        ) {
            this.year = year
            month = mount
            this.day = day
            this.listener = listener
        }

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            // Use the current time as the default values for the picker
            // Create a new instance of TimePickerDialog and return it
            return DatePickerDialog(requireActivity(), listener, year, month, day)
        }
    }

    @SuppressLint("Range")
    fun doTokenReadAll(cursor: Cursor) {
        if (cursor.moveToFirst()) {
            start = Calendar.getInstance()
            storedStart = Calendar.getInstance()
            start!!.timeInMillis = cursor.getLong(cursor.getColumnIndex(Contract.Diary.START))
            storedStart!!.timeInMillis = cursor.getLong(cursor.getColumnIndex(Contract.Diary.START))
            end = Calendar.getInstance()
            storedEnd = Calendar.getInstance()
            val endMillis = cursor.getLong(cursor.getColumnIndex(Contract.Diary.END))
            storedEnd!!.timeInMillis = endMillis
            mIsCurrent = if (endMillis != 0L) {
                end!!.timeInMillis = endMillis
                false
            } else {
                true
            }
            if (!updateValues!!.containsKey(Contract.Diary.NOTE)) {
                binding.editActivityNote.setText(cursor.getString(cursor.getColumnIndex(Contract.Diary.NOTE)))
            }
            //            actId = cursor.getInt(cursor.getColumnIndex(Contract.Diary.ACT_ID));
//            System.out.println("Contract.DiaryActivity: " + cursor.getString(cursor.getColumnIndex(Contract.DiaryActivity.NAME)) +
//                    " " + actId);
            binding.row.name.text =
                cursor.getString(cursor.getColumnIndex(Contract.DiaryActivity.NAME))
            //            binding.activityCard.setOnLongClickListener((view) -> {
//                Intent i = new Intent(EventDetailActivity.this, EditActivity.class);
//                i.putExtra("activityID", actId);
//                startActivity(i);
//                return true;
//            });
            binding.row.background.setBackgroundColor(cursor.getInt(cursor.getColumnIndex(Contract.DiaryActivity.COLOR)))
            if (diaryEntryID == -1L) {
                diaryEntryID = cursor.getLong(cursor.getColumnIndex(Contract.Diary._ID))
            }
            overrideUpdates()
        }
    }

    fun doUpdateTokens(token: Int) {
        if (token == UPDATE_ENTRY) {
            mUpdatePending[UPDATE_ENTRY] = false
        }
        if (token == UPDATE_SUCC) {
            mUpdatePending[UPDATE_SUCC] = false
        }
        if (token == UPDATE_PRE) {
            mUpdatePending[UPDATE_PRE] = false
        }
        var i = 0
        while (i < mUpdatePending.size) {
            if (mUpdatePending[i]) {
                break
            }
            i++
        }
        if (i >= mUpdatePending.size) {
            if (mIsCurrent) {
                ActivityHelper.helper.readCurrentActivity()
            }
            finish()
        }
    }

    private class QHandler(act: EventDetailActivity) :
        AsyncQueryHandler(MVApplication.appContext!!.contentResolver) {
        val act: EventDetailActivity?

        init {
            this.act = WeakReference(act).get()
        }

        @SuppressLint("Range")
        override fun onQueryComplete(token: Int, cookie: Any?, cursor: Cursor) {
            if (token == READ_ALL) act!!.doTokenReadAll(cursor)
            cursor.close()
        }

        override fun onUpdateComplete(token: Int, cookie: Any?, result: Int) {
            super.onUpdateComplete(token, cookie, result)
            act!!.doUpdateTokens(token)
        }
    }

    // override the UI by the values in updateValues
    private fun overrideUpdates() {
        if (updateValues!!.containsKey(Contract.Diary.NOTE)) {
            binding.editActivityNote.setText(updateValues!![Contract.Diary.NOTE] as CharSequence)
        }
        if (updateValues!!.containsKey(Contract.Diary.START)) {
            start!!.timeInMillis = updateValues!!.getAsLong(Contract.Diary.START)
        }
        if (updateValues!!.containsKey(Contract.Diary.END)) {
            end!!.timeInMillis = updateValues!!.getAsLong(Contract.Diary.END)
        }
        updateDateTimes()
    }

    private fun updateDateTimes() {
        binding.dateStart.text = DateFormat.format(dateFormatString, start)
        binding.timeStart.text = DateFormat.format(timeFormatString, start)
        binding.dateEnd.text = DateFormat.format(dateFormatString, end)
        binding.timeEnd.text = DateFormat.format(timeFormatString, end)
        checkConstraints()
    }

    private val mQHandler = QHandler(this)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_event_detail_content)
        setContent(binding.root)
        binding.activity = this
        val i = intent
        diaryEntryID = i.getIntExtra("diaryEntryID", -1).toLong()
        binding.editActivityNote.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                // empty
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                // empty
            }

            override fun afterTextChanged(s: Editable) {
                val ss = s.toString()
                updateValues!!.put(Contract.Diary.NOTE, ss)
            }
        })
        start = Calendar.getInstance()
        end = Calendar.getInstance()
        val layoutMan: RecyclerView.LayoutManager = StaggeredGridLayoutManager(3, StaggeredGridLayoutManager.VERTICAL)
        binding.pictureRecycler.layoutManager = layoutMan
        detailAdapter = DetailRecyclerViewAdapter(this, null)
        binding.pictureRecycler.adapter = detailAdapter
        LoaderManager.getInstance(this).restartLoader(0, null, this)
        if (savedInstanceState != null) {
            updateValues = savedInstanceState.getParcelable(UPDATE_VALUE_KEY)
            diaryEntryID = savedInstanceState.getLong(DIRAY_ENTRY_ID_KEY)
            binding.adjustAdjacent.isChecked = savedInstanceState.getBoolean(ADJUST_ADJACENT_KEY)
            overrideUpdates()
        }
        Arrays.fill(mUpdatePending, false)
        if (diaryEntryID == -1L) {
            mQHandler.startQuery(
                READ_ALL,
                null,
                Contract.Diary.CONTENT_URI,
                ENTRY_PROJ,
                Contract.Diary.TABLE_NAME + "." + Contract.Diary._ID
                        + " = (SELECT MAX(" + Contract.Diary._ID + ") FROM " + Contract.Diary.TABLE_NAME + ")",
                null,
                null
            )
        } else {
            mQHandler.startQuery(
                READ_ALL,
                null,
                Contract.Diary.CONTENT_URI,
                ENTRY_PROJ,
                Contract.Diary.TABLE_NAME + "." + Contract.Diary._ID + "=?",
                arrayOf(diaryEntryID.toString()),
                null
            )
        }
        mDrawerToggle.isDrawerIndicatorEnabled = false
        supportActionBar!!.setHomeAsUpIndicator(R.drawable.ic_close_cancel)
    }

    public override fun onResume() {
        mNavigationView.menu.findItem(R.id.nav_diary).isChecked = true
        super.onResume()
    }

    public override fun onPause() {
        super.onPause()
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(ADJUST_ADJACENT_KEY, binding.adjustAdjacent.isChecked)
        outState.putLong(DIRAY_ENTRY_ID_KEY, diaryEntryID)
        outState.putParcelable(UPDATE_VALUE_KEY, updateValues)
        // call superclass to save any view hierarchy
        super.onSaveInstanceState(outState)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.edit_diary_entry_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val mid = item.itemId
        //        if (mid == R.id.action_edit_delete) {
//            /* TODO: DELETE diary entry */
//            Toast.makeText(this, R.string.delete_event_entry_msg, Toast.LENGTH_SHORT).show();
//            finish();
//        } else
        if (mid == android.R.id.home) {
            /* cancel edit */
            finish()
        } else if (mid == R.id.action_edit_done) {
            /* finish edit and save */
            if (checkConstraints()) {
                if (updateValues!!.size() > 0) {
                    mQHandler.startUpdate(
                        UPDATE_ENTRY, null,
                        ContentUris.withAppendedId(Contract.Diary.CONTENT_URI, diaryEntryID),
                        updateValues, null, null
                    )
                    mUpdatePending[UPDATE_ENTRY] = true
                    if (binding.adjustAdjacent.isChecked) {
                        if (updateValues!!.containsKey(Contract.Diary.START)) {
                            // update also the predecessor
                            val updateEndTime = ContentValues()
                            updateEndTime.put(Contract.Diary.END, updateValues!!.getAsString(Contract.Diary.START))
                            mQHandler.startUpdate(
                                UPDATE_PRE, null,
                                Contract.Diary.CONTENT_URI,
                                updateEndTime,
                                Contract.Diary.END + "=?", arrayOf(
                                    storedStart!!.timeInMillis.toString()
                                )
                            )
                            mUpdatePending[UPDATE_PRE] = true
                        }
                        if (updateValues!!.containsKey(Contract.Diary.END)) {
                            // update also the successor
                            val updateStartTime = ContentValues()
                            updateStartTime.put(Contract.Diary.START, updateValues!!.getAsString(Contract.Diary.END))
                            mQHandler.startUpdate(
                                UPDATE_SUCC, null,
                                Contract.Diary.CONTENT_URI,
                                updateStartTime,
                                Contract.Diary.START + "=?", arrayOf(
                                    storedEnd!!.timeInMillis.toString()
                                )
                            )
                            mUpdatePending[UPDATE_SUCC] = true
                        }
                    }
                } else finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun checkConstraints(): Boolean {
        var result = true
        if (end!!.timeInMillis != 0L && !end!!.after(start)) {
            result = false
            binding.timeError.setText(R.string.constraint_positive_duration)
        }
        checkForOverlap()
        // TODO
        // end >= start + 1000
        // no overlap OR adjust adjacent (but still no oerlap with the next next and last last
        if (!result) {
            // TODO: make animation here, and do so only if it is not already visibile
            binding.timeError.visibility = View.VISIBLE
        } else {
            binding.timeError.visibility = View.GONE
        }
        return result
    }

    private fun checkForOverlap() {
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
    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return CursorLoader(
            this, Contract.DiaryImage.CONTENT_URI,
            PROJECTION_IMG,
            Contract.DiaryImage.TABLE_NAME + "." + Contract.DiaryImage.DIARY_ID + "=? AND "
                    + Contract.DiaryImage._DELETED + "=0", arrayOf(diaryEntryID.toString()),
            Contract.DiaryImage.SORT_ORDER_DEFAULT
        )
    }

    // Called when a previously created loader has finished loading
    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor) {
        // Swap the new cursor in
        detailAdapter!!.swapCursor(data)
    }

    // Called when a previously created loader is reset, making the data unavailable
    override fun onLoaderReset(loader: Loader<Cursor>) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed.  We need to make sure we are no
        // longer using it.
        detailAdapter!!.swapCursor(null)
    }

    fun showStartTimePickerDialog(v: View?) {
        val newFragment = TimePickerFragment()
        newFragment.setData(
            { _: TimePicker?, hourOfDay: Int, minute: Int ->
                start!![Calendar.HOUR_OF_DAY] = hourOfDay
                start!![Calendar.MINUTE] = minute
                start!![Calendar.SECOND] = 0
                start!![Calendar.MILLISECOND] = 0
                val newStart = start!!.timeInMillis
                updateValues!!.put(Contract.Diary.START, newStart)
                updateDateTimes()
            }, start!![Calendar.HOUR_OF_DAY], start!![Calendar.MINUTE])
        newFragment.show(supportFragmentManager, "startTimePicker")
    }

    fun showEndTimePickerDialog(v: View?) {
        val newFragment = TimePickerFragment()
        newFragment.setData(
            { _: TimePicker?, hourOfDay: Int, minute: Int ->
                end!![Calendar.HOUR_OF_DAY] = hourOfDay
                end!![Calendar.MINUTE] = minute
                end!![Calendar.SECOND] = 0
                end!![Calendar.MILLISECOND] = 0
                val newEnd = end!!.timeInMillis
                updateValues!!.put(Contract.Diary.END, newEnd)
                updateDateTimes()
            }, end!![Calendar.HOUR_OF_DAY], end!![Calendar.MINUTE])
        newFragment.show(supportFragmentManager, "endTimePicker")
    }

    fun showStartDatePickerDialog(v: View?) {
        val newFragment = DatePickerFragment()
        newFragment.setData(
            { _: DatePicker?, year: Int, month: Int, dayOfMonth: Int ->
                start!![Calendar.YEAR] = year
                start!![Calendar.MONTH] = month
                start!![Calendar.DAY_OF_MONTH] = dayOfMonth
                val newStart = start!!.timeInMillis
                updateValues!!.put(Contract.Diary.START, newStart)
                updateDateTimes()
            }, start!![Calendar.YEAR], start!![Calendar.MONTH], start!![Calendar.DAY_OF_MONTH])
        newFragment.show(supportFragmentManager, "startDatePicker")
    }

    fun showEndDatePickerDialog(v: View?) {
        val newFragment = DatePickerFragment()
        newFragment.setData(
            { _: DatePicker?, year: Int, month: Int, dayOfMonth: Int ->
                end!![Calendar.YEAR] = year
                end!![Calendar.MONTH] = month
                end!![Calendar.DAY_OF_MONTH] = dayOfMonth
                val newEnd = end!!.timeInMillis
                updateValues!!.put(Contract.Diary.END, newEnd)
                updateDateTimes()
            }, end!![Calendar.YEAR], end!![Calendar.MONTH], end!![Calendar.DAY_OF_MONTH])
        newFragment.show(supportFragmentManager, "endDatePicker")
    }

    companion object {
        private val PROJECTION_IMG = arrayOf(
            Contract.DiaryImage.URI,
            Contract.DiaryImage._ID
        )
        private const val READ_ALL = 1
        private const val UPDATE_ENTRY = 2
        private const val UPDATE_PRE = 3
        private const val UPDATE_SUCC = 4
        private const val DIRAY_ENTRY_ID_KEY = "ENTRY_ID"
        private const val UPDATE_VALUE_KEY = "UPDATE_VALUE"
        private const val ADJUST_ADJACENT_KEY = "ADJUST_ADJACENT"
    }
}