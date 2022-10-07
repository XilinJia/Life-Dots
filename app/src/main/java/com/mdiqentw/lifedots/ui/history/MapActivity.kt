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
package com.mdiqentw.lifedots.ui.history

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.database.Cursor
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.util.Pair
import androidx.databinding.DataBindingUtil
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import androidx.preference.PreferenceManager
import com.google.android.material.datepicker.MaterialDatePicker
import com.mdiqentw.lifedots.BuildConfig
import com.mdiqentw.lifedots.R
import com.mdiqentw.lifedots.databinding.ActivityMapBinding
import com.mdiqentw.lifedots.db.Contract
import com.mdiqentw.lifedots.helpers.LocationHelper
import com.mdiqentw.lifedots.ui.generic.BaseActivity
import org.osmdroid.api.IGeoPoint
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.ScaleBarOverlay
import org.osmdroid.views.overlay.simplefastpoint.LabelledGeoPoint
import org.osmdroid.views.overlay.simplefastpoint.SimpleFastPointOverlay
import org.osmdroid.views.overlay.simplefastpoint.SimpleFastPointOverlay.PointAdapter
import org.osmdroid.views.overlay.simplefastpoint.SimpleFastPointOverlayOptions
import org.osmdroid.views.overlay.simplefastpoint.SimplePointTheme
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

