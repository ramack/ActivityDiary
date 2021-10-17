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
package de.rampro.activitydiary.ui.history;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.AsyncQueryHandler;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import com.google.android.material.textfield.TextInputLayout;
import androidx.fragment.app.DialogFragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.preference.PreferenceManager;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;

import java.util.Calendar;

import de.rampro.activitydiary.ActivityDiaryApplication;
import de.rampro.activitydiary.R;
import de.rampro.activitydiary.db.ActivityDiaryContract;
import de.rampro.activitydiary.helpers.ActivityHelper;
import de.rampro.activitydiary.ui.generic.BaseActivity;
import de.rampro.activitydiary.ui.generic.DetailRecyclerViewAdapter;

/*
 * HistoryDetailActivity to show details of and modify diary entries
 *
 * */
public class HistoryDetailActivity extends BaseActivity implements LoaderManager.LoaderCallbacks<Cursor>{
    private static final String[] PROJECTION_IMG = new String[]{
            ActivityDiaryContract.DiaryImage.URI,
            ActivityDiaryContract.DiaryImage._ID
    };


    private RecyclerView detailRecyclerView;
    private DetailRecyclerViewAdapter detailAdapter;

    private final int READ_ALL = 1;
    private final int UPDATE_ENTRY = 2;
    private final int UPDATE_PRE = 3;
    private final int UPDATE_SUCC = 4;
    private boolean mUpdatePending[] = new boolean[UPDATE_SUCC + 1];
    private final int OVERLAP_CHECK = 5;


    private final String[] ENTRY_PROJ = new String[]{
            ActivityDiaryContract.DiaryActivity.TABLE_NAME + "." + ActivityDiaryContract.DiaryActivity.NAME,
            ActivityDiaryContract.DiaryActivity.TABLE_NAME + "." + ActivityDiaryContract.DiaryActivity.COLOR,
            ActivityDiaryContract.Diary.TABLE_NAME + "." + ActivityDiaryContract.Diary._ID,
            ActivityDiaryContract.Diary.NOTE,
            ActivityDiaryContract.Diary.START,
            ActivityDiaryContract.Diary.END};

    private final String DIRAY_ENTRY_ID_KEY = "ENTRY_ID";
    private final String UPDATE_VALUE_KEY = "UPDATE_VALUE";
    private final String ADJUST_ADJACENT_KEY = "ADJUST_ADJACENT";

    String dateFormatString = ActivityDiaryApplication.getAppContext().getResources().getString(R.string.date_format);
    String timeFormatString = ActivityDiaryApplication.getAppContext().getResources().getString(R.string.time_format);

    /* the id of the currently displayed diary entry */
    private long diaryEntryID;

    private CardView mActivityCard;
    private TextView mActivityName;
    private CheckBox mAdjustAdjacent;
    private Button mStartDate, mEndDate, mStartTime, mEndTime;
    private Calendar start, storedStart;
    private Calendar end, storedEnd;

    private EditText mNote;
    private TextInputLayout mNoteTIL;
    private View mBackground;
    private SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(ActivityDiaryApplication.getAppContext());

    private ContentValues updateValues = new ContentValues();
    private TextView mTimeError;
    private boolean mIsCurrent;

    public static class TimePickerFragment extends DialogFragment{
        private int hour, minute;
        private TimePickerDialog.OnTimeSetListener listener;

        public void setData(TimePickerDialog.OnTimeSetListener listener,
                           int hour, int minute){
            this.hour = hour;
            this.minute = minute;
            this.listener = listener;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current time as the default values for the picker
            // Create a new instance of TimePickerDialog and return it
            return new TimePickerDialog(getActivity(), listener, hour, minute,
                    DateFormat.is24HourFormat(getActivity()));
        }
    }

    public static class DatePickerFragment extends DialogFragment{
        private int year, month, day;
        private DatePickerDialog.OnDateSetListener listener;

        public void setData(DatePickerDialog.OnDateSetListener listener,
                            int year, int mount, int day){
            this.year = year;
            this.month = mount;
            this.day = day;
            this.listener = listener;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current time as the default values for the picker
            // Create a new instance of TimePickerDialog and return it
            return new DatePickerDialog(getActivity(), listener, year, month, day);
        }
    }

