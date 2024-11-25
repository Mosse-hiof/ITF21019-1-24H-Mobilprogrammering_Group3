package no.hiof.mobilproggroup3.modelviews

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth

class HistoryViewModel : ViewModel() {

    //deleting individual history items from the history screen, does delete from firebase, but is not updated inn app until screen is refreshed
    //going to a different screen and then going back to history refreshes the screen and the deleted item is removed
    fun deleteSavedHistoryText(db: FirebaseFirestore, id: String, context: Context) {
        val currentUser = FirebaseAuth.getInstance().currentUser?.uid

        if (currentUser == null) {
            Toast.makeText(context, "Log in to use app", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("users")
            .document(currentUser)
            .collection("history")
            .document(id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(context, "history deleted", Toast.LENGTH_SHORT).show()
            }
    }

    //for editing the recognized text, the changes are also done in firebase same time
    fun editingSavedHistoryText(db: FirebaseFirestore, id: String, newText: String, context: Context) {
        val currentUser = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("users")
            .document(currentUser)
            .collection("history")
            .document(id)
            .update("text", newText)
            .addOnSuccessListener {
                Toast.makeText(context, "history updated", Toast.LENGTH_SHORT).show()
            }
    }
}