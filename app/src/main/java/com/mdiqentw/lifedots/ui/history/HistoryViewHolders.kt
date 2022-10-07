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
package com.mdiqentw.lifedots.ui.history

import android.view.View
import android.view.View.OnLongClickListener
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.mdiqentw.lifedots.databinding.ActivityHistoryEntryBinding
import com.mdiqentw.lifedots.ui.generic.DetailRecyclerViewAdapter

class HistoryViewHolders(val detailLoaderID: Int, listener: HistoryRecyclerViewAdapter.SelectListener, bind: ActivityHistoryEntryBinding) : RecyclerView.ViewHolder(bind.root), View.OnClickListener, OnLongClickListener {
    var diaryEntryID = 0
    @JvmField
    val mSeparator: TextView
    @JvmField
    val mStartLabel: TextView
    @JvmField
    val mDurationLabel: TextView
    @JvmField
    val mNoteLabel: TextView
    var mSymbol: ImageView? = null
    val mActivityCardView: CardView
    @JvmField
    val mName: TextView
    @JvmField
    val mBackground: View
    @JvmField
    var mDetailAdapter: DetailRecyclerViewAdapter? = null
    @JvmField
    val mImageRecycler: RecyclerView
    private val mListener: HistoryRecyclerViewAdapter.SelectListener

    override fun onClick(view: View) {
        val position = bindingAdapterPosition
        if (position != RecyclerView.NO_POSITION) {
            mListener.onItemClick(this, position, diaryEntryID)
        }
    }

    override fun onLongClick(view: View): Boolean {
        val position = bindingAdapterPosition
        return if (position != RecyclerView.NO_POSITION) {
            mListener.onItemLongClick(this, position, diaryEntryID)
        } else false
    }

    init {
        bind.root.setOnClickListener(this)
        bind.root.setOnLongClickListener(this)
        mSeparator = bind.separator
        mStartLabel = bind.startLabel
        mNoteLabel = bind.note
        mDurationLabel = bind.durationLabel
        //        mSymbol = bind.row.picture;
        mActivityCardView = bind.activityCard
        mName = bind.row.name
        mBackground = bind.row.background
        mImageRecycler = bind.imageGrid
        mListener = listener
    }
}