package no.hiof.mobilproggroup3.ui.screens

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import no.hiof.mobilproggroup3.modelviews.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, textToSpeech: TextToSpeech, onLogout: () -> Unit, db: FirebaseFirestore) {
    var pitchSliderPosition by remember { mutableFloatStateOf(1.0f) }
    var speedSliderPosition by remember { mutableFloatStateOf(1.0f) }
    var volumeSliderPosition by remember { mutableFloatStateOf(0f) }
    var darkModeBool by remember { mutableStateOf(false) }
    var increaseTextSizeBool by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadUserSettings(db) { pitch, speed ->
            pitchSliderPosition = pitch
            speedSliderPosition = speed
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(modifier = Modifier
            .padding(16.dp),
            text = "Settings", fontWeight = FontWeight.Bold, fontSize = 25.sp)
        Row(modifier = Modifier
            .padding(16.dp)) {
            Text("Language Selection", fontWeight = FontWeight.Bold)
        }
        Text("TextToSpeech Settings", fontWeight = FontWeight.Bold)
        Row {
            Text("Voice Pitch")
            Spacer(modifier = Modifier.width(16.dp))
            Slider(value = pitchSliderPosition,
                onValueChange = {pitchSliderPosition = it },
                onValueChangeFinished = { textToSpeech.setPitch(pitchSliderPosition)
                },
                steps = 3,
                valueRange = 0.5f..2.0f,
                thumb = { Box(
                    Modifier
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(20.dp))
                    .size(20.dp))
                },
            )
        }

        Text(modifier = Modifier
            .padding(16.dp),
            text = "$pitchSliderPosition hz"
        )

        Row {
            Text("Voice Speed")
            Spacer(modifier = Modifier.width(16.dp))
            Slider(value = speedSliderPosition,
                onValueChange = {speedSliderPosition = it },
                onValueChangeFinished = {
                    textToSpeech.setSpeechRate(speedSliderPosition)
                },
                thumb = { Box(
                    Modifier
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(20.dp))
                    .size(20.dp))
                },
                steps = 2,
                valueRange = 0.5f..2.0f
            )
        }
        Text(modifier = Modifier
            .padding(16.dp),text = "$speedSliderPosition words per minute"
        )

        //Row with volume slider
        Row {
            Text("Voice Volume")
            Spacer(modifier = Modifier.width(10.dp))
            Slider(value = volumeSliderPosition,
                onValueChange = {volumeSliderPosition = it},
                thumb = { Box(
                    Modifier
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(20.dp))
                    .size(20.dp))
                },
                valueRange = 0f..10f,
                steps = 10)
        }

        //Text displaying value of volumeslider
        Text(volumeSliderPosition.toString(),
            fontWeight = FontWeight.Bold)


        //Accessibility settings start here. These include dark mode and increasing text size
        Text(modifier = Modifier
            .padding(16.dp),
            text = "Accessibility Options",
            fontWeight = FontWeight.Bold)

        //Row with switche and describable text for settings for dark mode and increasing size
        Row (
            verticalAlignment = Alignment.CenterVertically
        ) {
            Switch(modifier = Modifier.padding(16.dp),
                checked = darkModeBool,
                onCheckedChange = { darkModeBool = it }
            )

            MaterialTheme.colorScheme
            Spacer(modifier = Modifier.width((16.dp)))
            Text("Dark Mode")
        }

        //Row with switch
        Row (
            verticalAlignment = Alignment.CenterVertically
        ) {
            Switch(modifier = Modifier
                .padding(16.dp), checked = increaseTextSizeBool,
                onCheckedChange = { increaseTextSizeBool = it }
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Increase Text Size"
            )
        }
        Button(onClick = { viewModel.savedUserSettings(db, pitchSliderPosition, speedSliderPosition) }) {
            Text("Save Settings")

        }

        //Logout Button
        Button(
            onClick = { onLogout() },
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text("Logout")
        }

    }
}