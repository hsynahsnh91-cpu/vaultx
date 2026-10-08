package com.example.crypto

import kotlin.math.log2
import kotlin.math.pow

import com.example.i18n.VaultStrings

enum class PasswordStrengthSuggestion {
    CANNOT_BE_EMPTY,
    AT_LEAST_12_CHARS,
    ADD_LOWERCASE,
    ADD_UPPERCASE,
    ADD_DIGITS,
    ADD_SYMBOLS;

    fun localized(strings: VaultStrings): String = when (this) {
        CANNOT_BE_EMPTY -> strings.tipPasswordCannotBeEmpty
        AT_LEAST_12_CHARS -> strings.tipAtLeast12Chars
        ADD_LOWERCASE -> strings.tipAddLowercase
        ADD_UPPERCASE -> strings.tipAddUppercase
        ADD_DIGITS -> strings.tipAddDigits
        ADD_SYMBOLS -> strings.tipAddSymbols
    }
}

enum class StrengthLevel(val score: Int, val label: String) {
    VERY_WEAK(0, "Very Weak"),
    WEAK(1, "Weak"),
    FAIR(2, "Fair"),
    STRONG(3, "Strong"),
    VERY_STRONG(4, "Very Strong");

    fun localized(strings: VaultStrings): String = when (this) {
        VERY_WEAK -> strings.strengthVeryWeak
        WEAK -> strings.strengthWeak
        FAIR -> strings.strengthFair
        STRONG -> strings.strengthStrong
        VERY_STRONG -> strings.strengthVeryStrong
    }
}

data class PasswordStrengthResult(
    val level: StrengthLevel,
    val score: Int, // 0 to 4
    val entropyBits: Int,
    val suggestions: List<String>,
    val structuredSuggestions: List<PasswordStrengthSuggestion> = emptyList()
)

object PasswordStrength {

    fun evaluate(password: String): PasswordStrengthResult {
        if (password.isEmpty()) {
            return PasswordStrengthResult(
                level = StrengthLevel.VERY_WEAK,
                score = 0,
                entropyBits = 0,
                suggestions = listOf("Password cannot be empty"),
                structuredSuggestions = listOf(PasswordStrengthSuggestion.CANNOT_BE_EMPTY)
            )
        }

        var poolSize = 0
        var hasLower = false
        var hasUpper = false
        var hasDigit = false
        var hasSymbol = false

        for (c in password) {
            when {
                c.isLowerCase() -> hasLower = true
                c.isUpperCase() -> hasUpper = true
                c.isDigit() -> hasDigit = true
                else -> hasSymbol = true
            }
        }

        if (hasLower) poolSize += 26
        if (hasUpper) poolSize += 26
        if (hasDigit) poolSize += 10
        if (hasSymbol) poolSize += 33

        val entropy = if (poolSize > 0) {
            (password.length * log2(poolSize.toDouble())).toInt()
        } else {
            0
        }

        val suggestions = mutableListOf<String>()
        val structured = mutableListOf<PasswordStrengthSuggestion>()
        if (password.length < 12) {
            suggestions.add("Use at least 12 characters")
            structured.add(PasswordStrengthSuggestion.AT_LEAST_12_CHARS)
        }
        if (!hasLower) {
            suggestions.add("Add lowercase letters")
            structured.add(PasswordStrengthSuggestion.ADD_LOWERCASE)
        }
        if (!hasUpper) {
            suggestions.add("Add uppercase letters")
            structured.add(PasswordStrengthSuggestion.ADD_UPPERCASE)
        }
        if (!hasDigit) {
            suggestions.add("Add numeric digits")
            structured.add(PasswordStrengthSuggestion.ADD_DIGITS)
        }
        if (!hasSymbol) {
            suggestions.add("Add special symbols")
            structured.add(PasswordStrengthSuggestion.ADD_SYMBOLS)
        }

        // Score based on entropy and length
        val score = when {
            entropy < 28 || password.length < 8 -> 0
            entropy < 45 || password.length < 10 -> 1
            entropy < 60 || password.length < 12 -> 2
            entropy < 80 || password.length < 14 -> 3
            else -> 4
        }

        val level = when (score) {
            0 -> StrengthLevel.VERY_WEAK
            1 -> StrengthLevel.WEAK
            2 -> StrengthLevel.FAIR
            3 -> StrengthLevel.STRONG
            else -> StrengthLevel.VERY_STRONG
        }

        return PasswordStrengthResult(
            level = level,
            score = score,
            entropyBits = entropy,
            suggestions = suggestions,
            structuredSuggestions = structured
        )
    }
}
