package com.eduscope.eduscopeadminpanel

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


import kotlin.coroutines.suspendCoroutine as suspendCoroutine1

class TopicAllTopicTrainingEditActivity : ComponentActivity() {
    private lateinit var selectedSubject: String
    private lateinit var selectedMode: String
    private var selectedSet = 0
    private var documentNames: MutableList<String> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedSubject = intent.getStringExtra("selectedSubject").toString()
        selectedMode = intent.getStringExtra("selectedMode").toString()
        selectedSet = intent.getIntExtra("selectedSet", 1)
        setContent {
            ContentScreen()
        }
    }


    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    fun ContentScreen() {
        val firestore = FirebaseFirestore.getInstance()
        val questions by remember { mutableStateOf(mutableListOf<String>()) }
        val questionsOriginal by remember { mutableStateOf(mutableListOf<String>()) }
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
                                questions.add(question)
                                questionsOriginal.add(question)
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
                                Log.d("aefaawwaf", document.id)

                                if (document.id == "Question$numberOfQuestions") {
                                    numberConsideringSortSummary(this@TopicAllTopicTrainingEditActivity.documentNames) //correct number order in document names
                                    isLoading = false // done loading
                                }
                            } else {
                                if (document.id == "Question$numberOfQuestions") {
                                    numberConsideringSortSummary(this@TopicAllTopicTrainingEditActivity.documentNames) //correct number order in document names
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
                            end = 16.dp
                            //bottom = if (saveChangesButtonVisible) 66.dp else 0.dp
                        )
                    ) {
                        Log.d("checkincheck", "final = ${questions.size}")
                        items(questions.size) { index ->
                            var questionText by remember { mutableStateOf(questionsOriginal[index]) }
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
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White)
                                    .padding(bottom = 16.dp, top = if (index == 0) 16.dp else 0.dp),
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
                                            ),
                                            verticalArrangement = Arrangement.spacedBy((-15).dp)
                                        ) {
                                            TextField(
                                                value = questionText,
                                                onValueChange = {
                                                    questionText = it
                                                    questions[index] = questionText
                                                    saveChangesButtonVisible = true
                                                },
                                                label = { Text("") },
                                                textStyle = TextStyle(
                                                    fontSize = 17.sp,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                colors = TextFieldDefaults.textFieldColors(
                                                    backgroundColor = Color.Transparent,
                                                    focusedIndicatorColor = Color.Transparent,
                                                    unfocusedIndicatorColor = Color.Transparent,
                                                    cursorColor = BSUIR_Blue
                                                )
                                            )
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

                                            //delete functionality here
                                            val collectionRef =
                                                Firebase.firestore.collection("Questions/$selectedSubject/$selectedSubject/$selectedMode/QuestionSet$selectedSet")
                                            val query = collectionRef.whereEqualTo(
                                                "question",
                                                questionsOriginal[index]
                                            )
                                            query.get().addOnSuccessListener { documents ->
                                                for (document in documents) {
                                                    // Delete the document
                                                    collectionRef.document(document.id).delete()
                                                        .addOnSuccessListener {
                                                            Toast.makeText(
                                                                this@TopicAllTopicTrainingEditActivity,
                                                                "Document deleted successfully",
                                                                Toast.LENGTH_SHORT
                                                            ).show()

                                                            val i = Intent(
                                                                this@TopicAllTopicTrainingEditActivity,
                                                                TopicAllTopicTrainingEditActivity::class.java
                                                            )
                                                            i.putExtra("selectedSubject", selectedSubject)
                                                            i.putExtra("selectedMode", selectedMode)
                                                            i.putExtra("selectedSet", selectedSet)
                                                            startActivity(i)
                                                            finish()
                                                        }.addOnFailureListener {
                                                            Toast.makeText(
                                                                this@TopicAllTopicTrainingEditActivity,
                                                                "Failed to delete document",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        }
                                                }
                                            }.addOnFailureListener {
                                                Toast.makeText(
                                                    this@TopicAllTopicTrainingEditActivity,
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
                        for (i in 0..questions.size) {
                            saveChangesToFirebase(
                                i,
                                questions,
                                questionsOriginal,
                                option1s,
                                option1sOriginal,
                                option2s,
                                option2sOriginal,
                                option3s,
                                option3sOriginal,
                                option4s, option4sOriginal,
                                answers,
                                answersOriginal
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


    fun numberConsideringSortSummary(list: MutableList<String>) {
        list.sortWith { s1, s2 ->
            val num1 = s1.substring(8).toInt()
            val num2 = s2.substring(8).toInt()
            num1 - num2
        }
    }


    fun saveChangesToFirebase(
        mapIndex: Int,
        questions: MutableList<String>,
        questionsOriginal: MutableList<String>,
        option1s: MutableList<String>,
        option1sOriginal: MutableList<String>,
        option2s: MutableList<String>,
        option2sOriginal: MutableList<String>,
        option3s: MutableList<String>,
        option3sOriginal: MutableList<String>,
        option4s: MutableList<String>,
        option4sOriginal: MutableList<String>,
        answers: MutableList<String>,
        answersOriginal: MutableList<String>
    ) {
        val firestore = FirebaseFirestore.getInstance()

        for (i in 0 until questionsOriginal.size) {
            if (questions[i] != questionsOriginal[i]) {
                val collectionRef =
                    firestore.collection("Questions/$selectedSubject/$selectedSubject/$selectedMode/QuestionSet$selectedSet")
                val query = collectionRef.whereEqualTo("question", questionsOriginal[i])
                query.get().addOnSuccessListener { querySnapshot ->
                    for (document in querySnapshot.documents) {
                        document.reference.update("question", questions[i]).addOnSuccessListener {
                            Log.d("wtfewq", "question updated")
                            questionsOriginal[i] = questions[i]
                        }
                    }
                }
            }
        }
        for (i in 0 until questionsOriginal.size) {
            if (option1s[i] != option1sOriginal[i]) {
                val collectionRef =
                    firestore.collection("Questions/$selectedSubject/$selectedSubject/$selectedMode/QuestionSet$selectedSet")
                val query = collectionRef.whereEqualTo("question", questionsOriginal[i])
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
        for (i in 0 until questionsOriginal.size) {
            if (option2s[i] != option2sOriginal[i]) {
                val collectionRef =
                    firestore.collection("Questions/$selectedSubject/$selectedSubject/$selectedMode/QuestionSet$selectedSet")
                val query = collectionRef.whereEqualTo("question", questionsOriginal[i])
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
        for (i in 0 until questionsOriginal.size) {
            if (option3s[i] != option3sOriginal[i]) {
                val collectionRef =
                    firestore.collection("Questions/$selectedSubject/$selectedSubject/$selectedMode/QuestionSet$selectedSet")
                val query = collectionRef.whereEqualTo("question", questionsOriginal[i])
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
        for (i in 0 until questionsOriginal.size) {
            if (option4s[i] != option4sOriginal[i]) {
                val collectionRef =
                    firestore.collection("Questions/$selectedSubject/$selectedSubject/$selectedMode/QuestionSet$selectedSet")
                val query = collectionRef.whereEqualTo("question", questionsOriginal[i])
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
        for (i in 0 until questionsOriginal.size) {
            if (answers[i] != answersOriginal[i]) {
                val collectionRef =
                    firestore.collection("Questions/$selectedSubject/$selectedSubject/$selectedMode/QuestionSet$selectedSet")
                val query = collectionRef.whereEqualTo("question", questionsOriginal[i])
                query.get().addOnSuccessListener { querySnapshot ->
                    for (document in querySnapshot.documents) {
                        document.reference.update("answer", answers[i]).addOnSuccessListener {
                            Log.d("wtfewq", "answer updated")
                            answersOriginal[i] = answers[i]
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
                        contentDescription = stringResource(R.string.add),
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
            lateinit var question: String
            lateinit var option1: String
            lateinit var option2: String
            lateinit var option3: String
            lateinit var option4: String
            lateinit var answer: String
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
                        var questionText by remember { mutableStateOf("") }
                        var option1Text by remember { mutableStateOf("") }
                        var option2Text by remember { mutableStateOf("") }
                        var option3Text by remember { mutableStateOf("") }
                        var option4Text by remember { mutableStateOf("") }
                        var answerText by remember { mutableStateOf("") }
                        //Question
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp) // Set the height of the Box to a larger value than the TextField
                                .width(120.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(colorResource(id = R.color.not_so_light_gray))
                        ) {
                            TextField(
                                value = questionText,
                                onValueChange = {
                                    questionText = it
                                    addFactTextFieldEmpty = questionText.trim().isEmpty()
                                    if (!addFactTextFieldEmpty)
                                        fieldsEmptyError = false
                                    question = questionText
                                },
                                label = {
                                    Text(
                                        text = "Вопрос",
                                        color = if (questionText.isNotEmpty()) colorResource(id = R.color.BSUIR_Blue) else Color.Gray
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
                        //Option1
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
                                    if (!addFactTextFieldEmpty)
                                        fieldsEmptyError = false
                                    option1 = option1Text
                                },
                                label = {
                                    Text(
                                        text = "Вариант 1",
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
                        //option2
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
                                    if (!addFactTextFieldEmpty)
                                        fieldsEmptyError = false
                                    option2 = option2Text
                                },
                                label = {
                                    Text(
                                        text = "Вариант 2",
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
                        //option3
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
                                    if (!addFactTextFieldEmpty)
                                        fieldsEmptyError = false
                                    option3 = option3Text
                                },
                                label = {
                                    Text(
                                        text = "Вариант 3",
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
                        //option4
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
                                    if (!addFactTextFieldEmpty)
                                        fieldsEmptyError = false
                                    option4 = option4Text
                                },
                                label = {
                                    Text(
                                        text = "Вариант 4",
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
                        //answer
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
                                    if (!addFactTextFieldEmpty)
                                        fieldsEmptyError = false
                                    answer = answerText
                                },
                                label = {
                                    Text(
                                        text = "Ответ",
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
                                    numberConsideringSortSummary(this@TopicAllTopicTrainingEditActivity.documentNames)
                                    for (name in this@TopicAllTopicTrainingEditActivity.documentNames) {
                                        Log.d("documentNameza", name)
                                        if (name.substring(8).toInt() != number++) {
                                            docName = "Question${number - 1}"
                                            break
                                        } else {
                                            if (name.substring(8).toInt() == this@TopicAllTopicTrainingEditActivity.documentNames.size) {
                                                docName = "Question${number}"
                                                break
                                            }
                                        }
                                    }
                                    Log.d("documentNameza", "final docName: $docName")
                                    val db = Firebase.firestore
                                    val ref = db.collection("Questions")
                                        .document(selectedSubject)
                                        .collection(selectedSubject).document(selectedMode).collection("QuestionSet$selectedSet").document(docName)

                                    val questionData = hashMapOf(
                                        "question" to question,
                                        "option1" to option1,
                                        "option2" to option2,
                                        "option3" to option3,
                                        "option4" to option4,
                                        "answer" to answer
                                    )

                                    ref.set(questionData)
                                        .addOnSuccessListener {
                                            Toast.makeText(
                                                this@TopicAllTopicTrainingEditActivity,
                                                "Вопрос создан",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            showAddElement = false
                                            addBtnEnabled = true
                                            dialogLoadingVisible = false
                                            //recreate()
                                            val i = Intent(
                                                this@TopicAllTopicTrainingEditActivity,
                                                TopicAllTopicTrainingEditActivity::class.java
                                            )
                                            i.putExtra("selectedSubject", selectedSubject)
                                            i.putExtra("selectedMode", selectedMode)
                                            i.putExtra("selectedSet", selectedSet)
                                            startActivity(i)
                                            finish()
                                        }
                                        .addOnFailureListener { e ->
                                            addBtnEnabled = true
                                            dialogLoadingVisible = false
                                            Toast.makeText(
                                                this@TopicAllTopicTrainingEditActivity,
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
                                Text(text = "Add")
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