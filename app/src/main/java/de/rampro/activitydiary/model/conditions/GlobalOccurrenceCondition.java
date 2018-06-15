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
import java.util.List;

import de.rampro.activitydiary.db.ActivityDiaryContract;
import de.rampro.activitydiary.helpers.ActivityHelper;
import de.rampro.activitydiary.model.DiaryActivity;
import de.rampro.activitydiary.ui.settings.SettingsActivity;

/**
 * Model the likelihood of the activities based on its predecessors in the diary
 */

public class GlobalOccurrenceCondition extends Condition implements ActivityHelper.DataChangedListener {
    public GlobalOccurrenceCondition(ActivityHelper helper){
        helper.registerDataChangeListener(this);
    }

    @Override
    protected void doEvaluation() {
        double weight = Double.parseDouble(sharedPreferences.getString(SettingsActivity.KEY_PREF_COND_OCCURRENCE, "20"));
        List<DiaryActivity> all = ActivityHelper.helper.getUnsortedActivities();
        ArrayList<Likelihood> result = new ArrayList<>(all.size());

        if(weight > 0.000001) {
            SQLiteQueryBuilder qBuilder = new SQLiteQueryBuilder();
            SQLiteDatabase db = mOpenHelper.getReadableDatabase();

            qBuilder.setTables(ActivityDiaryContract.Diary.TABLE_NAME + " D, " + ActivityDiaryContract.DiaryActivity.TABLE_NAME + " A");
            Cursor c = qBuilder.query(db,
                    new String[]{"D." + ActivityDiaryContract.Diary.ACT_ID, "COUNT(D." + ActivityDiaryContract.Diary.ACT_ID + ")"},
                    "D." + ActivityDiaryContract.Diary._DELETED + " = 0 " +
                            "AND D." + ActivityDiaryContract.Diary.ACT_ID + " = A." + ActivityDiaryContract.DiaryActivity._ID + " AND A. " + ActivityDiaryContract.DiaryActivity._DELETED + " = 0 ",
                    null,
                    "D." + ActivityDiaryContract.Diary.ACT_ID,
                    null,
                    null);
            c.moveToFirst();
            long total = 0;
            long max = 0;
            while (!c.isAfterLast()) {
                DiaryActivity a = ActivityHelper.helper.activityWithId(c.getInt(0));
                total = total + c.getInt(1);
                max = Math.max(max, c.getInt(1));
                result.add(new Likelihood(a, c.getInt(1)));
                c.moveToNext();
            }

            c.close();

            for (Likelihood l : result) {
                l.likelihood = l.likelihood / max * weight;
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
        refresh();
    }

    /**
     * Called on removale of an activity.
     *
     * @param activity
     */
    @Override
    public void onActivityRemoved(DiaryActivity activity) {
        refresh();
    }

    /**
     * Called on change of the current activity.
     */
    @Override
    public void onActivityChanged() {
        refresh();
    }
}
