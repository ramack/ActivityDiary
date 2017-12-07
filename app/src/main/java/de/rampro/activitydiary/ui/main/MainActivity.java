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
package de.rampro.activitydiary.ui.main;

import android.Manifest;
import android.content.AsyncQueryHandler;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.media.ExifInterface;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.FileProvider;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
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

/*
 * MainActivity to show most of the UI, based on switching the fragements
 *
 * */
public class MainActivity extends BaseActivity implements
        LoaderManager.LoaderCallbacks<Cursor>,
        SelectRecyclerViewAdapter.SelectListener,
        DetailRecyclerViewAdapter.SelectListener,
        ActivityHelper.DataChangedListener,
        NoteEditDialog.NoteEditDialogListener,
        View.OnLongClickListener {
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

    private SelectRecyclerViewAdapter rcAdapter;
    private RecyclerView detailRecyclerView;

    private DetailRecyclerViewAdapter detailAdapter;

    private DiaryActivity mCurrentActivity;
    private Uri mCurrentDiaryUri;

    private class QHandler extends AsyncQueryHandler{
        /* Access only allowed via ActivityHelper.helper singleton */
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
        RecyclerView recyclerView = (RecyclerView)findViewById(R.id.select_recycler);

        View selector = contentView.findViewById(R.id.activity_background);
        selector.setOnLongClickListener(this);

        int rows;

        TypedValue value = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.listPreferredItemHeightSmall, value, true);

        android.util.DisplayMetrics metrics = new android.util.DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        rows = (int)Math.floor((metrics.heightPixels / value.getDimension(metrics) - 2) / 2);
        StaggeredGridLayoutManager selectorLayoutManager = new StaggeredGridLayoutManager(rows, StaggeredGridLayoutManager.HORIZONTAL);
        recyclerView.setLayoutManager(selectorLayoutManager);

        getSupportActionBar().setSubtitle(getResources().getString(R.string.activity_subtitle_main));

        rcAdapter = new SelectRecyclerViewAdapter(MainActivity.this, ActivityHelper.helper.activities);
        recyclerView.setAdapter(rcAdapter);

        durationLabel = (TextView) contentView.findViewById(R.id.duration_label);
        mNoteTextView =  (TextView) contentView.findViewById(R.id.note);

        FloatingActionButton fabNoteEdit = (FloatingActionButton) findViewById(R.id.fab_edit_note);
        FloatingActionButton fabAttachPicture = (FloatingActionButton) findViewById(R.id.fab_attach_picture);

        fabNoteEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle the click on the FAB
                NoteEditDialog dialog = new NoteEditDialog();
                dialog.setText(mNoteTextView.getText().toString());
                dialog.show(getSupportFragmentManager(), "NoteEditDialogFragment");
            }
        });

        fabAttachPicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle the click on the FAB
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    File photoFile = null;
                    try {
                        photoFile = createImageFile();
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
                        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                    }

                }

            }
        });

        onActivityChanged();
        fabNoteEdit.show();
        PackageManager pm = getPackageManager();

        if(pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            fabAttachPicture.show();
        }else{
            fabAttachPicture.hide();
        }

        detailRecyclerView = (RecyclerView)findViewById(R.id.detail_recycler);
        StaggeredGridLayoutManager detailLayoutManager = new StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.HORIZONTAL);

        detailLayoutManager.setAutoMeasureEnabled(true);
        detailRecyclerView.setLayoutManager(detailLayoutManager);

        detailAdapter = new DetailRecyclerViewAdapter(MainActivity.this,
                this, null);
        detailRecyclerView.setAdapter(detailAdapter);

    /* TODO #25: add a search box in the toolbar to filter / fuzzy search
    * see http://www.vogella.com/tutorials/AndroidActionBar/article.html and https://developer.android.com/training/appbar/action-views.html*/
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
            File image = File.createTempFile(
                    imageFileName,
                    ".jpg",
                    storageDir
            );

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
        onActivityChanged(); // refresh mainly the duration_label
        rcAdapter.notifyDataSetChanged(); // redraw the complete recyclerview
    }

    @Override
    public void onPause() {
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
            i.putExtra("activityID", rcAdapter.item(adapterPosition).getId());
        }
        startActivity(i);
        return true;
    }

    @Override
    public void onItemClick(int adapterPosition) {
        ActivityHelper.helper.setCurrentActivity(ActivityHelper.helper.activities.get(adapterPosition));
    }

    @Override
    public void onDetailItemClick(int adapterPosition) {
        Toast.makeText(this, "picture " + Integer.toString(adapterPosition) + " clicked -> please create a ticket and tell what you expect to be done here", Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onDetailItemLongClick(int adapterPosition) {
        //TODO: generalize the DetailView to include this code also
        //      such that it is not duplicated between MainActivity and HistoryRecyclerViewAdapter
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(R.string.dlg_delete_image_title)
                .setMessage(R.string.dlg_delete_image_text)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        ContentValues values = new ContentValues();
                        values.put(ActivityDiaryContract.DiaryImage._DELETED, 1);

                        mQHandler.startUpdate(0,
                                null,
                                ActivityDiaryContract.DiaryImage.CONTENT_URI,
                                values,
                                ActivityDiaryContract.DiaryImage._ID + "=?",
                                new String[]{Long.toString(detailAdapter.getDiaryImageIdAt(adapterPosition))}
                                );

                    }})
                .setNegativeButton(android.R.string.no, null);

        builder.create().show();
        return true;
    }

    public void onActivityChanged(){
        DiaryActivity newAct = ActivityHelper.helper.getCurrentActivity();
        mCurrentActivity = newAct;
        if(mCurrentActivity != null) {
            mCurrentDiaryUri = ActivityHelper.helper.getCurrentDiaryUri();
            TextView aName = (TextView) findViewById(R.id.activity_name);
            aName.setText(mCurrentActivity.getName());
            findViewById(R.id.activity_background).setBackgroundColor(mCurrentActivity.getColor());
            aName.setTextColor(GraphicsHelper.textColorOnBackground(mCurrentActivity.getColor()));

            String duration = getResources().getString(R.string.duration_description);
            duration += " ";
            duration += FuzzyTimeSpanFormatter.format(ActivityHelper.helper.getCurrentActivityStartTime(), new Date());
            durationLabel.setText(duration);
            mNoteTextView.setText(ActivityHelper.helper.getCurrentNote());
            /* TODO: move note and starttime from ActivityHelper to here
             * register a listener to get updates directly from the ContentProvider */

        }else{
            /* This should be really seldom, actually only at very first start or if something went wrong.
             * In those cases we keep the default text from the xml. */
            mCurrentDiaryUri = null;
        }
        getSupportLoaderManager().restartLoader(0, null, this);
    }

    /**
     * Called when the data has changed.
     */
    @Override
    public void onActivityDataChanged() {
        /* TODO: this could be done more fine grained here... */
        rcAdapter.notifyDataSetChanged();
    }

    @Override
    public void onActivityDataChanged(DiaryActivity activity){
        rcAdapter.notifyItemChanged(rcAdapter.positionOf(activity));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.action_add_activity:
                startActivity(new Intent(this, EditActivity.class));
                break;
//            case R.id.action_filter:
                /* TODO #25 filter -> open text box in actionbar to type a name, which filters using levenshtein distance */
//                Toast.makeText(this, "filtering not yet implemented.", Toast.LENGTH_LONG).show();
//                break;
        }
        return super.onOptionsItemSelected(item);
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
        ActivityHelper.helper.setCurrentNote(str);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            if(mCurrentPhotoPath != null) {
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

                try {
                    // TODO: store exif comment only if enabled in settings
                    ExifInterface exifInterface = new ExifInterface(mCurrentPhotoPath);
                    if(mCurrentActivity != null) {
                        /* TODO: #24: when using hierarchical activities tag them all here, seperated with comma */
                        /* would be great to use ICPT keywords instead of EXIF UserComment, but
                         * at time of writing (2017-11-24) it is hard to find a library able to write ICPT
                         * to JPEG for android. */
                        exifInterface.setAttribute(ExifInterface.TAG_USER_COMMENT, mCurrentActivity.getName());
                        exifInterface.saveAttributes();
                    }
                }catch (IOException e){
                    Log.e(TAG, "writing exif data to " + mCurrentPhotoPath + " failed");
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
        // Swap the new cursor in.  (The framework will take care of closing the
        // old cursor once we return.)
        detailAdapter.swapCursor(data);

        // this is a hack, as I failed to make the RecyclerView scroll correctly inside the HorizontalScrollView
        // it is neither clean nor generic, feel free to propose an improvement
        detailRecyclerView.setMinimumWidth((int)(data.getCount() * 3.3 * detailRecyclerView.getHeight()));

    }

    // Called when a previously created loader is reset, making the data unavailable
    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed.  We need to make sure we are no
        // longer using it.
        detailAdapter.swapCursor(null);
    }


}
