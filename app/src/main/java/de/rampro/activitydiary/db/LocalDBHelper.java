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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;


public class LocalDBHelper extends SQLiteOpenHelper {
    public static final String ACTIVITY_DB_TABLE = "activity";
    public static final String ACTIVITY_ALIAS_DB_TABLE = "activity_alias";
    public static final String CONDITION_DB_TABLE = "condition";
    public static final String CONDITIONS_DB_TABLE = "conditions_map";
    public static final String DIARY_DB_TABLE = "diary";

    LocalDBHelper(Context context) {
        super(context, "de.rampro.activitydiary", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        /* version 1 */
        /* TODO: should we add a constraint to forbid name reuse accross tables (even no alias to an existing name) */
        db.execSQL("CREATE TABLE " +
                ACTIVITY_DB_TABLE +
                "(" +
                " _id INTEGER PRIMARY KEY ASC, " +
                " _deleted INTEGER DEFAULT 0," +
                " name TEXT NOT NULL UNIQUE," +
                " color INTEGER," +
                " parent INTEGER " +
                ");");

        db.execSQL("CREATE TABLE " +
                ACTIVITY_ALIAS_DB_TABLE +
                "(" +
                " _deleted INTEGER DEFAULT 0," +
                " act_id INTEGER NOT NULL, " +
                " name TEXT NOT NULL UNIQUE," +
                " FOREIGN KEY(act_id) REFERENCES activity(_id) " +
                ");");

        db.execSQL("CREATE TABLE " +
                CONDITION_DB_TABLE +
                "(" +
                " _id INTEGER PRIMARY KEY ASC, " +
                " _deleted INTEGER DEFAULT 0," +
                " name TEXT NOT NULL UNIQUE, " +
                " type TEXT, " +
                " parameter TEXT " +
                ");");

        db.execSQL("CREATE TABLE " +
                CONDITIONS_DB_TABLE +
                "(" +
                " _deleted INTEGER DEFAULT 0," +
                " act_id INTEGER NOT NULL, " +
                " cond_id INTEGER NOT NULL, " +
                " FOREIGN KEY(act_id) REFERENCES activity(_id), " +
                " FOREIGN KEY(cond_id) REFERENCES condition(_id) " +
                ");");

        db.execSQL("CREATE TABLE " +
                DIARY_DB_TABLE +
                "(" +
                ActivityDiaryContract.Diary._ID + " INTEGER PRIMARY KEY ASC, " +
                ActivityDiaryContract.Diary._DELETED + " INTEGER DEFAULT 0," +
                ActivityDiaryContract.Diary.ACT_ID + " INTEGER NOT NULL, " +
                ActivityDiaryContract.Diary.START + " INTEGER NOT NULL, " +
                ActivityDiaryContract.Diary.END + " INTEGER, " +
                " FOREIGN KEY(act_id) REFERENCES activity(_id) " +
                ");");

        db.execSQL("INSERT INTO " +
                ACTIVITY_DB_TABLE +
                "(name, color)" +
                " VALUES " +
                " ('Gardening', '" + Color.parseColor("#388E3C") + "')," +
                " ('Woodworking', '" + Color.parseColor("#5D4037") + "')," +
                " ('Officework', '" + Color.parseColor("#00796B") + "')," +
                " ('Swimming', '" + Color.parseColor("#0288D1") + "')," +
                " ('Relaxing', '" + Color.parseColor("#FFA000") + "')," +
                " ('Cooking', '" + Color.parseColor("#AFB42B") + "')," +
                " ('Cleaning', '" + Color.parseColor("#CFD8DC") + "')," +
                " ('Cinema', '" + Color.parseColor("#C2185B") + "')," +
                " ('Sleeping', '" + Color.parseColor("#303F9F") + "');");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        /**
         * The SQLite ALTER TABLE documentation can be found
         * <a href="http://sqlite.org/lang_altertable.html">here</a>. If you add new columns
         * you can use ALTER TABLE to insert them into a live table. If you rename or remove columns
         * you can use ALTER TABLE to rename the old table, then create the new table and then
         * populate the new table with the contents of the old table.
         */
        if(newVersion > 1){
            /* upgrade from 1 to 2 */
        }
        if(newVersion > 2){
            /* upgrade from 2 to 3 */
        }
        if(newVersion > 1){
            throw new RuntimeException("Database upgrade to version " + newVersion + " nyi.");
        }
    }
}
