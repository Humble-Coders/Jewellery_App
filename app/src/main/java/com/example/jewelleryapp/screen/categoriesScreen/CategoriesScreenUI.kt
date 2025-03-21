package com.example.jewelleryapp.screen.categoriesScreen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jewelleryapp.R
import com.example.jewelleryapp.screen.homeScreen.BottomNavigationBar
import com.example.jewelleryapp.screen.homeScreen.TopAppbar

@Composable
fun CategoryScreenView() {
    Scaffold(
        topBar = {
            Box(modifier = Modifier.statusBarsPadding()) {
                TopAppbar("Categories")
            }
        },
        bottomBar = { BottomNavigationBar() }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 8.dp) // Reduced padding
            ) {
                CategoryRowView(
                    listOf(
                        CategoryData1(R.drawable.diamondring_homescreen, "Rings", "150+ Items"),
                        CategoryData1(R.drawable.diamondring_homescreen, "Necklaces", "120+ Items")
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))

                CategoryRowView(
                    listOf(
                        CategoryData1(R.drawable.diamondring_homescreen, "Earrings", "90+ Items"),
                        CategoryData1(R.drawable.diamondring_homescreen, "Bracelets", "80+ Items")
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))

                CategoryRowView(
                    listOf(
                        CategoryData1(R.drawable.diamondring_homescreen, "Pendants", "60+ Items"),
                        CategoryData1(R.drawable.diamondring_homescreen, "Wedding", "200+ Items")
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Featured Collections",
                    style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black),
                    modifier = Modifier.padding(start = 8.dp, bottom = 12.dp) // Adjusted padding
                )

                FeaturedCollectionView(R.drawable.diamondring_homescreen, "Royal Collection") {}
                Spacer(modifier = Modifier.height(12.dp))

                FeaturedCollectionView(R.drawable.diamondring_homescreen, "Bridal Sets") {}
                Spacer(modifier = Modifier.height(12.dp))

                FeaturedCollectionView(R.drawable.diamondring_homescreen, "Limited Edition") {}

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun CategoryRowView(categories: List<CategoryData1>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp), // Reduced padding
        horizontalArrangement = Arrangement.spacedBy(6.dp) // Reduced spacing
    ) {
        categories.forEach { category ->
            CategoryCardView(category.image, category.title, category.itemCount) {}
        }
    }
}

@Composable
fun CategoryCardView(image: Int, title: String, itemCount: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .size(width = 185.dp, height = 130.dp) // Increased width
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box {
            Image(
                painter = painterResource(id = image),
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
            Column(
                modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)
            ) {
                Text(
                    text = title,
                    style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                )
                Text(
                    text = itemCount,
                    style = TextStyle(fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                )
            }
        }
    }
}

@Composable
fun FeaturedCollectionView(image: Int, title: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp) // Reduced padding
            .height(80.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box {
            Image(
                painter = painterResource(id = image),
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f))
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )

                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.2f))
                        .padding(4.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_chevron_right),
                        contentDescription = "View More",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}


data class CategoryData1(val image: Int, val title: String, val itemCount: String)

