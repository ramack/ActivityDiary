/*
 * ActivityDiary
 *
 * Copyright (C) 2017-2018 Raphael Mack http://www.raphael-mack.de
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
package de.rampro.activitydiary.ui.main;

import android.Manifest;
import android.app.SearchManager;
import android.content.AsyncQueryHandler;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.media.ExifInterface;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.FileProvider;
import android.support.v4.content.Loader;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import de.rampro.activitydiary.ActivityDiaryApplication;
import de.rampro.activitydiary.BuildConfig;
import de.rampro.activitydiary.R;
import de.rampro.activitydiary.db.ActivityDiaryContract;
import de.rampro.activitydiary.helpers.ActivityHelper;
import de.rampro.activitydiary.helpers.FuzzyTimeSpanFormatter;
import de.rampro.activitydiary.helpers.GraphicsHelper;
import de.rampro.activitydiary.model.DiaryActivity;
import de.rampro.activitydiary.ui.generic.DetailRecyclerViewAdapter;
import de.rampro.activitydiary.ui.generic.BaseActivity;
import de.rampro.activitydiary.ui.generic.EditActivity;
import de.rampro.activitydiary.ui.settings.SettingsActivity;

/*
 * MainActivity to show most of the UI, based on switching the fragements
 *
 * */
public class MainActivity extends BaseActivity implements
        LoaderManager.LoaderCallbacks<Cursor>,
        SelectRecyclerViewAdapter.SelectListener,
        ActivityHelper.DataChangedListener,
        NoteEditDialog.NoteEditDialogListener,
        View.OnLongClickListener,
        SearchView.OnQueryTextListener,
        SearchView.OnCloseListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 4711;

    private static final String[] PROJECTION_IMG = new String[] {
            ActivityDiaryContract.DiaryImage.URI,
            ActivityDiaryContract.DiaryImage._ID
    };

    private TextView durationLabel;
    private TextView mNoteTextView;
    private String mCurrentPhotoPath;

    private RecyclerView selectRecyclerView;
    private StaggeredGridLayoutManager selectorLayoutManager;
    private SelectRecyclerViewAdapter selectAdapter;

    private RecyclerView detailRecyclerView;
    private DetailRecyclerViewAdapter detailAdapter;

    private DiaryActivity mCurrentActivity;
    private Uri mCurrentDiaryUri;
    private String filter = "";
    private View headerView;
    private int searchRowCount, normalRowCount;
    private FloatingActionButton fabNoteEdit;
    private FloatingActionButton fabAttachPicture;
    private SearchView searchView;
    private MenuItem searchMenuItem;

    private void setSearchMode(boolean searchMode){
        if(searchMode){
            headerView.setVisibility(View.GONE);
            fabNoteEdit.setVisibility(View.GONE);
            fabAttachPicture.setVisibility(View.GONE);
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            ((StaggeredGridLayoutManager)selectRecyclerView.getLayoutManager()).setSpanCount(searchRowCount);

        }else{
            ((StaggeredGridLayoutManager)selectRecyclerView.getLayoutManager()).setSpanCount(normalRowCount);

            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
            headerView.setVisibility(View.VISIBLE);
            fabNoteEdit.setVisibility(View.VISIBLE);
            fabAttachPicture.setVisibility(View.VISIBLE);
        }

    }

    private class QHandler extends AsyncQueryHandler{
        private QHandler(){
            super(ActivityDiaryApplication.getAppContext().getContentResolver());
        }
    }

    private QHandler mQHandler = new QHandler();

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("currentPhotoPath", mCurrentPhotoPath);

        // call superclass to save any view hierarchy
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // recovering the instance state
        if (savedInstanceState != null) {
            mCurrentPhotoPath = savedInstanceState.getString("currentPhotoPath");
        }

        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View contentView = inflater.inflate(R.layout.activity_main_content, null, false);

        setContent(contentView);
