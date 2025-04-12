package com.example.jewelleryapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.jewelleryapp.repository.CachedJewelryRepository
import com.example.jewelleryapp.repository.FirebaseAuthRepository
import com.example.jewelleryapp.repository.FirebaseStorageHelper
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
import com.example.jewelleryapp.screen.wishlist.WishlistScreen
import com.example.jewelleryapp.screen.wishlist.WishlistViewModel
import com.example.jewelleryapp.ui.theme.JewelleryAppTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var loginViewModel: LoginViewModel
    private lateinit var registerViewModel: RegisterViewModel
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var categoryViewModel: CategoriesViewModel
    private lateinit var itemDetailViewModel: ItemDetailViewModel
    private lateinit var wishlistViewModel: WishlistViewModel
    private val _isInitialLoading = MutableStateFlow(true)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase Auth
        val firebaseAuth = FirebaseAuth.getInstance()

        // Initialize Firestore
        val firestore = FirebaseFirestore.getInstance()

        val storageHelper = FirebaseStorageHelper()

        // Get the CacheManager from the Application
        val cacheManager = (application as JewelryApplication).cacheManager

        // Initialize Repositories
        val authRepository = FirebaseAuthRepository(firebaseAuth)

        // Get current user ID for repository (or empty string if not logged in)
        val userId = firebaseAuth.currentUser?.uid ?: ""
        Log.d("MainActivity", "User ID: $userId")

        // Use CachedJewelryRepository instead of JewelryRepository
        val jewelryRepository = CachedJewelryRepository(
            userId,
            firestore,
            storageHelper,
            cacheManager,
            applicationContext
        )

        // Initialize ViewModels
        loginViewModel = LoginViewModel(authRepository)
        registerViewModel = RegisterViewModel(authRepository)
        homeViewModel = HomeViewModel(jewelryRepository)
        categoryViewModel = CategoriesViewModel(jewelryRepository)
        itemDetailViewModel = ItemDetailViewModel(jewelryRepository)
        wishlistViewModel = WishlistViewModel(jewelryRepository)

        enableEdgeToEdge()
        setContent {
            val isInitialLoading by _isInitialLoading.collectAsState()
            val isConnected by connectivityState()
            var wasConnected by remember { mutableStateOf(isConnected) }

            // Update wasConnected when connection state changes
            LaunchedEffect(isConnected) {
                if (wasConnected != isConnected) {
                    wasConnected = isConnected
                }
            }

            JewelleryAppTheme {
                if (isInitialLoading) {
                    LoadingScreen()
                } else {
                    Box {
                        Surface(
                            modifier = Modifier.background(Color.White)
                        ) {
                            AppNavigation(
                                loginViewModel,
                                registerViewModel,
                                homeViewModel,
                                categoryViewModel,
                                itemDetailViewModel,
                                wishlistViewModel
                            )
                        }

                        ConnectivityPopup(
                            isConnected = isConnected,
                            modifier = Modifier.align(Alignment.TopCenter)
                        )
                    }
                }
            }
        }
        loadAllData()

    }
    private fun loadAllData() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // First check if we have cache data available
                val hasCachedData = homeViewModel.loadCachedDataSync() &&
                        categoryViewModel.loadCachedDataSync()

                if (hasCachedData) {
                    // We have cache data, show UI immediately
                    _isInitialLoading.value = false

                    // Then check for updates in background
                    launch {
                        homeViewModel.checkForUpdates()
                        categoryViewModel.checkForUpdates()
                    }
                } else {
                    // No cache data, need to wait for network load
                    homeViewModel.loadData()

                    // Wait until homeViewModel has data
                    while(homeViewModel.categories.value.isEmpty() &&
                        homeViewModel.featuredProducts.value.isEmpty() &&
                        homeViewModel.collections.value.isEmpty()) {
                        delay(100)
                        // Add timeout if needed
                    }

                    _isInitialLoading.value = false
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error loading initial data", e)
                // Show UI anyway after error
                _isInitialLoading.value = false
            }
        }
    }
}



@Composable
fun AppNavigation(
    loginViewModel: LoginViewModel,
    registerViewModel: RegisterViewModel,
    homeViewModel: HomeViewModel,
    categoryViewModel: CategoriesViewModel,
    itemDetailViewModel: ItemDetailViewModel,
    wishlistViewModel: WishlistViewModel
) {
    val navController = rememberNavController()

    // Check if user is already logged in
    val startDestination = if (FirebaseAuth.getInstance().currentUser != null) {
        "home"
    } else {
        "login"
    }

    NavHost(navController = navController, startDestination = startDestination) {
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
                navController = navController,
                onCategoryClick = { categoryId ->
                    navController.navigate("category/$categoryId")
                },
                onProductClick = { productId ->
                    navController.navigate("itemDetail/$productId")
                },
                onCollectionClick = { collectionId ->
                    navController.navigate("collection/$collectionId")
                }
            )
        }

        // Categories Screen
        composable(
            route = "category/{categoryId}",
            arguments = listOf(
                navArgument("categoryId") {
                    type = NavType.StringType
                    nullable = false
                }
            )
        ) { backStackEntry ->
            backStackEntry.arguments?.getString("categoryId") ?: ""
            CategoryScreenView(
                viewModel = categoryViewModel,
                navController = navController
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
            val productId = backStackEntry.arguments?.getString("productId") ?: ""
            JewelryProductScreen(
                productId = productId,
                viewModel = itemDetailViewModel,
                navController = navController,
                onBackClick = {
                    navController.popBackStack()
                },
                onShareClick = {},
                onAddToWishlistClick = {},
                onProductClick = { selectedProductId ->
                    navController.navigate("itemDetail/$selectedProductId") {
                        popUpTo("itemDetail/$productId") {
                            inclusive = true
                        }
                    }
                }
            )
        }

        // Collection Screen
        composable(
            route = "collection/{collectionId}",
            arguments = listOf(
                navArgument("collectionId") {
                    type = NavType.StringType
                    nullable = false
                }
            )
        ) { backStackEntry ->
            backStackEntry.arguments?.getString("collectionId") ?: ""
            CategoryScreenView(
                viewModel = categoryViewModel,
                navController = navController
            )
        }

        // Wishlist Screen
        composable("wishlist") {
            WishlistScreen(
                viewModel = wishlistViewModel,
                navController = navController
            )
        }

        // Profile Screen placeholder
        composable("profile") {
            LaunchedEffect(Unit) {
                navController.navigate("home")
            }
        }
    }
}





/**
 * A composable that shows a popup message when network connectivity changes
 */
@Composable
fun ConnectivityPopup(
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    var showPopup by remember { mutableStateOf(false) }
    var lastConnectionState by remember { mutableStateOf(isConnected) }

    // Only show popup when connection state changes, not on initial composition
    LaunchedEffect(isConnected) {
        if (lastConnectionState != isConnected) {
            showPopup = true
            delay(3000) // Show popup for 3 seconds
            showPopup = false
        }
        lastConnectionState = isConnected
    }

    AnimatedVisibility(
        visible = showPopup,
        enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
        exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isConnected) Color(0xFF4CAF50) else Color(0xFFE53935)
                )
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isConnected) {
                    "Back online! Syncing latest data..."
                } else {
                    "No internet connection. Using cached data."
                },
                color = Color.White,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.crown),
                contentDescription = "Logo",
                modifier = Modifier.size(80.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            CircularProgressIndicator(
                color = Color(0xFFB78628) // Gold color
            )
        }
    }
}