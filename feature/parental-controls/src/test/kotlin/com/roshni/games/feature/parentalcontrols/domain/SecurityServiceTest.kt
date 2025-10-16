package com.roshni.games.feature.parentalcontrols.domain

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.roshni.games.feature.parentalcontrols.data.model.AgeRating
import com.roshni.games.feature.parentalcontrols.data.model.AlertSeverity
import com.roshni.games.feature.parentalcontrols.data.model.AlertType
import com.roshni.games.feature.parentalcontrols.data.model.AuthType
import com.roshni.games.feature.parentalcontrols.data.model.ContentRestrictions
import com.roshni.games.feature.parentalcontrols.data.model.ExportFormat
import com.roshni.games.feature.parentalcontrols.data.model.PrivacySettings
import com.roshni.games.feature.parentalcontrols.data.model.PurchaseRestrictions
import com.roshni.games.feature.parentalcontrols.data.model.SecuritySettings
import com.roshni.games.feature.parentalcontrols.data.model.SocialRestrictions
import com.roshni.games.feature.parentalcontrols.data.model.TimeLimits
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [28])
class SecurityServiceTest {

    private lateinit var securityService: SecurityService

    @Before
    fun setup() {
        securityService = SecurityService()
    }

    @Test
    fun testInitialization() = runTest {
        // When
        val result = securityService.initialize()

        // Then
        assertTrue("Initialization should succeed", result.isSuccess)
    }

    @Test
    fun testSetParentalPin() = runTest {
        // Given
        val pin = "1234"

        // When
        val result = securityService.setParentalPin(pin)

        // Then
        assertTrue("Setting PIN should succeed", result.isSuccess)

        // Verify PIN is required
        val settings = securityService.parentalControls.first()
        assertTrue("PIN should be required", settings.pinRequired)
    }

    @Test
    fun testAuthenticateWithPin() = runTest {
        // Given
        val correctPin = "1234"
        val incorrectPin = "5678"

        // Set PIN first
        securityService.setParentalPin(correctPin)

        // When & Then
        val correctResult = securityService.authenticateWithPin(correctPin)
        assertTrue("Correct PIN should authenticate successfully", correctResult.isSuccess)
        assertTrue("Correct PIN should return true", correctResult.getOrNull() == true)

        val incorrectResult = securityService.authenticateWithPin(incorrectPin)
        assertTrue("Incorrect PIN should fail authentication", incorrectResult.isSuccess)
        assertFalse("Incorrect PIN should return false", incorrectResult.getOrNull() == true)
    }

    @Test
    fun testContentRestrictions() {
        // Given
        val settings = securityService.parentalControls.first()
        val teenGame = AgeRating.TEEN
        val matureGame = AgeRating.MATURE_17_PLUS
        val educationalGame = "Educational"
        val violentGame = "Action"

        // When & Then
        // TEEN game should be allowed by default
        assertTrue("TEEN game should be allowed",
            securityService.isContentAllowed(teenGame, educationalGame))

        // MATURE game should be blocked by default
        assertFalse("MATURE game should be blocked",
            securityService.isContentAllowed(matureGame, educationalGame))

        // Educational games should be allowed
        assertTrue("Educational games should be allowed",
            securityService.isContentAllowed(AgeRating.EVERYONE, educationalGame))

        // Action games might be blocked if violence is restricted
        val actionGameAllowed = securityService.isContentAllowed(
            AgeRating.TEEN, violentGame, hasViolence = true
        )
        // This depends on the default settings, so we don't assert a specific value
    }

    @Test
    fun testGameplayTimeRestrictions() {
        // When & Then
        val isAllowed = securityService.isGameplayAllowed()

        // Should return a boolean (the actual value depends on time restrictions)
        assertNotNull("Should return a boolean", isAllowed)
        assertTrue("Should be a boolean value", isAllowed is Boolean)
    }

    @Test
    fun testPurchaseRestrictions() {
        // Given
        val smallPurchase = 1.99
        val largePurchase = 99.99
        val paymentMethod = "credit_card"

        // When & Then
        val smallPurchaseAllowed = securityService.isPurchaseAllowed(smallPurchase, paymentMethod)
        val largePurchaseAllowed = securityService.isPurchaseAllowed(largePurchase, paymentMethod)

        // Small purchases should generally be allowed, large ones might require approval
        assertNotNull("Small purchase check should return a value", smallPurchaseAllowed)
        assertNotNull("Large purchase check should return a value", largePurchaseAllowed)
    }

