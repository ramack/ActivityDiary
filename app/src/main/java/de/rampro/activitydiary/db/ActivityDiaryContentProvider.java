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
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

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
    private static final int diary_image = 7;
    private static final int diary_image_ID = 8;
    private static final int diary_location = 9;
    private static final int diary_location_ID = 10;
    private static final int diary_stats = 11;
    private static final String TAG = ActivityDiaryContentProvider.class.getName();

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sUriMatcher.addURI(ActivityDiaryContract.AUTHORITY, ActivityDiaryContract.DiaryActivity.CONTENT_URI.getPath().replaceAll("^/+", ""), activities);
        sUriMatcher.addURI(ActivityDiaryContract.AUTHORITY, ActivityDiaryContract.DiaryActivity.CONTENT_URI.getPath().replaceAll("^/+", "") + "/#", activities_ID);

        sUriMatcher.addURI(ActivityDiaryContract.AUTHORITY, ActivityDiaryContract.Diary.CONTENT_URI.getPath().replaceAll("^/+", ""), diary);
        sUriMatcher.addURI(ActivityDiaryContract.AUTHORITY, ActivityDiaryContract.Diary.CONTENT_URI.getPath().replaceAll("^/+", "") + "/#", diary_ID);

        sUriMatcher.addURI(ActivityDiaryContract.AUTHORITY, ActivityDiaryContract.DiaryImage.CONTENT_URI.getPath().replaceAll("^/+", ""), diary_image);
        sUriMatcher.addURI(ActivityDiaryContract.AUTHORITY, ActivityDiaryContract.DiaryImage.CONTENT_URI.getPath().replaceAll("^/+", "") + "/#", diary_image_ID);

        sUriMatcher.addURI(ActivityDiaryContract.AUTHORITY, ActivityDiaryContract.DiaryStats.CONTENT_URI.getPath().replaceAll("^/+", ""), diary_stats);

        sUriMatcher.addURI(ActivityDiaryContract.AUTHORITY, ActivityDiaryContract.DiaryLocation.CONTENT_URI.getPath().replaceAll("^/+", ""), diary_location);
        sUriMatcher.addURI(ActivityDiaryContract.AUTHORITY, ActivityDiaryContract.DiaryLocation.CONTENT_URI.getPath().replaceAll("^/+", "") + "/#", diary_location_ID);

        sUriMatcher.addURI(ActivityDiaryContract.AUTHORITY, ActivityDiaryContract.DiaryLocation.CONTENT_URI.getPath().replaceAll("^/+", ""), diary_location);
        sUriMatcher.addURI(ActivityDiaryContract.AUTHORITY, ActivityDiaryContract.DiaryLocation.CONTENT_URI.getPath().replaceAll("^/+", "") + "/#", diary_location_ID);

        /* TODO #18 */
        sUriMatcher.addURI(ActivityDiaryContract.AUTHORITY, "conditions", conditions);
        sUriMatcher.addURI(ActivityDiaryContract.AUTHORITY, "conditions/#", conditions_ID);

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
        boolean useRawQuery = false;
        String sql = "";
        Cursor c;

        if(sUriMatcher.match(uri) < 1)
        {
            /* URI is not recognized, return an empty Cursor */
            return null;
        }
        switch (sUriMatcher.match(uri)) {
            case activities_ID:
            case conditions_ID:
            case diary_ID:
            case diary_image_ID:
            case diary_location_ID:
                if(selection != null) {
                    selection = selection + " AND ";
                }else{
                    selection = "";
                }
                selection = selection + "_id=" + uri.getLastPathSegment();
            default:
                /* empty */
        }

        switch (sUriMatcher.match(uri)) {
            case activities_ID: /* intended fall through */
            case activities:
                qBuilder.setTables(ActivityDiaryContract.DiaryActivity.TABLE_NAME);
                if (TextUtils.isEmpty(sortOrder)) sortOrder = ActivityDiaryContract.DiaryActivity.SORT_ORDER_DEFAULT;
                break;
            case diary_image_ID: /* intended fall through */
            case diary_image:
                qBuilder.setTables(ActivityDiaryContract.DiaryImage.TABLE_NAME);
                if (TextUtils.isEmpty(sortOrder)) sortOrder = ActivityDiaryContract.DiaryImage.SORT_ORDER_DEFAULT;
                break;
            case diary_location_ID: /* intended fall through */
            case diary_location:
                qBuilder.setTables(ActivityDiaryContract.DiaryLocation.TABLE_NAME);
                if (TextUtils.isEmpty(sortOrder)) sortOrder = ActivityDiaryContract.DiaryLocation.SORT_ORDER_DEFAULT;
                break;
            case diary_ID: /* intended fall through */
            case diary:
                /* rewrite projection, to prefix with tables */
                qBuilder.setTables(ActivityDiaryContract.Diary.TABLE_NAME + " INNER JOIN " +
                        ActivityDiaryContract.DiaryActivity.TABLE_NAME + " ON " +
                        ActivityDiaryContract.Diary.TABLE_NAME + "." + ActivityDiaryContract.Diary.ACT_ID + " = " +
                        ActivityDiaryContract.DiaryActivity.TABLE_NAME + "." + ActivityDiaryContract.DiaryActivity._ID
                );
                if (TextUtils.isEmpty(sortOrder)) sortOrder = ActivityDiaryContract.Diary.SORT_ORDER_DEFAULT;
                break;
            case diary_stats:
                useRawQuery = true;

                String subselect = "SELECT SUM(IFNULL(" + ActivityDiaryContract.Diary.END + ",strftime('%s','now') * 1000) - " + ActivityDiaryContract.Diary.START + ") from " + ActivityDiaryContract.Diary.TABLE_NAME;
                if(selectionArgs != null) {
                    /* we duplicate the where condition, so we have to do the same with the arguments */
                    String[] selArgs = new String[2 * selectionArgs.length];
                    System.arraycopy(selectionArgs, 0, selArgs, 0, selectionArgs.length);
                    System.arraycopy(selectionArgs, 0, selArgs, selectionArgs.length, selectionArgs.length);
                    selectionArgs = selArgs;
                }
                if(selection != null && selection.length() > 0) {
                    subselect += " WHERE " + selection;
                }

                sql = "SELECT " + ActivityDiaryContract.DiaryActivity.TABLE_NAME + "." + ActivityDiaryContract.DiaryActivity.NAME + " as " + ActivityDiaryContract.DiaryStats.NAME
                        + ", " + ActivityDiaryContract.DiaryActivity.TABLE_NAME + "." + ActivityDiaryContract.DiaryActivity.COLOR + " as " + ActivityDiaryContract.DiaryStats.COLOR
                        + ", SUM(IFNULL(" + ActivityDiaryContract.Diary.END + ",strftime('%s','now') * 1000) - " + ActivityDiaryContract.Diary.START + ") as " + ActivityDiaryContract.DiaryStats.DURATION
                        + ", (SUM(IFNULL(" + ActivityDiaryContract.Diary.END + ",strftime('%s','now') * 1000) - " + ActivityDiaryContract.Diary.START + ") * 100.0 / (" + subselect + ")) as " + ActivityDiaryContract.DiaryStats.PORTION
                        + " FROM " + ActivityDiaryContract.Diary.TABLE_NAME + ", " + ActivityDiaryContract.DiaryActivity.TABLE_NAME
                        + " WHERE " + ActivityDiaryContract.Diary.TABLE_NAME + "." + ActivityDiaryContract.Diary.ACT_ID + " = " + ActivityDiaryContract.DiaryActivity.TABLE_NAME + "." + ActivityDiaryContract.DiaryActivity._ID
                ;
                if(selection != null && selection.length() > 0) {
                    sql += " AND (" + selection + ")";
                }
                sql += " GROUP BY " + ActivityDiaryContract.DiaryActivity.TABLE_NAME + "." + ActivityDiaryContract.DiaryActivity._ID;
                if(sortOrder != null && sortOrder.length() > 0) {
                    sql += " ORDER by " + sortOrder;
                }
                break;
            case conditions_ID:
                /* intended fall through */
            case conditions:
//                qBuilder.setTables(ActivityDiaryContract.Condition.TABLE_NAME);
                /* TODO #18               if (TextUtils.isEmpty(sortOrder)) sortOrder = ActivityDiaryContract.Conditions.SORT_ORDER_DEFAULT; */
            default:
                /* empty */
        }

        if(useRawQuery){
            c = mOpenHelper.getReadableDatabase().rawQuery(sql, selectionArgs);
        }else {
            c = qBuilder.query(mOpenHelper.getReadableDatabase(),
                    projection,
                    selection,
                    selectionArgs,
                    null,
                    null,
                    sortOrder);
        }
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
            case diary:
                return ActivityDiaryContract.Diary.CONTENT_TYPE;
            case diary_ID:
                return ActivityDiaryContract.Diary.CONTENT_ITEM_TYPE;
            case diary_location:
                return ActivityDiaryContract.DiaryLocation.CONTENT_TYPE;
            case diary_location_ID:
                return ActivityDiaryContract.DiaryLocation.CONTENT_ITEM_TYPE;
            case diary_stats:
                return ActivityDiaryContract.DiaryStats.CONTENT_TYPE;
            // TODO #18: add other types
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
                table = ActivityDiaryContract.DiaryActivity.TABLE_NAME;
                resultUri = ActivityDiaryContract.DiaryActivity.CONTENT_URI;
                break;
            case diary:
                table = ActivityDiaryContract.Diary.TABLE_NAME;
                resultUri = ActivityDiaryContract.Diary.CONTENT_URI;
                break;
            case diary_image:
                table = ActivityDiaryContract.DiaryImage.TABLE_NAME;
                resultUri = ActivityDiaryContract.DiaryImage.CONTENT_URI;
                break;
            case diary_location:
                table = ActivityDiaryContract.DiaryLocation.TABLE_NAME;
                resultUri = ActivityDiaryContract.DiaryLocation.CONTENT_URI;
                break;
            case conditions:
