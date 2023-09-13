package com.eduscope.eduscopeadminpanel

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.URL
import java.util.concurrent.CountDownLatch

class RandomFactEditActivity : ComponentActivity() {
    private lateinit var pref: SharedPreferences
    private var selectedSubject: String =
        ""  //everytime subject choice is changes the list updates
    private var imageMap: MutableMap<String, Any> = mutableMapOf()
    private var imageMapUrl: MutableMap<String, String> = mutableMapOf()
    private var imageMapByteArray: MutableMap<String, ByteArray> = mutableMapOf()
    private var imageMapByteArrayOriginal: MutableMap<String, ByteArray> = mutableMapOf()
    private var randomFactDocumentNames: MutableList<String> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pref = getSharedPreferences("spinnerChoices", Context.MODE_PRIVATE)
        selectedSubject = pref.getString("selectedSubject", "Philosophy").toString()
        setContent {
            ContentScreen()
        }
    }


    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    fun ContentScreen() {
        val firestore = FirebaseFirestore.getInstance()
        var documentNames by remember { mutableStateOf(emptyList<String>()) }
        val randomFactContents by remember { mutableStateOf(mutableListOf<String>()) }
        val randomFactContentsOriginal by remember { mutableStateOf(mutableListOf<String>()) }
        var isLoading by remember { mutableStateOf(true) }
        var saveChangesButtonVisible by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                val querySnapshot = firestore.collection("RandomFact").get().await()
                documentNames = querySnapshot.documents.mapNotNull { it.id }

                val collectionRef = firestore.collection("RandomFact").document(selectedSubject)
                    .collection(selectedSubject)
                    .get().await()
                var i = 0
                for (document in collectionRef.documents) {
                    val image = document.getString("image")
                    val text = document.getString("text")
                    image?.let {
                        imageMap.put("key$i", it)
                    }
                    text?.let {
                        randomFactContents.add(it)
                        randomFactContentsOriginal.add(it)
                    }
                    randomFactDocumentNames.add(document.id)
                    i++
                }
                numberConsideringSortRandomFact(randomFactDocumentNames) //correct number order in document names
                imageMapUrl = convertMapToStringMap(imageMap) as MutableMap<String, String>
                imageMapByteArray =
                    urlToByteArray(imageMapUrl) as MutableMap<String, ByteArray> //the one needed
                imageMapByteArrayOriginal =
                    imageMapByteArray.toMap() as MutableMap<String, ByteArray>

                isLoading = false // done loading
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (saveChangesButtonVisible) 82.dp else 0.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    Modifier
                        .align(Alignment.Center)
                        .size(25.dp),
                    color = colorResource(id = R.color.BSUIR_Blue),
                    strokeWidth = 3.dp
                )
            }
            Column(modifier = Modifier.fillMaxSize()) {
                CustomTopAppBar(title = "Факты")

                if (!isLoading) {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp
                            //bottom = if (saveChangesButtonVisible) 66.dp else 0.dp
                        )
                    ) {
                        item {
                            var subjectText by rememberSaveable { mutableStateOf(selectedSubject) }
                            SubjectSpinner(documentNames, subjectText) { newSubjectText ->
                                subjectText = newSubjectText
                            }
                        }
                        items(imageMapByteArray.size) { index ->
                            val imageData = imageMapByteArray["key$index"]
                            if (imageData != null) {
                                var text by remember { mutableStateOf(randomFactContents.get(index)) }
                                val BSUIR_Blue = colorResource(id = R.color.BSUIR_Blue)
                                val customTextSelectionColors = TextSelectionColors(
                                    handleColor = BSUIR_Blue,
                                    backgroundColor = BSUIR_Blue.copy(alpha = 0.3f)
                                )

                                var expanded by remember { mutableStateOf(false) }
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White)
                                        .padding(bottom = 16.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    elevation = 5.dp
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(Color.White)
                                            .padding(10.dp)
                                            .pointerInput(Unit) {
                                                detectTapGestures(
                                                    onLongPress = {
                                                        expanded = true
                                                    }
                                                )
                                            }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {

                                            Box() {

                                                //IMAGE
                                                val context = LocalContext.current
                                                var imageBitmap by remember {
                                                    mutableStateOf<Bitmap?>(
                                                        null
                                                    )
                                                }
                                                val launcher = rememberLauncherForActivityResult(
                                                    contract = ActivityResultContracts.GetContent(),
                                                    onResult = { uri ->
                                                        if (uri != null) {
                                                            val inputStream =
                                                                context.contentResolver.openInputStream(
                                                                    uri
                                                                )
                                                            imageBitmap =
                                                                BitmapFactory.decodeStream(
                                                                    inputStream
                                                                )
                                                            if (imageBitmap != null) {
                                                                val byteArray =
                                                                    bitmapToByteArray(imageBitmap!!)
                                                                imageMapByteArray["key$index"] =
                                                                    byteArray
                                                                saveChangesButtonVisible = true
                                                            }
                                                        }
                                                    }
                                                )

                                                Box(
                                                    modifier = Modifier
                                                        .size(64.dp)
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .clickable(onClick = { launcher.launch("image/*") })
                                                        .background(Color.LightGray)
                                                ) {
                                                    if (imageBitmap != null) {
                                                        Image(
                                                            bitmap = imageBitmap!!.asImageBitmap(),
                                                            contentDescription = "selectedImage",
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier
                                                                .fillMaxSize()
                                                                .clip(RoundedCornerShape(10.dp))
                                                        )
                                                    } else {
                                                        ByteArrayImage(
                                                            imageData = imageData,
                                                            contentDescription = "defaultImage"
                                                        )
                                                    }
                                                }
                                                //

                                            }
                                            /*ByteArrayImage(
                                                imageData = imageData,
                                                contentDescription = "randFactImage"
                                            )*/
                                            CompositionLocalProvider(LocalTextSelectionColors provides customTextSelectionColors) {
                                                TextField(
                                                    value = text,
                                                    onValueChange = {
                                                        text = it
                                                        randomFactContents[index] = text
                                                        saveChangesButtonVisible = true
                                                    },
                                                    label = { Text("") },
                                                    textStyle = TextStyle(
                                                        fontSize = 17.sp
                                                    ),
                                                    modifier = Modifier.padding(
                                                        bottom = 6.dp,
                                                        start = 6.dp,
                                                        end = 6.dp,
                                                        top = 5.dp
                                                    ),
                                                    colors = TextFieldDefaults.textFieldColors(
                                                        backgroundColor = Color.Transparent,
                                                        focusedIndicatorColor = Color.Transparent,
                                                        unfocusedIndicatorColor = Color.Transparent,
                                                        cursorColor = BSUIR_Blue
                                                    )
                                                )
                                            }
                                        }
                                    }
                                    BoxWithConstraints {
                                        val constraints = maxWidth - 16.dp // subtract any horizontal padding/margin
                                        DropdownMenu(
                                            expanded = expanded,
                                            onDismissRequest = { expanded = false },
                                            modifier = Modifier.widthIn(max = constraints).clip(
                                                RoundedCornerShape(10.dp))
                                        ) {
                                            DropdownMenuItem(onClick = {
                                                //There'll be an extra menu asking if a user is sure with a note that if during the current session
                                                //the image of the fact they want to delete was edited it won't work properly and they gotta save changes first

                                                //delete functionality here
                                                val collectionRef = Firebase.firestore.collection("RandomFact/$selectedSubject/$selectedSubject")
                                                val query = collectionRef.whereEqualTo("image", imageMapUrl["key$index"])
                                                query.get().addOnSuccessListener { documents ->
                                                    for (document in documents) {
                                                        // Delete the document
                                                        collectionRef.document(document.id).delete().addOnSuccessListener {
                                                            Toast.makeText(this@RandomFactEditActivity, "Document deleted successfully", Toast.LENGTH_SHORT).show()

                                                            // Deleting the image from Firebase Storage:
                                                            val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(
                                                                imageMapUrl["key$index"].toString()
                                                            )
                                                            storageRef.delete()
                                                                .addOnSuccessListener {
                                                                    Toast.makeText(this@RandomFactEditActivity, "Fact deleted from Firebase Storage successfully", Toast.LENGTH_SHORT).show()
                                                                    startActivity(Intent(this@RandomFactEditActivity, RandomFactEditActivity::class.java))
                                                                    finish()
                                                                }
                                                                .addOnFailureListener { e ->
                                                                    Toast.makeText(this@RandomFactEditActivity, "Failed to delete the fact from Firebase Firestore", Toast.LENGTH_SHORT).show()
                                                                }
                                                        }.addOnFailureListener {
                                                            Toast.makeText(this@RandomFactEditActivity, "Failed to delete document", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                }.addOnFailureListener {
                                                    Toast.makeText(this@RandomFactEditActivity, "Matching document not found", Toast.LENGTH_SHORT).show()
                                                }

                                                expanded = false
                                            }) {
                                                Text(text = stringResource(R.string.delete))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

        }
        if (saveChangesButtonVisible) {
            Box(modifier = Modifier.fillMaxSize()) {
                Card(
                    onClick = {
                        for (i in 0..imageMapByteArray.size) {
                            imageMapByteArray["key$i"]?.let {
                                saveChangesToFirebase(
                                    it,
                                    imageMapUrl,
                                    imageMapByteArray,
                                    i,
                                    imageMapByteArrayOriginal,
                                    randomFactContents,
                                    randomFactContentsOriginal
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp)),
                    elevation = 5.dp,
                    backgroundColor = colorResource(id = R.color.not_so_light_gray)
                ) {
                    Text(
                        text = stringResource(R.string.save_changes),
                        color = colorResource(id = R.color.dark_text),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }
    }


    fun numberConsideringSortRandomFact(list: MutableList<String>) {
        list.sortWith { s1, s2 ->
            val num1 = s1.substring(10).toInt()
            val num2 = s2.substring(10).toInt()
            num1 - num2
        }
    }


    fun saveChangesToFirebase(
        byteArray: ByteArray,
        imageMapUrl: MutableMap<String, String>,
        imageMapByteArray: MutableMap<String, ByteArray>,
        mapIndex: Int,
        imageMapByteArrayOriginal: MutableMap<String, ByteArray>,
        randomFactContents: MutableList<String>,
        randomFactContentsOriginal: MutableList<String>
    ) {
        val firestore = FirebaseFirestore.getInstance()
        if (byteArray.isNotEmpty()) {
            Log.d("saveChanges", "function launched")
            var shouldUpload = true

            for (i in 0 until imageMapByteArrayOriginal.size) {
                if (byteArray.contentEquals(imageMapByteArrayOriginal["key$i"])) {
                    shouldUpload = false
                }
            }

            Log.d("saveChanges", "shouldUpload Initialization worked")

            if (shouldUpload) {
                Log.d("saveChanges", "inside the if (shouldUpload)")

                //Replacing the relevant image if it's changed
                val storageRef = FirebaseStorage.getInstance()
                    .getReferenceFromUrl(imageMapUrl["key$mapIndex"].toString())

                storageRef.putBytes(byteArray).addOnSuccessListener { taskSnapshot ->

                    // Get the download URL of the newly uploaded image
                    taskSnapshot.metadata?.reference?.downloadUrl?.addOnSuccessListener { uri ->
                        val downloadUrl = uri.toString()
                        Log.d("imageUpdate", "New image URL: $downloadUrl")

                        //Replacing the image's url in the RandomFact firestore collection:
                        val collectionRef = FirebaseFirestore.getInstance().collection("RandomFact")
                            .document(selectedSubject).collection(selectedSubject)
                        val query = collectionRef.whereEqualTo("image", imageMapUrl["key$mapIndex"])

                        query.get().addOnSuccessListener { documents ->
                            for (document in documents) {
                                // Update the "image" field with the new image URL
                                document.reference.update("image", downloadUrl)
                                    .addOnSuccessListener {
                                        Toast.makeText(
                                            this,
                                            "Image ${mapIndex + 1} is successfully updated in Firebase Storage",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                imageMapByteArrayOriginal["key$mapIndex"] =
                                    imageMapByteArray["key$mapIndex"] as ByteArray
                            }
                        }.addOnFailureListener { exception ->
                            // Handle any errors that occur while querying the collection
                        }
                        // Store the new URL in your map of image URLs
                        imageMapUrl["key$mapIndex"] = downloadUrl
                    }
                }.addOnFailureListener { exception ->
                    // Handle any errors that occur while uploading the new image
                }

                /*val inputStream = ByteArrayInputStream(byteArray)
                val filename = "${UUID.randomUUID()}.jpg"
                val imageRef = storageRef.child(filename)

                imageRef.putStream(inputStream)
                    .addOnSuccessListener { taskSnapshot ->
                        Toast.makeText(this, "Image uploaded to Firebase Storage successfully", Toast.LENGTH_SHORT).show()
                        //If it's succeeded than new one becomes original:
                        imageMapByteArrayOriginal["key$mapIndex"] = imageMapByteArray["key$mapIndex"] as ByteArray

                        // Image upload success
                        // Do something here, like display a success message
                    }
                    .addOnFailureListener { exception ->
                        Toast.makeText(this, "Failed to upload the image to Firebase Storage", Toast.LENGTH_SHORT).show()
                        // Image upload failed
                        // Handle the error here
                    }*/
            }

            for (i in 0 until randomFactContentsOriginal.size) {
                if (randomFactContents[i] != randomFactContentsOriginal[i]) {
                    val collectionRef =
                        firestore.collection("RandomFact/$selectedSubject/$selectedSubject")
// Find the document that contains the original content
                    val query = collectionRef.whereEqualTo("text", randomFactContentsOriginal[i])
                    query.get().addOnSuccessListener { querySnapshot ->
                        for (document in querySnapshot.documents) {
                            // Replace the original content with the updated content
                            document.reference.update("text", randomFactContents[i])
                                .addOnSuccessListener {
                                    randomFactContentsOriginal[i] = randomFactContents[i]
                                }
                        }
                    }.addOnFailureListener { exception ->
                        // Handle any errors that occur while querying the database
                        Log.e("Firestore", "Error getting documents.", exception)
                    }
                }
            }
        }
    }


    fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        return outputStream.toByteArray()
    }


    @Composable
    fun ByteArrayImage(imageData: ByteArray, contentDescription: String?) {
        val imageBitmap = remember {
            val inputStream: InputStream = ByteArrayInputStream(imageData)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            bitmap.asImageBitmap()
        }

        Image(
            painter = BitmapPainter(imageBitmap),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(10.dp))
        )
    }


    private fun convertMapToStringMap(imageMap: Map<String, Any>): Map<String, String> {
        val result = mutableMapOf<String, String>()
        for ((key, value) in imageMap) {
            if (value is String) {
                result[key] = value
            } else {
                result[key] = value.toString()
            }
        }
        return result
    }


    private fun urlToByteArray(imageUrls: Map<String, String>): Map<String, ByteArray> {
        val result = mutableMapOf<String, ByteArray>()
        val latch =
            CountDownLatch(imageUrls.size) // create a countdown latch with the number of image URLs

        for ((key, url) in imageUrls) {
            Thread({
                val connection = URL(url).openConnection()
                connection.connect()
                val inputStream = connection.getInputStream()
                val buffer = ByteArrayOutputStream()
                inputStream.use { input ->
                    buffer.use { output ->
                        input.copyTo(output)
                    }
                }
                result[key] = buffer.toByteArray()
                latch.countDown() // decrease the latch count
            }).start()
            Log.d("imageConverted", "imageConverted")
        }

        latch.await() // wait for all the threads to finish

        return result
    }


    @Composable
    fun SubjectSpinner(subjects: List<String>, subjectText: String, onSubjectTextChanged: (String) -> Unit) {
        var expanded by remember { mutableStateOf(false) }

        Box(Modifier.fillMaxWidth()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, top = 10.dp, bottom = 10.dp)
                    .clickable {
                        expanded = !expanded
                    }
                    .wrapContentWidth(Alignment.CenterHorizontally),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = subjectText, fontSize = 17.sp, modifier = Modifier.padding(end = 8.dp))
                Icon(imageVector = Icons.Filled.ArrowDropDown, contentDescription = "ArrowDropDown")
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    subjects.forEach { subjects ->
                        DropdownMenuItem(onClick = {
                            expanded = false
                            onSubjectTextChanged(subjects)
                            val editor = pref.edit()
                            editor.putString("selectedSubject", subjects)
                            editor.apply()
                            startActivity(Intent(this@RandomFactEditActivity, RandomFactEditActivity::class.java))
                            finish()
                        }) {
                            Text(text = subjects)
                        }
                    }
                }
            }
        }
    }


    @Composable
    fun CustomTopAppBar(title: String) {
        var showAddElement by remember { mutableStateOf(false) }

        val backgroundColor = colorResource(id = R.color.BSUIR_Blue)

        TopAppBar(
            title = {
                Text(
                    text = title,
                    color = Color.White
                )
            },
            backgroundColor = backgroundColor,
            navigationIcon = {
                IconButton(onClick = { onBackPressedDispatcher.onBackPressed() }) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_baseline_arrow_back_24),
                        contentDescription = "Back",
                        tint = Color.White,
                    )
                }
            },
            actions = {
                IconButton(onClick = { showAddElement = true }) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_baseline_add_24),
                        contentDescription = "Add",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        )

        if (showAddElement) {
            // Call the composable you want to show dynamically here
            val context = LocalContext.current
            var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }
            val launcher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent(),
                onResult = { uri ->
                    if (uri != null) {
                        val inputStream = context.contentResolver.openInputStream(uri)
                        imageBitmap = BitmapFactory.decodeStream(inputStream)
                    }
                }
            )

            var addFactTextFieldEmpty by remember { mutableStateOf(true) }
            var fieldsEmptyError by remember { mutableStateOf(false) }
            val fieldsEmptyErrorMessage by remember { mutableStateOf("Fields cannot be empty") }
            lateinit var content: String
            var dialogLoadingVisible by remember { mutableStateOf(false) }
            AlertDialog(
                onDismissRequest = { showAddElement = false },
                modifier = Modifier.clip(RoundedCornerShape(10.dp)),
                title = { Text(text = "") },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .width(172.dp)
                                .height(100.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(onClick = { launcher.launch("image/*") })
                                .background(colorResource(id = R.color.lighter_gray))
                                .align(Alignment.CenterHorizontally) // center horizontally
                        ) {
                            if (imageBitmap != null) {
                                Image(
                                    bitmap = imageBitmap!!.asImageBitmap(),
                                    contentDescription = "Selected Image",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(10.dp))
                                )
                            } else {
                                Text(
                                    text = stringResource(R.string.click_to_select_image),
                                    modifier = Modifier.align(Alignment.Center)
                                        .padding(16.dp),
                                    color = Color.Gray
                                )
                            }
                        }

                        // Large text field
                        var text by remember { mutableStateOf("") }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                                .height(200.dp) // Set the height of the Box to a larger value than the TextField
                                .width(120.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(colorResource(id = R.color.not_so_light_gray))
                        ) {
                            TextField(
                                value = text,
                                onValueChange = {
                                    text = it
                                    addFactTextFieldEmpty = text.trim().isEmpty()
                                    if (!addFactTextFieldEmpty && imageBitmap != null)
                                        fieldsEmptyError = false
                                    content = text
                                },
                                label = {
                                    Text(
                                        text = stringResource(R.string.enter_text),
                                        color = if (text.isNotEmpty()) colorResource(id = R.color.BSUIR_Blue) else Color.Gray
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        color = colorResource(
                                            id = R.color.white // set the new background color here
                                        )
                                    )
                                    .clip(RoundedCornerShape(10.dp)), // apply the RoundedCornerShape to the TextField
                                textStyle = TextStyle(fontSize = 17.sp),
                                colors = TextFieldDefaults.textFieldColors(
                                    backgroundColor = colorResource(R.color.lighter_gray),
                                    textColor = colorResource(id = R.color.dark_text),
                                    cursorColor = colorResource(id = R.color.BSUIR_Blue),
                                    focusedIndicatorColor = colorResource(id = R.color.transparent),
                                    unfocusedIndicatorColor = colorResource(id = R.color.transparent),
                                    disabledTextColor = colorResource(id = R.color.light_gray),
                                    trailingIconColor = colorResource(id = R.color.BSUIR_Blue)
                                )
                            )
                        }
                        if (fieldsEmptyError) {
                            Text(
                                text = fieldsEmptyErrorMessage,
                                color = colorResource(id = R.color.incorrect_answer_red),
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .padding(top = 3.dp)
                            )
                        }
                    }
                },
                buttons = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 6.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Button(
                            onClick = { showAddElement = false },
                            modifier = Modifier
                                .padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = colorResource(id = R.color.not_so_light_gray),
                                contentColor = colorResource(id = R.color.dark_text)
                            )
                        ) {
                            Text(text = stringResource(R.string.cancel))
                        }
                        var addBtnEnabled by remember { mutableStateOf(true) }
                        Button(
                            enabled = addBtnEnabled,
                            onClick = {
                                //checking if fields are empty:
                                if (imageBitmap == null || addFactTextFieldEmpty) {
                                    fieldsEmptyError = true
                                    addBtnEnabled = true
                                }
                                else {
                                    dialogLoadingVisible = true
                                    addBtnEnabled = false
                                    lateinit var docName: String
                                    var number = 1
                                    for (name in randomFactDocumentNames) {
                                        Log.d("randomFactDocumentNames", name)
                                        if (name.substring(10).toInt() != number++) {
                                            docName = "RandomFact${number - 1}"
                                            break
                                        } else {
                                            if (name.substring(10).toInt() == randomFactDocumentNames.size) {
                                                docName = "RandomFact${number}"
                                                break
                                            }
                                        }
                                    }

                                    val storageRef = Firebase.storage.reference.child("$selectedSubject Images/RandomFact/${System.currentTimeMillis()}.jpg")
                                    val baos = ByteArrayOutputStream()
                                    imageBitmap!!.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                                    val data = baos.toByteArray()

                                    val uploadTask = storageRef.putBytes(data)
                                    uploadTask.continueWithTask { task ->
                                        if (!task.isSuccessful) {
                                            task.exception?.let {
                                                throw it
                                            }
                                        }
                                        storageRef.downloadUrl
                                    }.addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            val downloadUri = task.result
                                            val db = Firebase.firestore
                                            val factRef = db.collection("RandomFact").document(selectedSubject).collection(selectedSubject).document(docName)

                                            val factData = hashMapOf(
                                                "image" to downloadUri.toString(),
                                                "text" to content
                                            )

                                            factRef.set(factData)
                                                .addOnSuccessListener {
                                                    Toast.makeText(this@RandomFactEditActivity, "Fact added successfully", Toast.LENGTH_SHORT).show()
                                                    showAddElement = false
                                                    addBtnEnabled = true
                                                    dialogLoadingVisible = false
                                                    //recreate()
                                                    startActivity(Intent(this@RandomFactEditActivity, RandomFactEditActivity::class.java))
                                                    finish()
                                                }
                                                .addOnFailureListener { e ->
                                                    addBtnEnabled = true
                                                    dialogLoadingVisible = false
                                                    Toast.makeText(this@RandomFactEditActivity, "Failed to add fact", Toast.LENGTH_SHORT).show()
                                                    // Handle the error
                                                }
                                        } else {
                                            // Handle failures
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = colorResource(id = R.color.not_so_light_gray),
                                contentColor = colorResource(id = R.color.dark_text)
                            )
                        ) {
                            if (!dialogLoadingVisible)
                                Text(text = stringResource(R.string.add))
                            if (dialogLoadingVisible) {
                                CircularProgressIndicator(
                                    Modifier
                                        .size(15.dp),
                                    color = colorResource(id = R.color.BSUIR_Blue),
                                    strokeWidth = 1.5.dp
                                )
                            }
                        }
                    }
                }
            )
        }
    }
}