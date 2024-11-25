package no.hiof.mobilproggroup3.ui.screens

import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import no.hiof.mobilproggroup3.historyitems.HistoryItem
import no.hiof.mobilproggroup3.historyitems.HistoryItemView
import no.hiof.mobilproggroup3.modelviews.HistoryViewModel

@Composable
fun HistoryScreen(viewModel: HistoryViewModel, db: FirebaseFirestore, textToSpeech: TextToSpeech) {
    val context = LocalContext.current
    var recognizedTexts by remember { mutableStateOf(listOf<HistoryItem>()) }
    val userId = FirebaseAuth.getInstance().currentUser?.uid

    LaunchedEffect(Unit) {
        if (userId != null) {
            db.collection("users")
                .document(userId)
                .collection("history")
                .orderBy("time", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener { documents ->
                    recognizedTexts = documents.map { document ->
                        HistoryItem(
                            id = document.id,
                            text = document.getString("text").orEmpty(),
                            datetime = document.getLong("time") ?: 0L
                        )
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error loading history: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "History Log", fontWeight = FontWeight.Bold, fontSize = 25.sp)
        Spacer(modifier = Modifier.height(20.dp))

        if (recognizedTexts.isEmpty()) {
            Text(text = "No history available yet.")
        } else {
            Column(modifier = Modifier.fillMaxWidth()) {
                recognizedTexts.forEach { item ->
                    HistoryItemView(
                        historyItem = item,
                        onDelete = { id -> viewModel.deleteSavedHistoryText(db, id, context) },
                        onEdit = { id, newText -> viewModel.editingSavedHistoryText(db, id, newText, context) },
                        onPlayback = { text -> textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null) }
                    )
                }
            }
        }
    }
}
