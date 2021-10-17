/*
 * ActivityDiary
 *
 * Copyright (C) 2017-2018 Raphael Mack http://www.raphael-mack.de
 * Copyright (C) 2018 Bc. Ondrej Janitor
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

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.AsyncQueryHandler;
import android.content.ComponentName;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.preference.PreferenceManager;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import de.rampro.activitydiary.ActivityDiaryApplication;
import de.rampro.activitydiary.R;
import de.rampro.activitydiary.db.ActivityDiaryContentProvider;
import de.rampro.activitydiary.db.ActivityDiaryContract;
import de.rampro.activitydiary.model.DetailViewModel;
import de.rampro.activitydiary.model.DiaryActivity;
import de.rampro.activitydiary.model.conditions.AlphabeticalCondition;
import de.rampro.activitydiary.model.conditions.Condition;
import de.rampro.activitydiary.model.conditions.DayTimeCondition;
import de.rampro.activitydiary.model.conditions.GlobalOccurrenceCondition;
import de.rampro.activitydiary.model.conditions.PausedCondition;
import de.rampro.activitydiary.model.conditions.PredecessorCondition;
import de.rampro.activitydiary.ui.main.MainActivity;
import de.rampro.activitydiary.ui.settings.SettingsActivity;

import static android.content.Context.JOB_SCHEDULER_SERVICE;
import static android.content.Context.NOTIFICATION_SERVICE;

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
    private static final int DELETE_LAST_DIARY_ENTRY = 7;
    private static final int REOPEN_LAST_DIARY_ENTRY = 8;
    private static final int UNDELETE_ACTIVITY = 9;

    private static final String[] DIARY_PROJ = new String[] {
            ActivityDiaryContract.Diary.ACT_ID,
            ActivityDiaryContract.Diary.START,
            ActivityDiaryContract.Diary.END,
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
    private static final String CURRENT_ACTIVITY_CHANNEL_ID = "CurrentActivity";
    private static final int CURRENT_ACTIVITY_NOTIFICATION_ID = 0;

    private static final int ACTIVITY_HELPER_REFRESH_JOB = 0;

    /* list of all activities, not including deleted ones */
    private List<DiaryActivity> activities;
    /* unsortedActivities is not allowed to be modified */
    private List<DiaryActivity> unsortedActivities;

    private DiaryActivity mCurrentActivity = null;
    private Date mCurrentActivityStartTime;
    private @Nullable Uri mCurrentDiaryUri;
    private /* @NonNull */ String mCurrentNote;
    private Condition[] conditions;

    private DetailViewModel viewModel;

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

    /* null if either no notification has be shown yet, or notification is disabled in settings */
    private @Nullable NotificationCompat.Builder notificationBuilder;
    private NotificationManagerCompat notificationManager;

    JobInfo refreshJobInfo;

    /* to be used only in the UI thread, consider getActivitiesCopy() */
    public List<DiaryActivity> getActivities() {
        return activities;
    }

    /* get a list of the activities as non-modifable copy, not guaranteed to be up to date */
    public List<DiaryActivity> getUnsortedActivities(){
        List<DiaryActivity> result = new ArrayList<DiaryActivity>(unsortedActivities.size());
        synchronized (this){
            if(unsortedActivities.isEmpty()){
                /* activities not yet loaded, so it doesn't make sense yet to read the activities */
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    /* intended empty */
                }
            }

            result.addAll(unsortedActivities);
        }
        return result;
    }

    public void scheduleRefresh() {
        int cycleTime;
        long delta = (new Date().getTime() - mCurrentActivityStartTime.getTime() + 500) / 1000;
        if(delta <= 15) {
            cycleTime = 1000 * 10;
        }else if(delta <= 45){
            cycleTime = 1000 * 20;
        }else if(delta <= 95){
            cycleTime = 1000 * 60;
        }else{
            cycleTime = 1000 * 60 * 5; /* 5 min for now. if we want we can make this time configurable in the settings */
        }
        ComponentName componentName = new ComponentName(ActivityDiaryApplication.getAppContext(), RefreshService.class);
        JobInfo.Builder builder = new JobInfo.Builder(ACTIVITY_HELPER_REFRESH_JOB, componentName);
        builder.setMinimumLatency(cycleTime);
        refreshJobInfo = builder.build();

        JobScheduler jobScheduler = (JobScheduler) ActivityDiaryApplication.getAppContext().getSystemService(JOB_SCHEDULER_SERVICE);
        int resultCode = jobScheduler.schedule(refreshJobInfo);
        if (resultCode != JobScheduler.RESULT_SUCCESS) {
            Log.w(TAG, "RefreshJob not scheduled");
        }
// TODO: do we need to keep track on the scheduled jobs, or is a waiting job with the same ID as a new one automatically canceled?
    }

    public ArrayList<DiaryActivity> sortedActivities(String query) {
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
        return filtered;
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
         * Called on removal of an activity.
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
        unsortedActivities = new ArrayList<DiaryActivity>();

        conditions = new Condition[]{new PredecessorCondition(this),
                new AlphabeticalCondition(this),
                new GlobalOccurrenceCondition(this),
                new DayTimeCondition(this),
                new PausedCondition(this)
        };
        reloadAll();

        LocationHelper.helper.updateLocation();
        mCurrentActivityStartTime = new Date();
        createNotificationChannels();
        scheduleRefresh();
    }

    /* reload all the activities from the database */
    public void reloadAll(){
        ContentResolver resolver = ActivityDiaryApplication.getAppContext().getContentResolver();
        ContentProviderClient client = resolver.acquireContentProviderClient(ActivityDiaryContract.AUTHORITY);
        ActivityDiaryContentProvider provider = (ActivityDiaryContentProvider) client.getLocalContentProvider();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
        {
            client.close();
        }
        else
        {
            client.release();
        }
        provider.resetDatabase();

        startQuery(QUERY_ALL_ACTIVITIES, null, ActivityDiaryContract.DiaryActivity.CONTENT_URI,
                ACTIVITIES_PROJ, SELECTION, null,
                null);
    }

    /* create all the notification channels */
    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel for the "current activity"
            CharSequence name = ActivityDiaryApplication.getAppContext().getResources().getString(R.string.notif_channel_current_activity_name);
            String description = ActivityDiaryApplication.getAppContext().getResources().getString(R.string.notif_channel_current_activity_desc);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel nChannel = new NotificationChannel(CURRENT_ACTIVITY_CHANNEL_ID, name, importance);
            nChannel.setDescription(description);

            NotificationManager notificationManager = (NotificationManager) ActivityDiaryApplication.getAppContext().getSystemService(
                    NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(nChannel);
        }
    }

    /* start the query to read the current activity
     * will trigger the update of currentActivity and send notifications afterwards */
    public void readCurrentActivity() {
        startQuery(QUERY_CURRENT_ACTIVITY, null, ActivityDiaryContract.Diary.CONTENT_URI,
                DIARY_PROJ, ActivityDiaryContract.Diary.START + " = (SELECT MAX("
                + ActivityDiaryContract.Diary.START + ") FROM "
                + ActivityDiaryContract.Diary.TABLE_NAME + " WHERE " + SELECTION +")"
                , null,
                ActivityDiaryContract.Diary.START + " DESC");
    }

    @Override
    protected void onQueryComplete(int token, Object cookie,
                                   Cursor cursor) {
        if ((cursor != null) && cursor.moveToFirst()) {
            if(token == QUERY_ALL_ACTIVITIES) {
                synchronized (this) {
                    activities.clear();
                    unsortedActivities.clear();
                    while (!cursor.isAfterLast()) {
                        DiaryActivity act = new DiaryActivity(cursor.getInt(cursor.getColumnIndex(ActivityDiaryContract.DiaryActivity._ID)),
                                cursor.getString(cursor.getColumnIndex(ActivityDiaryContract.DiaryActivity.NAME)),
                                cursor.getInt(cursor.getColumnIndex(ActivityDiaryContract.DiaryActivity.COLOR)));
                        /* TODO: optimize by keeping a map with id as key and the DiaryActivities */
                        activities.add(act);
                        unsortedActivities.add(act);
                        cursor.moveToNext();
                    }
                }
                readCurrentActivity();
                for(DataChangedListener listener : mDataChangeListeners) {
                    listener.onActivityDataChanged();
                }
            }else if(token == QUERY_CURRENT_ACTIVITY){
                if(!cursor.isNull(cursor.getColumnIndex(ActivityDiaryContract.Diary.END))){
                    /* no current activity */
                    mCurrentNote = "";
                    mCurrentDiaryUri = null;
                    mCurrentActivityStartTime.setTime(cursor.getLong(cursor.getColumnIndex(ActivityDiaryContract.Diary.END)));
                }else {
                    mCurrentActivity = activityWithId(cursor.getInt(cursor.getColumnIndex(ActivityDiaryContract.Diary.ACT_ID)));
                    mCurrentActivityStartTime.setTime(cursor.getLong(cursor.getColumnIndex(ActivityDiaryContract.Diary.START)));
                    mCurrentNote = cursor.getString(cursor.getColumnIndex(ActivityDiaryContract.Diary.NOTE));
                    mCurrentDiaryUri = Uri.withAppendedPath(ActivityDiaryContract.Diary.CONTENT_URI,
                                        Long.toString(cursor.getLong(cursor.getColumnIndex(ActivityDiaryContract.Diary._ID))));

                }
                showCurrentActivityNotification();

                for(DataChangedListener listener : mDataChangeListeners) {
                    listener.onActivityChanged();
                }
            }else if(token == UNDELETE_ACTIVITY){

                DiaryActivity act = (DiaryActivity)cookie;
                act.setColor(cursor.getInt(cursor.getColumnIndex(ActivityDiaryContract.DiaryActivity.COLOR)));
                act.setName(cursor.getString(cursor.getColumnIndex(ActivityDiaryContract.DiaryActivity.NAME)));
                act.setId(cursor.getInt(cursor.getColumnIndex(ActivityDiaryContract.DiaryActivity._ID)));

                for(DataChangedListener listener : mDataChangeListeners) {
                    // notify about the (re-)added activity
                    listener.onActivityAdded(act);
                }

            }
        }
        if (cursor != null) {
            cursor.close();
        }
    }

    public DiaryActivity getCurrentActivity(){
        return mCurrentActivity;
    }
    public Date getCurrentActivityStartTime() { return mCurrentActivityStartTime;}
    public String getCurrentNote() { return mCurrentNote;}
    public void setCurrentNote(String str) { mCurrentNote = str;}

    public void setCurrentActivity(@Nullable DiaryActivity activity){
        /* update the current diary entry to "finish" it
         * in theory there should be only one entry with end = NULL in the diary table
         * but who knows? -> Let's update all. */
        if(mCurrentActivity != activity) {
            ContentValues values = new ContentValues();
            Long timestamp = System.currentTimeMillis();
            values.put(ActivityDiaryContract.Diary.END, timestamp);

            startUpdate(UPDATE_CLOSE_ACTIVITY, timestamp, ActivityDiaryContract.Diary.CONTENT_URI,
                    values, ActivityDiaryContract.Diary.END + " is NULL", null);

            mCurrentActivity = activity;
            mCurrentDiaryUri = null;
            mCurrentActivityStartTime.setTime(timestamp);
            mCurrentNote = "";
            if(mCurrentActivity == null){
                // activity terminated, so we have to notify here...
                for(DataChangedListener listener : mDataChangeListeners) {
                    listener.onActivityChanged();
                }
            }
            LocationHelper.helper.updateLocation();
            showCurrentActivityNotification();
        }
    }

    public void showCurrentActivityNotification() {
        if(PreferenceManager
                .getDefaultSharedPreferences(ActivityDiaryApplication.getAppContext())
                .getBoolean(SettingsActivity.KEY_PREF_NOTIF_SHOW_CUR_ACT, true)
                && mCurrentActivity != null) {
            int col = 0;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                col = ActivityDiaryApplication.getAppContext().getResources().getColor(R.color.colorPrimary, null);
            }else {
                col = ActivityDiaryApplication.getAppContext().getResources().getColor(R.color.colorPrimary);
            }
            notificationBuilder =
                    new NotificationCompat.Builder(ActivityDiaryApplication.getAppContext(),
                            CURRENT_ACTIVITY_CHANNEL_ID)
                            .setColor(col)
                            .setSmallIcon(R.mipmap.ic_launcher) // TODO: use ic_nav_select in orange
                            .setContentTitle(mCurrentActivity.getName())
                            .setPriority(NotificationCompat.PRIORITY_LOW)
                            .setShowWhen(false);
            // TODO: add icon on implementing #33

            notificationBuilder.setOnlyAlertOnce(PreferenceManager
                    .getDefaultSharedPreferences(ActivityDiaryApplication.getAppContext())
                    .getBoolean(SettingsActivity.KEY_PREF_SILENT_RENOTIFICATIONS, true));

            notificationManager = NotificationManagerCompat.from(ActivityDiaryApplication.getAppContext());

            Intent intent = new Intent(ActivityDiaryApplication.getAppContext(), MainActivity.class);
            PendingIntent pIntent = PendingIntent.getActivity(ActivityDiaryApplication.getAppContext(), (int) System.currentTimeMillis(), intent, 0);
            notificationBuilder.setContentIntent(pIntent);
            updateNotification();
        }else{
            if(notificationManager != null) {
                notificationManager.cancel(CURRENT_ACTIVITY_NOTIFICATION_ID);
            }
            notificationBuilder = null;
        }
    }

    public void updateNotification(){
        String duration = ActivityDiaryApplication.getAppContext().getResources().
                getString(R.string.duration_description, TimeSpanFormatter.fuzzyFormat(ActivityHelper.helper.getCurrentActivityStartTime(), new Date()));

        if(notificationBuilder != null) {
            // if this comes faster than building the first notification we just ignore the update.
            // also in case the notification is disabled in the settings notificationBuilder is null
            boolean needUpdate = false;
            int idx = 0;
            for(NotificationCompat.Action a: notificationBuilder.mActions){
                if(notificationBuilder.mActions.size() - idx - 1 < activities.size()
                    &&
                   activities.get(notificationBuilder.mActions.size() - idx - 1).getId() != a.getExtras().getInt("SELECT_ACTIVITY_WITH_ID")) {
                    needUpdate = true;
                }
                idx++;
            }
            if(needUpdate || notificationBuilder.mActions.size() < 1) {
                notificationBuilder.mActions.clear();

                for (int i = 2; i >= 0; i--) {
                    if (i < activities.size()) {
                        DiaryActivity act = activities.get(i);
                        SpannableString coloredActivity = new SpannableString(act.getName());
                        coloredActivity.setSpan(new ForegroundColorSpan(act.getColor()), 0, coloredActivity.length(), 0);

                        Intent intent = new Intent(ActivityDiaryApplication.getAppContext(), MainActivity.class);
                        intent.putExtra("SELECT_ACTIVITY_WITH_ID", act.getId());
                        PendingIntent pIntent = PendingIntent.getActivity(ActivityDiaryApplication.getAppContext(), (int) System.currentTimeMillis(), intent, 0);
                        NotificationCompat.Action a = new NotificationCompat.Action(R.drawable.ic_nav_select, coloredActivity, pIntent);
                        a.getExtras().putInt("SELECT_ACTIVITY_WITH_ID", act.getId());
                        notificationBuilder.addAction(a);
                    }
                }
            }
            notificationBuilder.setContentText(duration);
            notificationManager.notify(CURRENT_ACTIVITY_NOTIFICATION_ID, notificationBuilder.build());
        }
    }

    /* undo the last activity selection by deleteing all open entries
     *
     * */
    public void undoLastActivitySelection() {
        if(mCurrentActivity != null) {
            startDelete(DELETE_LAST_DIARY_ENTRY, null,
                    ActivityDiaryContract.Diary.CONTENT_URI,
                    ActivityDiaryContract.Diary.END + " is NULL",
                    null);
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
        }else if(token == REOPEN_LAST_DIARY_ENTRY){
            mCurrentActivity = null;
            readCurrentActivity();
        }else if(token == UNDELETE_ACTIVITY){
            DiaryActivity act = (DiaryActivity)cookie;

            startQuery(UNDELETE_ACTIVITY, cookie,
                    ActivityDiaryContract.DiaryActivity.CONTENT_URI,
                    ACTIVITIES_PROJ, ActivityDiaryContract.DiaryActivity._ID + " = " + act.getId(),
                    null,
                    null);
        }
    }


    @Override
    protected void onDeleteComplete(int token, Object cookie, int result) {
        if(token == DELETE_LAST_DIARY_ENTRY){
            ContentValues values = new ContentValues();
            values.putNull(ActivityDiaryContract.Diary.END);

            startUpdate(REOPEN_LAST_DIARY_ENTRY, null,
                    ActivityDiaryContract.Diary.CONTENT_URI,
                    values,
                    ActivityDiaryContract.Diary.END + "=(SELECT MAX(" + ActivityDiaryContract.Diary.END + ") FROM " + ActivityDiaryContract.Diary.TABLE_NAME + " )",
                    null
            );
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
                unsortedActivities.add(act);
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

    /* undelete an activity with given ID */
    public DiaryActivity undeleteActivity(int id, String name){
        DiaryActivity result = new DiaryActivity(id, name, 0);
        ContentValues values = new ContentValues();
        values.put(ActivityDiaryContract.Diary._DELETED, 0);

        startUpdate(UNDELETE_ACTIVITY, result, ActivityDiaryContract.Diary.CONTENT_URI,
                values, ActivityDiaryContract.Diary._ID + " = " + id, null);

        activities.add(result);
        unsortedActivities.add(result);
        return result;
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
            if (activities.remove(act)) {
                unsortedActivities.remove(act);
            }else{
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
            if(unsortedActivities.isEmpty()){
                /* activities not yet loaded, so it doesn't make sense yet to read the activities */
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    /* intended empty */
                }
            }
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

    public @Nullable Uri getCurrentDiaryUri(){
        return mCurrentDiaryUri;
    }

    /* calculate the "search" distance between search string and model
     *
     * Code based on Levensthein distance from https://en.wikibooks.org/wiki/Algorithm_Implementation/Strings/Levenshtein_distance#Java
     */
    public static int searchDistance(CharSequence inSearch, CharSequence inModel) {
        String search = inSearch.toString().toLowerCase(Locale.getDefault()); // s0
        String model = inModel.toString().toLowerCase(Locale.getDefault());   // s1
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
        if(model.contains(search)){
            result = result - 30;
        }
        if(model.startsWith(search)){
            result = result - 10;
        }
        for(int i = 0; i < search.length(); i++){
            int idx = model.indexOf(search.charAt(i));
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
    private HashMap<DiaryActivity, Double> likeliActivites = new HashMap<>(1);
    public double likelihoodFor(DiaryActivity a){
        if(likeliActivites.containsKey(a)){
            return likeliActivites.get(a);
        }
        return 0.0;
    }

    public void reorderActivites(){
        synchronized (this) {
            List<DiaryActivity> as = activities;
            likeliActivites = new HashMap<>(as.size());

            for (DiaryActivity a : as) {
                likeliActivites.put(a, Double.valueOf(0.0));
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
        updateNotification();
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

    /* perform cyclic actions like update of timing on current activity and checking time based Conditions */
    public void cyclicUpdate(){
        // TODO add a service like RefreshService, to call this with configurable cycle time
    }
}
