/*
 * ActivityDiary
 *
 * Copyright (C) 2017-2018 Raphael Mack http://www.raphael-mack.de
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

package de.rampro.activitydiary.ui.settings;


import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

import de.rampro.activitydiary.ActivityDiaryApplication;
import de.rampro.activitydiary.R;
import de.rampro.activitydiary.db.ActivityDiaryContract;
import de.rampro.activitydiary.db.LocalDBHelper;
import de.rampro.activitydiary.helpers.ActivityHelper;
import de.rampro.activitydiary.ui.generic.BaseActivity;

public class SettingsActivity extends BaseActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = SettingsActivity.class.getName();

    public static final String KEY_PREF_DATETIME_FORMAT = "pref_datetimeFormat";
    public static final String KEY_PREF_AUTO_SELECT = "pref_auto_select_new";
    public static final String KEY_PREF_STORAGE_FOLDER = "pref_storageFolder";
    public static final String KEY_PREF_TAG_IMAGES = "pref_tag_images";
    public static final String KEY_PREF_DB_EXPORT = "pref_db_export";
    public static final String KEY_PREF_DB_IMPORT = "pref_db_import";
    public static final String KEY_PREF_COND_ALPHA = "pref_cond_alpha";
    public static final String KEY_PREF_COND_PREDECESSOR = "pref_cond_predecessor";
    public static final String KEY_PREF_COND_OCCURRENCE = "pref_cond_occurrence";
    public static final String KEY_PREF_NOTIF_SHOW_CUR_ACT = "pref_show_cur_activity_notification";
    public static final String KEY_PREF_SILENT_RENOTIFICATIONS = "pref_silent_renotification";
    public static final String KEY_PREF_DISABLE_CURRENT = "pref_disable_current_on_click";
    public static final String KEY_PREF_COND_DAYTIME = "pref_cond_daytime";
    public static final String KEY_PREF_USE_LOCATION = "pref_use_location";
    public static final String KEY_PREF_LOCATION_AGE = "pref_location_age";
    public static final String KEY_PREF_LOCATION_DIST = "pref_location_dist";
    public static final String KEY_PREF_PAUSED = "pref_cond_paused";
    public static final String KEY_PREF_DURATION_FORMAT = "pref_duration_format";

    public static final int ACTIVITIY_RESULT_EXPORT = 17;
    public static final int ACTIVITIY_RESULT_IMPORT = 18;

    private Preference dateformatPref;
    private ListPreference durationFormatPref;
    private Preference autoSelectPref;
    private Preference storageFolderPref;
    private Preference tagImagesPref;
    private Preference condAlphaPref;
    private Preference condPredecessorPref;
    private Preference condPausedPref;
    private Preference condOccurrencePref;
    private Preference condDayTimePref;
    private Preference nofifShowCurActPref;
    private Preference silentRenotifPref;
    private Preference exportPref;
    private Preference importPref;
    private Preference disableOnClickPref;
    private ListPreference useLocationPref;
    private EditTextPreference locationAgePref;
    private EditTextPreference locationDistPref;

    private PreferenceManager mPreferenceManager;

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {
        if (key.equals(KEY_PREF_DATETIME_FORMAT)) {
            String def = getResources().getString(R.string.default_datetime_format);
            // Set summary to be the user-description for the selected value
            dateformatPref.setSummary(DateFormat.format(sharedPreferences.getString(key, def), new Date()));
        }else if(key.equals(KEY_PREF_AUTO_SELECT)){
            updateAutoSelectSummary();
        }else if(key.equals(KEY_PREF_STORAGE_FOLDER)) {
            /* TODO: we could ask here whether we shall move the pictures... */
            updateStorageFolderSummary();
        }else if(key.equals(KEY_PREF_TAG_IMAGES)) {
            updateTagImageSummary();
        }else if(key.equals(KEY_PREF_COND_ALPHA)) {
            updateCondAlphaSummary();
        }else if(key.equals(KEY_PREF_COND_PREDECESSOR)) {
            updateCondPredecessorSummary();
        }else if(key.equals(KEY_PREF_COND_OCCURRENCE)) {
            updateCondOccurenceSummary();
        }else if(key.equals(KEY_PREF_NOTIF_SHOW_CUR_ACT)) {
            updateNotifShowCurActivity();
        }else if(key.equals(KEY_PREF_SILENT_RENOTIFICATIONS)){
            updateSilentNotifications();
        }else if(key.equals(KEY_PREF_COND_DAYTIME)){
            updateCondDayTimeSummary();
        }else if(key.equals(KEY_PREF_DISABLE_CURRENT)){
            updateDisableCurrent();
        }else if(key.equals(KEY_PREF_USE_LOCATION)){
            updateUseLocation();
        }else if(key.equals(KEY_PREF_LOCATION_AGE)){
            updateLocationAge();
        }else if(key.equals(KEY_PREF_LOCATION_DIST)){
            updateLocationDist();
        }else if(key.equals(KEY_PREF_PAUSED)){
            updateCondPaused();
        }else if(key.equals(KEY_PREF_DURATION_FORMAT)){
            updateDurationFormat();
        }
    }

    private void updateDurationFormat() {

        String value = PreferenceManager
                .getDefaultSharedPreferences(ActivityDiaryApplication.getAppContext())
                .getString(KEY_PREF_DURATION_FORMAT, "dynamic");

        if(value.equals("dynamic")){
            durationFormatPref.setSummary(getResources().getString(R.string.setting_duration_format_summary_dynamic));
        }else if(value.equals("nodays")){
            durationFormatPref.setSummary(getResources().getString(R.string.setting_duration_format_summary_nodays));
        }else if(value.equals("precise")){
            durationFormatPref.setSummary(getResources().getString(R.string.setting_duration_format_summary_precise));
        }else if(value.equals("hour_min")){
            durationFormatPref.setSummary(getResources().getString(R.string.setting_duration_format_summary_hour_min));
        }
    }

    private void updateUseLocation() {
        int permissionCheckFine = PackageManager.PERMISSION_DENIED;
        int permissionCheckCoarse = PackageManager.PERMISSION_DENIED;

        String value = PreferenceManager
                .getDefaultSharedPreferences(ActivityDiaryApplication.getAppContext())
                .getString(KEY_PREF_USE_LOCATION, "off");

        if(value.equals("off")){
            locationAgePref.setEnabled(false);
            locationDistPref.setEnabled(false);
            useLocationPref.setSummary(getResources().getString(R.string.setting_use_location_off_summary));
        }else {
            locationAgePref.setEnabled(true);
            locationDistPref.setEnabled(true);
            useLocationPref.setSummary(getResources().getString(R.string.setting_use_location_summary, useLocationPref.getEntry()));
        }

        if(value.equals("gps")) {
            permissionCheckFine = ContextCompat.checkSelfPermission(ActivityDiaryApplication.getAppContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION);
            if (permissionCheckFine != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)) {

                    Toast.makeText(this, R.string.perm_location_xplain, Toast.LENGTH_LONG).show();
                }
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        4712);
            }
        }else{
            permissionCheckCoarse = ContextCompat.checkSelfPermission(ActivityDiaryApplication.getAppContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION);
            if (permissionCheckCoarse != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION)) {

                    Toast.makeText(this, R.string.perm_location_xplain, Toast.LENGTH_LONG).show();
                }
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        4713);
            }
        }
    }

    private void updateLocationDist() {
        String def = getResources().getString(R.string.pref_location_dist_default);
        String value = PreferenceManager
                .getDefaultSharedPreferences(ActivityDiaryApplication.getAppContext())
                .getString(KEY_PREF_LOCATION_DIST, def);
        if(value.length() == 0){
            value = "0";
        }

        int v = Integer.parseInt(value.replaceAll("\\D",""));

        String nvalue = Integer.toString(v);
        if(!value.equals(nvalue)){
            SharedPreferences.Editor editor = PreferenceManager
                    .getDefaultSharedPreferences(ActivityDiaryApplication.getAppContext()).edit();
            editor.putString(KEY_PREF_LOCATION_DIST, nvalue);
            editor.apply();
            value = PreferenceManager
                    .getDefaultSharedPreferences(ActivityDiaryApplication.getAppContext())
                    .getString(KEY_PREF_LOCATION_DIST, def);
        }

        locationDistPref.setSummary(getResources().getString(R.string.pref_location_dist, value));
    }

    private void updateLocationAge() {
        String def = getResources().getString(R.string.pref_location_age_default);
        String value = PreferenceManager
                .getDefaultSharedPreferences(ActivityDiaryApplication.getAppContext())
                .getString(KEY_PREF_LOCATION_AGE, def);
        if(value.length() == 0){
            value = "5";
        }
        int v = Integer.parseInt(value.replaceAll("\\D",""));
        if(v < 2){
            v = 2;
        }else if(v > 60){
            v = 60;
        }
        String nvalue = Integer.toString(v);
        if(!value.equals(nvalue)){
            SharedPreferences.Editor editor = PreferenceManager
                    .getDefaultSharedPreferences(ActivityDiaryApplication.getAppContext()).edit();
            editor.putString(KEY_PREF_LOCATION_AGE, nvalue);
            editor.apply();
            value = PreferenceManager
                    .getDefaultSharedPreferences(ActivityDiaryApplication.getAppContext())
                    .getString(KEY_PREF_LOCATION_AGE, def);
        }
        locationAgePref.setSummary(getResources().getString(R.string.pref_location_age, value));
    }

    private void updateDisableCurrent() {
        if(PreferenceManager
                .getDefaultSharedPreferences(ActivityDiaryApplication.getAppContext())
                .getBoolean(KEY_PREF_DISABLE_CURRENT, true)){
            disableOnClickPref.setSummary(getResources().getString(R.string.setting_disable_on_click_summary_active));
        }else{
            disableOnClickPref.setSummary(getResources().getString(R.string.setting_disable_on_click_summary_inactive));
        }
    }

    private void updateTagImageSummary() {
        if(PreferenceManager
                .getDefaultSharedPreferences(ActivityDiaryApplication.getAppContext())
                .getBoolean(KEY_PREF_TAG_IMAGES, true)){
            tagImagesPref.setSummary(getResources().getString(R.string.setting_tag_yes));
        }else{
            tagImagesPref.setSummary(getResources().getString(R.string.setting_tag_no));
        }
    }

    private void updateStorageFolderSummary() {
        String dir = PreferenceManager
                .getDefaultSharedPreferences(ActivityDiaryApplication.getAppContext())
                .getString(KEY_PREF_STORAGE_FOLDER, "ActivityDiary");

        storageFolderPref.setSummary(getResources().getString(R.string.setting_storage_folder_summary, dir));
    }

    private void updateCondAlphaSummary() {
        String def = getResources().getString(R.string.pref_cond_alpha_default);
        String value = PreferenceManager
                .getDefaultSharedPreferences(ActivityDiaryApplication.getAppContext())
                .getString(KEY_PREF_COND_ALPHA, def);

        if(Double.parseDouble(value) == 0.0){
            condAlphaPref.setSummary(getResources().getString(R.string.setting_cond_alpha_not_used_summary));
        }else {
            condAlphaPref.setSummary(getResources().getString(R.string.setting_cond_alpha_summary, value));
        }
    }

    private void updateCondPredecessorSummary() {
        String def = getResources().getString(R.string.pref_cond_predecessor_default);
        String value = PreferenceManager
                .getDefaultSharedPreferences(ActivityDiaryApplication.getAppContext())
                .getString(KEY_PREF_COND_PREDECESSOR, def);

        if(Double.parseDouble(value) == 0.0){
            condPredecessorPref.setSummary(getResources().getString(R.string.setting_cond_predecessor_not_used_summary));
        }else {
            condPredecessorPref.setSummary(getResources().getString(R.string.setting_cond_predecessor_summary, value));
        }
    }

    private void updateCondPaused() {
        String def = getResources().getString(R.string.pref_cond_paused_default);
        String value = PreferenceManager
                .getDefaultSharedPreferences(ActivityDiaryApplication.getAppContext())
                .getString(KEY_PREF_PAUSED, def);

        if(Double.parseDouble(value) == 0.0){
            condPausedPref.setSummary(getResources().getString(R.string.setting_cond_paused_not_used_summary));
        }else {
            condPausedPref.setSummary(getResources().getString(R.string.setting_cond_paused_summary, value));
        }
    }

    private void updateCondDayTimeSummary() {
        String def = getResources().getString(R.string.pref_cond_daytime_default);
        String value = PreferenceManager
                .getDefaultSharedPreferences(ActivityDiaryApplication.getAppContext())
                .getString(KEY_PREF_COND_DAYTIME, def);

        if(Double.parseDouble(value) == 0.0){
            condDayTimePref.setSummary(getResources().getString(R.string.setting_cond_daytime_not_used_summary));
        }else {
            condDayTimePref.setSummary(getResources().getString(R.string.setting_cond_daytime_summary, value));
        }
    }

    private void updateCondOccurenceSummary() {
        String def = getResources().getString(R.string.pref_cond_occurrence_default);
        String value = PreferenceManager
                .getDefaultSharedPreferences(ActivityDiaryApplication.getAppContext())
                .getString(KEY_PREF_COND_OCCURRENCE, def);

        if(Double.parseDouble(value) == 0.0){
            condOccurrencePref.setSummary(getResources().getString(R.string.setting_cond_occurrence_not_used_summary));
        }else {
            condOccurrencePref.setSummary(getResources().getString(R.string.setting_cond_occurrence_summary, value));
        }
    }


    private void updateAutoSelectSummary() {
        if(PreferenceManager
                .getDefaultSharedPreferences(ActivityDiaryApplication.getAppContext())
                .getBoolean(KEY_PREF_AUTO_SELECT, true)){
            autoSelectPref.setSummary(getResources().getString(R.string.setting_auto_select_new_summary_active));
        }else{
            autoSelectPref.setSummary(getResources().getString(R.string.setting_auto_select_new_summary_inactive));
        }
    }

    private void updateNotifShowCurActivity() {
        if(PreferenceManager
                .getDefaultSharedPreferences(ActivityDiaryApplication.getAppContext())
                .getBoolean(KEY_PREF_NOTIF_SHOW_CUR_ACT, true)){
            nofifShowCurActPref.setSummary(getResources().getString(R.string.setting_show_cur_activitiy_notification_summary_active));
        }else{
            nofifShowCurActPref.setSummary(getResources().getString(R.string.setting_show_cur_activitiy_notification_summary_inactive));
        }
        ActivityHelper.helper.showCurrentActivityNotification();
    }

    private void updateSilentNotifications() {
        if(PreferenceManager
                .getDefaultSharedPreferences(ActivityDiaryApplication.getAppContext())
                .getBoolean(KEY_PREF_SILENT_RENOTIFICATIONS, true)){
            silentRenotifPref.setSummary(getResources().getString(R.string.setting_silent_reconfication_summary_active));
        }else{
            silentRenotifPref.setSummary(getResources().getString(R.string.setting_silent_reconfication_summary_inactive));
        }
        ActivityHelper.helper.showCurrentActivityNotification();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View contentView = inflater.inflate(R.layout.activity_settings, null, false);

        setContent(contentView);
        SettingsFragment sf = (SettingsFragment)getSupportFragmentManager().findFragmentById(R.id.settings_fragment);

        mPreferenceManager = sf.getPreferenceManager();
        dateformatPref = mPreferenceManager.findPreference(KEY_PREF_DATETIME_FORMAT);

        String def = getResources().getString(R.string.default_datetime_format);

        dateformatPref.setSummary(DateFormat.format(
                PreferenceManager
                        .getDefaultSharedPreferences(ActivityDiaryApplication.getAppContext())
                        .getString(KEY_PREF_DATETIME_FORMAT, def)
                , new Date()));

        durationFormatPref = (ListPreference)mPreferenceManager.findPreference(KEY_PREF_DURATION_FORMAT);
        autoSelectPref = mPreferenceManager.findPreference(KEY_PREF_AUTO_SELECT);
        disableOnClickPref = mPreferenceManager.findPreference(KEY_PREF_DISABLE_CURRENT);
        storageFolderPref = mPreferenceManager.findPreference(KEY_PREF_STORAGE_FOLDER);
        useLocationPref = (ListPreference) mPreferenceManager.findPreference(KEY_PREF_USE_LOCATION);
        locationAgePref = (EditTextPreference)mPreferenceManager.findPreference(KEY_PREF_LOCATION_AGE);
        locationDistPref = (EditTextPreference)mPreferenceManager.findPreference(KEY_PREF_LOCATION_DIST);

        tagImagesPref = mPreferenceManager.findPreference(KEY_PREF_TAG_IMAGES);
        nofifShowCurActPref = mPreferenceManager.findPreference(KEY_PREF_NOTIF_SHOW_CUR_ACT);
        silentRenotifPref = mPreferenceManager.findPreference(KEY_PREF_SILENT_RENOTIFICATIONS);

        exportPref =  mPreferenceManager.findPreference(KEY_PREF_DB_EXPORT);
        exportPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                /* export database */
                if (Build.VERSION.SDK_INT >= 19) {
                    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    intent.setType("application/x-sqlite3");
                    intent.putExtra(Intent.EXTRA_TITLE, getResources().getString(R.string.db_export_name_suggestion) + ".sqlite3");
                    intent.addCategory(Intent.CATEGORY_OPENABLE);

                    startActivityForResult(Intent.createChooser(intent, getResources().getString(R.string.db_export_selection)), ACTIVITIY_RESULT_EXPORT);
                }else{
                    Toast.makeText(SettingsActivity.this, getResources().getString(R.string.unsupported_on_api_level, 19), Toast.LENGTH_SHORT).show();
                }

                return true;
            }
        });
        importPref =  mPreferenceManager.findPreference(KEY_PREF_DB_IMPORT);
        importPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                /* import database */
                if (Build.VERSION.SDK_INT >= 19) {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("application/*");
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    startActivityForResult(Intent.createChooser(intent, getResources().getString(R.string.db_import_selection)), ACTIVITIY_RESULT_IMPORT);
                }else{
                    Toast.makeText(SettingsActivity.this, getResources().getString(R.string.unsupported_on_api_level, 19), Toast.LENGTH_SHORT).show();
                }

                return true;
            }
        });

        condAlphaPref = mPreferenceManager.findPreference(KEY_PREF_COND_ALPHA);
        condPredecessorPref = mPreferenceManager.findPreference(KEY_PREF_COND_PREDECESSOR);
        condPausedPref = mPreferenceManager.findPreference(KEY_PREF_PAUSED);
        condOccurrencePref = mPreferenceManager.findPreference(KEY_PREF_COND_OCCURRENCE);
        condDayTimePref = mPreferenceManager.findPreference(KEY_PREF_COND_DAYTIME);

        updateAutoSelectSummary();
        updateStorageFolderSummary();
        updateTagImageSummary();
        updateCondAlphaSummary();
        updateCondPredecessorSummary();
        updateCondPaused();
        updateCondOccurenceSummary();
        updateCondDayTimeSummary();
        updateNotifShowCurActivity();
        updateSilentNotifications();
        updateDisableCurrent();
        updateUseLocation();
        updateLocationAge();
        updateLocationDist();
        updateDurationFormat();

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
        if(requestCode == ACTIVITIY_RESULT_IMPORT && resultCode == RESULT_OK) {
            Uri selectedfile = data.getData(); //The uri with the location of the file
            // import
            // TODO: replace /data by Context.getFilesDir().getPath() -> see lint
            File db = new File("/data/data/" + getPackageName() + "/databases/" + ActivityDiaryContract.AUTHORITY);
            File bak = new File("/data/data/" + getPackageName() + "/databases/" + ActivityDiaryContract.AUTHORITY + ".bak");
            InputStream inputStream = null;
            OutputStream outputStream = null;
            try {
                db.renameTo(bak);

                String s = getResources().getString(R.string.db_import_success, data.getData().toString());
                inputStream = getContentResolver().openInputStream(data.getData());
                outputStream = new FileOutputStream(db);
                byte[] buff = new byte[4048];
                int len;
                while ((len = inputStream.read(buff)) > 0 ){
                    outputStream.write(buff,0,len);
                    outputStream.flush();
                }
                outputStream.close();
                outputStream = null;
                inputStream.close();
                inputStream = null;

                SQLiteDatabase sdb = SQLiteDatabase.openDatabase(db.getPath(), null, SQLiteDatabase.OPEN_READONLY);
                int v = sdb.getVersion();
                sdb.close();
                if(v > LocalDBHelper.CURRENT_VERSION){
                    throw new Exception("selected file has version " + v + " which is too high...");
                }

                ActivityHelper.helper.reloadAll();
                Toast.makeText(SettingsActivity.this, s, Toast.LENGTH_LONG).show();
            }catch (Exception e) {
                if(inputStream != null){
                    try {
                        inputStream.close();
                    } catch (IOException e1) {
                        /* ignore */
                    }
                }
                if(outputStream != null){
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
        if(requestCode == ACTIVITIY_RESULT_EXPORT && resultCode == RESULT_OK) {

            // export
            // TODO: replace /data by Context.getFilesDir().getPath() -> see lint
            File db = new File("/data/data/" + getPackageName() + "/databases/" + ActivityDiaryContract.AUTHORITY);
            try {
                String s = getResources().getString(R.string.db_export_success, data.getData().toString());
                InputStream inputStream = new FileInputStream(db);
                OutputStream outputStream = getContentResolver().openOutputStream(data.getData());
                byte[] buff = new byte[4048];
                int len;
                while ((len = inputStream.read(buff)) > 0 ){
                    outputStream.write(buff,0,len);
                    outputStream.flush();
                }
                outputStream.close();
                inputStream.close();
                Toast.makeText(SettingsActivity.this, s, Toast.LENGTH_LONG).show();
            }catch (Exception e){
                Log.e(TAG,"error on database export: "+e.getMessage());
                String s = getResources().getString(R.string.db_export_error, data.getData().toString());
                Toast.makeText(SettingsActivity.this, s, Toast.LENGTH_LONG).show();
            }
        }
    }
}
