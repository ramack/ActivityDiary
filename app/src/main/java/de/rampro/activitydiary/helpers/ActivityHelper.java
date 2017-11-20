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

package de.rampro.activitydiary.helpers;

import android.content.AsyncQueryHandler;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v7.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.rampro.activitydiary.ActivityDiaryApplication;
import de.rampro.activitydiary.db.ActivityDiaryContract;
import de.rampro.activitydiary.model.DiaryActivity;

/**
 * provide a smooth interface to an OO abstraction of the data for our diary.
 */
public class ActivityHelper extends AsyncQueryHandler{
    private static final int QUERY_ALL_ACTIVITIES = 0;
    private static final int UPDATE_CLOSE_ACTIVITY = 1;
    private static final int INSERT_NEW_DIARY_ENTRY = 2;
    private static final int UPDATE_ACTIVITY = 3;
    private static final int INSERT_NEW_ACTIVITY = 4;
    private static final int UPDATE_DELETE_ACTIVITY = 5;
    private static final int QUERY_CURRENT_ACTIVITY = 6;

    private static final String[] DIARY_PROJ = new String[] {
            ActivityDiaryContract.Diary.ACT_ID,
            ActivityDiaryContract.Diary.START,
            ActivityDiaryContract.Diary._ID,
            ActivityDiaryContract.Diary.NOTE
    };
    private static final String[] ACTIVITIES_PROJ = new String[] {
            ActivityDiaryContract.DiaryActivity._ID,
            ActivityDiaryContract.DiaryActivity.NAME,
            ActivityDiaryContract.DiaryActivity.COLOR
    };
    private static final String SELECTION = ActivityDiaryContract.DiaryActivity._DELETED + "=0";

    public static final ActivityHelper helper = new ActivityHelper();
    public List<DiaryActivity> activities;
    private DiaryActivity mCurrentActivity = null;
    private Date mCurrentActivityStartTime;
    private Uri mCurrentDiaryUri;
    private String mCurrentNote;

    /* TODO: this could be done more fine grained here... (I. e. not refresh everything on just an insert or delete) */
    public interface DataChangedListener{
        /**
         * Called when the data has changed.
         */
        void onActivityDataChanged();

        /**
         * Called on change of the current activity.
         */
        void onActivityChanged();
    }
    private List<DataChangedListener> mDataChangeListeners;

    public void registerDataChangeListener(DataChangedListener listener){
        mDataChangeListeners.add(listener);
    }

    public void unregisterDataChangeListener(DataChangedListener listener){
        mDataChangeListeners.remove(listener);
    }

    /* Access only allowed via ActivityHelper.helper singleton */
    private ActivityHelper(){
        super(ActivityDiaryApplication.getAppContext().getContentResolver());
        mDataChangeListeners = new ArrayList<DataChangedListener>(3);
        activities = new ArrayList<DiaryActivity>();

        startQuery(QUERY_ALL_ACTIVITIES, null, ActivityDiaryContract.DiaryActivity.CONTENT_URI,
                ACTIVITIES_PROJ, SELECTION, null,
                null);
        startQuery(QUERY_CURRENT_ACTIVITY, null, ActivityDiaryContract.Diary.CONTENT_URI,
                DIARY_PROJ, ActivityDiaryContract.Diary.END + " is NULL", null,
                ActivityDiaryContract.Diary.START + " DESC");
        mCurrentActivityStartTime = new Date();
    }

    @Override
    protected void onQueryComplete(int token, Object cookie,
                                   Cursor cursor) {
        if ((cursor != null) && cursor.moveToFirst()) {
            if(token == QUERY_ALL_ACTIVITIES) {
                activities.clear();
                while (!cursor.isAfterLast()) {
                    /* TODO: optimize by keeping a map with id as key and the DiaryActivities */
                    activities.add(new DiaryActivity(cursor.getInt(cursor.getColumnIndex(ActivityDiaryContract.DiaryActivity._ID)),
                            cursor.getString(cursor.getColumnIndex(ActivityDiaryContract.DiaryActivity.NAME)),
                            cursor.getInt(cursor.getColumnIndex(ActivityDiaryContract.DiaryActivity.COLOR))));
                    cursor.moveToNext();
                }
                for(DataChangedListener listener : mDataChangeListeners) {
                    listener.onActivityDataChanged();
                }
            }else if(token == QUERY_CURRENT_ACTIVITY){
                if(mCurrentActivity == null) {
                    mCurrentActivity = activityWithId(cursor.getInt(cursor.getColumnIndex(ActivityDiaryContract.Diary.ACT_ID)));
                    mCurrentActivityStartTime.setTime(cursor.getLong(cursor.getColumnIndex(ActivityDiaryContract.Diary.START)));
                    mCurrentNote = cursor.getString(cursor.getColumnIndex(ActivityDiaryContract.Diary.NOTE));
                    mCurrentDiaryUri = Uri.withAppendedPath(ActivityDiaryContract.Diary.CONTENT_URI,
                                        Long.toString(cursor.getLong(cursor.getColumnIndex(ActivityDiaryContract.Diary._ID))));
                    for(DataChangedListener listener : mDataChangeListeners) {
                        listener.onActivityChanged();
                    }
                }
            }
        } else if (cursor != null) {
            cursor.close();
        }
    }

