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
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

import de.rampro.activitydiary.db.ActivityDiaryContract;
import de.rampro.activitydiary.helpers.ActivityHelper;
import de.rampro.activitydiary.model.DiaryActivity;
import de.rampro.activitydiary.ui.settings.SettingsActivity;

public class PausedCondition extends Condition implements ActivityHelper.DataChangedListener {
    HashMap<DiaryActivity, Float> activityStartTimeMean = new HashMap<>(127);
    HashMap<DiaryActivity, Float> activityStartTimeVar = new HashMap<>(127);
    private static final long TIMEFRAME = 1000 * 60 * 60 * 24 * 10; // let's consider 10 days

    public PausedCondition(ActivityHelper helper){
        helper.registerDataChangeListener(this);
    }

    @Override
    protected void doEvaluation() {
        double weight = 10;
        weight = Double.parseDouble(sharedPreferences.getString(SettingsActivity.KEY_PREF_PAUSED, "10"));
        ArrayList<Likelihood> result = new ArrayList<>(ActivityHelper.helper.getUnsortedActivities().size());
        HashMap<DiaryActivity,Double> m = new HashMap<>(result.size());

        if(weight > 0.0000001) {
            SQLiteQueryBuilder qBuilder = new SQLiteQueryBuilder();
            SQLiteDatabase db = mOpenHelper.getReadableDatabase();

            qBuilder.setTables(ActivityDiaryContract.Diary.TABLE_NAME + " D, " + ActivityDiaryContract.DiaryActivity.TABLE_NAME + " A");
            Cursor c = qBuilder.query(db,
                    new String[]{"D." + ActivityDiaryContract.Diary.ACT_ID},
                    "D." + ActivityDiaryContract.Diary._DELETED + " = 0 " +
                            "AND D." + ActivityDiaryContract.Diary.ACT_ID + " = A." + ActivityDiaryContract.DiaryActivity._ID + " AND A." + ActivityDiaryContract.DiaryActivity._DELETED + " = 0 " +
                            "AND D." + ActivityDiaryContract.Diary.END + " > " + (System.currentTimeMillis() - TIMEFRAME),
                    null,
                    null,
                    null,
                    "D." + ActivityDiaryContract.Diary.END + " DESC");
            c.moveToFirst();
            long cnt = 1;
            while (!c.isAfterLast()) {
                DiaryActivity a = ActivityHelper.helper.activityWithId(c.getInt(0));
                double w = weight * Math.exp((1 - cnt) / 3.0);
                if(m.containsKey(a)){
                    w = w + m.get(a);
                }
                m.put(a, w);
                c.moveToNext();
                cnt++;
            }

            for(DiaryActivity da:m.keySet()) {
                result.add(new Likelihood(da, m.get(da)));
            }

            c.close();

        }
        setResult(result);
    }

    /**
     * Called when the data has changed and no further specification is possible.
     * => everything needs to be refreshed!
     */
    @Override
    public void onActivityDataChanged() {
        refresh();
    }

    /**
     * update the last 10 Days
     */
    private void updateHistory() {
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
        refresh();
    }

}
