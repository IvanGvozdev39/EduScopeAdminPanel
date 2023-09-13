package com.eduscope.eduscopeadminpanel

import android.app.KeyguardManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.biometrics.BiometricPrompt
import android.icu.lang.UCharacter
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.util.function.IntFunction
import java.util.function.Predicate


class BiometricsActivity : ComponentActivity() {

    private var cancellationSignal: CancellationSignal? = null
    private val authenticationCallback: BiometricPrompt.AuthenticationCallback
        get() =
            @RequiresApi(Build.VERSION_CODES.P)
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    startActivity(Intent(this@BiometricsActivity, MainActivity::class.java));
                    finish()
                }
            }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val firestore = FirebaseFirestore.getInstance()
        val user = FirebaseAuth.getInstance().currentUser
        var passwordExists: Boolean
        var correctPassword = ""

        if (user != null) {
            setContent { LoadingScreen() }
            firestore.collection("Users").document(user.uid).get()
                .addOnCompleteListener { task: Task<DocumentSnapshot> ->
                    if (task.isSuccessful) {
                        val document = task.getResult()
                        passwordExists = document.get("adminPanelPassword") != null
                        if (passwordExists)
                            correctPassword = document.get("adminPanelPassword").toString()
                        setContent {
                            BiometricsScreen(passwordExists, correctPassword)
                        }
                        callBiometricAuth()
                    }
                }
        }
    }


    private fun callBiometricAuth() {
        checkBiometricSupport()
        val biometricPrompt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            BiometricPrompt.Builder(this)
                .setTitle(this.resources.getString(R.string.biometrics_login))
                .setNegativeButton(
                    resources.getString(R.string.cancel),
                    this.mainExecutor,
                    DialogInterface.OnClickListener { dialog, which -> })
                .build()
        } else {
            TODO("VERSION.SDK_INT < P")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            biometricPrompt.authenticate(getCancellationSignal(), mainExecutor, authenticationCallback)
        }
    }


    private fun getCancellationSignal(): CancellationSignal {
        cancellationSignal = CancellationSignal()
        cancellationSignal?.setOnCancelListener {
        }
        return cancellationSignal as CancellationSignal
    }


    private fun checkBiometricSupport(): Boolean {
        val keyguardManager: KeyguardManager =
            getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

        if (!keyguardManager.isKeyguardSecure) {
            createToast(resources.getString(R.string.fingerprint_not_set))
            return false
        }
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.USE_BIOMETRIC
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            createToast(resources.getString(R.string.biometrics_not_allowed))
            return false
        }

        return if (packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)) true
        else true
    }

    /*fun checkBiometricSupport() : Int {
        val biometricManager = BiometricManager.from(this)
        when(biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)) {
            BIOMETRIC_SUCCESS -> {
                Log.d("biometricAuth", "App can authenticate using biometrics")
                return 0
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                Log.d("biometricAuth", "No hardware")
                return 1
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                Log.d("biometricAuth", "There is hardware but fingerprint not set")
                //change the title text accordingly
                //Add a button for setting biometry up:
                *//*val enrollIntent = Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                    putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED, BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
                }*//* //This intent should be taken out of the fun
                return 2
            }
        }
        return -1
    }*/


    @Composable
    private fun LoadingScreen() {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorResource(id = R.color.BSUIR_Blue))
        ) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(40.dp)
                    .padding(8.dp) // Add padding if needed
                    .wrapContentSize(align = Alignment.Center)
                    .align(Alignment.Center),
                color = colorResource(id = R.color.white), // Set the color of the progress indicator
                strokeWidth = 2.5.dp, // Set the width of the stroke
            )
        }
    }


    @Composable
    private fun BiometricsScreen(passwordExists: Boolean, correctPassword: String) {

        val passwordSetStr = stringResource(id = R.string.password_successfully_set)
        val logInStr = stringResource(id = R.string.log_in)
        val allFieldsRequiredStr = stringResource(id = R.string.all_fields_required)
        val loginConfirmEmailStr = stringResource(R.string.login_confirm_email)
        val loginErrorStr = stringResource(id = R.string.login_error)
        var showProgress by remember { mutableStateOf(false) }
        val buttonTextSize = remember { mutableStateOf(16.sp) }
        var passwordVisibility by remember { mutableStateOf(false) }
        var passwordVisibility2 by remember { mutableStateOf(false) }
        val accessDeniedStr = stringResource(id = R.string.access_denied)
        val passwordsNotSameStr = stringResource(id = R.string.passwords_not_same)
        val passwordRestrictionsStr = stringResource(id = R.string.password_restrictions)
        val passwordStr = stringResource(id = R.string.repeat_password)
        val passwordStr2Mode = stringResource(id = R.string.password)
        val wrongPasswordStr = stringResource(id = R.string.wrong_password)

        val password2Label = remember {
            mutableStateOf(passwordStr2Mode)
        }
        val imageResId = remember {
            mutableStateOf(R.drawable.biometrics)
        }
        val titleResId = remember {
            mutableStateOf(R.string.enter_key_or_use_biometrics)
        }
        val password1Visibility = remember {
            mutableStateOf(0f)
        }
        val password1Text = remember {
            mutableStateOf("")
        }
        val password2Text = remember {
            mutableStateOf("")
        }
        val buttonText = remember {
            mutableStateOf(logInStr)
        }
        val setPasswordStr = stringResource(id = R.string.set_password)

        if (!passwordExists) {
            titleResId.value = R.string.come_up_with_a_password
            password1Visibility.value = 1f
            buttonText.value = setPasswordStr
            imageResId.value = R.drawable.padlock
            password2Label.value = passwordStr
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
                    text = stringResource(id = titleResId.value),
                    color = colorResource(id = R.color.white),
                    fontSize = 22.sp,
                    modifier = Modifier
                        .padding(top = 24.dp)
                        .fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Image(
                    painter = painterResource(id = imageResId.value),
                    contentDescription = "Logo",
                    modifier = Modifier
                        .size(200.dp)
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 85.dp)
                        .clickable(onClick = { if (imageResId.value == R.drawable.biometrics) callBiometricAuth() }),
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
                    value = password1Text.value,
                    onValueChange = { updatedValue ->
                        password1Text.value = updatedValue
                        buttonText.value = logInStr
                        buttonTextSize.value = 16.sp
                    },
                    label = { Text(stringResource(id = R.string.password)) },
                    singleLine = true,
                    modifier = Modifier
                        .alpha(password1Visibility.value)
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
                    },
                    enabled = if (password1Visibility.value > 0f) true else false
                )

                TextField(
                    value = password2Text.value,
                    onValueChange = { updatedValue ->
                        password2Text.value = updatedValue
                        buttonText.value = logInStr
                        buttonTextSize.value = 16.sp
                    },
                    label = { Text(password2Label.value) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .border(
                            width = 1.dp,
                            color = if (passwordVisibility2) colorResource(id = R.color.black) else colorResource(
                                id = R.color.dark_gray
                            ),
                            shape = RoundedCornerShape(5.dp)
                        ),
                    textStyle = TextStyle(
                        fontSize = 16.sp // set the desired font size here
                    ),
                    colors = TextFieldDefaults.textFieldColors(
                        textColor = colorResource(id = R.color.dark_text),
                        backgroundColor = colorResource(id = R.color.white),
                        cursorColor = colorResource(id = R.color.dark_text),
                        focusedLabelColor = colorResource(id = R.color.BSUIR_Blue),
                        unfocusedLabelColor = colorResource(id = R.color.dark_text)
                    ),
                    visualTransformation = if (passwordVisibility2) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val image =
                            if (passwordVisibility2) R.drawable.ic_baseline_visibility_off_24 else R.drawable.ic_baseline_visibility_24
                        val icon = painterResource(id = image)
                        IconButton(
                            onClick = { passwordVisibility2 = !passwordVisibility2 }
                        ) {
                            Icon(
                                icon,
                                contentDescription = if (passwordVisibility2) stringResource(id = R.string.hide_password) else stringResource(
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
                        if (!passwordExists) {
                            val password1Str = password1Text.value
                            val password2Str = password2Text.value
                            if (password1Str.isEmpty() || password2Str.isEmpty()) {
                                buttonText.value =
                                    allFieldsRequiredStr
                                buttonTextSize.value = 16.sp
                            } else {
                                if (password1Str != password2Str)
                                    buttonText.value = passwordsNotSameStr
                                else {
                                    //Password check:
                                    var passwordCyrillicFound = false
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                        passwordCyrillicFound = password1Str.chars()
                                            .mapToObj<UCharacter.UnicodeBlock>(IntFunction { ch: Int ->
                                                UCharacter.UnicodeBlock.of(
                                                    ch
                                                )
                                            })
                                            .anyMatch(Predicate { b: UCharacter.UnicodeBlock -> b == UCharacter.UnicodeBlock.CYRILLIC })
                                    }

                                    var passwordNoNumbersFound = true
                                    for (i in 0 until password1Str.length) {
                                        if (Character.isDigit(password1Str.get(i))) {
                                            passwordNoNumbersFound = false
                                            break
                                        }
                                    }

                                    var passwordDoesntContainLetters = true
                                    for (i in 0 until password1Str.length) {
                                        if (Character.isLetter(password1Str.get(i))) {
                                            passwordDoesntContainLetters = false
                                            break
                                        }
                                    }

                                    val passwordWrongLength: Boolean = password1Str.length < 7

                                    if (passwordCyrillicFound || passwordNoNumbersFound || passwordDoesntContainLetters || passwordWrongLength) {
                                        buttonText.value = passwordRestrictionsStr
                                        buttonTextSize.value = 11.sp
                                    } else {
                                        //Наконец-то можно сохранить пароль
                                        showProgress = true
                                        val firestore = FirebaseFirestore.getInstance()
                                        val user = FirebaseAuth.getInstance().currentUser
                                        if (user != null) {
                                            firestore.collection("Users").document(user.uid)
                                                .update("adminPanelPassword", password1Str)
                                                .addOnSuccessListener {
                                                    Toast.makeText(
                                                        this@BiometricsActivity,
                                                        passwordSetStr,
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                    val intent = Intent(
                                                        this@BiometricsActivity,
                                                        MainActivity::class.java
                                                    )
                                                    startActivity(intent)
                                                    finish()
                                                }
                                        }
                                    }
                                }
                            }
                        } else {
                            val password2Str = password2Text.value
                            if (!password2Str.equals(correctPassword)) {
                                buttonText.value = wrongPasswordStr
                            } else {
                                showProgress = true;
                                val intent =
                                    Intent(this@BiometricsActivity, MainActivity::class.java)
                                startActivity(intent)
                                finish()
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
            }
        }
    }

    private fun createToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}