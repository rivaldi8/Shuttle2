package com.simplecityapps.shuttle

import kotlin.test.Test
import kotlin.test.assertTrue

class CommonGreetingTest {

    @Test
    fun testExample() {
        assertTrue(ViewModel().greeting().contains("Hello"), "Check 'Hello' is mentioned")
    }
}