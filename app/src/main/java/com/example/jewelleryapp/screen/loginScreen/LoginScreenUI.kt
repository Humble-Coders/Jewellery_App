package com.example.jewelleryapp.screen.loginScreen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jewelleryapp.R

@Composable
fun LoginScreen() {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(Color.White)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                focusManager.clearFocus()
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BrandHeader()
        Spacer(Modifier.padding(28.dp))

        WelcomeSection()

        EmailInput(email = email, onEmailChange = { email = it })
        Spacer(modifier = Modifier.height(16.dp))

        PasswordInput(
            password = password,
            isPasswordVisible = isPasswordVisible,
            onPasswordChange = { password = it },
            onTogglePasswordVisibility = { isPasswordVisible = !isPasswordVisible },
            onForgotPasswordClick = { /* Handle forgot password */ }
        )


        Spacer(modifier = Modifier.height(31.dp))

        SignInButton(isLoading = isLoading)

        AlternativeSignInOptions(isLoading = isLoading)

        Spacer(modifier = Modifier.padding(22.dp))

        SignUpPrompt()
    }
}

@Composable
private fun BrandHeader() {
    Spacer(modifier = Modifier.height(24.dp))

    Image(
        painter = painterResource(id = R.drawable.crown),
        contentDescription = "Crown Logo",
        modifier = Modifier.size(80.dp)
    )

    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "Gagan Jewellers",
        fontSize = 35.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFFC4A661)
    )
}

@Composable
private fun WelcomeSection() {
    Text(
        text = "Welcome Back",
        fontSize = 33.sp,
        fontWeight = FontWeight.Medium,
        color = Color.Black
    )
    Spacer(modifier = Modifier.padding(8.dp))

    Text(
        text = "Please sign in to continue",
        fontSize = 16.sp,
        color = Color.Gray,
        modifier = Modifier.padding(bottom = 24.dp)
    )
}

@Composable
private fun EmailInput(email: String, onEmailChange: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Email or Phone Number", fontSize = 14.sp, color = Color.Black)

        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            placeholder = { Text("Enter your email or phone") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            shape = RoundedCornerShape(8.dp),
            colors = getTextFieldColors()
        )
    }
}

@Composable
private fun PasswordInput(
    password: String,
    isPasswordVisible: Boolean,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onForgotPasswordClick: () -> Unit

) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Password", fontSize = 14.sp, color = Color.Black)

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            placeholder = { Text("Enter your password") },
            singleLine = true,
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            trailingIcon = {
                PasswordVisibilityToggle(
                    isPasswordVisible = isPasswordVisible,
                    onToggleClick = onTogglePasswordVisibility
                )
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = getTextFieldColors()
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = "Forgot Password?",
                fontSize = 12.sp,
                color = Color(0xFFC4A661), // Gold color
                modifier = Modifier.clickable(onClick = onForgotPasswordClick)
            )
        }
    }
}

@Composable
private fun PasswordVisibilityToggle(
    isPasswordVisible: Boolean,
    onToggleClick: () -> Unit
) {
    IconButton(onClick = onToggleClick) {
        Icon(
            imageVector = if (isPasswordVisible)
                Icons.Default.Visibility else Icons.Default.VisibilityOff,
            contentDescription = "Toggle password visibility"
        )
    }
}

@Composable
private fun SignInButton(isLoading: Boolean) {
    Button(
        onClick = { /* Handle sign in */ },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = Color(0xFFC4A661)
        ),
        shape = RoundedCornerShape(8.dp),
        enabled = !isLoading
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(24.dp)
            )
        } else {
            Text(
                text = "Sign In",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun AlternativeSignInOptions(isLoading: Boolean) {
    Spacer(modifier = Modifier.height(24.dp))

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Divider(modifier = Modifier.weight(1f), color = Color.LightGray)
        Text("Or continue with", fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(horizontal = 8.dp))
        Divider(modifier = Modifier.weight(1f), color = Color.LightGray)
    }

    Spacer(modifier = Modifier.height(29.dp))

    OutlinedButton(
        onClick = { /* Handle Google sign in */ },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.outlinedButtonColors(backgroundColor = Color.White),
        border = BorderStroke(1.dp, Color.LightGray),
        shape = RoundedCornerShape(8.dp),
        enabled = !isLoading
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Image(
                painter = painterResource(id = R.drawable.google_icon),
                contentDescription = "Google Logo",
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Continue with Google", color = Color.Black, fontSize = 16.sp)
        }
    }
}

@Composable
private fun SignUpPrompt() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Don't have an account? ", fontSize = 14.sp, color = Color.Gray)
        Text(
            text = "Sign Up",
            fontSize = 14.sp,
            fontWeight = FontWeight.W600,
            color = Color(0xFFC4A661),
            modifier = Modifier.clickable { /* Handle sign up */ }
        )
    }
}

@Composable
private fun getTextFieldColors() = TextFieldDefaults.outlinedTextFieldColors(
    focusedBorderColor = Color(0xFFC4A661),
    unfocusedBorderColor = Color.LightGray
)
