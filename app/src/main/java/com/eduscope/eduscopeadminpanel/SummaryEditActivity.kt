package com.eduscope.eduscopeadminpanel

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class SummaryEditActivity : ComponentActivity() {
    private lateinit var pref: SharedPreferences
    private var selectedSubject: String =
        "Philosophy"  //everytime subject choice is changes the list updates
    private var documentNames: MutableList<String> = mutableListOf()

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
        var contents by remember { mutableStateOf(mutableListOf<String>()) }
        var contentsOriginal by remember { mutableStateOf(mutableListOf<String>()) }
        var titles by remember { mutableStateOf(mutableListOf<String>()) }
        var titlesOriginal by remember { mutableStateOf(mutableListOf<String>()) }
        var isLoading by remember { mutableStateOf(true) }
        var saveChangesButtonVisible by remember { mutableStateOf(false) }
        var spanSearchWords by remember { mutableStateOf(mutableListOf<String>()) }
        var spanSearchWordsOriginal by remember { mutableStateOf(mutableListOf<String>()) }
        var spanSearchWordsDocNames by remember { mutableStateOf(mutableListOf<String>()) }
        var showAddElementSpan by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {

                val docRef = firestore.collection("Summary/$selectedSubject/SpanSearchWords")
                    .document("SpanSearchWords")
                docRef.get().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val document = task.result
                        if (document.exists()) {
                            val data = document.data
                            if (data != null) {
                                for (mapElement in data) {
                                    spanSearchWords.add(mapElement.value.toString())
                                    spanSearchWordsOriginal.add(mapElement.value.toString())
                                    spanSearchWordsDocNames.add(mapElement.key)
                                }
                            }
                        }

                    }
                }.addOnFailureListener { exception ->
                    Toast.makeText(
                        this@SummaryEditActivity,
                        "Failed to load span search words",
                        Toast.LENGTH_SHORT
                    ).show()
                }


                val querySnapshot = firestore.collection("Summary").get().await()
                documentNames = querySnapshot.documents.mapNotNull { it.id }

                val collectionRef = firestore.collection("Summary").document(selectedSubject)
                    .collection(selectedSubject)
                    .get().await()
                var i = 0
                for (document in collectionRef.documents) {
                    val title = document.getString("title")
                    val text = document.getString("content")
                    title?.let {
                        titles.add(it)
                        titlesOriginal.add(it)
                    }
                    text?.let {
                        contents.add(it)
                        contentsOriginal.add(it)
                    }
                    this@SummaryEditActivity.documentNames.add(document.id)
                    i++
                }
                numberConsideringSortSummary(this@SummaryEditActivity.documentNames) //correct number order in document names
                numberConisderingSortSpan(spanSearchWordsDocNames)
                Log.d("sortIssues", "numberConsideringSort")
                titles = sortStringsByNumber(titles) as MutableList<String>
                /*for (i in 0 until titles.size) {
                    Log.d("titles check", titles[i])
                }*/
                Log.d("sortIssues", "titles")
                contents = sortStringsByNumber(contents) as MutableList<String>
                Log.d("sortIssues", "contents")
                titlesOriginal = sortStringsByNumber(titlesOriginal) as MutableList<String>
                Log.d("sortIssues", "titlesOriginal")
                contentsOriginal = sortStringsByNumber(contentsOriginal) as MutableList<String>
                Log.d("sortIssues", "contentsOriginal")
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
                CustomTopAppBar(title = "Конспекты")

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
                        item {
                            val BSUIR_Blue = colorResource(id = R.color.BSUIR_Blue)
                            val spanTitle by remember { mutableStateOf("Span Search Words") }
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
                                        .clickable { }
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
                                    //
                                    CompositionLocalProvider(LocalTextSelectionColors provides customTextSelectionColors) {
                                        var spanWordToDeleteIndex by remember { mutableStateOf(-1) }
                                        var spanWordToDelete by remember { mutableStateOf("") }
                                        var showDeleteSpanDialog by remember { mutableStateOf(false) }
                                        var isExpanded by remember { mutableStateOf(false) }
                                        Row (verticalAlignment = Alignment.CenterVertically) {
                                            if (!isExpanded)
                                                Image(painter = painterResource(id = R.drawable.span_icon), contentDescription = "spanIcon",
                                                modifier = Modifier.size(50.dp).padding(start = 10.dp, end = 7.dp))
                                            Column(
                                                modifier = Modifier.padding(
                                                    start = 10.dp,
                                                    end = 10.dp
                                                )
                                            ) {
                                                Text(
                                                    text = spanTitle,
                                                    fontWeight = FontWeight.Medium,
                                                    fontSize = 17.sp,
                                                    modifier = Modifier.clickable {
                                                        isExpanded = true
                                                    }
                                                )
                                                if (!isExpanded) {
                                                    Text(
                                                        text = "Различные формы имен ученых в качестве ключевых слов для расставления ссылок в конспектах",
                                                        modifier = Modifier
                                                            .padding(bottom = 6.dp)
                                                            .clickable { isExpanded = true })
                                                }
                                                if (isExpanded) {
                                                    Text(text = "Добавить",
                                                        color = colorResource(
                                                            id = R.color.BSUIR_Blue
                                                        ),
                                                        fontSize = 17.sp,
                                                        modifier = Modifier
                                                            .clickable {
                                                                //Add span word functionality:
                                                                showAddElementSpan = true
                                                            }
                                                            .padding(top = 3.dp)
                                                    )


                                                    //Inner lazy column here:
                                                    Box(modifier = Modifier.height(400.dp)) {

                                                        LazyColumn(
                                                            contentPadding = PaddingValues(
                                                                start = 16.dp,
                                                                end = 16.dp
                                                                //bottom = if (saveChangesButtonVisible) 66.dp else 0.dp
                                                            )
                                                        ) {


                                                            items(spanSearchWords.size) { index ->
                                                                var spanTextFieldContent by remember {
                                                                    mutableStateOf(
                                                                        spanSearchWords[index]
                                                                    )
                                                                }
                                                                Row(
                                                                    modifier = Modifier.fillMaxWidth()
                                                                ) {
                                                                    TextField(
                                                                        value = spanTextFieldContent,
                                                                        onValueChange = {
                                                                            spanTextFieldContent =
                                                                                it
                                                                            spanSearchWords[index] =
                                                                                spanTextFieldContent
                                                                            saveChangesButtonVisible =
                                                                                true
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
                                                                        ),
                                                                        modifier = Modifier.weight(
                                                                            1f
                                                                        )
                                                                    )
                                                                    IconButton(
                                                                        onClick = {
                                                                            spanWordToDeleteIndex = index
                                                                            spanWordToDelete = spanTextFieldContent
                                                                            showDeleteSpanDialog = true
                                                                        },
                                                                        modifier = Modifier
                                                                            .size(22.dp)
                                                                            .offset(y = 25.dp) // adjust the value to your liking
                                                                    ) {
                                                                        Icon(
                                                                            imageVector = Icons.Default.Delete,
                                                                            contentDescription = null,
                                                                            tint = colorResource(id = R.color.dark_text) // Add a tint color to the Icon
                                                                        )
                                                                    }
                                                                }

                                                            }
                                                        }






                                                        if (showDeleteSpanDialog) {
                                                            var showLoadingSpanDeleteDialog by remember { mutableStateOf(false) }
                                                            AlertDialog(
                                                                onDismissRequest = {
                                                                    showDeleteSpanDialog = false
                                                                },
                                                                modifier = Modifier.clip(
                                                                    RoundedCornerShape(10.dp)
                                                                ),
                                                                title = { Text(text = stringResource(R.string.element_delete), fontSize = 20.sp) },
                                                                text = {
                                                                    val text = AnnotatedString.Builder().apply {
                                                                        append(stringResource(R.string.are_you_sure_you_want_to_delete_span_word))
                                                                        withStyle(style = SpanStyle(fontWeight = FontWeight.Medium)) {
                                                                            append(spanWordToDelete)
                                                                        }
                                                                    }.toAnnotatedString()

                                                                    Text(text = text, fontSize = 17.sp, color = colorResource(R.color.dark_text))
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
                                                                                showDeleteSpanDialog =
                                                                                    false
                                                                            },
                                                                            modifier = Modifier
                                                                                .padding(
                                                                                    start = 8.dp,
                                                                                    end = 8.dp,
                                                                                    bottom = 8.dp
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
                                                                            Text(text = "No")
                                                                        }
                                                                        val addBtnEnabled by remember {
                                                                            mutableStateOf(
                                                                                true
                                                                            )
                                                                        }
                                                                        Button(
                                                                            enabled = addBtnEnabled,
                                                                            onClick = {
                                                                                showLoadingSpanDeleteDialog = true
                                                                                val spanRef =
                                                                                    firestore.collection(
                                                                                        "Summary/$selectedSubject/SpanSearchWords"
                                                                                    )
                                                                                        .document("SpanSearchWords")

                                                                                spanRef.get()
                                                                                    .addOnSuccessListener { documentSnapshot ->
                                                                                        if (documentSnapshot != null && documentSnapshot.exists()) {
                                                                                            val data =
                                                                                                documentSnapshot.data
                                                                                            if (data != null) {
                                                                                                for ((key, value) in data) {
                                                                                                    if (value == spanSearchWordsOriginal[spanWordToDeleteIndex]) {
                                                                                                        spanRef.update(
                                                                                                            key,
                                                                                                            FieldValue.delete()
                                                                                                        )
                                                                                                            .addOnSuccessListener {
                                                                                                                Toast.makeText(
                                                                                                                    this@SummaryEditActivity,
                                                                                                                    "Document deleted successfully",
                                                                                                                    Toast.LENGTH_SHORT
                                                                                                                )
                                                                                                                    .show()
                                                                                                                startActivity(
                                                                                                                    Intent(
                                                                                                                        this@SummaryEditActivity,
                                                                                                                        SummaryEditActivity::class.java
                                                                                                                    )
                                                                                                                )
                                                                                                                finish()
                                                                                                            }
                                                                                                            .addOnFailureListener {
                                                                                                                Toast.makeText(
                                                                                                                    this@SummaryEditActivity,
                                                                                                                    "Failed to delete document",
                                                                                                                    Toast.LENGTH_SHORT
                                                                                                                )
                                                                                                                    .show()
                                                                                                            }
                                                                                                    }
                                                                                                }
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                    .addOnFailureListener { exception ->
                                                                                    }
                                                                            },
                                                                            modifier = Modifier
                                                                                .padding(
                                                                                    start = 8.dp,
                                                                                    end = 8.dp,
                                                                                    bottom = 8.dp
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
                                                                            if (!showLoadingSpanDeleteDialog)
                                                                                Text(text = "Yes")
                                                                            if (showLoadingSpanDeleteDialog) {
                                                                                CircularProgressIndicator(
                                                                                    Modifier
                                                                                        .size(15.dp),
                                                                                    color = colorResource(
                                                                                        id = R.color.BSUIR_Blue
                                                                                    ),
                                                                                    strokeWidth = 1.5.dp
                                                                                )
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            )
                                                        }











                                                    }


                                                    var text by remember { mutableStateOf("") }
                                                    var addFactTextFieldEmpty by remember {
                                                        mutableStateOf(
                                                            true
                                                        )
                                                    }
                                                    var fieldsEmptyError by remember {
                                                        mutableStateOf(
                                                            false
                                                        )
                                                    }
                                                    val fieldsEmptyErrorMessage by remember {
                                                        mutableStateOf(
                                                            "Field cannot be empty"
                                                        )
                                                    }
                                                    lateinit var content: String
                                                    var dialogLoadingVisible by remember {
                                                        mutableStateOf(
                                                            false
                                                        )
                                                    }

                                                    if (showAddElementSpan) {
                                                        AlertDialog(
                                                            onDismissRequest = {
                                                                showAddElementSpan = false
                                                            },
                                                            modifier = Modifier.clip(
                                                                RoundedCornerShape(
                                                                    10.dp
                                                                )
                                                            ),
                                                            title = { Text(text = "") },
                                                            text = {

                                                                Box(
                                                                    modifier = Modifier
                                                                        .fillMaxWidth()
                                                                        .height(55.dp) // Set the height of the Box to a larger value than the TextField
                                                                        .width(120.dp)
                                                                        .clip(RoundedCornerShape(10.dp))
                                                                        .background(colorResource(id = R.color.not_so_light_gray))
                                                                ) {
                                                                    TextField(
                                                                        value = text,
                                                                        onValueChange = {
                                                                            text = it
                                                                            addFactTextFieldEmpty =
                                                                                text.trim()
                                                                                    .isEmpty()
                                                                            if (!addFactTextFieldEmpty)
                                                                                fieldsEmptyError =
                                                                                    false
                                                                            content = text
                                                                        },
                                                                        label = {
                                                                            Text(
                                                                                text = "Enter new span word",
                                                                                color = if (text.isNotEmpty()) colorResource(
                                                                                    id = R.color.BSUIR_Blue
                                                                                ) else Color.Gray
                                                                            )
                                                                        },
                                                                        modifier = Modifier
                                                                            .fillMaxSize()
                                                                            .background(
                                                                                color = colorResource(
                                                                                    id = R.color.white // set the new background color here
                                                                                )
                                                                            )
                                                                            .clip(
                                                                                RoundedCornerShape(
                                                                                    10.dp
                                                                                )
                                                                            ), // apply the RoundedCornerShape to the TextField
                                                                        textStyle = TextStyle(
                                                                            fontSize = 17.sp
                                                                        ),
                                                                        colors = TextFieldDefaults.textFieldColors(
                                                                            textColor = colorResource(
                                                                                id = R.color.dark_text
                                                                            ),
                                                                            backgroundColor = colorResource(
                                                                                R.color.lighter_gray
                                                                            ),
                                                                            cursorColor = colorResource(
                                                                                id = R.color.BSUIR_Blue
                                                                            ),
                                                                            focusedIndicatorColor = colorResource(
                                                                                id = R.color.transparent
                                                                            ),
                                                                            unfocusedIndicatorColor = colorResource(
                                                                                id = R.color.transparent
                                                                            ),
                                                                            disabledTextColor = colorResource(
                                                                                id = R.color.light_gray
                                                                            ),
                                                                            trailingIconColor = colorResource(
                                                                                id = R.color.BSUIR_Blue
                                                                            )
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
                                                                            showAddElementSpan =
                                                                                false
                                                                        },
                                                                        modifier = Modifier
                                                                            .padding(
                                                                                start = 8.dp,
                                                                                end = 8.dp,
                                                                                bottom = 8.dp
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
                                                                        Text(text = "Cancel")
                                                                    }
                                                                    var addBtnEnabled by remember {
                                                                        mutableStateOf(
                                                                            true
                                                                        )
                                                                    }
                                                                    Button(
                                                                        enabled = addBtnEnabled,
                                                                        onClick = {
                                                                            //checking if fields are empty:
                                                                            if (addFactTextFieldEmpty) {
                                                                                fieldsEmptyError =
                                                                                    true
                                                                                addBtnEnabled = true
                                                                            } else {
                                                                                dialogLoadingVisible =
                                                                                    true
                                                                                addBtnEnabled =
                                                                                    false
                                                                                lateinit var docName: String
                                                                                var number = 1
                                                                                for (name in spanSearchWordsDocNames) {
                                                                                    if (name.substring(
                                                                                            10
                                                                                        )
                                                                                            .toInt() != number++
                                                                                    ) {
                                                                                        docName =
                                                                                            "searchWord${number - 1}"
                                                                                        break
                                                                                    } else {
                                                                                        if (name.substring(
                                                                                                10
                                                                                            )
                                                                                                .toInt() == spanSearchWordsDocNames.size
                                                                                        ) {
                                                                                            docName =
                                                                                                "searchWord${number}"
                                                                                            break
                                                                                        }
                                                                                    }
                                                                                }
                                                                                val db =
                                                                                    Firebase.firestore
                                                                                val spanRef =
                                                                                    firestore.collection(
                                                                                        "Summary"
                                                                                    ).document(
                                                                                        selectedSubject
                                                                                    )
                                                                                        .collection(
                                                                                            "SpanSearchWords"
                                                                                        )
                                                                                        .document("SpanSearchWords")
                                                                                val data =
                                                                                    hashMapOf(
                                                                                        docName to content
                                                                                    )
                                                                                spanRef.update(data as Map<String, Any>)
                                                                                    .addOnSuccessListener {
                                                                                        Toast.makeText(
                                                                                            this@SummaryEditActivity,
                                                                                            "Конспект создан",
                                                                                            Toast.LENGTH_SHORT
                                                                                        ).show()
                                                                                        showAddElementSpan =
                                                                                            false
                                                                                        addBtnEnabled =
                                                                                            true
                                                                                        dialogLoadingVisible =
                                                                                            false
                                                                                        //recreate()
                                                                                        startActivity(
                                                                                            Intent(
                                                                                                this@SummaryEditActivity,
                                                                                                SummaryEditActivity::class.java
                                                                                            )
                                                                                        )
                                                                                        finish()
                                                                                    }
                                                                                    .addOnFailureListener { e ->
                                                                                        addBtnEnabled =
                                                                                            true
                                                                                        dialogLoadingVisible =
                                                                                            false
                                                                                        Toast.makeText(
                                                                                            this@SummaryEditActivity,
                                                                                            "Failed to add fact",
                                                                                            Toast.LENGTH_SHORT
                                                                                        ).show()
                                                                                        // Handle the error
                                                                                    }

                                                                            }
                                                                        },
                                                                        modifier = Modifier
                                                                            .padding(
                                                                                start = 8.dp,
                                                                                end = 8.dp,
                                                                                bottom = 8.dp
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
                                                                        if (!dialogLoadingVisible)
                                                                            Text(text = "Add")
                                                                        if (dialogLoadingVisible) {
                                                                            CircularProgressIndicator(
                                                                                Modifier
                                                                                    .size(15.dp),
                                                                                color = colorResource(
                                                                                    id = R.color.BSUIR_Blue
                                                                                ),
                                                                                strokeWidth = 1.5.dp
                                                                            )
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        )
                                                    }












                                                    Box(modifier = Modifier.clickable {
                                                        isExpanded = false
                                                    }) {
                                                        Text(
                                                            text = "Скрыть контент",
                                                            fontSize = 17.sp,
                                                            color = colorResource(id = R.color.BSUIR_Blue)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                            }


                        }
                        items(titles.size) { index ->
                            var text by remember { mutableStateOf(contents[index]) }
                            val BSUIR_Blue = colorResource(id = R.color.BSUIR_Blue)
                            var title by remember { mutableStateOf(titles[index]) }
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
                                        .clickable { }
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
                                    //
                                    CompositionLocalProvider(LocalTextSelectionColors provides customTextSelectionColors) {
                                        Column(
                                            modifier = Modifier.padding(
                                                start = 10.dp,
                                                end = 10.dp
                                            )
                                        ) {
                                            TextField(
                                                value = title,
                                                onValueChange = {
                                                    title = it
                                                    titles[index] = title
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
                                            var isExpanded by remember { mutableStateOf(false) }
                                            if (!isExpanded) {
                                                Text(
                                                    text = text.take(150) + "...",
                                                    modifier = Modifier
                                                        .padding(bottom = 6.dp)
                                                        .clickable { isExpanded = true })
                                            }
                                            if (isExpanded) {
                                                Box(modifier = Modifier.clickable {
                                                    isExpanded = false
                                                }) {
                                                    Text(
                                                        text = "Скрыть контент",
                                                        fontSize = 17.sp,
                                                        color = colorResource(id = R.color.BSUIR_Blue)
                                                    )
                                                }
                                                Box(
                                                    modifier = Modifier.clickable {
                                                        isExpanded = true
                                                    }
                                                ) {
                                                    TextField(
                                                        value = text,
                                                        onValueChange = {
                                                            text = it
                                                            contents[index] = text
                                                            saveChangesButtonVisible = true
                                                        },
                                                        label = { Text("") },
                                                        textStyle = TextStyle(
                                                            fontSize = 17.sp
                                                        ),
                                                        modifier = Modifier.padding(
                                                            bottom = 6.dp
                                                        ),
                                                        colors = TextFieldDefaults.textFieldColors(
                                                            backgroundColor = Color.Transparent,
                                                            focusedIndicatorColor = Color.Transparent,
                                                            unfocusedIndicatorColor = Color.Transparent,
                                                            cursorColor = BSUIR_Blue
                                                        )
                                                    )
                                                }
                                                Box(modifier = Modifier.clickable {
                                                    isExpanded = false
                                                }) {
                                                    Text(
                                                        text = "Скрыть контент",
                                                        fontSize = 17.sp,
                                                        color = colorResource(id = R.color.BSUIR_Blue)
                                                    )
                                                }
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
                                            //There'll be an extra menu asking if a user is sure with a note that if during the current session
                                            //the image of the fact they want to delete was edited it won't work properly and they gotta save changes first

                                            //delete functionality here
                                            val collectionRef =
                                                Firebase.firestore.collection("Summary/$selectedSubject/$selectedSubject")
                                            val query = collectionRef.whereEqualTo(
                                                "title",
                                                titlesOriginal[index]
                                            )
                                            query.get().addOnSuccessListener { documents ->
                                                for (document in documents) {
                                                    // Delete the document
                                                    collectionRef.document(document.id).delete()
                                                        .addOnSuccessListener {
                                                            Toast.makeText(
                                                                this@SummaryEditActivity,
                                                                "Document deleted successfully",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                            startActivity(
                                                                Intent(
                                                                    this@SummaryEditActivity,
                                                                    SummaryEditActivity::class.java
                                                                )
                                                            )
                                                            finish()
                                                        }.addOnFailureListener {
                                                            Toast.makeText(
                                                                this@SummaryEditActivity,
                                                                "Failed to delete document",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        }
                                                }
                                            }.addOnFailureListener {
                                                Toast.makeText(
                                                    this@SummaryEditActivity,
                                                    "Matching document not found",
                                                    Toast.LENGTH_SHORT
                                                ).show()
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
        if (saveChangesButtonVisible) {
            Box(modifier = Modifier.fillMaxSize()) {
                Card(
                    onClick = {
                        for (i in 0..titlesOriginal.size) {
                            saveChangesToFirebase(
                                i,
                                titles,
                                titlesOriginal,
                                contents,
                                contentsOriginal,
                                spanSearchWords,
                                spanSearchWordsOriginal
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


    fun sortStringsByNumber(strings: List<String>): List<String> {
        return strings.sortedWith(compareBy { it.trimStart().substringBefore(".").toInt() })
    }


    fun numberConsideringSortSummary(list: MutableList<String>) {
        list.sortWith { s1, s2 ->
            val num1 = s1.substring(7).toInt()
            val num2 = s2.substring(7).toInt()
            num1 - num2
        }
    }

    fun numberConisderingSortSpan(list: MutableList<String>) {
        list.sortWith { s1, s2 ->
            val num1 = s1.substring(10).toInt()
            val num2 = s2.substring(10).toInt()
            num1 - num2
        }
    }


    fun saveChangesToFirebase(
        mapIndex: Int,
        titles: MutableList<String>,
        titlesOriginal: MutableList<String>,
        contents: MutableList<String>,
        contentsOriginal: MutableList<String>,
        spanSearchWords: MutableList<String>,
        spanSearchWordsOriginal: MutableList<String>
    ) {
        val firestore = FirebaseFirestore.getInstance()

        for (i in 0 until titlesOriginal.size) {
            if (titles[i] != titlesOriginal[i]) {
                val collectionRef =
                    firestore.collection("Summary/$selectedSubject/$selectedSubject")
                val query = collectionRef.whereEqualTo("title", titlesOriginal[i])
                query.get().addOnSuccessListener { querySnapshot ->
                    for (document in querySnapshot.documents) {
                        document.reference.update("title", titles[i]).addOnSuccessListener {
                            titlesOriginal[i] = titles[i]
                        }
                    }
                }
            }
        }

        for (i in 0 until contentsOriginal.size) {
            if (contents[i] != contentsOriginal[i]) {
                val collectionRef =
                    firestore.collection("Summary/$selectedSubject/$selectedSubject")
// Find the document that contains the original content
                val query = collectionRef.whereEqualTo("title", titlesOriginal[i])
                query.get().addOnSuccessListener { querySnapshot ->
                    for (document in querySnapshot.documents) {
                        // Replace the original content with the updated content
                        document.reference.update("content", contents[i])
                            .addOnSuccessListener {
                                contentsOriginal[i] = contents[i]
                                //Toast.makeText(this@SummaryEditActivity, "changes saved", Toast.LENGTH_SHORT).show()
                                //startActivity(Intent(this@SummaryEditActivity, SummaryEditActivity::class.java))
                                //finish()
                            }
                    }
                }
            }
        }

        for (i in 0 until spanSearchWordsOriginal.size) {
            if (spanSearchWords[i] != spanSearchWordsOriginal[i]) {
                val spanRef = firestore.collection("Summary/$selectedSubject/SpanSearchWords")
                    .document("SpanSearchWords")
                spanRef.get()
                    .addOnSuccessListener { documentSnapshot ->
                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            val data = documentSnapshot.data
                            if (data != null) {
                                for ((key, value) in data) {
                                    if (value == spanSearchWordsOriginal[i]) {
                                        // Update the field with a new value
                                        val updates = hashMapOf<String, Any>(
                                            key to spanSearchWords[i]
                                        )
                                        spanRef.update(updates)
                                            .addOnSuccessListener {
                                                println("Field $key updated successfully")
                                                //Toast.makeText(this@SummaryEditActivity, "changes saved", Toast.LENGTH_SHORT).show()
                                                //startActivity(Intent(this@SummaryEditActivity, SummaryEditActivity::class.java))
                                                //finish()
                                            }
                                            .addOnFailureListener { exception ->
                                                println("Error updating field $key: $exception")
                                            }
                                    }
                                }
                            }
                        }
                    }
                    .addOnFailureListener { exception ->
                        println("Error getting document: $exception")
                    }
            }
        }
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
                            startActivity(Intent(this@SummaryEditActivity, SummaryEditActivity::class.java))
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

            var addFactTextFieldEmpty by remember { mutableStateOf(true) }
            var fieldsEmptyError by remember { mutableStateOf(false) }
            val fieldsEmptyErrorMessage by remember { mutableStateOf("Fields cannot be empty") }
            lateinit var title: String
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
                        // Large text fields
                        var titleText by remember { mutableStateOf("") }
                        var text by remember { mutableStateOf("") }
                        //Title
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(55.dp) // Set the height of the Box to a larger value than the TextField
                                .width(120.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(colorResource(id = R.color.not_so_light_gray))
                        ) {
                            TextField(
                                value = titleText,
                                onValueChange = {
                                    titleText = it
                                    addFactTextFieldEmpty = titleText.trim().isEmpty()
                                    if (!addFactTextFieldEmpty)
                                        fieldsEmptyError = false
                                    title = titleText
                                },
                                label = {
                                    Text(
                                        text = stringResource(R.string.title),
                                        color = if (titleText.isNotEmpty()) colorResource(id = R.color.BSUIR_Blue) else Color.Gray
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
                                    //backgroundColor = Color.Transparent,                                  //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
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
                        //Content
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
                                    if (!addFactTextFieldEmpty)
                                        fieldsEmptyError = false
                                    content = text
                                },
                                label = {
                                    Text(
                                        text = stringResource(R.string.text),
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
                                    backgroundColor = colorResource(id = R.color.lighter_gray),
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
                                if (addFactTextFieldEmpty) {
                                    fieldsEmptyError = true
                                    addBtnEnabled = true
                                } else {
                                    dialogLoadingVisible = true
                                    addBtnEnabled = false
                                    lateinit var docName: String
                                    var number = 1
                                    for (name in documentNames) {
                                        if (name.substring(7).toInt() != number++) {
                                            docName = "summary${number - 1}"
                                            break
                                        } else {
                                            if (name.substring(7).toInt() == documentNames.size) {
                                                docName = "summary${number}"
                                                break
                                            }
                                        }
                                    }
                                    val db = Firebase.firestore
                                    val ref = db.collection("Summary")
                                        .document(selectedSubject)
                                        .collection(selectedSubject).document(docName)

                                    val factData = hashMapOf(
                                        "title" to title,
                                        "content" to content
                                    )

                                    ref.set(factData)
                                        .addOnSuccessListener {
                                            Toast.makeText(
                                                this@SummaryEditActivity,
                                                "Fact added successfully",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            showAddElement = false
                                            addBtnEnabled = true
                                            dialogLoadingVisible = false
                                            //recreate()
                                            startActivity(
                                                Intent(
                                                    this@SummaryEditActivity,
                                                    SummaryEditActivity::class.java
                                                )
                                            )
                                            finish()
                                        }
                                        .addOnFailureListener { e ->
                                            addBtnEnabled = true
                                            dialogLoadingVisible = false
                                            Toast.makeText(
                                                this@SummaryEditActivity,
                                                "Failed to add fact",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            // Handle the error
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