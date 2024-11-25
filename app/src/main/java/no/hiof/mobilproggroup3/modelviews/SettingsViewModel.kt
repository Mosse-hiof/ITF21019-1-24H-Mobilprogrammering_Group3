package no.hiof.mobilproggroup3.modelviews

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel : ViewModel() {
    private val _pitch = MutableStateFlow(1.0f)
    val pitch: StateFlow<Float> = _pitch

    private val _speed = MutableStateFlow(1.0f)
    val speed: StateFlow<Float> = _speed

    //saving and loading settings
    //saved settings and history can be seen here: https://console.firebase.google.com/project/snapreaderapp/firestore/databases/-default-/data
    //you might need to login with a google account to view the data
    //added users and login function to the app, so settings are now saved/loaded per user rather then globally
    fun savedUserSettings(db: FirebaseFirestore, pitch: Float, speed: Float) {
        val currentUser = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val userSettings = hashMapOf(
            "pitch" to pitch,
            "speed" to speed,
            "userId" to currentUser,
            "lastUpdated" to System.currentTimeMillis()
        )

        db.collection("users")
            .document(currentUser)
            .collection("settings")
            .document("userSettings")
            .set(userSettings)
    }

    fun loadUserSettings(db: FirebaseFirestore, onSettingsLoaded: (Float, Float) -> Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("users")
            .document(currentUser)
            .collection("settings")
            .document("userSettings")
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val pitch = document.getDouble("pitch")?.toFloat() ?: 1.0f
                    val speed = document.getDouble("speed")?.toFloat() ?: 1.0f
                    _pitch.value = pitch
                    _speed.value = speed
                    onSettingsLoaded(pitch, speed)
                }
            }
    }
}