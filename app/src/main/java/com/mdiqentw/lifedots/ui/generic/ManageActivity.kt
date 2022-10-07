/*
 * LifeDots
 *
 * Copyright (C) 2017 Raphael Mack http://www.raphael-mack.de
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
package com.mdiqentw.lifedots.ui.generic

import android.annotation.SuppressLint
import android.content.*
import android.database.Cursor
import android.graphics.Paint
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import com.mdiqentw.lifedots.MVApplication
import com.mdiqentw.lifedots.R
import com.mdiqentw.lifedots.databinding.ActivityManageContentBinding
import com.mdiqentw.lifedots.db.Contract
import com.mdiqentw.lifedots.helpers.GraphicsHelper.textColorOnBackground

/*
 * MainActivity to show most of the UI, based on switching the fragements
 *
 * */
class ManageActivity : BaseActivity(), LoaderManager.LoaderCallbacks<Cursor> {
    /* are deleted items currently visible? */
    private var showDeleted = false

    /* Access only allowed via ActivityHelper.helper singleton */
    private class QHandler : AsyncQueryHandler(MVApplication.appContext!!.contentResolver)

    private val mQHandler = QHandler()

    private class DiaryActivityAdapter(act: ManageActivity?) :
        ResourceCursorAdapter(act, R.layout.lifedot_row, null, 0) {
        @SuppressLint("Range")
        override fun bindView(view: View, context: Context, cursor: Cursor) {
            val name = cursor.getString(cursor.getColumnIndex(Contract.DiaryActivity.NAME))
            val color = cursor.getInt(cursor.getColumnIndex(Contract.DiaryActivity.COLOR))

//            ActivityRowBinding bindRow = DataBindingUtil.setContentView(ManageActivity.this, R.layout.activity_row);
            val actName = view.findViewById<TextView>(R.id.name)
            actName.text = name
            if (cursor.getInt(cursor.getColumnIndex(Contract.DiaryActivity._DELETED)) == 0) {
                actName.paintFlags = actName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            } else {
                actName.paintFlags = actName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            }
            val bgrd = view.findViewById<RelativeLayout>(R.id.background)
            bgrd.setBackgroundColor(color)
            actName.setTextColor(textColorOnBackground(color))
        }
    }

    private var mActivitiyListAdapter: DiaryActivityAdapter? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        /* TODO: save and restore state */
        val binding =
            DataBindingUtil.setContentView<ActivityManageContentBinding>(this, R.layout.activity_manage_content)
        setContent(binding.root)
        val mList = binding.manageActivityList
        mActivitiyListAdapter = DiaryActivityAdapter(this)
        mList.adapter = mActivitiyListAdapter
        mList.onItemClickListener = mOnClickListener

        // Prepare the loader.  Either re-connect with an existing one,
        // or start a new one.
        /* TODO: refactor to use the ActivityHelper instead of directly a Loader; 2017-12-02, RMk: not sure whether we should do this... */
        /* TODO: add a clear way to ensure loader ID uniqueness */LoaderManager.getInstance(this)
            .initLoader(-2, null, this)
        mDrawerToggle.isDrawerIndicatorEnabled = false
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.manage_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle your other action bar items...
        val mid = item.itemId
        if (mid == R.id.action_add_activity) {
            val intentaddact = Intent(this@ManageActivity, EditActivity::class.java)
            startActivity(intentaddact)
        } else if (mid == R.id.action_show_hide_deleted) {
            showDeleted = !showDeleted
            LoaderManager.getInstance(this).restartLoader(-2, null, this)
            if (showDeleted) {
                item.setIcon(R.drawable.ic_hide_deleted)
                item.setTitle(R.string.nav_hide_deleted)
            } else {
                item.setIcon(R.drawable.ic_show_deleted)
                item.setTitle(R.string.nav_show_deleted)
            }
        } else if (mid == android.R.id.home) finish()
        return super.onOptionsItemSelected(item)
    }

    // Called when a new Loader needs to be created
    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return CursorLoader(
            this, Contract.DiaryActivity.CONTENT_URI,
            PROJECTION,
            if (showDeleted) "" else SELECTION,
            null, null
        )
    }

    // Called when a previously created loader has finished loading
    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor) {
        // Swap the new cursor in.  (The framework will take care of closing the
        // old cursor once we return.)
        mActivitiyListAdapter!!.swapCursor(data)
    }

    // Called when a previously created loader is reset, making the data unavailable
    override fun onLoaderReset(loader: Loader<Cursor>) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed.  We need to make sure we are no
        // longer using it.
        mActivitiyListAdapter!!.swapCursor(null)
    }

    @SuppressLint("Range")
    private val mOnClickListener = OnItemClickListener { parent: AdapterView<*>, _: View?, position: Int, _: Long ->
        val c = parent.getItemAtPosition(position) as Cursor
        if (c.getInt(c.getColumnIndex(Contract.DiaryActivity._DELETED)) == 0) {
            val i = Intent(this@ManageActivity, EditActivity::class.java)
            i.putExtra("activityID", c.getInt(c.getColumnIndex(Contract.DiaryActivity._ID)))
            startActivity(i)
        } else {
            // selected item is deleted. Ask for undeleting it.
            val builder = AlertDialog.Builder(this@ManageActivity)
                .setTitle(R.string.dlg_undelete_activity_title)
                .setMessage(
                    resources.getString(
                        R.string.dlg_undelete_activity_text,
                        c.getString(c.getColumnIndex(Contract.DiaryActivity.NAME))
                    )
                )
                .setPositiveButton(android.R.string.yes) { _: DialogInterface?, _: Int ->
                    val values = ContentValues()
                    values.put(Contract.DiaryActivity._DELETED, 0)
                    mQHandler.startUpdate(
                        0,
                        null,
                        ContentUris.withAppendedId(
                            Contract.DiaryActivity.CONTENT_URI,
                            c.getLong(c.getColumnIndex(Contract.DiaryActivity._ID))
                        ),
                        values,
                        Contract.DiaryActivity._ID + "=?",
                        arrayOf(c.getString(c.getColumnIndex(Contract.DiaryActivity._ID)))
                    )
                }
                .setNegativeButton(android.R.string.no, null)
            builder.create().show()
        }
    }

    /* TODO #24: implement swipe for parent / child navigation */ /* TODO #24: add number of child activities in view */
    public override fun onResume() {
        mNavigationView.menu.findItem(R.id.nav_activity_manager).isChecked = true
        super.onResume()
    }

    companion object {
        private val PROJECTION = arrayOf(
            Contract.DiaryActivity._ID,
            Contract.DiaryActivity.NAME,
            Contract.DiaryActivity.COLOR,
            Contract.DiaryActivity._DELETED
        )
        private const val SELECTION = Contract.DiaryActivity._DELETED + "=0"
    }
}