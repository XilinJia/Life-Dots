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

package com.mdiqentw.lifedots.ui.settings;


import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;

import com.mdiqentw.lifedots.MVApplication;
import com.mdiqentw.lifedots.R;
import com.mdiqentw.lifedots.databinding.ActivitySettingsBinding;
import com.mdiqentw.lifedots.db.LocalDBHelper;
import com.mdiqentw.lifedots.db.Contract;
import com.mdiqentw.lifedots.helpers.ActivityHelper;
import com.mdiqentw.lifedots.helpers.LocationHelper;
import com.mdiqentw.lifedots.ui.generic.BaseActivity;

import org.jetbrains.annotations.NonNls;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

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

public class SettingsActivity extends BaseActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = SettingsActivity.class.getName();

    public static final String KEY_PREF_DATETIME_FORMAT = "pref_datetimeFormat";
    public static final String KEY_PREF_AUTO_SELECT = "pref_auto_select_new";
    public static final String KEY_PREF_DB_EXPORT = "pref_db_export";
    public static final String KEY_PREF_DB_IMPORT = "pref_db_import";
    public static final String KEY_PREF_COND_ALPHA = "pref_cond_alpha";
    public static final String KEY_PREF_COND_PREDECESSOR = "pref_cond_predecessor";
    public static final String KEY_PREF_COND_OCCURRENCE = "pref_cond_occurrence";
    public static final String KEY_PREF_COND_RECENCY = "pref_cond_recency";
//    public static final String KEY_PREF_NOTIF_SHOW_CUR_ACT = "pref_show_cur_activity_notification";
//    public static final String KEY_PREF_SILENT_RENOTIFICATIONS = "pref_silent_renotification";
    public static final String KEY_PREF_DISABLE_CURRENT = "pref_disable_current_on_click";
    public static final String KEY_PREF_COND_DAYTIME = "pref_cond_daytime";
    public static final String KEY_PREF_USE_LOCATION = "pref_use_location";
    public static final String KEY_PREF_LOCATION_AGE = "pref_location_age";
    public static final String KEY_PREF_LOCATION_DIST = "pref_location_dist";
    public static final String KEY_PREF_PAUSED = "pref_cond_paused";
    public static final String KEY_PREF_DURATION_FORMAT = "pref_duration_format";

    public static final int ACTIVITIY_RESULT_EXPORT = 17;
    public static final int ACTIVITIY_RESULT_IMPORT = 18;

    static LocalDBHelper mOpenHelper = new LocalDBHelper(MVApplication.getAppContext());

    private Preference dateformatPref;
    private ListPreference durationFormatPref;
    private Preference autoSelectPref;
//    private Preference storageFolderPref;
//    private Preference tagImagesPref;
    private Preference condAlphaPref;
    private Preference condOccurrencePref;
    private Preference condRecencyPref;
