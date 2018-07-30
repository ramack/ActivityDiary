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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IValueFormatter;
import com.github.mikephil.charting.utils.ViewPortHandler;

import org.osmdroid.config.Configuration;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import de.rampro.activitydiary.R;
import de.rampro.activitydiary.db.ActivityDiaryContract;
import de.rampro.activitydiary.ui.generic.BaseActivity;

public class StatisticsActivity extends BaseActivity implements LoaderManager.LoaderCallbacks<Cursor>, AdapterView.OnItemSelectedListener {
    private static final int LOADER_ID_TIME = 0;
    private static final int LOADER_ID_RANGE = 1;

    private static final String[] PROJECTION = new String[] {
            ActivityDiaryContract.DiaryStats.NAME,
            ActivityDiaryContract.DiaryStats.COLOR,
            ActivityDiaryContract.DiaryStats.PORTION,
            ActivityDiaryContract.DiaryStats.DURATION
    };
    private PieChart chart;

    private Spinner timeframeSpinner;

    private TextView rangeTextView;
    private ImageView rangeEarlierImageView;
    private ImageView rangeLaterImageView;

    private long currentDateTime;
    private int currentOffset = 0;
    private int currentRange = Calendar.WEEK_OF_YEAR;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View contentView = inflater.inflate(R.layout.activity_statistics, null, false);

        setContent(contentView);
        timeframeSpinner = (Spinner) findViewById(R.id.timeframeSpinner);
        timeframeSpinner.setOnItemSelectedListener(this);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.statistic_dropdown,
                android.R.layout.simple_spinner_item);
        timeframeSpinner.setAdapter(adapter);

        getLoaderManager().initLoader(LOADER_ID_TIME, null, this);
        chart = (PieChart) findViewById(R.id.piechart);
        chart.getLegend().setEnabled(false);
        chart.setDescription(null);
        chart.setHoleRadius(30.0f);
        chart.setTransparentCircleRadius(40.0f);

        rangeTextView = (TextView)findViewById(R.id.rangeTextView);
        rangeEarlierImageView = (ImageView)findViewById(R.id.img_earlier);
        rangeEarlierImageView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                currentOffset = currentOffset - 1;
                loadRange(currentRange, currentOffset);
            }
        });
        rangeLaterImageView = (ImageView) findViewById(R.id.img_later);
        rangeLaterImageView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                currentOffset = currentOffset + 1;
                loadRange(currentRange, currentOffset);
            }
        });
        currentDateTime = new Date().getTime();

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
        }else if(id == LOADER_ID_RANGE) {
            long start = args.getLong("start");
            long end = args.getLong("end");
            String sel = "(" + ActivityDiaryContract.Diary.START + " >= ? AND " + ActivityDiaryContract.Diary.START + " < ?)"
                    + " OR (" + ActivityDiaryContract.Diary.END + " > ? AND " + ActivityDiaryContract.Diary.END + " <= ?)"
                    + " OR (" + ActivityDiaryContract.Diary.START + " < ? AND " + ActivityDiaryContract.Diary.END + " > ?)";
            return new CursorLoader(this,
                    ActivityDiaryContract.DiaryStats.CONTENT_URI,
                    PROJECTION,
                    sel,
                    new String[] {Long.toString(start),
                            Long.toString(end),
                            Long.toString(start),
                            Long.toString(end),
                            Long.toString(start),
                            Long.toString(end)},
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

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        currentDateTime = new Date().getTime();
        Bundle bnd = new Bundle();
        switch (position) {
            case 0: // all
                break;
            case 1: // last 7 days
                bnd.putLong("start", currentDateTime - (1000 * 60 * 60 * 24 * 7));
                bnd.putLong("end", currentDateTime);
                break;
            case 2: // last 30 days
                bnd.putLong("start", currentDateTime - (1000 * 60 * 60 * 24 * 7 * 30));
                bnd.putLong("end", currentDateTime);
                break;
            case 3: // week
                currentOffset = 0;
                currentRange = Calendar.WEEK_OF_YEAR;
                loadRange(currentRange, currentOffset);
                break;
            case 4: // month
                currentOffset = 0;
                currentRange = Calendar.MONTH;
                loadRange(currentRange, currentOffset);
                break;
            case 5: // year
                currentOffset = 0;
                currentRange = Calendar.YEAR;
                loadRange(currentRange, currentOffset);
                break;
            default:
        }
        if(position < 3){
            rangeTextView.setVisibility(View.INVISIBLE);
            rangeEarlierImageView.setVisibility(View.INVISIBLE);
            rangeLaterImageView.setVisibility(View.INVISIBLE);
        }else{
            rangeTextView.setVisibility(View.VISIBLE);
            rangeEarlierImageView.setVisibility(View.VISIBLE);
            rangeLaterImageView.setVisibility(View.VISIBLE);
        }
        if(position < 1) {
            getLoaderManager().restartLoader(LOADER_ID_TIME, bnd, this);
        }else if(position < 3){
            getLoaderManager().restartLoader(LOADER_ID_RANGE, bnd, this);
        }
    }

    /* field is the field of Calender, e.g. Calendar.WEEK_OF_YEAR */
    private void loadRange(int field, int offset){
        Bundle bnd = new Bundle();

        Calendar calStart = Calendar.getInstance();
        calStart.set(Calendar.HOUR_OF_DAY, 0); // ! clear would not reset the hour of day !
        calStart.clear(Calendar.MINUTE);
        calStart.clear(Calendar.SECOND);
        calStart.clear(Calendar.MILLISECOND);
        switch(field){
            case Calendar.WEEK_OF_YEAR:
                calStart.set(Calendar.DAY_OF_WEEK, calStart.getFirstDayOfWeek());
                break;
            case Calendar.MONTH:
                calStart.set(Calendar.DAY_OF_MONTH, 1);
                break;
            case Calendar.YEAR:
                calStart.set(Calendar.DAY_OF_YEAR, 1);
                break;
            default:
                throw new RuntimeException("date field not supported: " + field);
        }

        calStart.add(field, offset);

        Calendar calEnd = (Calendar) calStart.clone();
        calEnd.add(field, 1);

        SimpleDateFormat sdf;
        switch(field){
            case Calendar.WEEK_OF_YEAR:
                sdf = new SimpleDateFormat(getResources().getString(R.string.week_format));
                break;
            case Calendar.MONTH:
                sdf = new SimpleDateFormat(getResources().getString(R.string.month_format));
                break;
            case Calendar.YEAR:
                sdf = new SimpleDateFormat(getResources().getString(R.string.year_format));
                break;
            default:
                throw new RuntimeException("date field not supported: " + field);
        }
        String tt = sdf.format(calStart.getTime());
        rangeTextView.setText(tt);
        bnd.putLong("start", calStart.getTimeInMillis());
        bnd.putLong("end", calEnd.getTimeInMillis());
        getLoaderManager().restartLoader(LOADER_ID_RANGE, bnd, this);

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
