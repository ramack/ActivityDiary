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
import android.graphics.Color;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.fragment.app.DialogFragment;
import android.view.View;
import android.widget.TextView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.shadows.ShadowToast;

import java.util.concurrent.TimeUnit;

import de.rampro.activitydiary.R;
import de.rampro.activitydiary.helpers.ActivityHelper;
import de.rampro.activitydiary.model.DiaryActivity;
import de.rampro.activitydiary.ui.main.MainActivity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * UI unit testing with robolectric
 */
@Config(sdk=27) // TODO: remove this workaround after robolectric is able to inflate the fragment in 28
@RunWith(RobolectricTestRunner.class)
public class MainActivityTest {
    private MainActivity activity;

    @Before
    public void setUp() throws Exception
    {
        activity = Robolectric.buildActivity( MainActivity.class )
                .create()
                .resume()
                .get();
    }

    @Test
    public void currentNullActivity() throws Exception {
        /* show absense of #60 */
        MainActivity activity = Robolectric.setupActivity(MainActivity.class);

        View card = activity.findViewById(R.id.card);
        assertNotNull("Current activity card available", card);

        TextView nameView = (TextView) card.findViewById(R.id.activity_name);
        assertNotNull("Current activity Text available", nameView);

        /* initial creation shall not have any activity selected */
        assertEquals(nameView.getText(), "<No Activity>");

        assertNull(ActivityHelper.helper.getCurrentActivity());

        FloatingActionButton fabNoteEdit = (FloatingActionButton) activity.findViewById(R.id.fab_edit_note);
        FloatingActionButton fabAttachPicture = (FloatingActionButton) activity.findViewById(R.id.fab_attach_picture);

        assertNotNull("we have two FABs", fabNoteEdit);
        assertNotNull("we have two FABs", fabAttachPicture);

        fabNoteEdit.performClick();

        ShadowLooper.idleMainLooper(100, TimeUnit.MILLISECONDS);
        assertEquals(ShadowToast.getTextOfLatestToast().toString(), "To perform this action it is necessary to select an activity first");

        fabAttachPicture.performClick();

        ShadowLooper.idleMainLooper(100, TimeUnit.MILLISECONDS);
        assertEquals(ShadowToast.getTextOfLatestToast().toString(), "To perform this action it is necessary to select an activity first");

    }

    @Test
    public void currentActivity() throws Exception {
        /* now select an activity */
        DiaryActivity someAct = new DiaryActivity(1, "Test", Color.BLACK);

        ActivityHelper.helper.insertActivity(someAct);
        assertNotNull(someAct);

        ActivityHelper.helper.setCurrentActivity(someAct);
        assertEquals(ActivityHelper.helper.getCurrentActivity(), someAct);

        MainActivity activity = Robolectric.setupActivity(MainActivity.class);

        View card = activity.findViewById(R.id.card);
        TextView nameView = (TextView) card.findViewById(R.id.activity_name);
        assertNotNull("Current activity Text available", nameView);

        assertEquals(nameView.getText(), "Test");

        FloatingActionButton fabNoteEdit = (FloatingActionButton) activity.findViewById(R.id.fab_edit_note);
        FloatingActionButton fabAttachPicture = (FloatingActionButton) activity.findViewById(R.id.fab_attach_picture);

        assertNotNull("we have two FABs", fabNoteEdit);
        assertNotNull("we have two FABs", fabAttachPicture);

        fabNoteEdit.performClick();

        DialogFragment dialogFragment = (DialogFragment) activity.getSupportFragmentManager()
                .findFragmentByTag("NoteEditDialogFragment");
        assertNotNull(dialogFragment);

        ShadowLooper.idleMainLooper(100, TimeUnit.MILLISECONDS);
        assertNull(ShadowToast.getTextOfLatestToast());

        fabAttachPicture.performClick();

        ShadowLooper.idleMainLooper(100, TimeUnit.MILLISECONDS);
        assertNull(ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void useAppContext() throws Exception {
        // Context of the app under test.
        Context appContext = activity;

        assertEquals("de.rampro.activitydiary", appContext.getPackageName());
    }

}

