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
package com.mdiqentw.lifedots.helpers

import android.Manifest
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.*
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.*
import android.os.Handler.Callback
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.mdiqentw.lifedots.MVApplication
import com.mdiqentw.lifedots.db.Contract
import com.mdiqentw.lifedots.ui.settings.SettingsActivity
import kotlin.math.*


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
class LocationHelper : AsyncQueryHandler(MVApplication.getAppContext().contentResolver),
    LocationListener, OnSharedPreferenceChangeListener {

    private var minTime: Long = 0
    private var minDist = 0f
    private var setting: String? = null
    var currentLocation: Location private set
    private val locationManager: LocationManager = MVApplication.getAppContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(MVApplication.getAppContext())
    private val mHandler: Handler

    private lateinit var refreshJobInfo: JobInfo

    fun updateLocation() {
        if (setting == "off") return

        var permissionCheckFine = PackageManager.PERMISSION_DENIED
        var permissionCheckCoarse = PackageManager.PERMISSION_DENIED
        if (setting == "gps" && locationManager.allProviders.contains(LocationManager.GPS_PROVIDER)) {
            permissionCheckFine = ContextCompat.checkSelfPermission(MVApplication.getAppContext(),
                Manifest.permission.ACCESS_FINE_LOCATION)
            permissionCheckCoarse = permissionCheckFine
        } else if (locationManager.allProviders.contains(LocationManager.NETWORK_PROVIDER)) {
            permissionCheckCoarse = ContextCompat.checkSelfPermission(MVApplication.getAppContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        var locationProvider = ""
        if (permissionCheckFine == PackageManager.PERMISSION_GRANTED)
            locationProvider = LocationManager.GPS_PROVIDER
        else if (permissionCheckCoarse == PackageManager.PERMISSION_GRANTED)
            locationProvider = LocationManager.NETWORK_PROVIDER

        if (locationProvider.isNotBlank()) {
            val location = locationManager.getLastKnownLocation(locationProvider)
            var locAge = 0L
            if (location != null) {
                locAge = System.currentTimeMillis() - location.time
//                println("LastLocation: " + location.longitude + ":" + location.latitude + " " + locAge)

                if (isAtZero(currentLocation)) {
                    if (locAge < minTime) {
                        onLocationChanged(location)
//                        println("currentLocation recorded: " + location.longitude + ":" + location.latitude)
                    } else {
                        currentLocation = location
                    }
                }
            }
//            if (location == null ||
//                locAge > 0.5*minTime ||
//                distance(location, currentLocation) > 0.5*minDist) {
//                locationManager.requestLocationUpdates(
//                    locationProvider,
//                    60000L,
//                    5f,
//                    this,
//                    Looper.getMainLooper()
//                )
////                println("requestLocationUpdates sent")
//                mHandler.sendEmptyMessageDelayed(
//                    LOCATION_UPDATE,
//                    5 * MIN_TIME_FACTOR
//                ) // time out in 5 minutes
//            }
            if (location == null ||
                locAge > 0.5*minTime ||
                distance(location, currentLocation) > 0.5*minDist) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//                    println("calling getCurrentLocation")
                    locationManager.getCurrentLocation(locationProvider,
                        null,
                        MVApplication.getAppContext().mainExecutor,
                        {
                            fun accept(location: Location) {
                                onLocationChanged(location)
                            }
                        })
                } else {
//                    println("calling requestSingleUpdate")
                    @Suppress("DEPRECATION")
                    locationManager.requestSingleUpdate(
                        locationProvider, this, Looper.getMainLooper())
                }
            }
        }
    }

    private fun isAtZero(loc: Location) : Boolean {
        return abs(loc.latitude) < 0.001 && abs(loc.longitude) < 0.001
    }

    /**
     * Calculate distance between two points in latitude and longitude taking
     * into account height difference. If you are not interested in height
     * difference pass 0.0. Uses Haversine method as its base.
     *
     * lat1, lon1 Start point lat2, lon2 End point el1 Start altitude in meters
     * el2 End altitude in meters
     * @returns Distance in Meters
     */
    private fun distance(loc1: Location, loc2: Location): Double {
        val lon1 = loc1.longitude
        val lat1 = loc1.latitude
        val lon2 = loc2.longitude
        val lat2 = loc2.latitude
        val el1 = 0.0
        val el2 = 0.0

        val ra = 6371 // Radius of the earth

        val latDistance = Math.toRadians(lat2 - lat1)
        val lonDistance = Math.toRadians(lon2 - lon1)
        val a = (sin(latDistance / 2) * sin(latDistance / 2)
                + (cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2))
                * sin(lonDistance / 2) * sin(lonDistance / 2)))
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        var distance: Double = ra * c * 1000 // convert to meters

        val height: Double = el1 - el2

        distance = distance.pow(2.0) + height.pow(2.0)

        return sqrt(distance)
    }

    private fun stopLocationUpdates() {
        locationManager.removeUpdates(this)
        mHandler.removeMessages(LOCATION_UPDATE)
    }

    /**
     * Called when the location has changed.
     *
     *  There are no restrictions on the use of the supplied Location object.
     *
     * @param location The new location, as a Location object.
     */
    override fun onLocationChanged(location: Location) {
        stopLocationUpdates()

//        println("onLocationChanged: " + location.time + " " + location.longitude + " " + location.latitude)

        @Suppress("SENSELESS_COMPARISON", "DEPRECATION")
        if (location == null || isAtZero(location)) return
        if (distance(location, currentLocation) < minDist) return

//        println("Adding location point: " + location.time + " " + location.longitude + " " + location.latitude)
        val values = ContentValues()
        currentLocation = location
        values.put(Contract.DiaryLocation.TIMESTAMP, location.time)
        values.put(Contract.DiaryLocation.LATITUDE, location.latitude)
        values.put(Contract.DiaryLocation.LONGITUDE, location.longitude)

        if (location.hasAccuracy()) {
            values.put(Contract.DiaryLocation.HACC, (location.accuracy * 10).roundToInt())
        }
        if (location.hasSpeed()) {
            values.put(Contract.DiaryLocation.SPEED, location.speed)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (location.hasSpeedAccuracy()) {
                    values.put(Contract.DiaryLocation.SACC,
                        (location.speedAccuracyMetersPerSecond * 10).roundToInt()
                    )
                }
            }
        }
        if (location.hasAltitude()) {
            values.put(Contract.DiaryLocation.ALTITUDE, location.altitude)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (location.hasVerticalAccuracy()) {
                    values.put(Contract.DiaryLocation.VACC,
                        (location.verticalAccuracyMeters * 10).roundToInt()
                    )
                }
            }
        }

        startInsert(0, null, Contract.DiaryLocation.CONTENT_URI, values)
    }

    // deprecated for Android Q
    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}

    /**
     * Called when the provider is enabled by the user.
     *
     * @param provider the name of the location provider associated with this
     * update.
     */
    override fun onProviderEnabled(provider: String) {}

    /**
     * Called when the provider is disabled by the user. If requestLocationUpdates
     * is called on an already disabled provider, this method is called
     * immediately.
     *
     * @param provider the name of the location provider associated with this
     * update.
     */
    override fun onProviderDisabled(provider: String) {}

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if (key == SettingsActivity.KEY_PREF_USE_LOCATION ||
            key == SettingsActivity.KEY_PREF_LOCATION_AGE ||
            key == SettingsActivity.KEY_PREF_LOCATION_DIST) {
            loadFromPreferences()
            updateLocation()
        }
    }

    private fun loadFromPreferences() {
        try {
            setting = sharedPreferences.getString(SettingsActivity.KEY_PREF_USE_LOCATION, "off")
            val minTimeS = sharedPreferences.getString(SettingsActivity.KEY_PREF_LOCATION_AGE, (MIN_TIME_DEF).toString())
            minTime = MIN_TIME_FACTOR * (minTimeS!!.toLong())
            val minDistS = sharedPreferences.getString(SettingsActivity.KEY_PREF_LOCATION_DIST, MIN_DISTANCE_DEF.toString())
            minDist = minDistS!!.toFloat()
        } catch (e: NumberFormatException) {
            /* no change in settings on invalid config */
        }
    }

    fun scheduleRefresh() {
        val componentName = ComponentName(MVApplication.getAppContext(), RefreshService::class.java)
        val builder = JobInfo.Builder(ACTIVITY_HELPER_REFRESH_JOB, componentName)
        builder.setMinimumLatency(minTime)
        refreshJobInfo = builder.build()
//        println("Job scheduled: $minTime")
        val jobScheduler = MVApplication.getAppContext()
            .getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        val resultCode = jobScheduler.schedule(refreshJobInfo)
        if (resultCode != JobScheduler.RESULT_SUCCESS) {
            Log.w(TAG, "RefreshJob not scheduled")
        }
    }

    companion object {
        private val TAG = LocationHelper::class.java.name
        @JvmField
        val helper = LocationHelper()
        private const val LOCATION_UPDATE = 1
        private const val MIN_TIME_DEF: Long = 5 // for now every 5 minutes
        private const val MIN_TIME_FACTOR = (1000 * 60).toLong()
        private const val MIN_DISTANCE_DEF = 50.0f
        private const val ACTIVITY_HELPER_REFRESH_JOB = 0
    }

    init {
        mHandler = Handler(Looper.myLooper()!!, Callback { msg: Message ->
            if (msg.what == LOCATION_UPDATE) {
                stopLocationUpdates()
                return@Callback true
            }
            false
        })
//        println("location providers: " + locationManager.allProviders)
        currentLocation = Location("DiaryLocation")
//        println("currentLocation: " + currentLocation.longitude + ":" + currentLocation.latitude)
        loadFromPreferences()
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        scheduleRefresh()
    }
}