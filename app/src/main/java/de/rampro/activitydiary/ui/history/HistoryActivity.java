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
import android.app.SearchManager;
import android.content.AsyncQueryHandler;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.CursorLoader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
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
        HistoryRecyclerViewAdapter.SelectListener, SearchView.OnCloseListener, SearchView.OnQueryTextListener {

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
    private MenuItem searchMenuItem;
    private SearchView searchView;

    @Override
    public void onItemClick(HistoryViewHolders viewHolder, int adapterPosition, int diaryID) {
        Intent i = new Intent(this, HistoryDetailActivity.class);
        i.putExtra("diaryEntryID", diaryID);
        startActivity(i);
    }

    public boolean onItemLongClick(HistoryViewHolders viewHolder, int adapterPosition, int diaryID){
        NoteEditDialog dialog = new NoteEditDialog();
        dialog.setDiaryId(diaryID);
        dialog.setText(viewHolder.mNoteLabel.getText().toString());
        dialog.show(getSupportFragmentManager(), "NoteEditDialogFragment");
        return true;
    }

    /**
     * The user is attempting to close the SearchView.
     *
     * @return true if the listener wants to override the default behavior of clearing the
     * text field and dismissing it, false otherwise.
     */
    @Override
    public boolean onClose() {
        filterHistoryView(null);
        return false;
    }

    /**
     * Called when the user submits the query. This could be due to a key press on the
     * keyboard or due to pressing a submit button.
     * The listener can override the standard behavior by returning true
     * to indicate that it has handled the submit request. Otherwise return false to
     * let the SearchView handle the submission by launching any associated intent.
     *
     * @param query the query text that is to be submitted
     * @return true if the query has been handled by the listener, false to let the
     * SearchView perform the default action.
     */
    @Override
    public boolean onQueryTextSubmit(String query) {
        filterHistoryView(query);

        return false;
    }

    /**
     * Called when the query text is changed by the user.
     *
     * @param newText the new content of the query text field.
     * @return false if the SearchView should perform the default action of showing any
     * suggestions if available, true if the action was handled by the listener.
     */
    @Override
    public boolean onQueryTextChange(String newText) {
// for now no dynamic change before starting the search...
        return false;
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
        inflater.inflate(R.menu.history_menu, menu);

        // Get the SearchView and set the searchable configuration
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchMenuItem = menu.findItem(R.id.action_filter);
        searchView = (SearchView) searchMenuItem.getActionView();
        // Assumes current activity is the searchable activity
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

        searchView.setOnCloseListener(this);
        searchView.setOnQueryTextListener(this);
        // setOnSuggestionListener -> for selection of a suggestion
        // setSuggestionsAdapter

        return true;
    }

    /**
     *
     * @param query the search string, if null resets the filter
     */
    private void filterHistoryView(@Nullable String query){
        Toast.makeText(this, "searching for " + query, Toast.LENGTH_LONG).show();
        /*
        this.filter = query;
        selectAdapter = new SelectRecyclerViewAdapter(MainActivity.this, filtered);
        selectRecyclerView.swapAdapter(selectAdapter, false);
        */
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

    protected void onNewIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            filterHistoryView(query);
        }
    }
}
