package com.example.donation

import android.content.Context
import android.content.SharedPreferences

object DonationManager {

    const val UPI_ID = "sam.world0099@okhdfcbank"
    const val RECIPIENT_NAME = "Sam"
    const val SUPPORT_EMAIL = "sam.world0099@gmail.com"

    private const val PREFS_NAME = "gmk_donation_prefs"
    private const val KEY_LAST_SHOWN = "last_popup_shown_time"
    private const val KEY_INSTALL_TIME = "app_install_time"
    private const val KEY_DONATION_COUNT = "donation_popup_count"

    // 2.5 days (60 hours) interval ~ 2 to 3 times per week
    private const val MIN_INTERVAL_MS = 60 * 60 * 1000L * 2L + 12 * 60 * 60 * 1000L // 60 hours

    // Minimum delay before showing popup for the first time (5 minutes after install)
    private const val FIRST_LAUNCH_DELAY_MS = 5 * 60 * 1000L

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Checks if the donation popup should be displayed based on frequency rules
     * (2-3 times per week, not on first minute of install).
     */
    fun shouldShowDonationPopup(context: Context): Boolean {
        val prefs = getPrefs(context)
        val now = System.currentTimeMillis()

        var installTime = prefs.getLong(KEY_INSTALL_TIME, 0L)
        if (installTime == 0L) {
            installTime = now
            prefs.edit().putLong(KEY_INSTALL_TIME, installTime).apply()
            return false // Do not show on fresh install / first launch
        }

        // Must be installed for at least 5 minutes
        if (now - installTime < FIRST_LAUNCH_DELAY_MS) {
            return false
        }

        val lastShown = prefs.getLong(KEY_LAST_SHOWN, 0L)
        if (lastShown == 0L) {
            // First time showing popup after initial delay
            return true
        }

        // Show approximately 2-3 times per week (every ~60 hours)
        return (now - lastShown) >= MIN_INTERVAL_MS
    }

    /**
     * Records that the donation popup was displayed to the user.
     */
    fun markPopupShown(context: Context) {
        val prefs = getPrefs(context)
        val count = prefs.getInt(KEY_DONATION_COUNT, 0)
        prefs.edit()
            .putLong(KEY_LAST_SHOWN, System.currentTimeMillis())
            .putInt(KEY_DONATION_COUNT, count + 1)
            .apply()
    }

    /**
     * Records dismissal ("Maybe Later" or "X").
     */
    fun markPopupDismissed(context: Context) {
        markPopupShown(context)
    }
}
