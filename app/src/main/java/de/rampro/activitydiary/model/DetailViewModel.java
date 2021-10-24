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

package de.rampro.activitydiary.model;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import android.net.Uri;

import de.rampro.activitydiary.db.ActivityDiaryContract;

/* the viewmodel for the details of a diary entry */
public class DetailViewModel extends ViewModel {
    public MutableLiveData<String> mNote;
    public MutableLiveData<String> mDuration;
    public MutableLiveData<String> mAvgDuration;
    public MutableLiveData<String> mStartOfLast;
    public MutableLiveData<String> mTotalToday;
    public MutableLiveData<String> mTotalWeek;
    public MutableLiveData<String> mTotalMonth;

    public MutableLiveData<DiaryActivity> mCurrentActivity;
    /* TODO: note and starttime from ActivityHelper to here, or even use directly the ContentProvider
     * register a listener to get updates directly from the ContentProvider */

    public MutableLiveData<Long> mDiaryEntryId;

    private Uri mDiaryUri;

    public DetailViewModel()
    {
        mNote = new MutableLiveData<>();
        mDuration = new MutableLiveData<>();
        mAvgDuration = new MutableLiveData<>();
        mStartOfLast = new MutableLiveData<>();
        mTotalToday = new MutableLiveData<>();
        mTotalWeek = new MutableLiveData<>();
        mTotalMonth = new MutableLiveData<>();
        mCurrentActivity = new MutableLiveData<>();
        mDiaryEntryId = new MutableLiveData<>();
    }

    public LiveData<String> note() {
        return mNote;
    }

    public LiveData<String> duration() {
        return mDuration;
    }

    public LiveData<DiaryActivity> currentActivity() {
        return mCurrentActivity;
    }

    public Uri getCurrentDiaryUri(){
        if(mCurrentActivity.getValue() == null){
            return null;
        } else {
            // TODO: this is not fully correct until the entry is stored in the DB and the ID is updated...
            return Uri.withAppendedPath(ActivityDiaryContract.Diary.CONTENT_URI,
                    Long.toString(mDiaryEntryId.getValue()));
        }
    }

    public void setCurrentDiaryUri(Uri currentDiaryUri) {
        mDiaryUri = currentDiaryUri;
        if(mDiaryUri != null) {
            mDiaryEntryId.setValue(Long.parseLong(mDiaryUri.getLastPathSegment()));
        }
    }
}
