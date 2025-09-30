package model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class Language(val code: String, val displayName: String) {
    ZH("zh", "中文"),
    EN("en", "EN")
}

object StringsManager {
    private var currentLanguage: Language = Language.ZH

    private val _strings = MutableStateFlow<Strings>(ZhStrings())
    val strings: StateFlow<Strings> = _strings.asStateFlow()

    private val _language = MutableStateFlow(Language.ZH)
    val language: StateFlow<Language> = _language.asStateFlow()

    fun setLanguage(language: Language) {
        currentLanguage = language
        _language.value = language
        _strings.value = when (language) {
            Language.ZH -> ZhStrings()
            Language.EN -> EnStrings()
        }
    }

    fun init() {
        val systemLanguage = Locale.getDefault().language
        setLanguage(if (systemLanguage == "zh") Language.ZH else Language.EN)
    }

    fun getCurrentLanguage(): Language = currentLanguage

    fun toggleLanguage() {
        setLanguage(if (currentLanguage == Language.ZH) Language.EN else Language.ZH)
    }
}
