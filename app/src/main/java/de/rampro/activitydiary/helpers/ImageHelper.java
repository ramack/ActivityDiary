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
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.support.media.ExifInterface;
import android.support.v4.content.ContextCompat;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import de.rampro.activitydiary.ActivityDiaryApplication;

public class ImageHelper {
    public static final String TAG = "ImageHelper";

    public static File imageStorageDirectory(){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(ActivityDiaryApplication.getAppContext());
        File root = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                sharedPreferences.getString("pref_storageFolder", "ActivityDiary"));

        int permissionCheck = ContextCompat.checkSelfPermission(ActivityDiaryApplication.getAppContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if(permissionCheck == PackageManager.PERMISSION_GRANTED) {
            if(!root.exists()){
                if( !root.mkdirs() ) {
                    Log.e(TAG, "failed to create directory");
                    throw new RuntimeException("failed to create directory " + root.toString());
                }
            }
        }else{
            /* no permission, return null */
        }

        return root;
    }

    /* return the rotation of the image at uri from the exif data
     *
     * do better not call this for a network uri, as this would probably mean to fetch it twice
     * */
    public static int getFileExifRotation(Uri uri) throws IOException {
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
    }

}
