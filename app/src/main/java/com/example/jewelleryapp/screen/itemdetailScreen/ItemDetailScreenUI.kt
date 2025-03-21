package com.example.jewelleryapp.screen.itemdetailScreen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jewelleryapp.R
import kotlinx.coroutines.launch

// Defined colors as constants
private val GoldColor = Color(0xFFC4A661)
private val GoldColorTransparent = GoldColor.copy(alpha = 0.3f)
private val ButtonColor = Color(0xFFC4A661)
private val BackgroundColor = Color(0xFFAA8F8F)
private val TextGrayColor = Color.Gray
private val TextDescriptionColor = Color(0xFF4B5563)
private val TextPriceColor = Color(0xFF333333)

// Data models
data class SimilarProductData(
    val imageId: Int,
    val title: String,
    val price: String
)

data class ProductSpec(
    val iconId: Int,
    val title: String,
    val value: String
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun JewelryProductScreen(
    onBackClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    onAddToWishlistClick: () -> Unit = {}
) {
    var isWishlisted by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    // Sample data
    val imageList = listOf(
        R.drawable.diamondring_homescreen,
        R.drawable.diamondring_homescreen,
        R.drawable.diamondring_homescreen
    )

    val specs = listOf(
        ProductSpec(R.drawable.material_icon, "Material", "18K White Gold"),
        ProductSpec(R.drawable.stone, "Stone", "1.5ct Diamond"),
        ProductSpec(R.drawable.clarity, "Clarity", "VS1"),
        ProductSpec(R.drawable.cut, "Cut", "Excellent")
    )

    val similarProducts = listOf(
        SimilarProductData(R.drawable.diamondring_homescreen, "Diamond Solitaire", "$3,999"),
        SimilarProductData(R.drawable.diamondring_homescreen, "Pearl Ring", "$1,999"),
        SimilarProductData(R.drawable.diamondring_homescreen, "Sapphire Ring", "$2,499")
    )
    val pagerState = rememberPagerState(pageCount = { imageList.size })

    Scaffold(
        topBar = {
            ProductTopAppBar(
                title = "Luxury Jewelry",
                isWishlisted = isWishlisted,
                onBackClick = onBackClick,
                onWishlistClick = { isWishlisted = !isWishlisted },
                onShareClick = onShareClick
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // Image carousel
            ImageCarousel(
                images = imageList,
                pagerState = pagerState,
                onDotClick = { index ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                }
            )
            // Product details
            Column(modifier = Modifier.padding(16.dp)) {
                // Product title and collection
                ProductHeader(
                    title = "Diamond Eternity Ring",
                    collection = "Timeless Collection",
                    currentPrice = "$4,999",
                    originalPrice = "$5,999"
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Product specifications
                ProductSpecifications(specs = specs)
                Spacer(modifier = Modifier.height(16.dp))
                // Description
                ProductDescription(
                    description = "This exquisite diamond eternity ring features perfectly matched round brilliant diamonds set in 18K white gold. Each stone is carefully selected for its exceptional cut, clarity, and brilliance, creating a seamless circle of light around your finger."
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Similar products
                SimilarProducts(products = similarProducts)

                Spacer(modifier = Modifier.height(24.dp))

                // Add to wishlist button
                WishlistButton(onClick = onAddToWishlistClick)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductTopAppBar(
    title: String,
    isWishlisted: Boolean,
    onBackClick: () -> Unit,
    onWishlistClick: () -> Unit,
    onShareClick: () -> Unit
) {
    TopAppBar(
        title = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            IconButton(onClick = onWishlistClick) {
                Icon(
                    if (isWishlisted) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Wishlist"
                )
            }
            IconButton(onClick = onShareClick) {
                Icon(Icons.Default.Share, contentDescription = "Share")
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ImageCarousel(
    images: List<Int>,
    pagerState: androidx.compose.foundation.pager.PagerState,
    onDotClick: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .background(BackgroundColor)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            Image(
                painter = painterResource(id = images[page]),
                contentDescription = "Product Image ${page + 1}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Dots indicator
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(images.size) { index ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (pagerState.currentPage == index) GoldColor else GoldColorTransparent
                        )
                        .clickable { onDotClick(index) }
                )
            }
        }
    }
}

@Composable
private fun ProductHeader(
    title: String,
    collection: String,
    currentPrice: String,
    originalPrice: String
) {
    Text(
        text = title,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold
    )

    Text(
        text = collection,
        fontSize = 14.sp,
        color = TextGrayColor
    )

    Spacer(modifier = Modifier.height(8.dp))

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = currentPrice,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = " $originalPrice",
            fontSize = 16.sp,
            color = TextGrayColor,
            textDecoration = TextDecoration.LineThrough,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun ProductSpecifications(specs: List<ProductSpec>) {
    Column {
        for (i in specs.indices step 2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // First spec in the row takes exactly half the space
                Box(modifier = Modifier.weight(1f)) {
                    ProductSpecItem(spec = specs[i])
                }

                // Second spec in the row also takes exactly half the space
                Box(modifier = Modifier.weight(1f)) {
                    // Only add the second item if it exists
                    if (i + 1 < specs.size) {
                        ProductSpecItem(spec = specs[i + 1])
                    }
                }
            }
            if (i + 2 < specs.size) {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ProductSpecItem(spec: ProductSpec) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(id = spec.iconId),
            contentDescription = null,
            tint = GoldColor,
            modifier = Modifier.size(16.dp)
        )
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(
                text = spec.title,
                fontSize = 12.sp,
                color = TextGrayColor
            )
            Text(
                text = spec.value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
@Composable
private fun ProductDescription(description: String) {
    Text(
        text = "Description",
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = description,
        fontSize = 17.sp,
        lineHeight = 20.sp,
        color = TextDescriptionColor
    )
}

@Composable
private fun SimilarProducts(products: List<SimilarProductData>) {
    Text(
        text = "You may also like",
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold
    )

    Spacer(modifier = Modifier.height(8.dp))

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(products) { product ->
            SimilarProductItem(product = product)
        }
    }
}

@Composable
private fun SimilarProductItem(product: SimilarProductData) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clip(RoundedCornerShape(8.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(Color.LightGray)
        ) {
            Image(
                painter = painterResource(id = product.imageId),
                contentDescription = product.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Text(
            text = product.title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            modifier = Modifier.padding(top = 4.dp)
        )

        Text(
            text = product.price,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextPriceColor
        )
    }
}

@Composable
private fun WishlistButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = ButtonColor),
        border = BorderStroke(1.dp, Color.Black)
    ) {
        Text(
            text = "Add to Wishlist",
            color = Color.Black,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}