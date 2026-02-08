package com.vicky.recsdk.demo.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vicky.recsdk.demo.data.model.DummyProduct
import com.vicky.recsdk.demo.ui.detail.ProductDetailScreen
import com.vicky.recsdk.demo.ui.detail.ProductDetailViewModel
import com.vicky.recsdk.demo.ui.home.HomeScreen
import com.vicky.recsdk.demo.ui.theme.RecoSDKDemoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RecoSDKDemoTheme {
                RecoSDKApp()
            }
        }
    }
}

@Composable
fun RecoSDKApp() {
    val navController = rememberNavController()
    var selectedProduct by remember { mutableStateOf<DummyProduct?>(null) }

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onProductClick = { product ->
                    selectedProduct = product
                    navController.navigate("detail")
                }
            )
        }
        composable("detail") {
            val detailViewModel: ProductDetailViewModel = viewModel()
            selectedProduct?.let { product ->
                LaunchedEffect(product.id) {
                    detailViewModel.setProduct(product)
                }
                ProductDetailScreen(
                    viewModel = detailViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
