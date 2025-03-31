package com.example.jewelleryapp.screen.homeScreen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Headset
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.jewelleryapp.R
import com.example.jewelleryapp.model.Category
import com.example.jewelleryapp.model.CarouselItem as CarouselItemModel
import com.example.jewelleryapp.model.Category as CategoryModel
import com.example.jewelleryapp.model.Collection as CollectionModel
import com.example.jewelleryapp.model.Product as ProductModel
import kotlinx.coroutines.launch

// Function to format price with currency
fun formatPrice(price: Double, currency: String): String {
    return when (currency) {
        "USD" -> "$${price.toInt()}"
        "EUR" -> "€${price.toInt()}"
        else -> "${price.toInt()} $currency"
    }
}

// Main screen composable
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onCategoryClick: (String) -> Unit = {},
    onProductClick: (String) -> Unit = {},
    onCollectionClick: (String) -> Unit = {}
) {
    // Collect state flows
    val categories by viewModel.categories.collectAsState()
    val featuredProducts by viewModel.featuredProducts.collectAsState()
    val collections by viewModel.collections.collectAsState()
    val carouselItems by viewModel.carouselItems.collectAsState()
    val recentlyViewed by viewModel.recentlyViewedProducts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    val scrollState = rememberScrollState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.fillMaxWidth(0.75f)
            ) {
                DrawerContent { scope.launch { drawerState.close() } }
            }
        }
    ) {
        Scaffold(
            topBar = { TopAppbar("Gagan Jewellers", onMenuClick = { scope.launch { drawerState.open() } })},
            bottomBar = { BottomNavigationBar() }
        ) { paddingValues ->
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFFB78628))
                }
            } else if (error != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Something went wrong",
                            color = Color.Red,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.refreshData() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFB78628)
                            )
                        ) {
                            Text("Try Again")
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .padding(paddingValues)
                        .verticalScroll(scrollState)
                ) {
                    ImageCarousel(carouselItems)
                    CategoryRow(categories, onCategoryClick)
                    FeaturedProductsSection(featuredProducts, onProductClick)
                    ThemedCollectionsSection(collections, onCollectionClick)
                }
            }
        }
    }
}

// Top app bar with search and cart
@Composable
fun TopAppbar(
    title: String,
    onMenuClick: () -> Unit = {}
) {
    val amberColor = Color(0xFFB78628) // Approximate amber/gold color

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .border(0.5.dp, Color.LightGray)
            .windowInsetsPadding(WindowInsets.statusBars) // Ensures it appears below the notch
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = amberColor
                )
            }

            Text(
                text = title,
                color = amberColor,
                fontWeight = FontWeight.Medium
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { }) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = amberColor
                    )
                }

                IconButton(onClick = { }) {
                    Icon(
                        imageVector = Icons.Default.FavoriteBorder,
                        contentDescription = "Favorites",
                        tint = amberColor
                    )
                }
            }
        }
    }
}

@Composable
fun ImageCarousel(items: List<CarouselItemModel>) {
    if (items.isEmpty()) return

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        val pagerState = rememberPagerState(pageCount = { items.size })
        val scope = rememberCoroutineScope()

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val item = items[page]
            Box(modifier = Modifier.fillMaxSize()) {
                // Use AsyncImage with Coil to load from URL
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(item.imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(id = R.drawable.swipeable_img1) // Use placeholder from resources
                )

                // Overlay with text
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                )

                // Text overlay
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Text(
                        text = item.subtitle,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Light
                    )

                    // Increased spacing between subtitle and title
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = item.title,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Increased spacing between title and button
                    Spacer(modifier = Modifier.height(16.dp))

                    // Updated to make the button round
                    Button(
                        onClick = { /* Handle click */ },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        modifier = Modifier
                            .height(40.dp)
                            .border(1.dp, Color.White, CircleShape)
                    ) {
                        Text(
                            text = item.buttonText,
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Dots indicator
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(items.size) { index ->
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (index == pagerState.currentPage) Color.White else Color.White.copy(alpha = 0.5f))
                        .padding(4.dp)
                        .clickable {
                            scope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        }
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
        }
    }
}

// Category row with circular images
@Composable
fun CategoryRow(categories: List<Category>, onCategoryClick: (String) -> Unit) {
    if (categories.isEmpty()) return

    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            items(categories) { category ->
                CategoryItem(category, onCategoryClick)
            }
        }
    }
}

// Individual category item
@Composable
fun CategoryItem(category: CategoryModel, onCategoryClick: (String) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onCategoryClick(category.id) }
    ) {
        // Use AsyncImage with Coil to load from URL
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(category.imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = category.name,
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
            placeholder = painterResource(id = R.drawable.necklace_homescreen) // Placeholder from resources
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = category.name,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

// Featured products section
@Composable
fun FeaturedProductsSection(products: List<ProductModel>, onProductClick: (String) -> Unit) {
    if (products.isEmpty()) return

    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        SectionTitle("Featured Products")

        Spacer(modifier = Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.height(350.dp)
        ) {
            items(products) { product ->
                ProductItem(product, onProductClick)
            }
        }
    }
}

// Section title component
@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold
    )
}

