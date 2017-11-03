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

package de.rampro.activitydiary.db;

import android.content.ContentResolver;
import android.net.Uri;

/* TODO: @SuppressWarnings("unused") */
public class ActivityDiaryContract {

    /* no instance of this class is allowed */
    private ActivityDiaryContract(){}

    public static final String AUTHORITY = "de.rampro.activitydiary";
    public static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY);

    /* DiaryActivities are the main action that can be logged in the Diary */
    public final static class DiaryActivity implements DiaryActivityColumns{
        /**
         * This utility class cannot be instantiated
         */
        private DiaryActivity() {}

        public static final Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, "activities");

        /**
         * The mime type of a directory of this entry.
         */
        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE +
                        "/vnd.de.rampro.activitydiary_activities";
        /**
         * The mime type of a single entry.
         */
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE +
                        "/vnd.de.rampro.activitydiary_activity";
        /**
         * A projection of all columns
         * in the items table.
         */
        public static final String[] PROJECTION_ALL =
                {_ID, NAME, COLOR, PARENT};

        /**
         * The default sort order for
         * queries containing NAME fields.
         */
        public static final String SORT_ORDER_DEFAULT =
                NAME + " ASC";
    }

    /* The columns in a DiaryActivity */
    protected interface DiaryActivityColumns {
        /**
         * The id (primary key) for the Activity
         * <P>Type: INTEGER</P>
         */
        public static final String _ID = "_id";
        /**
         * Deleted state (0 is alive, 1 is deleted)
         * <P>Type: INTEGER</P>
         */
        public static final String _DELETED = "_deleted";
        /**
         * The name for the Activity
         * <P>Type: TEXT</P>
         */
        public static final String NAME = "name";
        /**
         * The color for the Activity
         * <P>Type: TEXT</P>
         */
        public static final String COLOR = "color";
        /**
         * The if of the parent Activity
         * <P>Type: INT</P>
         */
        public static final String PARENT = "parent";

        /* TODO: add iamge, required and activation conditions */
    }

    /* DiaryActivities are the main action that can be logged in the Diary */
    public final static class Diary implements DiaryColumns{
        /**
         * This utility class cannot be instantiated
         */
        private Diary() {}

        public static final Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, "diary");

        /**
         * The mime type of a directory of this entry.
         */
        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE +
                        "/vnd.de.rampro.activitydiary_diary";
        /**
         * The mime type of a single entry.
         */
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE +
                        "/vnd.de.rampro.activitydiary_diaryentry";
        /**
         * A projection of all columns
         * in the items table.
         */
        public static final String[] PROJECTION_ALL =
                {_ID, ACT_ID, START, END};

        /**
         * The default sort order for the diary is time...
         */
        public static final String SORT_ORDER_DEFAULT =
                START + ", " + END;
    }

    /* The columns in the Diary */
    protected interface DiaryColumns {
        /**
         * The id (primary key) for the Diary (entry)
         * <P>Type: INTEGER</P>
         */
        public static final String _ID = "_id";
        /**
         * Deleted state (0 is alive, 1 is deleted)
         * <P>Type: INTEGER</P>
         */
        public static final String _DELETED = "_deleted";
        /**
         * The ID for the related Activity
         * <P>Type: TEXT</P>
         */
        public static final String ACT_ID = "act_id";
        /**
         * The start time of the diary entry in milli seconds since epoch.
         * <P>Type: INT</P>
         */
        public static final String START = "start";
        /**
         * The end time of the diary entry in milli seconds since epoch.
         * <P>Type: INT</P>
         */
        public static final String END = "end";

        /* TODO: add note entry */
        /* TODO: add location */
    }

    /**
     * Combines all columns returned by table queries.
     */
    protected interface DataColumnsWithJoins extends DiaryActivityColumns, DiaryColumns {
    }


}
