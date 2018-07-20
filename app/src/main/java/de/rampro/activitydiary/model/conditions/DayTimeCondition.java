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

package de.rampro.activitydiary.model.conditions;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import de.rampro.activitydiary.db.ActivityDiaryContract;
import de.rampro.activitydiary.helpers.ActivityHelper;
import de.rampro.activitydiary.model.DiaryActivity;
import de.rampro.activitydiary.ui.settings.SettingsActivity;

public class DayTimeCondition extends Condition implements ActivityHelper.DataChangedListener {
    HashMap<DiaryActivity, Float> activityStartTimeMean = new HashMap<>(127);
    HashMap<DiaryActivity, Float> activityStartTimeVar = new HashMap<>(127);
    private static float DAY = 24*60*60;

    public DayTimeCondition(ActivityHelper helper){
        helper.registerDataChangeListener(this);
    }

    private void updateStartTimes(){
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
// TODO: mean should consider "modulo" - i.e. agv of (23h and 1h) should be 0h
// TODO: extend to multiple peaks per day (configurable or better by generic k-means)
// TODO: check if utc handling is really correct...
        Cursor c = db.rawQuery(
                        "SELECT " + ActivityDiaryContract.Diary.TABLE_NAME + "."
                        + ActivityDiaryContract.Diary.ACT_ID
                        + ", sub.m as mean, "
                        + "AVG(  (   (    strftime('%s'," + ActivityDiaryContract.Diary.TABLE_NAME + "." + ActivityDiaryContract.Diary.START
                        +                      "/1000, 'unixepoch', 'utc')"
                        +             " - strftime('%s',datetime(" + ActivityDiaryContract.Diary.TABLE_NAME + "." + ActivityDiaryContract.Diary.START
                        +                      "/1000, 'unixepoch', 'start of day', 'utc'), 'utc')"
                        +           ") - sub.m"
                        +      " )*( (    strftime('%s'," + ActivityDiaryContract.Diary.TABLE_NAME + "." + ActivityDiaryContract.Diary.START
                        +                      "/1000, 'unixepoch', 'utc')"
                        +             " - strftime('%s',datetime(" + ActivityDiaryContract.Diary.TABLE_NAME + "." + ActivityDiaryContract.Diary.START
                        +                      "/1000, 'unixepoch', 'start of day', 'utc'), 'utc')"
                        +           ") - sub.m"
                        +         ")"
                        +    ") as var "
                        + "FROM " + ActivityDiaryContract.Diary.TABLE_NAME + ", "
                        + "(SELECT " + ActivityDiaryContract.Diary.ACT_ID + ", "
                        + "   AVG(    strftime('%s'," + ActivityDiaryContract.Diary.START
                        +                      "/1000, 'unixepoch', 'utc')"
                        +             " - strftime('%s',datetime(" + ActivityDiaryContract.Diary.START
                        +                      "/1000, 'unixepoch', 'start of day', 'utc'), 'utc')"
                        +        ") as m "
                        + " FROM " + ActivityDiaryContract.Diary.TABLE_NAME + " GROUP BY " + ActivityDiaryContract.Diary.ACT_ID + ") as sub "
                        + "WHERE " + ActivityDiaryContract.Diary.TABLE_NAME + "." + ActivityDiaryContract.Diary.ACT_ID + "=sub." + ActivityDiaryContract.Diary.ACT_ID
                        + " AND " + ActivityDiaryContract.Diary.TABLE_NAME + "." + ActivityDiaryContract.Diary._DELETED + "=0 "
                        + "GROUP BY " + ActivityDiaryContract.Diary.TABLE_NAME + "." + ActivityDiaryContract.Diary.ACT_ID
                ,null);
        c.moveToFirst();
        while (!c.isAfterLast()) {
            DiaryActivity a = ActivityHelper.helper.activityWithId(c.getInt(0));
            if(a != null) {
                Float mean = c.getFloat(1);
                activityStartTimeMean.put(a, mean);
                Float var = c.getFloat(2);
                if(var < 0.1){
                    // we use a sd of 30min for those activities which are only there once
                    var = (float)(30 * 60) * (30 * 60);
                }
                activityStartTimeVar.put(a, var);
            }
            c.moveToNext();
        }
        c.close();
    }

    @Override
    protected void doEvaluation() {
        double weight = Double.parseDouble(sharedPreferences.getString(SettingsActivity.KEY_PREF_COND_DAYTIME, "20"));
        ArrayList<Likelihood> result = new ArrayList<>(ActivityHelper.helper.getUnsortedActivities().size());

        if(weight > 0.0000001) {
            Calendar c = Calendar.getInstance();
            long nowm = c.getTimeInMillis();
            c.set(Calendar.HOUR_OF_DAY, 0);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
            long passed = nowm - c.getTimeInMillis();
            float now = passed / 1000.0f;
            List<DiaryActivity> list = ActivityHelper.helper.getUnsortedActivities();
            for (DiaryActivity a:list) {
                float mean = DAY / 2.0f;
                float var = 30*60*30*60;

                Float meanF = activityStartTimeMean.get(a);
                Float varF = activityStartTimeVar.get(a);
                if(meanF != null && varF != null){
                    mean = meanF;
                    var = varF;
                }

                /*
                 modulo time distance would be
                float delta = Math.abs(now - mean);
                float dist = Math.min(delta, DAY - delta);
                */

                double ld = DAY / 180 / Math.sqrt(2 * Math.PI * var); // Math.sqrt(2 * Math.PI);
                ld = ld * Math.exp(-((now - mean) * (now - mean) / (2 * var)));

                ld = ld * weight;
                Likelihood l = new Likelihood(a, ld);
                result.add(l);
            }
        }
        setResult(result);
    }

    /**
     * Called when the data has changed and no further specification is possible.
     * => everything needs to be refreshed!
     */
    @Override
    public void onActivityDataChanged() {
        updateStartTimes();
        refresh();
    }

    /**
     * Called when the data of one activity was changed.
     *
     * @param activity
     */
    @Override
    public void onActivityDataChanged(DiaryActivity activity) {

    }

    /**
     * Called on addition of an activity.
     *
     * @param activity
     */
    @Override
    public void onActivityAdded(DiaryActivity activity) {

    }

    /**
     * Called on removal of an activity.
     *
     * @param activity
     */
    @Override
    public void onActivityRemoved(DiaryActivity activity) {

    }

    /**
     * Called on change of the current activity.
     */
    @Override
    public void onActivityChanged() {
        // TODO: optimize performance: update only for the current newly selected ID
        updateStartTimes();
    }

}
