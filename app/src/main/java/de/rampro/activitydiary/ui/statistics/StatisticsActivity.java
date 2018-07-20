/*
 * ActivityDiary
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

package de.rampro.activitydiary.ui.statistics;

import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IValueFormatter;
import com.github.mikephil.charting.utils.ViewPortHandler;

import org.osmdroid.config.Configuration;


import java.util.ArrayList;
import java.util.List;

import de.rampro.activitydiary.R;
import de.rampro.activitydiary.db.ActivityDiaryContract;
import de.rampro.activitydiary.ui.generic.BaseActivity;

public class StatisticsActivity extends BaseActivity implements LoaderManager.LoaderCallbacks<Cursor>{
    private static final int LOADER_ID_TIME = 0;

    private static final String[] PROJECTION = new String[] {
            ActivityDiaryContract.DiaryStats.NAME,
            ActivityDiaryContract.DiaryStats.COLOR,
            ActivityDiaryContract.DiaryStats.PORTION,
            ActivityDiaryContract.DiaryStats.DURATION
    };
    private PieChart chart;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View contentView = inflater.inflate(R.layout.activity_statistics, null, false);

        setContent(contentView);

        getLoaderManager().initLoader(LOADER_ID_TIME, null, this);
        chart = (PieChart) findViewById(R.id.piechart);
        chart.getLegend().setEnabled(false);
        chart.setDescription(null);
        chart.setHoleRadius(30.0f);
        chart.setTransparentCircleRadius(40.0f);

        mDrawerToggle.setDrawerIndicatorEnabled(false);
}

    // Called when a new Loader needs to be created
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        if(id == LOADER_ID_TIME) {

            return new CursorLoader(this,
                    ActivityDiaryContract.DiaryStats.CONTENT_URI,
                    PROJECTION,
                    null,
                    null,
                    ActivityDiaryContract.DiaryStats.SORT_ORDER_DEFAULT);
        }else{
            return null;
        }
    }

    // Called when a previously created loader has finished loading
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Swap the new cursor in.  (The framework will take care of closing the
        // old cursor once we return.)

        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();

        int portion_idx = data.getColumnIndex(ActivityDiaryContract.DiaryStats.PORTION);
        int name_idx = data.getColumnIndex(ActivityDiaryContract.DiaryStats.NAME);
        int col_idx = data.getColumnIndex(ActivityDiaryContract.DiaryStats.COLOR);
        int dur_idx = data.getColumnIndex(ActivityDiaryContract.DiaryStats.DURATION);

        if ((data != null) && data.moveToFirst()) {
            float acc = 0.0f;
            float acc_po = 0.0f;
            while (!data.isAfterLast()) {
                float portion = data.getFloat(portion_idx);
                long duration = data.getLong(dur_idx);
                if(portion > 3.0f){
                    PieEntry ent = new PieEntry((float)duration, data.getString(name_idx));
                    entries.add(ent);
                    colors.add(data.getInt(col_idx));
                }else{
                    // accumulate the small, not shown entries
                    acc += duration;
                    acc_po += portion;
                }
                data.moveToNext();
            }
            if(acc_po > 2.0f) {
                entries.add(new PieEntry(acc, getResources().getString(R.string.statistics_rest)));
                colors.add(Color.GRAY);
            }
        }

        PieDataSet set = new PieDataSet(entries, getResources().getString(R.string.activities));
        PieData dat = new PieData(set);
        set.setColors(colors);

        set.setValueFormatter(new IValueFormatter() {
            @Override
            public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {
                PieEntry e = (PieEntry)entry;
                long dur = (long)e.getValue() / 1000;
                int d = (int)(dur / 60 / 60 / 24);
                int h = (int)((dur - d * (60 * 60 * 24)) / 60 / 60);
                int m = (int)((dur - d * (60 * 60 * 24) - h * (60 * 60)) / 60);
                int s = (int)((dur - d * (60 * 60 * 24) - h * (60 * 60) - m * 60));
                String result;
                if(d > 0){
                    result = d + "d";
                }else{
                    result = "";
                }

                if(d > 0 || h > 0){
                    result += h + "h";
                }
                if(d > 0 || h > 0 || m > 0){
                    result += m + "m";
                }
                if(d > 0 || h > 0 || m > 0 || s > 0){
                    result += s + "s";
                }
                return result;
            }
        });
        chart.setData(dat);
        chart.setUsePercentValues(true);
        chart.setRotationAngle(180.0f);
        chart.invalidate(); // refresh

    }

    // Called when a previously created loader is reset, making the data unavailable
    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed.  We need to make sure we are no
        // longer using it.
    }

    @Override
    public void onResume() {
        mNavigationView.getMenu().findItem(R.id.nav_statistics).setChecked(true);
        super.onResume();
    }
}
