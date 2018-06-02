/*
 * ActivityDiary
 *
 * Copyright (C) 2017 Raphael Mack http://www.raphael-mack.de
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


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

import de.rampro.activitydiary.ActivityDiaryApplication;
import de.rampro.activitydiary.R;
import de.rampro.activitydiary.db.ActivityDiaryContract;
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
    public static final String KEY_PREF_DISABLE_CURRENT = "pref_disable_current_on_click";
    public static final String KEY_PREF_COND_DAYTIME = "pref_cond_daytime";

    public static final int ACTIVITIY_RESULT_EXPORT = 17;
    public static final int ACTIVITIY_RESULT_IMPORT = 18;

    private Preference dateformatPref;
    private Preference autoSelectPref;
    private Preference storageFolderPref;
    private Preference tagImagesPref;
    private Preference condAlphaPref;
    private Preference condPredecessorPref;
    private Preference condOccurrencePref;
    private Preference condDayTimePref;
    private Preference nofifShowCurActPref;
    private Preference exportPref;
    private Preference importPref;
    private Preference disableOnClickPref;

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
        }else if(key.equals(KEY_PREF_COND_DAYTIME)){
            updateCondDayTimeSummary();
        }else if(key.equals(KEY_PREF_DISABLE_CURRENT)){
            updateDisableCurrent();
        }
    }

    private void updateDisableCurrent() {
        if(PreferenceManager
                .getDefaultSharedPreferences(ActivityDiaryApplication.getAppContext())
                .getBoolean(KEY_PREF_DISABLE_CURRENT, false)){
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

        autoSelectPref = mPreferenceManager.findPreference(KEY_PREF_AUTO_SELECT);
        disableOnClickPref = mPreferenceManager.findPreference(KEY_PREF_DISABLE_CURRENT);
        storageFolderPref = mPreferenceManager.findPreference(KEY_PREF_STORAGE_FOLDER);
        tagImagesPref = mPreferenceManager.findPreference(KEY_PREF_TAG_IMAGES);
        nofifShowCurActPref = mPreferenceManager.findPreference(KEY_PREF_NOTIF_SHOW_CUR_ACT);

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
        condOccurrencePref = mPreferenceManager.findPreference(KEY_PREF_COND_OCCURRENCE);
        condDayTimePref = mPreferenceManager.findPreference(KEY_PREF_COND_DAYTIME);

        updateAutoSelectSummary();
        updateStorageFolderSummary();
        updateTagImageSummary();
        updateCondAlphaSummary();
        updateCondPredecessorSummary();
        updateCondOccurenceSummary();
        updateCondDayTimeSummary();
        updateNotifShowCurActivity();
        updateDisableCurrent();

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
            try {
                String s = getResources().getString(R.string.db_import_success, data.getData().toString());
                InputStream inputStream = getContentResolver().openInputStream(data.getData());
                OutputStream outputStream = new FileOutputStream(db);
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
                Log.e(TAG,"error on database impport: "+e.getMessage());
                String s = getResources().getString(R.string.db_import_error, data.getData().toString());
                Toast.makeText(SettingsActivity.this, s, Toast.LENGTH_LONG).show();
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
