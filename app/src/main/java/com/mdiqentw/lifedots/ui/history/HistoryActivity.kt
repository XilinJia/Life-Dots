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
package com.mdiqentw.lifedots.ui.history

import android.app.SearchManager
import android.content.AsyncQueryHandler
import android.content.ContentValues
import android.content.Intent
import android.content.res.Configuration
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.text.format.DateFormat
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.DatePicker
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.util.Pair
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.datepicker.MaterialDatePicker
import com.mdiqentw.lifedots.MVApplication
import com.mdiqentw.lifedots.R
import com.mdiqentw.lifedots.databinding.ActivityHistoryContentBinding
import com.mdiqentw.lifedots.db.Contract
import com.mdiqentw.lifedots.db.LDContentProvider
import com.mdiqentw.lifedots.ui.generic.BaseActivity
import com.mdiqentw.lifedots.ui.generic.DetailRecyclerViewAdapter
import com.mdiqentw.lifedots.ui.history.EventDetailActivity.DatePickerFragment
import com.mdiqentw.lifedots.ui.main.NoteEditDialog
import com.mdiqentw.lifedots.ui.main.NoteEditDialog.NoteEditDialogListener
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

/*
 * Show the history of the Diary.
 * */
class HistoryActivity : BaseActivity(), LoaderManager.LoaderCallbacks<Cursor>, NoteEditDialogListener,
    HistoryRecyclerViewAdapter.SelectListener, MenuItem.OnMenuItemClickListener, SearchView.OnCloseListener,
    SearchView.OnQueryTextListener {
    lateinit var binding: ActivityHistoryContentBinding
    private var historyAdapter: HistoryRecyclerViewAdapter? = null
    private lateinit var detailAdapters: Array<DetailRecyclerViewAdapter?>
    private var searchView: SearchView? = null
    private var startTime: Long
    private var endTime: Long
    private var duration: Long
    val client = MVApplication.appContext!!.contentResolver.acquireContentProviderClient(Contract.AUTHORITY)
    val provider = client!!.localContentProvider as LDContentProvider?

    override fun onItemClick(viewHolder: HistoryViewHolders?, adapterPosition: Int, diaryID: Int) {
        val i = Intent(this, EventDetailActivity::class.java)
        i.putExtra("diaryEntryID", diaryID)
        startActivity(i)
    }

    override fun onItemLongClick(viewHolder: HistoryViewHolders?, adapterPosition: Int, diaryID: Int): Boolean {
        val dialog = NoteEditDialog()
        dialog.diaryId = diaryID.toLong()
        if (viewHolder != null) {
            val noteText = viewHolder.mNoteLabel.text
            if (noteText != null && noteText.isNotEmpty()) dialog.inputText = noteText.toString()
        }
        dialog.show(supportFragmentManager, "NoteEditDialogFragment")
        return true
    }

    /**
     * The user is attempting to close the SearchView.
     *
     * @return true if the listener wants to override the default behavior of clearing the
     * text field and dismissing it, false otherwise.
     */
    override fun onClose(): Boolean {
        filterHistoryView(null)
        return false
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        // handled via Intent
        return false
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (!searchView!!.isIconified) {
            searchView!!.isIconified = true
        } else {
            super.onBackPressed()
        }
    }

    /**
     * Called when the query text is changed by the user.
     *
     * @param newText the new content of the query text field.
     * @return false if the SearchView should perform the default action of showing any
     * suggestions if available, true if the action was handled by the listener.
     */
    override fun onQueryTextChange(newText: String): Boolean {
        // no dynamic change before starting the search...
        setDefaultColorSearchText()
        return true
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        val mid = item.itemId
        if (mid == R.id.menu_notes) {
            obtainHistoryNotes()
        } else if (mid == R.id.menu_dates) {
            val picker = MaterialDatePicker.Builder.dateRangePicker().build()
            picker.show(supportFragmentManager, picker.toString())
            picker.addOnPositiveButtonClickListener { selection: Pair<Long, Long> ->
                startTime = selection.first
                endTime = selection.second
                duration = endTime - startTime
                binding.hisRangeTextView.text = String.format("%d Days", duration / MS_Per_Day)
                obtainHistoryInPeriod()
            }
            //        } else if (item.getItemId() == R.id.menu_images) {
//            obtainHistoryImages();
        }
        return true
    }

    private class QHandler  /* Access only allowed via ActivityHelper.helper singleton */ :
        AsyncQueryHandler(MVApplication.appContext!!.contentResolver)

    private val mQHandler = QHandler()

    init {
        endTime = System.currentTimeMillis()
        startTime = endTime - MS_Per_Day
        duration = MS_Per_Day
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_history_content)
        setContent(binding.root)
        binding.activity = this
        val i = intent
        val tstart = i.getLongExtra("StartTime", 0L)
        val tend = i.getLongExtra("EndTime", 0L)
        if (tstart > 0L && tend > 0L) {
            startTime = tstart
            endTime = tend
        }
        detailAdapters = arrayOfNulls(5)
        val detailLayoutManager: StaggeredGridLayoutManager
        val hov: Int = if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) StaggeredGridLayoutManager.HORIZONTAL else StaggeredGridLayoutManager.VERTICAL
        detailLayoutManager = MyStaggeredGridLayoutManager(hov)

