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
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import de.rampro.activitydiary.R;
import de.rampro.activitydiary.db.ActivityDiaryContract;
import de.rampro.activitydiary.model.ActivityHelper;
import de.rampro.activitydiary.model.DiaryActivity;

/*
 * MainActivity to show most of the UI, based on switching the fragements
 *
 * */
public class EditActivity extends BaseActivity {
    @Nullable
    Uri currentObject; /* null is for creating a new object */
    EditText mActivityName;
    ImageView mActivityColorImg;
    int mActivityColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        Intent i = getIntent();
        currentObject = i.getData();

        View contentView = inflater.inflate(R.layout.activity_edit_content, null, false);

        setContent(contentView);
        mActivityName = (EditText) contentView.findViewById(R.id.edit_activity_name);
        mActivityColorImg = (ImageView) contentView.findViewById(R.id.edit_activity_color);

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
                mActivityName.setText(cursor.getString(1));
                ActionBar ab = getSupportActionBar();
                ab.setTitle(cursor.getString(1));
                mActivityColorImg.setBackgroundColor(cursor.getInt(2));
                mActivityColor = cursor.getInt(2);
            } else {
                currentObject = null;
            }
        }else{
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mActivityColor = getResources().getColor(R.color.colorPrimary,null);
            }else{
                mActivityColor = getResources().getColor(R.color.colorPrimary);
            }
        }
        mDrawerToggle.setDrawerIndicatorEnabled(false);
        mNavigationView.getMenu().findItem(R.id.nav_add_activity).setChecked(true);
    }

    @Override
    public void onResume(){
        super.onResume();
        mActivityName.requestFocus();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.edit_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        ContentResolver resolver = getContentResolver();

        switch(item.getItemId()){
            case R.id.action_edit_delete:
                Toast.makeText(this, item.getTitle() + " is not yet implemented :-(", Toast.LENGTH_LONG).show();
                if(currentObject != null){
                    resolver.delete(currentObject, null, null);
                }
                finish();
                break;
            case R.id.action_edit_done:
                ContentValues values = new ContentValues();
                values.put(ActivityDiaryContract.DiaryActivity.NAME, mActivityName.getText().toString());
                values.put(ActivityDiaryContract.DiaryActivity.COLOR, mActivityColor);

                if(currentObject == null) {
                    resolver.insert(ActivityDiaryContract.DiaryActivity.CONTENT_URI, values);
                    ActivityHelper.helper.insertActivity(new DiaryActivity(-1, mActivityName.getText().toString(), mActivityColor));
                }else {
                    /* TODO: update DiaryActivity here */
                    long noUpdated = resolver.update(currentObject, values, null, null);
                }
                finish();
                break;
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
