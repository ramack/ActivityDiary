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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import de.rampro.activitydiary.helpers.ActivityHelper;
import de.rampro.activitydiary.model.DiaryActivity;
import de.rampro.activitydiary.ui.settings.SettingsActivity;

/**
 * Model the likelihood of the activities based on the alphabetical sorting of their names
 */
public class AlphabeticalCondition extends Condition implements ActivityHelper.DataChangedListener {

    public AlphabeticalCondition(ActivityHelper helper){
        helper.registerDataChangeListener(this);
    }

    protected void doEvaluation(){
        double weight = Double.parseDouble(sharedPreferences.getString(SettingsActivity.KEY_PREF_COND_ALPHA, "5"));
        ArrayList<Likelihood> result = new ArrayList<>(ActivityHelper.helper.getUnsortedActivities().size());
        if(weight > 0.001) {

            ArrayList<DiaryActivity> sort = new ArrayList<>(ActivityHelper.helper.getUnsortedActivities());
            Collections.sort(sort, new Comparator<DiaryActivity>() {
                @Override
                public int compare(DiaryActivity o1, DiaryActivity o2) {
                    if(o1 == o2){
                        return 0;
                    }else if(o1 == null){
                        return -1;
                    }else if(o2 == null){
                        return 1;
                    }else{
                        return o2.getName().compareTo(o1.getName());
                    }
                }
            });
            double step = weight / sort.size();
            int no = 0;
            for (DiaryActivity a : sort) {
                result.add(new Likelihood(a, step * no));
                no++;
            }
        }
        this.setResult(result);
    }

    /**
     * Called when the data has changed and no further specification is possible.
     * => everything needs to be refreshed!
     *
     */
    @Override
    public void onActivityDataChanged() {
        refresh();
    }

    /**
     * Called when the data of one activity was changed.
     *
     * @param activity
     */
    @Override
    public void onActivityDataChanged(DiaryActivity activity) {
        refresh();
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
        // no influence on likelhood for this Condition
    }
}
