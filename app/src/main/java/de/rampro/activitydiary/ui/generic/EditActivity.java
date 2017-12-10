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
package de.rampro.activitydiary.ui.generic;

import android.content.AsyncQueryHandler;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.pes.androidmaterialcolorpickerdialog.ColorPicker;
import com.pes.androidmaterialcolorpickerdialog.ColorPickerCallback;

import de.rampro.activitydiary.ActivityDiaryApplication;
import de.rampro.activitydiary.R;
import de.rampro.activitydiary.db.ActivityDiaryContract;
import de.rampro.activitydiary.helpers.ActivityHelper;
import de.rampro.activitydiary.helpers.GraphicsHelper;
import de.rampro.activitydiary.model.DiaryActivity;

/*
 * EditActivity to add and modify activities
 *
 * */
public class EditActivity extends BaseActivity
{
    @Nullable
    private DiaryActivity currentActivity; /* null is for creating a new object */

    private final int QUERY_NAMES = 1;
    private final String COLOR_KEY = "COLOR";
    private final String NAME_KEY = "NAME";

    private EditText mActivityName;
    private TextInputLayout mActivityNameTIL;
    private ImageView mActivityColorImg;
    private int mActivityColor;
    private ColorPicker mCp;

    private class QHandler extends AsyncQueryHandler {
        /* Access only allowed via ActivityHelper.helper singleton */
        private QHandler(){
            super(ActivityDiaryApplication.getAppContext().getContentResolver());
        }
        @Override
        protected void onQueryComplete(int token, Object cookie,
                                       Cursor cursor) {
            if ((cursor != null)) {
                if(token == QUERY_NAMES && cursor.moveToFirst()) {
                    mActivityNameTIL.setError(getResources().getString(R.string.error_name_already_used, cursor.getString(0)));
                }
                else{
                    mActivityNameTIL.setError("");
                }
            }
            cursor.close();
        }

    }

    private QHandler mQHandler = new QHandler();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        Intent i = getIntent();
        int actId = i.getIntExtra("activityID", -1);
        if(actId == -1) {
            currentActivity = null;
        }else {
            currentActivity = ActivityHelper.helper.activityWithId(actId);
        }

        View contentView = inflater.inflate(R.layout.activity_edit_content, null, false);

        setContent(contentView);
        mActivityName = (EditText) contentView.findViewById(R.id.edit_activity_name);
        mActivityName.addTextChangedListener(new TextWatcher(){
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                checkConstraints();
            }
        });
        mActivityNameTIL = (TextInputLayout) findViewById(R.id.edit_activity_name_til);

        mActivityColorImg = (ImageView) contentView.findViewById(R.id.edit_activity_color);
        mActivityColorImg.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mCp.show();
            }
        });

        if(savedInstanceState != null) {
            String name = savedInstanceState.getString(NAME_KEY);
            mActivityColor = savedInstanceState.getInt(COLOR_KEY);
            mActivityName.setText(name);
            getSupportActionBar().setTitle(name);
            checkConstraints();
        }else{
            if (currentActivity != null) {
                mActivityName.setText(currentActivity.getName());
                getSupportActionBar().setTitle(currentActivity.getName());
                mActivityColor = currentActivity.getColor();
            } else {
                currentActivity = null;
                mActivityColor = GraphicsHelper.prepareColorForNextActivity();
            }
        }
        mActivityColorImg.setBackgroundColor(mActivityColor);
        mCp = new ColorPicker(EditActivity.this);
        mCp.setColor(mActivityColor);
        mCp.setCallback(new ColorPickerCallback() {
            @Override
            public void onColorChosen(@ColorInt int color) {
                mActivityColor = color;
                mActivityColorImg.setBackgroundColor(mActivityColor);
                mCp.hide();
            }
        });
        mDrawerToggle.setDrawerIndicatorEnabled(false);
        checkConstraints();
    }

    @Override
    public void onResume(){
        if(currentActivity == null) {
            mNavigationView.getMenu().findItem(R.id.nav_add_activity).setChecked(true);
        }
        super.onResume();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(NAME_KEY, mActivityName.getText().toString());
        outState.putInt(COLOR_KEY, mActivityColor);
        // call superclass to save any view hierarchy
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.edit_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.action_edit_delete:
                if(currentActivity != null){
                    ActivityHelper.helper.deleteActivity(currentActivity);
                }
                finish();
                break;
            case R.id.action_edit_done:
                CharSequence error = mActivityNameTIL.getError();
                if(error != null && error.length() > 0)
                {
                    Toast.makeText(EditActivity.this,
                            error,
                            Toast.LENGTH_LONG
                    ).show();
                }
                else {
                    if (currentActivity == null) {
                        ActivityHelper.helper.insertActivity(new DiaryActivity(-1, mActivityName.getText().toString(), mActivityColor));
                    } else {
                        currentActivity.setName(mActivityName.getText().toString());
                        currentActivity.setColor(mActivityColor);
                        ActivityHelper.helper.updateActivity(currentActivity);
                    }
                    finish();
                }
                break;
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void checkConstraints(){
        if(currentActivity == null) {
            mQHandler.startQuery(QUERY_NAMES,
                    null,
                    ActivityDiaryContract.DiaryActivity.CONTENT_URI,
                    new String[]{ActivityDiaryContract.DiaryActivity.NAME},
                    ActivityDiaryContract.DiaryActivity.NAME + "=?",
                    new String[]{mActivityName.getText().toString()}, null);
        }else{
            /* activity already there */
            mQHandler.startQuery(QUERY_NAMES,
                    null,
                    ActivityDiaryContract.DiaryActivity.CONTENT_URI,
                    new String[]{ActivityDiaryContract.DiaryActivity.NAME},
                    ActivityDiaryContract.DiaryActivity.NAME + "=? AND " +
                    ActivityDiaryContract.DiaryActivity._ID + " != ?",
                    new String[]{mActivityName.getText().toString(), Long.toString(currentActivity.getId())},
                    null);
        }
    }
}