// TODO: check whether there is some way to use instead of inflating with root null...
//        setContentView(R.layout.activity_main_content);
        selectRecyclerView = (RecyclerView)findViewById(R.id.select_recycler);

        View selector = findViewById(R.id.activity_background);
        selector.setOnLongClickListener(this);

        TypedValue value = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.listPreferredItemHeightSmall, value, true);

        android.util.DisplayMetrics metrics = new android.util.DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        normalRowCount = (int)Math.floor((metrics.heightPixels / value.getDimension(metrics) - 2) / 2);
        searchRowCount = normalRowCount - 2;
        if(searchRowCount <= 0) searchRowCount = 1;

        selectorLayoutManager = new StaggeredGridLayoutManager(normalRowCount, StaggeredGridLayoutManager.HORIZONTAL);
        selectRecyclerView.setLayoutManager(selectorLayoutManager);
        getSupportActionBar().setSubtitle(getResources().getString(R.string.activity_subtitle_main));

        likelyhoodSort();

        headerView = findViewById(R.id.header_area);

        durationLabel = findViewById(R.id.duration_label);
        mNoteTextView = findViewById(R.id.note);

        fabNoteEdit = (FloatingActionButton) findViewById(R.id.fab_edit_note);
        fabAttachPicture = (FloatingActionButton) findViewById(R.id.fab_attach_picture);

        fabNoteEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle the click on the FAB
                if(mCurrentActivity != null) {
                    NoteEditDialog dialog = new NoteEditDialog();
                    dialog.setText(mNoteTextView.getText().toString());
                    dialog.show(getSupportFragmentManager(), "NoteEditDialogFragment");
                }else{
                    Toast.makeText(MainActivity.this, getResources().getString(R.string.no_active_activity_error), Toast.LENGTH_LONG).show();
                }
            }
        });

        fabAttachPicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle the click on the FAB
                if(mCurrentActivity != null) {
                    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                    if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                        File photoFile = null;
                        try {
                            photoFile = createImageFile();
                            Log.i(TAG, "create file for image capture " + (photoFile == null ? "" : photoFile.getAbsolutePath()));

                        } catch (IOException ex) {
                            // Error occurred while creating the File
                            Toast.makeText(MainActivity.this, getResources().getString(R.string.camera_error), Toast.LENGTH_LONG).show();
                        }
                        // Continue only if the File was successfully created
                        if (photoFile != null) {
                            // Save a file: path for use with ACTION_VIEW intents
                            mCurrentPhotoPath = photoFile.getAbsolutePath();

                            Uri photoURI = FileProvider.getUriForFile(MainActivity.this,
                                    BuildConfig.APPLICATION_ID + ".fileprovider",
                                    photoFile);
                            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                            takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                        }

                    }
                }else{
                    Toast.makeText(MainActivity.this, getResources().getString(R.string.no_active_activity_error), Toast.LENGTH_LONG).show();
                }
            }
        });

        fabNoteEdit.show();
        PackageManager pm = getPackageManager();

        if(pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            fabAttachPicture.show();
        }else{
            fabAttachPicture.hide();
        }

        detailRecyclerView = (RecyclerView)findViewById(R.id.detail_recycler);
        LinearLayoutManager detailLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false) {

            @Override
            public void onMeasure(RecyclerView.Recycler recycler, RecyclerView.State state,
                                  int widthSpec, int heightSpec) {
                final int widthMode = View.MeasureSpec.getMode(widthSpec);
                final int heightMode = View.MeasureSpec.getMode(heightSpec);
                final int widthSize = View.MeasureSpec.getSize(widthSpec);
                final int heightSize = View.MeasureSpec.getSize(heightSpec);
                int width = 0;
                int height = 0;
                int[] mMeasuredDimension = new int[2];
                for (int i = 0; i < getItemCount(); i++) {
                    if(i < state.getItemCount()) {
                        if (getOrientation() == HORIZONTAL) {

                            measureScrapChild(recycler, i,
                                    View.MeasureSpec.makeMeasureSpec(i, View.MeasureSpec.UNSPECIFIED),
                                    heightSpec,
                                    mMeasuredDimension);

                            width = width + mMeasuredDimension[0];
                            if (i == 0) {
                                height = mMeasuredDimension[1];
                            }
                        } else {
                            measureScrapChild(recycler, i,
                                    widthSpec,
                                    View.MeasureSpec.makeMeasureSpec(i, View.MeasureSpec.UNSPECIFIED),
                                    mMeasuredDimension);
                            height = height + mMeasuredDimension[1];
                            if (i == 0) {
                                width = mMeasuredDimension[0];
                            }
                        }
                    }
                }

                if (height < heightSize || width < widthSize) {

                    switch (widthMode) {
                        case View.MeasureSpec.EXACTLY:
                            width = widthSize;
                        case View.MeasureSpec.AT_MOST:
                        case View.MeasureSpec.UNSPECIFIED:
                    }

                    switch (heightMode) {
                        case View.MeasureSpec.EXACTLY:
                            height = heightSize;
                        case View.MeasureSpec.AT_MOST:
                        case View.MeasureSpec.UNSPECIFIED:
                    }

                    setMeasuredDimension(width, height);
                } else {
                    setMeasuredDimension((width > widthSize)?width:widthSize, (height > heightSize)?height:heightSize);
                }
            }

            private void measureScrapChild(RecyclerView.Recycler recycler, int position, int widthSpec,
                                           int heightSpec, int[] measuredDimension) {
                View view = recycler.getViewForPosition(position);
                recycler.bindViewToPosition(view, position);
                if (view != null) {
                    RecyclerView.LayoutParams p = (RecyclerView.LayoutParams) view.getLayoutParams();
                    int childWidthSpec = ViewGroup.getChildMeasureSpec(widthSpec,
                            getPaddingLeft() + getPaddingRight(), p.width);
                    int childHeightSpec = ViewGroup.getChildMeasureSpec(heightSpec,
                            getPaddingTop() + getPaddingBottom(), p.height);
                    view.measure(childWidthSpec, childHeightSpec);
                    measuredDimension[0] = view.getMeasuredWidth() + p.leftMargin + p.rightMargin;
                    measuredDimension[1] = view.getMeasuredHeight() + p.bottomMargin + p.topMargin;
                    recycler.recycleView(view);
                }
            }
        };
        detailLayoutManager.setAutoMeasureEnabled(false);
        detailRecyclerView.setLayoutManager(detailLayoutManager);
