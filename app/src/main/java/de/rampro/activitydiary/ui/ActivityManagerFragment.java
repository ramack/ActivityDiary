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

import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.graphics.ColorUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import de.rampro.activitydiary.R;
import de.rampro.activitydiary.db.ActivityDiaryContract;

public class ActivityManagerFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor>{
    private static final String TAG = ActivityManagerFragment.class.getName();

    private static final String[] PROJECTION = ActivityDiaryContract.DiaryActivity.PROJECTION_ALL; // TODO use new String[] {ActivityDiaryContract.DiaryActivity.NAME};

    private class DiaryActivityAdapter extends ResourceCursorAdapter {

        public DiaryActivityAdapter() {
            super(ActivityManagerFragment.this.getActivity(), R.layout.activity_row, null, 0);
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

/* TODO: set title based on fragment */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mActivitiyListAdapter = new DiaryActivityAdapter();
        setListAdapter(mActivitiyListAdapter);

        // Prepare the loader.  Either re-connect with an existing one,
        // or start a new one.
        getLoaderManager().initLoader(0, null, this);
    }

    // Called when a new Loader needs to be created
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return new CursorLoader(getActivity(), ActivityDiaryContract.DiaryActivity.CONTENT_URI,
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

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        // TODO implement some logic
        Log.e(TAG, "clicked...");

    }

    /* TODO: implement swipe for parent / child navigation */
    /* TODO: add number of child activities in view */
}
