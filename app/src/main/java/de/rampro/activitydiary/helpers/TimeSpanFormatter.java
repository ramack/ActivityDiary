/*
 * ActivityDiary
 *
 * Copyright (C) 2017-2018 Raphael Mack http://www.raphael-mack.de
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


import android.content.res.Resources;
import androidx.preference.PreferenceManager;

import java.text.DecimalFormat;
import java.util.Date;

import de.rampro.activitydiary.ActivityDiaryApplication;
import de.rampro.activitydiary.R;
import de.rampro.activitydiary.ui.settings.SettingsActivity;

public class TimeSpanFormatter {

    public static String fuzzyFormat(Date start, Date end) {
        Resources res = ActivityDiaryApplication.getAppContext().getResources();
        if(end == null){
            end = new Date();
        }
        long delta = (end.getTime() - start.getTime() + 500) / 1000;

        if (delta <= 3){
            return res.getString(R.string.just_now);
        }else if (delta <= 35){
            return res.getString(R.string.few_seconds);
        }else if (delta <= 90) {
            int val = (int)((delta + 8) / 15) * 15;
            return res.getQuantityString(R.plurals.seconds_short, val, val);
        }else if (delta <= 90 * 60) {
            int val = (int)((delta + 30) / 60);
            return res.getQuantityString(R.plurals.minutes_short, val, val);
        }else if (delta <= 90 * 60 * 60) {
            int val = (int)((delta + 30 * 60) / 3600);
            return res.getQuantityString(R.plurals.hours_short, val, val);
        }else {
            int val = (int)((delta + 12 * 60 * 60) / 3600 / 24);
            return res.getQuantityString(R.plurals.days, val, val);
        }
    }

    /**
     * duration in millis
     */
    public static String format(long duration) {
        Resources res = ActivityDiaryApplication.getAppContext().getResources();
        long delta = duration / 1000;
        String displayFormat = PreferenceManager
                .getDefaultSharedPreferences(ActivityDiaryApplication.getAppContext())
                .getString(SettingsActivity.KEY_PREF_DURATION_FORMAT, "dynamic");
        String result = "";

        int sec = (int)(delta % 60);
        int min = (int)(((delta - sec) / 60) % 60);
        int hours = (int)(((delta - 60 * min - sec) / 60 / 60) % 24);
        int days = (int)((delta - 3600 * hours - 60 * min - sec) / 60 / 60 / 24);

        DecimalFormat df = new DecimalFormat("00");

        switch (displayFormat) {
            case "hour_min":
                if (days > 0){
                    hours += days * 24;
                }
                min += (sec + 30)/60;

                result = hours + "h " + df.format(min) + "'";
                break;
            case "nodays":
                if (days > 0){
                    hours += days * 24;
                    days = 0;
                }
                // fall through, as no days is "precise" without removeing full days from the hours
            case "precise":
                if(days > 0){
                    result = days + "d ";
                }
                if(hours > 0 || days > 0){
                    result += hours + "h "
                            + df.format(min) + "' ";
                }
                if(min > 0 && hours == 0 && days == 0){
                    result += min + "' ";
                }
                if(min > 0 || hours > 0 || days > 0){
                    result += df.format(sec) + "''";
                }else{
                    result = res.getQuantityString(R.plurals.seconds_short, sec, sec);
                }

                break;
            case "dynamic": // fall-through
            default:
                if (days >= 9) {
                    result = (days + ((hours + 12) / 24)) + "d";
                } else if (days >= 1){
                    result =  days + "d "
                            + (hours + ((min + 30) / 60)) + "h";
                } else if (hours >= 1){
                    result =  hours + "h "
                            + df.format((min + ((sec + 30) / 60))) + "'";
                } else if (min >= 1){
                    result =  min + "' "
                            + df.format(sec) + "''";
                } else{
                    result = res.getQuantityString(R.plurals.seconds_short, sec, sec);
                }
                break;
        }

        return result;
    }

}
