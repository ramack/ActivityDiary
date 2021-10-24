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

import android.Manifest;
import android.content.AsyncQueryHandler;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import de.rampro.activitydiary.ActivityDiaryApplication;
import de.rampro.activitydiary.db.ActivityDiaryContract;
import de.rampro.activitydiary.ui.settings.SettingsActivity;

public class LocationHelper extends AsyncQueryHandler implements LocationListener, SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = LocationHelper.class.getName();

    public static final LocationHelper helper = new LocationHelper();
    private static final long MIN_TIME_DEF = 5; // for now every 5 minutes
    private static final long MIN_TIME_FACTOR = 1000 * 60;
    private static final float MIN_DISTANCE_DEF = 50.0f;

    private long minTime;
    private float minDist;
    private String setting;

    private Location currentLocation;

    LocationManager locationManager = (LocationManager) ActivityDiaryApplication.getAppContext().getSystemService(Context.LOCATION_SERVICE);

    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(ActivityDiaryApplication.getAppContext());

    public LocationHelper() {
        super(ActivityDiaryApplication.getAppContext().getContentResolver());
        currentLocation = new Location("DiaryLocation");
        updatePreferences();
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    public Location getCurrentLocation(){
        return currentLocation;
    }

    void updateLocation() {

        if (setting.equals("off")) {
            // do nothing
        } else {
            int permissionCheckFine = PackageManager.PERMISSION_DENIED;
            int permissionCheckCoarse = PackageManager.PERMISSION_DENIED;

            if(setting.equals("gps") && locationManager.getAllProviders().contains(LocationManager.GPS_PROVIDER)) {
                permissionCheckFine = ContextCompat.checkSelfPermission(ActivityDiaryApplication.getAppContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION);
                permissionCheckCoarse = permissionCheckFine;
            }else if(locationManager.getAllProviders().contains(LocationManager.NETWORK_PROVIDER)){
                permissionCheckCoarse = ContextCompat.checkSelfPermission(ActivityDiaryApplication.getAppContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION);
            }
            if(permissionCheckFine == PackageManager.PERMISSION_GRANTED){
                String locationProvider = LocationManager.GPS_PROVIDER;
                locationManager.requestLocationUpdates(locationProvider, minTime, minDist, this, Looper.getMainLooper());
            }else if(permissionCheckCoarse == PackageManager.PERMISSION_GRANTED){
                String locationProvider = LocationManager.NETWORK_PROVIDER;
                locationManager.requestLocationUpdates(locationProvider, minTime, minDist, this, Looper.getMainLooper());
            }
        }
    }

    /**
     * Called when the location has changed.
     * <p>
     * <p> There are no restrictions on the use of the supplied Location object.
     *
     * @param location The new location, as a Location object.
     */
    @Override
    public void onLocationChanged(Location location) {
        ContentValues values = new ContentValues();
        currentLocation = location;
        values.put(ActivityDiaryContract.DiaryLocation.TIMESTAMP, location.getTime());
        values.put(ActivityDiaryContract.DiaryLocation.LATITUDE, location.getLatitude());
        values.put(ActivityDiaryContract.DiaryLocation.LONGITUDE, location.getLongitude());
        if (location.hasAccuracy()) {
            values.put(ActivityDiaryContract.DiaryLocation.HACC, new Integer(Math.round(location.getAccuracy() * 10)));
        }
        if (location.hasSpeed()) {
            values.put(ActivityDiaryContract.DiaryLocation.SPEED, location.getSpeed());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (location.hasSpeedAccuracy()) {
                    values.put(ActivityDiaryContract.DiaryLocation.SACC, new Integer(Math.round(location.getSpeedAccuracyMetersPerSecond() * 10)));
                }
            }
        }
        if (location.hasAltitude()) {
            values.put(ActivityDiaryContract.DiaryLocation.ALTITUDE, location.getAltitude());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (location.hasVerticalAccuracy()) {
                    values.put(ActivityDiaryContract.DiaryLocation.VACC,  new Integer(Math.round(location.getVerticalAccuracyMeters() * 10)));
                }
            }
        }
        startInsert(0, null, ActivityDiaryContract.DiaryLocation.CONTENT_URI,
                values);

    }

    /**
     * Called when the provider status changes. This method is called when
     * a provider is unable to fetch a location or if the provider has recently
     * become available after a period of unavailability.
     *
     * @param provider the name of the location provider associated with this
     *                 update.
     * @param status   {@link LocationProvider#OUT_OF_SERVICE} if the
     *                 provider is out of service, and this is not expected to change in the
     *                 near future; {@link LocationProvider#TEMPORARILY_UNAVAILABLE} if
     *                 the provider is temporarily unavailable but is expected to be available
     *                 shortly; and {@link LocationProvider#AVAILABLE} if the
     *                 provider is currently available.
     * @param extras   an optional Bundle which will contain provider specific
     *                 status variables.
     *                 <p>
     *                 <p> A number of common key/value pairs for the extras Bundle are listed
     *                 below. Providers that use any of the keys on this list must
     *                 provide the corresponding value as described below.
     *                 <p>
     *                 <ul>
     *                 <li> satellites - the number of satellites used to derive the fix
     */
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    /**
     * Called when the provider is enabled by the user.
     *
     * @param provider the name of the location provider associated with this
     *                 update.
     */
    @Override
    public void onProviderEnabled(String provider) {

    }

    /**
     * Called when the provider is disabled by the user. If requestLocationUpdates
     * is called on an already disabled provider, this method is called
     * immediately.
     *
     * @param provider the name of the location provider associated with this
     *                 update.
     */
    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(key.equals(SettingsActivity.KEY_PREF_USE_LOCATION)
                || key.equals(SettingsActivity.KEY_PREF_LOCATION_AGE)
                || key.equals(SettingsActivity.KEY_PREF_LOCATION_DIST)
                ) {
            updatePreferences();
            updateLocation();
        }
    }

    void updatePreferences(){
        try {
            setting = sharedPreferences.getString(SettingsActivity.KEY_PREF_USE_LOCATION, "off");
            String minTimeS = sharedPreferences.getString(SettingsActivity.KEY_PREF_LOCATION_AGE, Long.toString(MIN_TIME_DEF * MIN_TIME_FACTOR));
            minTime = Long.parseLong(minTimeS);
            String minDistS = sharedPreferences.getString(SettingsActivity.KEY_PREF_LOCATION_DIST, Float.toString(MIN_DISTANCE_DEF));
            minDist = Float.parseFloat(minDistS);
        }catch (NumberFormatException e){
            /* no change in settings on invalid config */
        }
    }
}

