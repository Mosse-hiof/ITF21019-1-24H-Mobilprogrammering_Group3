package com.hiof.mobilprog_androidapp_group3.controllers

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.*

class SpeechSynthesizer(private val context: Context) : TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech

    fun initializeTTS() {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.getDefault()
        }
    }

    fun speakText(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "")
    }

    fun setLanguage(language: Locale) {
        tts.language = language
    }
}