    private class QHandler extends AsyncQueryHandler {
        private QHandler(){
            super(ActivityDiaryApplication.getAppContext().getContentResolver());
        }
        @Override
        protected void onQueryComplete(int token, Object cookie,
                                       Cursor cursor) {
            if ((cursor != null)) {
                if(token == READ_ALL){
                    if(cursor.moveToFirst()) {
                        start = Calendar.getInstance();
                        storedStart = Calendar.getInstance();
                        start.setTimeInMillis(cursor.getLong(cursor.getColumnIndex(ActivityDiaryContract.Diary.START)));
                        storedStart.setTimeInMillis(cursor.getLong(cursor.getColumnIndex(ActivityDiaryContract.Diary.START)));
                        end = Calendar.getInstance();
                        storedEnd = Calendar.getInstance();
                        long endMillis = cursor.getLong(cursor.getColumnIndex(ActivityDiaryContract.Diary.END));
                        storedEnd.setTimeInMillis(endMillis);
                        if(endMillis != 0) {
                            end.setTimeInMillis(endMillis);
                            mIsCurrent = false;
                        }else{
                            mIsCurrent = true;
                        }

                        if(!updateValues.containsKey(ActivityDiaryContract.Diary.NOTE)) {
                            mNote.setText(cursor.getString(cursor.getColumnIndex(ActivityDiaryContract.Diary.NOTE)));
                        }
                        mActivityName.setText(
                            cursor.getString(
                                cursor.getColumnIndex(
                                    ActivityDiaryContract.DiaryActivity.NAME)));

                        mBackground.setBackgroundColor(cursor.getInt(cursor.getColumnIndex(
                                    ActivityDiaryContract.DiaryActivity.COLOR)));

                        if(diaryEntryID == -1){
                            diaryEntryID = cursor.getLong(cursor.getColumnIndex(ActivityDiaryContract.Diary._ID));
                        }
                        overrideUpdates();
                    }
                }
                cursor.close();
            }
        }

        @Override
        protected void onUpdateComplete(int token, Object cookie, int result) {
            super.onUpdateComplete(token, cookie, result);
            if(token == UPDATE_ENTRY){
                mUpdatePending[UPDATE_ENTRY] = false;
            }
            if(token == UPDATE_SUCC){
                mUpdatePending[UPDATE_SUCC] = false;
            }
            if(token == UPDATE_PRE){
                mUpdatePending[UPDATE_PRE] = false;
            }
            int i;
            for(i = 0; i < mUpdatePending.length; i++){
                if(mUpdatePending[i]){
                    break;
                }
            }
            if(i >= mUpdatePending.length) {
                if(mIsCurrent) {
                    ActivityHelper.helper.readCurrentActivity();
                }
                finish();
            }

        }
    }

    // override the UI by the values in updateValues
    private void overrideUpdates() {
        if(updateValues.containsKey(ActivityDiaryContract.Diary.NOTE)) {
            mNote.setText((CharSequence) updateValues.get(ActivityDiaryContract.Diary.NOTE));
        }
        if(updateValues.containsKey(ActivityDiaryContract.Diary.START)) {
            start.setTimeInMillis(updateValues.getAsLong(ActivityDiaryContract.Diary.START));
        }
        if(updateValues.containsKey(ActivityDiaryContract.Diary.END)) {
            end.setTimeInMillis(updateValues.getAsLong(ActivityDiaryContract.Diary.END));
        }
        updateDateTimes();
    }

    private void updateDateTimes() {
        mStartDate.setText(DateFormat.format(dateFormatString, start));
        mStartTime.setText(DateFormat.format(timeFormatString, start));
        mEndDate.setText(DateFormat.format(dateFormatString, end));
        mEndTime.setText(DateFormat.format(timeFormatString, end));
        checkConstraints();
    }

    private QHandler mQHandler = new QHandler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        Intent i = getIntent();
        diaryEntryID = i.getIntExtra("diaryEntryID", -1);

        View contentView = inflater.inflate(R.layout.activity_history_detail_content, null, false);

        setContent(contentView);
        mActivityCard = (CardView) contentView.findViewById(R.id.activity_card);
        mActivityName = (TextView) contentView.findViewById(R.id.activity_name);
        mBackground = (View) mActivityCard.findViewById(R.id.activity_background);

        mAdjustAdjacent = (CheckBox) contentView.findViewById(R.id.adjust_adjacent);

        mNoteTIL = (TextInputLayout) contentView.findViewById(R.id.edit_activity_note_til);
        mNote = (EditText) contentView.findViewById(R.id.edit_activity_note);

