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
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;

import de.rampro.activitydiary.ActivityDiaryApplication;
import de.rampro.activitydiary.db.ActivityDiaryContract;
import de.rampro.activitydiary.model.conditions.AlphabeticalCondition;
import de.rampro.activitydiary.model.conditions.Condition;
import de.rampro.activitydiary.model.DiaryActivity;
import de.rampro.activitydiary.model.conditions.GlobalOccurrenceCondition;
import de.rampro.activitydiary.model.conditions.PredecessorCondition;
import de.rampro.activitydiary.ui.settings.SettingsActivity;

/**
 * provide a smooth interface to an OO abstraction of the data for our diary.
 */
public class ActivityHelper extends AsyncQueryHandler{
    private static final String TAG = ActivityHelper.class.getName();

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
            ActivityDiaryContract.Diary.TABLE_NAME + "." + ActivityDiaryContract.Diary._ID,
            ActivityDiaryContract.Diary.NOTE
    };
    private static final String[] ACTIVITIES_PROJ = new String[] {
            ActivityDiaryContract.DiaryActivity._ID,
            ActivityDiaryContract.DiaryActivity.NAME,
            ActivityDiaryContract.DiaryActivity.COLOR
    };
    private static final String SELECTION = ActivityDiaryContract.DiaryActivity._DELETED + "=0";

    public static final ActivityHelper helper = new ActivityHelper();
    private List<DiaryActivity> activities;
    private DiaryActivity mCurrentActivity = null;
    private Date mCurrentActivityStartTime;
    private Uri mCurrentDiaryUri;
    private String mCurrentNote;
    private DataSetObserver mDataObserver;
    private Condition[] conditions;
    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        /*
         * handleMessage() defines the operations to perform when
         * the Handler receives a new Message to process.
         */
        @Override
        public void handleMessage(Message inputMessage) {
            // so far we only have one message here so no need to look at the details
            // just assume that at least one Condition evaluation is finished and we check
            // whether all are done
            boolean allDone = true;
            for(Condition c:conditions){
                if(c.isActive()){
                    allDone = false;
                    break;
                }
            }
            if(allDone) {
                reorderActivites();
            }
        }

    };

    /* to be used only in the UI thread */
    public List<DiaryActivity> getActivities() {
        return activities;
    }

    public interface DataChangedListener{
        /**
         * Called when the data has changed and no further specification is possible.
         * => everything needs to be refreshed!
         */
        void onActivityDataChanged();

        /**
         * Called when the data of one activity was changed.
         */
        void onActivityDataChanged(DiaryActivity activity);

        /**
         * Called on addition of an activity.
         */
        void onActivityAdded(DiaryActivity activity);

        /**
         * Called on removale of an activity.
         */
        void onActivityRemoved(DiaryActivity activity);

        /**
         * Called on change of the current activity.
         */
        void onActivityChanged();

        /**
         * Called on change of the activity order due to likelyhood.
         */
        void onActivityOrderChanged();

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

        conditions = new Condition[]{new PredecessorCondition(this),
                new AlphabeticalCondition(this),
                new GlobalOccurrenceCondition(this)};

        startQuery(QUERY_ALL_ACTIVITIES, null, ActivityDiaryContract.DiaryActivity.CONTENT_URI,
                ACTIVITIES_PROJ, SELECTION, null,
                null);
        startQuery(QUERY_CURRENT_ACTIVITY, null, ActivityDiaryContract.Diary.CONTENT_URI,
                DIARY_PROJ, ActivityDiaryContract.Diary.END + " is NULL", null,
                ActivityDiaryContract.Diary.START + " DESC");
        mCurrentActivityStartTime = new Date();
        mDataObserver = new DataSetObserver(){
            public void onChanged() {
                /* notify about the data change */
                for(DataChangedListener listener : mDataChangeListeners) {
                    listener.onActivityDataChanged();
                }
            }

            public void onInvalidated() {
                /* re-read the complete data */
                startQuery(QUERY_ALL_ACTIVITIES, null, ActivityDiaryContract.DiaryActivity.CONTENT_URI,
                        ACTIVITIES_PROJ, SELECTION, null,
                        null);
            }
        };
    }

    @Override
    protected void onQueryComplete(int token, Object cookie,
                                   Cursor cursor) {
        if ((cursor != null) && cursor.moveToFirst()) {
            if(token == QUERY_ALL_ACTIVITIES) {
                synchronized (this) {
                    activities.clear();
                    while (!cursor.isAfterLast()) {
                        DiaryActivity act = new DiaryActivity(cursor.getInt(cursor.getColumnIndex(ActivityDiaryContract.DiaryActivity._ID)),
                                cursor.getString(cursor.getColumnIndex(ActivityDiaryContract.DiaryActivity.NAME)),
                                cursor.getInt(cursor.getColumnIndex(ActivityDiaryContract.DiaryActivity.COLOR)));
                        /* TODO: optimize by keeping a map with id as key and the DiaryActivities */
                        activities.add(act);
                        cursor.moveToNext();
                    }
                }
                for(DataChangedListener listener : mDataChangeListeners) {
                    listener.onActivityDataChanged();
                }
                cursor.registerDataSetObserver(mDataObserver);
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
        }
        cursor.close();
    }

    public DiaryActivity getCurrentActivity(){
        return mCurrentActivity;
    }
    public Date getCurrentActivityStartTime() { return mCurrentActivityStartTime;}
    public String getCurrentNote() { return mCurrentNote;}
    public void setCurrentNote(String str) { mCurrentNote = str;}

    public void setCurrentActivity(@Nullable DiaryActivity activity){
        /* update the current diary entry to "finish" it
         * in theory there should be only one entry with end = NULL in the diray table
         * but who knows? -> Let's update all. */
        if(mCurrentActivity != activity) {
            ContentValues values = new ContentValues();
            Long timestamp = System.currentTimeMillis();
            values.put(ActivityDiaryContract.Diary.END, timestamp);

            startUpdate(UPDATE_CLOSE_ACTIVITY, timestamp, ActivityDiaryContract.Diary.CONTENT_URI,
                    values, ActivityDiaryContract.Diary.END + " is NULL", null);

            mCurrentActivity = activity;
            mCurrentActivityStartTime.setTime(timestamp);
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
                values.put(ActivityDiaryContract.Diary.START, (Long)cookie);

                startInsert(INSERT_NEW_DIARY_ENTRY, cookie, ActivityDiaryContract.Diary.CONTENT_URI,
                        values);
            }
        }else if(token == UPDATE_ACTIVITY){
            for(DataChangedListener listener : mDataChangeListeners) {
                listener.onActivityDataChanged((DiaryActivity)cookie);
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
            synchronized (this) {
                activities.add(act);
            }
            for(DataChangedListener listener : mDataChangeListeners) {
                listener.onActivityAdded(act);
            }
            if(PreferenceManager
                    .getDefaultSharedPreferences(ActivityDiaryApplication.getAppContext())
                    .getBoolean(SettingsActivity.KEY_PREF_AUTO_SELECT, true)){
                setCurrentActivity(act);
            }
        }
    }

    public void updateActivity(DiaryActivity act) {
        startUpdate(UPDATE_ACTIVITY,
                act,
                ContentUris.withAppendedId(ActivityDiaryContract.DiaryActivity.CONTENT_URI, act.getId()),
                contentFor(act),
                null,
                null);

        for(DataChangedListener listener : mDataChangeListeners) {
            listener.onActivityDataChanged(act);
        }
    }

    /* inserts a new activity and sets it as the current one if configured in the preferences */
    public void insertActivity(DiaryActivity act){
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

        startUpdate(UPDATE_DELETE_ACTIVITY,
                act,
                ContentUris.withAppendedId(ActivityDiaryContract.DiaryActivity.CONTENT_URI, act.getId()),
                values,
                null, /* entry selected via URI */
                null);
        synchronized (this) {
            if (!activities.remove(act)) {
                Log.e(TAG, "removal of activity " + act.toString() + " failed");
            }
        }
        for(DataChangedListener listener : mDataChangeListeners) {
            listener.onActivityRemoved(act);
        }
    }

    public DiaryActivity activityWithId(int id){
        /* TODO improve performance by storing the DiaryActivities in a map or Hashtable instead of a list */
        synchronized (this) {
            for (DiaryActivity a : activities) {
                if (a.getId() == id) {
                    return a;
                }
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

    /* calculate the "search" distance between search string and model
     *
     * Code based on Levensthein distance from https://en.wikibooks.org/wiki/Algorithm_Implementation/Strings/Levenshtein_distance#Java
     */
    public static int searchDistance(CharSequence search, CharSequence model) {
        int result;
        int len0 = search.length() + 1;
        int len1 = model.length() + 1;

        // the array of distances
        int[] cost = new int[len0];
        int[] newcost = new int[len0];

        // initial cost of skipping prefix in String s0
        for (int i = 0; i < len0; i++) cost[i] = i;

        // dynamically computing the array of distances

        // transformation cost for each letter in s1
        for (int j = 1; j < len1; j++) {
            // initial cost of skipping prefix in String s1
            newcost[0] = j;

            // transformation cost for each letter in s0
            for(int i = 1; i < len0; i++) {
                // matching current letters in both strings
                int match = (search.charAt(i - 1) == model.charAt(j - 1)) ? 0 : 1;

                // computing cost for each transformation
                int cost_replace = cost[i - 1] + match;
                int cost_insert  = cost[i] + 1;
                int cost_delete  = newcost[i - 1] + 1;

                // keep minimum cost
                newcost[i] = Math.min(Math.min(cost_insert, cost_delete), cost_replace);
            }

            // swap cost/newcost arrays
            int[] swap = cost; cost = newcost; newcost = swap;
        }

        // the distance is the cost for transforming all letters in both strings
        result = cost[len0 - 1];

        // we want to give some preference for true substrings and character occurrences
        String mStr = model.toString();
        String sStr = search.toString();
        if(mStr.contains(search)){
            result = result - 20;
        }
        if(mStr.toLowerCase().contains(sStr.toLowerCase())){
            result = result - 20;
        }
        for(int i = 0; i < search.length(); i++){
            int idx = mStr.indexOf(search.charAt(i));
            if(idx < 0){
                result = result + 4;
            }
        }
        return result;
    }

    /* reevaluate ALL conditions, very heavy operation, do not trigger without need */
    public void evaluateAllConditions() {
        for (Condition c : conditions) {
            c.refresh();
        }
    }
    /* is one of the conditions currently evaluating? */
    private boolean reorderingInProgress;

    public void reorderActivites(){
        synchronized (this) {
            List<DiaryActivity> as = activities;
            HashMap<DiaryActivity, Double> likeliActivites = new HashMap<>(as.size());

            for (DiaryActivity a : as) {
                likeliActivites.put(a, new Double(0.0));
            }

            // reevaluate the conditions
            for (Condition c : conditions) {
                List<Condition.Likelihood> s = c.likelihoods();
                for (Condition.Likelihood l : s) {
                    if (!likeliActivites.containsKey(l.activity)) {
                        Log.e(TAG, "Activity " + l.activity + " not in likeliActivites " + as.contains(l.activity));
                    }
                    Double lv = likeliActivites.get(l.activity);
                    if (lv == null) {
                        Log.e(TAG, "Activity " + l.activity + " has no likelyhood in Condition " + c.getClass().getSimpleName());
                    } else {
                        likeliActivites.put(l.activity, lv + l.likelihood);
                    }
                }
            }

            List<DiaryActivity> list = new ArrayList<DiaryActivity>(likeliActivites.keySet());

            Collections.sort(list, Collections.reverseOrder(new Comparator<DiaryActivity>() {
                public int compare(DiaryActivity o1,
                                   DiaryActivity o2) {
                    return likeliActivites.get(o1).compareTo(likeliActivites.get(o2));
                }
            }));
            activities = list;
            reorderingInProgress = false;
        }
        for(DataChangedListener listener : mDataChangeListeners) {
            listener.onActivityOrderChanged();
        }
    }

    /*
     * collect results from all Conditions (if all are finished)
     * can be called from any Thread
     */
    public void conditionEvaluationFinished() {
        Message completeMessage =
                mHandler.obtainMessage();
        completeMessage.sendToTarget();
    }

}
