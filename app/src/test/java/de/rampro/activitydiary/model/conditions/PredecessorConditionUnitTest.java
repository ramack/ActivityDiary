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

package de.rampro.activitydiary.model.conditions;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.shadows.ShadowSystemClock;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import de.rampro.activitydiary.ActivityDiaryApplication;
import de.rampro.activitydiary.helpers.ActivityHelper;
import de.rampro.activitydiary.model.DiaryActivity;

import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(application = ActivityDiaryApplication.class,
        instrumentedPackages={ "de.rampro.activitydiary.helpers" })
public class PredecessorConditionUnitTest {
    ActivityHelper helper;

    private void cleanActivities(){
        List<DiaryActivity> l = new LinkedList<>(helper.getActivities());

        for (DiaryActivity d: l) {
            helper.deleteActivity(d);
        }
    }

    @Test
    public void sorting() throws Exception {
        PredecessorCondition pc = new PredecessorCondition(helper);

        cleanActivities();

        List<Condition.Likelihood> list = pc.likelihoods();
        assertTrue("Empty List if not activity", list.isEmpty());

        DiaryActivity a1 = new DiaryActivity(1, "AAA", 13);
        DiaryActivity a2 = new DiaryActivity(2, "ZZZ", 14);
        DiaryActivity a3 = new DiaryActivity(3, "MMM", 15);

        helper.insertActivity(a1);
        helper.insertActivity(a2);
        helper.insertActivity(a3);
        long now = System.currentTimeMillis();
        Thread.sleep(4000);
//        ShadowSystemClock.setCurrentTimeMillis(now - 1000 * 60 * 5);
        helper.setCurrentActivity(a1);
        Thread.sleep(4000);
//        ShadowSystemClock.setCurrentTimeMillis(now - 1000 * 60 * 4);
        helper.setCurrentActivity(a2);
        Thread.sleep(4000);
//        ShadowSystemClock.setCurrentTimeMillis(now - 1000 * 60 * 3);
        helper.setCurrentActivity(a1);
        Thread.sleep(4000);
//        ShadowSystemClock.setCurrentTimeMillis(now - 1000 * 60 * 2);
        helper.setCurrentActivity(a3);
        Thread.sleep(4000);
//        ShadowSystemClock.setCurrentTimeMillis(now - 1000 * 60 * 1);
        helper.setCurrentActivity(a2);
        Thread.sleep(4000);
        ShadowLooper.runUiThreadTasks();
        Robolectric.flushForegroundThreadScheduler();
        Robolectric.flushBackgroundThreadScheduler();
        pc.refresh();
        boolean finished = false;
        while(!finished){
            Thread.sleep(100);
            if(!pc.isActive()){
                finished = true;
            }
        }

        Robolectric.flushForegroundThreadScheduler();
        Robolectric.flushBackgroundThreadScheduler();
        ShadowLooper.runUiThreadTasks();

        Robolectric.flushForegroundThreadScheduler();
        Robolectric.flushBackgroundThreadScheduler();
        ShadowLooper.runUiThreadTasks();

        Robolectric.flushForegroundThreadScheduler();
        Robolectric.flushBackgroundThreadScheduler();
        ShadowLooper.runUiThreadTasks();

        Robolectric.flushForegroundThreadScheduler();
        Robolectric.flushBackgroundThreadScheduler();
        ShadowLooper.runUiThreadTasks();

        list = pc.likelihoods();

        /* TODO: this is currently not easily testable, due to robolectrics
         * handling of multi-threading.
         * For now those tests are disabled. */
//        assertTrue("all activites are in likelihood list", list.size() == 3);

        /* TODO: this test needs to be adapted...
        * the diary is not written and read, and also the following conditions are valid only for the alphabetical Condition */
        Collections.sort(list, new Comparator<Condition.Likelihood>() {
                    @Override
                    public int compare(Condition.Likelihood o1, Condition.Likelihood o2) {
                        return Double.compare(o1.likelihood, o2.likelihood);
                    }
                });

        if(!list.isEmpty()) {
            Condition.Likelihood last = list.get(0);
            for (Condition.Likelihood l : list) {
                assertTrue("last is before", last.likelihood <= l.likelihood);
                assertTrue("last has a bigger text", last.activity.getName().compareTo(l.activity.getName()) >= 0);
                last = l;
            }
        }
    }

    @Before
    public void setUp() throws Exception
    {
        helper = ActivityHelper.helper;
    }

}
