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


import android.content.res.Resources;

import java.util.Date;

import de.rampro.activitydiary.ActivityDiaryApplication;
import de.rampro.activitydiary.R;

public class FuzzyTimeSpanFormatter {

    public static String format(Date start, Date end) {
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
            return (((delta + 8) / 15) * 15) + " " + res.getString(R.string.seconds_short);
        }else if (delta <= 90 * 60) {
            return (delta + 30) / 60 + " " + res.getString(R.string.minutes_short);
        }else if (delta <= 90 * 60 * 60) {
            return (delta + 30 * 60) / 3600 + " " + res.getString(R.string.hours_short);
        }else {
            return (delta + 12 * 60 * 60) / 3600 / 24 + " " + res.getString(R.string.days);
        }
    }
}
