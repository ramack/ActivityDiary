/*
 * ActivityDiary
 *
 * Copyright (C) 2017-2017 Raphael Mack http://www.raphael-mack.de
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

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

/*
 * Why a new Content Provider for Diary Activites?
 *
 * According https://developer.android.com/guide/topics/providers/content-provider-creating.html
 * we need it to do searching, synching or widget use of the data -> which in the long we all want to do.
 *
 * */
public class ActivityDiaryContentProvider extends ContentProvider {

    private static final int activities = 1;
    private static final int activities_ID = 2;
    private static final int conditions = 3;
    private static final int conditions_ID = 4;
    private static final int diary = 5;
    private static final int diary_ID = 6;
    private static final String TAG = ActivityDiaryContentProvider.class.getName();

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sUriMatcher.addURI(ActivityDiaryContract.AUTHORITY, ActivityDiaryContract.DiaryActivity.CONTENT_URI.getPath(), activities);
/* TODO */
        sUriMatcher.addURI(ActivityDiaryContract.AUTHORITY, "activities/#", activities_ID);

        sUriMatcher.addURI(ActivityDiaryContract.AUTHORITY, "conditions", conditions);
        sUriMatcher.addURI(ActivityDiaryContract.AUTHORITY, "conditions/#", conditions_ID);

        sUriMatcher.addURI(ActivityDiaryContract.AUTHORITY, "diary", diary);
        sUriMatcher.addURI(ActivityDiaryContract.AUTHORITY, "diary/#", diary_ID);

        /* TODO: add expected next activities?, which could include
         *  - child activities
         *  - recent activities
         *  - likely activites -> even the auto-set activities could be handled via this one...
         *  */

    }

    private LocalDBHelper mOpenHelper;

    @Override
    public boolean onCreate() {
        mOpenHelper = new LocalDBHelper(getContext());
        return true; /* successfully loaded */
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        SQLiteQueryBuilder qBuilder = new SQLiteQueryBuilder();

        if(sUriMatcher.match(uri) < 1)
        {
            /* URI is not recognized, return an empty Cursor */
            return null;
        }
        switch (sUriMatcher.match(uri)) {
            case activities_ID:
            case conditions_ID:
            case diary_ID:
                if(selection != null) {
                    selection = selection + " AND ";
                }else{
                    selection = "";
                }
                selection = selection + "_id = " + uri.getLastPathSegment(); /* TODO: check isn't this prone to SQL injection? */
            default:
                /* empty */
        }

        switch (sUriMatcher.match(uri)) {
            case activities_ID:
                /* intended fall through */
            case activities:
                qBuilder.setTables(LocalDBHelper.ACTIVITY_DB_TABLE);
                if (TextUtils.isEmpty(sortOrder)) sortOrder = ActivityDiaryContract.DiaryActivity.SORT_ORDER_DEFAULT;
                break;
            case conditions_ID:
                /* intended fall through */
            case conditions:
                qBuilder.setTables(LocalDBHelper.CONDITION_DB_TABLE);
/* TODO                if (TextUtils.isEmpty(sortOrder)) sortOrder = ActivityDiaryContract.Conditions.SORT_ORDER_DEFAULT; */
            case diary_ID:
                /* intended fall through */
            case diary:
                qBuilder.setTables(LocalDBHelper.DIARY_DB_TABLE);
/* TODO                if (TextUtils.isEmpty(sortOrder)) sortOrder = ActivityDiaryContract.Diary.SORT_ORDER_DEFAULT; */
                break;
            default:
                /* empty */
        }

        Cursor c = qBuilder.query(mOpenHelper.getReadableDatabase(),
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder);
        c.setNotificationUri(getContext().getContentResolver(), uri);

        return c;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case activities:
                return ActivityDiaryContract.DiaryActivity.CONTENT_TYPE;
            case activities_ID:
                return ActivityDiaryContract.DiaryActivity.CONTENT_ITEM_TYPE;
            default:
                Log.e(TAG, "MIME type for " + uri.toString() + " not defined.");
                return "";
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        String table;
        Uri resultUri;

        switch(sUriMatcher.match(uri)) {
            case activities:
                table = LocalDBHelper.ACTIVITY_DB_TABLE;
                resultUri = ActivityDiaryContract.DiaryActivity.CONTENT_URI;
                break;
            case conditions:
                table = LocalDBHelper.CONDITION_DB_TABLE;
// TODO               resultUri = ActivityDiaryContract.Condition.CONTENT_URI;
//                break;
            case diary:
                table = LocalDBHelper.DIARY_DB_TABLE;
// TODO               resultUri = ActivityDiaryContract.Diary.CONTENT_URI;
//                break;
            default:
                throw new IllegalArgumentException(
                        "Unsupported URI for insertion: " + uri);
        }
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        long id = db.insert(table,
                            null,
                            values);
        if(id > 0) {
            resultUri = ContentUris.withAppendedId(resultUri, id);
            getContext().
                    getContentResolver().
                    notifyChange(resultUri, null);

            return resultUri;
        }else {
            throw new SQLException(
                    "Problem while inserting into uri: " + uri);
        }
    }

    /**
     * Implement this to handle requests to delete one or more rows.
     * The implementation should apply the selection clause when performing
     * deletion, allowing the operation to affect multiple rows in a directory.
     * As a courtesy, call {@link ContentResolver#notifyChange(Uri, ContentObserver) notifyChange()}
     * after deleting.
     * This method can be called from multiple threads, as described in
     * <a href="{@docRoot}guide/topics/fundamentals/processes-and-threads.html#Threads">Processes
     * and Threads</a>.
     * <p>
     * <p>The implementation is responsible for parsing out a row ID at the end
     * of the URI, if a specific row is being deleted. That is, the client would
     * pass in <code>content://contacts/people/22</code> and the implementation is
     * responsible for parsing the record number (22) when creating a SQL statement.
     *
     * @param uri           The full URI to query, including a row ID (if a specific record is requested).
     * @param selection     An optional restriction to apply to rows when deleting.
     * @param selectionArgs
     * @return The number of rows affected.
     * @throws SQLException
     */
    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        /* TODO: implement deletion of activities and all others  */
/*notify        getContext().
                getContentResolver().
                notifyChange(resultUri, null);
*/
        return 0;
    }

    /**
     * Implement this to handle requests to update one or more rows.
     * The implementation should update all rows matching the selection
     * to set the columns according to the provided values map.
     * As a courtesy, call {@link ContentResolver#notifyChange(Uri, ContentObserver) notifyChange()}
     * after updating.
     * This method can be called from multiple threads, as described in
     * <a href="{@docRoot}guide/topics/fundamentals/processes-and-threads.html#Threads">Processes
     * and Threads</a>.
     *
     * @param uri           The URI to query. This can potentially have a record ID if this
     *                      is an update request for a specific record.
     * @param values        A set of column_name/value pairs to update in the database.
     *                      This must not be {@code null}.
     * @param selection     An optional filter to match rows to update.
     * @param selectionArgs
     * @return the number of rows affected.
     */
    @Override
    public int update(@NonNull Uri uri, @NonNull ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        /* TODO: implement update of activities and all others  */
/*notify        getContext().
                getContentResolver().
                notifyChange(resultUri, null);
*/
        return 0;
    }
}
