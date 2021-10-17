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

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

import de.rampro.activitydiary.R;

public class NoteEditDialog extends DialogFragment {
    private String note;
    private EditText input;
    private NoteEditDialogListener mListener;
    private long mDiaryId;

    public interface NoteEditDialogListener {
        void onNoteEditPositiveClock(String str, DialogFragment dialog);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        Dialog result;
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = getActivity().getLayoutInflater();

        builder.setTitle(R.string.dialog_title_note);

        View dlgView = inflater.inflate(R.layout.dialog_note_editor, null);
        input = (EditText) dlgView.findViewById(R.id.noteText);
        if(savedInstanceState != null){
            input.setText(savedInstanceState.getString("Note"));
        }else{
            input.setText(note);
        }
        input.setSelection(input.getText().length());
        builder.setView(dlgView)
                // Add action buttons
                .setPositiveButton(R.string.dlg_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        mListener.onNoteEditPositiveClock(input.getText().toString(), NoteEditDialog.this);
                    }
                })
                .setNegativeButton(R.string.dlg_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        result = builder.create();
        result.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        return result;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (NoteEditDialogListener) context;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(context.toString()
                    + " must implement NoteEditDialogListener");
        }
    }

    public void setText(String text){
        if(input != null){
            input.setText(text);
        }
        note = text;
    }

    public void setDiaryId(long diaryId){
        mDiaryId = diaryId;
    }

    public long getDiaryId(){
        return mDiaryId;
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putString("Note", input.getText().toString());
    }
}
