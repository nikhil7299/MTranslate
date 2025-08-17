package com.nikhildev.mtranslate

import android.annotation.SuppressLint
import androidx.fragment.app.viewModels
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Locale

class Translation (lang1 :String,lang2 : String){
    private var lang1 :String
    private var lang2 : String
    init {
        this.lang1 = lang1
        this.lang2 = lang2
    }
    fun getLang1():String{
        return lang1
    }
    fun getLang2():String{
        return lang2
    }
}


//
// NIKHIL KUMAR
// Software Engineer
// Instagram - @flutter.fury
//
class RecyclerAdapter (private var translationList : List<Translation>, private val textToSpeech: TextToSpeech,private var viewModel: ShowTranslationsViewModel):RecyclerView.Adapter<RecyclerAdapter.TranslationViewHolder>(){

    inner class TranslationViewHolder(itemView : View):RecyclerView.ViewHolder(itemView) {
        var lang1Text :TextView  = itemView.findViewById(R.id.listLang1Text)
        var lang1Speech : FloatingActionButton = itemView.findViewById(R.id.lang1Speech)
        var lang2Text :TextView  = itemView.findViewById(R.id.listLang2Text)
        var lang2Speech : FloatingActionButton = itemView.findViewById(R.id.lang2Speech)
    }
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): TranslationViewHolder {
        val layoutInflater  = LayoutInflater.from(parent.context).inflate(R.layout.translations_item_layout,parent,false)
        return TranslationViewHolder(layoutInflater)
    }
    override fun onBindViewHolder(holder: TranslationViewHolder, position: Int) {
        val translation = translationList[position]
        holder.lang1Text.text = translation.getLang1()
        holder.lang2Text.text = translation.getLang2()
        holder.lang1Speech.setOnClickListener {
            val ttsResult = textToSpeech.setLanguage(Locale(viewModel.sourceLang.value!!.code))
            if(ttsResult == TextToSpeech.LANG_MISSING_DATA || ttsResult == TextToSpeech.LANG_NOT_SUPPORTED){
                Toast.makeText(holder.itemView.context,"Language TTS Not Supported", Toast.LENGTH_SHORT).show()
            }else {
                Toast.makeText(holder.itemView.context,"Speaking Source Text", Toast.LENGTH_SHORT).show()
                textToSpeech.speak(holder.lang1Text.text.toString(), TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }
        holder.lang2Speech.setOnClickListener {
            val ttsResult = textToSpeech.setLanguage(Locale(viewModel.targetLang.value!!.code))
            if(ttsResult == TextToSpeech.LANG_MISSING_DATA || ttsResult == TextToSpeech.LANG_NOT_SUPPORTED){
                Toast.makeText(holder.itemView.context,"Language TTS Not Supported", Toast.LENGTH_SHORT).show()
            }else {
                Toast.makeText(holder.itemView.context,"Speaking Target Text", Toast.LENGTH_SHORT).show()
                textToSpeech.speak(holder.lang2Text.text.toString(), TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }
    }
    override fun getItemCount(): Int {
        return translationList.size
    }
}

class ShowTranslationsFragment : Fragment(),TextToSpeech.OnInitListener{
    //
    private lateinit var auth: FirebaseAuth
    private lateinit var firebaseDatabase: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    //
    private lateinit var adapter : ArrayAdapter<Language>
    private lateinit var  lang1Selector :Spinner
    private lateinit var  lang2Selector :Spinner

    private lateinit var switchLangButton : FloatingActionButton
    private lateinit var showTranslationsButton : MaterialButton
    private lateinit var showRecyclerView : RecyclerView

    private lateinit var recyclerAdapter: RecyclerAdapter
    private var translationList = ArrayList<Translation>()
    private lateinit var textToSpeech : TextToSpeech

    private val viewModel: ShowTranslationsViewModel by viewModels()

    companion object {
        fun newInstance() = ShowTranslationsFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_show_translations, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //
        auth = FirebaseAuth.getInstance()
        firebaseDatabase = FirebaseDatabase.getInstance()
        databaseReference = firebaseDatabase.reference.child("translations")
        //
        lang1Selector = view.findViewById(R.id.lang1Selector)
        lang2Selector = view.findViewById(R.id.lang2Selector)
        switchLangButton =  view.findViewById(R.id.swapLang)
        showTranslationsButton = view.findViewById(R.id.showTranslationsButton)
        showRecyclerView = view.findViewById(R.id.showRecyclerView)
        textToSpeech = TextToSpeech(this.context,this)

        adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item, viewModel.availableLanguages
        )
        lang1Selector.adapter = adapter
        lang2Selector.adapter = adapter

        lang1Selector.setSelection(adapter.getPosition(Language("en")))
        lang2Selector.setSelection(adapter.getPosition(Language("es")))
        //
        val layoutManager = LinearLayoutManager(requireContext())
        showRecyclerView.layoutManager = layoutManager
        showRecyclerView.itemAnimator = DefaultItemAnimator()
        recyclerAdapter = RecyclerAdapter(translationList,textToSpeech,viewModel)
        showRecyclerView.adapter = recyclerAdapter

        switchLangButton.setOnClickListener {
            val lang1Position = lang1Selector.selectedItemPosition
            lang1Selector.setSelection(lang2Selector.selectedItemPosition)
            lang2Selector.setSelection(lang1Position)
            Toast.makeText(requireContext(),"Languages Switched", Toast.LENGTH_SHORT).show()
        }

        lang1Selector.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long,
            ) {
                viewModel.sourceLang.value = adapter.getItem(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        lang2Selector.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long,
            ) {
                viewModel.targetLang.value = adapter.getItem(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        showTranslationsButton.setOnClickListener {
            val langKey = "${viewModel.sourceLang.value!!.code}-${viewModel.targetLang.value!!.code}"
            Log.d("langkey",langKey)
            databaseReference.child(auth.currentUser!!.uid).child(langKey)
            val postListener = object : ValueEventListener{
                @SuppressLint("NotifyDataSetChanged")
                override fun onDataChange(snapshot: DataSnapshot) {
                    translationList.clear()
                    val data = snapshot.value as Map<*, *>?
                    if (data != null) {
                        val translations =
                            data[auth.currentUser!!.uid] as Map<String, Map<String, String>>?
                        if ( translations!=null && translations[langKey] != null) {
                            Toast.makeText(requireContext(),"Fetching Translations", Toast.LENGTH_SHORT).show()
                            for ((k, v) in translations[langKey] as Map<String, String>) {
                                translationList.add(Translation(k, v))
                            }

                        }
                        else{
                            Toast.makeText(requireContext(),"No Translations Found", Toast.LENGTH_SHORT).show()
                        }
                    }
                    else{
                        Toast.makeText(requireContext(),"No Translations Found", Toast.LENGTH_SHORT).show()
                    }
                    showRecyclerView.invalidate()
                    recyclerAdapter.notifyDataSetChanged()

                }
                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }
            }
            databaseReference.addValueEventListener(postListener)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech.setLanguage(Locale.getDefault())
            // TTS is successfully initialized
        } else {
            Log.e("TTS", "Initialization failed")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        textToSpeech.stop()
        textToSpeech.shutdown()
    }
}