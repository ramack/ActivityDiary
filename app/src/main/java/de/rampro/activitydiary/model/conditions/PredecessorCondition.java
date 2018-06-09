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

import de.rampro.activitydiary.db.ActivityDiaryContract;
import de.rampro.activitydiary.helpers.ActivityHelper;
import de.rampro.activitydiary.model.DiaryActivity;
import de.rampro.activitydiary.ui.settings.SettingsActivity;

/**
 * Model the likelihood of the activities based on its predecessors in the diary
 */

public class PredecessorCondition extends Condition implements ActivityHelper.DataChangedListener {
    public PredecessorCondition(ActivityHelper helper){
        helper.registerDataChangeListener(this);
    }

    @Override
    protected void doEvaluation() {
        double weight = Double.parseDouble(sharedPreferences.getString(SettingsActivity.KEY_PREF_COND_PREDECESSOR, "20"));
        DiaryActivity current = ActivityHelper.helper.getCurrentActivity();
        ArrayList<Likelihood> result = new ArrayList<>(ActivityHelper.helper.getUnsortedActivities().size());

        if(weight > 0.0000001 && current != null) {

            SQLiteQueryBuilder qBuilder = new SQLiteQueryBuilder();
            SQLiteDatabase db = mOpenHelper.getReadableDatabase();

            qBuilder.setTables(ActivityDiaryContract.Diary.TABLE_NAME + " A, " + ActivityDiaryContract.Diary.TABLE_NAME + " B, " +
                    ActivityDiaryContract.DiaryActivity.TABLE_NAME + " C, " + ActivityDiaryContract.DiaryActivity.TABLE_NAME + " D");
            Cursor c = qBuilder.query(db,
                    new String[]{"A." + ActivityDiaryContract.Diary.ACT_ID, "COUNT(A." + ActivityDiaryContract.Diary.ACT_ID + ")"},
                    " B." + ActivityDiaryContract.Diary.ACT_ID + " = ? AND (A." +
                            ActivityDiaryContract.Diary.START + " >= B." + ActivityDiaryContract.Diary.END + " - 500) AND (A." +
                            ActivityDiaryContract.Diary.START + " < B." + ActivityDiaryContract.Diary.END + " + 50)" +
                            "AND A." + ActivityDiaryContract.Diary._DELETED + " = 0 AND B." + ActivityDiaryContract.Diary._DELETED + " = 0 " +
                            "AND A." + ActivityDiaryContract.Diary.ACT_ID + " = C." + ActivityDiaryContract.DiaryActivity._ID + " AND C. " + ActivityDiaryContract.DiaryActivity._DELETED + " = 0 " +
                            "AND B." + ActivityDiaryContract.Diary.ACT_ID + " = D." + ActivityDiaryContract.DiaryActivity._ID + " AND D. " + ActivityDiaryContract.DiaryActivity._DELETED + " = 0"
                    ,
                    new String[]{Long.toString(current.getId())},
                    "A." + ActivityDiaryContract.Diary.ACT_ID,
                    null,
                    null);
            c.moveToFirst();
            long total = 0;
            while (!c.isAfterLast()) {
                DiaryActivity a = ActivityHelper.helper.activityWithId(c.getInt(0));
                total = total + c.getInt(1);
                result.add(new Likelihood(a, c.getInt(1)));
                c.moveToNext();
            }

            for (Likelihood l : result) {
                l.likelihood = l.likelihood / total * weight;
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

    }

    /**
     * Called on removale of an activity.
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
