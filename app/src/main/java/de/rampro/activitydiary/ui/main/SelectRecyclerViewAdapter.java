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

import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;

import de.rampro.activitydiary.R;
import de.rampro.activitydiary.helpers.GraphicsHelper;
import de.rampro.activitydiary.model.DiaryActivity;

public class SelectRecyclerViewAdapter extends RecyclerView.Adapter<SelectViewHolders>{
    private List<DiaryActivity> mActivityList;
    private SelectListener mSelectListener;

    public SelectRecyclerViewAdapter(SelectListener selectListener, List<DiaryActivity> activityList){
        mActivityList = activityList;
        mSelectListener = selectListener;
        setHasStableIds(true);
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
        NumberFormat formatter = new DecimalFormat("#0.00");
        holder.mName.setText(act.getName());
// show likelyhood in activity name
//     holder.mName.setText(act.getName() + " (" + formatter.format(ActivityHelper.helper.likelihoodFor(act)) + ")");
// TODO #33:        holder.mSymbol.setImageResource(act.getPhoto());
        holder.mBackground.setBackgroundColor(act.getColor());
        holder.mName.setTextColor(GraphicsHelper.textColorOnBackground(act.getColor()));

        // TODO #31: set the width based on the likelyhood
    }

    @Override
    public int getItemCount() {
        return mActivityList.size();
    }

    public interface SelectListener{
        void onItemClick(int adapterPosition);
        boolean onItemLongClick(int adapterPosition);
    }

    @Override
    public long getItemId(int position){
        return mActivityList.get(position).getId();
    }

    public int positionOf(DiaryActivity activity){
        return mActivityList.indexOf(activity);
    }

    public DiaryActivity item(int id){
        return mActivityList.get(id);
    }
}
