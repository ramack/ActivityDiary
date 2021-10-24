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

package de.rampro.activitydiary.ui.main;

import androidx.lifecycle.ViewModelProviders;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.rampro.activitydiary.R;
import de.rampro.activitydiary.db.ActivityDiaryContract;
import de.rampro.activitydiary.model.DetailViewModel;
import de.rampro.activitydiary.ui.generic.DetailRecyclerViewAdapter;

public class DetailPictureFragement extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String[] PROJECTION_IMG = new String[]{
            ActivityDiaryContract.DiaryImage.URI,
            ActivityDiaryContract.DiaryImage._ID
    };

    private RecyclerView detailRecyclerView;
    private DetailRecyclerViewAdapter detailAdapter;
    private DetailViewModel viewModel;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_detail_pictures, container, false);

        viewModel = ViewModelProviders.of(getActivity()).get(DetailViewModel.class);

        detailRecyclerView = view.findViewById(R.id.picture_recycler);

        StaggeredGridLayoutManager detailLayoutManager = new StaggeredGridLayoutManager(3, StaggeredGridLayoutManager.VERTICAL);

        detailRecyclerView.setLayoutManager(detailLayoutManager);

// TODO:check        detailRecyclerView.setNestedScrollingEnabled(true);
        detailAdapter = new DetailRecyclerViewAdapter(getActivity(),
                null);
        detailRecyclerView.setAdapter(detailAdapter);

        reload();
        return view;
    }

    public void reload() {
        getActivity().getSupportLoaderManager().restartLoader(0,null,this);
    }


    // Called when a new Loader needs to be created
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri currentDiaryUri = viewModel.getCurrentDiaryUri();
        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return new CursorLoader(getActivity(), ActivityDiaryContract.DiaryImage.CONTENT_URI,
                PROJECTION_IMG,
                ActivityDiaryContract.DiaryImage.TABLE_NAME + "." + ActivityDiaryContract.DiaryImage.DIARY_ID + "=? AND "
                        + ActivityDiaryContract.DiaryImage._DELETED + "=0",
                currentDiaryUri == null ? new String[]{"0"}:new String[]{currentDiaryUri.getLastPathSegment()},
                ActivityDiaryContract.DiaryImage.SORT_ORDER_DEFAULT);
    }

    // Called when a previously created loader has finished loading
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Swap the new cursor in
        detailAdapter.swapCursor(data);

    }

    // Called when a previously created loader is reset, making the data unavailable
    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed.  We need to make sure we are no
        // longer using it.
        detailAdapter.swapCursor(null);
    }

}
