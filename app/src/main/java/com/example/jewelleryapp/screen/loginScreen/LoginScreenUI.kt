package com.example.jewelleryapp.screen.loginScreen

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.jewelleryapp.R
import com.example.jewelleryapp.Repository.User
import com.example.jewelleryapp.Repository.LoginScreenRepositoryImpl

/**
 * Main Login Screen Composable
 * This screen allows users to sign in with email/phone and password,
 * or alternatively with Google. It also provides options for forgot password
 * and sign up navigation.
 */
@Composable
fun LoginScreen(
    viewModel: LoginScreenViewModel = viewModel(
        factory = LoginViewModelFactory(LoginScreenRepositoryImpl())
    ),
    onNavigateToHome: (User) -> Unit,
    onNavigateToSignUp: () -> Unit,
    onNavigateToForgotPassword: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    // Set up event handling
    SetupEventHandling(
        viewModel = viewModel,
        onNavigateToHome = onNavigateToHome,
        onNavigateToSignUp = onNavigateToSignUp,
        onNavigateToForgotPassword = onNavigateToForgotPassword,
        context = context
    )

    // Main content
    Column(
        modifier = setupRootColumnModifier(focusManager),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BrandHeader()
        Spacer(Modifier.padding(28.dp))

        WelcomeSection()

        EmailInput(
            email = uiState.email,
            onEmailChange = viewModel::onEmailChange
        )

        Spacer(modifier = Modifier.height(16.dp))

        PasswordInput(
            password = uiState.password,
            isPasswordVisible = uiState.isPasswordVisible,
            onPasswordChange = viewModel::onPasswordChange,
            onTogglePasswordVisibility = viewModel::togglePasswordVisibility,
            onForgotPasswordClick = viewModel::onForgotPasswordClick,
            focusManager = focusManager
        )

        Spacer(modifier = Modifier.height(31.dp))

        SignInButton(
            isLoading = uiState.isLoading,
            onClick = viewModel::onSignInClick
        )

        AlternativeSignInOptions(
            isLoading = uiState.isLoading,
            onGoogleSignInClick = viewModel::onGoogleSignInClick
        )

        Spacer(modifier = Modifier.padding(22.dp))

        SignUpPrompt(
            onSignUpClick = viewModel::onSignUpClick
        )
    }
}

/**
 * Sets up event handling for UI events from the ViewModel
 */
@Composable
private fun SetupEventHandling(
    viewModel: LoginScreenViewModel,
    onNavigateToHome: (User) -> Unit,
    onNavigateToSignUp: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    context: android.content.Context
) {
    LaunchedEffect(key1 = true) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is LoginUiEvent.NavigateToHome -> onNavigateToHome(event.user)
                is LoginUiEvent.ShowError -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
                is LoginUiEvent.NavigateToForgotPassword -> onNavigateToForgotPassword()
                is LoginUiEvent.NavigateToSignUp -> onNavigateToSignUp()
            }
        }
    }
}

/**
 * Creates and returns the modifier for the root column
 */
@Composable
private fun setupRootColumnModifier(focusManager: FocusManager): Modifier {
    return Modifier
        .fillMaxSize()
        .padding(16.dp)
        .background(Color.White)
        .clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() }
        ) {
            focusManager.clearFocus()
        }
}

/**
 * Brand header section with logo and company name
 */
@Composable
private fun BrandHeader() {
    Spacer(modifier = Modifier.height(24.dp))

    // Crown Logo
    Image(
        painter = painterResource(id = R.drawable.crown),
        contentDescription = "Crown Logo",
        modifier = Modifier
            .size(80.dp)
            .padding(4.dp)
    )

    Spacer(modifier = Modifier.height(8.dp))
    Spacer(modifier = Modifier.padding(5.dp))
    // Brand name and tagline
    Text(
        text = "Gagan Jewellers",
        fontSize = 35.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFFC4A661) // Gold color
    )

   /* Text(
        text = "Timeless Elegance Since 1985",
        fontSize = 14.sp,
        color = Color.Gray,
        modifier = Modifier.padding(bottom = 24.dp)
    ) */
}

/**
 * Welcome message section
 */
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

/**
 * Email input field
 */
@Composable
private fun EmailInput(
    email: String,
    onEmailChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Email or Phone Number",
            fontSize = 14.sp,
            color = Color.Black,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            placeholder = { Text("Enter your email or phone") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            shape = RoundedCornerShape(8.dp),
            colors = getTextFieldColors()
        )
    }
}

/**
 * Password input field with visibility toggle and forgot password link
 */
@Composable
private fun PasswordInput(
    password: String,
    isPasswordVisible: Boolean,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    focusManager: FocusManager
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Password",
            fontSize = 14.sp,
            color = Color.Black,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            placeholder = { Text("Enter your password") },
            singleLine = true,
            visualTransformation = if (isPasswordVisible)
                VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() }
            ),
            trailingIcon = {
                PasswordVisibilityToggle(
                    isPasswordVisible = isPasswordVisible,
                    onToggleClick = onTogglePasswordVisibility
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(8.dp),
            colors = getTextFieldColors()
        )

        ForgotPasswordLink(onClick = onForgotPasswordClick)
    }
}

/**
 * Password visibility toggle icon button
 */
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

/**
 * Forgot password link
 */
@Composable
private fun ForgotPasswordLink(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.End
    ) {
        Text(
            text = "Forgot Password?",
            fontSize = 12.sp,
            color = Color(0xFFC4A661),
            modifier = Modifier.clickable(onClick = onClick)
        )
    }
}

/**
 * Sign in button with loading indicator
 */
@Composable
private fun SignInButton(
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
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

/**
 * Alternative sign-in options section (Google)
 */
@Composable
private fun AlternativeSignInOptions(
    isLoading: Boolean,
    onGoogleSignInClick: () -> Unit
) {
    Spacer(modifier = Modifier.height(24.dp))

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Divider(
            modifier = Modifier
                .weight(1f)
                .height(1.dp),
            color = Color.LightGray
        )

        Text(
            text = "Or continue with",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Divider(
            modifier = Modifier
                .weight(1f)
                .height(1.dp),
            color = Color.LightGray
        )
    }

    Spacer(modifier = Modifier.height(29.dp))

    GoogleSignInButton(
        isLoading = isLoading,
        onClick = onGoogleSignInClick
    )
}

/**
 * Google sign-in button
 */
@Composable
private fun GoogleSignInButton(
    isLoading: Boolean,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            backgroundColor = Color.White
        ),
        border = BorderStroke(1.dp, Color.LightGray),
        shape = RoundedCornerShape(8.dp),
        enabled = !isLoading
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.google_icon),
                contentDescription = "Google Logo",
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Continue with Google",
                color = Color.Black,
                fontSize = 16.sp
            )
        }
    }
}

/**
 * Sign up prompt at the bottom of the screen
 */
@Composable
private fun SignUpPrompt(onSignUpClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Don't have an account? ",
            fontSize = 14.sp,
            fontWeight = FontWeight.W400,
            color = Color.Gray
        )
        Text(
            text = "Sign Up",
            fontSize = 14.sp,
            fontWeight = FontWeight.W600,
            color = Color(0xFFC4A661),
            modifier = Modifier.clickable(onClick = onSignUpClick)
        )
    }
}

/**
 * Returns consistent text field colors for all text fields
 */
@Composable
private fun getTextFieldColors() = TextFieldDefaults.outlinedTextFieldColors(
    focusedBorderColor = Color(0xFFC4A661),
    unfocusedBorderColor = Color.LightGray
)