        mNote.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // empty
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // empty
            }

            @Override
            public void afterTextChanged(Editable s) {
                String ss = s.toString();
                updateValues.put(ActivityDiaryContract.Diary.NOTE, ss);
            }
        });

        mStartDate = (Button)contentView.findViewById(R.id.date_start);
        mEndDate = (Button)contentView.findViewById(R.id.date_end);
        mStartTime = (Button)contentView.findViewById(R.id.time_start);
        mEndTime = (Button)contentView.findViewById(R.id.time_end);
        start = Calendar.getInstance();
        end = Calendar.getInstance();
        mTimeError = (TextView) contentView.findViewById(R.id.time_error);

        detailRecyclerView = (RecyclerView)findViewById(R.id.picture_recycler);
        RecyclerView.LayoutManager layoutMan = new StaggeredGridLayoutManager(3, StaggeredGridLayoutManager.VERTICAL);
        detailRecyclerView.setLayoutManager(layoutMan);
        detailAdapter = new DetailRecyclerViewAdapter(this,null);
        detailRecyclerView.setAdapter(detailAdapter);

        getSupportLoaderManager().restartLoader(0, null, this);

        if(savedInstanceState != null) {
            updateValues = savedInstanceState.getParcelable(UPDATE_VALUE_KEY);
            diaryEntryID = savedInstanceState.getLong(DIRAY_ENTRY_ID_KEY);
            mAdjustAdjacent.setChecked(savedInstanceState.getBoolean(ADJUST_ADJACENT_KEY));
            overrideUpdates();
        }
        for(int n = 0; n < mUpdatePending.length; n++){
            mUpdatePending[n] = false;
        }
        if(diaryEntryID == -1) {
            mQHandler.startQuery(READ_ALL,
                    null,
                    ActivityDiaryContract.Diary.CONTENT_URI,
                    ENTRY_PROJ,
                    ActivityDiaryContract.Diary.TABLE_NAME + "." + ActivityDiaryContract.Diary._ID
                            + " = (SELECT MAX(" + ActivityDiaryContract.Diary._ID + ") FROM " + ActivityDiaryContract.Diary.TABLE_NAME + ")",
                    null,
                    null);
        }else {
            mQHandler.startQuery(READ_ALL,
                    null,
                    ActivityDiaryContract.Diary.CONTENT_URI,
                    ENTRY_PROJ,
                    ActivityDiaryContract.Diary.TABLE_NAME + "." + ActivityDiaryContract.Diary._ID + "=?",
                    new String[]{Long.toString(diaryEntryID)},
                    null);
        }

        mDrawerToggle.setDrawerIndicatorEnabled(false);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close_cancel);
    }

    @Override
    public void onResume(){
        mNavigationView.getMenu().findItem(R.id.nav_diary).setChecked(true);
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(ADJUST_ADJACENT_KEY, mAdjustAdjacent.isChecked());
        outState.putLong(DIRAY_ENTRY_ID_KEY, diaryEntryID);
        outState.putParcelable(UPDATE_VALUE_KEY, updateValues);
        // call superclass to save any view hierarchy
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.edit_diary_entry_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
//            case R.id.action_edit_delete:
                /* TODO: DELETE diary entry */
//                finish();
//                break;
            case android.R.id.home:
                /* cancel edit */
                finish();
                break;
            case R.id.action_edit_done:
                /* finish edit and save */
                if(checkConstraints()) {
                    if (updateValues.size() > 0) {
                        mQHandler.startUpdate(UPDATE_ENTRY, null,
                                ContentUris.withAppendedId(ActivityDiaryContract.Diary.CONTENT_URI, diaryEntryID),
                                updateValues, null, null);
                        mUpdatePending[UPDATE_ENTRY] = true;

                        if (mAdjustAdjacent.isChecked()) {
                            if (updateValues.containsKey(ActivityDiaryContract.Diary.START)) {
                                // update also the predecessor
                                ContentValues updateEndTime = new ContentValues();
                                updateEndTime.put(ActivityDiaryContract.Diary.END, updateValues.getAsString(ActivityDiaryContract.Diary.START));
                                mQHandler.startUpdate(UPDATE_PRE, null,
                                        ActivityDiaryContract.Diary.CONTENT_URI,
                                        updateEndTime,
                                        ActivityDiaryContract.Diary.END + "=?",
                                        new String[]{Long.toString(storedStart.getTimeInMillis())});
                                mUpdatePending[UPDATE_PRE] = true;

                            }
                            if (updateValues.containsKey(ActivityDiaryContract.Diary.END)) {
                                // update also the successor
                                ContentValues updateStartTime = new ContentValues();
                                updateStartTime.put(ActivityDiaryContract.Diary.START, updateValues.getAsString(ActivityDiaryContract.Diary.END));
                                mQHandler.startUpdate(UPDATE_SUCC, null,
                                        ActivityDiaryContract.Diary.CONTENT_URI,
                                        updateStartTime,
                                        ActivityDiaryContract.Diary.START + "=?",
                                        new String[]{Long.toString(storedEnd.getTimeInMillis())});
                                mUpdatePending[UPDATE_SUCC] = true;
                            }
                        }
                    } else {
                        finish();
                    }
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean checkConstraints(){
        boolean result = true;
        if(end.getTimeInMillis() != 0 && !end.after(start)){
            result = false;
            mTimeError.setText(R.string.constraint_positive_duration);
        }

        checkForOverlap();
// TODO
        // end >= start + 1000
        // no overlap OR adjust adjacent (but still no oerlap with the next next and last last

        if(!result) {
            // TODO: make animation here, and do so only if it is not already visibile
            mTimeError.setVisibility(View.VISIBLE);
        }else{
            mTimeError.setVisibility(View.GONE);
        }
        return result;
    }

    private void checkForOverlap() {
/*        mQHandler.startQuery(OVERLAP_CHECK,
                null,
                ActivityDiaryContract.Diary.CONTENT_URI,
                new String[]{
                        ActivityDiaryContract.Diary._ID
                },
                ActivityDiaryContract.Diary.TABLE_NAME + "." + ActivityDiaryContract.Diary._ID + "=?",
                new String[]{Long.toString(start.getTimeInMillis()), Long.toString(end.getTimeInMillis())},
                null);
                */
    }

    /**
     * Instantiate and return a new Loader for the given ID.
     *
     * @param id   The ID whose loader is to be created.
     * @param args Any arguments supplied by the caller.
     * @return Return a new Loader instance that is ready to start loading.
     */
    // Called when a new Loader needs to be created
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return new CursorLoader(this, ActivityDiaryContract.DiaryImage.CONTENT_URI,
                PROJECTION_IMG,
                ActivityDiaryContract.DiaryImage.TABLE_NAME + "." + ActivityDiaryContract.DiaryImage.DIARY_ID + "=? AND "
                        + ActivityDiaryContract.DiaryImage._DELETED + "=0",
                new String[]{Long.toString(diaryEntryID)},
                ActivityDiaryContract.DiaryImage.SORT_ORDER_DEFAULT);
    }

    // Called when a previously created loader has finished loading
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Swap the new cursor in
        detailAdapter.swapCursor(data);
    }

    // Called when a previously created loader is reset, making the data unavailable
    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed.  We need to make sure we are no
        // longer using it.
        detailAdapter.swapCursor(null);
    }

    public void showStartTimePickerDialog(View v) {
        TimePickerFragment newFragment = new TimePickerFragment();
        newFragment.setData(new TimePickerDialog.OnTimeSetListener (){
                                @Override
                                public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                                    start.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                    start.set(Calendar.MINUTE, minute);
                                    start.set(Calendar.SECOND, 0);
                                    start.set(Calendar.MILLISECOND, 0);

                                    Long newStart = Long.valueOf(start.getTimeInMillis());
                                    updateValues.put(ActivityDiaryContract.Diary.START, newStart);
                                    updateDateTimes();
                                }
                            }
                , start.get(Calendar.HOUR_OF_DAY), start.get(Calendar.MINUTE));
        newFragment.show(getSupportFragmentManager(), "startTimePicker");
    }

    public void showEndTimePickerDialog(View v) {
        TimePickerFragment newFragment = new TimePickerFragment();
        newFragment.setData(new TimePickerDialog.OnTimeSetListener (){
                                @Override
                                public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                                    end.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                    end.set(Calendar.MINUTE, minute);
                                    end.set(Calendar.SECOND, 0);
                                    end.set(Calendar.MILLISECOND, 0);

                                    Long newEnd = Long.valueOf(end.getTimeInMillis());
                                    updateValues.put(ActivityDiaryContract.Diary.END, newEnd);
                                    updateDateTimes();
                                }
                            }
                , end.get(Calendar.HOUR_OF_DAY), end.get(Calendar.MINUTE));
        newFragment.show(getSupportFragmentManager(), "endTimePicker");
    }

    public void showStartDatePickerDialog(View v) {
        DatePickerFragment newFragment = new DatePickerFragment();
        newFragment.setData(new DatePickerDialog.OnDateSetListener (){
                                @Override
                                public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                                    start.set(Calendar.YEAR, year);
                                    start.set(Calendar.MONTH, month);
                                    start.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                                    Long newStart = Long.valueOf(start.getTimeInMillis());
                                    updateValues.put(ActivityDiaryContract.Diary.START, newStart);
                                    updateDateTimes();
                                }
                            }
                , start.get(Calendar.YEAR), start.get(Calendar.MONTH), start.get(Calendar.DAY_OF_MONTH));
        newFragment.show(getSupportFragmentManager(), "startDatePicker");
    }

    public void showEndDatePickerDialog(View v) {
        DatePickerFragment newFragment = new DatePickerFragment();
        newFragment.setData(new DatePickerDialog.OnDateSetListener (){
                                @Override
                                public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                                    end.set(Calendar.YEAR, year);
                                    end.set(Calendar.MONTH, month);
                                    end.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                                    Long newEnd = Long.valueOf(end.getTimeInMillis());
                                    updateValues.put(ActivityDiaryContract.Diary.END, newEnd);
                                    updateDateTimes();
                                }
                            }
                , end.get(Calendar.YEAR), end.get(Calendar.MONTH), end.get(Calendar.DAY_OF_MONTH));
        newFragment.show(getSupportFragmentManager(), "endDatePicker");
    }
}
