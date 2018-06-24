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
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;

import de.rampro.activitydiary.ActivityDiaryApplication;
import de.rampro.activitydiary.ui.settings.SettingsActivity;

public class LocationHelper implements LocationListener {
    private static final String TAG = LocationHelper.class.getName();

    public static final LocationHelper helper = new LocationHelper();
    private static final long MIN_TIME = 1000 * 60 * 2; // for now every 2 minutes, TODO: make configurable
    private static final float MIN_DISTANCE = 50.0f;

    LocationManager locationManager = (LocationManager) ActivityDiaryApplication.getAppContext().getSystemService(Context.LOCATION_SERVICE);

    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(ActivityDiaryApplication.getAppContext());

    void updateLocation() {
        String setting = sharedPreferences.getString(SettingsActivity.KEY_PREF_USE_LOCATION, "off");

        if (setting.equals("off")) {
            // do nothing
        } else {
            int permissionCheckFine = PackageManager.PERMISSION_DENIED;
            int permissionCheckCoarse = PackageManager.PERMISSION_DENIED;

            if(setting.equals("gps")) {
                permissionCheckFine = ContextCompat.checkSelfPermission(ActivityDiaryApplication.getAppContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION);
                permissionCheckCoarse = permissionCheckFine;
            }else{
                permissionCheckCoarse = ContextCompat.checkSelfPermission(ActivityDiaryApplication.getAppContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION);
            }
            if(permissionCheckFine == PackageManager.PERMISSION_GRANTED){
                String locationProvider = LocationManager.GPS_PROVIDER;
                locationManager.requestLocationUpdates(locationProvider, MIN_TIME, MIN_DISTANCE, this);
            }

            if(permissionCheckCoarse == PackageManager.PERMISSION_GRANTED){
                String locationProvider = LocationManager.NETWORK_PROVIDER;
                locationManager.requestLocationUpdates(locationProvider, MIN_TIME, MIN_DISTANCE, this);

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
        Log.w(TAG, "location updated to " + location.toString());

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
}
