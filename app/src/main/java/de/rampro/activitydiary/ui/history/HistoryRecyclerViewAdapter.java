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

import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DataSetObserver;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import de.rampro.activitydiary.ActivityDiaryApplication;
import de.rampro.activitydiary.R;
import de.rampro.activitydiary.db.ActivityDiaryContract;
import de.rampro.activitydiary.helpers.TimeSpanFormatter;
import de.rampro.activitydiary.helpers.GraphicsHelper;
import de.rampro.activitydiary.ui.generic.DetailRecyclerViewAdapter;
import de.rampro.activitydiary.ui.settings.SettingsActivity;

public class HistoryRecyclerViewAdapter extends RecyclerView.Adapter<HistoryViewHolders> {
    private Cursor mCursor;
    private HistoryActivity mContext;
    private DataSetObserver mDataObserver;
    private int idRowIdx = -1, startRowIdx = -1,
                nameRowIdx = -1, endRowIdx = -1,
                colorRowIdx = -1, noteRowIdx = -1;

    private List<HistoryViewHolders> mViewHolders;

    private SelectListener mListener;

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
                SimpleDateFormat formatter = new SimpleDateFormat("MMMM yyyy");
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
        String formatString = sharedPref.getString(SettingsActivity.KEY_PREF_DATETIME_FORMAT,
                mContext.getResources().getString(R.string.default_datetime_format));
/* TODO: #36 register listener on preference change to redraw the date time formatting */

        holder.mStartLabel.setText(ActivityDiaryApplication.getAppContext().getResources().
                getString(R.string.history_start, DateFormat.format(formatString, start)));

        String noteStr = "";
        if(!mCursor.isNull(noteRowIdx)){
            noteStr = mCursor.getString(noteRowIdx);
            holder.mNoteLabel.setVisibility(View.VISIBLE);
        }else {
            holder.mNoteLabel.setVisibility(View.GONE);
        }
        holder.mNoteLabel.setText(noteStr);

        String duration;
        if(end == null){
            duration = ActivityDiaryApplication.getAppContext().getResources().
                    getString(R.string.duration_description, TimeSpanFormatter.fuzzyFormat(start, new Date()));
        }else {
            holder.mStartLabel.setText(ActivityDiaryApplication.getAppContext().getResources().
                    getString(R.string.history_start, DateFormat.format(formatString, start)));

            duration = ActivityDiaryApplication.getAppContext().getResources().
                    getString(R.string.history_end, DateFormat.format(formatString, end),
                    TimeSpanFormatter.format(end.getTime() - start.getTime()));
        }

        holder.mDurationLabel.setText(duration);

        /* TODO #33: set activity picture (icon + main pciture if available) */

        holder.mDetailAdapter = new DetailRecyclerViewAdapter(mContext, null);

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
