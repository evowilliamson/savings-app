package com.khaohom.savings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.pager.*
import com.khaohom.savings.ui.screens.*
import com.khaohom.savings.ui.theme.KhaohomSavingsTheme
import com.khaohom.savings.ui.viewmodel.SavingsViewModel
import com.khaohom.savings.ui.viewmodel.UiState
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KhaohomSavingsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalPagerApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: SavingsViewModel = viewModel()) {
    val pagerState = rememberPagerState()
    val coroutineScope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()
    val isThb by viewModel.isThb.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Khaohom's Savings") },
                actions = {
                    // Currency toggle
                    IconButton(
                        onClick = { viewModel.toggleCurrency() }
                    ) {
                        Text(
                            text = if (isThb) "THB" else "USD",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    
                    // Refresh button
                    IconButton(
                        onClick = { viewModel.loadData() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = pagerState.currentPage == 0,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(0)
                        }
                    },
                    icon = { Icon(Icons.Default.AccountBalance, "Assets") },
                    label = { Text("Assets") }
                )
                
                NavigationBarItem(
                    selected = pagerState.currentPage == 1,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(1)
                        }
                    },
                    icon = { Icon(Icons.Default.ShowChart, "Chart") },
                    label = { Text("Chart") }
                )
                
                NavigationBarItem(
                    selected = pagerState.currentPage == 2,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(2)
                        }
                    },
                    icon = { Icon(Icons.Default.List, "History") },
                    label = { Text("History") }
                )
                
                NavigationBarItem(
                    selected = pagerState.currentPage == 3,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(3)
                        }
                    },
                    icon = { Icon(Icons.Default.TrendingUp, "Prognosis") },
                    label = { Text("Future") }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (uiState) {
                is UiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator()
                            Text("Loading your savings data...")
                        }
                    }
                }
                
                is UiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                text = "Error loading data",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = (uiState as UiState.Error).message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(onClick = { viewModel.loadData() }) {
                                Text("Retry")
                            }
                        }
                    }
                }
                
                is UiState.Success -> {
                    HorizontalPager(
                        count = 4,
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        when (page) {
                            0 -> AssetsScreen(viewModel = viewModel)
                            1 -> ChartScreen(viewModel = viewModel)
                            2 -> HistoryScreen(viewModel = viewModel)
                            3 -> PrognosisScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}