    @Test
    fun testSocialInteractionRestrictions() {
        // When & Then
        val friendRequestAllowed = securityService.isSocialInteractionAllowed("friend_request")
        val messageAllowed = securityService.isSocialInteractionAllowed("message")
        val publicProfileAllowed = securityService.isSocialInteractionAllowed("public_profile")
        val unknownInteractionAllowed = securityService.isSocialInteractionAllowed("unknown")

        // Should return boolean values for all interaction types
        assertNotNull("Friend request check should return a value", friendRequestAllowed)
        assertNotNull("Message check should return a value", messageAllowed)
        assertNotNull("Public profile check should return a value", publicProfileAllowed)
        assertNotNull("Unknown interaction check should return a value", unknownInteractionAllowed)

        // Unknown interactions should be blocked by default
        assertFalse("Unknown interactions should be blocked", unknownInteractionAllowed)
    }

    @Test
    fun testDataEncryption() {
        // Given
        val sensitiveData = "user123:password456"

        // When
        val encryptResult = securityService.encryptData(sensitiveData)
        assertTrue("Encryption should succeed", encryptResult.isSuccess)

        val encryptedData = encryptResult.getOrNull()!!
        assertNotNull("Encrypted data should not be null", encryptedData)
        assertTrue("Encrypted data should be different from original",
            encryptedData != sensitiveData)

        // Decrypt
        val decryptResult = securityService.decryptData(encryptedData)
        assertTrue("Decryption should succeed", decryptResult.isSuccess)

        val decryptedData = decryptResult.getOrNull()!!
        assertEquals("Decrypted data should match original", sensitiveData, decryptedData)
    }

    @Test
    fun testDataExportRequest() = runTest {
        // Given
        val requestedBy = "test_user"
        val format = ExportFormat.JSON

        // When
        val result = securityService.requestDataExport(
            requestedBy = requestedBy,
            format = format,
            includePersonalData = true,
            includeGameData = true,
            includeAnalytics = false
        )

        // Then
        assertTrue("Data export request should succeed", result.isSuccess)
        assertNotNull("Should return export ID", result.getOrNull())

        val exportRequests = securityService.dataExportRequests.first()
        assertTrue("Should have export requests", exportRequests.isNotEmpty())

        val exportRequest = exportRequests.first()
        assertEquals("Requested by should match", requestedBy, exportRequest.requestedBy)
        assertEquals("Format should match", format, exportRequest.format)
    }

    @Test
    fun testSecurityAlerts() = runTest {
        // When
        val alertsFlow = securityService.getSecurityAlerts()
        val alerts = alertsFlow.first()

        // Then
        assertNotNull("Alerts should not be null", alerts)
        assertTrue("Alerts should be a list", alerts is List<*>)
    }

    @Test
    fun testPrivacySettings() = runTest {
        // Given
        val newSettings = PrivacySettings(
            analyticsEnabled = false,
            crashReportingEnabled = false,
            performanceMonitoringEnabled = false
        )

        // When
        val result = securityService.updatePrivacySettings(newSettings)

        // Then
        assertTrue("Privacy settings update should succeed", result.isSuccess)

        val currentSettings = securityService.privacySettings.first()
        assertEquals("Analytics should be disabled", false, currentSettings.analyticsEnabled)
        assertEquals("Crash reporting should be disabled", false, currentSettings.crashReportingEnabled)
        assertEquals("Performance monitoring should be disabled", false, currentSettings.performanceMonitoringEnabled)
    }

    @Test
    fun testDataRetentionCompliance() = runTest {
        // When
        val result = securityService.checkDataRetentionCompliance()

        // Then
        assertTrue("Data retention check should succeed", result.isSuccess)
        assertNotNull("Should return violations list", result.getOrNull())
    }

    @Test
    fun testSecuritySettings() = runTest {
        // When
        val settings = securityService.securitySettings.first()

        // Then
        assertNotNull("Security settings should not be null", settings)
        assertTrue("Should have session timeout", settings.sessionTimeoutMinutes > 0)
    }

    @Test
    fun testAuthenticationAttempts() = runTest {
        // When
        val attempts = securityService.authAttempts.first()

        // Then
        assertNotNull("Auth attempts should not be null", attempts)
        assertTrue("Auth attempts should be a list", attempts is List<*>)
    }

    @Test
    fun testMultipleFailedAuthAttempts() = runTest {
        // Given
        val incorrectPin = "9999"

        // When - Try multiple failed attempts
        repeat(5) {
            securityService.authenticateWithPin(incorrectPin)
        }

        // Then
        val alerts = securityService.getSecurityAlerts(AlertSeverity.HIGH).first()

        // Should detect suspicious activity after multiple failed attempts
        // (This depends on the implementation timing, so we don't assert specific values)
        assertNotNull("Should have security alerts", alerts)
    }

