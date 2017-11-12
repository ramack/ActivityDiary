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

import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.CursorLoader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;


import java.util.Date;

import de.rampro.activitydiary.ActivityDiaryApplication;
import de.rampro.activitydiary.R;
import de.rampro.activitydiary.db.ActivityDiaryContract;
import de.rampro.activitydiary.helpers.FuzzyTimeSpanFormatter;


/*
 * Show this history in the Diary.
 *
 * */
public class HistoryActivity extends BaseActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = ManageActivity.class.getName();
    private static final String[] PROJECTION = new String[] {
            ActivityDiaryContract.Diary._ID,
            ActivityDiaryContract.Diary.ACT_ID,
            ActivityDiaryContract.Diary.START,
            ActivityDiaryContract.Diary.END,
            ActivityDiaryContract.DiaryActivity.NAME,
            ActivityDiaryContract.DiaryActivity.COLOR
    };
    private static final String SELECTION = ActivityDiaryContract.Diary._DELETED + "=0";

    private ListView mList;
    private class DiaryActivityAdapter extends ResourceCursorAdapter {

        public DiaryActivityAdapter() {
            super(HistoryActivity.this, R.layout.activity_history_entry, null, 0);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor){
            Date start = new Date(cursor.getLong(cursor.getColumnIndex(ActivityDiaryContract.Diary.START)));
            Date end;
            String name = cursor.getString(cursor.getColumnIndex(ActivityDiaryContract.DiaryActivity.NAME));
            int color = cursor.getInt(cursor.getColumnIndex(ActivityDiaryContract.DiaryActivity.COLOR));

            int endIdx = cursor.getColumnIndex(ActivityDiaryContract.Diary.END);
            if(cursor.isNull(endIdx)) {
                end = null;
            }else {
                end = new Date(cursor.getLong(endIdx));
            }

            TextView actName = (TextView) view.findViewById(R.id.activity_name);
            actName.setText(name);

            TextView startLabel = (TextView) view.findViewById(R.id.start_label);
            startLabel.setText(DateFormat.format(getString(R.string.default_datetime_format), start));

            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(ActivityDiaryApplication.getAppContext());
            String formatString = sharedPref.getString("pref_datetimeFormat",
                    getResources().getString(R.string.default_datetime_format));
/* TODO: register listener on preference change to redraw the date time formatting */

            startLabel.setText(DateFormat.format(formatString, start));

            TextView durationLabel = (TextView) view.findViewById(R.id.duration_label);
            durationLabel.setText(FuzzyTimeSpanFormatter.format(start, end));

            ImageView imageView = (ImageView) view.findViewById(R.id.activity_image);
            /* TODO #33: set picture */

            view.findViewById(R.id.activity_background).setBackgroundColor(color);
            /* TODO #34: adjust also text color */
        }
    }

    private DiaryActivityAdapter mActivitiyListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View contentView = inflater.inflate(R.layout.activity_history, null, false);

        setContent(contentView);
        mList = (ListView)findViewById(R.id.history_list);
        mActivitiyListAdapter = new DiaryActivityAdapter();
        mList.setAdapter(mActivitiyListAdapter);

        mList.setOnItemClickListener(mOnClickListener);

        // Prepare the loader.  Either re-connect with an existing one,
        // or start a new one.
        // and yes, for performance reasons it is good to do it the relational way and not with an OO design
        getLoaderManager().initLoader(0, null, this);
        mDrawerToggle.setDrawerIndicatorEnabled(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        /* todo change menu */
        /* TODO #25: ADD a search */
/*        inflater.inflate(R.menu.manage_menu, menu);*/
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle your other action bar items...
        switch(item.getItemId()) {
            case R.id.action_add_activity:
                Intent intentaddact = new Intent(HistoryActivity.this, EditActivity.class);
                startActivity(intentaddact);
                break;
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    // Called when a new Loader needs to be created
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return new CursorLoader(this, ActivityDiaryContract.Diary.CONTENT_URI,
                PROJECTION, SELECTION, null, null);
    }

    // Called when a previously created loader has finished loading
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Swap the new cursor in.  (The framework will take care of closing the
        // old cursor once we return.)
        mActivitiyListAdapter.swapCursor(data);
    }

    // Called when a previously created loader is reset, making the data unavailable
    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed.  We need to make sure we are no
        // longer using it.
        mActivitiyListAdapter.swapCursor(null);
    }

    private AdapterView.OnItemClickListener mOnClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> parent, View v, int position, long id)
        {
            Cursor c = (Cursor)parent.getItemAtPosition(position);
            /* TODO */
        }
    };

    @Override
    public void onResume(){
        mNavigationView.getMenu().findItem(R.id.nav_diary).setChecked(true);
        super.onResume();
    }

}