//    private Preference nofifShowCurActPref;
//    private Preference silentRenotifPref;
    private Preference disableOnClickPref;
    private ListPreference useLocationPref;
    private EditTextPreference locationAgePref;
    private EditTextPreference locationDistPref;

    private PreferenceManager mPreferenceManager;

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {
        switch (key) {
            case KEY_PREF_DATETIME_FORMAT:
                String def = getResources().getString(R.string.default_datetime_format);
                // Set summary to be the user-description for the selected value
                dateformatPref.setSummary(DateFormat.format(sharedPreferences.getString(key, def), new Date()));
                break;
            case KEY_PREF_AUTO_SELECT:
                updateAutoSelectSummary();
                break;
            case KEY_PREF_COND_ALPHA:
                updateCondAlphaSummary();
                break;
            case KEY_PREF_COND_OCCURRENCE:
                updateCondOccurenceSummary();
                break;
            case KEY_PREF_COND_RECENCY:
                updateCondRecencySummary();
                break;
//            case KEY_PREF_NOTIF_SHOW_CUR_ACT:
//                updateNotifShowCurActivity();
//                break;
//            case KEY_PREF_SILENT_RENOTIFICATIONS:
//                updateSilentNotifications();
//                break;
            case KEY_PREF_DISABLE_CURRENT:
                updateDisableCurrent();
                break;
            case KEY_PREF_USE_LOCATION:
                updateUseLocation();
                break;
            case KEY_PREF_LOCATION_AGE:
                updateLocationAge();
                break;
            case KEY_PREF_LOCATION_DIST:
                updateLocationDist();
                break;
            case KEY_PREF_DURATION_FORMAT:
                updateDurationFormat();
                break;
        }
    }

    private void updateDurationFormat() {

        String value = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext())
                .getString(KEY_PREF_DURATION_FORMAT, "dynamic");

        switch (value) {
            case "dynamic":
                durationFormatPref.setSummary(getResources().getString(R.string.setting_duration_format_summary_dynamic));
                break;
            case "nodays":
                durationFormatPref.setSummary(getResources().getString(R.string.setting_duration_format_summary_nodays));
                break;
            case "precise":
                durationFormatPref.setSummary(getResources().getString(R.string.setting_duration_format_summary_precise));
                break;
            case "hour_min":
                durationFormatPref.setSummary(getResources().getString(R.string.setting_duration_format_summary_hour_min));
                break;
        }
    }

    private void updateUseLocation() {
        int permissionCheckFine;
        int permissionCheckCoarse;

        @NonNls String value = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext())
                .getString(KEY_PREF_USE_LOCATION, "off");

        if (value.equals("off")) {
            locationAgePref.setEnabled(false);
            locationDistPref.setEnabled(false);
            useLocationPref.setSummary(getResources().getString(R.string.setting_use_location_off_summary));
        } else {
            locationAgePref.setEnabled(true);
            locationDistPref.setEnabled(true);
            useLocationPref.setSummary(getResources().getString(R.string.setting_use_location_summary, useLocationPref.getEntry()));
        }

        if (value.equals("gps")) {
            permissionCheckFine = ContextCompat.checkSelfPermission(getApplicationContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION);
            if (permissionCheckFine != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION))
                    Toast.makeText(this, R.string.perm_location_xplain, Toast.LENGTH_LONG).show();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                                    Manifest.permission.ACCESS_FINE_LOCATION},
                            4711);
                } else {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            4712);
                }
            }
        } else if (value.equals("network")) {
            permissionCheckCoarse = ContextCompat.checkSelfPermission(getApplicationContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION);
            if (permissionCheckCoarse != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION))
                    Toast.makeText(this, R.string.perm_location_xplain, Toast.LENGTH_LONG).show();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION},
                            4711);
                } else {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                            4713);
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 4712 || requestCode == 4713) {
            if (grantResults[0] == 0) {
                LocationHelper.helper.updateLocation();
            }
        }
    }

    private void updateLocationDist() {
        String def = getResources().getString(R.string.pref_location_dist_default);
        @NonNls String value = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext())
                .getString(KEY_PREF_LOCATION_DIST, def);

        int v = Integer.parseInt(value.replaceAll("\\D",""));
        if (v < 5) {
            v = 5;
        }
        String nvalue = Integer.toString(v);
        if(!value.equals(nvalue)){
            SharedPreferences.Editor editor = PreferenceManager
                    .getDefaultSharedPreferences(getApplicationContext()).edit();
            editor.putString(KEY_PREF_LOCATION_DIST, nvalue);
            editor.apply();
            value = PreferenceManager
                    .getDefaultSharedPreferences(getApplicationContext())
                    .getString(KEY_PREF_LOCATION_DIST, def);
        }

        locationDistPref.setSummary(getResources().getString(R.string.pref_location_dist, value));
    }

    private void updateLocationAge() {
        String def = getResources().getString(R.string.pref_location_age_default);
        @NonNls String value = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext())
                .getString(KEY_PREF_LOCATION_AGE, def);
        int v = Integer.parseInt(value.replaceAll("\\D",""));
        if (v < 2) {
            v = 2;
        } else if (v > 720){
            v = 720;
        }
        String nvalue = Integer.toString(v);
        if(!value.equals(nvalue)){
            SharedPreferences.Editor editor = PreferenceManager
                    .getDefaultSharedPreferences(getApplicationContext()).edit();
            editor.putString(KEY_PREF_LOCATION_AGE, nvalue);
            editor.apply();
            value = PreferenceManager
                    .getDefaultSharedPreferences(getApplicationContext())
                    .getString(KEY_PREF_LOCATION_AGE, def);
        }
        locationAgePref.setSummary(getResources().getString(R.string.pref_location_age, value));
    }

    private void updateDisableCurrent() {
        if(PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext())
                .getBoolean(KEY_PREF_DISABLE_CURRENT, true)){
            disableOnClickPref.setSummary(getResources().getString(R.string.setting_disable_on_click_summary_active));
        }else{
            disableOnClickPref.setSummary(getResources().getString(R.string.setting_disable_on_click_summary_inactive));
        }
    }

    private void updateCondAlphaSummary() {
        String def = getResources().getString(R.string.pref_cond_alpha_default);
        String value = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext())
                .getString(KEY_PREF_COND_ALPHA, def);

        if (Double.parseDouble(value) == 0.0) {
            condAlphaPref.setSummary(getResources().getString(R.string.setting_cond_alpha_not_used_summary));
        }else {
            condAlphaPref.setSummary(getResources().getString(R.string.setting_cond_alpha_summary, value));
        }
    }

    private void updateCondOccurenceSummary() {
        String def = getResources().getString(R.string.pref_cond_occurrence_default);
        String value = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext())
                .getString(KEY_PREF_COND_OCCURRENCE, def);

        if (Double.parseDouble(value) == 0.0) {
            condOccurrencePref.setSummary(getResources().getString(R.string.setting_cond_occurrence_not_used_summary));
        }else {
            condOccurrencePref.setSummary(getResources().getString(R.string.setting_cond_occurrence_summary, value));
        }
    }

    private void updateCondRecencySummary() {
        String def = getResources().getString(R.string.pref_cond_recency_default);
        String value = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext())
                .getString(KEY_PREF_COND_RECENCY, def);

        if(Double.parseDouble(value) == 0.0){
            condRecencyPref.setSummary(getResources().getString(R.string.setting_cond_recency_not_used_summary));
        }else {
            condRecencyPref.setSummary(getResources().getString(R.string.setting_cond_recency_summary, value));
        }
    }

    private void updateAutoSelectSummary() {
        if(PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext())
                .getBoolean(KEY_PREF_AUTO_SELECT, true)){
            autoSelectPref.setSummary(getResources().getString(R.string.setting_auto_select_new_summary_active));
        }else{
            autoSelectPref.setSummary(getResources().getString(R.string.setting_auto_select_new_summary_inactive));
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivitySettingsBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_settings);

