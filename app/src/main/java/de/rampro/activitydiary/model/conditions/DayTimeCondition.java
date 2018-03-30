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
import android.database.sqlite.SQLiteQueryBuilder;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import de.rampro.activitydiary.db.ActivityDiaryContract;
import de.rampro.activitydiary.helpers.ActivityHelper;
import de.rampro.activitydiary.model.DiaryActivity;
import de.rampro.activitydiary.ui.settings.SettingsActivity;

public class DayTimeCondition extends Condition implements ActivityHelper.DataChangedListener {
    HashMap<DiaryActivity, Float> activityStartTimes = new HashMap<>(127);
    private static float DAY = 24*60*60;

    public DayTimeCondition(ActivityHelper helper){
        helper.registerDataChangeListener(this);
    }

    private void updateStartTimes(){
        SQLiteQueryBuilder qBuilder = new SQLiteQueryBuilder();
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();

        qBuilder.setTables(ActivityDiaryContract.Diary.TABLE_NAME);
        Cursor c = qBuilder.query(db,
                new String[]{ActivityDiaryContract.Diary.ACT_ID
                        + ", avg(strftime('%s'," + ActivityDiaryContract.Diary.START
                        + "/1000, 'unixepoch') - strftime('%s',datetime(" + ActivityDiaryContract.Diary.START
                        + "/1000, 'unixepoch', 'start of day')))"
                        },
                null,
                null,
                ActivityDiaryContract.Diary.ACT_ID,
                null,
                null);
        c.moveToFirst();
        while (!c.isAfterLast()) {
            DiaryActivity a = ActivityHelper.helper.activityWithId(c.getInt(0));
            if(a != null) {
                Float f = c.getFloat(1);
                activityStartTimes.put(a, f);
            }
            c.moveToNext();
        }
        c.close();
    }

    @Override
    protected void doEvaluation() {
        double weight = Double.parseDouble(sharedPreferences.getString(SettingsActivity.KEY_PREF_COND_DAY_TIME, "20"));

        if(weight > 0.0000001) {
            Calendar c = Calendar.getInstance();
            long nowm = c.getTimeInMillis();
            c.set(Calendar.HOUR_OF_DAY, 0);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
            long passed = nowm - c.getTimeInMillis();
            float now = passed / 1000.0f;

            ArrayList<Likelihood> result = new ArrayList<>(ActivityHelper.helper.getActivities().size());

            for (DiaryActivity a:ActivityHelper.helper.getActivities()) {
                float start = DAY / 2.0f;
                Float af = activityStartTimes.get(a);
                if(af != null){
                    start = af;
                }else
                {
                    start = 2;
                }
                float delta = Math.abs(now - start);
                float dist = Math.min(delta, DAY - delta);

                // TODO: remove
                int hd = (int)dist/3600;
                int md = (int)(dist-hd*3600)/60;

                int absh = (int)(start/3600);
                int absm = (int)((start - 3600*absh)/60);

                // TODO: add consideration of variance...
                // FOR now we do it linear
                /* create table t (row int);
                insert into t values (1),(2),(3);
                SELECT AVG((t.row - sub.a) * (t.row - sub.a)) as var from t,
                (SELECT AVG(row) AS a FROM t) AS sub;
                */
                Likelihood l = new Likelihood(a, weight * 2.0 / DAY * (DAY/2.0 - dist));
                result.add(l);
            }
            setResult(result);
        }
    }

    /**
     * Called when the data has changed and no further specification is possible.
     * => everything needs to be refreshed!
     */
    @Override
    public void onActivityDataChanged() {
        updateStartTimes();
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
        refresh();// TODO remove
    }

}
