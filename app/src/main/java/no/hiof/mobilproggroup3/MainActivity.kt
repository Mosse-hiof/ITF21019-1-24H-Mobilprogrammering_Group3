package no.hiof.mobilproggroup3

//Libraries and imports are now good, we have nothing to worry about. Remove and add as needed
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.compose.*
import androidx.navigation.compose.rememberNavController
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import no.hiof.mobilproggroup3.compose.MobilProgGroup3Theme
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.Locale
import androidx.compose.runtime.Composable
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Person
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import no.hiof.mobilproggroup3.modelviews.HistoryViewModel
import no.hiof.mobilproggroup3.modelviews.MainViewModel
import no.hiof.mobilproggroup3.ui.screens.HistoryScreen
import no.hiof.mobilproggroup3.ui.screens.MainScreen
import no.hiof.mobilproggroup3.ui.screens.SettingsScreen
import no.hiof.mobilproggroup3.modelviews.SettingsViewModel
import no.hiof.mobilproggroup3.ui.screens.ProfileScreen


data class BottomNavigationBarItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
    )

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore

    private val mainViewModel: MainViewModel by viewModels()
    private val historyViewModel: HistoryViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()
        textToSpeech = TextToSpeech(this, this)
        auth = Firebase.auth

        if (auth.currentUser == null) {
            startLoginActivity()
            return
        }

        //to load the actual setting values from firebase and actually apply them instead of just loading the UI, previously only loaded UI, but pitch and speed changes didnt take effect
        settingsViewModel.loadSettings(db) { pitch, speed ->
            textToSpeech.setPitch(pitch)
            textToSpeech.setSpeechRate(speed)
        }

        setContent {
            MobilProgGroup3Theme {
                val navItems = listOf(
                    BottomNavigationBarItem(
                        title = "main",
                        selectedIcon = Icons.Filled.Home,
                        unselectedIcon = Icons.Outlined.Home
                    ),

                    BottomNavigationBarItem(
                        title = "history",
                        selectedIcon = Icons.Filled.Email,
                        unselectedIcon = Icons.Outlined.Email,
                    ),

                    BottomNavigationBarItem(
                        title = "profile",
                        selectedIcon = Icons.Filled.Person,
                        unselectedIcon = Icons.Outlined.Person
                    ),

                    BottomNavigationBarItem(
                        title = "settings",
                        selectedIcon = Icons.Filled.Settings,
                        unselectedIcon = Icons.Outlined.Settings
                    )
                )

                val navController = rememberNavController()
                var selectedItemIndex by rememberSaveable { mutableIntStateOf(0) }

                Scaffold(modifier = Modifier.fillMaxWidth(),
                    bottomBar = {
                        NavigationBar(containerColor = NavigationBarDefaults.containerColor,
                            contentColor = MaterialTheme.colorScheme.primary){
                            navItems.forEachIndexed { index, item ->
                                NavigationBarItem(
                                    selected = selectedItemIndex == index,
                                    onClick = {
                                        selectedItemIndex = index
                                        navController.navigate(item.title)
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if ( index == selectedItemIndex) {
                                                item.selectedIcon
                                            } else item.unselectedIcon,
                                            contentDescription = item.title
                                        )
                                    }
                                )

                            }
                        }
                    }) {


                    //Navigation and screens
                    //Some needed navigation added
                    NavHost(navController, startDestination = "main") {
                        composable("main") { MainScreen(viewModel = mainViewModel, navController, cameraExecutor, this@MainActivity::readOutLoud, db=db) }
                        composable("history") { HistoryScreen(viewModel = historyViewModel, db=db, textToSpeech = textToSpeech) }
                        composable("profile") { ProfileScreen(auth = auth) }
                        composable("settings") { SettingsScreen(viewModel = settingsViewModel, textToSpeech,db = db, onLogout = { signOut() }) }
                    }
                }
            }
        }
    }
    //login
    private fun startLoginActivity() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

   //logout
    private fun signOut() {
        auth.signOut()
        startLoginActivity()
    }

    //android tts ''built inn'' api, no dependencies needed
    //but method names must match the required Android Text-to-Speech API conventions
    //reference: https://developer.android.com/reference/kotlin/android/speech/tts/TextToSpeech.OnInitListener
    override fun onInit(status: Int){
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech.setLanguage(Locale.ENGLISH)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED){
                Toast.makeText(this, "TTS language not supported.", Toast.LENGTH_SHORT).show()
            }
        }else {
            Toast.makeText(this, "TTS initialization failed.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        textToSpeech.stop()
        textToSpeech.shutdown()
    }
    //reading the captured text out load
    //was initially outside the main activity, but had to move it into mainactivity as the method was not recognized
    private fun readOutLoud(text: String) {
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }
}

fun saveCapturedImage(context: Context, bitmap: Bitmap): File? {
    val filename = "captured_image_${System.currentTimeMillis()}.jpg"
    val file = File(context.filesDir, filename)

    try {
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        }
        val galleryFile = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), filename)
        file.copyTo(galleryFile, overwrite = true)

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        uri?.let {
            context.contentResolver.openOutputStream(it).use { out ->
                galleryFile.inputStream().use { input ->
                    input.copyTo(out!!)
                }
            }
        }
        Toast.makeText(context, "Image saved to Gallery", Toast.LENGTH_SHORT).show()
        return file
    } catch (e: IOException) {
        e.printStackTrace()
        Toast.makeText(context, "Failed to save image: ${e.message}", Toast.LENGTH_SHORT).show()
    }
    return null
}

//This our other primary requirement/functionality for the alpha build
//This function sets up the camera preview using CameraX.
//It binds the camera to the lifecycle of the current activity or composable
//making sure that the camera starts and stops with the app's lifecycle.
//We create and set up the preview and image capture use cases, then pass the ImageCapture instance to the calling function.
@Composable
fun CameraPreview(
    imageCapture: (ImageCapture) -> Unit,
    onImageCaptured: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    var previewView: PreviewView? by remember { mutableStateOf(null) }

    LaunchedEffect(cameraProviderFuture) {
        try {
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build()
            val capture = ImageCapture.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            cameraProvider.unbindAll()

            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                capture
            )

            imageCapture(capture)

            previewView?.let { view ->
                preview.setSurfaceProvider(view.surfaceProvider)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to open camera.", Toast.LENGTH_LONG).show()
        }
    }

    //AndroidView for displaying the camera preview
    AndroidView(
        factory = { ctx ->
            val view = PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            previewView = view
            view
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
    )
}

fun ImageProxy.toBitmapSafe(): Bitmap? {
    return try {
        val buffer = planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (e: Exception) {
        null
    }
}
