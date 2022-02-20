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
package com.mdiqentw.lifedots.ui.history

import android.annotation.SuppressLint
import android.database.Cursor
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.databinding.DataBindingUtil
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import androidx.preference.PreferenceManager
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.charts.ScatterChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.interfaces.datasets.IScatterDataSet
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.mdiqentw.lifedots.MVApplication
import com.mdiqentw.lifedots.R
import com.mdiqentw.lifedots.databinding.ActivityAnalyticsBinding
import com.mdiqentw.lifedots.db.LocalDBHelper
import com.mdiqentw.lifedots.db.Contract
import com.mdiqentw.lifedots.helpers.DateHelper
import com.mdiqentw.lifedots.helpers.TimeSpanFormatter
import com.mdiqentw.lifedots.ui.generic.BaseActivity
import com.mdiqentw.lifedots.ui.history.HistoryDetailActivity.DatePickerFragment
import org.osmdroid.config.Configuration
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

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
class AnalyticsActivity : BaseActivity(), LoaderManager.LoaderCallbacks<Cursor?>, AdapterView.OnItemSelectedListener, OnChartValueSelectedListener {
    private var chartFrame: FrameLayout? = null
    private var pieChart: PieChart? = null
    private var timeFramePosition = 0
    private var bnbAct: Bundle? = null
    private var pieMax = 100.0f
    private var pieMin = 5.0f
    var pieUnit = true
    private var xyChart: ScatterChart? = null
    private var startOfTime = 0L
    private var useXYChart = false
    private var timeframeSpinner: Spinner? = null
    private var rangeTextView: TextView? = null
    private var rangeEarlierImageView: ImageView? = null
    private var rangeLaterImageView: ImageView? = null
    private var currentDateTime: Long = 0
    private var currentOffset = 0
    private var currentRange = Calendar.WEEK_OF_YEAR

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ctx = applicationContext
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
        val binding: ActivityAnalyticsBinding = DataBindingUtil.setContentView(this, R.layout.activity_analytics)
        setContent(binding.root)
        timeframeSpinner = binding.timeframeSpinner
        timeframeSpinner!!.onItemSelectedListener = this
        val adapter = ArrayAdapter.createFromResource(this,
                R.array.statistic_dropdown,
                android.R.layout.simple_spinner_item)
        timeframeSpinner!!.adapter = adapter
        LoaderManager.getInstance(this).initLoader(LOADER_ID_TIME, null, this)
        getStartOfTime()
        xyChart = ScatterChart(ctx)
        xyChart!!.axisRight.isEnabled = false
        xyChart!!.legend.isEnabled = true
        xyChart!!.legend.textSize = 16f
        xyChart!!.description.text = "Time Summary Per Day"
        xyChart!!.description.textSize = 16f
        xyChart!!.setOnChartValueSelectedListener(this)
        pieChart = PieChart(ctx)
        pieChart!!.legend.isEnabled = false
        pieChart!!.description = null
        pieChart!!.holeRadius = 30.0f
        pieChart!!.transparentCircleRadius = 40.0f
        pieChart!!.setOnChartValueSelectedListener(this)
        chartFrame = binding.chartFrame
        chartFrame!!.addView(pieChart)
        rangeTextView = binding.rangeTextView
        rangeEarlierImageView = binding.imgEarlier
        rangeEarlierImageView!!.setOnClickListener { loadRange(currentRange, --currentOffset) }
        rangeLaterImageView = binding.imgLater
        rangeLaterImageView!!.setOnClickListener { loadRange(currentRange, ++currentOffset) }
        currentDateTime = Date().time
        mDrawerToggle.isDrawerIndicatorEnabled = false
    }

    @SuppressLint("Range")
    private fun getStartOfTime() {
        val db = mOpenHelper.readableDatabase
        val cursor = db.query(Contract.Diary.TABLE_NAME, arrayOf("start"),
                "rowid = 1", null, null, null, null, null)
        if (cursor != null && cursor.moveToFirst()) {
            startOfTime = cursor.getLong(cursor.getColumnIndex("start"))
            cursor.close()
        }
    }

    // Called when a new Loader needs to be created
    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor?> {
        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return when (id) {
            LOADER_ID_TIME -> {
                CursorLoader(this,
                        Contract.DiaryStats.CONTENT_URI,
                        PROJECTION,
                        null,
                        null,
                        Contract.DiaryStats.SORT_ORDER_DEFAULT)
            }
            LOADER_ID_RANGE -> {
                val start = args!!.getLong("start")
                val end = args.getLong("end")
                var u = Contract.DiaryStats.CONTENT_URI
                u = Uri.withAppendedPath(u, start.toString())
                u = Uri.withAppendedPath(u, end.toString())
                CursorLoader(this,
                        u,
                        PROJECTION,
                        null,
                        null,
                        Contract.DiaryStats.PORTION + " DESC")
            }
            else -> {
                CursorLoader(this)
            }
        }
    }

    // Called when a previously created loader has finished loading
    override fun onLoadFinished(loader: Loader<Cursor?>, data: Cursor?) {
        // Swap the new cursor in.  (The framework will take care of closing the
        // old cursor once we return.)
        if (data != null) {
            val portionIdx = data.getColumnIndex(Contract.DiaryStats.PORTION)
            val nameIdx = data.getColumnIndex(Contract.DiaryStats.NAME)
            val colIdx = data.getColumnIndex(Contract.DiaryStats.COLOR)
            val durIdx = data.getColumnIndex(Contract.DiaryStats.DURATION)
            if (!useXYChart) {
                val entries: MutableList<PieEntry> = ArrayList(50)
                val colors: MutableList<Int> = ArrayList(50)
                if (data.moveToFirst()) {
                    var acc = 0.0f
                    while (!data.isAfterLast) {
                        val portion = data.getFloat(portionIdx)
                        var pieNumber = data.getFloat(durIdx)
                        if (pieUnit) {
                            pieNumber = ((portion * 10000).roundToInt() / 100).toFloat()
                        }
                        if (portion <= pieMax && portion > pieMin) {
                            val ent = PieEntry(pieNumber, data.getString(nameIdx))
                            entries.add(ent)
                            colors.add(data.getInt(colIdx))
                        } else if (portion <= pieMin) {
                            // accumulate the small, not shown entries
                            acc += pieNumber
                        }
                        data.moveToNext()
                    }
                    entries.add(PieEntry(acc, resources.getString(R.string.statistics_others)))
                    colors.add(Color.GRAY)
                }
                val set = PieDataSet(entries, resources.getString(R.string.activities))
                val dat = PieData(set)
                dat.setValueTextSize(13f)
                set.colors = colors
                set.valueFormatter = MyValueFormatter1()
                pieChart!!.data = dat
                pieChart!!.setUsePercentValues(false)
                pieChart!!.rotationAngle = 180.0f
                pieChart!!.invalidate() // refresh
            }
        }
    }

    // Called when a previously created loader is reset, making the data unavailable
    override fun onLoaderReset(loader: Loader<Cursor?>) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed.  We need to make sure we are no
        // longer using it.
    }

    public override fun onResume() {
        mNavigationView.menu.findItem(R.id.nav_statistics).isChecked = true
        super.onResume()
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {}
    override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        if (parent.id == R.id.timeframeSpinner) {
            currentDateTime = Date().time
            bnbAct = Bundle()
            timeFramePosition = position
            pieMax = 100f
            pieMin = 5f
            when (position) {
                0 -> {}
                1 -> {
                    bnbAct!!.putLong("start", currentDateTime - MS_Per_Day * 7)
                    bnbAct!!.putLong("end", currentDateTime)
                }
                2 -> {
                    bnbAct!!.putLong("start", currentDateTime - MS_Per_Day * 30)
                    bnbAct!!.putLong("end", currentDateTime)
                }
                3 -> {
                    currentOffset = 0
                    currentRange = Calendar.DAY_OF_YEAR
                }
                4 -> {
                    currentOffset = 0
                    currentRange = Calendar.WEEK_OF_YEAR
                }
                5 -> {
                    currentOffset = 0
                    currentRange = Calendar.MONTH
                }
                6 -> {
                    currentOffset = 0
                    currentRange = Calendar.YEAR
                }
                else -> {}
            }
            resetRangeTextView()
        }
        reload()
    }

    private fun resetRangeTextView() {
        if (timeFramePosition < 3 || useXYChart) {
            rangeTextView!!.visibility = View.INVISIBLE
            rangeEarlierImageView!!.visibility = View.INVISIBLE
            rangeLaterImageView!!.visibility = View.INVISIBLE
        } else {
            rangeTextView!!.visibility = View.VISIBLE
            rangeEarlierImageView!!.visibility = View.VISIBLE
            rangeLaterImageView!!.visibility = View.VISIBLE
        }
    }

    private fun reload() {
        when {
            timeFramePosition < 1 -> {
                LoaderManager.getInstance(this).restartLoader(LOADER_ID_TIME, bnbAct, this)
            }
            timeFramePosition < 3 -> {
                LoaderManager.getInstance(this).restartLoader(LOADER_ID_RANGE, bnbAct, this)
            }
            else -> {
                loadRange(currentRange, currentOffset)
            }
        }
    }

    /* field is the field of Calender, e.g. Calendar.WEEK_OF_YEAR */
    private fun loadRange(field: Int, offset: Int) {
        val bnd = Bundle()
        val calStart = DateHelper.startOf(field, currentDateTime)
        calStart.add(field, offset)
        val calEnd = calStart.clone() as Calendar
        calEnd.add(field, 1)
        val sdf = DateHelper.dateFormat(field)
        val tt = sdf.format(calStart.time)
        rangeTextView!!.text = tt
        bnd.putLong("start", calStart.timeInMillis)
        bnd.putLong("end", calEnd.timeInMillis)
        LoaderManager.getInstance(this).restartLoader(LOADER_ID_RANGE, bnd, this)
    }

    fun showDatePickerDialog() {
        val newFragment = DatePickerFragment()
        val date = Calendar.getInstance()
        date.timeInMillis = currentDateTime
        newFragment.setData({ _: DatePicker?, year: Int, month: Int, dayOfMonth: Int ->
            date[Calendar.YEAR] = year
            date[Calendar.MONTH] = month
            date[Calendar.DAY_OF_MONTH] = dayOfMonth
            currentDateTime = date.timeInMillis
            currentOffset = 0
            loadRange(currentRange, currentOffset)
        }, date[Calendar.YEAR], date[Calendar.MONTH], date[Calendar.DAY_OF_MONTH])
        newFragment.show(supportFragmentManager, "startDatePicker")
    }

    private fun switchToPieChart() {
        // can't remove the xyChart here because it still needs to handle this touch event
//            chartFrame.removeView(pieChart);
        xyChart!!.clear()
        chartFrame!!.addView(pieChart)
        timeframeSpinner!!.visibility = View.VISIBLE
        //            chartFrame.removeView(xyChart);
    }

    private fun switchToXYChart() {
        chartFrame!!.removeView(xyChart)
        chartFrame!!.addView(xyChart)
        chartFrame!!.removeView(pieChart)
    }

    // implementing the pie chart listener
    override fun onNothingSelected() {
        if (useXYChart) {
            useXYChart = false
            switchToPieChart()
        } else {
            if (pieMax < 99f) {
                pieMax = 100f
                pieMin = 5f
                reload()
            }
            pieUnit = !pieUnit
            reload()
        }
        resetRangeTextView()
    }

    @SuppressLint("Range")
    override fun onValueSelected(e: Entry, h: Highlight) {
        if (!useXYChart) {
            val pe = e as PieEntry
            if (pe.label == "Others") {
                pieMax = pieMin
                pieMin = 0.2f * pieMax
                reload()
            } else {
                useXYChart = true
                timeframeSpinner!!.visibility = View.GONE
                val db = mOpenHelper.readableDatabase
                var cursor = db.query(Contract.DiaryActivity.TABLE_NAME, arrayOf("*"),
                        "name = ?", arrayOf(pe.label), null, null, null, null)
                var actID = ""
                var actColor = 0
                if (cursor != null && cursor.moveToFirst()) {
                    actID = cursor.getString(cursor.getColumnIndex("_id"))
                    actColor = cursor.getInt(cursor.getColumnIndex("color"))
                    cursor.close()
                }
                val currentChartStep = Calendar.DAY_OF_YEAR
                val calStart = DateHelper.startOf(currentChartStep, startOfTime)
                val calEnd = calStart.clone() as Calendar
                calEnd.add(currentChartStep, 1)
                val valueFormatter: ValueFormatter = MyValueFormatter2()
                cursor = db.query(Contract.Diary.TABLE_NAME, arrayOf("start", "end"),
                        "act_id = ?", arrayOf(actID), null, null, null, null)
                val segEntries = ArrayList<Entry>(50)
                val dates = ArrayList<String>(50)
                if (cursor != null && cursor.moveToFirst()) {
                    var actStart: Long
                    var actEnd: Long
                    var startMS = calStart.timeInMillis
                    var endMS = calEnd.timeInMillis
                    var offset = 0
                    while (startMS < currentDateTime) {
                        var actSum = 0L
                        if (!cursor.isAfterLast) {
                            do {
                                actStart = cursor.getLong(cursor.getColumnIndex("start"))
                                if (actStart > endMS) {
                                    break
                                }
                                actEnd = cursor.getLong(cursor.getColumnIndex("end"))
                                if (actEnd < startMS) {
                                    continue
                                }
                                actSum += min(endMS, actEnd) - max(startMS, actStart)
                                if (actEnd > endMS) {
                                    break
                                }
                            } while (cursor.moveToNext())
                        }
                        segEntries.add(Entry(offset.toFloat(), actSum.toFloat()))
                        dates.add(String.format("%d-%d-%d", calStart[Calendar.MONTH] + 1, calStart[Calendar.DAY_OF_MONTH], calStart[Calendar.YEAR]))
                        offset += 1
                        calStart.add(currentChartStep, 1)
                        calEnd.add(currentChartStep, 1)
                        startMS = calStart.timeInMillis
                        endMS = calEnd.timeInMillis
                    }
                    cursor.close()
                }
                val dataSets: MutableList<IScatterDataSet> = ArrayList(50)
                val set = ScatterDataSet(segEntries, pe.label)
                set.valueFormatter = valueFormatter
                set.color = actColor
                set.setDrawValues(false)
                dataSets.add(set)
                val data = ScatterData(dataSets)
                xyChart!!.data = data
                xyChart!!.axisLeft.valueFormatter = valueFormatter
                val xAxis = xyChart!!.xAxis
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.setDrawAxisLine(true)
                xAxis.setDrawLabels(true)
                xAxis.labelRotationAngle = 60f
                xAxis.valueFormatter = MyValueFormatter(dates)
                xyChart!!.invalidate()
                switchToXYChart()
            }
        } else {
            useXYChart = false
            switchToPieChart()
        }
        resetRangeTextView()
    }

    private class MyValueFormatter(private val dates: ArrayList<String>) : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            val intVal = value.toInt()
            return if (intVal < dates.size) dates[value.toInt()] else ""
        }
    }

    private class MyValueFormatter1 : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            return TimeSpanFormatter.format(value.toLong())
        }
    }

    private class MyValueFormatter2 : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            return TimeSpanFormatter.format(value.toLong())
        }
    }

    companion object {
        private const val LOADER_ID_TIME = 0
        private const val LOADER_ID_RANGE = 1
        const val MS_Per_Day = (1000 * 60 * 60 * 24).toLong()
        private val PROJECTION = arrayOf(
                Contract.DiaryStats.NAME,
                Contract.DiaryStats.COLOR,
                Contract.DiaryStats.PORTION,
                Contract.DiaryStats.DURATION
        )
        val mOpenHelper = LocalDBHelper(MVApplication.getAppContext())
    }
}