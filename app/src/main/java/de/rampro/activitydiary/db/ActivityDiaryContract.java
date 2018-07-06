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

import de.rampro.activitydiary.BuildConfig;

/* TODO: @SuppressWarnings("unused") */
public class ActivityDiaryContract {

    /* no instance of this class is allowed */
    private ActivityDiaryContract(){}

    public static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".provider";
    public static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY);

    /* DiaryActivities are the main action that can be logged in the Diary */
    public final static class DiaryActivity implements DiaryActivityColumns{
        /**
         * This utility class cannot be instantiated
         */
        private DiaryActivity() {}

        public static final Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, "activities");

        /**
         * The name of the database table, to be used for joining queries as column prefix.
         */
        public static final String TABLE_NAME = "activity";

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

    /* The columns in a DiaryActivity which are joinable */
    protected interface DiaryActivityJoinableColumns {
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
         * The id of the parent Activity
         * <P>Type: INT</P>
         */
        public static final String PARENT = "parent";

        /* TODO #25: add image, required and activation conditions */
    }

    /* The columns in a DiaryActivity */
    protected interface DiaryActivityColumns extends DiaryActivityJoinableColumns{
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
    }

    /* Diary stores the history of the activities. */
    public final static class Diary implements DiaryColumns, DiaryActivityJoinableColumns{
        /**
         * This utility class cannot be instantiated
         */
        private Diary() {}

        public static final Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, "diary");

        /**
         * The name of the database table, to be used for joining queries as column prefix.
         */
        public static final String TABLE_NAME = "diary";

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
         * A projection of all columns in the diary table, including some joined colums from DiaryActivity.
         */
        public static final String[] PROJECTION_ALL =
                {TABLE_NAME + "." + _ID, TABLE_NAME + "." + _DELETED, ACT_ID, NAME, COLOR, START, END, NOTE};

        /**
         * The default sort order for the diary is time...
         */
        public static final String SORT_ORDER_DEFAULT =
                START + " DESC, " + END + " DESC";
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
        /**
         * The note attached to the diary entry. Could be NULL;
         * <P>Type: TEXT</P>
         */
        public static final String NOTE = "note";
    }

    /* DiaryImage attachments for diary entries*/
    public final static class DiaryImage {
        /**
         * This utility class cannot be instantiated
         */
        private DiaryImage() {}

        public static final Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, "diaryImage");

        /**
         * The name of the database table, to be used for joining queries as column prefix.
         */
        public static final String TABLE_NAME = "diary_image";

        /**
         * The mime type of a directory of this entry.
         */
        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE +
                        "/vnd.de.rampro.activitydiary_diary_images";
        /**
         * The mime type of a single entry.
         */
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE +
                        "/vnd.de.rampro.activitydiary_diary_image";

        /**
         * The id (primary key) for the image
         * <P>Type: INTEGER</P>
         */
        public static final String _ID = "_id";
        /**
         * Deleted state (0 is alive, 1 is deleted)
         * <P>Type: INTEGER</P>
         */
        public static final String _DELETED = "_deleted";
        /**
         * The ID for the related Diary entry
         * <P>Type: TEXT</P>
         */
        public static final String DIARY_ID = "diary_id";
        /**
         * The uri of the image.
         * <P>Type: STRING</P>
         */
        public static final String URI = "uri";

        /**
         * A projection of all columns in the diary table, including some joined colums from DiaryActivity.
         */
        public static final String[] PROJECTION_ALL =
                {_ID, _DELETED, DIARY_ID, URI};

        /**
         * The default sort order for the diary images.
         */
        public static final String SORT_ORDER_DEFAULT = "";

    }

    /* DiaryLocations are the location history logged in the Diary */
    public final static class DiaryLocation implements DiaryLocationColumns{
        /**
         * This utility class cannot be instantiated
         */
        private DiaryLocation() {}

        public static final Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, "locations");

        /**
         * The name of the database table, to be used for joining queries as column prefix.
         */
        public static final String TABLE_NAME = "location";

        /**
         * The mime type of a directory of this entry.
         */
        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE +
                        "/vnd.de.rampro.activitydiary_locations";
        /**
         * The mime type of a single entry.
         */
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE +
                        "/vnd.de.rampro.activitydiary_location";
        /**
         * A projection of all columns
         * in the items table.
         */
        public static final String[] PROJECTION_ALL =
                {_ID, LATITUDE, LONGITUDE, ALTITUDE, TIMESTAMP, SPEED, HACC, VACC, SACC};

        /**
         * The default sort order for
         * queries containing NAME fields.
         */
        public static final String SORT_ORDER_DEFAULT =
                _ID + " DESC";
    }

    /* The columns in a DiaryActivity which are joinable */
    protected interface DiaryLocationJoinableColumns {
        /**
         * The latitude of the position
         * <P>Type: REAL</P>
         */
        public static final String LATITUDE = "latitude";
        /**
         * The longitude of the position
         * <P>Type: REAL</P>
         */
        public static final String LONGITUDE = "longitude";
        /**
         * The altitude of the location information, in m above WSG84 reference ellipsoid
         * <P>Type: REAL</P>
         */
        public static final String ALTITUDE = "altitude";
        /**
         * The timestamp of the location information, in milli seconds since epoch.
         * <P>Type: INTEGER</P>
         */
        public static final String TIMESTAMP = "ts";
        /**
         * The speed associated the the location information in meters/second over ground, NULL means unknown
         * <P>Type: REAL</P>
         */
        public static final String SPEED = "speed";
        /**
         * The estimated horizontal accuracy of the location information, in 1/10 meters, NULL means unknown
         * <P>Type: INTEGER</P>
         */
        public static final String HACC = "hacc";
        /**
         * The estimated vertical accuracy of the location information, in 1/10 meters, NULL means unknown
         * <P>Type: INTEGER</P>
         */
        public static final String VACC = "vacc";
        /**
         * The estimated speed accuracy of the location information, in 1/10 meters/sec, NULL means unknown
         * <P>Type: INTEGER</P>
         */
        public static final String SACC = "sacc";

    }

    /* The columns in a location */
    protected interface DiaryLocationColumns extends DiaryLocationJoinableColumns{
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
    }

}
