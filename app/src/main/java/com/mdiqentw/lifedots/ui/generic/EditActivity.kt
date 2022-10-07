/*
 * LifeDots
 *
 * Copyright (C) 2017-2018 Raphael Mack http://www.raphael-mack.de
 * Copyright (C) 2018 Sam Partee
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
import android.content.AsyncQueryHandler
import android.content.ContentUris
import android.content.ContentValues
import android.content.DialogInterface
import android.database.Cursor
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.TooltipCompat
import androidx.databinding.DataBindingUtil
import com.flask.colorpicker.ColorPickerView
import com.flask.colorpicker.builder.ColorPickerDialogBuilder
import com.mdiqentw.lifedots.MVApplication
import com.mdiqentw.lifedots.R
import com.mdiqentw.lifedots.databinding.ActivityEditContentBinding
import com.mdiqentw.lifedots.db.Contract
import com.mdiqentw.lifedots.helpers.ActivityHelper
import com.mdiqentw.lifedots.helpers.ActivityHelper.DataChangedListener
import com.mdiqentw.lifedots.helpers.GraphicsHelper.prepareColorForNextActivity
import com.mdiqentw.lifedots.model.DiaryActivity
import java.lang.ref.WeakReference
import java.util.*

/*
 * EditActivity to add and modify activities
 *
 * */
class EditActivity : BaseActivity(), DataChangedListener {
    private var currentActivity /* null is for creating a new object */: DiaryActivity? = null
    private var checkState = CHECK_STATE_CHECKING
    lateinit var binding: ActivityEditContentBinding
    private var mActivityColor = 0
    private fun setCheckState(checkState: Int) {
        this.checkState = checkState
        if (checkState == CHECK_STATE_CHECKING) {
            binding.editActivityNameTil.error = "..."
        }
    }

    fun doTokenQueryName(cursor: Cursor, handler: AsyncQueryHandler) {
        binding.btnRename.visibility = View.GONE
        binding.btnRename.setOnClickListener(null)
        if (cursor.moveToFirst()) {
//            binding.btnQuickfix.setVisibility(View.VISIBLE);
            @SuppressLint("Range") val deleted =
                cursor.getLong(cursor.getColumnIndex(Contract.DiaryActivity._DELETED)) != 0L
            @SuppressLint("Range") val actId = cursor.getInt(cursor.getColumnIndex(Contract.DiaryActivity._ID))
            @SuppressLint("Range") val name = cursor.getString(cursor.getColumnIndex(Contract.DiaryActivity.NAME))
            setCheckState(CHECK_STATE_ERROR)
            if (deleted) {
                val str: CharSequence =
                    resources.getString(R.string.error_name_already_used_in_deleted, cursor.getString(0))
                binding.btnRename.visibility = View.VISIBLE
                setBtnTooltip(binding.btnRename, resources.getString(R.string.tooltip_quickfix_btn_rename_deleted))
                binding.btnRename.contentDescription = resources.getString(R.string.contentDesc_renameDeletedActivity)
                binding.editActivityNameTil.error = str
                binding.btnRename.setOnClickListener { _: View? ->
                    setCheckState(CHECK_STATE_CHECKING)
                    val values = ContentValues()
                    val newName = name + "_deleted"
                    Toast.makeText(
                        this,
                        resources.getString(R.string.renamed_deleted_activity_toast, newName),
                        Toast.LENGTH_LONG
                    ).show()
                    values.put(Contract.DiaryActivity.NAME, newName)
                    values.put(Contract.DiaryActivity._ID, java.lang.Long.valueOf(actId.toLong()))
                    handler.startQuery(
                        TEST_DELETED_NAME,
                        values,
                        Contract.DiaryActivity.CONTENT_URI,
                        NAME_TEST_PROJ,
                        Contract.DiaryActivity.NAME + " = ?", arrayOf(newName),
                        null
                    )
                    setCheckState(CHECK_STATE_OK)
                }
            } else {
                binding.editActivityNameTil.error =
                    resources.getString(R.string.error_name_already_used, cursor.getString(0))
                setCheckState(CHECK_STATE_ERROR)
            }
        } else {
            binding.editActivityNameTil.error = ""
            setCheckState(CHECK_STATE_OK)
        }
    }

