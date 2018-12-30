/*
 * ActivityDiary
 *
 * Copyright (C) 2018 Raphael Mack http://www.raphael-mack.de
 * Copyright (C) 2018 Sam Partee
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

import org.junit.Test;

import de.rampro.activitydiary.helpers.JaroWinkler;

public class JaroWinklerTest {
    JaroWinkler jw = new JaroWinkler(.7);

    // .8 is used at the threshold for showing user similar name dialog

    @Test
    public void testCapitalization() {
        String s = "Sleeping";
        String s2 = "sleeping";
        assert(jw.similarity(s,s2) > .8);
    }

    @Test
    public void testShort() {
        String s = "sl";
        String s2 = "s";
        assert(jw.similarity(s,s2) > .8);
    }

    @Test
    public void testDash() {
        String s = "Sleeping-well";
        String s2 = "Sleeping";
        assert(jw.similarity(s,s2) > .8);
    }

    @Test
    public void testNumbers() {
        String s = "Sleeping1";
        String s2 = "Sleeping2";
        assert(jw.similarity(s,s2) > .8);
    }

    @Test
    public void testContained() {
        String s = "onlaptopwork";
        String s2 = "laptop";
        assert(jw.similarity(s,s2) < .8);
    }

    @Test
    public void testIdentical() {
        String s = "sleeping";
        String s2 = "sleeping";
        assert(jw.similarity(s,s2) == 1.0);
    }
}