    public DiaryActivity getCurrentActivity(){
        return mCurrentActivity;
    }
    public Date getCurrentActivityStartTime() { return mCurrentActivityStartTime;}
    public String getCurrentNote() { return mCurrentNote;}

    public void setCurrentActivity(@Nullable DiaryActivity activity){
        /* update the current diary entry to "finish" it
         * in theory there should be only one entry with end = NULL in the diray table
         * but who knows? -> Let's update all. */
        if(mCurrentActivity != activity) {
            ContentValues values = new ContentValues();
            values.put(ActivityDiaryContract.Diary.END, System.currentTimeMillis());

            startUpdate(UPDATE_CLOSE_ACTIVITY, null, ActivityDiaryContract.Diary.CONTENT_URI,
                    values, ActivityDiaryContract.Diary.END + " is NULL", null);

            mCurrentActivity = activity;
            mCurrentActivityStartTime.setTime(System.currentTimeMillis());
            mCurrentNote = "";
        }
    }

    @Override
    protected void onUpdateComplete(int token, Object cookie, int result) {
        if(token == UPDATE_CLOSE_ACTIVITY) {
            if(mCurrentActivity != null) {
                /* create a new diary entry */
                ContentValues values = new ContentValues();

                values.put(ActivityDiaryContract.Diary.ACT_ID, mCurrentActivity.getId());
                values.put(ActivityDiaryContract.Diary.START, System.currentTimeMillis());

                startInsert(INSERT_NEW_DIARY_ENTRY, null, ActivityDiaryContract.Diary.CONTENT_URI,
                        values);
            }
        }
    }

    @Override
    protected void onInsertComplete(int token, Object cookie, Uri uri) {
        if(token == INSERT_NEW_DIARY_ENTRY){
            mCurrentDiaryUri = uri;
            for(DataChangedListener listener : mDataChangeListeners) {
                listener.onActivityChanged();
            }

        }else if(token == INSERT_NEW_ACTIVITY){

            DiaryActivity act = (DiaryActivity)cookie;
            act.setId(Integer.parseInt(uri.getLastPathSegment()));
            for(DataChangedListener listener : mDataChangeListeners) {
                listener.onActivityDataChanged();
            }
            if(PreferenceManager
                    .getDefaultSharedPreferences(ActivityDiaryApplication.getAppContext())
                    .getBoolean("pref_auto_select_new", true)){
                setCurrentActivity(act);
            }
        }
    }

    public void updateActivity(DiaryActivity act) {
        startUpdate(UPDATE_ACTIVITY,
                null,
                ContentUris.withAppendedId(ActivityDiaryContract.DiaryActivity.CONTENT_URI, act.getId()),
                contentFor(act),
                null,
                null);

        for(DataChangedListener listener : mDataChangeListeners) {
            listener.onActivityDataChanged();
        }
    }

    /* inserts a new activity and sets it as the current one if configured in the preferences */
    public void insertActivity(DiaryActivity act){
        activities.add(act);
        startInsert(INSERT_NEW_ACTIVITY,
                act,
                ActivityDiaryContract.DiaryActivity.CONTENT_URI,
                contentFor(act));
    }

    public void deleteActivity(DiaryActivity act) {
        if(act == mCurrentActivity){
            setCurrentActivity(null);
        }
        ContentValues values = new ContentValues();
        values.put(ActivityDiaryContract.DiaryActivity._DELETED, "1");

        startUpdate(UPDATE_DELETE_ACTIVITY, null, ActivityDiaryContract.Diary.CONTENT_URI,
                values, ActivityDiaryContract.DiaryActivity._ID + " = " + act.getId(), null);
        activities.remove(act);
        for(DataChangedListener listener : mDataChangeListeners) {
            listener.onActivityDataChanged();
        }
    }

    public DiaryActivity activityWithId(int id){
        /* TODO improve performance by storing the DiaryActivities in a map or Hashtable instead of a list */
        for (DiaryActivity a:activities) {
            if(a.getId() == id){
                return a;
            }
        }
        return null;
    }

    private ContentValues contentFor(DiaryActivity act){
        ContentValues result = new ContentValues();
        result.put(ActivityDiaryContract.DiaryActivity.NAME, act.getName());
        result.put(ActivityDiaryContract.DiaryActivity.COLOR, act.getColor());
        return result;
    }

    public Uri getCurrentDiaryUri(){
        return mCurrentDiaryUri;
    }

}