//                table = ActivityDiaryContract.Condition.TABLE_NAME;
// TODO #18               resultUri = ActivityDiaryContract.Condition.CONTENT_URI;
//                break;
            case diary_stats: /* intended fall-through */
            default:
                throw new IllegalArgumentException(
                        "Unsupported URI for insertion: " + uri);
        }
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        long id = db.insertOrThrow(table,
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
                    "Problem while inserting into uri: " + uri + " values " + values.toString());
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
        boolean isGlobalDelete = false;
        String table;
        ContentValues values = new ContentValues();
        switch(sUriMatcher.match(uri)) {
            case activities_ID:
                table = ActivityDiaryContract.DiaryActivity.TABLE_NAME;
                break;
            case diary:
                isGlobalDelete = true;
                /* fall though */
            case diary_ID:
                table = ActivityDiaryContract.Diary.TABLE_NAME;
                break;
            case diary_image:
                isGlobalDelete = true;
                /* fall though */
            case diary_image_ID:
                table = ActivityDiaryContract.DiaryImage.TABLE_NAME;
                break;
            case diary_location:
                isGlobalDelete = true;
                /* fall though */
            case diary_location_ID:
                table = ActivityDiaryContract.DiaryLocation.TABLE_NAME;
                break;
            case conditions_ID:
//                table = ActivityDiaryContract.Condition.TABLE_NAME;
//                break;
            case diary_stats: /* intended fall-through */
            default:
                throw new IllegalArgumentException(
                        "Unsupported URI for deletion: " + uri);
        }
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        if(!isGlobalDelete) {
            if(selection != null) {
                selection = selection + " AND ";
            }else{
                selection = "";
            }
            selection = selection + "_id=" + uri.getLastPathSegment();
        }
        values.put(ActivityDiaryContract.DiaryActivity._DELETED, "1");

        int upds = db.update(table,
                values,
                selection,
                selectionArgs);
        if(upds > 0) {
            getContext().
                    getContentResolver().
                    notifyChange(uri, null);

        }else {
            Log.i(TAG,"Could not delete anything for uri: " + uri + " with selection '" + selection + "'");
        }
        return upds;
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
        String table;
        boolean isID = false;
        switch(sUriMatcher.match(uri)) {
            case activities_ID:
                isID = true;
                table = ActivityDiaryContract.DiaryActivity.TABLE_NAME;
                break;
            case diary_ID:
                isID = true;
            case diary:
                table = ActivityDiaryContract.Diary.TABLE_NAME;
                break;
            case diary_image:
                table = ActivityDiaryContract.DiaryImage.TABLE_NAME;
                break;
            case diary_location_ID:
                isID = true;
            case diary_location:
                table = ActivityDiaryContract.DiaryLocation.TABLE_NAME;
                break;
            case conditions_ID:
                isID = true;
//                table = ActivityDiaryContract.Condition.TABLE_NAME;
//                break;
            case diary_stats: /* intended fall-through */
            default:
                throw new IllegalArgumentException(
                        "Unsupported URI for update: " + uri);
        }
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        if(isID) {
            if (selection != null) {
                selection = selection + " AND ";
            } else {
                selection = "";
            }
            selection = selection + "_id=" + uri.getLastPathSegment();
        }

        int upds = db.update(table,
                values,
                selection,
                selectionArgs);
        if(upds > 0) {
            getContext().
                    getContentResolver().
                    notifyChange(uri, null);

        }else if(isID) {
            throw new SQLException(
                    "Problem while updating uri: " + uri + " with selection '" + selection + "'");
        }
        return upds;
    }

    public void resetDatabase() {
        mOpenHelper.close();
    }


    /**
     Search for all dates in database which match start/end date or are in range (between start and end date)
     * @param date - date is searched
     * @param dateFormat - format under we search and compare matches
     * @return query (string) with ids that fulfills defined conditions
     */
    public String searchDate(String date, String dateFormat) {
        String querySelection = " ";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat);
        Calendar searchedValueCal = Calendar.getInstance();
        Calendar startValueCal = Calendar.getInstance();
        Calendar endValueCal = Calendar.getInstance();

        //Gets all start, end dates and also id of activities
        try {
            Cursor allRowsStart =  mOpenHelper.getReadableDatabase().rawQuery(
                    "SELECT " + ActivityDiaryContract.Diary._ID + ", "
                            + ActivityDiaryContract.Diary.START +", "
                            + ActivityDiaryContract.Diary.END
                            + " FROM " + ActivityDiaryContract.Diary.TABLE_NAME, null
            );

            if (allRowsStart.moveToFirst()) {
                String[] columnNames = allRowsStart.getColumnNames();
                do {
                    String id = null, start = null, end = null;
                    for (String name : columnNames) {
                        if (name.equals(ActivityDiaryContract.Diary.START))
                            start = simpleDateFormat.format(allRowsStart.getLong(allRowsStart.getColumnIndex(name)));
                        if (name.equals(ActivityDiaryContract.Diary.END))
                            end = allRowsStart.getLong(allRowsStart.getColumnIndex(name)) == 0 ? "0" : simpleDateFormat.format(allRowsStart.getLong(allRowsStart.getColumnIndex(name)));
                        if (name.equals(ActivityDiaryContract.Diary._ID))
                            id = (allRowsStart.getString(allRowsStart.getColumnIndex(name)));
                    }
                    //System.out.println("id: " + id + ", start: " + start + ", end: " + end);

                    //Values (date, start, end) are set to specific calendar - needed for 'between' searching
                    // (ignore values that are equals to '0' -> if end date have '0' value it means that it has not set end date
                    // what means that this activity is still in progress)
                    try {
                        if (date != null)
                            searchedValueCal.setTime(simpleDateFormat.parse(date));
                        if (!start.equals("0") && start != null)
                            startValueCal.setTime(simpleDateFormat.parse(start));
                        if (end != null && !end.equals("0"))
                            endValueCal.setTime(simpleDateFormat.parse(end));
                    }catch (ParseException e){
                        e.printStackTrace();
                    }

                    //here it is creating query from IDs of those activities that are matching with searched date
                    if ((searchedValueCal.after(startValueCal) && searchedValueCal.before(endValueCal) || searchedValueCal.equals(startValueCal) || searchedValueCal.equals(endValueCal))) {
                        querySelection += querySelection.equals(" ") ?  ActivityDiaryContract.Diary.TABLE_NAME + "." +  ActivityDiaryContract.Diary._ID + "=" + id : " OR " + ActivityDiaryContract.Diary.TABLE_NAME + "." +  ActivityDiaryContract.Diary._ID + " =" + id ;
                    } else if (searchedValueCal.after(startValueCal) && end.equals("0") && !searchedValueCal.equals("0")) {
                        querySelection += querySelection.equals(" ") ?  ActivityDiaryContract.Diary.TABLE_NAME + "." +  ActivityDiaryContract.Diary._ID + " =" + id : "OR " + ActivityDiaryContract.Diary.TABLE_NAME + "." +  ActivityDiaryContract.Diary._ID + " =" + id ;
                    }

                } while (allRowsStart.moveToNext());
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
        }

        // if there is no matching dates it returns query which links to find nothings
        // otherwise it will return query with IDs of matching dates
        return querySelection.equals(" ") ?  " start=null" : querySelection;
    }




}
