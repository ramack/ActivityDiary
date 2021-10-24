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

package de.rampro.activitydiary.ui.location;

import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import androidx.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.config.Configuration;
import org.osmdroid.views.overlay.CopyrightOverlay;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.ScaleBarOverlay;

import java.util.ArrayList;
import java.util.List;

import de.rampro.activitydiary.R;
import de.rampro.activitydiary.db.ActivityDiaryContract;
import de.rampro.activitydiary.helpers.LocationHelper;
import de.rampro.activitydiary.ui.generic.BaseActivity;

public class MapActivity extends BaseActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final int LOADER_ID_INIT = 0;

    private static final String[] PROJECTION = new String[] {
            ActivityDiaryContract.DiaryLocation.LONGITUDE,
            ActivityDiaryContract.DiaryLocation.LATITUDE,
            ActivityDiaryContract.DiaryLocation.HACC
    };
    private static final String SELECTION_INIT = ActivityDiaryContract.DiaryLocation._DELETED + "=0";

    MapView map = null;
    TextView noMap = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View contentView = inflater.inflate(R.layout.activity_map, null, false);

        setContent(contentView);

        noMap = (TextView) findViewById(R.id.noMap);

        map = (MapView) findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setTilesScaledToDpi(true);
        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);

        IMapController mapController = map.getController();
        mapController.setZoom(14.0);
        GeoPoint startPoint = new GeoPoint(LocationHelper.helper.getCurrentLocation());
        mapController.setCenter(startPoint);

        CopyrightOverlay copyrightOverlay = new CopyrightOverlay(this);
        copyrightOverlay.setTextSize(10);
        map.getOverlays().add(copyrightOverlay);

        ScaleBarOverlay scaleBarOverlay = new ScaleBarOverlay(map);
        map.getOverlays().add(scaleBarOverlay);
        // Scale bar tries to draw as 1-inch, so to put it in the top center, set x offset to
        // half screen width, minus half an inch.
        scaleBarOverlay.setScaleBarOffset(
                (int) (getResources().getDisplayMetrics().widthPixels / 2 - getResources()
                        .getDisplayMetrics().xdpi / 2), 10);

        getLoaderManager().initLoader(LOADER_ID_INIT, null, this);

        mDrawerToggle.setDrawerIndicatorEnabled(false);
    }

    // Called when a new Loader needs to be created
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        if(id == LOADER_ID_INIT) {
            return new CursorLoader(this, ActivityDiaryContract.DiaryLocation.CONTENT_URI,
                    PROJECTION, SELECTION_INIT, null, null);
        }else{
            return null;
        }
    }

    // Called when a previously created loader has finished loading
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Swap the new cursor in.  (The framework will take care of closing the
        // old cursor once we return.)
        Polyline line = new Polyline(map);
        line.setWidth(4f);
        line.setColor(Color.BLUE);
        if(data.getCount() > 0) {
            List<GeoPoint> pts = new ArrayList<>(data.getCount());
            if ((data != null) && data.moveToFirst()) {
                while (!data.isAfterLast()) {
                    int hacc_idx = data.getColumnIndex(ActivityDiaryContract.DiaryLocation.HACC);

                    if (data.isNull(hacc_idx) || data.getInt(hacc_idx) < 250) {
                        pts.add(new GeoPoint(data.getDouble(data.getColumnIndex(ActivityDiaryContract.DiaryLocation.LATITUDE)), data.getDouble(data.getColumnIndex(ActivityDiaryContract.DiaryLocation.LONGITUDE))));
                    }
                    data.moveToNext();
                }
            }
            line.setPoints(pts);
            map.getOverlayManager().add(line);
            noMap.setVisibility(View.GONE);
            map.setVisibility(View.VISIBLE);
        }else{
            noMap.setVisibility(View.VISIBLE);
            map.setVisibility(View.GONE);
        }
    }

    // Called when a previously created loader is reset, making the data unavailable
    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed.  We need to make sure we are no
        // longer using it.
    }

    @Override
    public void onResume(){
        mNavigationView.getMenu().findItem(R.id.nav_map).setChecked(true);
        super.onResume();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        map.onResume(); //needed for compass, my location overlays, v6.0.0 and up
    }

    public void onPause(){
        super.onPause();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
        map.onPause();  //needed for compass, my location overlays, v6.0.0 and up
    }

}
