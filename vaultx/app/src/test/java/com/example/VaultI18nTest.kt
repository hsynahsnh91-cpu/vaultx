package com.example

import androidx.compose.ui.unit.LayoutDirection
import com.example.crypto.StrengthLevel
import com.example.i18n.VaultI18n
import org.junit.Assert.*
import org.junit.Test
import kotlin.reflect.full.memberProperties

class VaultI18nTest {

    @Test
    fun testLanguageSelection_English() {
        val (strings, layoutDirection) = VaultI18n.forLanguage("en")
        assertEquals(LayoutDirection.Ltr, layoutDirection)
        assertEquals("VaultX", strings.appName)
        assertEquals("Vault is Locked", strings.vaultLocked)
        assertEquals("Unlock Vault", strings.unlock)
        assertEquals("Dashboard", strings.dashboard)
        assertEquals("Settings", strings.settings)
        assertEquals("Files", strings.files)
        assertEquals("Notes", strings.notes)
        assertEquals("Passwords", strings.passwords)
        assertEquals("Records", strings.records)
    }

    @Test
    fun testLanguageSelection_Arabic() {
        val (strings, layoutDirection) = VaultI18n.forLanguage("ar")
        assertEquals(LayoutDirection.Rtl, layoutDirection)
        assertEquals("VaultX", strings.appName)
        assertEquals("الخزنة مقفلة", strings.vaultLocked)
        assertEquals("فتح الخزنة", strings.unlock)
        assertEquals("لوحة التحكم", strings.dashboard)
        assertEquals("الإعدادات", strings.settings)
        assertEquals("الملفات", strings.files)
        assertEquals("الملاحظات", strings.notes)
        assertEquals("كلمات المرور", strings.passwords)
        assertEquals("السجلات", strings.records)
    }

    @Test
    fun testLanguageSelection_CaseInsensitiveAndFallback() {
        val (stringsEnUpper, dirEnUpper) = VaultI18n.forLanguage("EN")
        assertEquals(LayoutDirection.Ltr, dirEnUpper)
        assertEquals("Vault is Locked", stringsEnUpper.vaultLocked)

        val (stringsArUpper, dirArUpper) = VaultI18n.forLanguage("AR")
        assertEquals(LayoutDirection.Rtl, dirArUpper)
        assertEquals("الخزنة مقفلة", stringsArUpper.vaultLocked)

        // Fallback for unknown language
        val (stringsFallback, dirFallback) = VaultI18n.forLanguage("fr")
        assertEquals(LayoutDirection.Ltr, dirFallback)
        assertEquals("Vault is Locked", stringsFallback.vaultLocked)
    }

    @Test
    fun testStringDictionaries_NoEmptyStrings() {
        val english = VaultI18n.English
        val arabic = VaultI18n.Arabic

        // Test English properties
        val enProperties = english::class.memberProperties
        for (prop in enProperties) {
            val value = prop.getter.call(english)
            if (value is String) {
                assertFalse("English property ${prop.name} should not be blank", value.isBlank())
            }
        }

        // Test Arabic properties
        val arProperties = arabic::class.memberProperties
        for (prop in arProperties) {
            val value = prop.getter.call(arabic)
            if (value is String) {
                assertFalse("Arabic property ${prop.name} should not be blank", value.isBlank())
            }
        }
    }

    @Test
    fun testFormatBytes_EnglishAndArabic() {
        // Bytes
        val bEn = VaultI18n.formatBytes(512L, "en")
        val bAr = VaultI18n.formatBytes(512L, "ar")
        assertTrue(bEn.contains("B"))
        assertTrue(bAr.contains("بايت"))

        // Kilobytes
        val kbEn = VaultI18n.formatBytes(2048L, "en")
        val kbAr = VaultI18n.formatBytes(2048L, "ar")
        assertTrue(kbEn.contains("KB"))
        assertTrue(kbAr.contains("ك.ب"))

        // Megabytes
        val mbEn = VaultI18n.formatBytes(5 * 1024 * 1024L, "en")
        val mbAr = VaultI18n.formatBytes(5 * 1024 * 1024L, "ar")
        assertTrue(mbEn.contains("MB"))
        assertTrue(mbAr.contains("م.ب"))

        // Gigabytes
        val gbEn = VaultI18n.formatBytes(2L * 1024 * 1024 * 1024L, "en")
        val gbAr = VaultI18n.formatBytes(2L * 1024 * 1024 * 1024L, "ar")
        assertTrue(gbEn.contains("GB"))
        assertTrue(gbAr.contains("ج.ب"))
    }

