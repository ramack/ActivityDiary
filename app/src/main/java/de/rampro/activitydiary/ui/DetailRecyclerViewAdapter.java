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

package de.rampro.activitydiary.ui;

import android.content.Context;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.squareup.picasso.Picasso;

import de.rampro.activitydiary.R;
import de.rampro.activitydiary.db.ActivityDiaryContract;

public class DetailRecyclerViewAdapter extends RecyclerView.Adapter<DetailViewHolders>{
    private SelectListener mSelectListener;
    private Cursor mCursor;
    private Context mContext;
    private DataSetObserver mDataSetObserver;
    private int uriRowIdx = 0;

    public DetailRecyclerViewAdapter(Context context, SelectListener selectListener, Cursor details){
        mCursor = details;
        mSelectListener = selectListener;
        mContext = context;
// TODO see https://medium.com/@emuneee/cursors-recyclerviews-and-itemanimators-b3f08cfbd370
//        mDataSetObserver = new NotifyingDataSetObserver();
        if (mCursor != null) {
            mCursor.registerDataSetObserver(mDataSetObserver);
            uriRowIdx = mCursor.getColumnIndex(ActivityDiaryContract.DiaryImage.URI);
        }
    }

    @Override
    public DetailViewHolders onCreateViewHolder(ViewGroup parent, int viewType) {

        View layoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.detail_recycler_item, null);
        DetailViewHolders rcv = new DetailViewHolders(mSelectListener, layoutView);

        return rcv;
    }

    @Override
    public void onBindViewHolder(DetailViewHolders holder, int position) {
        if (!mCursor.moveToPosition(position)) {
            throw new IllegalStateException("couldn't move cursor to position " + position);
        }
        Picasso.with(mContext).load(Uri.parse(mCursor.getString(uriRowIdx)))
                .resize(170,170)
                .centerCrop()
                .into(holder.mSymbol);
        ;
    }

    public Cursor getCursor() {
        return mCursor;
    }

    @Override
    public int getItemCount() {
        if (mCursor != null) {
            return mCursor.getCount();
        }
        return 0;
    }

    public interface SelectListener{
        void onItemClick(int adapterPosition);
    }

    public Cursor swapCursor(Cursor newCursor) {
        if (newCursor == mCursor) {
            return null;
        }
        final Cursor oldCursor = mCursor;
        if (oldCursor != null && mDataSetObserver != null) {
            oldCursor.unregisterDataSetObserver(mDataSetObserver);
        }
        mCursor = newCursor;
        if (mCursor != null) {
            if (mDataSetObserver != null) {
                mCursor.registerDataSetObserver(mDataSetObserver);
            }
            uriRowIdx = mCursor.getColumnIndex(ActivityDiaryContract.DiaryImage.URI);
            notifyDataSetChanged();
        } else {
            uriRowIdx = -1;
            notifyDataSetChanged();
        }
        return oldCursor;
    }

}
