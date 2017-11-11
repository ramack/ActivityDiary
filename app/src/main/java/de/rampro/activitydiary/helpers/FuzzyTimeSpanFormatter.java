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


import java.util.Date;

public class FuzzyTimeSpanFormatter {

    public static String format(Date start, Date end) {
        if(end == null){
            end = new Date();
        }
        long delta = (end.getTime() - start.getTime() + 500) / 1000;
/* TODO: allow localization for these strings */
        if (delta <= 90) {
            return delta == 1 ? "1 sec" : delta + " sec";
        }
        if (delta <= 90 * 60) {
            return (delta + 30) / 60 + " min";
        }
        if (delta <= 90 * 60 * 60) {
            return (delta + 30 * 60) / 3600 + " h";
        }
        return (delta + 12 * 60 * 60) / 3600 / 24 + " days";
    }
}
