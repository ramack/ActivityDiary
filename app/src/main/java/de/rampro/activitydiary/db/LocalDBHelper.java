/*
 * ActivityDiary
 *
 * Copyright (C) 2017-2018 Raphael Mack http://www.raphael-mack.de
 * Copyright (C) 2018 Bc. Ondrej Janitor
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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;


public class LocalDBHelper extends SQLiteOpenHelper {

    public LocalDBHelper(Context context) {
        super(context, ActivityDiaryContract.AUTHORITY, null, CURRENT_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createTablesForVersion(db, CURRENT_VERSION);

        /* now fill some sample data */
        db.execSQL("INSERT INTO " +
                ActivityDiaryContract.DiaryActivity.TABLE_NAME +
                "(" + ActivityDiaryContract.DiaryActivity.NAME + "," + ActivityDiaryContract.DiaryActivity.COLOR + ")" +
                " VALUES " +
                " ('Gardening', '" + Color.parseColor("#388e3c") + "');");
        db.execSQL("INSERT INTO " +
                ActivityDiaryContract.DiaryActivity.TABLE_NAME +
                "(" + ActivityDiaryContract.DiaryActivity.NAME + "," + ActivityDiaryContract.DiaryActivity.COLOR + ")" +
                " VALUES " +
                " ('Woodworking', '" + Color.parseColor("#5d4037") + "');");
        db.execSQL("INSERT INTO " +
                ActivityDiaryContract.DiaryActivity.TABLE_NAME +
                "(" + ActivityDiaryContract.DiaryActivity.NAME + "," + ActivityDiaryContract.DiaryActivity.COLOR + ")" +
                " VALUES " +
                " ('Officework', '" + Color.parseColor("#00796b") + "');");
        db.execSQL("INSERT INTO " +
                ActivityDiaryContract.DiaryActivity.TABLE_NAME +
                "(" + ActivityDiaryContract.DiaryActivity.NAME + "," + ActivityDiaryContract.DiaryActivity.COLOR + ")" +
                " VALUES " +
                " ('Swimming', '" + Color.parseColor("#0288d1") + "');");
        db.execSQL("INSERT INTO " +
                ActivityDiaryContract.DiaryActivity.TABLE_NAME +
                "(" + ActivityDiaryContract.DiaryActivity.NAME + "," + ActivityDiaryContract.DiaryActivity.COLOR + ")" +
                " VALUES " +
                " ('Relaxing', '" + Color.parseColor("#fbc02d") + "');");
        db.execSQL("INSERT INTO " +
                ActivityDiaryContract.DiaryActivity.TABLE_NAME +
                "(" + ActivityDiaryContract.DiaryActivity.NAME + "," + ActivityDiaryContract.DiaryActivity.COLOR + ")" +
                " VALUES " +
                " ('Cooking', '" + Color.parseColor("#e64a19") + "');");
        db.execSQL("INSERT INTO " +
                ActivityDiaryContract.DiaryActivity.TABLE_NAME +
                "(" + ActivityDiaryContract.DiaryActivity.NAME + "," + ActivityDiaryContract.DiaryActivity.COLOR + ")" +
                " VALUES " +
                " ('Cleaning', '" + Color.parseColor("#CFD8DC") + "');");
        db.execSQL("INSERT INTO " +
                ActivityDiaryContract.DiaryActivity.TABLE_NAME +
                "(" + ActivityDiaryContract.DiaryActivity.NAME + "," + ActivityDiaryContract.DiaryActivity.COLOR + ")" +
                " VALUES " +
                " ('Cinema', '" + Color.parseColor("#c2185b") + "');");
        db.execSQL("INSERT INTO " +
                ActivityDiaryContract.DiaryActivity.TABLE_NAME +
                "(" + ActivityDiaryContract.DiaryActivity.NAME + "," + ActivityDiaryContract.DiaryActivity.COLOR + ")" +
                " VALUES " +
                " ('Sleeping', '" + Color.parseColor("#303f9f") + "');");
    }

    public static final int CURRENT_VERSION = 5;