//        detailRecyclerView.setNestedScrollingEnabled(true);
        detailAdapter = new DetailRecyclerViewAdapter(MainActivity.this,
                null);
        detailRecyclerView.setAdapter(detailAdapter);

        // Get the intent, verify the action and get the search query
        Intent intent = getIntent();
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            filterActivityView(query);
        }

        onActivityChanged(); /* do this at the very end to ensure that no Loader finishes its data loading before */
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "IMG_";
        if(mCurrentActivity != null){
            imageFileName += mCurrentActivity.getName();
            imageFileName += "_";
        }

        imageFileName += timeStamp;
        File storageDir = GraphicsHelper.imageStorageDirectory();
        int permissionCheck = ContextCompat.checkSelfPermission(ActivityDiaryApplication.getAppContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if(permissionCheck != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                Toast.makeText(this,R.string.perm_write_external_storage_xplain, Toast.LENGTH_LONG).show();
            }
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
            storageDir = null;
        }

        if(storageDir != null){
            File image = new File(storageDir, imageFileName + ".jpg");
            image.createNewFile();
/* #80            File image = File.createTempFile(
                    imageFileName,
                    ".jpg",
                    storageDir
            );
            */
            return image;
        }else{
            return null;
        }

    }

    @Override
    public void onResume() {
        mNavigationView.getMenu().findItem(R.id.nav_main).setChecked(true);
        ActivityHelper.helper.registerDataChangeListener(this);
        super.onResume();
        updateDurationTextView();
        updateDurationHandler.postDelayed(updateDurationRunnable, 10 * 1000);

        selectAdapter.notifyDataSetChanged(); // redraw the complete recyclerview
        ActivityHelper.helper.evaluateAllConditions(); // this is quite heavy and I am not so sure whether it is a good idea to do it unconditionally here...
    }

    @Override
    public void onPause() {
        updateDurationHandler.removeCallbacks(updateDurationRunnable);
        ActivityHelper.helper.unregisterDataChangeListener(this);

        super.onPause();
    }

    @Override
    public boolean onLongClick(View view) {
        Intent i = new Intent(MainActivity.this, EditActivity.class);
        if(mCurrentActivity != null) {
            i.putExtra("activityID", mCurrentActivity.getId());
        }
        startActivity(i);
        return true;
    }

    @Override
    public boolean onItemLongClick(int adapterPosition){
        Intent i = new Intent(MainActivity.this, EditActivity.class);
        if(mCurrentActivity != null) {
            i.putExtra("activityID", selectAdapter.item(adapterPosition).getId());
        }
        startActivity(i);
        return true;
    }

    @Override
    public void onItemClick(int adapterPosition) {
        DiaryActivity newAct = selectAdapter.item(adapterPosition);
        ActivityHelper.helper.setCurrentActivity(newAct);

        searchView.setQuery("",false);
        searchView.setIconified(true);


        SpannableStringBuilder snackbarText = new SpannableStringBuilder();
        snackbarText.append(newAct.getName());
        int end = snackbarText.length();
        snackbarText.setSpan(new ForegroundColorSpan(newAct.getColor()), 0, end, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        snackbarText.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, end, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        snackbarText.setSpan(new RelativeSizeSpan((float)1.4152), 0, end, Spannable.SPAN_INCLUSIVE_INCLUSIVE);

        Snackbar undoSnackBar = Snackbar.make(findViewById(R.id.main_layout),
                snackbarText, Snackbar.LENGTH_LONG);
        undoSnackBar.setAction(R.string.action_undo, new View.OnClickListener() {
            /**
             * Called when a view has been clicked.
             *
             * @param v The view that was clicked.
             */
            @Override
            public void onClick(View v) {
                Log.v(TAG, "UNDO Activity Selection");
                ActivityHelper.helper.undoLastActivitySelection();
            }
        });
        undoSnackBar.show();
    }

    private Handler updateDurationHandler = new Handler();
    private Runnable updateDurationRunnable = new Runnable() {
        @Override
        public void run() {
            updateDurationTextView();
            updateDurationHandler.postDelayed(this, 10 * 1000);
        }
    };

    public void onActivityChanged(){
        DiaryActivity newAct = ActivityHelper.helper.getCurrentActivity();
        boolean onlyRefresh = false;
        if(mCurrentActivity == newAct){
            onlyRefresh = true;
        }
        mCurrentActivity = newAct;
        if(mCurrentActivity != null) {
            mCurrentDiaryUri = ActivityHelper.helper.getCurrentDiaryUri();
            TextView aName = (TextView) findViewById(R.id.activity_name);
            aName.setText(mCurrentActivity.getName());
            findViewById(R.id.activity_background).setBackgroundColor(mCurrentActivity.getColor());
            aName.setTextColor(GraphicsHelper.textColorOnBackground(mCurrentActivity.getColor()));

            updateDurationTextView();
            /* TODO: move note and starttime from ActivityHelper to here
             * register a listener to get updates directly from the ContentProvider */

        }else{
            /* This should be really seldom, actually only at very first start or if something went wrong.
             * In those cases we keep the default text from the xml. */
            mCurrentDiaryUri = null;
        }
        getSupportLoaderManager().restartLoader(0, null, this);
        selectorLayoutManager.scrollToPosition(0);
    }

    private void updateDurationTextView() {
        String duration = getResources().getString(R.string.duration_description, FuzzyTimeSpanFormatter.format(ActivityHelper.helper.getCurrentActivityStartTime(), new Date()));
        durationLabel.setText(duration);
        mNoteTextView.setText(ActivityHelper.helper.getCurrentNote());
    }

    /**
     * Called on change of the activity order due to likelyhood.
     */
    @Override
    public void onActivityOrderChanged() {
        /* only do likelihood sort in case we are not in a search */
        if(filter.length() == 0){
            likelyhoodSort();
        }
    }

    /**
     * Called when the data has changed.
     */
    @Override
    public void onActivityDataChanged() {
        selectAdapter.notifyDataSetChanged();
    }

    @Override
    public void onActivityDataChanged(DiaryActivity activity){
        selectAdapter.notifyItemChanged(selectAdapter.positionOf(activity));
    }

    /**
     * Called on addition of an activity.
     *
     * @param activity
     */
    @Override
    public void onActivityAdded(DiaryActivity activity) {
        /* no need to add it, as due to the reevaluation of the conditions the order change will happen */
    }

    /**
     * Called on removale of an activity.
     *
     * @param activity
     */
    @Override
    public void onActivityRemoved(DiaryActivity activity) {
        selectAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);

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
        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setSearchMode(true);
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.action_add_activity:
                startActivity(new Intent(this, EditActivity.class));
                break;
            /* filtering is handled by the SearchView widget
            case R.id.action_filter:
            */
        }
        return super.onOptionsItemSelected(item);
    }

    protected void onNewIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            filterActivityView(query);
        }

        if (intent.hasExtra("SELECT_ACTIVITY_WITH_ID")) {
            int id = intent.getIntExtra("SELECT_ACTIVITY_WITH_ID", -1);
            ActivityHelper.helper.setCurrentActivity(ActivityHelper.helper.activityWithId(id));
        }
    }

    private void filterActivityView(String query){
        this.filter = query;
        // TODO: do in separate thread?
        ArrayList<DiaryActivity> filtered = new ArrayList<DiaryActivity>(ActivityHelper.helper.getActivities().size());
        ArrayList<Integer> filteredDist = new ArrayList<Integer>(ActivityHelper.helper.getActivities().size());

        for(DiaryActivity a : ActivityHelper.helper.getActivities()){
            int dist = ActivityHelper.searchDistance(query, a.getName());
            int pos = 0;
            // search where to enter it
            for(Integer i : filteredDist){
                if(dist > i.intValue()){
                    pos++;
                }else{
                    break;
                }
            }

            filteredDist.add(pos, Integer.valueOf(dist));
            filtered.add(pos, a);
        }

        selectAdapter = new SelectRecyclerViewAdapter(MainActivity.this, filtered);
        selectRecyclerView.swapAdapter(selectAdapter, false);
    }

    private void likelyhoodSort() {
        selectAdapter = new SelectRecyclerViewAdapter(MainActivity.this, ActivityHelper.helper.getActivities());
        selectRecyclerView.swapAdapter(selectAdapter, false);
    }

    @Override
    public boolean onClose() {
        setSearchMode(false);
        likelyhoodSort();
        return false; /* we wanna clear and close the search */
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        setSearchMode(false);
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        filterActivityView(newText);
        return true; /* we handle the search directly, so no suggestions need to be show even if #70 is implemented */
    }

    @Override
    public void onNoteEditPositiveClock(String str, DialogFragment dialog) {
        ContentValues values = new ContentValues();
        values.put(ActivityDiaryContract.Diary.NOTE, str);

        mQHandler.startUpdate(0,
                null,
                mCurrentDiaryUri,
                values,
                null, null);
        mNoteTextView.setText(str);
        resizeNote();

        ActivityHelper.helper.setCurrentNote(str);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            if(mCurrentPhotoPath != null && mCurrentDiaryUri != null) {
                Uri photoURI = FileProvider.getUriForFile(MainActivity.this,
                        BuildConfig.APPLICATION_ID + ".fileprovider",
                        new File(mCurrentPhotoPath));
                ContentValues values = new ContentValues();
                values.put(ActivityDiaryContract.DiaryImage.URI, photoURI.toString());
                values.put(ActivityDiaryContract.DiaryImage.DIARY_ID, mCurrentDiaryUri.getLastPathSegment());

                mQHandler.startInsert(0,
                        null,
                        ActivityDiaryContract.DiaryImage.CONTENT_URI,
                        values);

                if(PreferenceManager
                        .getDefaultSharedPreferences(ActivityDiaryApplication.getAppContext())
                        .getBoolean(SettingsActivity.KEY_PREF_TAG_IMAGES, true)) {
                    try {
                        ExifInterface exifInterface = new ExifInterface(mCurrentPhotoPath);
                        if (mCurrentActivity != null) {
                            /* TODO: #24: when using hierarchical activities tag them all here, seperated with comma */
                            /* would be great to use IPTC keywords instead of EXIF UserComment, but
                             * at time of writing (2017-11-24) it is hard to find a library able to write IPTC
                             * to JPEG for android.
                             * pixymeta-android or apache/commons-imaging could be interesting for this.
                             * */
                            exifInterface.setAttribute(ExifInterface.TAG_USER_COMMENT, mCurrentActivity.getName());
                            exifInterface.saveAttributes();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "writing exif data to " + mCurrentPhotoPath + " failed", e);
                        throw new RuntimeException("writing exif data to " + mCurrentPhotoPath + " failed " + e.toString(), e);
                    }
                }
            }
        }
    }

    // Called when a new Loader needs to be created
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return new CursorLoader(this, ActivityDiaryContract.DiaryImage.CONTENT_URI,
                PROJECTION_IMG,
                ActivityDiaryContract.DiaryImage.TABLE_NAME + "." + ActivityDiaryContract.DiaryImage.DIARY_ID + "=? AND "
                 + ActivityDiaryContract.DiaryImage._DELETED + "=0",
                mCurrentDiaryUri == null ? new String[]{"0"}:new String[]{mCurrentDiaryUri.getLastPathSegment()},
                ActivityDiaryContract.DiaryImage.SORT_ORDER_DEFAULT);
    }

    // Called when a previously created loader has finished loading
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Swap the new cursor in
        detailAdapter.swapCursor(data);

        resizeNote();
    }

    private void resizeNote() {
        if(detailAdapter.getItemCount() == 0){
            LinearLayout.LayoutParams p = (LinearLayout.LayoutParams)mNoteTextView.getLayoutParams();
            p.width = LinearLayout.LayoutParams.WRAP_CONTENT;
            mNoteTextView.setLayoutParams(p);
        } else {
            LinearLayout.LayoutParams p = (LinearLayout.LayoutParams)mNoteTextView.getLayoutParams();
            Display display = getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);

            if(mNoteTextView.getText().length() == 0){
                p.width = 0;
            }else if(mNoteTextView.getText().length() < 30) {
                p.width = size.x * 40 / 100;
            }else{
                p.width = size.x * 67 / 100;
            }
            mNoteTextView.setLayoutParams(p);
        }

    }

    // Called when a previously created loader is reset, making the data unavailable
    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed.  We need to make sure we are no
        // longer using it.
        detailAdapter.swapCursor(null);
    }
}
