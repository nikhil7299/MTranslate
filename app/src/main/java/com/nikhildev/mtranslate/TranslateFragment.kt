package com.nikhildev.mtranslate

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Locale

class TranslateFragment : Fragment(),TextToSpeech.OnInitListener {
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var firebaseDatabase: FirebaseDatabase
   private lateinit var databaseReference: DatabaseReference
    //
    private lateinit var clearBtn1 : FloatingActionButton
    private lateinit var saveBtn : FloatingActionButton
    private lateinit var textToSpeechBtn1 : FloatingActionButton
    private lateinit var textToSpeechBtn2 : FloatingActionButton
    private lateinit var textToSpeech : TextToSpeech
    //
    private val REQUEST_CODE_SPEECH_INPUT = 1
    private lateinit var speechToTextButton:FloatingActionButton
    private lateinit var sourceSyncButton:ToggleButton
    private lateinit var targetSyncButton:ToggleButton
    private lateinit var sourceTextView:TextInputEditText
    private lateinit var targetTextView:TextView
    private lateinit var downloadedModelsTextView:TextView
    private lateinit var sourceLangSelector:Spinner
    private lateinit var targetLangSelector:Spinner
    private lateinit var viewModel: TranslateViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(false)
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_translate, container, false)
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {}

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()
        firebaseDatabase = FirebaseDatabase.getInstance()
        databaseReference = firebaseDatabase.reference.child("translations")
        //
        clearBtn1 = view.findViewById(R.id.clearBtn1)
        saveBtn  = view.findViewById(R.id.saveBtn)
        textToSpeechBtn1 = view.findViewById(R.id.txtToSpeech1)
        textToSpeechBtn2 = view.findViewById(R.id.txtToSpeech2)
        textToSpeech = TextToSpeech(this.context,this)
        //
        speechToTextButton =view.findViewById(R.id.speechToText)
        sourceSyncButton = view.findViewById(R.id.buttonSyncSource)
        targetSyncButton = view.findViewById(R.id.buttonSyncTarget)
        sourceTextView = view.findViewById(R.id.sourceText)
        targetTextView = view.findViewById(R.id.targetText)
        downloadedModelsTextView = view.findViewById(R.id.downloadedModels)
        sourceLangSelector = view.findViewById(R.id.sourceLangSelector)
        targetLangSelector = view.findViewById(R.id.targetLangSelector)
        viewModel = ViewModelProvider(this)[TranslateViewModel::class.java]


        val inputMethodManager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.google_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient  = GoogleSignIn.getClient(requireActivity(),gso)

        view.setOnTouchListener { _, _ ->
            sourceTextView.clearFocus()
            inputMethodManager.hideSoftInputFromWindow(sourceTextView.windowToken, 0)
            false
        }

        // NIKHIL KUMAR
        // SOFTWARE ENGINEER
        // Instagram - @flutter.fury

        textToSpeechBtn1.setOnClickListener {
            val srcText : String = sourceTextView.text.toString()
            val ttsResult = textToSpeech.setLanguage(Locale(viewModel.sourceLang.value!!.code))
            if(ttsResult == TextToSpeech.LANG_MISSING_DATA || ttsResult == TextToSpeech.LANG_NOT_SUPPORTED){
                Toast.makeText(requireContext(),"Language TTS Not Supported",Toast.LENGTH_SHORT).show()
            }else {
                Toast.makeText(requireContext(),"Speaking Source Text",Toast.LENGTH_SHORT).show()
                textToSpeech.speak(srcText, TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }

        clearBtn1.setOnClickListener{
            sourceTextView.text?.clear()
            sourceTextView.clearFocus()
            val inputMM =
                    getSystemService(requireContext(), InputMethodManager::class.java) as InputMethodManager
            inputMM.hideSoftInputFromWindow(view.windowToken, 0)
            Toast.makeText(requireContext(),"Cleared Source Text",Toast.LENGTH_SHORT).show()

        }
        textToSpeechBtn2.setOnClickListener {
            val targetText : String = targetTextView.text.toString()
            val ttsResult = textToSpeech.setLanguage(Locale(viewModel.targetLang.value!!.code))
            if(ttsResult == TextToSpeech.LANG_MISSING_DATA || ttsResult == TextToSpeech.LANG_NOT_SUPPORTED){
                Toast.makeText(requireContext(),"Language TTS Not Supported",Toast.LENGTH_SHORT).show()
            }else {
                Toast.makeText(requireContext(),"Speaking Target Text",Toast.LENGTH_SHORT).show()
                textToSpeech.speak(targetText, TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }

        saveBtn.setOnClickListener {
            val langKey = "${viewModel.sourceLang.value!!.code}-${viewModel.targetLang.value!!.code}"
            if(!sourceTextView.text.isNullOrEmpty() || !targetTextView.text.isNullOrEmpty()){
             try {
                 databaseReference.child(auth.currentUser!!.uid).child(langKey).updateChildren(
                     hashMapOf(sourceTextView.text.toString() to
                             targetTextView.text.toString()) as Map<String, String>
                 )
                 Toast.makeText(requireContext(),"Translation Saved",Toast.LENGTH_SHORT).show()
             }  catch (e: Exception){
                 Toast.makeText(requireContext(),"Unexpected Error Occurred",Toast.LENGTH_SHORT).show()
             }

        }
            val postListener = object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
//                    val post = dataSnapshot.value as Map<*, *>
//                    val enes  = post[auth.currentUser!!.uid] as Map<String,Any?>
//                    Log.d("data", post[auth.currentUser!!.uid].toString())
//                    Log.d("enes",enes["en-es"].toString())
                }
                override fun onCancelled(databaseError: DatabaseError) {
                    // Getting Post failed, log a message
                    Log.w("loadPost:onCancelled", databaseError.toException())
                }
            }
            databaseReference.addValueEventListener(postListener)
        }

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item, viewModel.availableLanguages
        )
        sourceLangSelector.adapter = adapter
        targetLangSelector.adapter = adapter
        sourceLangSelector.setSelection(adapter.getPosition(Language("en")))
        targetLangSelector.setSelection(adapter.getPosition(Language("es")))
        sourceLangSelector.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long,
            ) {
                setProgressText(targetTextView)
                viewModel.sourceLang.value = adapter.getItem(position)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                targetTextView.text = ""
            }
        }

        targetLangSelector.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long,
            ) {
                setProgressText(targetTextView)
                viewModel.targetLang.value = adapter.getItem(position)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                targetTextView.text = ""
            }
        }

        fun getLocaleCode(languageCode: String): String {
            return when (languageCode) {
                "af" -> "af-ZA"   // Afrikaans -> South Africa
                "sq" -> "sq-AL"   // Albanian -> Albania
                "ar" -> "ar-SA"   // Arabic -> Saudi Arabia
                "be" -> "be-BY"   // Belarusian -> Belarus
                "bg" -> "bg-BG"   // Bulgarian -> Bulgaria
                "bn" -> "bn-BD"   // Bengali -> Bangladesh
                "ca" -> "ca-ES"   // Catalan -> Spainkage
                "zh" -> "zh-CN"   // Chinese -> China
                "hr" -> "hr-HR"   // Croatian -> Croatia
                "cs" -> "cs-CZ"   // Czech -> Czech Republic
                "da" -> "da-DK"   // Danish -> Denmark
                "nl" -> "nl-NL"   // Dutch -> Netherlands
                "en" -> "en-US"   // English -> United States
                "eo" -> ""        // Esperanto -> No specific country
                "et" -> "et-EE"   // Estonian -> Estonia
                "fi" -> "fi-FI"   // Finnish -> Finland
                "fr" -> "fr-FR"   // French -> France
                "gl" -> "gl-ES"   // Galician -> Spain
                "ka" -> "ka-GE"   // Georgian -> Georgia
                "de" -> "de-DE"   // German -> Germany
                "el" -> "el-GR"   // Greek -> Greece
                "gu" -> "gu-IN"   // Gujarati -> India
                "ht" -> "ht-HT"   // Haitian Creole -> Haiti
                "he" -> "he-IL"   // Hebrew -> Israel
                "hi" -> "hi-IN"   // Hindi -> India
                "hu" -> "hu-HU"   // Hungarian -> Hungary
                "is" -> "is-IS"   // Icelandic -> Iceland
                "id" -> "id-ID"   // Indonesian -> Indonesia
                "ga" -> "ga-IE"   // Irish -> Ireland
                "it" -> "it-IT"   // Italian -> Italy
                "ja" -> "ja-JP"   // Japanese -> Japan
                "kn" -> "kn-IN"   // Kannada -> India
                "ko" -> "ko-KR"   // Korean -> South Korea
                "lt" -> "lt-LT"   // Lithuanian -> Lithuania
                "lv" -> "lv-LV"   // Latvian -> Latvia
                "mk" -> "mk-MK"   // Macedonian -> North Macedonia
                "mr" -> "mr-IN"   // Marathi -> India
                "ms" -> "ms-MY"   // Malay -> Malaysia
                "mt" -> "mt-MT"   // Maltese -> Malta
                "no" -> "nb-NO"   // Norwegian -> Norway
                "fa" -> "fa-IR"   // Persian -> Iran
                "pl" -> "pl-PL"   // Polish -> Poland
                "pt" -> "pt-PT"   // Portuguese -> Portugal
                "ro" -> "ro-RO"   // Romanian -> Romania
                "ru" -> "ru-RU"   // Russian -> Russia
                "sk" -> "sk-SK"   // Slovak -> Slovakia
                "sl" -> "sl-SI"   // Slovenian -> Slovenia
                "es" -> "es-ES"   // Spanish -> Spain
                "sv" -> "sv-SE"   // Swedish -> Sweden
                "sw" -> "sw-KE"   // Swahili -> Kenya
                "tl" -> "tl-PH"   // Tagalog -> Philippines
                "ta" -> "ta-IN"   // Tamil -> India
                "te" -> "te-IN"   // Telugu -> India
                "th" -> "th-TH"   // Thai -> Thailand
                "tr" -> "tr-TR"   // Turkish -> Turkey
                "uk" -> "uk-UA"   // Ukrainian -> Ukraine
                "ur" -> "ur-PK"   // Urdu -> Pakistan
                "vi" -> "vi-VN"   // Vietnamese -> Vietnam
                "cy" -> "cy-GB"   // Welsh -> United Kingdom
                else -> ""         // Return empty string for unknown language codes
            }
        }
        speechToTextButton.setOnClickListener {
            val locale = getLocaleCode(viewModel.sourceLang.value!!.code)
            Log.d("Locale", locale)
            if (locale.isEmpty()) {
                Toast.makeText(requireContext(),"Language Not Supported",Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    )
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale)
                    putExtra(
                        RecognizerIntent.EXTRA_PROMPT, "Speak to text"
                    )
                }
                try {
                    startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), " ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        sourceSyncButton.setOnCheckedChangeListener { _, isChecked ->
            val language = adapter.getItem(sourceLangSelector.selectedItemPosition)
            if (isChecked) {
                viewModel.downloadLanguage(language!!)
            } else {
                viewModel.deleteLanguage(language!!)
                Toast.makeText(requireContext(),"Language Model Deleted",Toast.LENGTH_SHORT).show()
            }
        }
        targetSyncButton.setOnCheckedChangeListener { _, isChecked ->
            val language = adapter.getItem(targetLangSelector.selectedItemPosition)
            if (isChecked) {
                viewModel.downloadLanguage(language!!)
            } else {
                viewModel.deleteLanguage(language!!)
                Toast.makeText(requireContext(),"Language Model Deleted",Toast.LENGTH_SHORT).show()

            }
        }

        sourceTextView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                setProgressText(targetTextView)
                viewModel.sourceText.postValue(s.toString())
            }
        })
        viewModel.translatedText.observe(
            viewLifecycleOwner
        ) { resultOrError ->
            if (resultOrError.error != null) {
                sourceTextView.error = resultOrError.error!!.localizedMessage
            } else {
                targetTextView.text = resultOrError.result
            }
        }

        viewModel.availableModels.observe(
            viewLifecycleOwner
        ) { translateRemoteModels ->
            val output = requireContext().getString(
                R.string.downloaded_models_label,
                translateRemoteModels
            )
            downloadedModelsTextView.text = output

            sourceSyncButton.isChecked = !viewModel.requiresModelDownload(
                adapter.getItem(sourceLangSelector.selectedItemPosition)!!,
                translateRemoteModels
            )
            targetSyncButton.isChecked = !viewModel.requiresModelDownload(
                adapter.getItem(targetLangSelector.selectedItemPosition)!!,
                translateRemoteModels
            )
        }
    }

    private fun setProgressText(tv: TextView) {
        tv.text = requireContext().getString(R.string.translate_progress)
    }

    companion object {
        fun newInstance(): TranslateFragment {
            return TranslateFragment()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // TTS is successfully initialized
            textToSpeech.setLanguage(Locale.getDefault())

        } else {
            Log.e("TTS", "Initialization failed")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        textToSpeech.stop()
        textToSpeech.shutdown()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SPEECH_INPUT) {
            if (resultCode == RESULT_OK && data != null) {
                val result =
                    data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                val editable = Editable.Factory.getInstance().newEditable(result?.get(0))
                sourceTextView.text = editable
            }
        }
    }

}
