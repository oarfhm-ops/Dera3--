package com.example

import com.example.ui.auth.PhoneNumberFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SmsVerificationTest {

    @Test
    fun toE164_normalizesEgyptianAndArabicDigits() {
        assertEquals("+201012345678", PhoneNumberFormatter.toE164("01012345678"))
        assertEquals("+201198765432", PhoneNumberFormatter.toE164("011-9876-5432"))
        assertEquals("+201512345678", PhoneNumberFormatter.toE164("٠١٥١٢٣٤٥٦٧٨"))
        assertEquals("+201212345678", PhoneNumberFormatter.toE164("201212345678"))
        assertEquals("+201012345678", PhoneNumberFormatter.toE164("00201012345678"))
    }

    @Test
    fun toE164_preservesValidInternationalAndRejectsInvalid() {
        assertEquals("+966501234567", PhoneNumberFormatter.toE164("+966 50 123 4567"))
        assertEquals("+16505551234", PhoneNumberFormatter.toE164("+1 (650) 555-1234"))
        assertNull(PhoneNumberFormatter.toE164("12345"))
        assertNull(PhoneNumberFormatter.toE164(""))
        assertNull(PhoneNumberFormatter.toE164("+0123456789"))
    }

    @Test
    fun normalizeOtpDigits_handlesArabicDigitsAndTruncation() {
        assertEquals("123456", PhoneNumberFormatter.normalizeOtpDigits("١٢٣٤٥٦"))
        assertEquals("482910", PhoneNumberFormatter.normalizeOtpDigits("48-29-1099"))
        assertEquals("907", PhoneNumberFormatter.normalizeOtpDigits("ab9c0-7"))
    }

    @Test
    fun extractOtpFromSms_parsesSixDigitCode() {
        assertEquals(
            "742819",
            PhoneNumberFormatter.extractOtpFromSms("Your DERA3 verification code is 742819.")
        )
        assertEquals(
            "593012",
            PhoneNumberFormatter.extractOtpFromSms("كود تفعيل DERA3 الخاص بك هو ٥٩٣٠١٢ لا تشاركه.")
        )
        assertNull(PhoneNumberFormatter.extractOtpFromSms("No code here 1234"))
    }

    @Test
    fun maskPhone_hidesMiddleDigits() {
        val masked = PhoneNumberFormatter.maskPhone("+201012345678")
        assertEquals("+2010••••••78", masked)
    }
}
