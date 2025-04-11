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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.jewelleryapp.repository.FirebaseAuthRepository
import com.example.jewelleryapp.repository.FirebaseStorageHelper
import com.example.jewelleryapp.repository.JewelryRepository
import com.example.jewelleryapp.screen.categoriesScreen.CategoriesViewModel
import com.example.jewelleryapp.screen.categoriesScreen.CategoryScreenView
import com.example.jewelleryapp.screen.homeScreen.HomeScreen
import com.example.jewelleryapp.screen.homeScreen.HomeViewModel
import com.example.jewelleryapp.screen.itemdetailScreen.ItemDetailViewModel
import com.example.jewelleryapp.screen.itemdetailScreen.JewelryProductScreen
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
    private lateinit var categoryViewModel: CategoriesViewModel
    private lateinit var itemDetailViewModel: ItemDetailViewModel

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
        categoryViewModel = CategoriesViewModel(jewelryRepository)
        itemDetailViewModel = ItemDetailViewModel(jewelryRepository)

        enableEdgeToEdge()
        setContent {
            JewelleryAppTheme {
                Surface(
                    modifier = Modifier.background(Color.White)
                ) {
                    AppNavigation(loginViewModel, registerViewModel, homeViewModel, itemDetailViewModel)
                }
            }
        }
    }
}

@Composable
fun AppNavigation(
    loginViewModel: LoginViewModel,
    registerViewModel: RegisterViewModel,
    homeViewModel: HomeViewModel,
    itemDetailViewModel: ItemDetailViewModel
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "login") {
        // Login Screen
        composable("login") {
            LoginScreen(loginViewModel, navController)
        }

        // Register Screen
        composable("register") {
            RegisterScreen(registerViewModel, navController)
        }

        // Home Screen
        composable("home") {
            HomeScreen(
                viewModel = homeViewModel,
                onCategoryClick = { categoryId ->
                    // Navigate to category detail screen
                    navController.navigate("category/$categoryId")
                },
                onProductClick = { productId ->
                    // Navigate to product detail screen
                    navController.navigate("itemDetail/$productId")
                },
                onCollectionClick = { collectionId ->
                    // Navigate to collection screen
                    navController.navigate("collection/$collectionId")
                }
            )
        }

        // Item Detail Screen
        composable(
            route = "itemDetail/{productId}",
            arguments = listOf(
                navArgument("productId") {
                    type = NavType.StringType
                    nullable = false
                }
            )
        ) { backStackEntry ->
            // Extract the product ID from navigation arguments
            val productId = backStackEntry.arguments?.getString("productId") ?: ""

            // Display the Jewelry Product Screen
            JewelryProductScreen(
                productId = productId,
                viewModel = itemDetailViewModel,
                onBackClick = {
                    // Navigate back to the previous screen
                    navController.popBackStack()
                },
                onProductClick = { selectedProductId ->
                    // Navigate to the selected product's detail screen
                    navController.navigate("itemDetail/$selectedProductId") {
                        // Pop up to the current product detail to avoid stacking multiple details
                        popUpTo("itemDetail/$productId") {
                            inclusive = true
                        }
                    }
                }
            )
        }
    }
}








