package com.klintsoft.imagecapture

import android.Manifest.permission.CAMERA
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.klintsoft.imagecapture.ui.theme.ImageCaptureTheme
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ImageCaptureTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    ImageCapture()
                }
            }
        }
    }
}

/** README Info **/
/*
<uses-permission android:name="android.permission.CAMERA" /> is needed for this although the app doesn't directly access the camera
See below a nice way to ask for permissions only when the button is clicked and not before that

We also need Provider in manifest file and filepaths.xml in resources.
<cache-path name="cache_pictures" path="/" /> works for the temp picture stored in cache while
<external-files-path name="images" path="images"/> works for picture saved in the app/files/images folder
(for some reason it is not files-path as stated in documentation
*/

@Composable
fun ImageCapture(){
    Column() {
        ImageCaptureTemp(modifier = Modifier.weight(0.5f))
        ImageCaptureToFolder(modifier = Modifier.weight(0.5f))
    }

}


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ImageCaptureTemp(modifier: Modifier) {
    val context = LocalContext.current

    var currentPhotoUri by remember { mutableStateOf(value = Uri.EMPTY) }
    var tempPhotoUri by remember { mutableStateOf(value = Uri.EMPTY) }

    /** Image grabber using camera , Camera permission required, need to ask for it **/
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { success ->
        if (success) { currentPhotoUri = tempPhotoUri}
        Log.i("MyInfo", "Success grabbing photo: $success, uri: $tempPhotoUri")
    }

    //Good way to check permissions and if granted execute the needed TODO
    // in here instead of in the onClick body at the same time
    //This is activates when cameraPermissionState::launchPermissionRequest is invoked
    val cameraPermissionState = rememberPermissionState(
        permission = CAMERA,
        onPermissionResult = { granted ->
            if (granted) {
                tempPhotoUri = context.createTempPictureUri()
                cameraLauncher.launch(tempPhotoUri)
            }
            else print("camera permission is denied")
        }
    )

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedVisibility(modifier = Modifier.weight(1f), visible = currentPhotoUri.toString().isNotEmpty()) {
            // from coil library
            AsyncImage(
                modifier = Modifier
                    .padding(4.dp)
                    .fillMaxWidth(),
                model = currentPhotoUri,
                contentScale = ContentScale.Fit,
                contentDescription = null
            )
        }
        Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
            Text(text = "Take a temp photo with Camera")
        }
    }
}

/** Creating a file path in cache and uri for the temporary image file **/
fun Context.createTempPictureUri(
    provider: String = "${BuildConfig.APPLICATION_ID}.provider",
    fileName: String = "picture_${System.currentTimeMillis()}",
    fileExtension: String = ".png"
): Uri {
    val tempFile = File.createTempFile(
        fileName, fileExtension, cacheDir
    ).apply {
        createNewFile()
    }
    return FileProvider.getUriForFile(applicationContext, provider, tempFile)
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ImageCaptureToFolder(modifier: Modifier) {
    val context = LocalContext.current

    var uri by remember { mutableStateOf(Uri.EMPTY) }
    var showImage by remember { mutableStateOf(false) }

    /** Image grabber using camera , Camera permission required, need to ask for it **/
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        showImage = success
        Log.i("MyInfo", "Success grabbing photo: $success")
    }

    //Good way to check permissions and if granted execute the needed TODO
    // in here instead of in the onClick body at the same time
    //This is activates when cameraPermissionState::launchPermissionRequest is invoked
    val cameraPermissionState = rememberPermissionState(
        permission = CAMERA,
        onPermissionResult = { granted ->
            if (granted) {
                uri = context.createPictureUri()
                cameraLauncher.launch(uri)
            }
            else print("camera permission is denied")
        }
    )

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (showImage){
            val image = context.contentResolver.openInputStream(uri).use { data -> BitmapFactory.decodeStream(data) }
            Log.i("MyInfo", "image: $image")
            Image(
                bitmap = image.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .weight(1f)
                    .padding(4.dp)
                    .fillMaxWidth(),
                contentScale = ContentScale.Fit
            )
        }
        Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
            Text(text = "Take a photo with Camera to folder")
        }
    }
}

/** Creating a file path in the app's folder and uri for the image file **/
fun Context.createPictureUri(
    folder: File = File(applicationContext.getExternalFilesDir(null), "images"),
    filename: String = "newPicture.jpg",
    provider: String = "${BuildConfig.APPLICATION_ID}.provider",
): Uri {
    if(!folder.exists()) folder.mkdir()
    val file = File(folder, filename)
    val uri = FileProvider.getUriForFile(applicationContext, provider, file)
    Log.i("MyInfo", "createPictureUri: uri: $uri")
    return uri
}
