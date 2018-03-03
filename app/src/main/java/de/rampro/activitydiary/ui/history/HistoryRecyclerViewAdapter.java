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

import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import de.rampro.activitydiary.ActivityDiaryApplication;
import de.rampro.activitydiary.R;
import de.rampro.activitydiary.db.ActivityDiaryContract;
import de.rampro.activitydiary.helpers.FuzzyTimeSpanFormatter;
import de.rampro.activitydiary.helpers.GraphicsHelper;
import de.rampro.activitydiary.ui.generic.DetailRecyclerViewAdapter;
import de.rampro.activitydiary.ui.settings.SettingsActivity;

public class HistoryRecyclerViewAdapter extends RecyclerView.Adapter<HistoryViewHolders> implements DetailRecyclerViewAdapter.SelectListener {
    private Cursor mCursor;
    private HistoryActivity mContext;
    private DataSetObserver mDataObserver;
    private int idRowIdx = -1, startRowIdx = -1,
                nameRowIdx = -1, endRowIdx = -1,
                colorRowIdx = -1, noteRowIdx = -1;

    private List<HistoryViewHolders> mViewHolders;

    private SelectListener mListener;

    @Override
    public void onDetailItemClick(int adapterPosition) {
        Toast.makeText(mContext, "history picture " + Integer.toString(adapterPosition) +  " clicked", Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onDetailItemLongClick(final int adapterPosition) {
        //TODO: generalize the DetailView to include this code also
        //      such that it is not duplicated between MainActivity and HistoryRecyclerViewAdapter
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext)
                .setTitle(R.string.dlg_delete_image_title)
                .setMessage(R.string.dlg_delete_image_text)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        ContentValues values = new ContentValues();
                        values.put(ActivityDiaryContract.DiaryImage._DELETED, 1);

                        mContext.mQHandler.startUpdate(0,
                                null,
                                ActivityDiaryContract.DiaryImage.CONTENT_URI,
                                values,
                                ActivityDiaryContract.DiaryImage._ID + "=?",
                                new String[]{Long.toString(
                                        mViewHolders.get(adapterPosition).mDetailAdapter.getDiaryImageIdAt(adapterPosition))}
                        );

                    }})
                .setNegativeButton(android.R.string.no, null);

        builder.create().show();
        return true;
    }

    public interface SelectListener{
        void onItemClick(HistoryViewHolders viewHolder, int adapterPosition, int diaryID);
        boolean onItemLongClick(HistoryViewHolders viewHolder, int adapterPosition, int diaryID);
    }

    public HistoryRecyclerViewAdapter(HistoryActivity context, SelectListener listener,  Cursor history){
        mCursor = history;
        mListener = listener;
        mContext = context;
        mViewHolders = new ArrayList<>(17);

        mDataObserver = new DataSetObserver(){
            public void onChanged() {
                /* notify about the data change */
                notifyDataSetChanged();
            }

            public void onInvalidated() {
                /* notify about the data change */
                notifyDataSetChanged();
            }
        };
        if (mCursor != null) {
            mCursor.registerDataSetObserver(mDataObserver);
            setRowIndex();
        }
    }

    @Override
    public HistoryViewHolders onCreateViewHolder(ViewGroup parent, int viewType) {

        View layoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_history_entry, null);
        HistoryViewHolders rcv = new HistoryViewHolders(mViewHolders.size(), mListener, layoutView);
        mViewHolders.add(rcv);
        return rcv;
    }

    @Override
    public void onBindViewHolder(HistoryViewHolders holder, int position) {
        boolean showHeader = false;
        String header = "";

        if (!mCursor.moveToPosition(position)) {
            throw new IllegalStateException("couldn't move cursor to position " + position);
        }

        Date start = new Date(mCursor.getLong(startRowIdx));
        Date end;
        String name = mCursor.getString(nameRowIdx);
        int color = mCursor.getInt(colorRowIdx);

        holder.mBackground.setBackgroundColor(color);
        holder.mName.setTextColor(GraphicsHelper.textColorOnBackground(color));

        holder.setDiaryEntryID(mCursor.getInt(idRowIdx));

        if(mCursor.isNull(endRowIdx)) {
            end = null;
        }else {
            end = new Date(mCursor.getLong(endRowIdx));
        }

        Calendar startCal = Calendar.getInstance();
        startCal.setTimeInMillis(mCursor.getLong(startRowIdx));

        if(mCursor.isFirst()){
            showHeader = true;
        }else {
            mCursor.moveToPrevious();
            Calendar clast = Calendar.getInstance();
            clast.setTimeInMillis(mCursor.getLong(startRowIdx));
            mCursor.moveToNext();

            if(clast.get(Calendar.DATE) != startCal.get(Calendar.DATE)) {
                showHeader = true;
            }
        }
        if(showHeader){
            Calendar now = Calendar.getInstance();
            if(now.get(Calendar.DATE) == startCal.get(Calendar.DATE)){
                header = mContext.getResources().getString(R.string.today);
            }else if(now.get(Calendar.DATE) - startCal.get(Calendar.DATE) == 1){
                header = mContext.getResources().getString(R.string.yesterday);
            }else if(now.get(Calendar.WEEK_OF_YEAR) - startCal.get(Calendar.WEEK_OF_YEAR) == 0){
                SimpleDateFormat formatter = new SimpleDateFormat("EEEE");
                header = formatter.format(start);
            }else if(now.get(Calendar.WEEK_OF_YEAR) - startCal.get(Calendar.WEEK_OF_YEAR) == 1){
                header = mContext.getResources().getString(R.string.lastWeek);
                    /* TODO: this is shown for each day last week, which is too much... -> refactor to get rid of showHeader or set it in this if-elsif-chain */
            }else{
                SimpleDateFormat formatter = new SimpleDateFormat("MMMMM yyyy");
                header = formatter.format(start);
            }
        }
        if(showHeader) {
            holder.mSeparator.setVisibility(View.VISIBLE);
            holder.mSeparator.setText(header);
        }else{
            holder.mSeparator.setVisibility(View.GONE);
        }

        holder.mName.setText(name);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(ActivityDiaryApplication.getAppContext());
        String formatString = sharedPref.getString(SettingsActivity.KEY_PREF_DATE_FORMAT,
                mContext.getResources().getString(R.string.default_datetime_format));
/* TODO: #36 register listener on preference change to redraw the date time formatting */

        holder.mStartLabel.setText(DateFormat.format(formatString, start));

        String noteStr = "";
        if(!mCursor.isNull(noteRowIdx)){
            noteStr = mCursor.getString(noteRowIdx);
            holder.mNoteLabel.setVisibility(View.VISIBLE);
        }else {
            holder.mNoteLabel.setVisibility(View.GONE);
        }
        holder.mNoteLabel.setText(noteStr);

        String duration = mContext.getResources().
                getString(R.string.duration_description, FuzzyTimeSpanFormatter.format(start, end));

        holder.mDurationLabel.setText(duration);

        /* TODO #33: set activity picture (icon + main pciture if available) */

        holder.mDetailAdapter = new DetailRecyclerViewAdapter(mContext, this, null);

        mContext.addDetailAdapter(mCursor.getLong(idRowIdx), holder.mDetailAdapter);

        /* TODO: make it a configuration option how many picture columns we should show */
        RecyclerView.LayoutManager layoutMan = new StaggeredGridLayoutManager(3, StaggeredGridLayoutManager.VERTICAL);
        holder.mImageRecycler.setLayoutManager(layoutMan);
        holder.mImageRecycler.setAdapter(holder.mDetailAdapter);
        /* click handlers are done via ViewHolder */
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
            setRowIndex();
            notifyDataSetChanged();
        } else {
            idRowIdx = -1;
            notifyDataSetChanged();
        }
        return oldCursor;
    }

    private void setRowIndex(){
        idRowIdx = mCursor.getColumnIndex(ActivityDiaryContract.Diary._ID);
        startRowIdx = mCursor.getColumnIndex(ActivityDiaryContract.Diary.START);
        nameRowIdx = mCursor.getColumnIndex(ActivityDiaryContract.DiaryActivity.NAME);
        colorRowIdx = mCursor.getColumnIndex(ActivityDiaryContract.DiaryActivity.COLOR);
        endRowIdx = mCursor.getColumnIndex(ActivityDiaryContract.Diary.END);
        noteRowIdx = mCursor.getColumnIndex(ActivityDiaryContract.Diary.NOTE);
    }

}
