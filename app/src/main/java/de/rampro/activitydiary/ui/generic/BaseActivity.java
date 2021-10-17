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

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import com.google.android.material.navigation.NavigationView;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import de.rampro.activitydiary.R;
import de.rampro.activitydiary.ui.history.HistoryActivity;
import de.rampro.activitydiary.ui.location.MapActivity;
import de.rampro.activitydiary.ui.main.MainActivity;
import de.rampro.activitydiary.ui.settings.SettingsActivity;
import de.rampro.activitydiary.ui.statistics.StatisticsActivity;

/*
 * MainActivity to show most of the UI, based on switching the fragements
 *
 * */
public class BaseActivity extends AppCompatActivity {
    protected DrawerLayout mDrawerLayout;
    protected ActionBarDrawerToggle mDrawerToggle;
    protected NavigationView mNavigationView;

@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_base);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        Toolbar mainToolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(mainToolbar);
        mNavigationView = (NavigationView) findViewById(R.id.navigation_view);

//        View logoView = getLayoutInflater().inflate(R.layout.drawer_header, mNavigationView, false);

//        mNavigationView.addHeaderView(logoView);
        mDrawerToggle = new ActionBarDrawerToggle(
                this,
                mDrawerLayout,
                R.string.drawer_open,
                R.string.drawer_close
        );
        mDrawerLayout.addDrawerListener(mDrawerToggle);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        mNavigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {
                boolean highlight = true;
                switch (menuItem.getItemId()) {
                    case R.id.nav_main:
                        if(!menuItem.isChecked()) {
                            // start activity only if it is not currently checked
                            Intent intentmain = new Intent(BaseActivity.this, MainActivity.class);
                            intentmain.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intentmain);
                        }
                        break;
                    case R.id.nav_add_activity:
                        Intent intentaddact = new Intent(BaseActivity.this, EditActivity.class);
                        startActivity(intentaddact);
                        break;
                    case R.id.nav_activity_manager:
                        Intent intentmanage = new Intent(BaseActivity.this, ManageActivity.class);
                        startActivity(intentmanage);
                        break;
                    case R.id.nav_diary:
                        Intent intentdiary = new Intent(BaseActivity.this, HistoryActivity.class);
                        startActivity(intentdiary);
                        break;
                    case R.id.nav_map:
                        Intent intentmap = new Intent(BaseActivity.this, MapActivity.class);
                        startActivity(intentmap);
                        break;
                    case R.id.nav_statistics:
                        Intent intentstats = new Intent(BaseActivity.this, StatisticsActivity.class);
                        startActivity(intentstats);
                        break;
                    case R.id.nav_about:
                        Intent intentabout = new Intent(BaseActivity.this, AboutActivity.class);
                        startActivity(intentabout);
                        break;
                    case R.id.nav_privacy:
                        Intent intentpriv = new Intent(BaseActivity.this, PrivacyPolicyActivity.class);
                        startActivity(intentpriv);
                        break;

                    case R.id.nav_settings:
                        Intent intentsettings = new Intent(BaseActivity.this, SettingsActivity.class);
                        startActivity(intentsettings);
                        break;
                    default:
                        Toast.makeText(BaseActivity.this, menuItem.getTitle() + " is not yet implemented :-(", Toast.LENGTH_LONG).show();
                        break;
                }
                mDrawerLayout.closeDrawers();
                return highlight;
            }
        });
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }else if(item.getItemId() == android.R.id.home){
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    protected void setContent(View contentView){
        FrameLayout content = ((FrameLayout)findViewById(R.id.content_fragment));
        content.removeAllViews();
        content.addView(contentView);
    }
}
