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

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import android.view.View.OnLongClickListener
import android.widget.ImageView
import android.widget.TextView
import com.mdiqentw.lifedots.databinding.DetailRecyclerItemBinding

class DetailViewHolders(listener: SelectListener, bind: DetailRecyclerItemBinding) : RecyclerView.ViewHolder(bind.root), View.OnClickListener, OnLongClickListener {
    interface SelectListener {
        fun onDetailItemClick(adapterPosition: Int)
        fun onDetailItemLongClick(adapterPosition: Int): Boolean
    }

    private val mTextView: TextView
    @JvmField
    val mSymbol: ImageView
    private val mListener: SelectListener

    override fun onClick(view: View) {
        val position = bindingAdapterPosition
        if (position != RecyclerView.NO_POSITION) {
            mListener.onDetailItemClick(position)
        }
    }

    override fun onLongClick(v: View): Boolean {
        val position = bindingAdapterPosition
        return if (position != RecyclerView.NO_POSITION) {
            mListener.onDetailItemLongClick(position)
        } else false
    }

    init {
        bind.root.setOnClickListener(this)
        bind.root.setOnLongClickListener(this)
        mTextView = bind.detailText
        mSymbol = bind.picture
        mListener = listener
    }
}