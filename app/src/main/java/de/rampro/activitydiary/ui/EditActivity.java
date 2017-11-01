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
package de.rampro.activitydiary.ui;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import de.rampro.activitydiary.R;
import de.rampro.activitydiary.db.ActivityDiaryContract;

/*
 * MainActivity to show most of the UI, based on switching the fragements
 *
 * */
public class EditActivity extends BaseActivity {
    @Nullable
    Uri currentObject; /* null is for creating a new object */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        Intent i = getIntent();
        currentObject = i.getData();

        View contentView = inflater.inflate(R.layout.activity_edit_content, null, false);

        setContent(contentView);
        if(currentObject != null) {
            ContentResolver resolver = getContentResolver();
            String[] projection = new String[]{
                    ActivityDiaryContract.DiaryActivity._ID,
                    ActivityDiaryContract.DiaryActivity.NAME,
                    ActivityDiaryContract.DiaryActivity.COLOR
            };
            Cursor cursor =
                    resolver.query(currentObject,
                            projection,
                            null,
                            null,
                            null);
            if (cursor.moveToFirst() && cursor.getCount() == 1) {
                /* now update the list */
                EditText e = (EditText) contentView.findViewById(R.id.edit_activity_name);
                e.setText(cursor.getString(1));
                ActionBar ab = getSupportActionBar();
                ab.setTitle(cursor.getString(1));
                ImageView col = (ImageView) contentView.findViewById(R.id.edit_activity_color);
                col.setBackgroundColor(cursor.getInt(2));
            } else {
                currentObject = null;
            }
        }

    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle your other action bar items...

        return super.onOptionsItemSelected(item);
    }
}