class MapActivity : BaseActivity(),
    LoaderManager.LoaderCallbacks<Cursor?>, MenuItem.OnMenuItemClickListener {

    lateinit var binding: ActivityMapBinding

    private var startTime = 0L
    private var endTime = 0L
    private var startPoint = GeoPoint(LocationHelper.helper.currentLocation)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ctx = applicationContext

        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
        Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID

        binding = DataBindingUtil.setContentView(this, R.layout.activity_map)
        setContent(binding.root)

        val i = intent
        val tstart = i.getLongExtra("StartTime", 0L)
        val tend = i.getLongExtra("EndTime", 0L)
        if (tstart > 0L && tend > 0L) {
            startTime = tstart
            endTime = tend
        }

        if (!hasTimeInterval()) buildMap(startPoint)

        LoaderManager.getInstance(this).initLoader(LOADER_ID_INIT, null, this)
    }

    private fun hasTimeInterval() : Boolean {
        return startTime > 0 && endTime > 0
    }

    private fun buildMap(startPoint : GeoPoint) {
        if (abs(startPoint.latitude) > 0.001 && abs(startPoint.longitude) > 0.001) {
            binding.map.setTileSource(TileSourceFactory.MAPNIK)
            binding.map.isTilesScaledToDpi = true
            binding.map.zoomController.setVisibility(CustomZoomButtonsController.Visibility.ALWAYS)
            binding.map.setMultiTouchControls(true)
            if (hasTimeInterval())
                binding.map.controller.setZoom(16.0)
            else
                binding.map.controller.setZoom(13.0)
            binding.map.controller.setCenter(startPoint)
            val copyrightOverlay = CopyrightOverlay(this)
            copyrightOverlay.setTextSize(10)
            binding.map.overlays.add(copyrightOverlay)
            val scaleBarOverlay = ScaleBarOverlay(binding.map)
            binding.map.overlays.add(scaleBarOverlay)

            // Scale bar tries to draw as 1-inch, so to put it in the top center, set x offset to
            // half screen width, minus half an inch.
            scaleBarOverlay.setScaleBarOffset(
                (resources.displayMetrics.widthPixels / 2 -
                        resources.displayMetrics.xdpi / 2).toInt(), 10
            )
            mDrawerToggle.isDrawerIndicatorEnabled = false
        } else {
            binding.noMap.visibility = View.VISIBLE
            binding.map.visibility = View.GONE
        }
    }

    // Called when a new Loader needs to be created
    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor?> {
        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return if (id == LOADER_ID_INIT) {
            var sel = SELECTION_INIT
            if (hasTimeInterval())
                sel = (sel + " AND " + Contract.DiaryLocation.TIMESTAMP + " >= " + startTime
                        + " AND " + Contract.DiaryLocation.TIMESTAMP + " <= " + endTime)

            CursorLoader(this, Contract.DiaryLocation.CONTENT_URI,
                    PROJECTION, sel, null, null)
        } else {
            CursorLoader(this)
        }
    }

    // Called when a previously created loader has finished loading
    // Here GeoPoints are drawn as markers
    @SuppressLint("Range")
    override fun onLoadFinished(loader: Loader<Cursor?>, data: Cursor?) {
        // Swap the new cursor in.  (The framework will take care of closing the
        // old cursor once we return.)
        if (data != null && data.count > 0) {
            val pts: MutableList<IGeoPoint> = ArrayList(data.count)
            val ts: MutableList<Long> = ArrayList(data.count)
            if (data.moveToFirst()) {
                var i = 0
                var aveLon = 0.0
                var aveLat = 0.0
                while (!data.isAfterLast) {
                    val haccIdx = data.getColumnIndex(Contract.DiaryLocation.HACC)
                    if (data.isNull(haccIdx) || data.getInt(haccIdx) < 1000) {
                        val lat = data.getDouble(data.getColumnIndex(Contract.DiaryLocation.LATITUDE))
                        val lon = data.getDouble(data.getColumnIndex(Contract.DiaryLocation.LONGITUDE))
                        aveLon += lon
                        aveLat += lat
                        pts.add(LabelledGeoPoint(lat, lon, "P$i"))
                        val tCol = data.getColumnIndex(Contract.DiaryLocation.TIMESTAMP)
                        ts.add(if (tCol>=0) data.getLong(tCol) else 0L)
                        i++
                    }
                    data.moveToNext()
                }
                aveLon /= i
                aveLat /= i
                if (hasTimeInterval()) {
                    startPoint = GeoPoint(aveLat, aveLon)
                    buildMap(startPoint)
                }

                Toast.makeText(
                    binding.map.context,
                    i.toString() + getString(R.string.num_valid_points),
                    Toast.LENGTH_LONG
                ).show()
            }

            // wrap them in a theme
            val pt = SimplePointTheme(pts, true)

            // create label style
            val textStyle = Paint()
            textStyle.style = Paint.Style.FILL
            textStyle.color = Color.parseColor("#0000ff")
            textStyle.textAlign = Paint.Align.CENTER
            textStyle.textSize = 24f

            // set some visual options for the overlay
            // we use here MAXIMUM_OPTIMIZATION algorithm, which works well with >100k points
            val opt = SimpleFastPointOverlayOptions.getDefaultStyle()
                    .setAlgorithm(SimpleFastPointOverlayOptions.RenderingAlgorithm.MAXIMUM_OPTIMIZATION)
                    .setRadius(7f).setIsClickable(true).setCellSize(15).setTextStyle(textStyle)

            // create the overlay with the theme
            val sfpo = SimpleFastPointOverlay(pt, opt)

            // onClick callback
            sfpo.setOnClickListener { points: PointAdapter, point: Int? ->
                val date = Date(ts[point!!])
                val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

                // selected item is deleted. Ask for undeleting it.
                val builder = AlertDialog.Builder(this)
                    .setMessage((points[point] as LabelledGeoPoint).label + ": " + format.format(date) +
                    "\n" + getString(R.string.review_that_day))
                    .setPositiveButton(android.R.string.yes) { _: DialogInterface?, _: Int ->
                        val hist = Intent(this, HistoryActivity::class.java)
                        hist.putExtra("StartTime", ts[point] - 12*60*60*1000)
                        hist.putExtra("EndTime", ts[point] + 12*60*60*1000)
                        startActivity(hist)
                    }
                    .setNegativeButton(android.R.string.no, null)
                builder.create().show()
            }

            binding.map.overlays.add(sfpo)

            binding.noMap.visibility = View.GONE
            binding.map.visibility = View.VISIBLE
        } else {
            Toast.makeText(
                binding.map.context,
                getString(R.string.no_valid_points),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Called when a previously created loader is reset, making the data unavailable
    override fun onLoaderReset(loader: Loader<Cursor?>) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed.  We need to make sure we are no
        // longer using it.
    }

    override fun onCreateOptionsMenu(menu: Menu) : Boolean {
        val inflater = getMenuInflater ()
        inflater.inflate(R.menu.map_menu, menu)

        val datesMenuItem = menu.findItem (R.id.menu_dates)
        datesMenuItem.setOnMenuItemClickListener(this)

        return true
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        val mid = item.itemId

        if (mid == R.id.menu_dates) {
            val picker = MaterialDatePicker.Builder.dateRangePicker().build()
            picker.show(supportFragmentManager, picker.toString())
            picker.addOnPositiveButtonClickListener { selection: Pair<Long, Long> ->
                binding.map.getOverlays().clear()
                binding.map.invalidate()
                startTime = selection.first
                endTime = selection.second
                LoaderManager.getInstance(this@MapActivity).restartLoader(LOADER_ID_INIT, null, this@MapActivity)
            }
        }
        return true
    }

    public override fun onResume() {
        mNavigationView.menu.findItem(R.id.nav_map).isChecked = true
        super.onResume()
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        binding.map.onResume() //needed for compass, my location overlays, v6.0.0 and up
    }

    public override fun onPause() {
        super.onPause()
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
        binding.map.onPause() //needed for compass, my location overlays, v6.0.0 and up
    }

    companion object {
        private const val LOADER_ID_INIT = 0
        private val PROJECTION = arrayOf(
            Contract.DiaryLocation.TIMESTAMP,
            Contract.DiaryLocation.LONGITUDE,
            Contract.DiaryLocation.LATITUDE,
            Contract.DiaryLocation.HACC
        )
        private const val SELECTION_INIT = Contract.DiaryLocation._DELETED + "=0"
    }
}