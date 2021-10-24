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

package de.rampro.activitydiary.helpers;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import androidx.exifinterface.media.ExifInterface;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.preference.PreferenceManager;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import de.rampro.activitydiary.ActivityDiaryApplication;
import de.rampro.activitydiary.R;
import de.rampro.activitydiary.model.DiaryActivity;
import de.rampro.activitydiary.ui.settings.SettingsActivity;

public class GraphicsHelper {
    public static final String TAG = "GraphicsHelper";

    /* list if recommended colors for new activites, populated from resources on startup */
    public static ArrayList<Integer> activityColorPalette = new ArrayList<Integer>(19);

    /* Checks if external storage is available for read and write */
    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    public static File imageStorageDirectory(){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(ActivityDiaryApplication.getAppContext());
        File directory;

        if(isExternalStorageWritable()) {
            directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        }else {
            directory = ActivityDiaryApplication.getAppContext().getFilesDir();
        }

        File root = new File(directory,
                sharedPreferences.getString(SettingsActivity.KEY_PREF_STORAGE_FOLDER, "ActivityDiary"));

        int permissionCheck = ContextCompat.checkSelfPermission(ActivityDiaryApplication.getAppContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            if (!root.exists()) {
                if (!root.mkdirs()) {
                    Log.e(TAG, "failed to create directory");
                    throw new RuntimeException("failed to create directory " + root.toString());
                }
            }
        } else {
            /* no permission, return null */
        }

        return root;
    }

    /* return the rotation of the image at uri from the exif data
     *
     * do better not call this for a network uri, as this would probably mean to fetch it twice
     * */
    public static int getFileExifRotation(Uri uri) {
        try {
            InputStream inputStream = ActivityDiaryApplication.getAppContext().getContentResolver().openInputStream(uri);
            ExifInterface exifInterface = new ExifInterface(inputStream);
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return 90;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return 180;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return 270;
                default:
                    return 0;
            }
        }catch (SecurityException e){
            Log.e(TAG, "reading image failed (for exif rotation)", e);
            return 0;
        }catch (IOException e) {
            Log.e(TAG, "reading image failed (for exif rotation)", e);
            return 0;
        }
    }

    /*
     * Calculate a font color with high contrast to the given background color
     */
    public static int textColorOnBackground(int color){
        int textColor = 0;
        Context context = ActivityDiaryApplication.getAppContext();
        if(ColorUtils.calculateLuminance(color) > 0.3){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                textColor = context.getResources().getColor(R.color.activityTextColorDark, null);
            }else{
                @SuppressWarnings("deprecation")
                Resources res= context.getResources();
                textColor = res.getColor(R.color.activityTextColorDark);
            }
        }else{
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                textColor = context.getResources().getColor(R.color.activityTextColorLight, null);
            }else{
                @SuppressWarnings("deprecation")
                Resources res= context.getResources();
                textColor = res.getColor(R.color.activityTextColorLight);
            }
        }
        return textColor;
    }

    // function to calculate the color to be set for next newly created activity
    public static int prepareColorForNextActivity(){
        int result = activityColorPalette.get(0);
        List<DiaryActivity> acts = ActivityHelper.helper.getActivities();
        double maxDistance = 0;

        // check for each color in the palette the average distance to what is already configured
        for(int c : activityColorPalette){
            double dist = 0;
            for(DiaryActivity a:acts){
                dist += Math.log(1 + (double)colorDistance(c, a.getColor()));
            }

            if(dist > maxDistance){
                // this one is better than the last
                result = c;
                maxDistance = dist;
            }
        }

        return result;
    }

    /* some function estimating perceptional color difference
     * see https://en.wikipedia.org/wiki/Color_difference for details
     */
    public static int colorDistance(int ci1, int ci2) {
        int r1 = (ci1 >> 16) & 0xFF;
        int r2 = (ci2 >> 16) & 0xFF;
        int g1 = (ci1 >> 8) & 0xFF;
        int g2 = (ci2 >> 8) & 0xFF;
        int b1 = (ci1 >> 0) & 0xFF;
        int b2 = (ci2 >> 0) & 0xFF;

        double f = Math.sqrt(2.0 * (r1-r2)*(r1-r2)
                           + 4.0 * (g1-g2)*(g1-g2)
                           + 3.0 * (b1-b2)*(b1-b2)
                           + (r1 + r2) / 2.0 * ((r1-r2)*(r1-r2) - (b1-b2)*(b1-b2)) / 256.0
                           );
        return (int)f;
    }
}
