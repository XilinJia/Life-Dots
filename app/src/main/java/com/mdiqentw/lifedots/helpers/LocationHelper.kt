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
import android.content.AsyncQueryHandler
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.*
import android.os.Handler.Callback
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.mdiqentw.lifedots.MVApplication
import com.mdiqentw.lifedots.db.Contract
import com.mdiqentw.lifedots.ui.settings.SettingsActivity

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
class LocationHelper : AsyncQueryHandler(MVApplication.getAppContext().contentResolver), LocationListener, OnSharedPreferenceChangeListener {
    private var minTime: Long = 0
    private var minDist = 0f
    private var setting: String? = null
    var currentLocation: Location
        private set
    private val locationManager: LocationManager
    private val sharedPreferences: SharedPreferences
    private val mHandler: Handler
    fun updateLocation() {
        if (setting == "off") {
            // do nothing
        } else {
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
            if (permissionCheckFine == PackageManager.PERMISSION_GRANTED) {
                val locationProvider = LocationManager.GPS_PROVIDER
                locationManager.requestLocationUpdates(locationProvider, minTime, minDist, this, Looper.getMainLooper())
                mHandler.sendEmptyMessageDelayed(LOCATION_UPDATE, 5 * MIN_TIME_FACTOR) // time out in 5 minutes
            } else if (permissionCheckCoarse == PackageManager.PERMISSION_GRANTED) {
                val locationProvider = LocationManager.NETWORK_PROVIDER
                locationManager.requestLocationUpdates(locationProvider, minTime, minDist, this, Looper.getMainLooper())
                //                mHandler.sendEmptyMessageDelayed(LOCATION_UPDATE, timeOut);
            }
        }
    }

    fun stopLocationUpdates() {
        locationManager.removeUpdates(this)
        mHandler.removeMessages(LOCATION_UPDATE)
    }

    /**
     * Called when the location has changed.
     *
     *
     *
     *  There are no restrictions on the use of the supplied Location object.
     *
     * @param location The new location, as a Location object.
     */
    override fun onLocationChanged(location: Location) {
        stopLocationUpdates()
        val values = ContentValues()
        currentLocation = location
        values.put(Contract.DiaryLocation.TIMESTAMP, location.time)
        values.put(Contract.DiaryLocation.LATITUDE, location.latitude)
        values.put(Contract.DiaryLocation.LONGITUDE, location.longitude)
        if (location.hasAccuracy()) {
            values.put(Contract.DiaryLocation.HACC, Math.round(location.accuracy * 10))
        }
        if (location.hasSpeed()) {
            values.put(Contract.DiaryLocation.SPEED, location.speed)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (location.hasSpeedAccuracy()) {
                    values.put(Contract.DiaryLocation.SACC, Math.round(location.speedAccuracyMetersPerSecond * 10))
                }
            }
        }
        if (location.hasAltitude()) {
            values.put(Contract.DiaryLocation.ALTITUDE, location.altitude)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (location.hasVerticalAccuracy()) {
                    values.put(Contract.DiaryLocation.VACC, Math.round(location.verticalAccuracyMeters * 10))
                }
            }
        }
        startInsert(0, null, Contract.DiaryLocation.CONTENT_URI,
                values)
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
        if (key == SettingsActivity.KEY_PREF_USE_LOCATION || key == SettingsActivity.KEY_PREF_LOCATION_AGE || key == SettingsActivity.KEY_PREF_LOCATION_DIST) {
            updatePreferences()
            updateLocation()
        }
    }

    fun updatePreferences() {
        try {
            setting = sharedPreferences.getString(SettingsActivity.KEY_PREF_USE_LOCATION, "off")
            val minTimeS = sharedPreferences.getString(SettingsActivity.KEY_PREF_LOCATION_AGE, (MIN_TIME_DEF * MIN_TIME_FACTOR).toString())
            minTime = minTimeS!!.toLong()
            val minDistS = sharedPreferences.getString(SettingsActivity.KEY_PREF_LOCATION_DIST, MIN_DISTANCE_DEF.toString())
            minDist = minDistS!!.toFloat()
        } catch (e: NumberFormatException) {
            /* no change in settings on invalid config */
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
    }

    init {
        locationManager = MVApplication.getAppContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        //        System.out.println("location providers available: " + locationManager.getAllProviders());
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(MVApplication.getAppContext())
        mHandler = Handler(Looper.myLooper()!!, Callback { msg: Message ->
            if (msg.what == LOCATION_UPDATE) {
                stopLocationUpdates()
                return@Callback true
            }
            false
        })
        currentLocation = Location("DiaryLocation")
        updatePreferences()
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }
}