package com.eduscope.eduscopeadminpanel

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.*
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

class ForgotPasswordActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ForgotPasswordScreen()
        }
    }


    @Preview
    @Composable
    fun ForgotPasswordScreen() {
        val compositionLock by rememberLottieComposition(spec = LottieCompositionSpec.RawRes(R.raw.lock_lottie_anim))
        val compositionMailSent by rememberLottieComposition(spec = LottieCompositionSpec.RawRes(R.raw.mail_sent_anim))
        val recoverPasswordStr = stringResource(id = R.string.recover_password)
        val mailSentStr = stringResource(id = R.string.mail_sent)
        var mailSentAnim = remember {
            mutableStateOf(false)
        }
        val errorOccuredStr = stringResource(id = R.string.error_occured)
        val invalidMailStr = stringResource(id = R.string.invalid_mail)
        val backToLoginScreenStr = stringResource(id = R.string.back_to_login_screen)
        val allFieldsRequiredStr = stringResource(id = R.string.all_fields_required)
        var showProgress by remember { mutableStateOf(false) }
        val buttonTextSize = remember { mutableStateOf(16.sp) }

        val emailText = remember {
            mutableStateOf("")
        }
        val buttonText = remember {
            mutableStateOf(recoverPasswordStr)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorResource(id = R.color.BSUIR_Blue))
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 30.dp)
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(0.6f)
            ) {
                Text(
                    text = stringResource(id = R.string.password_recovery),
                    color = colorResource(id = R.color.white),
                    fontSize = 24.sp,
                    modifier = Modifier
                        .padding(top = 24.dp)
                        .fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                LottieAnimation(
                    composition = compositionLock,
                    iterations = 1,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxSize(0.6f)
                        .alpha(if (mailSentAnim.value == false) 1f else 0f)
                )
                if (mailSentAnim.value) {
                    LottieAnimation(
                        composition = compositionMailSent,
                        iterations = 1,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxSize(0.55f)
                    )
                }
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
                        buttonText.value = recoverPasswordStr
                        buttonTextSize.value = 16.sp
                    },
                    label = { Text(stringResource(id = R.string.email)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    textStyle = TextStyle(
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
                        buttonText.value = recoverPasswordStr
                        buttonTextSize.value = 16.sp
                        showProgress = true
                        var emailStr = emailText.value.trim()
                        if (emailStr.isEmpty()) {
                            showProgress = false
                            buttonText.value = allFieldsRequiredStr
                            buttonTextSize.value = 16.sp
                        } else {
                            val emailExists = booleanArrayOf(false)
                            val firestore = FirebaseFirestore.getInstance()
                            firestore.collection("Users").get().addOnCompleteListener(
                                OnCompleteListener<QuerySnapshot> { task ->
                                    if (task.isSuccessful) {
                                        outerloop@ for (document in task.result) {
                                            if (document["email"] == emailStr) {
                                                emailExists[0] = true
                                                break@outerloop
                                            }
                                        }
                                    }
                                    if (emailExists[0]) {
                                        val auth = FirebaseAuth.getInstance()
                                        auth.sendPasswordResetEmail(emailStr).addOnCompleteListener(
                                            OnCompleteListener<Void?> { task ->
                                                showProgress = false
                                                if (task.isSuccessful) {
                                                    buttonText.value = mailSentStr
                                                    mailSentAnim.value = true
                                                } else {
                                                    buttonText.value = errorOccuredStr
                                                }
                                            })
                                    } else {
                                        showProgress = false
                                        buttonText.value = invalidMailStr
                                    }
                                })
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
                    text = backToLoginScreenStr,
                    color = colorResource(id = R.color.white),
                    fontSize = 18.sp,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 10.dp)
                        .clickable(onClick = {
                            startActivity(
                                Intent(
                                    this@ForgotPasswordActivity,
                                    LoginActivity::class.java
                                )
                            )
                            finish()
                        }),
                    style = androidx.compose.ui.text.TextStyle(
                        textDecoration = TextDecoration.Underline
                    )
                )
            }
        }
    }
}