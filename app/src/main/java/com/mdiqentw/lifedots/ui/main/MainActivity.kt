/*
 * LifeDots
 *
 * Copyright (C) 2017-2018 Raphael Mack http://www.raphael-mack.de
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
package com.mdiqentw.lifedots.ui.main

import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.AsyncQueryHandler
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.view.View.OnLongClickListener
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import androidx.viewpager.widget.ViewPager
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.mdiqentw.lifedots.BuildConfig
import com.mdiqentw.lifedots.R
import com.mdiqentw.lifedots.databinding.ActivityMainContentBinding
import com.mdiqentw.lifedots.db.Contract
import com.mdiqentw.lifedots.helpers.ActivityHelper
import com.mdiqentw.lifedots.helpers.ActivityHelper.DataChangedListener
import com.mdiqentw.lifedots.helpers.DateHelper
import com.mdiqentw.lifedots.helpers.DateHelper.dateFormat
import com.mdiqentw.lifedots.helpers.GraphicsHelper.compressAndSaveImage
import com.mdiqentw.lifedots.helpers.GraphicsHelper.createImageFile
import com.mdiqentw.lifedots.helpers.GraphicsHelper.textColorOnBackground
import com.mdiqentw.lifedots.helpers.TimeSpanFormatter.format
import com.mdiqentw.lifedots.model.DetailViewModel
import com.mdiqentw.lifedots.model.DiaryActivity
import com.mdiqentw.lifedots.ui.generic.BaseActivity
import com.mdiqentw.lifedots.ui.generic.EditActivity
import com.mdiqentw.lifedots.ui.history.EventDetailActivity
import com.mdiqentw.lifedots.ui.main.NoteEditDialog.NoteEditDialogListener
import com.mdiqentw.lifedots.ui.settings.SettingsActivity
import java.io.File
import java.util.*

/*
 * MainActivity to show most of the UI, based on switching the fragements
 *
 * */
