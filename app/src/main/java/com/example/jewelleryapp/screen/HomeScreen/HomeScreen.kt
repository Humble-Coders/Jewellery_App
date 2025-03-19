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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jewelleryapp.R
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import kotlinx.coroutines.launch

// Data classes for future backend integration
data class Category(
    val id: String,
    val name: String,
    val imageResId: Int
)

data class Product(
    val id: String,
    val name: String,
    val price: Double,
    val currency: String = "USD",
    val imageResId: Int,
    val isFavorite: Boolean = false
)

data class Collection(
    val id: String,
    val name: String,
    val imageResId: Int
)

data class CarouselItem(
    val id: String,
    val imageResId: Int,
    val title: String,
    val subtitle: String,
    val buttonText: String
)

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
fun HomeScreen() {
    // Sample data - would be replaced with API data
    val carouselItems = listOf(
        CarouselItem(
            id = "1",
            imageResId = R.drawable.swipeable_img1,
            title = "Timeless Elegance",
            subtitle = "NEW COLLECTION",
            buttonText = "Discover"
        ),
        CarouselItem(id = "2",
            imageResId = R.drawable.swipeable_img1,
            title = "Timeless Elegance",
            subtitle = "NEW COLLECTION",
            buttonText = "Discover"
        ),
        CarouselItem(
            id = "3",
            imageResId = R.drawable.swipeable_img1,
            title = "Timeless Elegance",
            subtitle = "NEW COLLECTION",
            buttonText = "Discover"
        )
    )

    val categories = listOf(
        Category("1", "Necklaces", R.drawable.necklace_homescreen),
        Category("2", "Earrings", R.drawable.earrings_homescreen),
        Category("3", "Rings", R.drawable.diamondring_homescreen),
        Category("4", "Bracelets", R.drawable.goldbracelet_homescreen)
    )

    val featuredProducts = listOf(
        Product("1", "Diamond Eternity Ring", 4200.0, "USD", R.drawable.diamondring_homescreen, false),
        Product("2", "Pearl Necklace", 1800.0, "USD", R.drawable.necklace_homescreen, false),
        Product("3", "Sapphire Earrings", 3200.0, "USD", R.drawable.earrings_homescreen, false),
        Product("4", "Gold Bracelet", 1500.0, "USD", R.drawable.goldbracelet_homescreen, false)
    )

    val collections = listOf(
        Collection("1", "Royal Collection", R.drawable.collectioin_img1),
        Collection("2", "Bridal Collection", R.drawable.collections_pic3)
    )

    val recentlyViewed = listOf(
        Product("5", "Diamond Ring", 3800.0, "USD", R.drawable.diamondring_homescreen, false),
        Product("6", "Gold Bracelet", 950.0, "USD", R.drawable.recentlyviewed_pic2, false),
        Product("7", "Pearl Earrings", 1200.0, "USD", R.drawable.recentlyviewed_pic3, false)
    )

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = { TopAppbar("Gagan Jewellers") },
        bottomBar = { BottomNavigationBar() }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .verticalScroll(scrollState)
        ) {
            ImageCarousel(carouselItems)
            CategoryRow(categories)
            FeaturedCollection(featuredProducts)
            CollectionsSection(collections)
            RecentlyViewedSection(recentlyViewed)
        }
    }
}

// Top app bar with search and cart
@Composable
fun TopAppbar(
    title: String,
    //   onMenuClick: () -> Unit = {},
    //   onSearchClick: () -> Unit = {},
//    onFavoriteClick: () -> Unit = {}
) {
    val amberColor = Color(0xFFB78628) // Approximate amber/gold color

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .border(0.5.dp, Color.LightGray)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = {}) {
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
            IconButton(onClick = { } ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = amberColor
                )
            }

            IconButton(onClick = { } ) {
                Icon(
                    imageVector = Icons.Default.FavoriteBorder,
                    contentDescription = "Favorites",
                    tint = amberColor
                )
            }
        }
    }
}

@Composable
fun ImageCarousel(items: List<CarouselItem>) {
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

                // Use Image with painterResource
                Image(
                    painter = painterResource(id = item.imageResId),
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
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
fun CategoryRow(categories: List<Category>) {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            items(categories) { category ->
                CategoryItem(category)
            }
        }
    }
}

// Individual category item
@Composable
fun CategoryItem(category: Category) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = category.imageResId),
            contentDescription = category.name,
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = category.name,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

// Featured collection section
@Composable
fun FeaturedCollection(products: List<Product>) {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        SectionTitle("Featured Collection")

        Spacer(modifier = Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.height(350.dp)
        ) {
            items(products) { product ->
                ProductItem(product)
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
fun ProductItem(product: Product) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Handle click */ },
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
                Image(
                    painter = painterResource(id = product.imageResId),
                    contentDescription = product.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
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
// Collections section
@Composable
fun CollectionsSection(collections: List<Collection>) {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        SectionTitle("Collections")

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(collections) { collection ->
                CollectionItem(collection)
            }
        }
    }
}


// Individual collection item
@Composable
fun CollectionItem(collection: Collection) {
    Box(
        modifier = Modifier
            .width(200.dp)
            .height(120.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { /* Handle click */ }
    ) {
        Image(
            painter = painterResource(id = collection.imageResId),
            contentDescription = collection.name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Dark overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
        )

        // Collection name and View All text
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Text(
                text = collection.name,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "View All",
                color = Color(0xFFB4A06C),
                fontSize = 12.sp
            )
        }
    }
}


// Recently viewed section
// Update the Recently Viewed section to match the design
@Composable
fun RecentlyViewedSection(products: List<Product>) {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        SectionTitle("Recently Viewed")

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp) // Slightly reduced spacing
        ) {
            items(products) { product ->
                RecentlyViewedItem(product)
            }
        }
    }
}

// Individual recently viewed item with increased height
@Composable
fun RecentlyViewedItem(product: Product) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black)
            .clickable { /* Handle click */ }
    ) {
        // Image
        Image(
            painter = painterResource(id = product.imageResId),
            contentDescription = product.name,
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp),
            contentScale = ContentScale.Crop
        )

        // Product info on black background
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = product.name,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = formatPrice(product.price, product.currency),
                fontSize = 14.sp,
                color = Color(0xFFB4A06C), // Gold/amber color for price
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// Bottom navigation bar
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