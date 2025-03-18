package com.example.jewelleryapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.jewelleryapp.Repository.LoginScreenRepositoryImpl
import com.example.jewelleryapp.screen.loginScreen.LoginScreen
import com.example.jewelleryapp.screen.loginScreen.LoginScreenViewModel
import com.example.jewelleryapp.screen.loginScreen.LoginViewModelFactory
import com.example.jewelleryapp.ui.theme.JewelleryAppTheme
import com.google.firebase.Firebase
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JewelleryAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                //    color = MaterialTheme.colors.background
                ) {
                    val repository = LoginScreenRepositoryImpl()
                    val viewModel: LoginScreenViewModel = viewModel(
                        factory = LoginViewModelFactory(repository)
                    )

                    LoginScreen(
                        viewModel = viewModel,
                        onNavigateToHome = { user ->
                            // Navigate to home screen
                            Toast.makeText(this, "Login successful: ${user.email}", Toast.LENGTH_SHORT).show()
                        },
                        onNavigateToSignUp = {
                            // Navigate to sign up screen
                            Toast.makeText(this, "Navigate to Sign Up", Toast.LENGTH_SHORT).show()
                        },
                        onNavigateToForgotPassword = {
                            // Navigate to forgot password screen
                            Toast.makeText(this, "Navigate to Forgot Password", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}