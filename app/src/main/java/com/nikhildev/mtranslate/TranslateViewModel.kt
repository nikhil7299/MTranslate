package com.nikhildev.mtranslate

import android.app.Application
import android.util.LruCache
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions

class TranslateViewModel(application: Application) : AndroidViewModel(application) {

  companion object {
    private const val NUM_TRANSLATORS = 3
  }

  private val modelManager: RemoteModelManager = RemoteModelManager.getInstance()
  private val pendingDownloads: HashMap<String, Task<Void>> = hashMapOf()
  private val translators =
    object : LruCache<TranslatorOptions, Translator>(NUM_TRANSLATORS) {
      override fun create(options: TranslatorOptions): Translator {
        return Translation.getClient(options)
      }
      override fun entryRemoved(
        evicted: Boolean,
        key: TranslatorOptions,
        oldValue: Translator,
        newValue: Translator?,
      ) {
        oldValue.close()
      }
    }
  val sourceLang = MutableLiveData<Language>()
  val targetLang = MutableLiveData<Language>()
  val sourceText = MutableLiveData<String>()
  val translatedText = MediatorLiveData<ResultOrError>()
  val availableModels = MutableLiveData<List<String>>()

  val availableLanguages: List<Language> = TranslateLanguage.getAllLanguages().map { Language(it) }


  init {
    // Create a translation result or error object.
//    Log.d("size",availableLanguages.size.toString())
//    Log.d("sizelang",availableLanguages.toString())
    val processTranslation =
      OnCompleteListener { task ->
        if (task.isSuccessful) {
          translatedText.value = ResultOrError(task.result, null)
        } else {
          translatedText.value = ResultOrError(null, task.exception)
        }
        fetchDownloadedModels()
      }
    translatedText.addSource(sourceText) { translate().addOnCompleteListener(processTranslation) }
    val languageObserver =
      Observer<Language> { translate().addOnCompleteListener(processTranslation) }
    translatedText.addSource(sourceLang, languageObserver)
    translatedText.addSource(targetLang, languageObserver)

    fetchDownloadedModels()
  }

  private fun getModel(languageCode: String): TranslateRemoteModel {
    return TranslateRemoteModel.Builder(languageCode).build()
  }

  private fun fetchDownloadedModels() {
    modelManager.getDownloadedModels(TranslateRemoteModel::class.java).addOnSuccessListener {
      remoteModels ->
      availableModels.value = remoteModels.sortedBy { it.language }.map { it.language }
    }
  }

  internal fun downloadLanguage(language: Language) {
    val model = getModel(TranslateLanguage.fromLanguageTag(language.code)!!)
    var downloadTask: Task<Void>?
    if (pendingDownloads.containsKey(language.code)) {
      downloadTask = pendingDownloads[language.code]
      if (downloadTask != null && !downloadTask.isCanceled) {
        return
      }
    }
    downloadTask =
      modelManager.download(model, DownloadConditions.Builder().build()).addOnCompleteListener {
        pendingDownloads.remove(language.code)
        fetchDownloadedModels()
      }
    pendingDownloads[language.code] = downloadTask
  }

  fun requiresModelDownload(
    lang: Language,
    downloadedModels: List<String?>?,
  ): Boolean {
    return if (downloadedModels == null) {
      true
    } else !downloadedModels.contains(lang.code) && !pendingDownloads.containsKey(lang.code)
  }

  internal fun deleteLanguage(language: Language) {
    val model = getModel(TranslateLanguage.fromLanguageTag(language.code)!!)
    modelManager.deleteDownloadedModel(model).addOnCompleteListener { fetchDownloadedModels() }
    pendingDownloads.remove(language.code)
  }

  fun translate(): Task<String> {
    val text = sourceText.value
    val source = sourceLang.value
    val target = targetLang.value
    if (source == null || target == null || text.isNullOrEmpty()) {
      return Tasks.forResult("")
    }
    val sourceLangCode = TranslateLanguage.fromLanguageTag(source.code)!!
    val targetLangCode = TranslateLanguage.fromLanguageTag(target.code)!!
    val options =
      TranslatorOptions.Builder()
        .setSourceLanguage(sourceLangCode)
        .setTargetLanguage(targetLangCode)
        .build()
    return translators[options].downloadModelIfNeeded().continueWithTask { task ->
      if (task.isSuccessful) {
        translators[options].translate(text)
      } else {
        Tasks.forException(
          task.exception
            ?: Exception(getApplication<Application>().getString(R.string.unknown_error))
        )
      }
    }
  }

  inner class ResultOrError(var result: String?, var error: Exception?)

  override fun onCleared() {
    super.onCleared()
    translators.evictAll()
  }
}
