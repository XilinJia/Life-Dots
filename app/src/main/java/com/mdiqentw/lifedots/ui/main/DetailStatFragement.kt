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
package com.mdiqentw.lifedots.ui.main

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.mdiqentw.lifedots.R
import com.mdiqentw.lifedots.databinding.FragmentDetailStatsBinding
import com.mdiqentw.lifedots.helpers.ActivityHelper
import com.mdiqentw.lifedots.helpers.TimeSpanFormatter
import com.mdiqentw.lifedots.model.DetailViewModel
import com.mdiqentw.lifedots.ui.history.HistoryDetailActivity
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
class DetailStatFragement : Fragment() {
    private val updateDurationHandler = Handler(Looper.myLooper()!!)
    private var viewModel: DetailViewModel? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        val binding: FragmentDetailStatsBinding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_detail_stats, container, false)
        val view = binding.root

        // passing no diaryEntryID will edit the last one
        val headerClickHandler = View.OnClickListener {
            if (viewModel!!.currentActivity().value != null) {
                val i = Intent(activity, HistoryDetailActivity::class.java)
                // passing no diaryEntryID will edit the last one
                startActivity(i)
            }
        }
        view.setOnClickListener(headerClickHandler)
        binding.detailContent.setOnClickListener(headerClickHandler)
        viewModel = ViewModelProvider(requireActivity()).get(DetailViewModel::class.java)
        binding.viewModel = viewModel
        // Specify the current activity as the lifecycle owner.
        binding.lifecycleOwner = this
        return view
    }

    private val updateDurationRunnable: Runnable = object : Runnable {
        override fun run() {
            updateDurationTextView()
            updateDurationHandler.postDelayed(this, (10 * 1000).toLong())
        }
    }

    private fun updateDurationTextView() {
        val duration = resources.getString(R.string.duration_description, TimeSpanFormatter.fuzzyFormat(ActivityHelper.helper.currentActivityStartTime, Date()))
        viewModel!!.mDuration.value = duration
        val a: Activity? = activity
        if (a is MainActivity) {
            a.queryAllTotals()
        }
    }

    override fun onResume() {
        super.onResume()
        updateDurationTextView()
        updateDurationHandler.postDelayed(updateDurationRunnable, (10 * 1000).toLong())
    }

    override fun onPause() {
        updateDurationHandler.removeCallbacks(updateDurationRunnable)
        super.onPause()
    }
}