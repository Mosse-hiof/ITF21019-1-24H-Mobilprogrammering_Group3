package no.hiof.mobilproggroup3.historyitems

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class HistoryItem(val id: String, val text: String, val datetime: Long)

@Composable
fun HistoryItemView(historyItem: HistoryItem, onDelete: (String) -> Unit, onEdit: (String, String) -> Unit, onPlayback: (String) -> Unit, ) {
    var isEditing by remember { mutableStateOf(false) }
    var newText by remember { mutableStateOf(historyItem.text) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .border(1.dp, MaterialTheme.colorScheme.primary)
            .verticalScroll(rememberScrollState())
    ){
        Column(modifier = Modifier
            .padding(8.dp)
        )
        {
            if (isEditing) {
                TextField(
                    value = newText,
                    onValueChange = { newText = it },
                    label = { Text("Edit Text") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row {
                    Button(onClick = {
                        onEdit(historyItem.id, newText)
                        isEditing = false
                    }) {
                        Text("Save")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { isEditing = false }) {
                        Text("Cancel")
                    }
                }
            } else {
                Text(text = historyItem.text, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)

                Row {
                    IconButton(onClick = { onPlayback(historyItem.text) }) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Play")
                    }
                    IconButton(onClick = { isEditing = true }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { onDelete(historyItem.id) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete")
                    }
                }
            }
        }
    }
}

