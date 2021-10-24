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

import android.app.Activity;
import androidx.lifecycle.ViewModelProviders;
import android.content.Intent;
import androidx.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Handler;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Date;

import de.rampro.activitydiary.R;
import de.rampro.activitydiary.databinding.FragmentDetailStatsBinding;
import de.rampro.activitydiary.helpers.ActivityHelper;
import de.rampro.activitydiary.helpers.TimeSpanFormatter;
import de.rampro.activitydiary.model.DetailViewModel;
import de.rampro.activitydiary.ui.history.HistoryDetailActivity;

public class DetailStatFragement extends Fragment {

    private Handler updateDurationHandler = new Handler();
    private DetailViewModel viewModel;

    private View.OnClickListener headerClickHandler;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        FragmentDetailStatsBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_detail_stats, container, false);
        View view = binding.getRoot();

        headerClickHandler = v -> {
            if(viewModel.currentActivity().getValue() != null) {
                Intent i = new Intent(getActivity(), HistoryDetailActivity.class);
                // passing no diaryEntryID will edit the last one
                startActivity(i);
            }
        };
        view.setOnClickListener(headerClickHandler);

        view.findViewById(R.id.detail_content).setOnClickListener(headerClickHandler);

        viewModel = ViewModelProviders.of(getActivity()/*, viewModelFactory TODO */).get(DetailViewModel.class);

        binding.setViewModel(viewModel);
        // Specify the current activity as the lifecycle owner.
        binding.setLifecycleOwner(this);

        return view;
    }

    private Runnable updateDurationRunnable = new Runnable() {
        @Override
        public void run() {
            updateDurationTextView();
            updateDurationHandler.postDelayed(this, 10 * 1000);
        }
    };

    private void updateDurationTextView() {
        String duration = getResources().getString(R.string.duration_description, TimeSpanFormatter.fuzzyFormat(ActivityHelper.helper.getCurrentActivityStartTime(), new Date()));
        viewModel.mDuration.setValue(duration);
        Activity a = getActivity();
        if(a instanceof MainActivity){
            ((MainActivity)a).queryAllTotals();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateDurationTextView();
        updateDurationHandler.postDelayed(updateDurationRunnable, 10 * 1000);
    }

    @Override
    public void onPause() {
        updateDurationHandler.removeCallbacks(updateDurationRunnable);
        super.onPause();
    }

}
