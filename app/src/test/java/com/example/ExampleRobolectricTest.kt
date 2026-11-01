package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SeedData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
        assertEquals("PartPilot", appName)
    }

    @Test
    fun `verify seed data contains 20 parts and 3 locations`() {
        assertEquals(20, SeedData.parts.size)
        assertEquals(3, SeedData.locations.size)
        assertEquals(2, SeedData.projects.size)
        assertTrue(SeedData.parts.any { it.name.contains("ESP32") })
        assertTrue(SeedData.parts.any { it.name.contains("MPU6050") })
        assertTrue(SeedData.parts.any { it.name.contains("L298N") })
    }
}
