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

package de.rampro.activitydiary.ui.generic;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import de.rampro.activitydiary.R;

class DetailViewHolders extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
    public interface SelectListener{
        void onDetailItemClick(int adapterPosition);
        boolean onDetailItemLongClick(int adapterPosition);
    }

    @NonNull public TextView mTextView;
    @NonNull public ImageView mSymbol;
    @NonNull private SelectListener mListener;

    public DetailViewHolders(@NonNull SelectListener listener, @NonNull View itemView) {
        super(itemView);
        itemView.setOnClickListener(this);
        itemView.setOnLongClickListener(this);
        mTextView = (TextView) itemView.findViewById(R.id.detailText);
        mSymbol = (ImageView) itemView.findViewById(R.id.picture);
        mListener = listener;
    }

    @Override
    public void onClick(View view) {
        final int position = getAdapterPosition();
        if(position != RecyclerView.NO_POSITION) {
            mListener.onDetailItemClick(position);
        }
    }

    @Override
    public boolean onLongClick(View v) {
        final int position = getAdapterPosition();
        if(position != RecyclerView.NO_POSITION) {
            return mListener.onDetailItemLongClick(position);
        }
        return false;
    }
}
