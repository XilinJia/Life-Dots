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
package com.mdiqentw.lifedots.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.mdiqentw.lifedots.R
import com.mdiqentw.lifedots.databinding.SelectRecyclerItemBinding
import com.mdiqentw.lifedots.helpers.GraphicsHelper
import com.mdiqentw.lifedots.model.DiaryActivity

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
class SelectRecyclerViewAdapter(private val mSelectListener: SelectListener, private var mActivityList: List<DiaryActivity>) : RecyclerView.Adapter<SelectViewHolders>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectViewHolders {
        val binding: SelectRecyclerItemBinding = DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.select_recycler_item,
                parent,
                false)
        return SelectViewHolders(mSelectListener, binding)
    }

    override fun onBindViewHolder(holder: SelectViewHolders, position: Int) {
        val act = mActivityList[position]
//        val formatter: NumberFormat = DecimalFormat("#0.00")
        holder.mName.text = act.name

        // show likelyhood in activity name
        //     holder.mName.setText(act.getName() + " (" + formatter.format(ActivityHelper.helper.likelihoodFor(act)) + ")");
        // TODO #33:        holder.mSymbol.setImageResource(act.getPhoto());
        holder.mBackground.setBackgroundColor(act.color)
        holder.mName.setTextColor(GraphicsHelper.textColorOnBackground(act.color))

        // TODO #31: set the width based on the likelyhood
    }

    override fun getItemCount(): Int {
        return mActivityList.size
    }

    interface SelectListener {
        fun onItemClick(adapterPosition: Int)
        fun onItemLongClick(adapterPosition: Int): Boolean
    }

    fun setActivities(activityList: List<DiaryActivity>) {
        mActivityList = activityList
        notifyDataSetChanged()
    }

    override fun getItemId(position: Int): Long {
        return mActivityList[position].id.toLong()
    }

    fun positionOf(activity: DiaryActivity): Int {
        return mActivityList.indexOf(activity)
    }

    fun item(id: Int): DiaryActivity {
        return mActivityList[id]
    }

    init {
        setHasStableIds(true)
    }
}