    @Test
    fun testDateAndDateTimeFormatting() {
        val now = 1711500000000L // arbitrary fixed timestamp
        val dateEn = VaultI18n.formatDate(now, "en")
        val dateAr = VaultI18n.formatDate(now, "ar")
        assertNotNull(dateEn)
        assertNotNull(dateAr)
        assertFalse(dateEn.isBlank())
        assertFalse(dateAr.isBlank())

        val dateTimeEn = VaultI18n.formatDateTime(now, "en")
        val dateTimeAr = VaultI18n.formatDateTime(now, "ar")
        assertNotNull(dateTimeEn)
        assertNotNull(dateTimeAr)
        assertFalse(dateTimeEn.isBlank())
        assertFalse(dateTimeAr.isBlank())
    }

    @Test
    fun testNumberAndPercentageFormatting() {
        val numEn = VaultI18n.formatNumber(1234, "en")
        val numAr = VaultI18n.formatNumber(1234, "ar")
        assertNotNull(numEn)
        assertNotNull(numAr)

        val pctEn = VaultI18n.formatPercentage(85, "en")
        val pctAr = VaultI18n.formatPercentage(85, "ar")
        assertTrue(pctEn.contains("%"))
        assertTrue(pctAr.contains("%"))
    }

    @Test
    fun testFormatDuration() {
        val durEn = VaultI18n.formatDuration(3, 45, "en")
        val durAr = VaultI18n.formatDuration(3, 45, "ar")
        assertEquals("03:45", durEn)
        assertNotNull(durAr)
    }

    @Test
    fun testDynamicFormatters_EnglishAndArabic() {
        val en = VaultI18n.English
        val ar = VaultI18n.Arabic

        assertEquals("5 minutes", en.timeoutMinutes(5))
        assertEquals("5 دقائق", ar.timeoutMinutes(5))

        assertEquals("1 minute", en.timeoutMinutes(1))
        assertEquals("دقيقة واحدة", ar.timeoutMinutes(1))

        assertEquals("10 items recorded", en.auditLogsRecordedCount(10))
        assertEquals("تم تسجيل 10 أحداث أمان وتشفير", ar.auditLogsRecordedCount(10))

        assertEquals("Strong (54 bits entropy)", en.strengthWithEntropy(en.strengthStrong, 54))
        assertEquals("قوي (54 بت إنتروبيا)", ar.strengthWithEntropy(ar.strengthStrong, 54))
    }

    @Test
    fun testPasswordStrengthLabels_Bilingual() {
        val en = VaultI18n.English
        val ar = VaultI18n.Arabic

        assertEquals("Very Weak", en.strengthVeryWeak)
        assertEquals("ضعيف جداً", ar.strengthVeryWeak)

        assertEquals("Weak", en.strengthWeak)
        assertEquals("ضعيف", ar.strengthWeak)

        assertEquals("Fair", en.strengthFair)
        assertEquals("مقبول", ar.strengthFair)

        assertEquals("Strong", en.strengthStrong)
        assertEquals("قوي", ar.strengthStrong)

        assertEquals("Very Strong", en.strengthVeryStrong)
        assertEquals("قوي جداً", ar.strengthVeryStrong)
    }

    @Test
    fun testSecurityAndRecoveryBilingualParity() {
        val en = VaultI18n.English
        val ar = VaultI18n.Arabic

        assertEquals("Security", en.securityCenter)
        assertEquals("مركز الأمان", ar.securityCenter)

        assertEquals("Privacy", en.privacyCenter)
        assertEquals("مركز الخصوصية", ar.privacyCenter)

        assertEquals("Emergency Recovery Kit", en.recoveryKit)
        assertEquals("حقيبة استعادة الطوارئ", ar.recoveryKit)

        assertEquals("Wipe All Vault Data", en.wipeVault)
        assertEquals("مسح جميع بيانات الخزنة", ar.wipeVault)

        assertEquals("Privacy Mode", en.privacyMode)
        assertEquals("وضع الخصوصية", ar.privacyMode)
    }
}
