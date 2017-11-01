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
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.graphics.ColorUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;


import de.rampro.activitydiary.R;
import de.rampro.activitydiary.db.ActivityDiaryContract;

/*
 * MainActivity to show most of the UI, based on switching the fragements
 *
 * */
public class ManageActivity extends BaseActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = ManageActivity.class.getName();
    private static final String[] PROJECTION = new String[] {
            ActivityDiaryContract.DiaryActivity._ID,
            ActivityDiaryContract.DiaryActivity.NAME,
            ActivityDiaryContract.DiaryActivity.COLOR
    };

    private ListAdapter mAdapter;
    private ListView mList;
    private class DiaryActivityAdapter extends ResourceCursorAdapter {

        public DiaryActivityAdapter() {
            super(ManageActivity.this, R.layout.activity_row, null, 0);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor){
            String name = cursor.getString(cursor.getColumnIndex(ActivityDiaryContract.DiaryActivity.NAME));
            int color = cursor.getInt(cursor.getColumnIndex(ActivityDiaryContract.DiaryActivity.COLOR));

            TextView actName = (TextView) view.findViewById(R.id.activity_name);
            actName.setText(name);
            RelativeLayout bgrd = (RelativeLayout) view.findViewById(R.id.background);
            bgrd.setBackgroundColor(color);
            if(ColorUtils.calculateLuminance(color) > 0.5){
                actName.setTextColor(context.getResources().getColor(R.color.activityTextColorDark));
            }else{
                actName.setTextColor(context.getResources().getColor(R.color.activityTextColorLight));
            }

            ImageView imageView = (ImageView) view.findViewById(R.id.activity_image);
    /* TODO fill image here */
        }
    }

    private DiaryActivityAdapter mActivitiyListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View contentView = inflater.inflate(R.layout.activity_manage_content, null, false);

        setContent(contentView);
        mList = (ListView)findViewById(R.id.manage_activity_list);
        mActivitiyListAdapter = new DiaryActivityAdapter();
        mList.setAdapter(mActivitiyListAdapter);

        mList.setOnItemClickListener(mOnClickListener);

        // Prepare the loader.  Either re-connect with an existing one,
        // or start a new one.
        getLoaderManager().initLoader(0, null, this);
        mDrawerToggle.setDrawerIndicatorEnabled(false);
        mNavigationView.getMenu().findItem(R.id.nav_activity_manager).setChecked(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.manage_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle your other action bar items...
        switch(item.getItemId()) {
            case R.id.action_add_activity:
                Intent intentaddact = new Intent(ManageActivity.this, EditActivity.class);
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
        return new CursorLoader(this, ActivityDiaryContract.DiaryActivity.CONTENT_URI,
                PROJECTION, null, null, null);
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
            Uri uri = Uri.withAppendedPath(ActivityDiaryContract.DiaryActivity.CONTENT_URI,
                    c.getString(c.getColumnIndex(ActivityDiaryContract.DiaryActivity._ID)));
            Intent i = new Intent(ManageActivity.this, EditActivity.class);
            i.setData(uri);
            startActivity(i);
        }
    };

    /* TODO: implement swipe for parent / child navigation */
    /* TODO: add number of child activities in view */

}
