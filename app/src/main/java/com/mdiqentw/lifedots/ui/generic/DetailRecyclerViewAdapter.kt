/*
 * LifeDots
 *
 * Copyright (C) 2020 Xilin Jia https://github.com/XilinJia
  ~ Copyright (C) 2017-2018 Raphael Mack http://www.raphael-mack.de
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

import android.content.AsyncQueryHandler
import android.content.ContentValues
import android.content.DialogInterface
import android.content.Intent
import android.database.Cursor
import android.database.DataSetObserver
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.mdiqentw.lifedots.MVApplication
import com.mdiqentw.lifedots.R
import com.mdiqentw.lifedots.databinding.DetailRecyclerItemBinding
import com.mdiqentw.lifedots.db.Contract
import com.mdiqentw.lifedots.helpers.GraphicsHelper
import com.squareup.picasso.Picasso
import java.io.File

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
class DetailRecyclerViewAdapter(act: BaseActivity, details: Cursor?) :
    RecyclerView.Adapter<DetailViewHolders>(), DetailViewHolders.SelectListener {
    private class QHandler : AsyncQueryHandler(MVApplication.getAppContext().contentResolver)

    private val mQHandler = QHandler()
    private var mCursor: Cursor?
    private val mAct: BaseActivity
    private val mDataObserver: DataSetObserver?
    private var uriRowIdx = 0
    private var idRowIdx = 0
    val adapterId: Int = lastAdapterId

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DetailViewHolders {
        val binding: DetailRecyclerItemBinding = DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.detail_recycler_item,
                parent,
                false)
        return DetailViewHolders(this, binding)
    }

    override fun onBindViewHolder(holder: DetailViewHolders, position: Int) {
        check(mCursor!!.moveToPosition(position)) { "couldn't move cursor to position $position" }
        val s: String
        if (uriRowIdx >= 0) {
            s = mCursor!!.getString(uriRowIdx)
            val i = Uri.parse(s)
            Picasso.get().load(i)
                    .rotate(GraphicsHelper.getFileExifRotation(i).toFloat())
                    .resize(500, 500)
                    .centerInside()
                    .into(holder.mSymbol)
        } else {
            Log.e(TAG, "onBindViewHolder: uriRowIdx = $uriRowIdx")
        }
    }

    override fun getItemCount(): Int {
        return if (mCursor != null) {
            mCursor!!.count
        } else 0
    }

    fun swapCursor(newCursor: Cursor?) {
        if (newCursor === mCursor) {
//            newCursor.close();    // closing this causes crash after return from photo view
            return
        }
        val oldCursor = mCursor
        if (oldCursor != null && mDataObserver != null) {
            oldCursor.unregisterDataSetObserver(mDataObserver)
        }
        oldCursor?.close()
        mCursor = newCursor
        if (mCursor != null) {
            if (mDataObserver != null) {
                mCursor!!.registerDataSetObserver(mDataObserver)
            }
            uriRowIdx = mCursor!!.getColumnIndex(Contract.DiaryImage.URI)
            idRowIdx = mCursor!!.getColumnIndex(Contract.DiaryImage._ID)
        } else {
            uriRowIdx = -1
            idRowIdx = -1
        }
        notifyDataSetChanged()
    }

    fun getDiaryImageIdAt(position: Int): Long {
        check(idRowIdx >= 0) { "idRowIdx not valid" }
        require(position >= 0) { "position ($position) too small" }
        require(position < mCursor!!.count) { "position ($position) too small" }
        val pos = mCursor!!.position
        mCursor!!.moveToPosition(position)
        val result: Long = mCursor!!.getLong(idRowIdx)
        mCursor!!.moveToPosition(pos)
        return result
    }

    private fun deleteImageAt(position: Int) {
        check(idRowIdx >= 0) { "idRowIdx not valid" }
        require(position >= 0) { "position ($position) too small" }
        require(position < mCursor!!.count) { "position ($position) too small" }
        val pos = mCursor!!.position
        mCursor!!.moveToPosition(position)
        val result: Long = mCursor!!.getLong(idRowIdx)

        val uri = mCursor!!.getString(uriRowIdx)
        val filename = uri.substring(uri.lastIndexOf("/")+1)
        File(GraphicsHelper.imageStorageDirectory(), filename).delete()
        mCursor!!.moveToPosition(pos)

        val values = ContentValues()
        values.put(Contract.DiaryImage._DELETED, 1)
        mQHandler.startUpdate(0,
            null,
            Contract.DiaryImage.CONTENT_URI,
            values,
            Contract.DiaryImage._ID + "=?", arrayOf(result.toString()))
    }

    override fun onDetailItemClick(adapterPosition: Int) {
        check(mCursor!!.moveToPosition(adapterPosition)) { "couldn't move cursor to position $adapterPosition" }
        val s = mCursor!!.getString(uriRowIdx)
        val i = Uri.parse(s)
        val intent = Intent()
        intent.action = Intent.ACTION_VIEW
        intent.setDataAndType(i, "image/*")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        mAct.startActivity(intent)
    }

    override fun onDetailItemLongClick(adapterPosition: Int): Boolean {
        //TODO: generalize the DetailView to include this code also
        val builder = AlertDialog.Builder(mAct)
                .setTitle(R.string.dlg_delete_image_title)
                .setMessage(R.string.dlg_delete_image_text)
                .setPositiveButton(R.string.yes) { _: DialogInterface?, _: Int ->
                    deleteImageAt(adapterPosition)
                }
                .setNegativeButton(R.string.no, null)
        builder.create().show()
        return true
    }

    companion object {
        private val TAG = DetailRecyclerViewAdapter::class.java.name
        private var lastAdapterId = 0
    }

    init {
        lastAdapterId++
        mCursor = details
        mAct = act
        // TODO: do we need this one here?
        mDataObserver = object : DataSetObserver() {
            override fun onChanged() {
                /* notify about the data change */
                notifyDataSetChanged()

                // TODO: remove #56
            }

            override fun onInvalidated() {
                /* notify about the data change */
                notifyDataSetChanged()
            }
        }
        if (mCursor != null) {
            mCursor!!.registerDataSetObserver(mDataObserver)
            uriRowIdx = mCursor!!.getColumnIndex(Contract.DiaryImage.URI)
            idRowIdx = mCursor!!.getColumnIndex(Contract.DiaryImage._ID)
        }
    }
}