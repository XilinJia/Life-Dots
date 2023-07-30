/*
 * LifeDots
 *
 * Copyright (C) 2017-2018 Raphael Mack http://www.raphael-mack.de
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
package com.mdiqentw.lifedots.ui.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.pm.PackageManager
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import android.os.Bundle
import android.text.format.DateFormat
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.text.isDigitsOnly
import androidx.databinding.DataBindingUtil
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import com.mdiqentw.lifedots.MVApplication
import com.mdiqentw.lifedots.R
import com.mdiqentw.lifedots.databinding.ActivitySettingsBinding
import com.mdiqentw.lifedots.db.Contract
import com.mdiqentw.lifedots.db.LocalDBHelper
import com.mdiqentw.lifedots.helpers.ActivityHelper
import com.mdiqentw.lifedots.helpers.LocationHelper
import com.mdiqentw.lifedots.ui.generic.BaseActivity
import org.jetbrains.annotations.NonNls
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

class SettingsActivity : BaseActivity(), OnSharedPreferenceChangeListener {
    private var dateformatPref: Preference? = null
    private var durationFormatPref: ListPreference? = null
    private var autoSelectPref: Preference? = null

    //    private Preference storageFolderPref;
    //    private Preference tagImagesPref;
    private var condAlphaPref: Preference? = null
    private var condOccurrencePref: Preference? = null
    private var condRecencyPref: Preference? = null

    //    private Preference nofifShowCurActPref;
    //    private Preference silentRenotifPref;
    private var disableOnClickPref: Preference? = null
    private var useLocationPref: ListPreference? = null
    private var locationStartPref: EditTextPreference? = null
    private var locationStopPref: EditTextPreference? = null
    private var locationAgePref: EditTextPreference? = null
    private var locationDistPref: EditTextPreference? = null
    private lateinit var mPreferenceManager: PreferenceManager

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        when (key) {
            KEY_PREF_DATETIME_FORMAT -> {
                val def = resources.getString(R.string.default_datetime_format)
                // Set summary to be the user-description for the selected value
                dateformatPref!!.summary =
                    DateFormat.format(sharedPreferences.getString(key, def), Date())
            }

            KEY_PREF_AUTO_SELECT -> updateAutoSelectSummary()
            KEY_PREF_COND_ALPHA -> updateCondAlphaSummary()
            KEY_PREF_COND_OCCURRENCE -> updateCondOccurenceSummary()
            KEY_PREF_COND_RECENCY -> updateCondRecencySummary()
            KEY_PREF_DISABLE_CURRENT -> updateDisableCurrent()
            KEY_PREF_USE_LOCATION -> updateUseLocation()
            KEY_PREF_LOCATION_START -> updateLocationStart()
            KEY_PREF_LOCATION_STOP -> updateLocationStop()
            KEY_PREF_LOCATION_AGE -> updateLocationAge()
            KEY_PREF_LOCATION_DIST -> updateLocationDist()
            KEY_PREF_DURATION_FORMAT -> updateDurationFormat()
        }
    }

    private fun updateDurationFormat() {
        val value = PreferenceManager
            .getDefaultSharedPreferences(applicationContext)
            .getString(KEY_PREF_DURATION_FORMAT, "dynamic")
        when (value) {
            "dynamic" -> durationFormatPref!!.summary =
                resources.getString(R.string.setting_duration_format_summary_dynamic)

            "nodays" -> durationFormatPref!!.summary =
                resources.getString(R.string.setting_duration_format_summary_nodays)

            "precise" -> durationFormatPref!!.summary =
                resources.getString(R.string.setting_duration_format_summary_precise)

            "hour_min" -> durationFormatPref!!.summary =
                resources.getString(R.string.setting_duration_format_summary_hour_min)
        }
    }

    private fun updateUseLocation() {
        val permissionCheckFine: Int
        val permissionCheckCoarse: Int
        @NonNls val value = PreferenceManager
            .getDefaultSharedPreferences(applicationContext)
            .getString(KEY_PREF_USE_LOCATION, "off")
        if (value == "off") {
            locationStartPref!!.isEnabled = false
            locationStopPref!!.isEnabled = false
            locationAgePref!!.isEnabled = false
            locationDistPref!!.isEnabled = false
            useLocationPref!!.summary = resources.getString(R.string.setting_use_location_off_summary)
        } else {
            locationStartPref!!.isEnabled = true
            locationStopPref!!.isEnabled = true
            locationAgePref!!.isEnabled = true
            locationDistPref!!.isEnabled = true
            useLocationPref!!.summary =
                resources.getString(R.string.setting_use_location_summary, useLocationPref!!.entry)
        }
        if (value == "gps") {
            permissionCheckFine = ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            if (permissionCheckFine != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                ) Toast.makeText(this, R.string.perm_location_xplain, Toast.LENGTH_LONG).show()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ActivityCompat.requestPermissions(
                        this, arrayOf(
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ),
                        4711
                    )
                } else {
                    ActivityCompat.requestPermissions(
                        this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        4712
                    )
                }
            }
        } else if (value == "network") {
            permissionCheckCoarse = ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            if (permissionCheckCoarse != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                ) Toast.makeText(this, R.string.perm_location_xplain, Toast.LENGTH_LONG).show()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ActivityCompat.requestPermissions(
                        this, arrayOf(
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ),
                        4711
                    )
                } else {
                    ActivityCompat.requestPermissions(
                        this, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                        4713
                    )
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 4712 || requestCode == 4713) {
            if (grantResults[0] == 0) {
                LocationHelper.helper.updateLocation(false)
            }
        }
    }

    private fun updateLocationDist() {
        val def = resources.getString(R.string.pref_location_dist_default)
        @NonNls var value = PreferenceManager
            .getDefaultSharedPreferences(applicationContext)
            .getString(KEY_PREF_LOCATION_DIST, def)
        if (value.isNullOrBlank() || !value.isDigitsOnly()) value = def

        var v = value.replace("\\D".toRegex(), "").toInt()
        if (v < 5) {
            v = 5
        }
        val nvalue = v.toString()
        if (value != nvalue) {
            val editor = PreferenceManager
                .getDefaultSharedPreferences(applicationContext).edit()
            editor.putString(KEY_PREF_LOCATION_DIST, nvalue)
            editor.apply()
            value = PreferenceManager
                .getDefaultSharedPreferences(applicationContext)
                .getString(KEY_PREF_LOCATION_DIST, def)
        }
        locationDistPref!!.summary = resources.getString(R.string.pref_location_dist, value)
    }

    private fun updateLocationAge() {
        val def = resources.getString(R.string.pref_location_age_default)
        var value = PreferenceManager
            .getDefaultSharedPreferences(applicationContext)
            .getString(KEY_PREF_LOCATION_AGE, def)
        if (value.isNullOrBlank() || !value.isDigitsOnly()) value = def

        var v = value.replace("\\D".toRegex(), "").toInt()
        if (v < 2) {
            v = 2
        } else if (v > 720) {
            v = 720
        }
        val nvalue = v.toString()
        if (value != nvalue) {
            val editor = PreferenceManager
                .getDefaultSharedPreferences(applicationContext).edit()
            editor.putString(KEY_PREF_LOCATION_AGE, nvalue)
            editor.apply()
            value = PreferenceManager
                .getDefaultSharedPreferences(applicationContext)
                .getString(KEY_PREF_LOCATION_AGE, def)
        }
        locationAgePref!!.summary = resources.getString(R.string.pref_location_age, value)
    }

    private fun updateLocationStart() {
        val def = resources.getString(R.string.pref_location_start_default)
        var value = PreferenceManager
            .getDefaultSharedPreferences(applicationContext)
            .getString(KEY_PREF_LOCATION_START, def)
        if (value.isNullOrBlank() || !value.isDigitsOnly()) value = def

        var v = value.replace("\\D".toRegex(), "").toInt()
        if (v < 0) {
            v = 0
        } else if (v > 24) {
            v = 24
        }
        val nvalue = v.toString()
        if (value != nvalue) {
            val editor = PreferenceManager
                .getDefaultSharedPreferences(applicationContext).edit()
            editor.putString(KEY_PREF_LOCATION_START, nvalue)
            editor.apply()
            value = PreferenceManager
                .getDefaultSharedPreferences(applicationContext)
                .getString(KEY_PREF_LOCATION_START, def)
        }
        locationStartPref!!.summary = resources.getString(R.string.pref_location_start, value)
    }

    private fun updateLocationStop() {
        val def = resources.getString(R.string.pref_location_stop_default)
        var value = PreferenceManager
            .getDefaultSharedPreferences(applicationContext)
            .getString(KEY_PREF_LOCATION_STOP, def)
        if (value.isNullOrBlank() || !value.isDigitsOnly()) value = def

        var v = value.replace("\\D".toRegex(), "").toInt()
        if (v < 0) {
            v = 0
        } else if (v > 24) {
            v = 24
        }
        val nvalue = v.toString()
        if (value != nvalue) {
            val editor = PreferenceManager
                .getDefaultSharedPreferences(applicationContext).edit()
            editor.putString(KEY_PREF_LOCATION_STOP, nvalue)
            editor.apply()
            value = PreferenceManager
                .getDefaultSharedPreferences(applicationContext)
                .getString(KEY_PREF_LOCATION_STOP, def)
        }
        locationStopPref!!.summary = resources.getString(R.string.pref_location_stop, value)
    }

    private fun updateDisableCurrent() {
        if (PreferenceManager
                .getDefaultSharedPreferences(applicationContext)
                .getBoolean(KEY_PREF_DISABLE_CURRENT, true)
        ) {
            disableOnClickPref!!.summary = resources.getString(R.string.setting_disable_on_click_summary_active)
        } else {
            disableOnClickPref!!.summary = resources.getString(R.string.setting_disable_on_click_summary_inactive)
        }
    }

    private fun updateCondAlphaSummary() {
        val def = resources.getString(R.string.pref_cond_alpha_default)
        val value = PreferenceManager
            .getDefaultSharedPreferences(applicationContext)
            .getString(KEY_PREF_COND_ALPHA, def)
        if (value!!.toDouble() == 0.0) {
            condAlphaPref!!.summary = resources.getString(R.string.setting_cond_alpha_not_used_summary)
        } else {
            condAlphaPref!!.summary = resources.getString(R.string.setting_cond_alpha_summary, value)
        }
    }

    private fun updateCondOccurenceSummary() {
        val def = resources.getString(R.string.pref_cond_occurrence_default)
        val value = PreferenceManager
            .getDefaultSharedPreferences(applicationContext)
            .getString(KEY_PREF_COND_OCCURRENCE, def)
        if (value!!.toDouble() == 0.0) {
            condOccurrencePref!!.summary = resources.getString(R.string.setting_cond_occurrence_not_used_summary)
        } else {
            condOccurrencePref!!.summary = resources.getString(R.string.setting_cond_occurrence_summary, value)
        }
    }

    private fun updateCondRecencySummary() {
        val def = resources.getString(R.string.pref_cond_recency_default)
        val value = PreferenceManager
            .getDefaultSharedPreferences(applicationContext)
            .getString(KEY_PREF_COND_RECENCY, def)
        if (value!!.toDouble() == 0.0) {
            condRecencyPref!!.summary = resources.getString(R.string.setting_cond_recency_not_used_summary)
        } else {
            condRecencyPref!!.summary = resources.getString(R.string.setting_cond_recency_summary, value)
        }
    }

    private fun updateAutoSelectSummary() {
        if (PreferenceManager
                .getDefaultSharedPreferences(applicationContext)
                .getBoolean(KEY_PREF_AUTO_SELECT, true)
        ) {
            autoSelectPref!!.summary = resources.getString(R.string.setting_auto_select_new_summary_active)
        } else {
            autoSelectPref!!.summary = resources.getString(R.string.setting_auto_select_new_summary_inactive)
        }
    }

    //    private void updateNotifShowCurActivity() {
    //        if(PreferenceManager
    //                .getDefaultSharedPreferences(getApplicationContext())
    //                .getBoolean(KEY_PREF_NOTIF_SHOW_CUR_ACT, false)){
    //            nofifShowCurActPref.setSummary(getResources().getString(R.string.setting_show_cur_activitiy_notification_summary_active));
    //        }else{
    //            nofifShowCurActPref.setSummary(getResources().getString(R.string.setting_show_cur_activitiy_notification_summary_inactive));
    //        }
    //        ActivityHelper.helper.showCurrentActivityNotification();
    //    }
    //    private void updateSilentNotifications() {
    //        if(PreferenceManager
    //                .getDefaultSharedPreferences(getApplicationContext())
    //                .getBoolean(KEY_PREF_SILENT_RENOTIFICATIONS, true)){
    //            silentRenotifPref.setSummary(getResources().getString(R.string.setting_silent_reconfication_summary_active));
    //        }else{
    //            silentRenotifPref.setSummary(getResources().getString(R.string.setting_silent_reconfication_summary_inactive));
    //        }
    //        ActivityHelper.helper.showCurrentActivityNotification();
    //    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = DataBindingUtil.setContentView<ActivitySettingsBinding>(this, R.layout.activity_settings)

//        View contentView = View.inflate(this, R.layout.activity_settings, null);
//        setContent(contentView);
        val sf = supportFragmentManager.findFragmentById(R.id.settings_fragment) as SettingsFragment?
        setContent(binding.root)
        mPreferenceManager = sf!!.preferenceManager
        dateformatPref = mPreferenceManager.findPreference(KEY_PREF_DATETIME_FORMAT)
        val def = resources.getString(R.string.default_datetime_format)
        dateformatPref!!.summary = DateFormat.format(
            PreferenceManager
                .getDefaultSharedPreferences(applicationContext)
                .getString(KEY_PREF_DATETIME_FORMAT, def), Date()
        )
        durationFormatPref = mPreferenceManager.findPreference(KEY_PREF_DURATION_FORMAT)
        autoSelectPref = mPreferenceManager.findPreference(KEY_PREF_AUTO_SELECT)
        disableOnClickPref = mPreferenceManager.findPreference(KEY_PREF_DISABLE_CURRENT)
        //        storageFolderPref = mPreferenceManager.findPreference(KEY_PREF_STORAGE_FOLDER);
        useLocationPref = mPreferenceManager.findPreference(KEY_PREF_USE_LOCATION)
        locationStartPref = mPreferenceManager.findPreference(KEY_PREF_LOCATION_START)
        locationStopPref = mPreferenceManager.findPreference(KEY_PREF_LOCATION_STOP)
        locationAgePref = mPreferenceManager.findPreference(KEY_PREF_LOCATION_AGE)
        locationDistPref = mPreferenceManager.findPreference(KEY_PREF_LOCATION_DIST)

//        tagImagesPref = mPreferenceManager.findPreference(KEY_PREF_TAG_IMAGES);
//        nofifShowCurActPref = mPreferenceManager.findPreference(KEY_PREF_NOTIF_SHOW_CUR_ACT);
//        silentRenotifPref = mPreferenceManager.findPreference(KEY_PREF_SILENT_RENOTIFICATIONS);
        val exportPref = mPreferenceManager.findPreference<Preference>(KEY_PREF_DB_EXPORT)
        exportPref!!.onPreferenceClickListener = Preference.OnPreferenceClickListener { _: Preference? ->
            /* export database */
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
            intent.type = "application/x-sqlite3"
            intent.putExtra(
                Intent.EXTRA_TITLE, resources.getString(R.string.db_export_name_suggestion) + "_" +
                        SimpleDateFormat("yyyy-MM-dd").format(Date()) + ".sqlite3"
            )
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            startActivityForResult(
                Intent.createChooser(intent, resources.getString(R.string.db_export_selection)),
                ACTIVITIY_RESULT_EXPORT
            )
            true
        }
        val importPref = mPreferenceManager.findPreference<Preference>(KEY_PREF_DB_IMPORT)
        importPref!!.onPreferenceClickListener = Preference.OnPreferenceClickListener { _: Preference? ->
            /* import database */
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "application/*"
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            startActivityForResult(
                Intent.createChooser(intent, resources.getString(R.string.db_import_selection)),
                ACTIVITIY_RESULT_IMPORT
            )
            true
        }
        condAlphaPref = mPreferenceManager.findPreference(KEY_PREF_COND_ALPHA)
        condOccurrencePref = mPreferenceManager.findPreference(KEY_PREF_COND_OCCURRENCE)
        condRecencyPref = mPreferenceManager.findPreference(KEY_PREF_COND_RECENCY)

