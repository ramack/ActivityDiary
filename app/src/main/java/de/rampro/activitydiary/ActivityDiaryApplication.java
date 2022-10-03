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

package de.rampro.activitydiary;

import android.app.Application;
import android.content.Context;
import android.graphics.Color;

import org.acra.ACRA;
import org.acra.ReportField;
import org.acra.config.*;
import org.acra.data.StringFormat;

import de.rampro.activitydiary.helpers.GraphicsHelper;

public class ActivityDiaryApplication extends Application {

    private static Context context;

    public void onCreate() {
        super.onCreate();
        ActivityDiaryApplication.context = getApplicationContext();

        /* now do some init stuff */
        String colors[] = context.getResources().getStringArray(R.array.activityColorPalette);

        for (int i = 0; i < colors.length; i++) {
            GraphicsHelper.activityColorPalette.add(Color.parseColor(colors[i]));
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        ACRA.init(this, new CoreConfigurationBuilder()
                .withBuildConfigClass(BuildConfig.class)
                .withReportFormat(StringFormat.JSON)
                .withReportContent(ReportField.APP_VERSION_CODE,
                        ReportField.APP_VERSION_NAME,
                        ReportField.USER_COMMENT,
                        ReportField.SHARED_PREFERENCES,
                        ReportField.ANDROID_VERSION,
                        ReportField.BRAND,
                        ReportField.PHONE_MODEL,
                        ReportField.CUSTOM_DATA,
                        ReportField.STACK_TRACE,
                        ReportField.BUILD,
                        ReportField.BUILD_CONFIG,
                        ReportField.CRASH_CONFIGURATION,
                        ReportField.DISPLAY)
                .withReportFormat(StringFormat.KEY_VALUE_LIST)
                .withAlsoReportToAndroidFramework(true)
                .withBuildConfigClass(de.rampro.activitydiary.BuildConfig.class)
                .withPluginConfigurations(
                        new DialogConfigurationBuilder()
                                .withCommentPrompt(getString(R.string.crash_dialog_comment_prompt))
                                .withText(getString(R.string.crash_dialog_text))
                                .build(),
                        new MailSenderConfigurationBuilder()
                                .withMailTo("activity-diary@rampro.de")
                                .withReportAsFile(true)
                                .withReportFileName("Crash.txt")
                                .build()
                )
        );
    }

    public static Context getAppContext() {
        return ActivityDiaryApplication.context;
    }
}
