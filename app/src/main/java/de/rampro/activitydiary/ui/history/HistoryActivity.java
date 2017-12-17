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

package de.rampro.activitydiary.ui.history;

import android.app.LoaderManager;
import android.content.AsyncQueryHandler;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.CursorLoader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import de.rampro.activitydiary.ActivityDiaryApplication;
import de.rampro.activitydiary.R;
import de.rampro.activitydiary.db.ActivityDiaryContract;
import de.rampro.activitydiary.ui.generic.BaseActivity;
import de.rampro.activitydiary.ui.generic.DetailRecyclerViewAdapter;
import de.rampro.activitydiary.ui.generic.EditActivity;
import de.rampro.activitydiary.ui.main.NoteEditDialog;

/*
 * Show the history of the Diary.
 * */
public class HistoryActivity extends BaseActivity implements
        LoaderManager.LoaderCallbacks<Cursor>,
        NoteEditDialog.NoteEditDialogListener,
        HistoryRecyclerViewAdapter.SelectListener {

    private static final String[] PROJECTION = new String[] {
            ActivityDiaryContract.Diary.TABLE_NAME + "." + ActivityDiaryContract.Diary._ID,
            ActivityDiaryContract.Diary.ACT_ID,
            ActivityDiaryContract.Diary.START,
            ActivityDiaryContract.Diary.END,
            ActivityDiaryContract.Diary.NOTE,
            ActivityDiaryContract.DiaryActivity.NAME,
            ActivityDiaryContract.DiaryActivity.COLOR
    };
    private static final String SELECTION = ActivityDiaryContract.Diary.TABLE_NAME + "." + ActivityDiaryContract.Diary._DELETED + "=0";

    private static final int LOADER_ID_HISTORY = -1;

    private HistoryRecyclerViewAdapter historyAdapter;
    private DetailRecyclerViewAdapter detailAdapters[];

    @Override
    public void onItemClick(HistoryViewHolders viewHolder, int adapterPosition, int diaryID) {
        /* TODO: #25 filter history to show only entries of the clicked activity */
        Toast.makeText(HistoryActivity.this, "changing the activity in the history is not yet supported, maybe we filter the history one day for " + viewHolder.mName.getText(), Toast.LENGTH_LONG).show();
    }

    public boolean onItemLongClick(HistoryViewHolders viewHolder, int adapterPosition, int diaryID){
        NoteEditDialog dialog = new NoteEditDialog();
        dialog.setDiaryId(diaryID);
        dialog.setText(viewHolder.mNoteLabel.getText().toString());
        dialog.show(getSupportFragmentManager(), "NoteEditDialogFragment");
        return true;
    }

    protected class QHandler extends AsyncQueryHandler {
        /* Access only allowed via ActivityHelper.helper singleton */
        private QHandler(){
            super(ActivityDiaryApplication.getAppContext().getContentResolver());
        }
    }

    protected QHandler mQHandler = new QHandler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        detailAdapters = new DetailRecyclerViewAdapter[5];

        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View contentView = inflater.inflate(R.layout.activity_history_content, null, false);

        setContent(contentView);

        RecyclerView historyRecyclerView = (RecyclerView)findViewById(R.id.history_list);
        StaggeredGridLayoutManager detailLayoutManager = new StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL);

        detailLayoutManager.setAutoMeasureEnabled(true);

        historyRecyclerView.setLayoutManager(detailLayoutManager);

        historyAdapter = new HistoryRecyclerViewAdapter(HistoryActivity.this, this, null);
        historyRecyclerView.setAdapter(historyAdapter);

        // Prepare the loader.  Either re-connect with an existing one,
        // or start a new one.
        // and yes, for performance reasons it is good to do it the relational way and not with an OO design
        getLoaderManager().initLoader(LOADER_ID_HISTORY, null, this);
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
        if(id == LOADER_ID_HISTORY) {
            return new CursorLoader(this, ActivityDiaryContract.Diary.CONTENT_URI,
                    PROJECTION, SELECTION, null, null);
        }else{

            return new CursorLoader(HistoryActivity.this,
                    ActivityDiaryContract.DiaryImage.CONTENT_URI,
                    new String[] {ActivityDiaryContract.DiaryImage._ID,
                            ActivityDiaryContract.DiaryImage.URI},
                    ActivityDiaryContract.DiaryImage.DIARY_ID + "=? AND "
                            + ActivityDiaryContract.DiaryImage._DELETED + "=0",
                    new String[] {Long.toString(args.getLong("DiaryID"))},
                    null);
        }
    }

    // Called when a previously created loader has finished loading
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Swap the new cursor in.  (The framework will take care of closing the
        // old cursor once we return.)
        int i = loader.getId();
        if(i == LOADER_ID_HISTORY) {
            historyAdapter.swapCursor(data);
        }else{
            detailAdapters[i].swapCursor(data);
        }

    }

    // Called when a previously created loader is reset, making the data unavailable
    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed.  We need to make sure we are no
        // longer using it.
        int i = loader.getId();
        if(i == LOADER_ID_HISTORY) {
            historyAdapter.swapCursor(null);
        }else{
            detailAdapters[i].swapCursor(null);
        }

    }

    @Override
    public void onNoteEditPositiveClock(String str, DialogFragment dialog) {
        /* update note */
        NoteEditDialog dlg = (NoteEditDialog)dialog;

        ContentValues values = new ContentValues();
        values.put(ActivityDiaryContract.Diary.NOTE, str);

        mQHandler.startUpdate(0,
                null,
                Uri.withAppendedPath(ActivityDiaryContract.Diary.CONTENT_URI,
                                     Long.toString(dlg.getDiaryId())),
                values,
                null, null);

    }

    @Override
    public void onResume(){
        mNavigationView.getMenu().findItem(R.id.nav_diary).setChecked(true);
        super.onResume();
        historyAdapter.notifyDataSetChanged(); /* redraw the complete recyclerview to take care of e.g. date format changes in teh preferences etc. #36 */
    }

    public void addDetailAdapter(long diaryEntryId, DetailRecyclerViewAdapter adapter) {
        /* ensure size of detailsAdapters */
        if(detailAdapters.length <= adapter.getAdapterId())
        {
            int i = 0;
            DetailRecyclerViewAdapter[] newArray = new DetailRecyclerViewAdapter[adapter.getAdapterId() + 4];
            for(DetailRecyclerViewAdapter a: detailAdapters){
                newArray[i] = a;
                i++;
            }
            detailAdapters = newArray;
        }

        Bundle b = new Bundle();
        b.putLong("DiaryID", diaryEntryId);
        b.putInt("DetailAdapterID", adapter.getAdapterId());

        detailAdapters[adapter.getAdapterId()] = adapter;
        getLoaderManager().initLoader(adapter.getAdapterId(), b, this);

    }
}
