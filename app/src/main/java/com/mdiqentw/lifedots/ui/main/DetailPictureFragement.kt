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
package com.mdiqentw.lifedots.ui.main

import android.database.Cursor
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.mdiqentw.lifedots.R
import com.mdiqentw.lifedots.db.Contract
import com.mdiqentw.lifedots.model.DetailViewModel
import com.mdiqentw.lifedots.ui.generic.BaseActivity
import com.mdiqentw.lifedots.ui.generic.DetailRecyclerViewAdapter

//import androidx.gridlayout.widget.GridLayoutManager;
//import androidx.recyclerview.widget.LinearLayoutManager;

class DetailPictureFragement : Fragment(), LoaderManager.LoaderCallbacks<Cursor> {

    private lateinit var detailRecyclerView: RecyclerView
    private lateinit var detailAdapter: DetailRecyclerViewAdapter
    private var viewModel: DetailViewModel? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_detail_pictures, container, false)
        viewModel = ViewModelProvider(requireActivity()).get(DetailViewModel::class.java)
        detailRecyclerView = view.findViewById(R.id.picture_recycler)
        val detailLayoutManager = StaggeredGridLayoutManager(3, StaggeredGridLayoutManager.VERTICAL)
        detailRecyclerView.layoutManager = detailLayoutManager

        // TODO:check  detailRecyclerView.setNestedScrollingEnabled(true);
        detailAdapter = DetailRecyclerViewAdapter(activity as BaseActivity, null)
        detailRecyclerView.adapter = detailAdapter
        reload()
        return view
    }

    fun reload() {
        LoaderManager.getInstance(this).restartLoader(0, null, this)
    }

    // Called when a new Loader needs to be created
    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        val currentDiaryUri = viewModel!!.currentDiaryUri
        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return CursorLoader(requireActivity(), Contract.DiaryImage.CONTENT_URI,
                PROJECTION_IMG,
                Contract.DiaryImage.TABLE_NAME + "." + Contract.DiaryImage.DIARY_ID + "=? AND "
                        + Contract.DiaryImage._DELETED + "=0",
                if (currentDiaryUri == null) arrayOf("0") else arrayOf(currentDiaryUri.lastPathSegment),
                Contract.DiaryImage.SORT_ORDER_DEFAULT)
    }

    // Called when a previously created loader has finished loading
    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor) {
        // Swap the new cursor in
        detailAdapter.swapCursor(data)
    }

    // Called when a previously created loader is reset, making the data unavailable
    override fun onLoaderReset(loader: Loader<Cursor>) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed.  We need to make sure we are no
        // longer using it.
        detailAdapter.swapCursor(null)
    }

    companion object {
        private val PROJECTION_IMG = arrayOf(
                Contract.DiaryImage.URI,
                Contract.DiaryImage._ID
        )
    }
}