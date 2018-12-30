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
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowContentResolver;

import java.util.Date;

import de.rampro.activitydiary.BuildConfig;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class ActivityDiaryContentProviderTest {
    ContentResolver contentResolver;

    @Before
    public void setup() {
        ActivityDiaryContentProvider provider = new ActivityDiaryContentProvider();
        provider.onCreate();
//        ShadowContentResolver.registerProviderInternal(
//                ActivityDiaryContract.AUTHORITY, provider
//        );
        contentResolver = RuntimeEnvironment.application.getContentResolver();
    }

    @Test
    public void simpleQuery() throws Exception {
        Cursor cursor = contentResolver.query(
                ActivityDiaryContract.DiaryActivity.CONTENT_URI,
                ActivityDiaryContract.DiaryActivity.PROJECTION_ALL,
                null,
                null,
                ActivityDiaryContract.DiaryActivity.SORT_ORDER_DEFAULT);
        assertNotNull(cursor);

        assertTrue("at least some random data", cursor.getCount() > 0);
    }

    @Test
    public void insertion() throws Exception {
        Cursor cursor = contentResolver.query(
                ActivityDiaryContract.DiaryActivity.CONTENT_URI,
                ActivityDiaryContract.DiaryActivity.PROJECTION_ALL,
                null,
                null,
                ActivityDiaryContract.DiaryActivity.SORT_ORDER_DEFAULT);
        int oldCount = cursor.getCount();
        ContentValues vals = new ContentValues();
        vals.put(ActivityDiaryContract.DiaryActivity.NAME,"TestName");
        vals.put(ActivityDiaryContract.DiaryActivity.COLOR,"BLACK");
        contentResolver.insert(ActivityDiaryContract.DiaryActivity.CONTENT_URI, vals);

        cursor = contentResolver.query(
                ActivityDiaryContract.DiaryActivity.CONTENT_URI,
                ActivityDiaryContract.DiaryActivity.PROJECTION_ALL,
                null,
                null,
                ActivityDiaryContract.DiaryActivity.SORT_ORDER_DEFAULT);
        assertTrue("content added", cursor.getCount() == oldCount + 1);
    }

    @Test
    public void modification_Activity() throws Exception {
        Cursor cursor = contentResolver.query(
                ActivityDiaryContract.DiaryActivity.CONTENT_URI,
                ActivityDiaryContract.DiaryActivity.PROJECTION_ALL,
                null,
                null,
                ActivityDiaryContract.DiaryActivity.SORT_ORDER_DEFAULT);

        cursor.moveToFirst();
        int idRow = cursor.getColumnIndex(ActivityDiaryContract.DiaryActivity._ID);

        long id = cursor.getLong(idRow);
        Uri uri = Uri.withAppendedPath(ActivityDiaryContract.DiaryActivity.CONTENT_URI,
                "/" + Long.toString(id));
        ContentValues vals = new ContentValues();

        vals.put(ActivityDiaryContract.DiaryActivity.NAME,
                cursor.getString(cursor.getColumnIndex(ActivityDiaryContract.DiaryActivity.NAME))
                        + "-TestSuffix");

        contentResolver.update(uri, vals, null, null);

        cursor = contentResolver.query(
                ActivityDiaryContract.DiaryActivity.CONTENT_URI,
                ActivityDiaryContract.DiaryActivity.PROJECTION_ALL,
                ActivityDiaryContract.DiaryActivity._ID + " = ?",
                new String[]{Long.toString(id)},
                ActivityDiaryContract.DiaryActivity.SORT_ORDER_DEFAULT);

        assertTrue("exactly one with that ID", cursor.getCount() == 1);
        cursor.moveToFirst();

        String name = cursor.getString(cursor.getColumnIndex(ActivityDiaryContract.DiaryActivity.NAME));
        assertTrue("name suffix", name.endsWith("-TestSuffix"));

    }

    @Test
    public void modification_Diary() throws Exception {
        ContentValues vals = new ContentValues();

        vals.put(ActivityDiaryContract.Diary.NOTE, "");
        vals.put(ActivityDiaryContract.Diary.ACT_ID, "1");
        vals.put(ActivityDiaryContract.Diary.START, new Date().toString());
        contentResolver.insert(ActivityDiaryContract.Diary.CONTENT_URI, vals);

        Cursor cursor = contentResolver.query(
                ActivityDiaryContract.Diary.CONTENT_URI,
                ActivityDiaryContract.Diary.PROJECTION_ALL,
                null,
                null,
                ActivityDiaryContract.Diary.SORT_ORDER_DEFAULT);

        assertTrue("has entries", cursor.moveToLast());
        int idRow = cursor.getColumnIndex(ActivityDiaryContract.Diary.TABLE_NAME + "." + ActivityDiaryContract.Diary._ID);

        long id = cursor.getLong(idRow);
        Uri uri = Uri.withAppendedPath(ActivityDiaryContract.Diary.CONTENT_URI, Long.toString(id));
        vals = new ContentValues();

        vals.put(ActivityDiaryContract.Diary.NOTE,
                cursor.getString(cursor.getColumnIndex(ActivityDiaryContract.Diary.NOTE))
                        + "-TestSuffix");

        contentResolver.update(uri, vals, null, null);

        cursor = contentResolver.query(
                ActivityDiaryContract.Diary.CONTENT_URI,
                ActivityDiaryContract.Diary.PROJECTION_ALL,
                ActivityDiaryContract.Diary.TABLE_NAME + "." + ActivityDiaryContract.Diary._ID + " = ?",
                new String[]{Long.toString(id)},
                ActivityDiaryContract.Diary.SORT_ORDER_DEFAULT);

        assertTrue("exactly one with that ID", cursor.getCount() == 1);
        cursor.moveToFirst();

        String name = cursor.getString(cursor.getColumnIndex(ActivityDiaryContract.Diary.NOTE));
        assertTrue("name suffix", name.endsWith("-TestSuffix"));
    }

}