//        detailLayoutManager.setAutoMeasureEnabled(true);
        binding.historyList.layoutManager = detailLayoutManager
        historyAdapter = HistoryRecyclerViewAdapter(this@HistoryActivity, this, null)
        binding.historyList.adapter = historyAdapter
        binding.hisRangeTextView.text = String.format("%d Days", duration / MS_Per_Day)
        binding.hisImgEarlier.setOnClickListener { _: View? ->
            endTime = startTime
            startTime -= duration
            filterHistoryView(null)
        }
        binding.hisImgLater.setOnClickListener { _: View? ->
            startTime = endTime
            endTime += duration
            filterHistoryView(null)
        }

        // Prepare the loader.  Either re-connect with an existing one,
        // or start a new one.
        // and yes, for performance reasons it is good to do it the relational way and not with an OO design
        LoaderManager.getInstance(this).initLoader(LOADER_ID_HISTORY, null, this)
        mDrawerToggle.isDrawerIndicatorEnabled = false

        // Get the intent, verify the action and get the query
        handleIntent(intent)
    }

    fun showDatePickerDialog(v: View?) {
        val newFragment = DatePickerFragment()
        val date = Calendar.getInstance()
        date.timeInMillis = startTime + duration / 2
        newFragment.setData(
            { _: DatePicker?, year: Int, month: Int, dayOfMonth: Int ->
                date[Calendar.YEAR] = year
                date[Calendar.MONTH] = month
                date[Calendar.DAY_OF_MONTH] = dayOfMonth
                startTime = date.timeInMillis - duration / 2
                endTime = date.timeInMillis + duration / 2
                filterHistoryView(null)
            }, date[Calendar.YEAR], date[Calendar.MONTH], date[Calendar.DAY_OF_MONTH])
        newFragment.show(supportFragmentManager, "startDatePicker")
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        var query: String? = null
        var action = intent.action
        if (LDContentProvider.SEARCH_ACTIVITY == action) {
            query = intent.getStringExtra(SearchManager.QUERY)
            val data = intent.data
            if (data != null) {
                query = data.lastPathSegment
                if (query != null) {
                    val id = java.lang.Long.decode(query)
                    filterHistoryView(id)
                }
            }
        } else if (LDContentProvider.SEARCH_NOTE == action) {
            val data = intent.data
            if (data != null) {
                query = data.lastPathSegment
                filterHistoryNotes(query)
            }
        } else if (LDContentProvider.SEARCH_GLOBAL == action) {
            val data = intent.data
            if (data != null) {
                query = data.lastPathSegment
                filterHistoryView(query)
            }
        } else if (LDContentProvider.SEARCH_DATE == action) {
            val data = intent.data
            if (data != null) {
                query = data.path
                query = query!!.replaceFirst("/".toRegex(), "")
                filterHistoryDates(query)
            }
        } else if (Intent.ACTION_SEARCH == action) {
            query = intent.getStringExtra(SearchManager.QUERY)
            action = LDContentProvider.SEARCH_GLOBAL
            filterHistoryView(query)
        }
        /*
            if query was searched, then insert query into suggestion table
         */if (query != null) {
            val uri = Contract.DiarySearchSuggestion.CONTENT_URI
            val values = ContentValues()
            contentResolver.delete(
                uri,
                Contract.DiarySearchSuggestion.SUGGESTION + " LIKE ? AND "
                        + Contract.DiarySearchSuggestion.ACTION + " LIKE ?", arrayOf(query, action)
            )
            values.put(Contract.DiarySearchSuggestion.SUGGESTION, query)
            values.put(Contract.DiarySearchSuggestion.ACTION, action)
            contentResolver.insert(uri, values)
            contentResolver.delete(
                uri,
                Contract.DiarySearchSuggestion._ID +
                        " IN (SELECT " + Contract.DiarySearchSuggestion._ID +
                        " FROM " + Contract.DiarySearchSuggestion.TABLE_NAME +
                        " ORDER BY " + Contract.DiarySearchSuggestion._ID + " DESC LIMIT " + SEARCH_SUGGESTION_DISPLAY_COUNT + ",1)",
                null
            )
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.history_menu, menu)
        val notesMenuItem = menu.findItem(R.id.menu_notes)
        notesMenuItem.setOnMenuItemClickListener(this)
        val datesMenuItem = menu.findItem(R.id.menu_dates)
        datesMenuItem.setOnMenuItemClickListener(this)

//        MenuItem imagesMenuItem = menu.findItem(R.id.menu_images);
//        imagesMenuItem.setOnMenuItemClickListener(this);
//
        // Get the SearchView and set the searchable configuration
        val searchManager = getSystemService(SEARCH_SERVICE) as SearchManager
        val searchMenuItem = menu.findItem(R.id.action_filter)
        searchView = searchMenuItem.actionView as SearchView?
        searchView!!.setIconifiedByDefault(true)
        // Assumes current activity is the searchable activity
        searchView!!.setSearchableInfo(searchManager.getSearchableInfo(componentName))
        searchView!!.setOnCloseListener(this)
        searchView!!.setOnQueryTextListener(this)
        searchView!!.setOnSuggestionListener(object : SearchView.OnSuggestionListener {
            override fun onSuggestionSelect(position: Int): Boolean {
                return false
            }

            override fun onSuggestionClick(position: Int): Boolean {
                val selectedView = searchView!!.suggestionsAdapter
                val cursor = selectedView.getItem(position) as Cursor
                val index = cursor.getColumnIndexOrThrow(SearchManager.SUGGEST_COLUMN_QUERY)
                val q = cursor.getString(index)
                searchView!!.setQuery(q, false)
                return false // let super handle all the real search stuff
            }
        })
        searchView!!.imeOptions = searchView!!.imeOptions or EditorInfo.IME_ACTION_SEARCH
        //TODO to make it look nice
//        searchView.setSuggestionsAdapter(new ExampleAdapter(this, cursor, items));
        return true
    }

    /**
     * @param query the search string, if null resets the filter
     */
    private fun filterHistoryView(query: String?) {
        if (query == null) {
            LoaderManager.getInstance(this).restartLoader(LOADER_ID_HISTORY, null, this)
        } else {
            val args = Bundle()
            args.putInt("TYPE", SEARCH_TYPE_TEXT_ALL)
            args.putString("TEXT", query)
            LoaderManager.getInstance(this).restartLoader(LOADER_ID_HISTORY, args, this)
        }
    }

    /* show only activity with id activityId
     */
    private fun filterHistoryView(activityId: Long) {
        val args = Bundle()
        args.putInt("TYPE", SEARCH_TYPE_ACTIVITYID)
        args.putLong("ACTIVITY_ID", activityId)
        LoaderManager.getInstance(this).restartLoader(LOADER_ID_HISTORY, args, this)
    }

    /* show only activity that contains note
     */
    private fun filterHistoryNotes(notetext: String?) {
        val args = Bundle()
        args.putInt("TYPE", SEARCH_TYPE_NOTE)
        args.putString("TEXT", notetext)
        LoaderManager.getInstance(this).restartLoader(LOADER_ID_HISTORY, args, this)
    }

    private fun obtainHistoryNotes() {
        val args = Bundle()
        args.putInt("TYPE", OBTAIN_TYPE_NOTE)
        LoaderManager.getInstance(this).restartLoader(LOADER_ID_HISTORY, args, this)
    }

    private fun obtainHistoryInPeriod() {
        val args = Bundle()
        args.putInt("TYPE", OBTAIN_TYPE_PERIOD)
        args.putLong("START", startTime)
        args.putLong("END", endTime)
        LoaderManager.getInstance(this).restartLoader(LOADER_ID_HISTORY, args, this)
    }

    private fun obtainHistoryImages() {
        val args = Bundle()
        args.putInt("TYPE", OBTAIN_TYPE_IMAGE)
        LoaderManager.getInstance(this).restartLoader(LOADER_ID_HISTORY, args, this)
    }

    /* show only activities that match date
     */
    private fun filterHistoryDates(date: String) {
        val dateInMilis = checkDateFormatAndParse(date)
        if (dateInMilis != null) {
            val args = Bundle()
            args.putInt("TYPE", SEARCH_TYPE_DATE)
            args.putLong("MILLIS", dateInMilis)
            LoaderManager.getInstance(this).restartLoader(LOADER_ID_HISTORY, args, this)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle your other action bar items...
        if (item.itemId == R.id.action_map) {
            val map = Intent(this@HistoryActivity, MapActivity::class.java)
            map.putExtra("StartTime", startTime)
            map.putExtra("EndTime", endTime)
            startActivity(map)
        }
        return super.onOptionsItemSelected(item)
    }

    // Called when a new Loader needs to be created
    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return if (id == LOADER_ID_HISTORY) {
            var sel = SELECTION
            var sel_args: Array<String>? = null
            if (args != null) {
                when (args.getInt("TYPE")) {
                    SEARCH_TYPE_ACTIVITYID -> {
                        sel = sel + " AND " + Contract.Diary.ACT_ID + " = ?"
                        sel_args = arrayOf(args.getLong("ACTIVITY_ID").toString())
                    }

                    SEARCH_TYPE_NOTE -> {
                        sel = sel + " AND " + Contract.Diary.NOTE + " LIKE ?"
                        sel_args = arrayOf("%" + args.getString("TEXT") + "%")
                    }

                    OBTAIN_TYPE_NOTE -> {
                        sel = sel + " AND " + Contract.Diary.NOTE + " IS NOT NULL AND " +
                                Contract.Diary.NOTE + " != ''"
                        sel_args = null
                    }

                    OBTAIN_TYPE_PERIOD -> {
                        sel = (sel + " AND " + Contract.Diary.END + " >= " + args.getLong("START")
                                + " AND " + Contract.Diary.START + " <= " + args.getLong("END"))
                        sel_args = null
                    }

                    SEARCH_TYPE_TEXT_ALL -> {
                        sel = (sel + " AND (" + Contract.Diary.NOTE + " LIKE ?"
                                + " OR " + Contract.DiaryActivity.NAME + " LIKE ?)")
                        sel_args = arrayOf(
                            "%" + args.getString("TEXT") + "%",
                            "%" + args.getString("TEXT") + "%"
                        )
                    }

                    SEARCH_TYPE_DATE -> {
                        // TOOD: calling here this provider method is a bit strange...
                        val searchResultQuery = provider!!.searchDate(args.getLong("MILLIS"))
                        sel = "$sel AND $searchResultQuery"
                        sel_args = null
                    }

                    else -> {}
                }
            } else {
                sel = (sel + " AND " + Contract.Diary.END + " >= " + startTime
                        + " AND " + Contract.Diary.START + " <= " + endTime)
                sel_args = null
            }
            CursorLoader(
                this, Contract.Diary.CONTENT_URI,
                PROJECTION, sel, sel_args, null
            )
        } else {
            CursorLoader(
                this@HistoryActivity,
                Contract.DiaryImage.CONTENT_URI, arrayOf(
                    Contract.DiaryImage._ID,
                    Contract.DiaryImage.URI
                ),
                Contract.DiaryImage.DIARY_ID + "=? AND "
                        + Contract.DiaryImage._DELETED + "=0", arrayOf(
                    args!!.getLong("DiaryID").toString()
                ),
                null
            )
        }
    }

    // Called when a previously created loader has finished loading
    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor) {
        // Swap the new cursor in.  (The framework will take care of closing the
        // old cursor once we return.)
        val i = loader.id
        if (i == LOADER_ID_HISTORY) {
            historyAdapter!!.swapCursor(data)
        } else {
            detailAdapters[i]!!.swapCursor(data)
        }
    }

    // Called when a previously created loader is reset, making the data unavailable
    override fun onLoaderReset(loader: Loader<Cursor>) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed.  We need to make sure we are no
        // longer using it.
        val i = loader.id
        if (i == LOADER_ID_HISTORY) {
            historyAdapter!!.swapCursor(null)
        } else {
            detailAdapters[i]!!.swapCursor(null)
        }
    }

    override fun onNoteEditPositiveClick(str: String?, dialog: DialogFragment?) {
        /* update note */
        val dlg = dialog as NoteEditDialog?
        val values = ContentValues()
        values.put(Contract.Diary.NOTE, str)
        mQHandler.startUpdate(
            0,
            null,
            Uri.withAppendedPath(
                Contract.Diary.CONTENT_URI,
                dlg!!.diaryId.toString()
            ),
            values,
            null, null
        )
    }

    public override fun onResume() {
        mNavigationView.menu.findItem(R.id.nav_diary).isChecked = true
        super.onResume()
        historyAdapter!!.notifyDataSetChanged() /* redraw the complete recyclerview to take care of e.g. date format changes in teh preferences etc. #36 */
    }

    fun addDetailAdapter(diaryEntryId: Long, adapter: DetailRecyclerViewAdapter) {
        /* ensure size of detailsAdapters */
        if (detailAdapters.size <= adapter.adapterId) {
            val newArray = arrayOfNulls<DetailRecyclerViewAdapter>(adapter.adapterId + 4)
            for ((i, a) in detailAdapters.withIndex()) {
                newArray[i] = a
            }
            detailAdapters = newArray
        }
        val b = Bundle()
        b.putLong("DiaryID", diaryEntryId)
        b.putInt("DetailAdapterID", adapter.adapterId)
        detailAdapters[adapter.adapterId] = adapter
        LoaderManager.getInstance(this).initLoader(adapter.adapterId, b, this)
    }

    /** Checks date format and also checks date can be parsed (used for not existing dates like 35.13.2000)
     * (in case format not exists or date is incorrect Toast about wrong format is displayed)
     * @param date input that is checked
     * @return millis of parsed input
     */
    private fun checkDateFormatAndParse(date: String): Long? {
        // TODO: generalize data format for search
        val formats = arrayOf(
            resources.getString(R.string.date_format),  //get default format from strings.xml
            (DateFormat.getDateFormat(applicationContext) as SimpleDateFormat).toLocalizedPattern() //locale format
        )
        var simpleDateFormat: SimpleDateFormat
        for (format in formats) {
            simpleDateFormat = SimpleDateFormat(format)
            simpleDateFormat.isLenient = false
            try {
                return Objects.requireNonNull(simpleDateFormat.parse(date)).time
            } catch (e: ParseException) {
                /* intentionally no further handling. We try the next date format and onyl if we cannot parse the date with any
                 * supported format we return null afterwards. */
            }
        }
        setWrongColorSearchText()
        Toast.makeText(application.baseContext, resources.getString(R.string.wrongFormat), Toast.LENGTH_LONG).show()
        return null
    }

    /**
     * Sets searched text to default color (white) in case it is set to red
     */
    private fun setDefaultColorSearchText() {
//        TextView textView =  searchView.findViewById(androidx.appcompat.R.id.search_src_text);
//        if (textView.getCurrentTextColor() == ContextCompat.getColor(MVApplication.getAppContext(), R.color.colorWrongText))
//            textView.setTextColor(ContextCompat.getColor(MVApplication.getAppContext(), R.color.activityTextColorLight));
    }

    /**
     * Sets searched text to color which indicates wrong searching (red)
     */
    private fun setWrongColorSearchText() {
//        TextView textView =  searchView.findViewById(androidx.appcompat.R.id.search_src_text);
//        textView.setTextColor(ContextCompat.getColor(MVApplication.getAppContext(), R.color.colorWrongText));
    }

    private class MyStaggeredGridLayoutManager(hov: Int) : StaggeredGridLayoutManager(1, hov) {
        override fun isAutoMeasureEnabled(): Boolean {
            return true
        }
    }

    companion object {
        private val PROJECTION = arrayOf(
            Contract.Diary.TABLE_NAME + "." + Contract.Diary._ID,
            Contract.Diary.ACT_ID,
            Contract.Diary.START,
            Contract.Diary.END,
            Contract.Diary.NOTE,
            Contract.DiaryActivity.NAME,
            Contract.DiaryActivity.COLOR
        )
        private const val SELECTION = Contract.Diary.TABLE_NAME + "." + Contract.Diary._DELETED + "=0"
        private const val SEARCH_SUGGESTION_DISPLAY_COUNT = 5
        private const val LOADER_ID_HISTORY = -1
        private const val SEARCH_TYPE_ACTIVITYID = 1
        private const val SEARCH_TYPE_NOTE = 2
        private const val SEARCH_TYPE_TEXT_ALL = 3
        private const val SEARCH_TYPE_DATE = 4
        private const val OBTAIN_TYPE_NOTE = 5
        private const val OBTAIN_TYPE_IMAGE = 6
        private const val OBTAIN_TYPE_PERIOD = 7
        const val MS_Per_Day = (1000 * 60 * 60 * 24).toLong()
    }
}