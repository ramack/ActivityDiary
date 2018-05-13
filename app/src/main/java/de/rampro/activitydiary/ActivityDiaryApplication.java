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

import org.acra.*;
import org.acra.annotation.*;
import org.acra.data.StringFormat;

import de.rampro.activitydiary.helpers.GraphicsHelper;

@AcraCore(reportContent = { ReportField.APP_VERSION_CODE,
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
                ReportField.DISPLAY
        },
        buildConfigClass = de.rampro.activitydiary.BuildConfig.class,
        alsoReportToAndroidFramework = true,
        reportFormat = StringFormat.KEY_VALUE_LIST
)
@AcraMailSender(mailTo = "activity-diary@rampro.de")
@AcraDialog(resCommentPrompt = R.string.crash_dialog_comment_prompt, // optional. When defined, adds a user text field input with this text resource as a label
        resText = R.string.crash_dialog_text)
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

        ACRA.init(this);
    }

    public static Context getAppContext() {
        return ActivityDiaryApplication.context;
    }
}
