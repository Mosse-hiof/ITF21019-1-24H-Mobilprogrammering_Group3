package no.hiof.mobilproggroup3.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.google.firebase.firestore.FirebaseFirestore
import no.hiof.mobilproggroup3.CameraPreview
//import no.hiof.mobilproggroup3.modelviews.MainViewModel.captureText
import no.hiof.mobilproggroup3.modelviews.MainViewModel
import no.hiof.mobilproggroup3.savePictureOnPhone
import no.hiof.mobilproggroup3.toBitmapSafe
import java.util.concurrent.ExecutorService
import no.hiof.mobilproggroup3.R

//Main screen of the application AKA landing screen
//opens up on camera and currently in alpha only uses simple buttons for capture/recognize etc
//the capture function dosent do a traditional image capture yet, but expect to be polished/added later
@Composable
fun MainScreen(
    viewModel: MainViewModel,
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
                    text = stringResource(R.string.app_name),
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
                                    savePictureOnPhone(context, bitmap)
                                    viewModel.captureText(bitmap, context, readOutLoud, db)
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
                    viewModel.captureText(capturedImage, context, readOutLoud, db)

                }) {//no longer need this icon on the main screen because all operation are done with the capture button only now, captured images are now directly passed onto captureText
                    //Icon(Icons.Outlined.Add, contentDescription = "process text", modifier = Modifier.size(48.dp))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = stringResource(R.string.request_permissions))
        }
    }
}