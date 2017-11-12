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

package de.rampro.activitydiary.ui;


import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;

import java.util.Date;

import de.rampro.activitydiary.ActivityDiaryApplication;
import de.rampro.activitydiary.R;

public class SettingsActivity extends BaseActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    public static final String KEY_PREF_DATE_FORMAT = "pref_datetimeFormat";
    public static final String KEY_PREF_AUTO_SELECT = "pref_auto_select_new";

    private Preference dateformatPref;
    private Preference autoSelectPref;
    private PreferenceManager mPreferenceManager;

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {
        if (key.equals(KEY_PREF_DATE_FORMAT)) {
            String def = getResources().getString(R.string.default_datetime_format);
            // Set summary to be the user-description for the selected value
            dateformatPref.setSummary(DateFormat.format(sharedPreferences.getString(key, def), new Date()));
        }else if(key.equals(KEY_PREF_AUTO_SELECT)){
            updateAutoSelectSummary();
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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View contentView = inflater.inflate(R.layout.activity_settings, null, false);

        setContent(contentView);
        SettingsFragment sf = (SettingsFragment)getSupportFragmentManager().findFragmentById(R.id.settings_fragment);

        mPreferenceManager = sf.getPreferenceManager();
        dateformatPref = mPreferenceManager.findPreference(KEY_PREF_DATE_FORMAT);

        String def = getResources().getString(R.string.default_datetime_format);

        dateformatPref.setSummary(DateFormat.format(
                PreferenceManager
                        .getDefaultSharedPreferences(ActivityDiaryApplication.getAppContext())
                        .getString(KEY_PREF_DATE_FORMAT, def)
                , new Date()));

        autoSelectPref = mPreferenceManager.findPreference(KEY_PREF_AUTO_SELECT);
        updateAutoSelectSummary();

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

}
