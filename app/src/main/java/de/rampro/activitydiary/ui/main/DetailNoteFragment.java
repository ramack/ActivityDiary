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
import androidx.databinding.DataBindingUtil;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.rampro.activitydiary.R;
import de.rampro.activitydiary.databinding.FragmentDetailNoteBinding;
import de.rampro.activitydiary.model.DetailViewModel;

public class DetailNoteFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        FragmentDetailNoteBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_detail_note, container, false);
        View view = binding.getRoot();
        //here data must be an instance of the class MarsDataProvider

        DetailViewModel viewModel = ViewModelProviders.of(getActivity()).get(DetailViewModel.class);

        binding.setViewModel(viewModel);
        // Specify the current activity as the lifecycle owner.
        binding.setLifecycleOwner(this);

        return view;
    }

}