class MainActivity : BaseActivity(), SelectRecyclerViewAdapter.SelectListener, DataChangedListener,
    NoteEditDialogListener, OnLongClickListener, SearchView.OnQueryTextListener, SearchView.OnCloseListener {
    lateinit var binding: ActivityMainContentBinding
    private var viewModel: DetailViewModel? = null
    private var mCurrentPhotoPath: String? = null
    var layoutManager: FlexboxLayoutManager? = null
    private var selectAdapter: SelectRecyclerViewAdapter? = null
    private var filter: String? = ""
    private var searchView: SearchView? = null
    private fun setSearchMode(searchMode: Boolean) {
        if (searchMode) {
            binding.headerArea.visibility = View.GONE
            binding.fabAttachPicture.hide()
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        } else {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
            binding.headerArea.visibility = View.VISIBLE
            binding.fabAttachPicture.show()
        }
    }

    private var mQHandler: MainAsyncQueryHandler? = null
    public override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("currentPhotoPath", mCurrentPhotoPath)

        // call superclass to save any view hierarchy
        super.onSaveInstanceState(outState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main_content)
        setContent(binding.root)
        //        initNavigation();
        viewModel = ViewModelProvider(this)[DetailViewModel::class.java]
        mQHandler = MainAsyncQueryHandler(applicationContext.contentResolver, viewModel)

        // recovering the instance state
        if (savedInstanceState != null) {
            mCurrentPhotoPath = savedInstanceState.getString("currentPhotoPath")
        }
        setupViewPager(binding.viewpager)
        binding.tablayout.setupWithViewPager(binding.viewpager)
        binding.row.background.setOnLongClickListener(this)
        binding.row.background.setOnClickListener { _: View? ->
            if (PreferenceManager
                    .getDefaultSharedPreferences(applicationContext)
                    .getBoolean(SettingsActivity.KEY_PREF_DISABLE_CURRENT, true)
            ) {
                ActivityHelper.helper.currentActivity = null
            } else {
                val i = Intent(this@MainActivity, EventDetailActivity::class.java)
                // no diaryEntryID will edit the last one
                startActivity(i)
            }
        }
        val value = TypedValue()
        theme.resolveAttribute(android.R.attr.listPreferredItemHeightSmall, value, true)
        layoutManager = FlexboxLayoutManager(this)
        layoutManager!!.flexDirection = FlexDirection.ROW
        layoutManager!!.justifyContent = JustifyContent.FLEX_START
        binding.selectRecycler.layoutManager = layoutManager
        supportActionBar!!.subtitle = resources.getString(R.string.activity_subtitle_main)
        likelyhoodSort()
        binding.fabAttachPicture.setOnClickListener { _: View? ->
            // Handle the click on the FAB
            if (viewModel!!.currentActivity() != null && viewModel!!.currentActivity().value != null) {
                val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                if (takePictureIntent.resolveActivity(packageManager) != null) {
                    val photoFile = createImageFile()
                    Log.i(TAG, "create file for image capture " + photoFile.absolutePath)

                    // Continue only if the File was successfully created
                    // Save a file: path for use with ACTION_VIEW intents
                    mCurrentPhotoPath = photoFile.absolutePath
                    val photoURI = FileProvider.getUriForFile(
                        this@MainActivity,
                        BuildConfig.APPLICATION_ID + ".fileprovider",
                        photoFile
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                }
            } else Toast.makeText(
                this@MainActivity,
                resources.getString(R.string.no_active_activity_error),
                Toast.LENGTH_LONG
            ).show()
        }
        val pm = packageManager
        if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) binding.fabAttachPicture.show() else binding.fabAttachPicture.hide()

        // Get the intent, verify the action and get the search query
        val intent = intent
        if (Intent.ACTION_SEARCH == intent.action) {
            val query = intent.getStringExtra(SearchManager.QUERY)
            filterActivityView(query)
        }
        // TODO: this is crazy to call onActivityChagned here,
        //  as it reloads the statistics and refills the viewModel...
        //  Completely against the idea of the viewmodel :-(
        onActivityChanged() /* do this at the very end to ensure that no Loader finishes its data loading before */
    }

    public override fun onResume() {
        mNavigationView.menu.findItem(R.id.nav_main).isChecked = true
        ActivityHelper.helper.registerDataChangeListener(this)
        onActivityChanged() /* refresh the current activity data */
        super.onResume()
        selectAdapter!!.notifyDataSetChanged() // redraw the complete recyclerview
        ActivityHelper.helper.evaluateAllConditions() // this is quite heavy and I am not so sure whether it is a good idea to do it unconditionally here...
    }

    public override fun onPause() {
        ActivityHelper.helper.unregisterDataChangeListener(this)
        super.onPause()
    }

    override fun onLongClick(view: View): Boolean {
        if (viewModel!!.currentActivity().value != null) {
            val i = Intent(this@MainActivity, EditActivity::class.java)
            i.putExtra("activityID", viewModel!!.currentActivity().value!!.mId)
            startActivity(i)
        }
        return true
    }

    override fun onItemLongClick(adapterPosition: Int): Boolean {
        val i = Intent(this@MainActivity, EditActivity::class.java)
        i.putExtra("activityID", selectAdapter!!.item(adapterPosition).mId)
        startActivity(i)
        return true
    }

    override fun onItemClick(adapterPosition: Int) {
        val newAct = selectAdapter!!.item(adapterPosition)
        if (newAct !== ActivityHelper.helper.currentActivity) {
            ActivityHelper.helper.currentActivity = newAct
            searchView!!.setQuery("", false)
            searchView!!.isIconified = true
        }
    }

    override fun onActivityChanged() {
        val newAct = ActivityHelper.helper.currentActivity
        viewModel!!.mCurrentActivity.value = newAct
        if (newAct != null) queryAllTotals()
        viewModel!!.currentDiaryUri = ActivityHelper.helper.currentDiaryUri
        // TODO: move this logic into the DetailViewModel??
//        viewModel.mAvgDuration.setValue("-");
//        viewModel.mStartOfLast.setValue("-");
//        viewModel.mTotalToday.setValue("-");
        /* stats are updated after query finishes in mQHelper */
        if (viewModel!!.currentActivity().value != null) {
            binding.row.name.text = viewModel!!.currentActivity().value!!.mName
            binding.row.background.setBackgroundColor(viewModel!!.currentActivity().value!!.mColor)
            binding.row.name.setTextColor(
                textColorOnBackground(viewModel!!.currentActivity().value!!.mColor))
            viewModel!!.mNote.setValue(ActivityHelper.helper.currentNote)
        } else {
            val col = ContextCompat.getColor(applicationContext, R.color.colorPrimary)
            binding.row.name.text = resources.getString(R.string.activity_title_no_selected_act)
            binding.row.background.setBackgroundColor(col)
            binding.row.name.setTextColor(textColorOnBackground(col))
            viewModel!!.mDuration.value = "-"
            viewModel!!.mNote.setValue("")
        }
        layoutManager!!.scrollToPosition(0)
    }

    fun queryAllTotals() {
        // TODO: move this into the DetailStatFragement
        val a = viewModel!!.mCurrentActivity.value
        if (a != null) {
            val id = a.mId

//            TODO: need better display format
            val end = System.currentTimeMillis()
            val oneDayAgo = end - DateHelper.DAY_IN_MS
            queryTotal(Calendar.DAY_OF_YEAR, oneDayAgo, end, id)
            //            queryTotal(DateHelper.FULLDAY, oneDayAgo, end, id);
            val sevenDaysAgo = end - 7 * DateHelper.DAY_IN_MS
            queryTotal(Calendar.WEEK_OF_YEAR, sevenDaysAgo, end, id)
            val thirtyDaysAgo = end - 30 * DateHelper.DAY_IN_MS
            queryTotal(Calendar.MONTH, thirtyDaysAgo, end, id)
            //            queryTotal(Calendar.WEEK_OF_YEAR, end, id);
//            queryTotal(Calendar.MONTH, end, id);
        }
    }

    private fun queryTotal(field: Int, start: Long, end: Long, actID: Int) {
        var u = Contract.DiaryStats.CONTENT_URI
        u = Uri.withAppendedPath(u, start.toString())
        u = Uri.withAppendedPath(u, end.toString())
        mQHandler!!.startQuery(
            QUERY_CURRENT_ACTIVITY_TOTAL, StatParam(field, end),
            u, arrayOf(Contract.DiaryStats.DURATION),
            Contract.DiaryActivity.TABLE_NAME + "." + Contract.DiaryActivity._ID
                    + " = ?", arrayOf(
                actID.toString()
            ), null
        )
    }

    /**
     * Called on change of the activity order due to likelyhood.
     */
    override fun onActivityOrderChanged() {
        /* only do likelihood sort in case we are not in a search */
        if (filter!!.isEmpty()) likelyhoodSort()
    }

    /**
     * Called when the data has changed.
     */
    override fun onActivityDataChanged() {
        selectAdapter!!.notifyDataSetChanged()
    }

    override fun onActivityDataChanged(activity: DiaryActivity) {
        selectAdapter!!.notifyItemChanged(selectAdapter!!.positionOf(activity))
    }

    /**
     * Called on addition of an activity.
     *
     */
    override fun onActivityAdded(activity: DiaryActivity) {
        /* no need to add it, as due to the reevaluation of the conditions the order change will happen */
    }

    /**
     * Called on removale of an activity.
     */
    override fun onActivityRemoved(activity: DiaryActivity) {
        selectAdapter!!.notifyDataSetChanged()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)

        // Get the SearchView and set the searchable configuration
        val searchManager = getSystemService(SEARCH_SERVICE) as SearchManager
        val searchMenuItem = menu.findItem(R.id.action_filter)
        searchView = searchMenuItem.actionView as SearchView?
        // Assumes current activity is the searchable activity
        searchView!!.setSearchableInfo(searchManager.getSearchableInfo(componentName))
        searchView!!.setOnCloseListener(this)
        searchView!!.setOnQueryTextListener(this)
        // setOnSuggestionListener -> for selection of a suggestion
        // setSuggestionsAdapter
        searchView!!.setOnSearchClickListener { _: View? -> setSearchMode(true) }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_add_activity) {
            startActivity(Intent(this, EditActivity::class.java))
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (Intent.ACTION_SEARCH == intent.action) {
            val query = intent.getStringExtra(SearchManager.QUERY)
            filterActivityView(query)
        }
        if (intent.hasExtra("SELECT_ACTIVITY_WITH_ID")) {
            val id = intent.getIntExtra("SELECT_ACTIVITY_WITH_ID", -1)
            ActivityHelper.helper.currentActivity = ActivityHelper.helper.activityWithId(id)
        }
    }

    private fun filterActivityView(query: String?) {
        filter = query
        if (filter == null || filter!!.isEmpty()) {
            likelyhoodSort()
        } else {
            val filtered = ActivityHelper.sortedActivities(query!!)
            //
            selectAdapter = SelectRecyclerViewAdapter(this@MainActivity, filtered)
            binding.selectRecycler.swapAdapter(selectAdapter, false)
            binding.selectRecycler.scrollToPosition(0)
        }
    }

    private fun likelyhoodSort() {
        if (selectAdapter == null || selectAdapter != binding.selectRecycler.adapter) {
            selectAdapter = SelectRecyclerViewAdapter(this@MainActivity, ActivityHelper.helper.activities)
            binding.selectRecycler.swapAdapter(selectAdapter, false)
        } else {
            selectAdapter!!.setActivities(ActivityHelper.helper.activities)
        }
    }

    override fun onClose(): Boolean {
        setSearchMode(false)
        likelyhoodSort()
        return false /* we wanna clear and close the search */
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        setSearchMode(false)
        return false
    }

    override fun onQueryTextChange(newText: String): Boolean {
        filterActivityView(newText)
        return true /* we handle the search directly, so no suggestions need to be show even if #70 is implemented */
    }

    override fun onNoteEditPositiveClick(str: String?, dialog: DialogFragment?) {
        val values = ContentValues()
        values.put(Contract.Diary.NOTE, str)
        mQHandler!!.startUpdate(
            0,
            null,
            viewModel!!.currentDiaryUri,
            values,
            null, null
        )
        viewModel!!.mNote.postValue(str)
        ActivityHelper.helper.currentNote = str
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            if (mCurrentPhotoPath != null && viewModel!!.currentDiaryUri != null) {
                compressAndSaveImage(mCurrentPhotoPath!!)
                val photoURI = FileProvider.getUriForFile(
                    this@MainActivity,
                    BuildConfig.APPLICATION_ID + ".fileprovider",
                    File(mCurrentPhotoPath!!)
                )
                val values = ContentValues()
                values.put(Contract.DiaryImage.URI, photoURI.toString())
                values.put(Contract.DiaryImage.DIARY_ID, viewModel!!.currentDiaryUri!!.lastPathSegment)
                mQHandler!!.startInsert(
                    0,
                    null,
                    Contract.DiaryImage.CONTENT_URI,
                    values
                )
            }
        }
    }

    private fun setupViewPager(viewPager: ViewPager) {
        val adapter = ViewPagerAdapter(supportFragmentManager)
        adapter.addFragment(DetailStatFragement(), resources.getString(R.string.fragment_detail_stats_title))
        adapter.addFragment(DetailNoteFragment(), resources.getString(R.string.fragment_detail_note_title))
        adapter.addFragment(DetailPictureFragement(), resources.getString(R.string.fragment_detail_pictures_title))
        viewPager.adapter = adapter
    }

    internal class ViewPagerAdapter(manager: FragmentManager?) : FragmentPagerAdapter(
        manager!!
    ) {
        private val mFragmentList: MutableList<Fragment> = ArrayList(50)
        private val mFragmentTitleList: MutableList<String> = ArrayList(50)
        override fun getItem(position: Int): Fragment {
            return mFragmentList[position]
        }

        override fun getCount(): Int {
            return mFragmentList.size
        }

        fun addFragment(fragment: Fragment, title: String) {
            mFragmentList.add(fragment)
            mFragmentTitleList.add(title)
        }

        override fun getPageTitle(position: Int): CharSequence {
            return mFragmentTitleList[position]
        }
    }

    private class MainAsyncQueryHandler(cr: ContentResolver?, val viewModel: DetailViewModel?) : AsyncQueryHandler(cr) {
        override fun startQuery(
            token: Int,
            cookie: Any,
            uri: Uri,
            projection: Array<String>,
            selection: String,
            selectionArgs: Array<String>,
            orderBy: String?
        ) {
            super.startQuery(token, cookie, uri, projection, selection, selectionArgs, orderBy)
        }

        override fun onQueryComplete(token: Int, cookie: Any?, cursor: Cursor) {
            super.onQueryComplete(token, cookie, cursor)
            if (cursor.moveToFirst()) {
                if (token == QUERY_CURRENT_ACTIVITY_STATS) {
//                    long avg = cursor.getLong(cursor.getColumnIndex(Contract.DiaryActivity.X_AVG_DURATION));
//                    viewModel.mAvgDuration.setValue(getResources().
//                            getString(R.string.avg_duration_description, TimeSpanFormatter.format(avg)));

//                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
//                    String formatString = sharedPref.getString(SettingsActivity.KEY_PREF_DATETIME_FORMAT,
//                            getResources().getString(R.string.default_datetime_format));
//
//                    long start = cursor.getLong(cursor.getColumnIndex(Contract.DiaryActivity.X_START_OF_LAST));
//
//                    viewModel.mStartOfLast.setValue(getResources().
//                            getString(R.string.last_done_description, DateFormat.format(formatString, start)));
                } else if (token == QUERY_CURRENT_ACTIVITY_TOTAL) {
                    if (cookie != null) {
                        val p = cookie as StatParam
                        @SuppressLint("Range") val total =
                            cursor.getLong(cursor.getColumnIndex(Contract.DiaryStats.DURATION))
                        var x = dateFormat(p.field).format(p.end)
                        x = x + ": " + format(total)
                        when (p.field) {
                            Calendar.DAY_OF_YEAR -> viewModel!!.mTotalToday.setValue(x)
                            Calendar.WEEK_OF_YEAR -> viewModel!!.mTotalWeek.setValue(x)
                            Calendar.MONTH -> viewModel!!.mTotalMonth.setValue(x)
                            else -> {}
                        }
                    }
                }
            }
            cursor.close()
        }
    }

    private class StatParam(val field: Int, val end: Long)
    companion object {
        private val TAG = MainActivity::class.java.simpleName
        private const val REQUEST_IMAGE_CAPTURE = 1

        //    private static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 4711;
        private const val QUERY_CURRENT_ACTIVITY_STATS = 1
        private const val QUERY_CURRENT_ACTIVITY_TOTAL = 2
    }
}