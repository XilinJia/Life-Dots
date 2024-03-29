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
package com.mdiqentw.lifedots.ui.main

import android.view.View
import android.view.View.OnLongClickListener
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mdiqentw.lifedots.databinding.LifedotRowBinding
import com.mdiqentw.lifedots.databinding.SelectRecyclerItemBinding

class SelectViewHolders(listener: SelectRecyclerViewAdapter.SelectListener, bind: SelectRecyclerItemBinding) : RecyclerView.ViewHolder(bind.root), View.OnClickListener, OnLongClickListener {
    val binding: LifedotRowBinding
    @JvmField
    val mName: TextView
    @JvmField
    val mBackground: View
    private val mListener: SelectRecyclerViewAdapter.SelectListener

    override fun onClick(view: View) {
        val position = bindingAdapterPosition
        if (position != RecyclerView.NO_POSITION) {
            mListener.onItemClick(position)
        }
    }

    override fun onLongClick(v: View): Boolean {
        val position = bindingAdapterPosition
        return if (position != RecyclerView.NO_POSITION) {
            mListener.onItemLongClick(position)
        } else false
    }

    init {
        bind.root.setOnClickListener(this)
        bind.root.setOnLongClickListener(this)
        binding = bind.row
        mName = binding.name
        mBackground = binding.background
        mListener = listener
    }
}