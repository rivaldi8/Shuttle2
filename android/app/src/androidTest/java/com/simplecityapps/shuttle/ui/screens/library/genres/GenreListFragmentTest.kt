package com.simplecityapps.shuttle.ui.screens.library.genres

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.navigation.Navigation.findNavController
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.simplecityapps.shuttle.R
import com.simplecityapps.shuttle.ui.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

@HiltAndroidTest
class GenreListFragmentTest {
    private val hiltRule = HiltAndroidRule(this)

    private val composeTestRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rule: RuleChain = RuleChain
        .outerRule(hiltRule)
        .around(composeTestRule)

    @get:Rule
    val composeTestRule2 = createComposeRule()

    @Before
    fun goToNonOrganizerFragment() {
        composeTestRule.activityRule.scenario.onActivity {
            // FIXME: I guess there's a better way to do this
            it.preferenceManager.hasSeenThankYouDialog = true
            it.preferenceManager.showChangelogOnLaunch = false

            findNavController(it, R.id.onboardingNavHostFragment)
                .navigate(R.id.mainFragment)
        }
    }

    @Test
    fun testEventFragment2() {
        onView(withText("Genres")).perform(click())
        composeTestRule.onNodeWithTag("genres-list-lazy-column")
            .assertIsDisplayed()
    }
}
