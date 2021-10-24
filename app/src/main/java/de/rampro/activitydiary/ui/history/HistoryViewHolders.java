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

package de.rampro.activitydiary.ui.history;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import de.rampro.activitydiary.R;
import de.rampro.activitydiary.ui.generic.DetailRecyclerViewAdapter;

class HistoryViewHolders extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {

    private int mDiaryEntryId;
    private int mLoaderId = -1;

    public TextView mSeparator;
    public TextView mStartLabel;
    public TextView mDurationLabel;
    public TextView mNoteLabel;
    public ImageView mSymbol;
    public CardView mActivityCardView;
    public TextView mName;
    public View mBackground;
    public DetailRecyclerViewAdapter mDetailAdapter;
    public RecyclerView mImageRecycler;
    private HistoryRecyclerViewAdapter.SelectListener mListener;

    public HistoryViewHolders(int loaderId, HistoryRecyclerViewAdapter.SelectListener listener, View itemView) {
        super(itemView);
        mLoaderId = loaderId;

        itemView.setOnClickListener(this);
        itemView.setOnLongClickListener(this);
        mSeparator = (TextView) itemView.findViewById(R.id.separator);
        mStartLabel = (TextView) itemView.findViewById(R.id.start_label);
        mNoteLabel = (TextView) itemView.findViewById(R.id.note);
        mDurationLabel = (TextView) itemView.findViewById(R.id.duration_label);
        mSymbol = (ImageView) itemView.findViewById(R.id.picture);
        mActivityCardView = (CardView) itemView.findViewById(R.id.activity_card);
        mName = (TextView) itemView.findViewById(R.id.activity_name);
        mBackground = itemView.findViewById(R.id.activity_background);
        mImageRecycler = (RecyclerView)itemView.findViewById(R.id.image_grid);
        mListener = listener;
    }

    public int getDiaryEntryID(){
        return mDiaryEntryId;
    }

    public void setDiaryEntryID(int id){
        mDiaryEntryId = id;
    }

    public int getDetailLoaderID(){
        return mLoaderId;
    }

    @Override
    public void onClick(View view) {
        final int position = getAdapterPosition();
        if(position != RecyclerView.NO_POSITION) {
            mListener.onItemClick(this, position, mDiaryEntryId);
        }
    }

    @Override
    public boolean onLongClick(View view) {
        final int position = getAdapterPosition();
        if(position != RecyclerView.NO_POSITION) {
            return mListener.onItemLongClick(this, position, mDiaryEntryId);
        }
        return false;
    }

}