//        View contentView = View.inflate(this, R.layout.activity_settings, null);
//        setContent(contentView);
        SettingsFragment sf = (SettingsFragment)getSupportFragmentManager().findFragmentById(R.id.settings_fragment);

        setContent(binding.getRoot());

        mPreferenceManager = sf.getPreferenceManager();
        dateformatPref = mPreferenceManager.findPreference(KEY_PREF_DATETIME_FORMAT);

        String def = getResources().getString(R.string.default_datetime_format);

        dateformatPref.setSummary(DateFormat.format(
                PreferenceManager
                        .getDefaultSharedPreferences(getApplicationContext())
                        .getString(KEY_PREF_DATETIME_FORMAT, def)
                , new Date()));

        durationFormatPref = mPreferenceManager.findPreference(KEY_PREF_DURATION_FORMAT);
        autoSelectPref = mPreferenceManager.findPreference(KEY_PREF_AUTO_SELECT);
        disableOnClickPref = mPreferenceManager.findPreference(KEY_PREF_DISABLE_CURRENT);
//        storageFolderPref = mPreferenceManager.findPreference(KEY_PREF_STORAGE_FOLDER);
        useLocationPref = mPreferenceManager.findPreference(KEY_PREF_USE_LOCATION);
        locationAgePref = mPreferenceManager.findPreference(KEY_PREF_LOCATION_AGE);
        locationDistPref = mPreferenceManager.findPreference(KEY_PREF_LOCATION_DIST);

