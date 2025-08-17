package com.nikhildev.mtranslate

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.google.mlkit.nl.translate.TranslateLanguage

class ShowTranslationsViewModel(application: Application) : AndroidViewModel(application) {
    val sourceLang = MutableLiveData<Language>()
    val targetLang = MutableLiveData<Language>()

    val availableLanguages: List<Language> = TranslateLanguage.getAllLanguages().map { Language(it) }


}