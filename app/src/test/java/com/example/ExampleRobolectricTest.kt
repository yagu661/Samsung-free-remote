package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.TvPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Samsung AI Remote", appName)
  }

  @Test
  fun `verify preferences persistence`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = TvPreferences(context)
    prefs.tvIp = "192.168.1.45"
    prefs.tvToken = "test_token_123"
    assertEquals("192.168.1.45", prefs.tvIp)
    assertEquals("test_token_123", prefs.tvToken)
  }
}
