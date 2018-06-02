/*
 * ActivityDiary
 *
 * Copyright (C) 2018-2018 Raphael Mack http://www.raphael-mack.de
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

package de.rampro.activitydiary.search;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;

import de.rampro.activitydiary.BuildConfig;
import de.rampro.activitydiary.R;
import de.rampro.activitydiary.helpers.ActivityHelper;
import de.rampro.activitydiary.model.DiaryActivity;

import static android.app.SearchManager.SUGGEST_COLUMN_ICON_1;
import static android.app.SearchManager.SUGGEST_COLUMN_INTENT_ACTION;
import static android.app.SearchManager.SUGGEST_COLUMN_INTENT_DATA;
import static android.app.SearchManager.SUGGEST_COLUMN_TEXT_1;
import static android.app.SearchManager.SUGGEST_COLUMN_TEXT_2;

public class ActivityDiarySuggestionProvider extends ContentProvider {
    private static final int search_history = 1;

    public static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".search.ActivityDiarySuggestionProvider";
    public static final Uri SEARCH_URI = Uri.parse("content://" + AUTHORITY);
    public static final String SEARCH_ACTIVITY = "de.rampro.activitydiary.action.SEARCH_ACTIVITY";
    public static final String SEARCH_NOTE = "de.rampro.activitydiary.action.SEARCH_NOTE";
    public static final String SEARCH_GLOBAL = "de.rampro.activitydiary.action.SEARCH_GLOBAL";

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sUriMatcher.addURI(AUTHORITY, "history/" + SearchManager.SUGGEST_URI_PATH_QUERY + "/*", search_history);
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        int id = 0;
        MatrixCursor result = null;
        switch(sUriMatcher.match(uri)){
            case search_history :
                String query = uri.getLastPathSegment().toLowerCase();
                result = new MatrixCursor(new String[]{
                        BaseColumns._ID,
                        SUGGEST_COLUMN_TEXT_1,
                        SUGGEST_COLUMN_ICON_1,
                        SUGGEST_COLUMN_INTENT_ACTION,
                        SUGGEST_COLUMN_INTENT_DATA
                });

                if(query != null && query.length() > 0) {
                    // activities matching the current search
                    ArrayList<DiaryActivity> filtered = ActivityHelper.helper.sortedActivities(query);

                    // TODO: make the amount of activities shown configurable
                    for (int i = 0; i < 3; i++) {
                        result.addRow(new Object[]{id++,
                                filtered.get(i).getName(),
                                /* icon */ null,
                                /* intent action */ SEARCH_ACTIVITY,
                                /* intent data */ Uri.withAppendedPath(SEARCH_URI, Integer.toString(filtered.get(i).getId()))
                        });
                    }

                    // Notes
                    result.addRow(new Object[]{id++,
                            getContext().getResources().getString(R.string.search_notes, query),
                            /* icon */ R.drawable.ic_search,
                            /* intent action */ SEARCH_NOTE,
                            /* intent data */ Uri.withAppendedPath(SEARCH_URI, query)

                    });

                    // Global search
                    result.addRow(new Object[]{id++,
                            getContext().getResources().getString(R.string.search_diary, query),
                            /* icon */ R.drawable.ic_search,
                            /* intent action */ SEARCH_GLOBAL,
                            /* intent data */ Uri.withAppendedPath(SEARCH_URI, query)

                    });

                }
                // has Pictures
                // TODO: add picture search

                // Location (GPS)
                // TODO: add location search

                // Date
                // TODO: add date search
            default:
                /* empty */
        }

        return result;
    }

    /**
     * Implement this to handle requests for the MIME type of the data at the
     * given URI.  The returned MIME type should start with
     * <code>vnd.android.cursor.item</code> for a single record,
     * or <code>vnd.android.cursor.dir/</code> for multiple items.
     * This method can be called from multiple threads, as described in
     * <a href="{@docRoot}guide/topics/fundamentals/processes-and-threads.html#Threads">Processes
     * and Threads</a>.
     * <p>
     * <p>Note that there are no permissions needed for an application to
     * access this information; if your content provider requires read and/or
     * write permissions, or is not exported, all applications can still call
     * this method regardless of their access permissions.  This allows them
     * to retrieve the MIME type for a URI when dispatching intents.
     *
     * @param uri the URI to query.
     * @return a MIME type string, or {@code null} if there is no type.
     */
    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }
}
