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

import android.graphics.Color;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class GraphicsHelperUnitTest {
    @Test
    public void colorDistance_equalColor() throws Exception {
        assertEquals(0, GraphicsHelper.colorDistance(0,0));
        assertEquals(0, GraphicsHelper.colorDistance(Color.BLUE,Color.BLUE));
        assertEquals(0, GraphicsHelper.colorDistance(Color.BLACK,Color.BLACK));
        assertEquals(0, GraphicsHelper.colorDistance(0xffffffff, 0xffffffff));
        assertEquals(0, GraphicsHelper.colorDistance(17,17));
        assertEquals(10, GraphicsHelper.colorDistance(0xff0000,0xf60000),7);
    }
}