package com.example.jewelleryapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.jewelleryapp.repository.FirebaseAuthRepository
import com.example.jewelleryapp.screen.loginScreen.LoginScreen
import com.example.jewelleryapp.screen.loginScreen.LoginViewModel
import com.example.jewelleryapp.screen.registerScreen.RegisterScreen
import com.example.jewelleryapp.screen.registerScreen.RegisterViewModel
import com.example.jewelleryapp.ui.theme.JewelleryAppTheme
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    private lateinit var loginViewModel: LoginViewModel
    private lateinit var RegisterViewModel: RegisterViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize Firebase Auth
        val firebaseAuth = FirebaseAuth.getInstance()

        // Initialize Repository
        val authRepository = FirebaseAuthRepository(firebaseAuth)

        // Initialize ViewModel
        loginViewModel = LoginViewModel(authRepository)
        RegisterViewModel = RegisterViewModel(authRepository)
        enableEdgeToEdge()
        setContent {
            JewelleryAppTheme {
                Surface(
                    modifier = Modifier.background(Color.White)
                ) {
                    AppNavigation(loginViewModel, RegisterViewModel)
                }
            }
        }
    }
}

@Composable
fun AppNavigation(loginViewModel: LoginViewModel, registerViewModel: RegisterViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(loginViewModel, navController)
        }
        // Add more screens as needed
        composable("register") {
            RegisterScreen(registerViewModel, navController)
        }

        composable("home") {
            // HomeScreen()

        }
    }
}