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

package de.rampro.activitydiary.helpers;

import android.content.res.Resources;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import de.rampro.activitydiary.ActivityDiaryApplication;
import de.rampro.activitydiary.R;

public class DateHelper {

    /* Get the start of the time span from timeRef
     * possible range values are the field constants of Calender, e. g. Calendar.MONTH
     *
     * Current supported granularity down to DAY, so MILLISECOND, SECOND, MINUTE and HOUR_OF_DAY are always set to 0
     *
     * timeRef is in millis since epoch
     * */
    public static Calendar startOf(int field, long timeRef){
        Calendar result = Calendar.getInstance();
        result.setTimeInMillis(timeRef);
        result.set(Calendar.HOUR_OF_DAY, 0); // ! clear would not reset the hour of day !
        result.clear(Calendar.MINUTE);
        result.clear(Calendar.SECOND);
        result.clear(Calendar.MILLISECOND);
        switch(field){
            case Calendar.DAY_OF_MONTH:
            case Calendar.DAY_OF_WEEK:
            case Calendar.DAY_OF_WEEK_IN_MONTH:
            case Calendar.DAY_OF_YEAR:
                /* nothing to do, as HOUR_OF_DAY is already 0 */
                break;
            case Calendar.WEEK_OF_YEAR:
                result.set(Calendar.DAY_OF_WEEK, result.getFirstDayOfWeek());
                break;
            case Calendar.MONTH:
                result.set(Calendar.DAY_OF_MONTH, 1);
                break;
            case Calendar.YEAR:
                result.set(Calendar.DAY_OF_YEAR, 1);
                break;
            default:
                throw new RuntimeException("date field not supported: " + field);
        }
        return result;
    }

    public static SimpleDateFormat dateFormat(int field) {
        Resources res = ActivityDiaryApplication.getAppContext().getResources();
        SimpleDateFormat result;
        switch(field){
            case Calendar.DAY_OF_MONTH:
            case Calendar.DAY_OF_WEEK:
            case Calendar.DAY_OF_WEEK_IN_MONTH:
            case Calendar.DAY_OF_YEAR:
                result = new SimpleDateFormat(res.getString(R.string.day_format));
                break;
            case Calendar.WEEK_OF_YEAR:
                result = new SimpleDateFormat(res.getString(R.string.week_format));
                break;
            case Calendar.MONTH:
                result = new SimpleDateFormat(res.getString(R.string.month_format));
                break;
            case Calendar.YEAR:
                result = new SimpleDateFormat(res.getString(R.string.year_format));
                break;
            default:
                throw new RuntimeException("date field not supported: " + field);
        }
        return result;
    }
}
