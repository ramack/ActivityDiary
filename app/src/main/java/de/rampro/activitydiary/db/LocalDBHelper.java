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

    LocalDBHelper(Context context) {
        super(context, ActivityDiaryContract.AUTHORITY, null, CURRENT_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        /* version 2 */
        /* TODO: should we add a constraint to forbid name reuse accross tables (even no alias to an existing name) */
        db.execSQL("CREATE TABLE " +
                ActivityDiaryContract.DiaryActivity.TABLE_NAME +
                "(" +
                ActivityDiaryContract.DiaryActivity._ID + " INTEGER PRIMARY KEY ASC, " +
                ActivityDiaryContract.DiaryActivity._DELETED + " INTEGER DEFAULT 0," +
                ActivityDiaryContract.DiaryActivity.NAME + " TEXT NOT NULL UNIQUE," +
                ActivityDiaryContract.DiaryActivity.COLOR + " INTEGER," +
                ActivityDiaryContract.DiaryActivity.PARENT + " INTEGER " +
                ");");
/* TODO #20 do in a dedicated method, to allow an upgrade path of the DB
        db.execSQL("CREATE TABLE " +
                ACTIVITY_ALIAS_DB_TABLE +
                "(" +
                " _deleted INTEGER DEFAULT 0," +
                " act_id INTEGER NOT NULL, " +
                " name TEXT NOT NULL UNIQUE," +
                " FOREIGN KEY(act_id) REFERENCES activity(_id) " +
                ");");
 */
/*
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
*/
        db.execSQL("CREATE TABLE " +
                ActivityDiaryContract.Diary.TABLE_NAME +
                "(" +
                "_id INTEGER PRIMARY KEY ASC, " +
                "_deleted INTEGER DEFAULT 0," +
                ActivityDiaryContract.Diary.ACT_ID + " INTEGER NOT NULL, " +
                ActivityDiaryContract.Diary.START + " INTEGER NOT NULL, " +
                ActivityDiaryContract.Diary.END + " INTEGER DEFAULT NULL, " +
                ActivityDiaryContract.Diary.NOTE + " TEXT, " +
                " FOREIGN KEY(act_id) REFERENCES activity(_id) " +
                ");");

        db.execSQL("INSERT INTO " +
                ActivityDiaryContract.Diary.TABLE_NAME +
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

    public static final int CURRENT_VERSION = 2;

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        /**
         * The SQLite ALTER TABLE documentation can be found
         * <a href="http://sqlite.org/lang_altertable.html">here</a>. If you add new columns
         * you can use ALTER TABLE to insert them into a live table. If you rename or remove columns
         * you can use ALTER TABLE to rename the old table, then create the new table and then
         * populate the new table with the contents of the old table.
         */
        if(oldVersion == 1){
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
        if(oldVersion == 2){
            /* upgrade from 2 to 3 */
        }
        if(newVersion > 2){
            throw new RuntimeException("Database upgrade to version " + newVersion + " nyi.");
        }
    }
}
