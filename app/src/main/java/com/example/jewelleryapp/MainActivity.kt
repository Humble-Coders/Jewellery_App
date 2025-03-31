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
import com.example.jewelleryapp.repository.FirebaseStorageHelper
import com.example.jewelleryapp.repository.JewelryRepository
import com.example.jewelleryapp.screen.homeScreen.HomeScreen
import com.example.jewelleryapp.screen.homeScreen.HomeViewModel
import com.example.jewelleryapp.screen.loginScreen.LoginScreen
import com.example.jewelleryapp.screen.loginScreen.LoginViewModel
import com.example.jewelleryapp.screen.registerScreen.RegisterScreen
import com.example.jewelleryapp.screen.registerScreen.RegisterViewModel
import com.example.jewelleryapp.ui.theme.JewelleryAppTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    private lateinit var loginViewModel: LoginViewModel
    private lateinit var registerViewModel: RegisterViewModel
    private lateinit var homeViewModel: HomeViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase Auth
        val firebaseAuth = FirebaseAuth.getInstance()

        // Initialize Firestore
        val firestore = FirebaseFirestore.getInstance()


        val storageHelper = FirebaseStorageHelper()


        // Initialize Repositories
        val authRepository = FirebaseAuthRepository(firebaseAuth)
        val jewelryRepository = JewelryRepository(firestore, storageHelper)

        // Initialize ViewModels
        loginViewModel = LoginViewModel(authRepository)
        registerViewModel = RegisterViewModel(authRepository)
        homeViewModel = HomeViewModel(jewelryRepository)

        enableEdgeToEdge()
        setContent {
            JewelleryAppTheme {
                Surface(
                    modifier = Modifier.background(Color.White)
                ) {
                    AppNavigation(loginViewModel, registerViewModel, homeViewModel)
                }
            }
        }
    }
}

@Composable
fun AppNavigation(
    loginViewModel: LoginViewModel,
    registerViewModel: RegisterViewModel,
    homeViewModel: HomeViewModel
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(loginViewModel, navController)
        }

        composable("register") {
            RegisterScreen(registerViewModel, navController)
        }

        composable("home") {
            HomeScreen(
                viewModel = homeViewModel,
                onCategoryClick = { categoryId ->
                    // Navigate to category detail screen
                    // navController.navigate("category/$categoryId")
                },
                onProductClick = { productId ->
                    // Navigate to product detail screen
                    // navController.navigate("product/$productId")
                },
                onCollectionClick = { collectionId ->
                    // Navigate to collection screen
                    // navController.navigate("collection/$collectionId")
                }
            )
        }
    }
}








