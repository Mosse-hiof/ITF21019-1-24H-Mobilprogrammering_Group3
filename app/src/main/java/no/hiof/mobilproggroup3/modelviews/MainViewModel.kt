package no.hiof.mobilproggroup3.modelviews

import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.firebase.auth.FirebaseAuth

class MainViewModel : ViewModel() {

    //This is one of our primary requirements/functionality
    //Capture text from the image using ML Kit
    //This function checks if we have a captured image and then tries to find any text in it.
    //If it finds some text, it adds that text to our list of recognized texts. If no text is found it just adds blank to the history screen
    //removed the logs and added some error handling,
    fun captureText(
        capturedImage: Bitmap?,
        context: Context,
        readOutLoud: (String) -> Unit,
        db: FirebaseFirestore
    ) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        if (capturedImage == null) {
            Toast.makeText(context, "No picture to process capture an picture first", Toast.LENGTH_SHORT).show()
            return
        }

        val image = InputImage.fromBitmap(capturedImage, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val recognizedText = visionText.text
                Toast.makeText(context, "Text recognition successful", Toast.LENGTH_SHORT).show()
                readOutLoud(recognizedText)

                val captureHistory = hashMapOf(
                    "text" to recognizedText,
                    "timestamp" to System.currentTimeMillis(),
                    "userId" to userId
                )

                db.collection("users")
                    .document(userId)
                    .collection("history")
                    .add(captureHistory)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Text saved to history.", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Failed to save to history: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e -> Toast.makeText(context, "Failed to recognize text: ${e.message}", Toast.LENGTH_SHORT).show()}
    }
}
