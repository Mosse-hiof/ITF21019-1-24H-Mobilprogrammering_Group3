package no.hiof.mobilproggroup3.modelviews

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth

class HistoryViewModel : ViewModel() {

    fun deleteHistoryItem(db: FirebaseFirestore, id: String, context: Context) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId == null) {
            Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("users")
            .document(userId)
            .collection("history")
            .document(id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(context, "history deleted successfully.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to delete history: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    fun editHistoryItem(db: FirebaseFirestore, id: String, newText: String, context: Context) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("users")
            .document(userId)
            .collection("history")
            .document(id)
            .update("text", newText)
            .addOnSuccessListener {
                Toast.makeText(context, "history updated successfully.", Toast.LENGTH_SHORT).show()
            }
    }
}