    private class QHandler(act: EditActivity) :
        AsyncQueryHandler(MVApplication.appContext!!.contentResolver) {
        val act: EditActivity?

        /* Access only allowed via ActivityHelper.helper singleton */
        init {
            this.act = WeakReference(act).get()
        }

        override fun onQueryComplete(token: Int, cookie: Any?, cursor: Cursor) {
            when (token) {
                QUERY_NAMES -> act!!.doTokenQueryName(cursor, this)
                TEST_DELETED_NAME -> {
                    if (cookie != null) {
                        val values = cookie as ContentValues
                        if (cursor.moveToFirst()) {
                            // name already exists, choose another one
                            val triedName = values[Contract.Diary.NAME] as String
                            var newName = triedName.replaceFirst("-\\d+$".toRegex(), "")
                            val idx: String = if (triedName.length == newName.length) {
                                // no "-x" at the end so far
                                "-2"
                            } else {
                                val x = triedName.substring(newName.length + 1)
                                "-" + (x.toInt() + 1)
                            }
                            newName += idx
                            values.put(Contract.DiaryActivity.NAME, newName)
                            startQuery(
                                TEST_DELETED_NAME, values,
                                Contract.DiaryActivity.CONTENT_URI,
                                NAME_TEST_PROJ,
                                Contract.DiaryActivity.NAME + " = ?", arrayOf(newName),
                                null
                            )
                        } else {
                            // name not found, use it for the deleted one
                            val actId = values[Contract.Diary._ID] as Long
                            values.remove(Contract.Diary._ID)
                            startUpdate(
                                RENAME_DELETED_ACTIVITY, null,
                                ContentUris.withAppendedId(Contract.DiaryActivity.CONTENT_URI, actId),
                                values, Contract.Diary._ID + " = " + actId, null
                            )
                        }
                    }
                }
            }
            cursor.close()
        }

        override fun onUpdateComplete(token: Int, cookie: Any?, result: Int) {
            super.onUpdateComplete(token, cookie, result)
            if (token == RENAME_DELETED_ACTIVITY) {
                act!!.checkConstraints()
            } else {
                act!!.setCheckState(CHECK_STATE_OK)
            }
        }
    }

    /* refresh all view elements depending on currentActivity */
    private fun refreshElements() {
        if (currentActivity != null) {
            binding.editActivityName.setText(currentActivity!!.mName)
            supportActionBar!!.title = currentActivity!!.mName
            mActivityColor = currentActivity!!.mColor
        } else {
            mActivityColor = prepareColorForNextActivity()
        }
        binding.editActivityColor.setBackgroundColor(mActivityColor)
    }

