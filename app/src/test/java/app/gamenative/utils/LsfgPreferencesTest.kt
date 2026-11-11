package app.gamenative.utils

import android.app.Application
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class LsfgPreferencesTest {
    private val context: Application get() = RuntimeEnvironment.getApplication()

    @Before fun reset() {
        context.getSharedPreferences("wow_lsfg", 0).edit().clear().commit()
    }

    @Test fun startsOffUntilChosen() {
        assertEquals(0, LsfgPreferences.multiplier(context, "FOREVER"))
    }

    @Test fun savesProductsIndependentlyAndRetainsOff() {
        LsfgPreferences.save(context, "FOREVER", 2)
        LsfgPreferences.save(context, "RETAIL", 4)
        assertEquals(2, LsfgPreferences.multiplier(context, "FOREVER"))
        assertEquals(4, LsfgPreferences.multiplier(context, "RETAIL"))
        LsfgPreferences.save(context, "FOREVER", 0)
        assertEquals(0, LsfgPreferences.multiplier(context, "FOREVER"))
        assertEquals(4, LsfgPreferences.multiplier(context, "RETAIL"))
    }
}
