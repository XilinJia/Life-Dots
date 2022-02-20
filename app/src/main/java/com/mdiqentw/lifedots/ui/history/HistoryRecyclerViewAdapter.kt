/*
 * LifeDots
 *
 * Copyright (C) 2017 Raphael Mack http://www.raphael-mack.de
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

import android.database.Cursor
import android.database.DataSetObserver
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.mdiqentw.lifedots.MVApplication
import com.mdiqentw.lifedots.R
import com.mdiqentw.lifedots.databinding.ActivityHistoryEntryBinding
import com.mdiqentw.lifedots.db.Contract
import com.mdiqentw.lifedots.helpers.GraphicsHelper
import com.mdiqentw.lifedots.helpers.TimeSpanFormatter
import com.mdiqentw.lifedots.ui.generic.DetailRecyclerViewAdapter
import com.mdiqentw.lifedots.ui.settings.SettingsActivity
import java.text.SimpleDateFormat
import java.util.*

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
class HistoryRecyclerViewAdapter(private val mContext: HistoryActivity, private val mListener: SelectListener, private var mCursor: Cursor?) : RecyclerView.Adapter<HistoryViewHolders>() {
    private val mDataObserver: DataSetObserver?
    private var idRowIdx = -1
    private var startRowIdx = -1
    private var nameRowIdx = -1
    private var endRowIdx = -1
    private var colorRowIdx = -1
    private var noteRowIdx = -1
    private val mViewHolders: MutableList<HistoryViewHolders>

    interface SelectListener {
        fun onItemClick(viewHolder: HistoryViewHolders?, adapterPosition: Int, diaryID: Int)
        fun onItemLongClick(viewHolder: HistoryViewHolders?, adapterPosition: Int, diaryID: Int): Boolean
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolders {
        val binding: ActivityHistoryEntryBinding = DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.activity_history_entry,
                parent,
                false)
        val rcv = HistoryViewHolders(mViewHolders.size, mListener, binding)

//        View layoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_history_entry, null, false);
//        HistoryViewHolders rcv = new HistoryViewHolders(mViewHolders.size(), mListener, layoutView);
        mViewHolders.add(rcv)
        return rcv
    }

    override fun onBindViewHolder(holder: HistoryViewHolders, position: Int) {
        var showHeader = false
        var header = ""
        check(mCursor!!.moveToPosition(position)) { "couldn't move cursor to position $position" }
        val start = Date(mCursor!!.getLong(startRowIdx))
        val name = mCursor!!.getString(nameRowIdx)
        val color = mCursor!!.getInt(colorRowIdx)
        holder.mBackground.setBackgroundColor(color)
        holder.mName.setTextColor(GraphicsHelper.textColorOnBackground(color))
        holder.diaryEntryID = mCursor!!.getInt(idRowIdx)
        val end: Date? = if (mCursor!!.isNull(endRowIdx)) {
            null
        } else {
            Date(mCursor!!.getLong(endRowIdx))
        }
        val startCal = Calendar.getInstance()
        startCal.timeInMillis = mCursor!!.getLong(startRowIdx)
        if (mCursor!!.isFirst) {
            showHeader = true
        } else {
            mCursor!!.moveToPrevious()
            val clast = Calendar.getInstance()
            clast.timeInMillis = mCursor!!.getLong(startRowIdx)
            mCursor!!.moveToNext()
            if (clast[Calendar.DATE] != startCal[Calendar.DATE]) {
                showHeader = true
            }
        }
        if (showHeader) {
            val now = Calendar.getInstance()
            header = when {
                now[Calendar.DATE] == startCal[Calendar.DATE] -> {
                    mContext.resources.getString(R.string.today)
                }
                now[Calendar.DATE] - startCal[Calendar.DATE] == 1 -> {
                    mContext.resources.getString(R.string.yesterday)
                }
                now[Calendar.WEEK_OF_YEAR] - startCal[Calendar.WEEK_OF_YEAR] == 0 -> {
                    val formatter = SimpleDateFormat("EEEE")
                    formatter.format(start)
                }
                now[Calendar.WEEK_OF_YEAR] - startCal[Calendar.WEEK_OF_YEAR] == 1 -> {
                    mContext.resources.getString(R.string.lastWeek)
                    /* TODO: this is shown for each day last week, which is too much... -> refactor to get rid of showHeader or set it in this if-elsif-chain */
                }
                else -> {
                    val formatter = SimpleDateFormat("MMMM dd yyyy")
                    formatter.format(start)
                }
            }
        }
        if (showHeader) {
            holder.mSeparator.visibility = View.VISIBLE
            holder.mSeparator.text = header
        } else {
            holder.mSeparator.visibility = View.GONE
        }
        holder.mName.text = name
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(MVApplication.getAppContext())
        val formatString = sharedPref.getString(SettingsActivity.KEY_PREF_DATETIME_FORMAT,
                mContext.resources.getString(R.string.default_datetime_format))
        /* TODO: #36 register listener on preference change to redraw the date time formatting */holder.mStartLabel.text = MVApplication.getAppContext().resources.getString(R.string.history_start, DateFormat.format(formatString, start))
        var noteStr: String? = ""
        if (!mCursor!!.isNull(noteRowIdx)) {
            noteStr = mCursor!!.getString(noteRowIdx)
            holder.mNoteLabel.visibility = View.VISIBLE
        } else {
            holder.mNoteLabel.visibility = View.GONE
        }
        holder.mNoteLabel.text = noteStr
        val duration: String
        if (end == null) {
            duration = MVApplication.getAppContext().resources.getString(R.string.duration_description, TimeSpanFormatter.fuzzyFormat(start, Date()))
        } else {
            holder.mStartLabel.text = MVApplication.getAppContext().resources.getString(R.string.history_start, DateFormat.format(formatString, start))
            duration = MVApplication.getAppContext().resources.getString(R.string.history_end, DateFormat.format(formatString, end),
                    TimeSpanFormatter.format(end.time - start.time))
        }
        holder.mDurationLabel.text = duration

        /* TODO #33: set activity picture (icon + main pciture if available) */holder.mDetailAdapter = DetailRecyclerViewAdapter(mContext, null)
        mContext.addDetailAdapter(mCursor!!.getLong(idRowIdx), holder.mDetailAdapter)

        /* TODO: make it a configuration option how many picture columns we should show */
        val layoutMan: RecyclerView.LayoutManager = StaggeredGridLayoutManager(3, StaggeredGridLayoutManager.VERTICAL)
        holder.mImageRecycler.layoutManager = layoutMan
        holder.mImageRecycler.adapter = holder.mDetailAdapter
        /* click handlers are done via ViewHolder */
    }

    override fun getItemCount(): Int {
        return if (mCursor != null) {
            mCursor!!.count
        } else 0
    }

    //    XJ edit
    fun swapCursor(newCursor: Cursor) {
        if (newCursor === mCursor) {
//            if (newCursor != null) {
//                newCursor.close();    // not sure if this should be closed,  leave it open for now
//            }
            return
        }
        val oldCursor = mCursor
        if (oldCursor != null && mDataObserver != null) {
            oldCursor.unregisterDataSetObserver(mDataObserver)
        }
        oldCursor?.close()
        mCursor = newCursor
        if (mCursor != null) {
            if (mDataObserver != null) mCursor!!.registerDataSetObserver(mDataObserver)
            setRowIndex()
        } else {
            idRowIdx = -1
        }
        notifyDataSetChanged()
    }

    private fun setRowIndex() {
        idRowIdx = mCursor!!.getColumnIndex(Contract.Diary._ID)
        startRowIdx = mCursor!!.getColumnIndex(Contract.Diary.START)
        nameRowIdx = mCursor!!.getColumnIndex(Contract.DiaryActivity.NAME)
        colorRowIdx = mCursor!!.getColumnIndex(Contract.DiaryActivity.COLOR)
        endRowIdx = mCursor!!.getColumnIndex(Contract.Diary.END)
        noteRowIdx = mCursor!!.getColumnIndex(Contract.Diary.NOTE)
    }

    init {
        mViewHolders = ArrayList(17)
        mDataObserver = object : DataSetObserver() {
            override fun onChanged() {
                /* notify about the data change */
                notifyDataSetChanged()
            }

            override fun onInvalidated() {
                /* notify about the data change */
                notifyDataSetChanged()
            }
        }
        if (mCursor != null) {
            mCursor!!.registerDataSetObserver(mDataObserver)
            setRowIndex()
        }
    }
}