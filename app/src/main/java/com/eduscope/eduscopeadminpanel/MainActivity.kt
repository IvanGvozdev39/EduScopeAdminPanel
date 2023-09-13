package com.eduscope.eduscopeadminpanel

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class MainActivity : ComponentActivity() {

    companion object {
        init {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            layoutCreate(this)
        }
    }


    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    private fun layoutCreate(context: Context) {
        val subjectList by remember { mutableStateOf(mutableListOf<String>()) }
        val dbVersionList by remember { mutableStateOf(mutableListOf<Int>()) }
        var currentDbVersionIndex by remember { mutableStateOf<Int>(0) }
        var displayDbVersionCards by remember { mutableStateOf(false) }
        var dbVersionUpdateConfirmed by remember { mutableStateOf(false) }
        var dbVersionUpdateConfirmedDown by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                val db = FirebaseFirestore.getInstance()
                val collectionRef = db.collection("DatabaseVersion")
                collectionRef.get().addOnSuccessListener { querySnapshot ->
                    for (document in querySnapshot.documents) {
                        subjectList.add(document.id)
                        dbVersionList.add((document.get("databaseVersion") as Long).toInt())
                    }
                    displayDbVersionCards = true
                }
            }
        }

        val firestore = FirebaseFirestore.getInstance()
        val gridIcons = intArrayOf(
            R.drawable.random_fact,
            R.drawable.summary,
            R.drawable.scientists,
            R.drawable.tests
        )
        val gridLabels = arrayOf(
            stringResource(id = R.string.random_fact), stringResource(id = R.string.summary),
            stringResource(id = R.string.scientists), stringResource(id = R.string.tests)
        )
        var showDbVersionDialog by remember { mutableStateOf(false) }
        var decreaseDialog by remember { mutableStateOf(false) }


        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentWidth(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = stringResource(id = R.string.app_name),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)
                    )
                }
            }
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 15.dp, start = 16.dp, end = 16.dp),
                    shape = RoundedCornerShape(10.dp),
                    elevation = 5.dp
                ) {
                    Box(
                        modifier = Modifier
                            .clickable {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://console.firebase.google.com/u/0/project/eduscope-f19aa/analytics/app/android:com.eduscope.eduscope/overview")
                                )
                                context.startActivity(intent)
                            }
                            .padding(top = 10.dp, bottom = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(id = R.mipmap.firebase_logo_foreground),
                                contentDescription = "image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                            )
                            Column(
                                modifier = Modifier.padding(
                                    start = 10.dp,
                                    end = 15.dp,
                                    top = 5.dp,
                                    bottom = 5.dp
                                )
                            ) {
                                Text(
                                    text = stringResource(id = R.string.go_to_firebase_analytics),
                                    fontSize = 18.sp,
                                    modifier = Modifier.padding(bottom = 1.dp)
                                )
                                Text(
                                    text = stringResource(id = R.string.firebase_analytics_description),
                                    fontSize = 12.5.sp,
                                    color = Color.DarkGray
                                )
                            }
                        }
                    }
                }
            }
            gridItems(4, nColumns = 2) { index ->
                Card(
                    modifier = Modifier
                        .padding(
                            start = if (index % 2 == 0) 16.dp else 8.dp,
                            end = if (index % 2 == 0) 8.dp else 16.dp,
                            top = 8.dp,
                            bottom = 8.dp
                        )
                        .fillMaxWidth(),
                    elevation = 5.dp,
                    onClick = {
                        when (index) {
                            0 -> {
                                startActivity(
                                    Intent(
                                        this@MainActivity,
                                        RandomFactEditActivity::class.java
                                    )
                                )
                            }
                            1 -> {
                                startActivity(
                                    Intent(
                                        this@MainActivity,
                                        SummaryEditActivity::class.java
                                    )
                                )
                            }
                            2 -> {
                                startActivity(
                                    Intent(
                                        this@MainActivity,
                                        ScientistsEditActivity::class.java
                                    )
                                )
                            }
                            3 -> {
                                startActivity(
                                    Intent(
                                        this@MainActivity,
                                        TestsSetSelectActivity::class.java
                                    )
                                )
                            }
                        }
                    }
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Image(
                            painter = painterResource(id = gridIcons[index]),
                            contentDescription = "Image",
                            modifier = Modifier.size(50.dp)
                        )
                        Text(
                            text = gridLabels[index],
                            fontSize = 16.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
            if (displayDbVersionCards) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentWidth(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text = stringResource(id = R.string.database_version),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 16.dp)
                        )
                    }
                }
                items(subjectList.size) { index ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp, start = 16.dp, end = 16.dp),
                        shape = RoundedCornerShape(10.dp),
                        elevation = 2.dp
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = subjectList[index],
                                fontSize = 18.sp,
                                modifier = Modifier.padding(start = 10.dp)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End,
                                modifier = Modifier.padding(
                                    top = 10.dp,
                                    bottom = 10.dp,
                                    start = 10.dp
                                ),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(
                                            color = colorResource(id = R.color.not_so_light_gray),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable(onClick = {
                                            currentDbVersionIndex = index
                                            decreaseDialog = true
                                            showDbVersionDialog = true
                                        }),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_baseline_horizontal_rule_24_black),
                                        contentDescription = "Add",
                                        tint = Color.Black,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    //text = if (!dbVersionUpdateConfirmed) dbVersionList[index].toString() else if (!dbVersionUpdateConfirmedDown) (dbVersionList[index]+1).toString() else (dbVersionList[index]-1).toString(),
                                    text = if (!dbVersionUpdateConfirmed) dbVersionList[index].toString() else dbVersionList[index].toString(),
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 17.sp
                                )
                                if (dbVersionUpdateConfirmed) {
                                    dbVersionUpdateConfirmed = false
                                    dbVersionUpdateConfirmedDown = false
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(
                                            color = colorResource(id = R.color.not_so_light_gray),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable(onClick = {
                                            currentDbVersionIndex = index
                                            decreaseDialog = false
                                            showDbVersionDialog = true
                                        }),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add",
                                        tint = Color.Black,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            else {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .padding(top = 36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            Modifier
                                .size(25.dp),
                            color = colorResource(id = R.color.BSUIR_Blue),
                            strokeWidth = 2.5.dp
                        )
                    }
                }

            }
        }

        if (showDbVersionDialog) {
            var dialogLoadingVisible by remember { mutableStateOf(false) }
            AlertDialog(
                onDismissRequest = { showDbVersionDialog = false },
                modifier = Modifier.clip(RoundedCornerShape(10.dp)),
                title = {
                    Text(
                        text = stringResource(id = R.string.db_version_update),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium
                    )
                },
                text = {
                    val text = AnnotatedString.Builder().apply {
                        append(
                            if (decreaseDialog) stringResource(id = R.string.are_you_sure_you_wanna_decrease_db_version) else stringResource(
                                id = R.string.are_you_sure_you_wanna_increase_db_version
                            )
                        )
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Medium)) {
                            append(if (decreaseDialog) " ${dbVersionList[currentDbVersionIndex] - 1}" else " ${dbVersionList[currentDbVersionIndex] + 1}")
                        }
                        append(" по предмету: ")
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Medium)) {
                            append(subjectList[currentDbVersionIndex] + "?")
                        }
                    }.toAnnotatedString()
                    Text(
                        text = text,
                        fontSize = 17.sp,
                        color = colorResource(id = R.color.dark_text)
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
                            onClick = { showDbVersionDialog = false },
                            modifier = Modifier
                                .padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = colorResource(id = R.color.not_so_light_gray),
                                contentColor = colorResource(id = R.color.dark_text)
                            )
                        ) {
                            Text(text = stringResource(id = R.string.no))
                        }
                        var dbVersionChangeDialogBtnEnabled by remember { mutableStateOf(true) }
                        Button(
                            enabled = dbVersionChangeDialogBtnEnabled,
                            onClick = {
                                //checking if fields are empty:
                                dialogLoadingVisible = true
                                dbVersionChangeDialogBtnEnabled = false
                                val docRef = firestore.document("DatabaseVersion/${subjectList[currentDbVersionIndex]}")
                                docRef.get().addOnSuccessListener { documentSnapshot ->
                                    val currentVersion =
                                        documentSnapshot.getLong("databaseVersion") ?: 0
                                    if (decreaseDialog) {
                                        docRef.update("databaseVersion", currentVersion - 1)
                                            .addOnSuccessListener {
                                                dbVersionList[currentDbVersionIndex] =
                                                    ((currentVersion - 1).toInt())
                                                dbVersionUpdateConfirmed = true
                                                dbVersionUpdateConfirmedDown = true
                                            }
                                    }
                                    else {
                                        docRef.update("databaseVersion", currentVersion + 1)
                                            .addOnSuccessListener {
                                                dbVersionList[currentDbVersionIndex] =
                                                    ((currentVersion + 1).toInt())
                                                dbVersionUpdateConfirmed = true
                                                dbVersionUpdateConfirmedDown = false
                                            }
                                    }
                                    dialogLoadingVisible = false
                                    dbVersionChangeDialogBtnEnabled = true
                                    showDbVersionDialog = false
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
                                Text(text = stringResource(id = R.string.yes))
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


    fun LazyListScope.gridItems(
        count: Int,
        nColumns: Int,
        horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
        itemContent: @Composable BoxScope.(Int) -> Unit,
    ) {
        gridItems(
            data = List(count) { it },
            nColumns = nColumns,
            horizontalArrangement = horizontalArrangement,
            itemContent = itemContent,
        )
    }

    fun <T> LazyListScope.gridItems(
        data: List<T>,
        nColumns: Int,
        horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
        key: ((item: T) -> Any)? = null,
        itemContent: @Composable BoxScope.(T) -> Unit,
    ) {
        val rows = if (data.isEmpty()) 0 else 1 + (data.count() - 1) / nColumns
        items(rows) { rowIndex ->
            Row(horizontalArrangement = horizontalArrangement) {
                for (columnIndex in 0 until nColumns) {
                    val itemIndex = rowIndex * nColumns + columnIndex
                    if (itemIndex < data.count()) {
                        val item = data[itemIndex]
                        androidx.compose.runtime.key(key?.invoke(item)) {
                            Box(
                                modifier = Modifier.weight(1f, fill = true),
                                propagateMinConstraints = true
                            ) {
                                itemContent.invoke(this, item)
                            }
                        }
                    } else {
                        Spacer(Modifier.weight(1f, fill = true))
                    }
                }
            }
        }
    }
}