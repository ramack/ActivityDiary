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
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import de.rampro.activitydiary.R;
import de.rampro.activitydiary.db.ActivityDiaryContract;

public class ActivityManagerFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor>{
    private static final String TAG = ActivityManagerFragment.class.getName();

    private static final String[] PROJECTION = ActivityDiaryContract.DiaryActivity.PROJECTION_ALL; // TODO use new String[] {ActivityDiaryContract.DiaryActivity.NAME};

    private SimpleCursorAdapter mActivitiyListAdapter;

/* TODO: set title based on fragment */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // For the cursor adapter, specify which columns go into which views
        String[] fromColumns = {ActivityDiaryContract.DiaryActivity.NAME};
        int[] toViews = {android.R.id.text1}; // The TextView in simple_list_item_1

        /* TODO: replace text1 and simple_list_item_1 by custom... */
        // Create an empty adapter we will use to display the loaded data.
        // We pass null for the cursor, then update it in onLoadFinished()
/* TODO: chose font color
        double a = 1 - ( 0.299 * color.R + 0.587 * color.G + 0.114 * color.B)/255;
        if (a < 0.5)
            black font
        else
            white font
        */

        mActivitiyListAdapter = new SimpleCursorAdapter(getActivity(),
                android.R.layout.simple_list_item_1, null,
                fromColumns, toViews, 0);
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

    public View onCreateView2(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View fragmentView = inflater.inflate(R.layout.activity_manager_fragment, container, false);

        // For the cursor adapter, specify which columns go into which views
        String[] fromColumns = {ActivityDiaryContract.DiaryActivity.NAME};
        int[] toViews = {android.R.id.text1}; // The TextView in simple_list_item_1

        /* TODO: reoplace text1 and simple_list_item_1 by custom... */
        // Create an empty adapter we will use to display the loaded data.
        // We pass null for the cursor, then update it in onLoadFinished()
        mActivitiyListAdapter = new SimpleCursorAdapter(getActivity(),
                android.R.layout.simple_list_item_1, null,
                fromColumns, toViews, 0);
        setListAdapter(mActivitiyListAdapter);

        return fragmentView;
    }

}