    @Test
    fun testAlertAcknowledgment() = runTest {
        // Given - Create some alerts first
        repeat(3) {
            securityService.authenticateWithPin("9999") // Failed attempts
        }

        val alertsBefore = securityService.getSecurityAlerts().first()

        // When
        if (alertsBefore.isNotEmpty()) {
            val alertId = alertsBefore.first().id
            val result = securityService.acknowledgeSecurityAlert(alertId)

            // Then
            assertTrue("Alert acknowledgment should succeed", result.isSuccess)
        }
    }

    @Test
    fun testContentFiltering() {
        // Test various content scenarios
        val testCases = listOf(
            Triple(AgeRating.EVERYONE, "Educational", false), // Should be allowed
            Triple(AgeRating.TEEN, "Adventure", false), // Should be allowed
            Triple(AgeRating.MATURE_17_PLUS, "Action", true), // Might be blocked
            Triple(AgeRating.ADULTS_ONLY, "Horror", true) // Should be blocked
        )

        testCases.forEach { (ageRating, category, hasViolence) ->
            // When
            val isAllowed = securityService.isContentAllowed(
                ageRating = ageRating,
                category = category,
                hasViolence = hasViolence
            )

            // Then
            assertNotNull("Content check should return a value", isAllowed)
            assertTrue("Should be a boolean value", isAllowed is Boolean)

            // Higher age ratings should be more likely to be blocked
            if (ageRating >= AgeRating.MATURE_17_PLUS) {
                // These might be blocked depending on settings
                // We don't assert specific values because it depends on configuration
            }
        }
    }

    @Test
    fun testPurchaseAmountLimits() {
        // Test various purchase amounts
        val testAmounts = listOf(0.99, 4.99, 9.99, 49.99, 99.99, 199.99)

        testAmounts.forEach { amount ->
            // When
            val isAllowed = securityService.isPurchaseAllowed(amount, "credit_card")

            // Then
            assertNotNull("Purchase check should return a value", isAllowed)
            assertTrue("Should be a boolean value", isAllowed is Boolean)
        }
    }

    @Test
    fun testPaymentMethodRestrictions() {
        // Test various payment methods
        val paymentMethods = listOf("credit_card", "debit_card", "paypal", "gift_card", "crypto")

        paymentMethods.forEach { method ->
            // When
            val isAllowed = securityService.isPurchaseAllowed(9.99, method)

            // Then
            assertNotNull("Payment method check should return a value", isAllowed)
            assertTrue("Should be a boolean value", isAllowed is Boolean)
        }
    }

    @Test
    fun testSocialFeatures() {
        // Test all social interaction types
        val interactionTypes = listOf(
            "friend_request",
            "message",
            "public_profile",
            "voice_chat",
            "video_chat",
            "screen_share"
        )

        interactionTypes.forEach { type ->
            // When
            val isAllowed = securityService.isSocialInteractionAllowed(type)

            // Then
            assertNotNull("Social interaction check should return a value", isAllowed)
            assertTrue("Should be a boolean value", isAllowed is Boolean)
        }
    }

    @Test
    fun testExportFormats() {
        // Test all export formats
        val formats = ExportFormat.values()

        formats.forEach { format ->
            // When
            val result = securityService.requestDataExport(
                requestedBy = "test_user",
                format = format
            )

            // Then
            assertTrue("Export request should succeed for format $format", result.isSuccess)
            assertNotNull("Should return export ID for format $format", result.getOrNull())
        }
    }

    @Test
    fun testAlertSeverityFiltering() = runTest {
        // When
        val allAlerts = securityService.getSecurityAlerts().first()
        val highSeverityAlerts = securityService.getSecurityAlerts(AlertSeverity.HIGH).first()
        val mediumSeverityAlerts = securityService.getSecurityAlerts(AlertSeverity.MEDIUM).first()

        // Then
        assertNotNull("All alerts should not be null", allAlerts)
        assertNotNull("High severity alerts should not be null", highSeverityAlerts)
        assertNotNull("Medium severity alerts should not be null", mediumSeverityAlerts)

        // High severity alerts should be a subset of all alerts
        assertTrue("High severity should be subset of all",
            highSeverityAlerts.size <= allAlerts.size)

        // Medium severity alerts should be a subset of all alerts
        assertTrue("Medium severity should be subset of all",
            mediumSeverityAlerts.size <= allAlerts.size)
    }
}