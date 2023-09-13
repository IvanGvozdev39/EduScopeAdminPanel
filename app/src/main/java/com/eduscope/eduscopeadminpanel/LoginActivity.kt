package com.eduscope.eduscopeadminpanel

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val firebaseUser = FirebaseAuth.getInstance().currentUser
        if (firebaseUser != null) {
            val autoIntent = Intent(this@LoginActivity, BiometricsActivity::class.java)
//            autoIntent.putExtra("loggedInManually", false)
            startActivity(autoIntent)
            finish()
        }

        setContent {
            LoginScreen()
        }
    }

    @Preview
    @Composable
    fun LoginScreen() {
        val logInStr = stringResource(id = R.string.log_in)
        val allFieldsRequiredStr = stringResource(id = R.string.all_fields_required)
        val loginConfirmEmailStr = stringResource(R.string.login_confirm_email)
        val loginErrorStr = stringResource(id = R.string.login_error)
        var showProgress by remember { mutableStateOf(false) }
        val buttonTextSize = remember { mutableStateOf(16.sp) }
        var passwordVisibility by remember { mutableStateOf(false) }
        val accessDeniedStr = stringResource(id = R.string.access_denied)
        val forgotPasswordStr = stringResource(id = R.string.forgot_password)

        val emailText = remember {
            mutableStateOf("")
        }
        val passwordText = remember {
            mutableStateOf("")
        }
        val buttonText = remember {
            mutableStateOf(logInStr)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorResource(id = R.color.BSUIR_Blue))
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 30.dp)
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
            ) {
                Text(
                    text = stringResource(id = R.string.app_name),
                    color = colorResource(id = R.color.white),
                    fontSize = 22.sp,
                    modifier = Modifier
                        .padding(top = 24.dp)
                        .fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Image(
                    painter = painterResource(id = R.drawable.logo_admin_panel_blank),
                    contentDescription = "Logo",
                    modifier = Modifier
                        .size(240.dp)
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 85.dp)
                )
            }

            Column(
                modifier = Modifier
                    .padding(horizontal = 30.dp)
                    .padding(bottom = 100.dp)
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {

                TextField(
                    value = emailText.value,
                    onValueChange = { updatedValue ->
                        emailText.value = updatedValue
                        buttonText.value = logInStr
                        buttonTextSize.value = 16.sp
                    },
                    label = { Text(stringResource(id = R.string.email)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 16.sp // set the desired font size here
                    ),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = colorResource(id = R.color.dark_text),
                        focusedBorderColor = colorResource(id = R.color.transparent),
                        unfocusedBorderColor = colorResource(id = R.color.transparent),
                        backgroundColor = colorResource(id = R.color.white),
                        cursorColor = colorResource(id = R.color.dark_text),
                        focusedLabelColor = colorResource(id = R.color.BSUIR_Blue),
                        unfocusedLabelColor = colorResource(id = R.color.dark_text)
                    )
                )

                TextField(
                    value = passwordText.value,
                    onValueChange = { updatedValue ->
                        passwordText.value = updatedValue
                        buttonText.value = logInStr
                        buttonTextSize.value = 16.sp
                    },
                    label = { Text(stringResource(id = R.string.password)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .border(
                            width = 1.dp,
                            color = if (passwordVisibility) colorResource(id = R.color.black) else colorResource(
                                id = R.color.dark_gray
                            ),
                            shape = RoundedCornerShape(5.dp)
                        ),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 16.sp // set the desired font size here
                    ),
                    colors = TextFieldDefaults.textFieldColors(
                        textColor = colorResource(id = R.color.dark_text),
                        backgroundColor = colorResource(id = R.color.white),
                        cursorColor = colorResource(id = R.color.dark_text),
                        focusedLabelColor = colorResource(id = R.color.BSUIR_Blue),
                        unfocusedLabelColor = colorResource(id = R.color.dark_text)
                    ),
                    visualTransformation = if (passwordVisibility) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val image =
                            if (passwordVisibility) R.drawable.ic_baseline_visibility_off_24 else R.drawable.ic_baseline_visibility_24
                        val icon = painterResource(id = image)
                        IconButton(
                            onClick = { passwordVisibility = !passwordVisibility }
                        ) {
                            Icon(
                                icon,
                                contentDescription = if (passwordVisibility) stringResource(id = R.string.hide_password) else stringResource(
                                    id = R.string.show_password
                                )
                            )
                        }
                    }
                )

                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(65.dp)
                        .padding(vertical = 5.dp)
                        .clip(RoundedCornerShape(20.dp)),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = colorResource(id = R.color.white),
                        contentColor = colorResource(id = R.color.black)
                    ),
                    onClick = {
                        buttonText.value = logInStr
                        buttonTextSize.value = 16.sp
                        showProgress = true
                        var emailStr = emailText.value.trim()
                        var passwordStr = passwordText.value
                        if (emailStr.isEmpty() || passwordStr.isEmpty()) {
                            showProgress = false
                            buttonText.value = allFieldsRequiredStr
                            buttonTextSize.value = 16.sp
                        } else {
                            val firebaseAuth = FirebaseAuth.getInstance()
                            firebaseAuth.signInWithEmailAndPassword(emailStr, passwordStr)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        val user = FirebaseAuth.getInstance().currentUser
                                        if (user != null) {
                                            if (user.isEmailVerified) {
                                                val firestore = FirebaseFirestore.getInstance()
                                                firestore.collection("Users")
                                                    .document(user.uid)
                                                    .get()
                                                    .addOnCompleteListener { task1: Task<DocumentSnapshot> ->
                                                        if (task1.isSuccessful) {
                                                            val document = task1.result
                                                            if (document.get("role")?.equals("owner") == true) { //equals will only be called if document.get("role") is not null
                                                                val intent = Intent(
                                                                    this@LoginActivity,
                                                                    BiometricsActivity::class.java
                                                                )
                                                                startActivity(intent)
                                                                finish()
                                                            }
                                                            else {
                                                                FirebaseAuth.getInstance().signOut()
                                                                showProgress = false
                                                                buttonText.value = accessDeniedStr
                                                                buttonTextSize.value = 16.sp
                                                            }
                                                        }
                                                    }
                                            } else {
                                                user.sendEmailVerification()
                                                    .addOnCompleteListener { innerTask ->
                                                        showProgress = false
                                                        buttonText.value =
                                                            loginConfirmEmailStr
                                                        buttonTextSize.value = 11.sp
                                                    }
                                            }
                                        }
                                    } else {
                                        showProgress = false
                                        buttonText.value =
                                            loginErrorStr
                                        buttonTextSize.value = 11.sp
                                    }
                                }
                        }
                    }
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            buttonText.value,
                            fontSize = buttonTextSize.value,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.alpha(if (showProgress) 0f else 1f)
                        )
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(40.dp)
                                .padding(8.dp) // Add padding if needed
                                .wrapContentSize(align = Alignment.Center)
                                .alpha(if (showProgress) 1f else 0f)
                                .align(Alignment.Center),
                            color = colorResource(id = R.color.BSUIR_Blue), // Set the color of the progress indicator
                            strokeWidth = 2.5.dp, // Set the width of the stroke
                        )
                    }


                }

                Text(
                    text = forgotPasswordStr,
                    color = colorResource(id = R.color.white),
                    fontSize = 18.sp,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 10.dp)
                        .clickable(onClick = {
                            startActivity(
                                Intent(
                                    this@LoginActivity,
                                    ForgotPasswordActivity::class.java
                                )
                            )
                        }),
                    style = androidx.compose.ui.text.TextStyle(
                        textDecoration = TextDecoration.Underline
                    )
                )
            }
        }
    }
}