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

class TrueOrFalseEditActivity : ComponentActivity() {
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
        val answers by remember { mutableStateOf(mutableListOf<String>()) }
        val answersOriginal by remember { mutableStateOf(mutableListOf<String>()) }
        val revealAnswers by remember { mutableStateOf(mutableListOf<String>()) }
        val revealAnswersOriginal by remember { mutableStateOf(mutableListOf<String>()) }
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
                                val answer = document.get("answer").toString()
                                answers.add(answer)
                                answersOriginal.add(answer)
                                val revealAnswer = document.get("revealAnswer").toString()
                                revealAnswers.add(revealAnswer)
                                revealAnswersOriginal.add(revealAnswer)
                                documentNames.add(document.id)

                                if (document.id == "Question$numberOfQuestions") {
                                    numberConsideringSortSummary(this@TrueOrFalseEditActivity.documentNames) //correct number order in document names
                                    isLoading = false // done loading
                                }
                            } else {
                                if (document.id == "Question$numberOfQuestions") {
                                    numberConsideringSortSummary(this@TrueOrFalseEditActivity.documentNames) //correct number order in document names
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
                        items(questions.size) { index ->
                            var questionText by remember { mutableStateOf(questionsOriginal[index]) }
                            var answerText by remember { mutableStateOf(answersOriginal[index]) }
                            var revealAnswerText by remember { mutableStateOf(revealAnswersOriginal[index]) }
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
                                            TextField(
                                                value = revealAnswerText,
                                                onValueChange = {
                                                    revealAnswerText = it
                                                    revealAnswers[index] = revealAnswerText
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
                                                                this@TrueOrFalseEditActivity,
                                                                "Document deleted successfully",
                                                                Toast.LENGTH_SHORT
                                                            ).show()

                                                            val i = Intent(
                                                                this@TrueOrFalseEditActivity,
                                                                TrueOrFalseEditActivity::class.java
                                                            )
                                                            i.putExtra("selectedSubject", selectedSubject)
                                                            i.putExtra("selectedMode", selectedMode)
                                                            i.putExtra("selectedSet", selectedSet)
                                                            startActivity(i)
                                                            finish()
                                                        }.addOnFailureListener {
                                                            Toast.makeText(
                                                                this@TrueOrFalseEditActivity,
                                                                "Failed to delete document",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        }
                                                }
                                            }.addOnFailureListener {
                                                Toast.makeText(
                                                    this@TrueOrFalseEditActivity,
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
                                answers,
                                answersOriginal,
                                revealAnswers,
                                revealAnswersOriginal
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
        answers: MutableList<String>,
        answersOriginal: MutableList<String>,
        revealAnswers: MutableList<String>,
        revealAnswersOriginal: MutableList<String>
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
                            questionsOriginal[i] = questions[i]
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
                            answersOriginal[i] = answers[i]
                        }
                    }
                }
            }
        }
        for (i in 0 until questionsOriginal.size) {
            if (revealAnswers[i] != revealAnswersOriginal[i]) {
                val collectionRef =
                    firestore.collection("Questions/$selectedSubject/$selectedSubject/$selectedMode/QuestionSet$selectedSet")
                val query = collectionRef.whereEqualTo("revealAnswer", revealAnswersOriginal[i])
                query.get().addOnSuccessListener { querySnapshot ->
                    for (document in querySnapshot.documents) {
                        document.reference.update("revealAnswer", revealAnswers[i]).addOnSuccessListener {
                            revealAnswersOriginal[i] = revealAnswers[i]
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
                        contentDescription = stringResource(id = R.string.add),
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        )

        if (showAddElement) {
            var addFactTextFieldEmpty by remember { mutableStateOf(true) }
            var fieldsEmptyError by remember { mutableStateOf(false) }
            val fieldsEmptyErrorMessage by remember { mutableStateOf("Fields cannot be empty") }
            lateinit var question: String
            lateinit var answer: String
            lateinit var revealAnswer: String
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
                        var answerText by remember { mutableStateOf("") }
                        var revealAnswerText by remember { mutableStateOf("") }
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
                        //Reveal Answer
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                                .height(120.dp) // Set the height of the Box to a larger value than the TextField
                                .width(90.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(colorResource(id = R.color.not_so_light_gray))
                        ) {
                            TextField(
                                value = revealAnswerText,
                                onValueChange = {
                                    revealAnswerText = it
                                    addFactTextFieldEmpty = revealAnswerText.trim().isEmpty()
                                    if (!addFactTextFieldEmpty)
                                        fieldsEmptyError = false
                                    revealAnswer = revealAnswerText
                                },
                                label = {
                                    Text(
                                        text = "Объяснение ответа",
                                        color = if (revealAnswerText.isNotEmpty()) colorResource(id = R.color.BSUIR_Blue) else Color.Gray
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
                            Text(text = stringResource(id = R.string.cancel))
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
                                    numberConsideringSortSummary(this@TrueOrFalseEditActivity.documentNames)
                                    for (name in this@TrueOrFalseEditActivity.documentNames) {
                                        if (name.substring(8).toInt() != number++) {
                                            docName = "Question${number - 1}"
                                            break
                                        } else {
                                            if (name.substring(8).toInt() == this@TrueOrFalseEditActivity.documentNames.size) {
                                                docName = "Question${number}"
                                                break
                                            }
                                        }
                                    }
                                    val db = Firebase.firestore
                                    val ref = db.collection("Questions")
                                        .document(selectedSubject)
                                        .collection(selectedSubject).document(selectedMode).collection("QuestionSet$selectedSet").document(docName)

                                    val questionData = hashMapOf(
                                        "question" to question,
                                        "answer" to answer,
                                        "revealAnswer" to revealAnswer
                                    )

                                    ref.set(questionData)
                                        .addOnSuccessListener {
                                            Toast.makeText(
                                                this@TrueOrFalseEditActivity,
                                                "Вопрос создан",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            showAddElement = false
                                            addBtnEnabled = true
                                            dialogLoadingVisible = false
                                            //recreate()
                                            val i = Intent(
                                                this@TrueOrFalseEditActivity,
                                                TrueOrFalseEditActivity::class.java
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
                                                this@TrueOrFalseEditActivity,
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
                                Text(text = stringResource(id = R.string.add))
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