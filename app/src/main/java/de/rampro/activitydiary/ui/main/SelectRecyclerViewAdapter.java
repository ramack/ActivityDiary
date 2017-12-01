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

package de.rampro.activitydiary.ui.main;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import de.rampro.activitydiary.R;
import de.rampro.activitydiary.model.DiaryActivity;

public class SelectRecyclerViewAdapter extends RecyclerView.Adapter<SelectViewHolders>{
    private List<DiaryActivity> mActivityList;
    private SelectListener mSelectListener;

    public SelectRecyclerViewAdapter(SelectListener selectListener, List<DiaryActivity> activityList){
        mActivityList = activityList;
        mSelectListener = selectListener;
    }

    @Override
    public SelectViewHolders onCreateViewHolder(ViewGroup parent, int viewType) {

        View layoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.select_recycler_item, null);
        SelectViewHolders rcv = new SelectViewHolders(mSelectListener, layoutView);

        return rcv;
    }

    @Override
    public void onBindViewHolder(SelectViewHolders holder, int position) {
        DiaryActivity act = mActivityList.get(position);
        holder.mName.setText(act.getName());
// TODO #33:        holder.mSymbol.setImageResource(act.getPhoto());
        holder.mBackground.setBackgroundColor(act.getColor());
        // TODO #31: set the width based on the likelyhood
    }

    @Override
    public int getItemCount() {
        return mActivityList.size();
    }

    public interface SelectListener{
        void onItemClick(int adapterPosition);
    }
}
