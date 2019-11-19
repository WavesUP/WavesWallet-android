/*
 * Created by Eduard Zaydel on 1/4/2019
 * Copyright © 2019 Waves Platform. All rights reserved.
 */

package com.wavesplatform.wallet.v2.data.model.local

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.wavesplatform.wallet.R
import java.util.*

enum class Language(
        @DrawableRes var image: Int,
        @StringRes var title: Int,
        var code: String,
        var iso: String,
        var oldCode: String = code
) {
    ENGLISH(R.drawable.ic_flag_18_britain, R.string.choose_language_english, "en", "EN"),
    RUSSIAN(R.drawable.ic_flag_18_rus, R.string.choose_language_russia, "ru", "RU"),
    KOREAN(R.drawable.ic_flag_18_korea, R.string.choose_language_korea, "ko", "KO"),
    CHINESE_SIMPLIFIED(R.drawable.ic_flag_18_china, R.string.choose_language_china, "zh_CN", "ZH"),
    TURKISH(R.drawable.ic_flag_18_turkey, R.string.choose_language_turkey, "tr", "TR"),
    DUTCH(R.drawable.ic_flag_18_nederland, R.string.choose_language_nederlands, "nl_NL", "NL", "nl"),
    HINDI(R.drawable.ic_flag_18_hindi, R.string.choose_language_hindi, "hi_IN", "HI", "hi"),
    SPANISH(R.drawable.ic_flag_18_spain, R.string.choose_language_spain, "es", "ES"),
    FRENCH(R.drawable.ic_flag_18_france, R.string.choose_language_french, "fr", "FR"),
    PORTUGUESE(R.drawable.ic_flag_18_portugal, R.string.choose_language_portuguese, "pt_PT", "PT", "pt"),
    BRAZILIAN(R.drawable.ic_flag_18_brazil, R.string.choose_language_brazilian, "pt_BR", "BR"),
    POLISH(R.drawable.ic_flag_18_polszczyzna, R.string.choose_language_polish, "pl", "PL"),
    ITALIAN(R.drawable.ic_flag_18_italiano, R.string.choose_language_italian, "it", "IT"),
    GERMAN(R.drawable.ic_flag_18_germany, R.string.choose_language_german, "de", "DE"),
    INDONESIAN(R.drawable.ic_flag_18_indonesia, R.string.choose_language_indonesian, "in", "ID"),
    JAPAN(R.drawable.ic_flag_18_japan, R.string.choose_language_japan, "ja", "JA"),
    // DANISH(R.drawable.ic_flag_18_danish, R.string.choose_language_danish, "dn", "dn"),
    ;

    companion object {

        fun getLanguagesItems(): ArrayList<LanguageItem> {
            return values().mapTo(ArrayList()) { LanguageItem(it, false) }
        }

        fun getLanguageItemByCode(code: String): LanguageItem {
            values().forEach {
                if (it.code == code || it.oldCode == code) {
                    return LanguageItem(it, false)
                }
            }
            return LanguageItem(ENGLISH, false)
        }

        fun getLanguageByCode(code: String): Language {
            return values().firstOrNull { it.code == code || it.oldCode == code } ?: ENGLISH
        }

        fun getLocale(code: String?): Locale {
            return if (code != null) {
                if (code.contains("_")) {
                    val locale = code.split("_")
                    Locale(locale[0], locale[1])
                } else {
                    Locale(code)
                }
            } else {
                Locale(ENGLISH.code)
            }
        }
    }
}