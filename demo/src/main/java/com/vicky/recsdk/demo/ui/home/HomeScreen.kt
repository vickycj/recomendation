package com.vicky.recsdk.demo.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.vicky.recsdk.demo.data.model.DummyProduct
import com.vicky.recsdk.demo.ui.theme.ProfileBg
import com.vicky.recsdk.demo.ui.theme.RecommendedBg
import com.vicky.recsdk.model.RecoResult
import com.vicky.recsdk.model.ScoredItem
import com.vicky.recsdk.model.UserProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onProductClick: (DummyProduct) -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("RecoSDK Demo") },
                actions = {
                    IconButton(onClick = { viewModel.onClearData() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear data")
                    }
                    IconButton(onClick = { viewModel.loadProducts() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = uiState.error ?: "Error",
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadProducts() }) {
                            Text("Retry")
                        }
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    // Recommendations section
                    if (uiState.recommendations.items.isNotEmpty()) {
                        item {
                            RecommendationsSection(
                                recommendations = uiState.recommendations,
                                onProductClick = { scoredItem ->
                                    val product = uiState.products.find {
                                        it.id.toString() == scoredItem.item.id
                                    }
                                    if (product != null) {
                                        viewModel.onProductClicked(product)
                                        onProductClick(product)
                                    }
                                }
                            )
                        }
                    }

                    // User profile section
                    if (uiState.userProfile.topCategories.isNotEmpty() ||
                        uiState.userProfile.topBrands.isNotEmpty() ||
                        uiState.userProfile.interestTags.isNotEmpty()
                    ) {
                        item {
                            UserProfileSection(profile = uiState.userProfile)
                        }
                    }

                    // Categories section
                    if (uiState.categories.isNotEmpty()) {
                        item {
                            CategoriesSection(
                                categories = uiState.categories,
                                selectedCategory = uiState.selectedCategory,
                                onCategorySelected = { viewModel.onCategorySelected(it) }
                            )
                        }
                    }

                    // Products header
                    item {
                        Text(
                            text = if (uiState.selectedCategory != null)
                                "${uiState.selectedCategory}" else "All Products",
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    // Products grid (using chunked for 2-column layout in LazyColumn)
                    val chunked = uiState.filteredProducts.chunked(2)
                    items(chunked) { row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            for (product in row) {
                                ProductCard(
                                    product = product,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        viewModel.onProductClicked(product)
                                        onProductClick(product)
                                    },
                                    onAddToCart = {
                                        viewModel.onProductAddedToCart(product)
                                    }
                                )
                            }
                            // Fill empty space if odd number
                            if (row.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecommendationsSection(
    recommendations: RecoResult,
    onProductClick: (ScoredItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(RecommendedBg)
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = "Recommended For You",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(recommendations.items) { scoredItem ->
                RecommendedCard(
                    scoredItem = scoredItem,
                    onClick = { onProductClick(scoredItem) }
                )
            }
        }
    }
}

@Composable
fun RecommendedCard(
    scoredItem: ScoredItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            AsyncImage(
                model = scoredItem.item.imageUrl,
                contentDescription = scoredItem.item.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = scoredItem.item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$${scoredItem.item.price}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                if (scoredItem.reasons.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = scoredItem.reasons.first(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFE65100),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "Score: ${"%.2f".format(scoredItem.score)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileSection(profile: UserProfile) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ProfileBg)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Your Profile",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (profile.topCategories.isNotEmpty()) {
                Text(
                    text = "Top Categories",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Gray
                )
                Text(
                    text = profile.topCategories.take(5)
                        .joinToString(", ") { "${it.category} (${"%.0f".format(it.score * 100)}%)" },
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            if (profile.topBrands.isNotEmpty()) {
                Text(
                    text = "Favorite Brands",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Gray
                )
                Text(
                    text = profile.topBrands.take(5)
                        .joinToString(", ") { "${it.brand} (${"%.0f".format(it.score * 100)}%)" },
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            if (profile.interestTags.isNotEmpty()) {
                Text(
                    text = "Interests",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Gray
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(profile.interestTags.take(8)) { tag ->
                        AssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    "${tag.tag} ${"%.0f".format(tag.score * 100)}%",
                                    fontSize = 11.sp
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesSection(
    categories: List<String>,
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = "Categories",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { onCategorySelected(null) },
                    label = { Text("All") }
                )
            }
            items(categories) { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = {
                        onCategorySelected(if (selectedCategory == category) null else category)
                    },
                    label = { Text(category) }
                )
            }
        }
    }
}

@Composable
fun ProductCard(
    product: DummyProduct,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onAddToCart: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            AsyncImage(
                model = product.thumbnail,
                contentDescription = product.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = product.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = product.brand ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$${product.price}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${"%.1f".format(product.rating)}",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedButton(
                    onClick = onAddToCart,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    Icon(
                        Icons.Default.ShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add to Cart", fontSize = 12.sp)
                }
            }
        }
    }
}
