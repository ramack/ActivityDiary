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
package de.rampro.activitydiary.ui;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import de.rampro.activitydiary.R;
import de.rampro.activitydiary.model.DiaryActivity;

/*
 * MainActivity to show most of the UI, based on switching the fragements
 *
 * */
public class MainActivity extends BaseActivity implements View.OnClickListener, SelectRecyclerViewAdapter.SelectListener{
    private StaggeredGridLayoutManager gaggeredGridLayoutManager;

    private DiaryActivity currentActivity = null;
    List<DiaryActivity> gaggeredList;
    SelectRecyclerViewAdapter rcAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View contentView = inflater.inflate(R.layout.activity_main_content, null, false);

        setContent(contentView);
        RecyclerView recyclerView = (RecyclerView)findViewById(R.id.select_recycler);

        int rows = 0;
        Configuration configuration = getResources().getConfiguration();
        int screenSize = configuration.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;

        TypedValue value = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.listPreferredItemHeightSmall, value, true);
        android.util.DisplayMetrics metrics = new android.util.DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        float ret = value.getDimension(metrics);
        rows = (int)Math.floor(configuration.screenHeightDp / value.getDimension(metrics));

        gaggeredGridLayoutManager = new StaggeredGridLayoutManager(rows, StaggeredGridLayoutManager.HORIZONTAL);
        recyclerView.setLayoutManager(gaggeredGridLayoutManager);

        View selector = contentView.findViewById(R.id.activity_background);
        selector.setOnClickListener(this);

        mNavigationView.getMenu().findItem(R.id.nav_main).setChecked(true);

        /* TODO: populate from ContentProvider */
        gaggeredList = new ArrayList<DiaryActivity>();
        gaggeredList.add(new DiaryActivity(1,"Gardening", Color.parseColor("#388E3C")));
        gaggeredList.add(new DiaryActivity(2,"Woodworking", Color.parseColor("#5D4037")));
        gaggeredList.add(new DiaryActivity(3,"Officework", Color.parseColor("#00796B")));
        gaggeredList.add(new DiaryActivity(4,"Swimming", Color.parseColor("#0288D1")));
        gaggeredList.add(new DiaryActivity(5,"Relaxing", Color.parseColor("#FFA000")));
        gaggeredList.add(new DiaryActivity(6,"Cooking", Color.parseColor("#AFB42B")));
        gaggeredList.add(new DiaryActivity(7,"Cleaning", Color.parseColor("#CFD8DC")));
        gaggeredList.add(new DiaryActivity(8,"Cinema", Color.parseColor("#C2185B")));
        gaggeredList.add(new DiaryActivity(9,"Sleeping", Color.parseColor("#303F9F")));

        rcAdapter = new SelectRecyclerViewAdapter(MainActivity.this, gaggeredList);
        recyclerView.setAdapter(rcAdapter);

    /* TODO: add a search box in the toolbar to filter / fuzzy search
    * see http://www.vogella.com/tutorials/AndroidActionBar/article.html and https://developer.android.com/training/appbar/action-views.html*/
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle your other action bar items...

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view){
        Toast.makeText(this, "You clicked on the current activity! Boom!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onItemClick(int adapterPosition) {
        currentActivity = gaggeredList.get(adapterPosition);
        ((TextView)findViewById(R.id.activity_name)).setText(currentActivity.getName());
        findViewById(R.id.activity_background).setBackgroundColor(currentActivity.getColor());
    }
}