/*
    For debugging sometimes it is handy to drop a table again. This can easily be achieved in onDowngrade,
    after CURRENT_VERSION is decremented again

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE diary_search_suggestions");
    }
*/
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        /**
         * The SQLite ALTER TABLE documentation can be found
         * <a href="http://sqlite.org/lang_altertable.html">here</a>. If you add new columns
         * you can use ALTER TABLE to insert them into a live table. If you rename or remove columns
         * you can use ALTER TABLE to rename the old table, then create the new table and then
         * populate the new table with the contents of the old table.
         */
        if (oldVersion == 1) {
            /* upgrade from 1 to current */
            /* still alpha, so just delete and restart */
            /* do not use synmbolic names here, because in case of later rename the old names shall be dropped */
            db.execSQL("DROP TABLE activity");
            db.execSQL("DROP TABLE activity_alias");
            db.execSQL("DROP TABLE condition");
            db.execSQL("DROP TABLE conditions_map");
            db.execSQL("DROP TABLE diary");
            onCreate(db);
            oldVersion = CURRENT_VERSION;
        }
        if (oldVersion < 3) {
            /* upgrade from 2 to 3 */
            createDiaryImageTable(db);
        }
        if (oldVersion < 4) {
            /* upgrade from 3 to 4 */
            createDiaryLocationTable(db);
        }

        if (oldVersion < 5) {
            /* upgrade from 4 to 5 */
            createRecentSuggestionsTable(db);
        }

        if (newVersion > 5) {
            throw new RuntimeException("Database upgrade to version " + newVersion + " nyi.");
        }
    }

    private void createDiaryLocationTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " +
                ActivityDiaryContract.DiaryLocation.TABLE_NAME + " " +
                "(" +
                "_id INTEGER PRIMARY KEY ASC, " +
                "_deleted INTEGER DEFAULT 0, " +
                "ts INTEGER NOT NULL, " +
                "latitude REAL NOT NULL, " +
                "longitude REAL NOT NULL, " +
                "altitude REAL DEFAULT NULL, " +
                "speed INTEGER DEFAULT NULL," +
                "hacc INTEGER DEFAULT NULL, " +
                "vacc INTEGER DEFAULT NULL, " +
                "sacc INTEGER DEFAULT NULL " +
                ");");
    }

    private void createDiaryImageTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " +
                "diary_image " +
                "(" +
                "_id INTEGER PRIMARY KEY ASC, " +
                "_deleted INTEGER DEFAULT 0, " +
                "diary_id INTEGER NOT NULL, " +
                "uri TEXT NOT NULL, " +
                " FOREIGN KEY(diary_id) REFERENCES diary(_id)" +
                ");");
    }

    private void createRecentSuggestionsTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " +
                "diary_search_suggestions" +
                "(" +
                "_id INTEGER PRIMARY KEY ASC, " +
                "_deleted INTEGER DEFAULT 0, " +
                "action TEXT NOT NULL, " +
                "suggestion TEXT NOT NULL " +
                ");");
    }

    private void createTablesForVersion(SQLiteDatabase db, int version) {
        db.execSQL("CREATE TABLE " +
                "activity " +
                "(" +
                "_id INTEGER PRIMARY KEY ASC, " +
                "_deleted INTEGER DEFAULT 0, " +
                "name TEXT NOT NULL UNIQUE," +
                "color INTEGER," +
                "parent INTEGER " +
                ");");

        db.execSQL("CREATE TABLE " +
                "diary" +
                "(" +
                "_id INTEGER PRIMARY KEY ASC, " +
                "_deleted INTEGER DEFAULT 0," +
                "act_id INTEGER NOT NULL, " +
                "start INTEGER NOT NULL, " +
                "'end' INTEGER DEFAULT NULL, " +
                "note TEXT, " +
                " FOREIGN KEY(act_id) REFERENCES activity(_id) " +
                ");");

        if (version >= 3) {
            createDiaryImageTable(db);
        }

        if (version >= 4) {
            createDiaryLocationTable(db);
        }

        if (version >= 5) {
            createRecentSuggestionsTable(db);
        }

    }
}