    private val mQHandler = QHandler(this)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_edit_content)
        setContent(binding.root)
        setCheckState(CHECK_STATE_CHECKING)
        val i = intent
        val actId = i.getIntExtra("activityID", -1)
        //        System.out.println("ActId: " + actId);
        currentActivity = if (actId == -1) {
            null
        } else {
            ActivityHelper.helper.activityWithId(actId)
        }
        binding.editActivityName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                checkConstraints()
                //                checkSimilarNames();
            }
        })
        binding.editActivityColor.setOnClickListener { _: View? ->
            ColorPickerDialogBuilder
                .with(this@EditActivity)
                .setTitle("Choose color")
                .initialColor(R.color.activityTextColorLight)
                .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                .density(12)
                .setOnColorSelectedListener { _: Int -> }
                .setPositiveButton("ok") { _: DialogInterface?, selectedColor: Int, _: Array<Int?>? ->
                    mActivityColor = selectedColor
                    binding.editActivityColor.setBackgroundColor(mActivityColor)
                }
                .setNegativeButton("cancel") { _: DialogInterface?, _: Int -> }
                .build()
                .show()
        }
        if (savedInstanceState != null) {
            val name = savedInstanceState.getString(NAME_KEY)
            mActivityColor = savedInstanceState.getInt(COLOR_KEY)
            binding.editActivityName.setText(name)
            supportActionBar!!.title = name
            checkConstraints()
        } else {
            refreshElements()
        }
        mDrawerToggle.isDrawerIndicatorEnabled = false
        supportActionBar!!.setHomeAsUpIndicator(R.drawable.ic_close_cancel)
        checkConstraints()
    }

    public override fun onResume() {
        if (currentActivity == null) {
//            mNavigationView.getMenu().findItem(R.id.nav_add_activity).setChecked(true);
        }
        ActivityHelper.helper.registerDataChangeListener(this)
        super.onResume()
    }

    public override fun onPause() {
        super.onPause()
        ActivityHelper.helper.unregisterDataChangeListener(this)
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(NAME_KEY, Objects.requireNonNull(binding.editActivityName.text).toString())
        outState.putInt(COLOR_KEY, mActivityColor)
        // call superclass to save any view hierarchy
        super.onSaveInstanceState(outState)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.edit_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val mid = item.itemId
        if (mid == R.id.action_edit_delete) {
            if (currentActivity != null) {
                ActivityHelper.helper.deleteActivity(currentActivity!!)
            }
            finish()
        } else if (mid == R.id.action_edit_done) {
            if (checkState != CHECK_STATE_CHECKING) {
                if (checkState == CHECK_STATE_ERROR) {
                    Toast.makeText(
                        this@EditActivity,
                        binding.editActivityNameTil.error,
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    if (currentActivity == null) {
                        ActivityHelper.helper.insertActivity(
                            DiaryActivity(
                                -1, Objects.requireNonNull(
                                    binding.editActivityName.text
                                ).toString(), mActivityColor
                            )
                        )
                    } else {
                        currentActivity!!.mName = Objects.requireNonNull(binding.editActivityName.text).toString()
                        currentActivity!!.mColor = mActivityColor
                        ActivityHelper.helper.updateActivity(currentActivity!!)
                    }
                    finish()
                }
            }
        } else if (mid == android.R.id.home) {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }

    fun checkConstraints() {
        setCheckState(CHECK_STATE_CHECKING)
        if (currentActivity == null) {
            mQHandler.startQuery(
                QUERY_NAMES,
                null,
                Contract.DiaryActivity.CONTENT_URI,
                arrayOf(Contract.DiaryActivity.NAME, Contract.DiaryActivity._DELETED, Contract.DiaryActivity._ID),
                Contract.DiaryActivity.NAME + "=?",
                arrayOf(
                    Objects.requireNonNull(
                        binding.editActivityName.text
                    ).toString()
                ),
                null
            )
        } else {
            mQHandler.startQuery(
                QUERY_NAMES,
                null,
                Contract.DiaryActivity.CONTENT_URI,
                arrayOf(Contract.DiaryActivity.NAME, Contract.DiaryActivity._DELETED, Contract.DiaryActivity._ID),
                Contract.DiaryActivity.NAME + "=? AND " +
                        Contract.DiaryActivity._ID + " != ?",
                arrayOf(
                    Objects.requireNonNull(
                        binding.editActivityName.text
                    ).toString(), currentActivity!!.mId.toLong().toString()
                ),
                null
            )
        }
    }

    /**
     * Called when the data has changed and no further specification is possible.
     * => everything needs to be refreshed!
     */
    override fun onActivityDataChanged() {
        refreshElements()
    }

    /**
     * Called when the data of one activity was changed.
     *
     * @param activity
     */
    override fun onActivityDataChanged(activity: DiaryActivity) {
        if (activity === currentActivity) {
            refreshElements()
        }
    }

    /**
     * Called on addition of an activity.
     *
     * @param activity
     */
    override fun onActivityAdded(activity: DiaryActivity) {
        if (activity === currentActivity) {
            refreshElements()
        }
    }

    /**
     * Called on removale of an activity.
     *
     * @param activity
     */
    override fun onActivityRemoved(activity: DiaryActivity) {
        if (activity === currentActivity) {
            refreshElements()
            // TODO: handle deletion of the activity while in editing it...
        }
    }

    /**
     * Called on change of the current activity.
     */
    override fun onActivityChanged() {}

    /**
     * Called on change of the activity order due to likelyhood.
     */
    override fun onActivityOrderChanged() {}

    companion object {
        private const val QUERY_NAMES = 1
        private const val RENAME_DELETED_ACTIVITY = 2
        private const val TEST_DELETED_NAME = 3

        //    private static final int SIMILAR_ACTIVITY = 4;
        private const val COLOR_KEY = "COLOR"
        private const val NAME_KEY = "NAME"
        private const val CHECK_STATE_CHECKING = 0
        private const val CHECK_STATE_OK = 1

        //    private static final int CHECK_STATE_WARNING = 2;
        private const val CHECK_STATE_ERROR = 3
        private val NAME_TEST_PROJ = arrayOf(Contract.DiaryActivity.NAME)
        private fun setBtnTooltip(view: View, tooltipText: CharSequence?) {
            if (Build.VERSION.SDK_INT < 26) {
                TooltipCompat.setTooltipText(view, tooltipText)
            } else {
                view.tooltipText = tooltipText
            }
        }
    }
}