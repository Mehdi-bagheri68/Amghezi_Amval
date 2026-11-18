package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.ExcelHelper
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("اموال‌گردانی فروشگاه", appName)
    }

    @Test
    fun `excel helper generates and parses xlsx template`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val fileResult = ExcelHelper.generateSampleTemplateXlsx(context, "شعبه مرکزی")
        assertTrue(fileResult.isSuccess)
        val file = fileResult.getOrThrow()
        val parsedRows = ExcelHelper.parseXlsxBytes(file.readBytes())
        assertEquals(6, parsedRows.size)
        assertEquals("شرح کارت دارایی", parsedRows[0][0])
        assertEquals("IT-MONITOR", parsedRows[1][4])
    }
}
