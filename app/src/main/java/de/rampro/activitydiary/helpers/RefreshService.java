/*
 * ActivityDiary
 *
 * Copyright (C) 2018 Raphael Mack http://www.raphael-mack.de
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

package de.rampro.activitydiary.helpers;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.util.Log;

public class RefreshService extends JobService {
    private static final String TAG = RefreshService.class.getName();
    boolean isWorking = false;
    boolean jobCancelled = false;

    // Called by the Android system when it's time to run the job
    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        Log.d(TAG, "Refreshing...");
        isWorking = true;

        /* UI refresh is so fast we can do it directly here */
        ActivityHelper.helper.updateNotification();

        // We need 'jobParameters' so we can call 'jobFinished'
        startWorkOnNewThread(jobParameters);

        return isWorking;
    }

    private void startWorkOnNewThread(final JobParameters jobParameters) {
        new Thread(new Runnable() {
            public void run() {
                refresh(jobParameters);
            }
        }).start();
    }

    private void refresh(JobParameters jobParameters) {

        if (jobCancelled)
            return;

        isWorking = false;
        boolean needsReschedule = false;
        ActivityHelper.helper.scheduleRefresh();
        LocationHelper.helper.updateLocation();
        jobFinished(jobParameters, needsReschedule);
    }

    // Called if the job was cancelled before being finished
    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        jobCancelled = true;
        boolean needsReschedule = isWorking;
        jobFinished(jobParameters, needsReschedule);
        return needsReschedule;
    }
}