package com.hiof.mobilprog_androidapp_group3.controllers

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.TextRecognizerOptionsInterface
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class TextRecognizer {

    // Initialize the TextRecognizer with the appropriate options
    private val recognizer: TextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)


    fun initializeTextRecognition() {
        // Code to initialize text recognition if needed
    }

    fun processImage(bitmap: Bitmap, onTextRecognized: (String) -> Unit) {
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                // Pass recognized text to callback
                onTextRecognized(visionText.text)
            }
            .addOnFailureListener {
                // Handle error
            }
    }

    fun getRecognizedText(): String {
        // Return recognized text (if storing it)
        return ""
    }
}