//        updateAutoSelectSummary();
//        updateStorageFolderSummary();
//        updateTagImageSummary();
//        updateCondAlphaSummary();
//        updateCondOccurenceSummary();
//        updateNotifShowCurActivity();
//        updateSilentNotifications();
//        updateDisableCurrent();
//        updateUseLocation();
//        updateLocationAge();
//        updateLocationDist();
//        updateDurationFormat();
        mDrawerToggle.isDrawerIndicatorEnabled = false
    }

    public override fun onResume() {
        mNavigationView.menu.findItem(R.id.nav_settings).isChecked = true
        super.onResume()
        mPreferenceManager.preferenceScreen.sharedPreferences!!
            .registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        mPreferenceManager.preferenceScreen.sharedPreferences!!
            .unregisterOnSharedPreferenceChangeListener(this)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ACTIVITIY_RESULT_IMPORT && resultCode == RESULT_OK) {
            val selectedfile = data!!.data //The uri with the location of the file
            // import
            checkpointIfWALEnabled(applicationContext)
            val db = File(applicationContext.getDatabasePath(Contract.AUTHORITY).path)
            val bak = File(applicationContext.getDatabasePath(Contract.AUTHORITY).path + ".bak")
            var inputStream: InputStream? = null
            var outputStream: OutputStream? = null
            try {
                db.renameTo(bak)
                val s = resources.getString(R.string.db_import_success, data.data.toString())
                inputStream = contentResolver.openInputStream(data.data!!)
                outputStream = FileOutputStream(db)
                val buff = ByteArray(4048)
                var len: Int
                while (inputStream!!.read(buff).also { len = it } > 0) {
                    outputStream.write(buff, 0, len)
                    outputStream.flush()
                }
                outputStream.close()
                outputStream = null
                inputStream.close()
                inputStream = null
                val sdb = SQLiteDatabase.openDatabase(db.path, null, SQLiteDatabase.OPEN_READONLY)
                val v = sdb.version
                sdb.close()
                if (v > LocalDBHelper.CURRENT_VERSION) {
                    throw Exception("selected file has version $v which is too high...")
                }
                ActivityHelper.helper.reloadAll()
                Toast.makeText(this@SettingsActivity, s, Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                if (inputStream != null) {
                    try {
                        inputStream.close()
                    } catch (e1: IOException) {
                        /* ignore */
                    }
                }
                if (outputStream != null) {
                    try {
                        outputStream.close()
                    } catch (e1: IOException) {
                        /* ignore */
                    }
                }
                bak.renameTo(db)
                Log.e(TAG, "error on database import: " + e.message)
                val s = resources.getString(R.string.db_import_error, data.data.toString())
                Toast.makeText(this@SettingsActivity, s, Toast.LENGTH_LONG).show()
                bak.renameTo(db)
            }
        }
        if (requestCode == ACTIVITIY_RESULT_EXPORT && resultCode == RESULT_OK) {

            // export
            checkpointIfWALEnabled(applicationContext)

//            System.out.println(getApplicationContext().getDatabasePath(Contract.AUTHORITY).getPath());
            val db = File(applicationContext.getDatabasePath(Contract.AUTHORITY).path)
            try {
                val s = resources.getString(R.string.db_export_success, data!!.data.toString())
                val inputStream: InputStream = FileInputStream(db)
                val outputStream = contentResolver.openOutputStream(data.data!!)
                val buff = ByteArray(4048)
                var len: Int
                while (inputStream.read(buff).also { len = it } > 0) {
                    outputStream!!.write(buff, 0, len)
                    outputStream.flush()
                }
                outputStream!!.close()
                inputStream.close()
                Toast.makeText(this@SettingsActivity, s, Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Log.e(TAG, "error on database export: " + e.message)
                val s = resources.getString(R.string.db_export_error, data!!.data.toString())
                Toast.makeText(this@SettingsActivity, s, Toast.LENGTH_LONG).show()
            }
        }
    }

    companion object {
        private val TAG = SettingsActivity::class.java.name
        const val KEY_PREF_DATETIME_FORMAT = "pref_datetimeFormat"
        const val KEY_PREF_AUTO_SELECT = "pref_auto_select_new"
        const val KEY_PREF_DB_EXPORT = "pref_db_export"
        const val KEY_PREF_DB_IMPORT = "pref_db_import"
        const val KEY_PREF_COND_ALPHA = "pref_cond_alpha"
        const val KEY_PREF_COND_PREDECESSOR = "pref_cond_predecessor"
        const val KEY_PREF_COND_OCCURRENCE = "pref_cond_occurrence"
        const val KEY_PREF_COND_RECENCY = "pref_cond_recency"

        //    public static final String KEY_PREF_NOTIF_SHOW_CUR_ACT = "pref_show_cur_activity_notification";
        //    public static final String KEY_PREF_SILENT_RENOTIFICATIONS = "pref_silent_renotification";
        const val KEY_PREF_DISABLE_CURRENT = "pref_disable_current_on_click"
        const val KEY_PREF_COND_DAYTIME = "pref_cond_daytime"
        const val KEY_PREF_USE_LOCATION = "pref_use_location"
        const val KEY_PREF_LOCATION_START = "pref_location_start"
        const val KEY_PREF_LOCATION_STOP = "pref_location_stop"
        const val KEY_PREF_LOCATION_AGE = "pref_location_age"
        const val KEY_PREF_LOCATION_DIST = "pref_location_dist"
        const val KEY_PREF_PAUSED = "pref_cond_paused"
        const val KEY_PREF_DURATION_FORMAT = "pref_duration_format"
        const val ACTIVITIY_RESULT_EXPORT = 17
        const val ACTIVITIY_RESULT_IMPORT = 18
        var mOpenHelper = LocalDBHelper(MVApplication.appContext!!)
        private fun checkpointIfWALEnabled(context: Context) {
            val TAGLocal = "WALCHKPNT"
            val csr: Cursor
            var wal_busy = -99
            var wal_log = -99
            var wal_checkpointed = -99
            val db = SQLiteDatabase.openDatabase(
                context.getDatabasePath(Contract.AUTHORITY).path,
                null,
                SQLiteDatabase.OPEN_READWRITE
            )
            csr = db.rawQuery("PRAGMA journal_mode", null)
            if (csr.moveToFirst()) {
                val mode = csr.getString(0)
                //Log.d(TAGLocal, "Mode is " + mode);
                if (mode.equals("wal", ignoreCase = true)) {
                    val csr1 = db.rawQuery("PRAGMA wal_checkpoint", null)
                    if (csr1.moveToFirst()) {
                        wal_busy = csr1.getInt(0)
                        wal_log = csr1.getInt(1)
                        wal_checkpointed = csr1.getInt(2)
                    }
                    Log.d(
                        TAGLocal, "Checkpoint pre checkpointing Busy = " + wal_busy + " LOG = " +
                                wal_log + " CHECKPOINTED = " + wal_checkpointed
                    )
                    csr1.close()
                    val csr2 = db.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null)
                    csr2.count
                    csr2.close()
                    val csr3 = db.rawQuery("PRAGMA wal_checkpoint", null)
                    if (csr3.moveToFirst()) {
                        wal_busy = csr3.getInt(0)
                        wal_log = csr3.getInt(1)
                        wal_checkpointed = csr3.getInt(2)
                    }
                    Log.d(
                        TAGLocal, "Checkpoint post checkpointing Busy = " + wal_busy + " LOG = " +
                                wal_log + " CHECKPOINTED = " + wal_checkpointed
                    )
                    csr3.close()
                }
            }
            csr.close()
            db.close()
        }
    }
}