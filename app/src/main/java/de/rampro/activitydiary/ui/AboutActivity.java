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


import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import de.rampro.activitydiary.BuildConfig;
import de.rampro.activitydiary.R;

public class AboutActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View contentView = inflater.inflate(R.layout.activity_about, null, false);

        setContent(contentView);
        TextView aboutText = (TextView)findViewById(R.id.aboutTextView);

        String appName = getResources().getString(R.string.app_name);
        String versionName = BuildConfig.VERSION_NAME;

        String mergedAboutText = getResources().getString(R.string.about_text, appName, versionName);
        aboutText.setText(Html.fromHtml(mergedAboutText));
        aboutText.setMovementMethod(LinkMovementMethod.getInstance());

        mDrawerToggle.setDrawerIndicatorEnabled(false);
    }

    @Override
    public void onResume(){
        mNavigationView.getMenu().findItem(R.id.nav_about).setChecked(true);
        super.onResume();
    }
}
