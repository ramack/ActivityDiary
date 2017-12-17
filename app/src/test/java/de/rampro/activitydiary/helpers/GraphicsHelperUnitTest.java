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

package de.rampro.activitydiary.helpers;

import android.content.Context;
import android.graphics.Color;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.io.File;

import de.rampro.activitydiary.ActivityDiaryApplication;
import de.rampro.activitydiary.helpers.GraphicsHelper;
import de.rampro.activitydiary.ui.main.MainActivity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(RobolectricTestRunner.class)
public class GraphicsHelperUnitTest {
    private MainActivity activity;

    @Test
    public void colorDistance() throws Exception {
        assertEquals(0, GraphicsHelper.colorDistance(0,0));
        assertEquals(0, GraphicsHelper.colorDistance(Color.BLUE,Color.BLUE));
        assertEquals(0, GraphicsHelper.colorDistance(Color.BLACK,Color.BLACK));
        assertEquals(0, GraphicsHelper.colorDistance(0xffffffff, 0xffffffff));
        assertEquals(0, GraphicsHelper.colorDistance(17,17));
        assertEquals(10, GraphicsHelper.colorDistance(0xff0000,0xf60000),7);
    }

    @Before
    public void setUp() throws Exception
    {
        activity = Robolectric.buildActivity( MainActivity.class )
                .create()
                .resume()
                .get();
    }

    @Test
    public void shouldNotBeNull() throws Exception
    {
        assertNotNull( activity );
    }

    @Test
    public void imageStorageDirectory(){
        Context appCtx = ActivityDiaryApplication.getAppContext();
        assertNotNull(appCtx);

        File f = GraphicsHelper.imageStorageDirectory();
        String p = f.getAbsolutePath();
        assertNotNull(p);
        assertFalse(p.isEmpty());
    }
}
