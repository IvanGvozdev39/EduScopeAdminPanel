//Spinners' value changes don't work


package com.eduscope.eduscopeadminpanel

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

class TestsSetSelectActivity : ComponentActivity() {
    private lateinit var pref: SharedPreferences
    private var selectedSubject: String =
        "Philosophy"  //everytime subject choice is changes the list updates
    private var selectedMode: String = "TopicTraining"
    private var documentNames: MutableList<String> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pref = getSharedPreferences("spinnerChoices", Context.MODE_PRIVATE)
        selectedSubject = pref.getString("selectedSubject", "Philosophy").toString()
        selectedMode = pref.getString("testsMode", "TopicTraining").toString()
        setContent {
            ContentScreen()
        }
    }


    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    fun ContentScreen() {
        val firestore = FirebaseFirestore.getInstance()
        var documentNames by remember { mutableStateOf(emptyList<String>()) }
        var modes by remember { mutableStateOf(emptyList<String>()) }
        var isLoading by remember { mutableStateOf(true) }
        val numberOfQuestionsInSet by remember { mutableStateOf(mutableListOf<Int>()) }
        var numberOfSets by remember { mutableStateOf(0) }

        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {

                val querySnapshot = firestore.collection("Questions").get().await()
                documentNames = querySnapshot.documents.mapNotNull { it.id }

                val modesRef =
                    firestore.collection("Questions/$selectedSubject/$selectedSubject").get()
                        .await()
                modes = modesRef.documents.mapNotNull { it.id }

                val docRef = firestore.collection("Questions/$selectedSubject/$selectedSubject")
                    .document(selectedMode)
                docRef.get().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val document = task.result
                        if (document.exists()) {
                            val data = document.data
                            if (data != null) {
                                numberOfSets = (data["NumberOfSets"] as Long).toInt()

                                for (i in 1..numberOfSets) {
                                    val colRef =
                                        firestore.collection("Questions/$selectedSubject/$selectedSubject/$selectedMode/QuestionSet$i")
                                    colRef.get().addOnSuccessListener { documents ->
                                        numberOfQuestionsInSet.add(documents.size())
                                    }
                                }
                            }
                        }
                    }
                }

                isLoading = false
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
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
                CustomTopAppBar(title = "Тесты", numberOfSets)

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
                            var modeText by rememberSaveable { mutableStateOf(selectedMode) }
                            ModeSpinner(modes, modeText) { newModeText ->
                                // Update the state of modeText when it changes
                                modeText = newModeText
                            }
                        }
                        if (selectedMode == "AllTopicsTraining") {
                            item {
                                var isAutomaticFillLoading by remember { mutableStateOf(false) }
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(IntrinsicSize.Min)
                                        .padding(bottom = 16.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    elevation = 5.dp,
                                    backgroundColor = Color.White,
                                    onClick = {
                                        isAutomaticFillLoading = true
                                        val db = FirebaseFirestore.getInstance()
                                        val docRef = db.collection("Questions")
                                            .document(selectedSubject)
                                            .collection(selectedSubject)
                                            .document("TopicTraining")

                                        docRef.get().addOnSuccessListener { document ->
                                            if (document != null) {
                                                val numberOfSets = document.getLong("NumberOfSets")
                                                if (numberOfSets != null) {
                                                    val questionList = mutableListOf<String>()
                                                    val option1List = mutableListOf<String>()
                                                    val option2List = mutableListOf<String>()
                                                    val option3List = mutableListOf<String>()
                                                    val option4List = mutableListOf<String>()
                                                    val answerList = mutableListOf<String>()

                                                    // Use a coroutine scope to launch the async tasks
                                                    CoroutineScope(Dispatchers.Default).launch {
                                                        // Create a list of Deferred objects to hold the async tasks
                                                        val deferredList =
                                                            mutableListOf<Deferred<Unit>>()

                                                        for (i in 1..numberOfSets) {
                                                            val setRef =
                                                                docRef.collection("QuestionSet$i")

                                                            // Launch an async task to fetch the data and populate the lists
                                                            val deferred = async {
                                                                val querySnapshot =
                                                                    setRef.get().await()
                                                                for (doc in querySnapshot.documents) {
                                                                    val question =
                                                                        doc.getString("question")
                                                                    val option1 =
                                                                        doc.getString("option1")
                                                                    val option2 =
                                                                        doc.getString("option2")
                                                                    val option3 =
                                                                        doc.getString("option3")
                                                                    val option4 =
                                                                        doc.getString("option4")
                                                                    val answer =
                                                                        doc.getString("answer")

                                                                    questionList += question!!
                                                                    option1List += option1!!
                                                                    option2List += option2!!
                                                                    option3List += option3!!
                                                                    option4List += option4!!
                                                                    answerList += answer!!
                                                                }
                                                            }
                                                            deferredList.add(deferred)
                                                        }

                                                        // Wait for all async tasks to complete before continuing
                                                        deferredList.awaitAll()

                                                        val indexList = mutableListOf<Int>()
                                                        for (i in 0..questionList.size - 5)
                                                            indexList.add(i, i)
                                                        indexList.shuffle()
                                                        var indexListIndex = 0
                                                        var questionCounter = 0

                                                        val allTopicsTrainingRef =
                                                            db.collection("Questions")
                                                                .document(selectedSubject)
                                                                .collection(selectedSubject)
                                                                .document("AllTopicsTraining")

                                                        Log.d(
                                                            "questionListSize",
                                                            "QuestionListSize: ${questionList.size}"
                                                        )
                                                        allTopicsTrainingRef.get()
                                                            .addOnSuccessListener { document ->
                                                                if (document != null) {
                                                                    val numberOfSetsAT =
                                                                        document.getLong("NumberOfSets")
                                                                    if (numberOfSetsAT != null) {
                                                                        for (j in 1..numberOfSetsAT) {
                                                                            val questionSetRef =
                                                                                allTopicsTrainingRef.collection(
                                                                                    "QuestionSet$j"
                                                                                )

                                                                            questionSetRef.get()
                                                                                .addOnSuccessListener { querySnapshot ->
                                                                                    for (doc in querySnapshot.documents) {
                                                                                        val data =
                                                                                            mapOf(
                                                                                                "question" to questionList[indexList[indexListIndex]],
                                                                                                "option1" to option1List[indexList[indexListIndex]],
                                                                                                "option2" to option2List[indexList[indexListIndex]],
                                                                                                "option3" to option3List[indexList[indexListIndex]],
                                                                                                "option4" to option4List[indexList[indexListIndex]],
                                                                                                "answer" to answerList[indexList[indexListIndex]]
                                                                                            )
                                                                                        questionCounter++
                                                                                        if (questionCounter >= numberOfSetsAT * 10) {
                                                                                            startActivity(
                                                                                                Intent(
                                                                                                    this@TestsSetSelectActivity,
                                                                                                    TestsSetSelectActivity::class.java
                                                                                                )
                                                                                            )
                                                                                            finish()
                                                                                        }
                                                                                        indexListIndex++
                                                                                        doc.reference.update(
                                                                                            data
                                                                                        )
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
                                    }
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!isAutomaticFillLoading) {
                                            Text(
                                                text = stringResource(id = R.string.automatic_all_topic_training_question_fill),
                                                color = colorResource(R.color.dark_text),
                                                fontSize = 17.sp,
                                                modifier = Modifier.padding(horizontal = 16.dp)
                                            )
                                        } else {
                                            CircularProgressIndicator(
                                                Modifier
                                                    .size(20.dp),
                                                color = colorResource(
                                                    id = R.color.BSUIR_Blue
                                                ),
                                                strokeWidth = 2.dp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        items(numberOfSets) { index ->
                            val BSUIR_Blue = colorResource(id = R.color.BSUIR_Blue)
                            val customTextSelectionColors = TextSelectionColors(
                                handleColor = BSUIR_Blue,
                                backgroundColor = BSUIR_Blue.copy(alpha = 0.3f)
                            )
                            var showDeleteSpanDialog by remember { mutableStateOf(false) }
                            var expanded by remember { mutableStateOf(false) }
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White)
                                    .padding(bottom = 16.dp)
                                    .clickable { questionsIntent(index) },
                                shape = RoundedCornerShape(10.dp),
                                elevation = 5.dp
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clickable {
                                            questionsIntent(index)
                                        }
                                        .background(Color.White)
                                        .padding(10.dp)
                                        .pointerInput(Unit) {
                                            detectTapGestures(
                                                onLongPress = {
                                                    expanded = true
                                                },
                                                onPress = {
                                                    questionsIntent(index)
                                                }
                                            )
                                        }
                                ) {
                                    //
                                    CompositionLocalProvider(LocalTextSelectionColors provides customTextSelectionColors) {
                                        Column(
                                            modifier = Modifier
                                                .padding(
                                                    start = 10.dp,
                                                    end = 10.dp,
                                                    top = 3.dp,
                                                    bottom = 3.dp
                                                )
                                                .clickable {
                                                    questionsIntent(index)
                                                }
                                        ) {
                                            //Title
                                            Text(
                                                text = "QuestionSet${index + 1}",
                                                fontSize = 17.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = colorResource(id = R.color.dark_text),
                                                modifier = Modifier
                                                    .padding(bottom = 4.dp)
                                                    .clickable { questionsIntent(index) }
                                            )
                                            var questionNumber by remember { mutableStateOf(" ") }

                                            firestore.collection("Questions")
                                                .document(selectedSubject)
                                                .collection(selectedSubject).document(selectedMode)
                                                .collection("QuestionSet${index + 1}").get()
                                                .addOnSuccessListener { documents ->
                                                    questionNumber = documents.size().toString()

                                                }
                                            Text(
                                                text = "Количество вопросов: $questionNumber",
                                                fontSize = 17.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = colorResource(id = R.color.dark_gray),
                                                modifier = Modifier.clickable {
                                                    questionsIntent(
                                                        index
                                                    )
                                                }
                                            )
                                        }




                                        if (showDeleteSpanDialog) {
                                            var showLoadingSpanDeleteDialog by remember {
                                                mutableStateOf(
                                                    false
                                                )
                                            }
                                            AlertDialog(
                                                onDismissRequest = {
                                                    showDeleteSpanDialog = false
                                                },
                                                modifier = Modifier.clip(
                                                    RoundedCornerShape(10.dp)
                                                ),
                                                title = {
                                                    Text(
                                                        text = stringResource(R.string.set_delete),
                                                        fontSize = 20.sp
                                                    )
                                                },
                                                text = {
                                                    val text = AnnotatedString.Builder().apply {
                                                        append(stringResource(R.string.are_you_sure_you_want_to_delete))
                                                        withStyle(style = SpanStyle(fontWeight = FontWeight.Medium)) {
                                                            append("QuestionSet${index + 1}")
                                                        }
                                                    }.toAnnotatedString()

                                                    Text(
                                                        text = text,
                                                        fontSize = 17.sp,
                                                        color = colorResource(R.color.dark_text)
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
                                                                showDeleteSpanDialog =
                                                                    false
                                                                expanded = false
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
                                                            Text(text = stringResource(R.string.no))
                                                        }
                                                        val addBtnEnabled by remember {
                                                            mutableStateOf(
                                                                true
                                                            )
                                                        }
                                                        Button(
                                                            enabled = addBtnEnabled,
                                                            onClick = {
                                                                val collectionRef =
                                                                    firestore.collection("Question/$selectedSubject/$selectedSubject/$selectedMode/QuestionSet${index + 1}")
                                                                collectionRef.get()
                                                                    .addOnSuccessListener { querySnapshot ->
                                                                        val batch =
                                                                            firestore.batch()
                                                                        querySnapshot.documents.forEach { document ->
                                                                            batch.delete(document.reference)
                                                                        }
                                                                        batch.commit()
                                                                            .addOnSuccessListener {
                                                                                val docRef =
                                                                                    FirebaseFirestore.getInstance()
                                                                                        .collection(
                                                                                            "Questions/$selectedSubject/$selectedSubject"
                                                                                        )
                                                                                        .document(
                                                                                            selectedMode
                                                                                        )
                                                                                docRef.update(
                                                                                    "NumberOfSets",
                                                                                    numberOfSets - 1
                                                                                )
                                                                                    .addOnSuccessListener {
                                                                                        Toast.makeText(
                                                                                            this@TestsSetSelectActivity,
                                                                                            "Set successfully deleted",
                                                                                            Toast.LENGTH_SHORT
                                                                                        ).show()
                                                                                        startActivity(
                                                                                            Intent(
                                                                                                this@TestsSetSelectActivity,
                                                                                                TestsSetSelectActivity::class.java
                                                                                            )
                                                                                        )
                                                                                        finish()
                                                                                    }
                                                                            }
                                                                            .addOnFailureListener { exception ->
                                                                                // Handle any errors
                                                                            }
                                                                    }
                                                                    .addOnFailureListener { exception ->
                                                                        // Handle any errors
                                                                    }

                                                                expanded = false
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
                                                                Text(text = stringResource(R.string.yes))
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
                                            showDeleteSpanDialog = true
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


    fun questionsIntent(index: Int) {
        var i: Intent? = null
        if (selectedMode == "TopicTraining" || selectedMode == "AllTopicsTraining") {
            i = Intent(this@TestsSetSelectActivity, TopicAllTopicTrainingEditActivity::class.java)
        } else if (selectedMode == "TrueOrFalse") {
            i = Intent(this@TestsSetSelectActivity, TrueOrFalseEditActivity::class.java)
        } else if (selectedMode == "GuessByImage") {
            i = Intent(this@TestsSetSelectActivity, GuessByImageEditActivity::class.java)
        }
        i?.putExtra("selectedSubject", selectedSubject)
        i?.putExtra("selectedMode", selectedMode)
        i?.putExtra("selectedSet", index + 1)
        startActivity(i)
    }


    @Composable
    fun SubjectSpinner(
        subjects: List<String>,
        subjectText: String,
        onSubjectTextChanged: (String) -> Unit
    ) {
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
                            startActivity(
                                Intent(
                                    this@TestsSetSelectActivity,
                                    TestsSetSelectActivity::class.java
                                )
                            )
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
    fun ModeSpinner(modes: List<String>, modeText: String, onModeTextChanged: (String) -> Unit) {
        var expanded by remember { mutableStateOf(false) }

        Box(Modifier.fillMaxWidth()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, bottom = 12.dp)
                    .clickable {
                        expanded = !expanded
                    }
                    .wrapContentWidth(Alignment.CenterHorizontally),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = modeText, fontSize = 17.sp, modifier = Modifier.padding(end = 8.dp))
                Icon(imageVector = Icons.Filled.ArrowDropDown, contentDescription = "ArrowDropDown")
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    modes.forEach { mode ->
                        DropdownMenuItem(onClick = {
                            expanded = false
                            onModeTextChanged(mode)
                            val editor = pref.edit()
                            editor.putString("testsMode", mode)
                            editor.apply()
                            startActivity(
                                Intent(
                                    this@TestsSetSelectActivity,
                                    TestsSetSelectActivity::class.java
                                )
                            )
                            finish()
                        }) {
                            Text(text = mode)
                        }
                    }
                }
            }
        }
    }


    @Composable
    fun CustomTopAppBar(title: String, numberOfSets: Int) {
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
            var showLoading by remember { mutableStateOf(false) }
            AlertDialog(
                onDismissRequest = {
                    showAddElement = false
                },
                modifier = Modifier.clip(
                    RoundedCornerShape(10.dp)
                ),
                title = { Text(text = stringResource(R.string.set_create), fontSize = 20.sp) },
                text = {
                    val text = AnnotatedString.Builder().apply {
                        append(stringResource(R.string.are_you_sure_you_want_to_create) + " ")
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Medium)) {
                            append("QuestionSet${numberOfSets + 1}")
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
                                showAddElement =
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
                            Text(text = stringResource(R.string.no))
                        }
                        val addBtnEnabled by remember {
                            mutableStateOf(
                                true
                            )
                        }
                        Button(
                            enabled = addBtnEnabled,
                            onClick = {
                                showLoading = true
                                val oldDocumentRef = FirebaseFirestore.getInstance()
                                    .document("Questions/$selectedSubject/$selectedSubject/$selectedMode")
                                val newCollectionRef =
                                    oldDocumentRef.collection("QuestionSet${numberOfSets + 1}")
                                val newDocumentRef = newCollectionRef.document("Question1")
                                val data = hashMapOf(
                                    "question" to " ",
                                )
                                newDocumentRef.set(data).addOnSuccessListener {
                                    val docRef = FirebaseFirestore.getInstance()
                                        .collection("Questions/$selectedSubject/$selectedSubject")
                                        .document(selectedMode)
                                    docRef.update("NumberOfSets", numberOfSets + 1)
                                        .addOnSuccessListener {
                                            Toast.makeText(
                                                this@TestsSetSelectActivity,
                                                "Set created",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            startActivity(
                                                Intent(
                                                    this@TestsSetSelectActivity,
                                                    TestsSetSelectActivity::class.java
                                                )
                                            )
                                            finish()
                                            showAddElement = false
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
                            if (!showLoading)
                                Text(text = stringResource(R.string.yes))
                            if (showLoading) {
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
}