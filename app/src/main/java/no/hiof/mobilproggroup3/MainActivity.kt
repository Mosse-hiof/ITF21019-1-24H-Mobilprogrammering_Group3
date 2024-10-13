package no.hiof.mobilproggroup3

//DO NOT UNDER ANY CIRCUMSTANCES REMOVE ANY OF THE IMPORTED LIBRARIES
//IF A IMPORT IS UNUSED OR GIVEN AN ERROR FOR, THEN JUST COMMENT IT OUT WITH // I WILL CHECK IT LATER
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
//import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import androidx.navigation.compose.rememberNavController
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import no.hiof.mobilproggroup3.ui.theme.MobilProgGroup3Theme
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {

    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()

        setContent {
            MobilProgGroup3Theme {
                //for navigation
                val navController = rememberNavController()

                //saves recognized text from images in a mutable compose list temporarily in aplha version
                //saved text gets deleted once emulator/app is closed
                //we will probably redo/change this for when we start using persistent storage and firebase.
                var recognizedTexts by remember { mutableStateOf(mutableListOf<String>()) }

                //Navigation and screens
                //alot of inn-app navigation like ''go back to previous screen'' or navigation arrows are currently missing
                //we can try to add some before deadline, if not just use the IDE emulator buttons for back and forward navigation
                NavHost(navController, startDestination = "main") {
                    composable("main") { MainScreen(navController, cameraExecutor, recognizedTexts) }
                    composable("history") { HistoryScreen(recognizedTexts) }
                    composable("settings") { SettingsScreen() }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}

//Main screen of the application AKA landing screen
//opens up on camera and currently in alpha only uses simple buttons for capture/recognize etc
//the capture function dosent do a traditional image capture yet, but expect to be polished/added later
@Composable
fun MainScreen(
    navController: NavHostController,
    cameraExecutor: ExecutorService,
    recognizedTexts: MutableList<String>
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
    //I will try to show where you guys can do UI improvements via comments the best i can
    //but i might be mistaken on some parts
    if (hasCameraPermission) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            //title of the app, its supposed to be on the top of the screen
            //but as of right now it immediately gets hidden behind the camera screen upon opening
            //maybe move camera screen a bit down to show title
            Text(text = "SnapSpeech: Capture and Recognize Text")

            Spacer(modifier = Modifier.height(20.dp))

            CameraPreview(imageCapture = { capture -> imageCapture = capture }) { bitmap ->
                capturedImage = bitmap
            }

            Spacer(modifier = Modifier.height(20.dp))

            //button for capturing image, its pretty basic
            Button(onClick = {
                imageCapture?.takePicture(
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageCapturedCallback() {
                        override fun onCaptureSuccess(imageProxy: ImageProxy) {
                            val bitmap = imageProxy.toBitmapSafe()
                            if (bitmap != null) {
                                capturedImage = bitmap
                            }else {
                                Toast.makeText(context, "Image Capture Failed", Toast.LENGTH_SHORT).show()
                            }
                            imageProxy.close()
                        }

                        override fun onError(exception: ImageCaptureException) {
                            Toast.makeText(context, "Error capturing image: ${exception.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                )
            }) {
                Text(text = "Capture Text")
            }

            Spacer(modifier = Modifier.height(20.dp))

            //button for capture/recognize text
            Button(onClick = {
                captureText(capturedImage, recognizedTexts, context)
            }) {
                Text(text = "Process Text")
            }

            Spacer(modifier = Modifier.height(20.dp))

            //nav button for history screen
            Button(onClick = { navController.navigate("history") }) {
                Text(text = "View History")
            }

            Spacer(modifier = Modifier.height(10.dp))

            //nav button for settings screen
            Button(onClick = { navController.navigate("settings") }) {
                Text(text = "Settings")
            }
        }
    } else {
        //Display message while waiting for camera permission to be granted
        //maybe add some better effects and some delays/loading effect gifs and stuff
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
//If it finds some text, it adds that text to our list of recognized texts.
//so iremoved the logs and added some error handling,
//however as of right now even a blank screen without any texts gives "Text recognition successful" message
//on the bright side error messages given to users are kind of nice and looks ok
private fun captureText(capturedImage: Bitmap?, recognizedTexts: MutableList<String>, context: android.content.Context) {
    if (capturedImage == null) {
        Toast.makeText(context, "No picture to process capture an picture first", Toast.LENGTH_SHORT).show()
        return
    }

    val image = InputImage.fromBitmap(capturedImage, 0)
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    recognizer.process(image)
        .addOnSuccessListener { visionText ->
            val recognizedText = visionText.text
            recognizedTexts.add(recognizedText)
            Toast.makeText(context, "Text recognition successful", Toast.LENGTH_SHORT).show()
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
//tried to add erorhandling, but it dosent do anything
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
//UI improvement for the history can be done from here
//stuff like navigation arrows to previous screen etc would be nice
//Recognized texts are displayed in ugly column boxes for now.
@Composable
fun HistoryScreen(recognizedTexts: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        Text(text = "History Log", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(20.dp))

        if (recognizedTexts.isEmpty()) {
            Text(text = "No history available yet.")
        } else {
            Column(modifier = Modifier.fillMaxWidth()) {
                recognizedTexts.forEach { recognizedText ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .border(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        BasicText(text = recognizedText)
                    }
                }
            }
        }
    }
}

//Composable for Settings Screen
//UI improvement for the settings screen can be done from here, we'll stick to the single MainActivity file structure we have now
//stuff like navigation arrows to previous screen etc would be nice however, but can wait til assignment 5 no rush
//this is a placeholder/dummy screen which is literally a copy/paste of the history screen
@Composable
fun SettingsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Settings")
        Spacer(modifier = Modifier.height(20.dp))

        Text(text = "No settings functionality is added in alpha")
    }
}
