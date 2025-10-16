package com.roshni.games.core.utils.terms

/**
 * Methods by which users can accept terms and conditions
 */
enum class AcceptanceMethod {
    /**
     * User explicitly clicked "Accept" or "Agree" button
     */
    EXPLICIT_CLICK,

    /**
     * User continued using the app after being presented with terms
     */
    IMPLIED_CONTINUED_USE,

    /**
     * User scrolled through the entire document before accepting
     */
    SCROLL_TO_BOTTOM,

    /**
     * User provided digital signature or e-signature
     */
    DIGITAL_SIGNATURE,

    /**
     * User accepted via checkbox selection
     */
    CHECKBOX_SELECTION,

    /**
     * User accepted via voice command or biometric input
     */
    BIOMETRIC_CONFIRMATION,

    /**
     * User accepted via email confirmation link
     */
    EMAIL_CONFIRMATION,

    /**
     * User accepted via SMS verification code
     */
    SMS_VERIFICATION,

    /**
     * Parent/guardian accepted on behalf of child user
     */
    PARENTAL_CONSENT,

    /**
     * User accepted via OAuth or social login consent
     */
    OAUTH_CONSENT,

    /**
     * User accepted via API call or programmatic consent
     */
    PROGRAMMATIC_CONSENT
}