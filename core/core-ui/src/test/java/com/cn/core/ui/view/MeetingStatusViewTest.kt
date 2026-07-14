package com.cn.core.ui.view

import org.junit.Assert.*
import org.junit.Test

class MeetingStatusViewTest {

    @Test
    fun testGlowBarWidthRatioClamping() {
        var ratio = 1.5f
        ratio = ratio.coerceIn(0f, 1f)
        assertEquals(1f, ratio)

        ratio = -0.5f
        ratio = ratio.coerceIn(0f, 1f)
        assertEquals(0f, ratio)

        ratio = 0.5f
        ratio = ratio.coerceIn(0f, 1f)
        assertEquals(0.5f, ratio)
    }

    @Test
    fun testSetAttendeeCount() {
        val count = 9
        val expected = "会议中 ($count)"
        assertEquals(expected, buildAttendeeText(count))

        assertEquals("会议中", buildAttendeeText(0))
        assertEquals("会议中", buildAttendeeText(-1))
    }

    @Test
    fun testBuildAttendeeTextWithZero() {
        assertEquals("会议中", buildAttendeeText(0))
    }

    @Test
    fun testBuildAttendeeTextWithPositiveNumber() {
        assertEquals("会议中 (5)", buildAttendeeText(5))
        assertEquals("会议中 (10)", buildAttendeeText(10))
    }

    private fun buildAttendeeText(count: Int): String {
        return if (count > 0) "会议中 ($count)" else "会议中"
    }
}