// Individual product item
@Composable
fun ProductItem(product: ProductModel, onProductClick: (String) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onProductClick(product.id) },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                // Use AsyncImage with Coil to load from URL
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(product.imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = product.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(id = R.drawable.diamondring_homescreen) // Placeholder from resources
                )

                // Favorite icon
                IconButton(
                    onClick = { /* Handle favorite */ },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(32.dp)
                ) {
                    Icon(
                        imageVector = if (product.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (product.isFavorite) Color.Red else Color.White
                    )
                }
            }

            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = product.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = formatPrice(product.price, product.currency),
                    fontSize = 14.sp,
                    color = Color(0xFFB78628) // Amber/gold color for price
                )
            }
        }
    }
}

// Themed Collections section
@Composable
fun ThemedCollectionsSection(collections: List<CollectionModel>, onCollectionClick: (String) -> Unit) {
    if (collections.isEmpty()) return

    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        SectionTitle("Themed Collections")

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(collections) { collection ->
                CollectionItem(collection, onCollectionClick)
            }
        }
    }
}

// Individual collection item
@Composable
fun CollectionItem(collection: CollectionModel, onCollectionClick: (String) -> Unit) {
    Box(
        modifier = Modifier
            .width(280.dp) // Increased width to fit description
            .height(140.dp) // Increased height to fit description
            .clip(RoundedCornerShape(8.dp))
            .clickable { onCollectionClick(collection.id) }
    ) {
        // Use AsyncImage with Coil to load from URL
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(collection.imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = collection.name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            placeholder = painterResource(id = R.drawable.collectioin_img1) // Placeholder from resources
        )

        // Dark overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)) // Slightly darker overlay
        )

        // Collection name, description and View All text
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
                .fillMaxWidth(0.85f) // Limit width of text
        ) {
            Text(
                text = collection.name,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = collection.description,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "View Collection",
                color = Color(0xFFB4A06C),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// Removed Recently Viewed section as requested

@Composable
fun BottomNavigationBar() {
    val amberColor = Color(0xFFB4A06C) // Amber/gold color for all icons

    NavigationBar(
        containerColor = Color.White
    ) {
        val items = listOf(
            Triple(Icons.Default.Home, "Home", true),
            Triple(Icons.Default.GridView, "Categories", false),
            Triple(Icons.Default.FavoriteBorder, "Favorites", false),
            Triple(Icons.Default.Person, "Profile", false)
        )

        items.forEach { (icon, label, selected) ->
            NavigationBarItem(
                icon = {
                    Icon(
                        icon,
                        contentDescription = label,
                        tint = amberColor // All icons use the amber color
                    )
                },
                label = {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        color = if (selected) amberColor else Color.Gray
                    )
                },
                selected = selected,
                onClick = { /* Handle navigation */ }
            )
        }
    }
}

@Composable
fun DrawerContent(onItemClick: () -> Unit) {

    val amberColor = Color(0xFFB78628)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(amberColor, CircleShape) // Gold Border
                    .padding(3.dp) // Padding for border effect
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_background), // Replace with your image
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "Hi ___!",
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                modifier = Modifier.padding(bottom = 8.dp),
                color = Color.Black
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        HorizontalDivider(thickness = 1.dp, color = Color.LightGray) // 👈 Line under "Hi Shreya!"

        Spacer(modifier = Modifier.height(16.dp))

        DrawerItem(icon = Icons.Outlined.Person, text = "My Profile", onClick = onItemClick)
        DrawerItem(icon = Icons.Outlined.History, text = "Order History", onClick = onItemClick)

        SectionHeader("Shop By")
        DrawerItem("All Jewellery", onItemClick)
        DrawerItem("Metal", onItemClick)
        DrawerItem("Collections", onItemClick)

        SectionHeader("Shop For")
        DrawerItem("Men", onItemClick)
        DrawerItem("Kids", onItemClick)

        SectionHeader("More")
        DrawerItem(icon = Icons.Outlined.AttachMoney, text = "Gold Rate", onClick = onItemClick)
        DrawerItem(icon = Icons.Outlined.Headset, text = "Get In Touch", onClick = onItemClick)
        DrawerItem(icon = Icons.Outlined.LocationOn, text = "Store Locator", onClick = onItemClick)
        DrawerItem(
            icon = Icons.AutoMirrored.Outlined.ExitToApp,
            text = "Logout",
            onClick = onItemClick
        )
    }
}

@Composable
fun DrawerItem(text: String, onClick: () -> Unit, icon: ImageVector? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = Color.DarkGray,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp)) // Space between icon & text
        }
        Text(
            text = text,
            fontSize = 18.sp,
            modifier = Modifier.weight(1f), // Pushes text to the left
            color = Color.Black
        )
        Icon(
            imageVector = Icons.Outlined.ChevronRight, // Right arrow icon
            contentDescription = "Arrow",
            tint = Color.Gray
        )
    }
}

@Composable
fun SectionHeader(text: String) {

    val amberColor = Color(0xFFB78628)
    Text(
        text = text,
        color = amberColor,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    )
}