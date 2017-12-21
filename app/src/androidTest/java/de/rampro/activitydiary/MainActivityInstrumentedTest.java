package de.rampro.activitydiary;

import android.content.Context;
import android.view.Menu;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import de.rampro.activitydiary.ui.main.MainActivity;

import static org.robolectric.Shadows.shadowOf;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@Config(constants = BuildConfig.class)
@RunWith(RobolectricTestRunner.class)
//@RunWith(AndroidJUnit4.class)
public class MainActivityInstrumentedTest {
    private MainActivity activity;

    @Before
    public void setUp() throws Exception
    {
/*        activity = Robolectric.buildActivity( MainActivity.class )
                .create()
                .resume()
                .get();*/
    }

    @Test
    public void useAppContext() throws Exception {
        // Context of the app under test.
/*        Context appContext = InstrumentationRegistry.getTargetContext();

        assertEquals("de.rampro.activitydiary.debug", appContext.getPackageName());*/
    }

    @Test
    public void createMainActivity() throws Exception {
        final MainActivity activity = Robolectric.setupActivity(MainActivity.class);

/*        final Menu menu = shadowOf(activity).getOptionsMenu();
        assertNotNull(activity);

        assertTrue(menu.findItem(R.id.action_add_activity).isEnabled());*/

    }
}
