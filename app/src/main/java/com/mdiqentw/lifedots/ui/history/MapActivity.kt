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
import android.graphics.Paint
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import androidx.preference.PreferenceManager
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
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.ScaleBarOverlay
import org.osmdroid.views.overlay.simplefastpoint.LabelledGeoPoint
import org.osmdroid.views.overlay.simplefastpoint.SimpleFastPointOverlay
import org.osmdroid.views.overlay.simplefastpoint.SimpleFastPointOverlay.PointAdapter
import org.osmdroid.views.overlay.simplefastpoint.SimpleFastPointOverlayOptions
import org.osmdroid.views.overlay.simplefastpoint.SimplePointTheme

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
class MapActivity : BaseActivity(), LoaderManager.LoaderCallbacks<Cursor?> {
    var map: MapView? = null
    private var noMap: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ctx = applicationContext

        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
        Configuration.getInstance().setUserAgentValue(BuildConfig.APPLICATION_ID)

        val binding: ActivityMapBinding = DataBindingUtil.setContentView(this, R.layout.activity_map)
        setContent(binding.root)

        noMap = binding.noMap
        map = binding.map
        map!!.setTileSource(TileSourceFactory.MAPNIK)
        map!!.isTilesScaledToDpi = true
        map!!.zoomController.setVisibility(CustomZoomButtonsController.Visibility.ALWAYS)
        map!!.setMultiTouchControls(true)
        val mapController = map!!.controller
        mapController.setZoom(14.0)
        val startPoint = GeoPoint(LocationHelper.helper.currentLocation)
        mapController.setCenter(startPoint)
        val copyrightOverlay = CopyrightOverlay(this)
        copyrightOverlay.setTextSize(10)
        map!!.overlays.add(copyrightOverlay)
        val scaleBarOverlay = ScaleBarOverlay(map)
        map!!.overlays.add(scaleBarOverlay)
        // Scale bar tries to draw as 1-inch, so to put it in the top center, set x offset to
        // half screen width, minus half an inch.
        scaleBarOverlay.setScaleBarOffset(
                (resources.displayMetrics.widthPixels / 2 - resources
                        .displayMetrics.xdpi / 2).toInt(), 10)
        LoaderManager.getInstance(this).initLoader(LOADER_ID_INIT, null, this)
        mDrawerToggle.isDrawerIndicatorEnabled = false
    }

    // Called when a new Loader needs to be created
    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor?> {
        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return if (id == LOADER_ID_INIT) {
            CursorLoader(this, Contract.DiaryLocation.CONTENT_URI,
                    PROJECTION, SELECTION_INIT, null, null)
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
            if (data.moveToFirst()) {
                var i = 0
                while (!data.isAfterLast) {
                    val haccIdx = data.getColumnIndex(Contract.DiaryLocation.HACC)
                    if (data.isNull(haccIdx) || data.getInt(haccIdx) < 250) {
                        pts.add(LabelledGeoPoint(data.getDouble(data.getColumnIndex(Contract.DiaryLocation.LATITUDE)),
                                data.getDouble(data.getColumnIndex(Contract.DiaryLocation.LONGITUDE)), "Pt $i"))
                    }
                    i++
                    data.moveToNext()
                }
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
                Toast.makeText(map!!.context, "You clicked " + (points[point!!] as LabelledGeoPoint).label, Toast.LENGTH_SHORT).show()
            }

            // add overlay
            map!!.overlays.add(sfpo)
            noMap!!.visibility = View.GONE
            map!!.visibility = View.VISIBLE
        } else {
            noMap!!.visibility = View.VISIBLE
            map!!.visibility = View.GONE
        }
    }

    // Called when a previously created loader is reset, making the data unavailable
    override fun onLoaderReset(loader: Loader<Cursor?>) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed.  We need to make sure we are no
        // longer using it.
    }

    public override fun onResume() {
        mNavigationView.menu.findItem(R.id.nav_map).isChecked = true
        super.onResume()
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        map!!.onResume() //needed for compass, my location overlays, v6.0.0 and up
    }

    public override fun onPause() {
        super.onPause()
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
        map!!.onPause() //needed for compass, my location overlays, v6.0.0 and up
    }

    companion object {
        private const val LOADER_ID_INIT = 0
        private val PROJECTION = arrayOf(
                Contract.DiaryLocation.LONGITUDE,
                Contract.DiaryLocation.LATITUDE,
                Contract.DiaryLocation.HACC
        )
        private const val SELECTION_INIT = Contract.DiaryLocation._DELETED + "=0"
    }
}