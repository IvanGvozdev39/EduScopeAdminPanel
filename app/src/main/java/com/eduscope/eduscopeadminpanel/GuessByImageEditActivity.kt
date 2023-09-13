package com.eduscope.eduscopeadminpanel

import android.annotation.SuppressLint
import android.content.Intent
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
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
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.URL
import java.util.concurrent.CountDownLatch

class GuessByImageEditActivity : ComponentActivity() {
    private lateinit var selectedSubject: String
    private lateinit var selectedMode: String
    private var selectedSet = 0
    private var documentNames: MutableList<String> = mutableListOf()
    private var imageMap: MutableMap<String, Any> = mutableMapOf()
    private var imageMapUrl: MutableMap<String, String> = mutableMapOf()
    private var imageMapByteArray: MutableMap<String, ByteArray> = mutableMapOf()
    private var imageMapByteArrayOriginal: MutableMap<String, ByteArray> = mutableMapOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedSubject = intent.getStringExtra("selectedSubject").toString()
        selectedMode = intent.getStringExtra("selectedMode").toString()
        selectedSet = intent.getIntExtra("selectedSet", 1)
        setContent {
            ContentScreen()
        }
    }


    @SuppressLint("MutableCollectionMutableState")
    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    fun ContentScreen() {
        val firestore = FirebaseFirestore.getInstance()
        val imageNotMap by remember { mutableStateOf(mutableListOf<String>()) }
        val option1s by remember { mutableStateOf(mutableListOf<String>()) }
        val option1sOriginal by remember { mutableStateOf(mutableListOf<String>()) }
        val option2s by remember { mutableStateOf(mutableListOf<String>()) }
        val option2sOriginal by remember { mutableStateOf(mutableListOf<String>()) }
        val option3s by remember { mutableStateOf(mutableListOf<String>()) }
        val option3sOriginal by remember { mutableStateOf(mutableListOf<String>()) }
        val option4s by remember { mutableStateOf(mutableListOf<String>()) }
        val option4sOriginal by remember { mutableStateOf(mutableListOf<String>()) }
        val answers by remember { mutableStateOf(mutableListOf<String>()) }
        val answersOriginal by remember { mutableStateOf(mutableListOf<String>()) }
        var isLoading by remember { mutableStateOf(true) }
        var saveChangesButtonVisible by remember { mutableStateOf(false) }
        var numberOfQuestions by remember { mutableStateOf(0) }
        val imageEditableTakenFromStorage by remember { mutableStateOf(mutableListOf<Boolean>()) }
        val imageEditableUrlTakenFromStorage by remember { mutableStateOf(mutableListOf<String>()) }


        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {

                val colRefQuestionsNum =
                    firestore.collection("Questions/$selectedSubject/$selectedSubject/$selectedMode/QuestionSet$selectedSet")
                colRefQuestionsNum.get().addOnSuccessListener { documents ->
                    numberOfQuestions = documents.size()
                    for (i in 1..numberOfQuestions) {
                        val docRef =
                            firestore.document("Questions/$selectedSubject/$selectedSubject/$selectedMode/QuestionSet$selectedSet/Question$i")

                        docRef.get().addOnSuccessListener { document ->
                            if (document.exists()) {
                                val question = document.get("question").toString()
                                imageNotMap.add(question)
                                //imageMap.put("key$i", question)
                                val option1 = document.get("option1").toString()
                                option1s.add(option1)
                                option1sOriginal.add(option1)
                                val option2 = document.get("option2").toString()
                                option2s.add(option2)
                                option2sOriginal.add(option2)
                                val option3 = document.get("option3").toString()
                                option3s.add(option3)
                                option3sOriginal.add(option3)
                                val option4 = document.get("option4").toString()
                                option4s.add(option4)
                                option4sOriginal.add(option4)
                                val answer = document.get("answer").toString()
                                answers.add(answer)
                                answersOriginal.add(answer)
                                documentNames.add(document.id)

                                if (document.id == "Question$numberOfQuestions") {
                                    numberConsideringSortSummary(this@GuessByImageEditActivity.documentNames) //correct number order in document names
                                    for (i in 0 until imageNotMap.size) {
                                        imageEditableTakenFromStorage.add(false)
                                        imageEditableUrlTakenFromStorage.add("")
                                        imageMap.put("key$i", imageNotMap[i])
                                    }
                                    imageMapUrl =
                                        convertMapToStringMap(imageMap) as MutableMap<String, String>
                                    imageMapByteArray =
                                        urlToByteArray(imageMapUrl) as MutableMap<String, ByteArray> //the one needed
                                    imageMapByteArrayOriginal =
                                        imageMapByteArray.toMap() as MutableMap<String, ByteArray>
                                    isLoading = false // done loading
                                }
                            } else {
                                if (document.id == "Question$numberOfQuestions") {
                                    numberConsideringSortSummary(this@GuessByImageEditActivity.documentNames) //correct number order in document names
                                    imageMapUrl =
                                        convertMapToStringMap(imageMap) as MutableMap<String, String>
                                    imageMapByteArray =
                                        urlToByteArray(imageMapUrl) as MutableMap<String, ByteArray> //the one needed
                                    imageMapByteArrayOriginal =
                                        imageMapByteArray.toMap() as MutableMap<String, ByteArray>
                                    isLoading = false // done loading
                                }
                            }
                        }
                    }
                }
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
                CustomTopAppBar(title = "$selectedSubject / $selectedMode / Сет $selectedSet")

                if (!isLoading) {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 16.dp
                            //bottom = if (saveChangesButtonVisible) 66.dp else 0.dp
                        )
                    ) {
                        items(imageMapByteArray.size) { index ->
                            val imageData = imageMapByteArray.get("key$index")
                            if (imageData != null) {
                                var option1Text by remember { mutableStateOf(option1sOriginal[index]) }
                                var option2Text by remember { mutableStateOf(option2sOriginal[index]) }
                                var option3Text by remember { mutableStateOf(option3sOriginal[index]) }
                                var option4Text by remember { mutableStateOf(option4sOriginal[index]) }
                                var answerText by remember { mutableStateOf(answersOriginal[index]) }
                                val BSUIR_Blue = colorResource(id = R.color.BSUIR_Blue)
                                val customTextSelectionColors = TextSelectionColors(
                                    handleColor = BSUIR_Blue,
                                    backgroundColor = BSUIR_Blue.copy(alpha = 0.3f)
                                )

                                var expanded by remember { mutableStateOf(false) }
                                var showStorageImageDeleteDialog by remember { mutableStateOf(false) }
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White)
                                        .padding(
                                            bottom = 16.dp,
                                            top = if (index == 0) 16.dp else 0.dp
                                        ),
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

                                        var imageSelectedFromStorage by remember {
                                            mutableStateOf(
                                                false
                                            )
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box() {

                                                //IMAGE
                                                val context = LocalContext.current
                                                var imageBitmap by remember {
                                                    mutableStateOf<Bitmap?>(
                                                        null
                                                    )
                                                }
                                                val launcher =
                                                    rememberLauncherForActivityResult(
                                                        contract = ActivityResultContracts.GetContent(),
                                                        onResult = { uri ->
                                                            if (uri != null) {
                                                                val inputStream =
                                                                    context.contentResolver.openInputStream(
                                                                        uri
                                                                    )
                                                                imageSelectedFromStorage = false
                                                                imageBitmap =
                                                                    BitmapFactory.decodeStream(
                                                                        inputStream
                                                                    )
                                                                if (imageBitmap != null) {
                                                                    val byteArray =
                                                                        bitmapToByteArray(
                                                                            imageBitmap!!
                                                                        )
                                                                    imageMapByteArray["key$index"] =
                                                                        byteArray
                                                                    saveChangesButtonVisible =
                                                                        true
                                                                }
                                                                imageEditableTakenFromStorage[index] =
                                                                    false
                                                                imageEditableUrlTakenFromStorage[index] = ""
                                                            }
                                                        }
                                                    )

                                                var showStorageGalleryDialog by remember {
                                                    mutableStateOf(
                                                        false
                                                    )
                                                }
                                                val imageList by remember {
                                                    mutableStateOf(
                                                        mutableListOf<ByteArray>()
                                                    )
                                                }
                                                var imageFromStorage by remember {
                                                    mutableStateOf<ByteArray?>(
                                                        null
                                                    )
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .size(64.dp)
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .clickable(onClick = {
                                                            showStorageGalleryDialog = true
                                                            //launcher.launch("image/*")
                                                        })
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
                                                    } else if (imageSelectedFromStorage) {
                                                        ByteArrayImage(
                                                            imageData = imageFromStorage!!,
                                                            contentDescription = "image from storage"
                                                        )
                                                    } else {
                                                        ByteArrayImage(
                                                            imageData = imageData,
                                                            contentDescription = "defaultImage"
                                                        )
                                                    }
                                                }

                                                var showStorageImageGrid by remember {
                                                    mutableStateOf(
                                                        false
                                                    )
                                                }
                                                var storageLoading by remember {
                                                    mutableStateOf(
                                                        false
                                                    )
                                                }
                                                val imageUrlList by remember {
                                                    mutableStateOf(
                                                        mutableListOf<String>()
                                                    )
                                                }
                                                if (showStorageGalleryDialog) {
                                                    AlertDialog(
                                                        modifier = Modifier.clip(
                                                            RoundedCornerShape(
                                                                10.dp
                                                            )
                                                        ),
                                                        onDismissRequest = {
                                                            showStorageGalleryDialog = false
                                                        },
                                                        title = { Text(text = "") },
                                                        buttons = {
                                                            Column(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .padding(bottom = 45.dp),
                                                                horizontalAlignment = Alignment.CenterHorizontally
                                                            ) {
                                                                Button(
                                                                    onClick = {
                                                                        storageLoading = true
                                                                        val storageRef =
                                                                            FirebaseStorage.getInstance()
                                                                                .getReference("Philosophy Images/Scientists")
                                                                        storageRef.listAll()
                                                                            .addOnSuccessListener { listResult ->
                                                                                val maxImages =
                                                                                    listResult.items.size
                                                                                var currentImage = 0
                                                                                listResult.items.forEach { item ->
                                                                                    item.getBytes(
                                                                                        Long.MAX_VALUE
                                                                                    )
                                                                                        .addOnSuccessListener { bytes ->
                                                                                            imageList.add(
                                                                                                bytes
                                                                                            )
                                                                                            currentImage++
                                                                                            item.downloadUrl.addOnSuccessListener { uri ->
                                                                                                imageUrlList.add(
                                                                                                    uri.toString()
                                                                                                )
                                                                                                if (currentImage == maxImages) {
                                                                                                    showStorageImageGrid =
                                                                                                        true
                                                                                                    showStorageGalleryDialog =
                                                                                                        false
                                                                                                    storageLoading =
                                                                                                        false
                                                                                                }
                                                                                            }
                                                                                        }
                                                                                }
                                                                            }
                                                                    },
                                                                    modifier = Modifier
                                                                        .width(225.dp)
                                                                        .padding(bottom = 2.dp)
                                                                        .clip(
                                                                            RoundedCornerShape(10.dp)
                                                                        ),
                                                                    colors = ButtonDefaults.buttonColors(
                                                                        backgroundColor = colorResource(
                                                                            id = R.color.not_so_light_gray
                                                                        ),
                                                                        contentColor = colorResource(
                                                                            id = R.color.dark_text
                                                                        )
                                                                    )
                                                                ) {
                                                                    if (storageLoading) {
                                                                        CircularProgressIndicator(
                                                                            Modifier
                                                                                .size(18.dp),
                                                                            color = colorResource(id = R.color.BSUIR_Blue),
                                                                            strokeWidth = 2.dp
                                                                        )
                                                                    } else
                                                                        Text(
                                                                            text = "Из Firebase Storage",
                                                                            fontSize = 16.sp
                                                                        )
                                                                }
                                                                Button(
                                                                    onClick = {
                                                                        launcher.launch("image/*")
                                                                        showStorageGalleryDialog =
                                                                            false
                                                                    },
                                                                    modifier = Modifier
                                                                        .width(225.dp)
                                                                        .padding(top = 2.dp)
                                                                        .clip(
                                                                            RoundedCornerShape(10.dp)
                                                                        ),
                                                                    colors = ButtonDefaults.buttonColors(
                                                                        backgroundColor = colorResource(
                                                                            id = R.color.not_so_light_gray
                                                                        ),
                                                                        contentColor = colorResource(
                                                                            id = R.color.dark_text
                                                                        )
                                                                    )
                                                                ) {
                                                                    Text(
                                                                        text = "Из галереи",
                                                                        fontSize = 16.sp
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    )
                                                }

                                                if (showStorageImageGrid) {
                                                    AlertDialog(
                                                        onDismissRequest = {
                                                            showStorageImageGrid = false
                                                        },
                                                        title = { },
                                                        text = {
                                                            LazyVerticalGrid(
                                                                columns = GridCells.Fixed(3),
                                                                contentPadding = PaddingValues(
                                                                    horizontal = 8.dp,
                                                                    vertical = 8.dp
                                                                ),
                                                                verticalArrangement = Arrangement.spacedBy(
                                                                    16.dp
                                                                ),
                                                                horizontalArrangement = Arrangement.spacedBy(
                                                                    16.dp
                                                                )
                                                            ) {
                                                                items(imageList.size) { index2 ->
                                                                    val image = imageList[index2]
                                                                    val imageBitmapLocal =
                                                                        BitmapFactory.decodeByteArray(
                                                                            image,
                                                                            0,
                                                                            image.size
                                                                        ).asImageBitmap()
                                                                    Image(
                                                                        bitmap = imageBitmapLocal,
                                                                        contentDescription = null,
                                                                        contentScale = ContentScale.Crop,
                                                                        modifier = Modifier
                                                                            .height(100.dp)
                                                                            .width(100.dp)
                                                                            .clip(
                                                                                RoundedCornerShape(
                                                                                    10.dp
                                                                                )
                                                                            )
                                                                            .clickable {
                                                                                imageEditableTakenFromStorage[index] = true
                                                                                imageEditableUrlTakenFromStorage[index] = imageUrlList[index2]
                                                                                imageFromStorage =
                                                                                    imageList[index2]
                                                                                //val byteArray =
                                                                                //    bitmapToByteArray(imageBitmapLocal.asAndroidBitmap())
                                                                                imageMapByteArray["key$index"] =
                                                                                    imageFromStorage!!
                                                                                imageSelectedFromStorage =
                                                                                    true
                                                                                showStorageImageGrid =
                                                                                    false
                                                                                imageBitmap = BitmapFactory.decodeByteArray(imageFromStorage, 0, imageFromStorage!!.size)
                                                                                saveChangesButtonVisible = true
                                                                            }
                                                                    )
                                                                }
                                                            }
                                                        },
                                                        confirmButton = {
                                                            TextButton(onClick = {
                                                                showStorageImageGrid = false
                                                            }) {
                                                                Text(
                                                                    text = "Отмена",
                                                                    color = colorResource(R.color.BSUIR_Blue),
                                                                    fontSize = 17.sp
                                                                )
                                                            }
                                                        }
                                                    )
                                                }

                                            }

                                            CompositionLocalProvider(LocalTextSelectionColors provides customTextSelectionColors) {
                                                Column(
                                                    modifier = Modifier.padding(
                                                        start = 10.dp,
                                                        end = 10.dp
                                                    ),
                                                    verticalArrangement = Arrangement.spacedBy((-15).dp)
                                                ) {
                                                    TextField(
                                                        value = option1Text,
                                                        onValueChange = {
                                                            option1Text = it
                                                            option1s[index] = option1Text
                                                            saveChangesButtonVisible = true
                                                        },
                                                        label = { Text("") },
                                                        textStyle = TextStyle(
                                                            fontSize = 17.sp
                                                        ),
                                                        colors = TextFieldDefaults.textFieldColors(
                                                            backgroundColor = Color.Transparent,
                                                            focusedIndicatorColor = Color.Transparent,
                                                            unfocusedIndicatorColor = Color.Transparent,
                                                            cursorColor = BSUIR_Blue
                                                        )
                                                    )
                                                    TextField(
                                                        value = option2Text,
                                                        onValueChange = {
                                                            option2Text = it
                                                            option2s[index] = option2Text
                                                            saveChangesButtonVisible = true
                                                        },
                                                        label = { Text("") },
                                                        textStyle = TextStyle(
                                                            fontSize = 17.sp,
                                                        ),
                                                        colors = TextFieldDefaults.textFieldColors(
                                                            backgroundColor = Color.Transparent,
                                                            focusedIndicatorColor = Color.Transparent,
                                                            unfocusedIndicatorColor = Color.Transparent,
                                                            cursorColor = BSUIR_Blue
                                                        )
                                                    )
                                                    TextField(
                                                        value = option3Text,
                                                        onValueChange = {
                                                            option3Text = it
                                                            option3s[index] = option3Text
                                                            saveChangesButtonVisible = true
                                                        },
                                                        label = { Text("") },
                                                        textStyle = TextStyle(
                                                            fontSize = 17.sp
                                                        ),
                                                        colors = TextFieldDefaults.textFieldColors(
                                                            backgroundColor = Color.Transparent,
                                                            focusedIndicatorColor = Color.Transparent,
                                                            unfocusedIndicatorColor = Color.Transparent,
                                                            cursorColor = BSUIR_Blue
                                                        )
                                                    )
                                                    TextField(
                                                        value = option4Text,
                                                        onValueChange = {
                                                            option4Text = it
                                                            option4s[index] = option4Text
                                                            saveChangesButtonVisible = true
                                                        },
                                                        label = { Text("") },
                                                        textStyle = TextStyle(
                                                            fontSize = 17.sp
                                                        ),
                                                        colors = TextFieldDefaults.textFieldColors(
                                                            backgroundColor = Color.Transparent,
                                                            focusedIndicatorColor = Color.Transparent,
                                                            unfocusedIndicatorColor = Color.Transparent,
                                                            cursorColor = BSUIR_Blue
                                                        )
                                                    )
                                                    TextField(
                                                        value = answerText,
                                                        onValueChange = {
                                                            answerText = it
                                                            answers[index] = answerText
                                                            saveChangesButtonVisible = true
                                                        },
                                                        label = { Text("") },
                                                        textStyle = TextStyle(
                                                            fontSize = 17.sp,
                                                            fontWeight = FontWeight.Medium
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
                                    }
                                    BoxWithConstraints {
                                        val constraints =
                                            maxWidth - 16.dp // subtract any horizontal padding/margin
                                        DropdownMenu(
                                            expanded = expanded,
                                            onDismissRequest = { expanded = false },
                                            modifier = Modifier
                                                .widthIn(max = constraints)
                                                .clip(
                                                    RoundedCornerShape(10.dp)
                                                )
                                        ) {
                                            DropdownMenuItem(onClick = {
                                                showStorageImageDeleteDialog = true
                                            }) {
                                                Text(text = stringResource(R.string.delete))
                                            }
                                        }
                                    }
                                }


                                var dialogLoadingVisible by remember { mutableStateOf(false) }
                                var isDeleteNoLoading by remember { mutableStateOf(false) }
                                if (showStorageImageDeleteDialog) {
                                    AlertDialog(
                                        onDismissRequest = { showStorageImageDeleteDialog = false },
                                        modifier = Modifier.clip(RoundedCornerShape(10.dp)),
                                        title = { Text(text = "") },
                                        text = {
                                            Text(
                                                text = "Хотите ли также удалить изображение из Firebase Storage?",
                                                fontSize = 18.sp
                                            )
                                        },
                                        buttons = {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(end = 6.dp),
                                                horizontalArrangement = Arrangement.End,
                                            ) {
                                                Button(
                                                    onClick = {
                                                        isDeleteNoLoading = true
                                                        val collectionRef =
                                                            Firebase.firestore.collection("Questions/$selectedSubject/$selectedSubject/$selectedMode/QuestionSet$selectedSet")
                                                        val query = collectionRef.whereEqualTo(
                                                            "question",
                                                            imageMapUrl["key$index"]
                                                        )
                                                        query.get()
                                                            .addOnSuccessListener { documents ->
                                                                for (document in documents) {
                                                                    // Delete the document
                                                                    collectionRef.document(document.id)
                                                                        .delete()
                                                                        .addOnSuccessListener {
                                                                            Toast.makeText(
                                                                                this@GuessByImageEditActivity,
                                                                                "Document deleted successfully",
                                                                                Toast.LENGTH_SHORT
                                                                            ).show()

                                                                            val i = Intent(
                                                                                this@GuessByImageEditActivity,
                                                                                GuessByImageEditActivity::class.java
                                                                            )
                                                                            i.putExtra(
                                                                                "selectedSubject",
                                                                                selectedSubject
                                                                            )
                                                                            i.putExtra(
                                                                                "selectedMode",
                                                                                selectedMode
                                                                            )
                                                                            i.putExtra(
                                                                                "selectedSet",
                                                                                selectedSet
                                                                            )
                                                                            startActivity(i)
                                                                            finish()
                                                                        }.addOnFailureListener {
                                                                            Toast.makeText(
                                                                                this@GuessByImageEditActivity,
                                                                                "Failed to delete document",
                                                                                Toast.LENGTH_SHORT
                                                                            ).show()
                                                                        }
                                                                }
                                                                showStorageImageDeleteDialog = false
                                                            }.addOnFailureListener {
                                                                Toast.makeText(
                                                                    this@GuessByImageEditActivity,
                                                                    "Matching document not found",
                                                                    Toast.LENGTH_SHORT
                                                                ).show()
                                                                showStorageImageDeleteDialog = false
                                                            }
                                                    },
                                                    modifier = Modifier
                                                        .padding(
                                                            start = 8.dp,
                                                            end = 8.dp,
                                                            bottom = 8.dp
                                                        ),
                                                    colors = ButtonDefaults.buttonColors(
                                                        backgroundColor = colorResource(id = R.color.not_so_light_gray),
                                                        contentColor = colorResource(id = R.color.dark_text)
                                                    )
                                                ) {
                                                    if (!isDeleteNoLoading)
                                                        Text(text = "Нет")
                                                    else {
                                                        CircularProgressIndicator(
                                                            Modifier
                                                                .size(15.dp),
                                                            color = colorResource(id = R.color.BSUIR_Blue),
                                                            strokeWidth = 1.5.dp
                                                        )
                                                    }
                                                }
                                                val addBtnEnabled by remember { mutableStateOf(true) }
                                                Button(
                                                    enabled = addBtnEnabled,
                                                    onClick = {
                                                        dialogLoadingVisible =
                                                            true//delete functionality here
                                                        val collectionRef =
                                                            Firebase.firestore.collection("Questions/$selectedSubject/$selectedSubject/$selectedMode/QuestionSet$selectedSet")
                                                        val query = collectionRef.whereEqualTo(
                                                            "question",
                                                            imageMapUrl["key$index"]
                                                        )
                                                        query.get()
                                                            .addOnSuccessListener { documents ->
                                                                for (document in documents) {
                                                                    // Delete the document
                                                                    collectionRef.document(document.id)
                                                                        .delete()
                                                                        .addOnSuccessListener {
                                                                            Toast.makeText(
                                                                                this@GuessByImageEditActivity,
                                                                                "Document deleted successfully",
                                                                                Toast.LENGTH_SHORT
                                                                            ).show()

                                                                            // Deleting the image from Firebase Storage:
                                                                            val storageRef =
                                                                                FirebaseStorage.getInstance()
                                                                                    .getReferenceFromUrl(
                                                                                        imageMapUrl["key$index"].toString()
                                                                                    )
                                                                            storageRef.delete()
                                                                                .addOnSuccessListener {
                                                                                    showStorageImageDeleteDialog =
                                                                                        false
                                                                                    Toast.makeText(
                                                                                        this@GuessByImageEditActivity,
                                                                                        "Question deleted from Firebase Storage successfully",
                                                                                        Toast.LENGTH_SHORT
                                                                                    ).show()
                                                                                    val i = Intent(
                                                                                        this@GuessByImageEditActivity,
                                                                                        GuessByImageEditActivity::class.java
                                                                                    )
                                                                                    i.putExtra(
                                                                                        "selectedSubject",
                                                                                        selectedSubject
                                                                                    )
                                                                                    i.putExtra(
                                                                                        "selectedMode",
                                                                                        selectedMode
                                                                                    )
                                                                                    i.putExtra(
                                                                                        "selectedSet",
                                                                                        selectedSet
                                                                                    )
                                                                                    startActivity(i)
                                                                                    finish()
                                                                                }
                                                                        }
                                                                        .addOnFailureListener { e ->
                                                                            Toast.makeText(
                                                                                this@GuessByImageEditActivity,
                                                                                "Failed to delete the fact from Firebase Firestore",
                                                                                Toast.LENGTH_SHORT
                                                                            ).show()
                                                                        }
                                                                }
                                                            }.addOnFailureListener {
                                                                Toast.makeText(
                                                                    this@GuessByImageEditActivity,
                                                                    "Matching document not found",
                                                                    Toast.LENGTH_SHORT
                                                                ).show()
                                                                showStorageImageDeleteDialog = false
                                                            }
                                                    },
                                                    modifier = Modifier
                                                        .padding(
                                                            start = 8.dp,
                                                            end = 8.dp,
                                                            bottom = 8.dp
                                                        ),
                                                    colors = ButtonDefaults.buttonColors(
                                                        backgroundColor = colorResource(id = R.color.not_so_light_gray),
                                                        contentColor = colorResource(id = R.color.dark_text)
                                                    )
                                                ) {
                                                    if (!dialogLoadingVisible)
                                                        Text(text = "Да")
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
                    }
                }
            }

        }
        if (saveChangesButtonVisible) {
            Box(modifier = Modifier.fillMaxSize()) {
                Card(
                    onClick = {
                        for (i in 0..imageMapByteArray.size) {
                            saveChangesToFirebase(
                                i,
                                imageMapUrl,
                                imageMapByteArray,
                                imageMapByteArrayOriginal,
                                option1s,
                                option1sOriginal,
                                option2s,
                                option2sOriginal,
                                option3s,
                                option3sOriginal,
                                option4s, option4sOriginal,
                                answers,
                                answersOriginal,
                                imageEditableTakenFromStorage,
                                imageEditableUrlTakenFromStorage
                            )
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


    fun numberConsideringSortSummary(list: MutableList<String>) {
        list.sortWith { s1, s2 ->
            val num1 = s1.substring(8).toInt()
            val num2 = s2.substring(8).toInt()
            num1 - num2
        }
    }


    fun saveChangesToFirebase(
        mapIndex: Int,
        imageMapUrl: MutableMap<String, String>,
        imageMapByteArray: MutableMap<String, ByteArray>,
        imageMapByteArrayOriginal: MutableMap<String, ByteArray>,
        option1s: MutableList<String>,
        option1sOriginal: MutableList<String>,
        option2s: MutableList<String>,
        option2sOriginal: MutableList<String>,
        option3s: MutableList<String>,
        option3sOriginal: MutableList<String>,
        option4s: MutableList<String>,
        option4sOriginal: MutableList<String>,
        answers: MutableList<String>,
        answersOriginal: MutableList<String>,
        imageEditableTakenFromStorage: MutableList<Boolean>,
        imageEditableUrlTakenFromStorage: MutableList<String>
    ) {
        val firestore = FirebaseFirestore.getInstance()

        /*for (i in 0 until imageMapByteArrayOriginal.size) {
            if (!imageMapByteArray["key$i"].contentEquals(imageMapByteArrayOriginal["key$i"])) {
                val collectionRef =
                    firestore.collection("Questions/$selectedSubject/$selectedSubject/$selectedMode/QuestionSet$selectedSet")
                val query = collectionRef.whereEqualTo("question", imageMapUrl["key$i"])
                query.get().addOnSuccessListener { querySnapshot ->
                    for (document in querySnapshot.documents) {
                        document.reference.update("question", questions[i])
                            .addOnSuccessListener {
                                Log.d("wtfewq", "question updated")
                                questionsOriginal[i] = questions[i]
                            }
                    }
                }
            }
        }*/

        var shouldUpload = false
        if (!imageMapByteArray["key$mapIndex"].contentEquals(imageMapByteArrayOriginal["key$mapIndex"]))
            shouldUpload = true

        /*for (i in 0 until imageMapByteArrayOriginal.size) {
            if (!imageMapByteArray["key$i"].contentEquals(imageMapByteArrayOriginal["key$i"])) {
                shouldUpload = true
            }
        }*/

        if (shouldUpload) {
            if (!imageEditableTakenFromStorage[mapIndex]) {
                val storageRef = Firebase.storage.reference.child("Philosophy Images/Scientists/${System.currentTimeMillis()}.jpg")

                storageRef.putBytes(imageMapByteArray["key$mapIndex"]!!)
                    .addOnSuccessListener { taskSnapshot ->

                        // Get the download URL of the newly uploaded image
                        taskSnapshot.metadata?.reference?.downloadUrl?.addOnSuccessListener { uri ->
                            val downloadUrl = uri.toString()

                            //Replacing the image's url in the RandomFact firestore collection:
                            val collectionRef =
                                FirebaseFirestore.getInstance().collection(
                                    "Questions/$selectedSubject/" +
                                            "$selectedSubject/$selectedMode/QuestionSet$selectedSet"
                                )
                            val query = collectionRef.whereEqualTo(
                                "question",
                                imageMapUrl["key$mapIndex"]
                            )

                            query.get().addOnSuccessListener { documents ->
                                for (document in documents) {
                                    // Update the "image" field with the new image URL
                                    document.reference.update(
                                        "question",
                                        downloadUrl
                                    )
                                    imageMapByteArrayOriginal["key$mapIndex"] =
                                        imageMapByteArray["key$mapIndex"] as ByteArray
                                }
                            }.addOnFailureListener { exception ->
                                // Handle any errors that occur while querying the collection
                            }
                            // Store the new URL in your map of image URLs
                            imageMapUrl["key$mapIndex"] = downloadUrl
                        }
                    }
            } else {
                Log.d("grgea", "попадает")
                val collectionRef =
                    FirebaseFirestore.getInstance().collection(
                        "Questions/$selectedSubject/" +
                                "$selectedSubject/$selectedMode/QuestionSet$selectedSet"
                    )
                val query = collectionRef.whereEqualTo(
                    "question",
                    imageMapUrl["key$mapIndex"]
                )

                query.get().addOnSuccessListener { documents ->
                    for (document in documents) {
                        // Update the "image" field with the new image URL
                        document.reference.update(
                            "question",
                            imageEditableUrlTakenFromStorage[mapIndex]
                        )
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
                imageMapUrl["key$mapIndex"] = imageEditableUrlTakenFromStorage[mapIndex]
            }
        }

        for (i in 0 until imageMapUrl.size) {
            if (option1s[i] != option1sOriginal[i]) {
                val collectionRef =
                    firestore.collection("Questions/$selectedSubject/$selectedSubject/$selectedMode/QuestionSet$selectedSet")
                val query = collectionRef.whereEqualTo("question", imageMapUrl["key$i"])
                query.get().addOnSuccessListener { querySnapshot ->
                    for (document in querySnapshot.documents) {
                        document.reference.update("option1", option1s[i]).addOnSuccessListener {
                            Log.d("wtfewq", "option1 updated")
                            option1sOriginal[i] = option1s[i]
                        }
                    }
                }
            }
        }
        for (i in 0 until imageMapUrl.size) {
            if (option2s[i] != option2sOriginal[i]) {
                val collectionRef =
                    firestore.collection("Questions/$selectedSubject/$selectedSubject/$selectedMode/QuestionSet$selectedSet")
                val query = collectionRef.whereEqualTo("question", imageMapUrl["key$i"])
                query.get().addOnSuccessListener { querySnapshot ->
                    for (document in querySnapshot.documents) {
                        document.reference.update("option2", option2s[i]).addOnSuccessListener {
                            Log.d("wtfewq", "option2 updated")
                            option2sOriginal[i] = option2s[i]
                        }
                    }
                }
            }
        }
        for (i in 0 until imageMapUrl.size) {
            if (option3s[i] != option3sOriginal[i]) {
                val collectionRef =
                    firestore.collection("Questions/$selectedSubject/$selectedSubject/$selectedMode/QuestionSet$selectedSet")
                val query = collectionRef.whereEqualTo("question", imageMapUrl["key$i"])
                query.get().addOnSuccessListener { querySnapshot ->
                    for (document in querySnapshot.documents) {
                        document.reference.update("option3", option3s[i]).addOnSuccessListener {
                            Log.d("wtfewq", "option3 updated")
                            option3sOriginal[i] = option3s[i]
                        }
                    }
                }
            }
        }
        for (i in 0 until imageMapUrl.size) {
            if (option4s[i] != option4sOriginal[i]) {
                val collectionRef =
                    firestore.collection("Questions/$selectedSubject/$selectedSubject/$selectedMode/QuestionSet$selectedSet")
                val query = collectionRef.whereEqualTo("question", imageMapUrl["key$i"])
                query.get().addOnSuccessListener { querySnapshot ->
                    for (document in querySnapshot.documents) {
                        document.reference.update("option4", option4s[i]).addOnSuccessListener {
                            Log.d("wtfewq", "option4 updated")
                            option4sOriginal[i] = option4s[i]
                        }
                    }
                }
            }
        }
        for (i in 0 until imageMapUrl.size) {
            if (answers[i] != answersOriginal[i]) {
                val collectionRef =
                    firestore.collection("Questions/$selectedSubject/$selectedSubject/$selectedMode/QuestionSet$selectedSet")
                val query = collectionRef.whereEqualTo("question", imageMapUrl["key$i"])
                query.get().addOnSuccessListener { querySnapshot ->
                    for (document in querySnapshot.documents) {
                        document.reference.update("answer", answers[i]).addOnSuccessListener {
                            answersOriginal[i] = answers[i]
                        }
                    }
                }
            }
        }
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
            Thread {
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
            }.start()
        }
        latch.await() // wait for all the threads to finish
        return result
    }


    @SuppressLint("MutableCollectionMutableState")
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
            var imageSelectedFromStorage by remember { mutableStateOf(false) }
            val context = LocalContext.current
            var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }
            val launcher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent(),
                onResult = { uri ->
                    if (uri != null) {
                        val inputStream = context.contentResolver.openInputStream(uri)
                        imageBitmap = BitmapFactory.decodeStream(inputStream)
                        imageSelectedFromStorage = false
                    }
                }
            )

            var addFactTextFieldEmpty by remember { mutableStateOf(true) }
            var fieldsEmptyError by remember { mutableStateOf(false) }
            val fieldsEmptyErrorMessage by remember { mutableStateOf("Fields cannot be empty") }
            lateinit var option1: String
            lateinit var option2: String
            lateinit var option3: String
            lateinit var option4: String
            lateinit var answer: String
            var dialogLoadingVisible by remember { mutableStateOf(false) }

            val imageList by remember {
                mutableStateOf(
                    mutableListOf<ByteArray>()
                )
            }
            var imageFromStorage by remember {
                mutableStateOf<ByteArray?>(
                    null
                )
            }
            var showStorageGalleryDialog by remember {
                mutableStateOf(
                    false
                )
            }
            var imageSelectedFromStorageUrl by remember { mutableStateOf("") }
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
                                .width(120.dp)
                                .height(150.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(onClick = {
                                    showStorageGalleryDialog = true
                                })
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
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        // Option1
                        var option1Text by remember { mutableStateOf("") }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                                .height(55.dp) // Set the height of the Box to a larger value than the TextField
                                .width(120.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(colorResource(id = R.color.not_so_light_gray))
                        ) {
                            TextField(
                                value = option1Text,
                                onValueChange = {
                                    option1Text = it
                                    addFactTextFieldEmpty = option1Text.trim().isEmpty()
                                    if (!addFactTextFieldEmpty && imageBitmap != null)
                                        fieldsEmptyError = false
                                    option1 = option1Text
                                },
                                label = {
                                    Text(
                                        text = stringResource(R.string.option1),
                                        color = if (option1Text.isNotEmpty()) colorResource(id = R.color.BSUIR_Blue) else Color.Gray
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
                        // Option2
                        var option2Text by remember { mutableStateOf("") }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                                .height(55.dp) // Set the height of the Box to a larger value than the TextField
                                .width(120.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(colorResource(id = R.color.not_so_light_gray))
                        ) {
                            TextField(
                                value = option2Text,
                                onValueChange = {
                                    option2Text = it
                                    addFactTextFieldEmpty = option2Text.trim().isEmpty()
                                    if (!addFactTextFieldEmpty && imageBitmap != null)
                                        fieldsEmptyError = false
                                    option2 = option2Text
                                },
                                label = {
                                    Text(
                                        text = stringResource(R.string.option2),
                                        color = if (option2Text.isNotEmpty()) colorResource(id = R.color.BSUIR_Blue) else Color.Gray
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
                        // Option3
                        var option3Text by remember { mutableStateOf("") }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                                .height(55.dp) // Set the height of the Box to a larger value than the TextField
                                .width(120.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(colorResource(id = R.color.not_so_light_gray))
                        ) {
                            TextField(
                                value = option3Text,
                                onValueChange = {
                                    option3Text = it
                                    addFactTextFieldEmpty = option3Text.trim().isEmpty()
                                    if (!addFactTextFieldEmpty && imageBitmap != null)
                                        fieldsEmptyError = false
                                    option3 = option3Text
                                },
                                label = {
                                    Text(
                                        text = stringResource(R.string.option3),
                                        color = if (option3Text.isNotEmpty()) colorResource(id = R.color.BSUIR_Blue) else Color.Gray
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
                        // Option4
                        var option4Text by remember { mutableStateOf("") }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                                .height(55.dp) // Set the height of the Box to a larger value than the TextField
                                .width(120.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(colorResource(id = R.color.not_so_light_gray))
                        ) {
                            TextField(
                                value = option4Text,
                                onValueChange = {
                                    option4Text = it
                                    addFactTextFieldEmpty = option4Text.trim().isEmpty()
                                    if (!addFactTextFieldEmpty && imageBitmap != null)
                                        fieldsEmptyError = false
                                    option4 = option4Text
                                },
                                label = {
                                    Text(
                                        text = stringResource(R.string.option4),
                                        color = if (option4Text.isNotEmpty()) colorResource(id = R.color.BSUIR_Blue) else Color.Gray
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
                        //Answer
                        var answerText by remember { mutableStateOf("") }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                                .height(55.dp) // Set the height of the Box to a larger value than the TextField
                                .width(120.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(colorResource(id = R.color.not_so_light_gray))
                        ) {
                            TextField(
                                value = answerText,
                                onValueChange = {
                                    answerText = it
                                    addFactTextFieldEmpty = answerText.trim().isEmpty()
                                    if (!addFactTextFieldEmpty && imageBitmap != null)
                                        fieldsEmptyError = false
                                    answer = answerText
                                },
                                label = {
                                    Text(
                                        text = stringResource(R.string.answer),
                                        color = if (answerText.isNotEmpty()) colorResource(id = R.color.BSUIR_Blue) else Color.Gray
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
                            Text(text = "Отмена")
                        }
                        var addBtnEnabled by remember { mutableStateOf(true) }
                        Button(
                            enabled = addBtnEnabled,
                            onClick = {
                                //checking if fields are empty:
                                if (imageBitmap == null || addFactTextFieldEmpty) {
                                    fieldsEmptyError = true
                                    addBtnEnabled = true
                                } else {
                                    dialogLoadingVisible = true
                                    addBtnEnabled = false
                                    lateinit var docName: String
                                    var number = 1
                                    numberConsideringSortSummary(this@GuessByImageEditActivity.documentNames)
                                    for (name in this@GuessByImageEditActivity.documentNames) {
                                        if (name.substring(8).toInt() != number++) {
                                            docName = "Question${number - 1}"
                                            break
                                        } else {
                                            if (name.substring(8)
                                                    .toInt() == this@GuessByImageEditActivity.documentNames.size
                                            ) {
                                                docName = "Question${number}"
                                                break
                                            }
                                        }
                                    }

                                    //Checking if storage already contains the image:
                                    val storageRef =
                                        FirebaseStorage.getInstance()
                                            .getReference("Philosophy Images/Scientists")

                                    val maxDownloadSizeBytes: Long = 1024 * 1024 // 1 MB

                                    var imageExists = false
                                    /*var imageRefToReplace: StorageReference? = null
                                    storageRef.listAll().addOnSuccessListener { listResult ->
                                        // Loop through all the items in the folder
                                        for (item in listResult.items) {
                                            item.getBytes(maxDownloadSizeBytes)
                                                .addOnSuccessListener { remoteByteArray ->
                                                    // remoteByteArray is the byte array of the current image file in Firebase Storage
                                                    if (remoteByteArray.contentEquals(bitmapToByteArray(imageBitmap!!))) {
                                                        imageExists = true
                                                        imageRefToReplace = item
                                                    }

                                                }
                                        }*/
                                    if (imageSelectedFromStorage)
                                        imageExists = true

                                    if (!imageExists) {
                                        val storageToPut =
                                            FirebaseStorage.getInstance()
                                                .getReference("Philosophy Images/Scientists/${System.currentTimeMillis()}.jpg")
                                        val baos = ByteArrayOutputStream()
                                        imageBitmap!!.compress(
                                            Bitmap.CompressFormat.JPEG,
                                            100,
                                            baos
                                        )
                                        val data = baos.toByteArray()
                                        storageToPut.putBytes(data)
                                            .addOnSuccessListener { taskSnapshot ->

                                                // Get the download URL of the newly uploaded image
                                                taskSnapshot.metadata?.reference?.downloadUrl?.addOnSuccessListener { uri ->
                                                    val downloadUrl = uri.toString()
                                                    val db = Firebase.firestore
                                                    val factRef =
                                                        db.collection("Questions")
                                                            .document(
                                                                selectedSubject
                                                            )
                                                            .collection(
                                                                selectedSubject
                                                            ).document(selectedMode)
                                                            .collection(
                                                                "QuestionSet$selectedSet".toString()
                                                            ).document(docName)

                                                    val questionData = hashMapOf(
                                                        "question" to downloadUrl.toString(),
                                                        "option1" to option1,
                                                        "option2" to option2,
                                                        "option3" to option3,
                                                        "option4" to option4,
                                                        "answer" to answer
                                                    )

                                                    factRef.set(questionData)
                                                        .addOnSuccessListener {
                                                            Toast.makeText(
                                                                this@GuessByImageEditActivity,
                                                                "Question added successfully",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                            showAddElement = false
                                                            addBtnEnabled = true
                                                            dialogLoadingVisible =
                                                                false
                                                            //recreate()
                                                            val i = Intent(
                                                                this@GuessByImageEditActivity,
                                                                GuessByImageEditActivity::class.java
                                                            )
                                                            i.putExtra(
                                                                "selectedSubject",
                                                                selectedSubject
                                                            )
                                                            i.putExtra(
                                                                "selectedMode",
                                                                selectedMode
                                                            )
                                                            i.putExtra(
                                                                "selectedSet",
                                                                selectedSet
                                                            )
                                                            startActivity(i)
                                                            finish()
                                                        }
                                                        .addOnFailureListener { e ->
                                                            addBtnEnabled = true
                                                            dialogLoadingVisible =
                                                                false
                                                            Toast.makeText(
                                                                this@GuessByImageEditActivity,
                                                                "Failed to add question",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                            // Handle the error
                                                        }
                                                }
                                            }
                                    } else {
                                        val existingImageUrl = imageSelectedFromStorageUrl
                                        val db = Firebase.firestore
                                        val factRef = db.collection("Questions")
                                            .document(selectedSubject)
                                            .collection(selectedSubject)
                                            .document(selectedMode)
                                            .collection(
                                                "QuestionSet$selectedSet"
                                            ).document(docName)

                                        val questionData = hashMapOf(
                                            "question" to existingImageUrl,
                                            "option1" to option1,
                                            "option2" to option2,
                                            "option3" to option3,
                                            "option4" to option4,
                                            "answer" to answer
                                        )

                                        factRef.set(questionData)
                                            .addOnSuccessListener {
                                                Toast.makeText(
                                                    this@GuessByImageEditActivity,
                                                    "Question added successfully",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                showAddElement = false
                                                addBtnEnabled = true
                                                dialogLoadingVisible = false
                                                //recreate()
                                                val i = Intent(
                                                    this@GuessByImageEditActivity,
                                                    GuessByImageEditActivity::class.java
                                                )
                                                i.putExtra(
                                                    "selectedSubject",
                                                    selectedSubject
                                                )
                                                i.putExtra(
                                                    "selectedMode",
                                                    selectedMode
                                                )
                                                i.putExtra(
                                                    "selectedSet",
                                                    selectedSet
                                                )
                                                startActivity(i)
                                                finish()
                                            }
                                            .addOnFailureListener { e ->
                                                addBtnEnabled = true
                                                dialogLoadingVisible = false
                                                Toast.makeText(
                                                    this@GuessByImageEditActivity,
                                                    "Failed to add question",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                // Handle the error
                                            }
                                    }
                                    //}
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

                        var storageLoading by remember {
                            mutableStateOf(
                                false
                            )
                        }
                        val imageUrlList by remember { mutableStateOf(mutableListOf<String>()) }
                        var showStorageImageGrid by remember {
                            mutableStateOf(
                                false
                            )
                        }

                        if (showStorageGalleryDialog) {
                            AlertDialog(
                                modifier = Modifier.clip(
                                    RoundedCornerShape(
                                        10.dp
                                    )
                                ),
                                onDismissRequest = {
                                    showStorageGalleryDialog = false
                                },
                                title = { Text(text = "") },
                                buttons = {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 45.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Button(
                                            onClick = {
                                                storageLoading = true
                                                val storageRef =
                                                    FirebaseStorage.getInstance()
                                                        .getReference("$selectedSubject Images/Scientists")
                                                storageRef.listAll()
                                                    .addOnSuccessListener { listResult ->
                                                        val maxImages =
                                                            listResult.items.size
                                                        var currentImage = 0
                                                        listResult.items.forEach { item ->
                                                            item.getBytes(
                                                                Long.MAX_VALUE
                                                            )
                                                                .addOnSuccessListener { bytes ->
                                                                    imageList.add(
                                                                        bytes
                                                                    )
                                                                    currentImage++
                                                                    item.downloadUrl.addOnSuccessListener { uri ->
                                                                        imageUrlList.add(
                                                                            uri.toString()
                                                                        )
                                                                        if (currentImage == maxImages) {
                                                                            showStorageImageGrid =
                                                                                true
                                                                            showStorageGalleryDialog =
                                                                                false
                                                                            storageLoading =
                                                                                false
                                                                        }
                                                                    }
                                                                }
                                                        }
                                                    }
                                            },
                                            modifier = Modifier
                                                .width(225.dp)
                                                .padding(bottom = 2.dp)
                                                .clip(
                                                    RoundedCornerShape(10.dp)
                                                ),
                                            colors = ButtonDefaults.buttonColors(
                                                backgroundColor = colorResource(
                                                    id = R.color.not_so_light_gray
                                                ),
                                                contentColor = colorResource(
                                                    id = R.color.dark_text
                                                )
                                            )
                                        ) {
                                            if (storageLoading) {
                                                CircularProgressIndicator(
                                                    Modifier
                                                        .size(18.dp),
                                                    color = colorResource(id = R.color.BSUIR_Blue),
                                                    strokeWidth = 2.dp
                                                )
                                            } else
                                                Text(
                                                    text = "Из Firebase Storage",
                                                    fontSize = 16.sp
                                                )
                                        }
                                        Button(
                                            onClick = {
                                                launcher.launch("image/*")
                                                showStorageGalleryDialog =
                                                    false
                                            },
                                            modifier = Modifier
                                                .width(225.dp)
                                                .padding(top = 2.dp)
                                                .clip(
                                                    RoundedCornerShape(10.dp)
                                                ),
                                            colors = ButtonDefaults.buttonColors(
                                                backgroundColor = colorResource(
                                                    id = R.color.not_so_light_gray
                                                ),
                                                contentColor = colorResource(
                                                    id = R.color.dark_text
                                                )
                                            )
                                        ) {
                                            Text(
                                                text = "Из галереи",
                                                fontSize = 16.sp
                                            )
                                        }
                                    }
                                }
                            )
                        }

                        if (showStorageImageGrid) {
                            AlertDialog(
                                onDismissRequest = {
                                    showStorageImageGrid = false
                                },
                                title = { },
                                text = {
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(3),
                                        contentPadding = PaddingValues(
                                            horizontal = 8.dp,
                                            vertical = 8.dp
                                        ),
                                        verticalArrangement = Arrangement.spacedBy(
                                            16.dp
                                        ),
                                        horizontalArrangement = Arrangement.spacedBy(
                                            16.dp
                                        )
                                    ) {
                                        items(imageList.size) { index ->
                                            val image = imageList[index]
                                            val imageBitmapLocal =
                                                BitmapFactory.decodeByteArray(
                                                    image,
                                                    0,
                                                    image.size
                                                ).asImageBitmap()
                                            Image(
                                                bitmap = imageBitmapLocal,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .height(100.dp)
                                                    .width(100.dp)
                                                    .clip(
                                                        RoundedCornerShape(
                                                            10.dp
                                                        )
                                                    )
                                                    .clickable {
                                                        imageFromStorage =
                                                            imageList[index]
                                                        val byteArray =
                                                            bitmapToByteArray(imageBitmapLocal.asAndroidBitmap())
                                                        imageMapByteArray["key$index"] = byteArray
                                                        imageSelectedFromStorage =
                                                            true
                                                        showStorageImageGrid =
                                                            false
                                                        imageBitmap =
                                                            imageBitmapLocal.asAndroidBitmap()
                                                        imageSelectedFromStorageUrl =
                                                            imageUrlList[index]
                                                    }
                                            )
                                        }
                                    }
                                },
                                confirmButton = {
                                    TextButton(onClick = {
                                        showStorageImageGrid = false
                                    }) {
                                        Text(
                                            text = "Отмена",
                                            color = colorResource(R.color.BSUIR_Blue),
                                            fontSize = 17.sp
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            )
        }
    }
}