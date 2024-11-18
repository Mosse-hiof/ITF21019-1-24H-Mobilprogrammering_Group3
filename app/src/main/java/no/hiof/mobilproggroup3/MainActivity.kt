package no.hiof.mobilproggroup3

//Libraries and imports are now good, we have nothing to worry about. Remove and add as needed
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import androidx.navigation.compose.rememberNavController
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import no.hiof.mobilproggroup3.compose.MobilProgGroup3Theme
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.Locale
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore



data class BottomNavigationBarItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
    )

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var textToSpeech: TextToSpeech

    private val db = Firebase.firestore

    @OptIn(ExperimentalMaterial3Api::class)
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()
        textToSpeech = TextToSpeech(this, this)

        //to load the actual setting values from firebase and actually apply them instead of just loading the UI, previously only loaded UI, but pitch and speed changes didnt take effect
        loadSettings { pitch, speed ->
            textToSpeech.setPitch(pitch)
            textToSpeech.setSpeechRate(speed)}

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
                        composable("main") { MainScreen(navController, cameraExecutor, this@MainActivity::readOutLoud, db=db) }
                        composable("history") { HistoryScreen(db=db, textToSpeech = textToSpeech) }
                        composable("settings") { SettingsScreen(textToSpeech, saveSettings = ::saveSettings, loadSettings = ::loadSettings) }
                    }
                }
            }
        }
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

    //saving and loading settings
    //saved settings and history can be seen here: https://console.firebase.google.com/project/snapreaderapp/firestore/databases/-default-/data
    //you might need to login with a google account to view the data
    private fun saveSettings(pitch: Float, speed: Float) {
        val settings = hashMapOf("pitch" to pitch, "speed" to speed)
        db.collection("settings").document("userSettings")
            .set(settings)
            .addOnSuccessListener {
                Toast.makeText(this, "Settings saved successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to save settings: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadSettings(onSettingsLoaded: (Float, Float) -> Unit) {
        db.collection("settings").document("userSettings")
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val pitch = document.getDouble("pitch")?.toFloat() ?: 1.0f
                    val speed = document.getDouble("speed")?.toFloat() ?: 1.0f
                    onSettingsLoaded(pitch, speed)

                    textToSpeech.setPitch(pitch)
                    textToSpeech.setSpeechRate(speed)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load settings: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}

//Main screen of the application AKA landing screen
//opens up on camera and currently in alpha only uses simple buttons for capture/recognize etc
//the capture function dosent do a traditional image capture yet, but expect to be polished/added later
@Composable
fun MainScreen(
    navController: NavHostController,
    cameraExecutor: ExecutorService,
    readOutLoud: (String) -> Unit,
    db: FirebaseFirestore
) {
    //capturedImage: Holds the Bitmap of the image we capture
    //context: Gets the current context for accessing system resources
    //hasCameraPermission: Keeps track of whether we have camera permission
    //imageCapture: Holds the ImageCapture instance for taking pictures
    var capturedImage by remember { mutableStateOf<Bitmap?>(null) }
    val context = LocalContext.current
    var hasCameraPermission by remember { mutableStateOf(false) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            hasCameraPermission = true
        }
    }

    //UI logic based on camera permission
    //UI polishing and improvements for the main screen/landing screen can be done from here
    //I will try to show where you guys where you can do UI improvements via comments the best i can
    //but i might be mistaken on some parts
    if (hasCameraPermission) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            //title of the app, its supposed to be on the top of the screen
            //but as of right now it immediately gets hidden behind the camera screen upon opening
            //maybe move camera screen a bit down to show title
            Box(modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .fillMaxWidth()
                )
            {
                Text(
                    text = "SnapReader",
                    fontSize = 24.sp,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.Center)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            CameraPreview(imageCapture = { capture -> imageCapture = capture }) { bitmap ->
                capturedImage = bitmap
            }

            Spacer(modifier = Modifier.height(60.dp))

            //button for capturing image
            Column {
            IconButton(onClick = {
                imageCapture?.takePicture(
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(imageProxy: ImageProxy) {
                        val bitmap = imageProxy.toBitmapSafe()
                        imageProxy.close()

                        if (bitmap != null) {
                            captureText(bitmap, context, readOutLoud, db)
                        } else {
                            Toast.makeText(context, "Image Capture Failed", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Toast.makeText(
                            context,
                            "Error capturing image: ${exception.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            )
            }) {
                //if darktheme on phone camera button changes to white, else black camera button
                //code logic is not visible on emulator, but on the mobile device it works.
                if(isSystemInDarkTheme()) {
                    Image(
                        painterResource(R.drawable.baseline_camera_24_white),
                        contentDescription = "Camera button, capture text",
                        modifier = Modifier.size(48.dp)
                    )
                } else {
                    Image(
                    painterResource(R.drawable.baseline_camera_24),
                    contentDescription = "Camera button, capture text",
                    modifier = Modifier.size(48.dp))
                }
            }
            Spacer(modifier = Modifier.height(20.dp))

            //button for capture/recognize text
            IconButton(onClick = {
                captureText(capturedImage, context, readOutLoud, db)

            }) {//no longer need this icon on the main screen because all operation are done with the capture button above now, captured images are now directly passed onto captureText
                //Icon(Icons.Outlined.Add, contentDescription = "process text", modifier = Modifier.size(48.dp))
                }}

           Spacer(modifier = Modifier.height(20.dp))

                    }
        } else {
        //Display message while waiting for camera permission to be granted
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Requesting Permissions And Getting the App Ready")
        }
    }
}

//This is one of our primary requirements/functionality
//Capture text from the image using ML Kit
//This function checks if we have a captured image and then tries to find any text in it.
//If it finds some text, it adds that text to our list of recognized texts. If no text is found it just adds blank to the history screen
//removed the logs and added some error handling,
private fun captureText(capturedImage: Bitmap?, context: android.content.Context,  readOutLoud: (String) -> Unit, db: FirebaseFirestore) {

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

            val captureHistory = hashMapOf("text" to recognizedText, "timestamp" to System.currentTimeMillis())

            db.collection("history").add(captureHistory).addOnSuccessListener{
                Toast.makeText(context, "Text saved to Firebase.", Toast.LENGTH_SHORT).show()
            }
        }
        .addOnFailureListener { e ->
            Toast.makeText(context, "Failed to recognize text: ${e.message}", Toast.LENGTH_SHORT).show()
        }
}

//This our other primary requirement/functionality for the alpha build
//This function sets up the camera preview using CameraX.
//It binds the camera to the lifecycle of the current activity or composable
//making sure that the camera starts and stops with the app's lifecycle.
//We create and set up the preview and image capture use cases, then pass the ImageCapture instance to the calling function.
@Composable
fun CameraPreview(
    //Lambda to pass ImageCapture instance and to handle captured image
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

//Composable for History Screen
//Recognized texts are displayed in ugly column boxes for now.
@Composable
fun HistoryScreen(db: FirebaseFirestore, textToSpeech: TextToSpeech) {
    val context = LocalContext.current
    var recognizedTexts by remember { mutableStateOf(listOf<HistoryItem>()) }

    LaunchedEffect(Unit) {
        db.collection("history")
            .get()
            .addOnSuccessListener { documents ->
                recognizedTexts = documents.map { document ->
                    HistoryItem(
                        id = document.id,
                        text = document.getString("text").orEmpty(),
                        timestamp = document.getLong("timestamp") ?: 0L
                    )
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error loading history: ${e.message}", Toast.LENGTH_SHORT).show()
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
                        onDelete = { id -> deleteHistoryItem(db, id, context) },
                        onEdit = { id, newText -> editHistoryItem(db, id, newText, context) },
                        onPlayback = { text -> textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null) }
                    )
                }
            }
        }
    }
}

//=========================== 09.11.2024 HistoryScreen minor additions==========================
data class HistoryItem(
    val id: String,
    val text: String,
    val timestamp: Long
)

@Composable
fun HistoryItemView(
    historyItem: HistoryItem,
    onDelete: (String) -> Unit,
    onEdit: (String, String) -> Unit,
    onPlayback: (String) -> Unit,

) {
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

private fun deleteHistoryItem(db: FirebaseFirestore, id: String, context: Context) {
    db.collection("history").document(id)
        .delete()
        .addOnSuccessListener {
            Toast.makeText(context, "Text deleted successfully.", Toast.LENGTH_SHORT).show()
        }
        .addOnFailureListener { e ->
            Toast.makeText(context, "Failed to delete text: ${e.message}", Toast.LENGTH_SHORT).show()
        }
}

private fun editHistoryItem(db: FirebaseFirestore, id: String, newText: String, context: Context) {
    db.collection("history").document(id)
        .update("text", newText)
        .addOnSuccessListener {
            Toast.makeText(context, "Text updated successfully.", Toast.LENGTH_SHORT).show()
        }
        .addOnFailureListener { e ->
            Toast.makeText(context, "Failed to update text: ${e.message}", Toast.LENGTH_SHORT).show()
        }
}
//===========================History additions==========================


//Composable for Settings Screen
//setting scren now has some functionality, specificlly the the slider for tts pitch and speed
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(textToSpeech: TextToSpeech, saveSettings: (Float, Float) -> Unit, loadSettings: ((Float, Float) -> Unit) -> Unit) {
    var pitchSliderPosition by remember { mutableFloatStateOf(1.0f) }
    var speedSliderPosition by remember { mutableFloatStateOf(1.0f) }
    var volumeSliderPosition by remember { mutableFloatStateOf(0f) }
    var darkModeBool by remember { mutableStateOf(false) }
    var increaseTextSizeBool by remember { mutableStateOf(false) }
    //val context = LocalContext.current
    //val db = Firebase.firestore

    LaunchedEffect(Unit) {
        loadSettings { pitch, speed ->
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
                thumb = { Box(Modifier
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(20.dp))
                    .size(20.dp))},
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
                thumb = { Box(Modifier
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(20.dp))
                    .size(20.dp))},
                steps = 2,
                valueRange = 0.5f..2.0f
            )
        }
        Text(modifier = Modifier
            .padding(16.dp),text = "$speedSliderPosition words per minute"
        )

        // Row with volume slider
        Row {
            Text("Voice Volume")
            Spacer(modifier = Modifier.width(10.dp))
            Slider(value = volumeSliderPosition,
                onValueChange = {volumeSliderPosition = it},
                thumb = { Box(Modifier
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(20.dp))
                    .size(20.dp))},
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

        // Row with switch
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
        Button(onClick = { saveSettings(pitchSliderPosition, speedSliderPosition) }) {
            Text("Save Settings")

        }

    }
}


@androidx.compose.ui.tooling.preview.Preview
@Composable
fun SettingsPreview() {
    MobilProgGroup3Theme {
        SettingsScreen(textToSpeech = TextToSpeech(LocalContext.current) { }, saveSettings = { _, _ -> }, loadSettings = { onSettingsLoaded -> onSettingsLoaded(1.0f, 1.0f) })
    }
}
