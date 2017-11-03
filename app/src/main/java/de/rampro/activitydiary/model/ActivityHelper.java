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

package de.rampro.activitydiary.model;

import android.content.AsyncQueryHandler;
import android.database.Cursor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.rampro.activitydiary.ActivityDiaryApplication;
import de.rampro.activitydiary.db.ActivityDiaryContract;

/**
 * provide a smooth interface to an OO abstraction of the data for our diary.
 */
public class ActivityHelper extends AsyncQueryHandler{
    private static final int QUERY_ALL_ACTIVITIES = 0;
    private static final String[] PROJECTION = new String[] {
            ActivityDiaryContract.DiaryActivity._ID,
            ActivityDiaryContract.DiaryActivity.NAME,
            ActivityDiaryContract.DiaryActivity.COLOR
    };
    private static final String SELECTION = ActivityDiaryContract.DiaryActivity._DELETED + "=0";

    public static final ActivityHelper helper = new ActivityHelper();
    public List<DiaryActivity> activities;
    private DiaryActivity currentActivity = null;

    /* TODO: this could be done more fine grained here... (I. e. not refresh everything on just an insert or delete) */
    public interface DataChangedListener{
        /**
         * Called when the data has changed.
         *
         */
        void onActivityDataChanged();
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
                PROJECTION, SELECTION, null,
                null);
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
                mDataChangeListeners.forEach(listener -> listener.onActivityDataChanged()) ;
            }
        } else if (cursor != null) {
            cursor.close();
        }
    }

    public DiaryActivity getCurrentActivity(){
        return currentActivity;
    }

    public void setCurrentActivity(DiaryActivity activity){
        /* update the current diary entry to "finish" it */

        /* create a new diary entry */

        /* TODO: create listener class and notify the listeners here... */
        currentActivity = activity;
        /* TODO insert into Diary here... */

    }

    public void insertActivity(DiaryActivity act){
        activities.add(act);
        /* TODO: insert into ContentProvider and update id afterwards */
        mDataChangeListeners.forEach(listener -> listener.onActivityDataChanged()) ;
    }
}
