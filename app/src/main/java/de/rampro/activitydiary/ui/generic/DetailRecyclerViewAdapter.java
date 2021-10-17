/*
 * ActivityDiary
 *
 * Copyright (C) 2017-2018 Raphael Mack http://www.raphael-mack.de
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

import android.content.AsyncQueryHandler;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.net.Uri;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.squareup.picasso.Picasso;

import de.rampro.activitydiary.ActivityDiaryApplication;
import de.rampro.activitydiary.R;
import de.rampro.activitydiary.db.ActivityDiaryContract;
import de.rampro.activitydiary.helpers.GraphicsHelper;

public class DetailRecyclerViewAdapter extends RecyclerView.Adapter<DetailViewHolders> implements DetailViewHolders.SelectListener {
    private static final String TAG = DetailRecyclerViewAdapter.class.getName();

    private class QHandler extends AsyncQueryHandler {
        private QHandler(){
            super(ActivityDiaryApplication.getAppContext().getContentResolver());
        }
    }

    private QHandler mQHandler = new QHandler();

    private static int lastAdapterId = 0;

    private Cursor mCursor;
    private Context mContext;
    private DataSetObserver mDataObserver;
    private int uriRowIdx = 0, idRowIdx = 0;
    private int mAdapterId;

    public DetailRecyclerViewAdapter(Context context, Cursor details){
        mAdapterId = lastAdapterId;
        lastAdapterId++;
        mCursor = details;
        mContext = context;
        // TODO: do we need this one here?
        mDataObserver = new DataSetObserver(){
            public void onChanged() {
                /* notify about the data change */
                notifyDataSetChanged();

                // TODO: remove #56
            }

            public void onInvalidated() {
                /* notify about the data change */
                notifyDataSetChanged();
            }
        };
        if (mCursor != null) {
            mCursor.registerDataSetObserver(mDataObserver);
            uriRowIdx = mCursor.getColumnIndex(ActivityDiaryContract.DiaryImage.URI);
            idRowIdx = mCursor.getColumnIndex(ActivityDiaryContract.DiaryImage._ID);
        }
    }

    @Override
    public DetailViewHolders onCreateViewHolder(ViewGroup parent, int viewType) {

        View layoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.detail_recycler_item, null);
        DetailViewHolders rcv = new DetailViewHolders(this, layoutView);

        return rcv;
    }

    @Override
    public void onBindViewHolder(DetailViewHolders holder, int position) {
        if (!mCursor.moveToPosition(position)) {
            throw new IllegalStateException("couldn't move cursor to position " + position);
        }
        String s;
        if(uriRowIdx >= 0) {
            s = mCursor.getString(uriRowIdx);
            Uri i = Uri.parse(s);

            Picasso.get().load(i)
                    .rotate(GraphicsHelper.getFileExifRotation(i))
                    .resize(500, 500)
                    .centerInside()
                    .into(holder.mSymbol);
        }else{
            Log.e(TAG, "onBindViewHolder: uriRowIdx = " + Integer.toString(uriRowIdx));
        }
    }

    @Override
    public int getItemCount() {
        if (mCursor != null) {
            return mCursor.getCount();
        }
        return 0;
    }

    public Cursor swapCursor(Cursor newCursor) {
        if (newCursor == mCursor) {
            return null;
        }
        final Cursor oldCursor = mCursor;
        if (oldCursor != null && mDataObserver != null) {
            oldCursor.unregisterDataSetObserver(mDataObserver);
        }
        mCursor = newCursor;
        if (mCursor != null) {
            if (mDataObserver != null) {
                mCursor.registerDataSetObserver(mDataObserver);
            }
            uriRowIdx = mCursor.getColumnIndex(ActivityDiaryContract.DiaryImage.URI);
            idRowIdx = mCursor.getColumnIndex(ActivityDiaryContract.DiaryImage._ID);
            notifyDataSetChanged();
        } else {
            uriRowIdx = -1;
            idRowIdx = -1;
            notifyDataSetChanged();
        }
        return oldCursor;
    }

    public int getAdapterId(){
        return mAdapterId;
    }

    public long getDiaryImageIdAt(int position){
        if(idRowIdx < 0){
            throw new IllegalStateException("idRowIdx not valid");
        }
        if(position < 0){
            throw new IllegalArgumentException("position (" + Integer.toString(position) + ") too small");
        }
        if(position >= mCursor.getCount()){
            throw new IllegalArgumentException("position (" + Integer.toString(position) + ") too small");
        }
        int pos = mCursor.getPosition();
        long result;
        mCursor.moveToPosition(position);
        result = mCursor.getLong(idRowIdx);
        mCursor.moveToPosition(pos);
        return result;
    }

    @Override
    public void onDetailItemClick(int adapterPosition) {
        long diaryImageId = getDiaryImageIdAt(adapterPosition);

        if (!mCursor.moveToPosition(adapterPosition)) {
            throw new IllegalStateException("couldn't move cursor to position " + adapterPosition);
        }

        String s = mCursor.getString(uriRowIdx);
        Uri i = Uri.parse(s);

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(i, "image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        mContext.startActivity(intent);

    }

    public boolean onDetailItemLongClick(int adapterPosition) {
        long diaryImageId = getDiaryImageIdAt(adapterPosition);
        //TODO: generalize the DetailView to include this code also
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext)
                .setTitle(R.string.dlg_delete_image_title)
                .setMessage(R.string.dlg_delete_image_text)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        ContentValues values = new ContentValues();
                        values.put(ActivityDiaryContract.DiaryImage._DELETED, 1);

                        mQHandler.startUpdate(0,
                                null,
                                ActivityDiaryContract.DiaryImage.CONTENT_URI,
                                values,
                                ActivityDiaryContract.DiaryImage._ID + "=?",
                                new String[]{Long.toString(diaryImageId)}
                        );

                    }})
                .setNegativeButton(android.R.string.no, null);

        builder.create().show();
        return true;
    }

}