//        tagImagesPref = mPreferenceManager.findPreference(KEY_PREF_TAG_IMAGES);
//        nofifShowCurActPref = mPreferenceManager.findPreference(KEY_PREF_NOTIF_SHOW_CUR_ACT);
//        silentRenotifPref = mPreferenceManager.findPreference(KEY_PREF_SILENT_RENOTIFICATIONS);

        Preference exportPref = mPreferenceManager.findPreference(KEY_PREF_DB_EXPORT);
        exportPref.setOnPreferenceClickListener(preference -> {
            /* export database */
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.setType("application/x-sqlite3");
            intent.putExtra(Intent.EXTRA_TITLE, getResources().getString(R.string.db_export_name_suggestion) + "_" +
                    new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + ".sqlite3");
            intent.addCategory(Intent.CATEGORY_OPENABLE);

            startActivityForResult(Intent.createChooser(intent, getResources().getString(R.string.db_export_selection)), ACTIVITIY_RESULT_EXPORT);
            return true;
        });
        Preference importPref = mPreferenceManager.findPreference(KEY_PREF_DB_IMPORT);
        importPref.setOnPreferenceClickListener(preference -> {
            /* import database */
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("application/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(intent, getResources().getString(R.string.db_import_selection)), ACTIVITIY_RESULT_IMPORT);
            return true;
        });

        condAlphaPref = mPreferenceManager.findPreference(KEY_PREF_COND_ALPHA);
        condOccurrencePref = mPreferenceManager.findPreference(KEY_PREF_COND_OCCURRENCE);
        condRecencyPref = mPreferenceManager.findPreference(KEY_PREF_COND_RECENCY);

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

        mDrawerToggle.setDrawerIndicatorEnabled(false);
    }
    
    @Override
    public void onResume(){
        mNavigationView.getMenu().findItem(R.id.nav_settings).setChecked(true);
        super.onResume();
        mPreferenceManager.getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);

    }

    @Override
    protected void onPause() {
        super.onPause();
        mPreferenceManager.getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ACTIVITIY_RESULT_IMPORT && resultCode == RESULT_OK) {
            Uri selectedfile = data.getData(); //The uri with the location of the file
            // import
            checkpointIfWALEnabled(getApplicationContext());

            File db = new File(getApplicationContext().getDatabasePath(Contract.AUTHORITY).getPath());
            File bak = new File(getApplicationContext().getDatabasePath(Contract.AUTHORITY).getPath() + ".bak");
            InputStream inputStream = null;
            OutputStream outputStream = null;
            try {
                db.renameTo(bak);

                String s = getResources().getString(R.string.db_import_success, data.getData().toString());
                inputStream = getContentResolver().openInputStream(data.getData());
                outputStream = new FileOutputStream(db);
                byte[] buff = new byte[4048];
                int len;
                while ((len = inputStream.read(buff)) > 0) {
                    outputStream.write(buff, 0, len);
                    outputStream.flush();
                }
                outputStream.close();
                outputStream = null;
                inputStream.close();
                inputStream = null;

                SQLiteDatabase sdb = SQLiteDatabase.openDatabase(db.getPath(), null, SQLiteDatabase.OPEN_READONLY);
                int v = sdb.getVersion();
                sdb.close();
                if (v > LocalDBHelper.CURRENT_VERSION) {
                    throw new Exception("selected file has version " + v + " which is too high...");
                }

                ActivityHelper.helper.reloadAll();
                Toast.makeText(SettingsActivity.this, s, Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e1) {
                        /* ignore */
                    }
                }
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException e1) {
                        /* ignore */
                    }
                }
                bak.renameTo(db);
                Log.e(TAG, "error on database import: " + e.getMessage());
                String s = getResources().getString(R.string.db_import_error, data.getData().toString());
                Toast.makeText(SettingsActivity.this, s, Toast.LENGTH_LONG).show();
                bak.renameTo(db);

            }
        }
        if (requestCode == ACTIVITIY_RESULT_EXPORT && resultCode == RESULT_OK) {

            // export
            checkpointIfWALEnabled(getApplicationContext());

//            System.out.println(getApplicationContext().getDatabasePath(Contract.AUTHORITY).getPath());
            File db = new File(getApplicationContext().getDatabasePath(Contract.AUTHORITY).getPath());
            try {
                String s = getResources().getString(R.string.db_export_success, data.getData().toString());
                InputStream inputStream = new FileInputStream(db);
                OutputStream outputStream = getContentResolver().openOutputStream(data.getData());
                byte[] buff = new byte[4048];
                int len;
                while ((len = inputStream.read(buff)) > 0) {
                    outputStream.write(buff, 0, len);
                    outputStream.flush();
                }
                outputStream.close();
                inputStream.close();

                Toast.makeText(SettingsActivity.this, s, Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Log.e(TAG, "error on database export: " + e.getMessage());
                String s = getResources().getString(R.string.db_export_error, data.getData().toString());
                Toast.makeText(SettingsActivity.this, s, Toast.LENGTH_LONG).show();
            }
        }
    }

    private static void checkpointIfWALEnabled(Context context) {
        final String TAGLocal = "WALCHKPNT";
        Cursor csr;
        int wal_busy = -99, wal_log = -99, wal_checkpointed = -99;
        SQLiteDatabase db = SQLiteDatabase.openDatabase(context.getDatabasePath(Contract.AUTHORITY).getPath(),null,SQLiteDatabase.OPEN_READWRITE);
        csr = db.rawQuery("PRAGMA journal_mode",null);
        if (csr.moveToFirst()) {
            String mode = csr.getString(0);
            //Log.d(TAGLocal, "Mode is " + mode);
            if (mode.equalsIgnoreCase("wal")) {
                Cursor csr1 = db.rawQuery("PRAGMA wal_checkpoint",null);
                if (csr1.moveToFirst()) {
                    wal_busy = csr1.getInt(0);
                    wal_log = csr1.getInt(1);
                    wal_checkpointed = csr1.getInt(2);
                }
                Log.d(TAGLocal,"Checkpoint pre checkpointing Busy = " + wal_busy + " LOG = " +
                        wal_log + " CHECKPOINTED = " + wal_checkpointed);
                csr1.close();
                
                Cursor csr2 = db.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)",null);
                csr2.getCount();
                csr2.close();
                
                Cursor csr3 = db.rawQuery("PRAGMA wal_checkpoint",null);
                if (csr3.moveToFirst()) {
                    wal_busy = csr3.getInt(0);
                    wal_log = csr3.getInt(1);
                    wal_checkpointed = csr3.getInt(2);
                }
                Log.d(TAGLocal,"Checkpoint post checkpointing Busy = " + wal_busy + " LOG = " +
                        wal_log + " CHECKPOINTED = " + wal_checkpointed);
                csr3.close();
            }
        }
        csr.close();
        db.close();
    }
}

