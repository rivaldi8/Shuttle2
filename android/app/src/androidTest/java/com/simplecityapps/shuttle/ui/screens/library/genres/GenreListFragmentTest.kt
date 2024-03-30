package com.simplecityapps.shuttle.ui.screens.library.genres

import android.view.View
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.fragment.app.testing.withFragment
import androidx.navigation.Navigation
import androidx.navigation.Navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.simplecityapps.shuttle.R
import com.simplecityapps.shuttle.ui.MainActivity
import com.simplecityapps.shuttle.ui.common.view.multisheet.CustomBottomSheetBehavior
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

// @RunWith(AndroidJUnit4::class)
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
            findNavController(it, R.id.onboardingNavHostFragment)
                // .navigate(R.id.action_onboardingFragment_to_mainFragment)
                .navigate(R.id.mainFragment)
            // val navController = Navigation.findNavController(it, R.id.navHostFragment)
            // navController.navigate(R.id.libraryFragment)
            // it.preferenceManager.hasSeenThankYouDialog = true
            // val frag = it.supportFragmentManager.findFragmentById(R.id.mainFragment)
            /*
            it.runOnUiThread {
                onView(withId(R.id.bottomNavigationView)).perform(click())
            }
            */
            // frag?.findNavController()
            // val viewById: View = it.findViewById(R.id.mainFragment)
            // MatcherAssert.assertThat(viewById, CoreMatchers.notNullValue())
            // MatcherAssert.assertThat(viewById, CoreMatchers.instanceOf(CustomBottomSheetBehavior::class.java))
        }
        onView(withId(R.id.closeButton)).perform(click())
    }

    /*
    @Test
    fun testEventFragment() {
        // The "fragmentArgs" argument is optional.
        // val fragmentArgs = bundleOf(“selectedListItem” to 0)
        // val scenario = launchFragmentInContainer<EventFragment>(fragmentArgs)
        val scenario = launchFragmentInContainer<GenreListFragment>()
        onView(withId(R.id.refresh)).perform(click())
    }
    */

    @Test
    fun testEventFragment2() {
        onView(withId(R.id.recyclerView)).check(matches(isDisplayed()))
        // composeTestRule.onNodeWithText("Apple").assertIsDisplayed()
    }
}
