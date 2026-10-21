package id.nkz.nokontzzzmanager.ui.screens
import id.nkz.nokontzzzmanager.ui.dialog.*
import id.nkz.nokontzzzmanager.ui.components.*

import kotlinx.coroutines.delay
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import id.nkz.nokontzzzmanager.ui.components.IndeterminateExpressiveLoadingIndicator
import id.nkz.nokontzzzmanager.ui.components.GpuControlCard
import id.nkz.nokontzzzmanager.ui.components.SwappinessCard
import id.nkz.nokontzzzmanager.ui.components.ThermalCard
import id.nkz.nokontzzzmanager.viewmodel.TuningViewModel
import kotlinx.coroutines.launch


import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.nkz.nokontzzzmanager.R

// Daftar fitur dengan terjemahannya
val tuningFeatures = listOf(
    R.string.tuning_feature_performance_mode_title to R.string.tuning_feature_performance_mode_desc,
    R.string.tuning_feature_cpu_governor_title to R.string.tuning_feature_cpu_governor_desc,
    R.string.tuning_feature_cpu_freq_title to R.string.tuning_feature_cpu_freq_desc,
    R.string.tuning_feature_core_control_title to R.string.tuning_feature_core_control_desc,
    R.string.tuning_feature_gpu_control_title to R.string.tuning_feature_gpu_control_desc,
    R.string.tuning_feature_thermal_title to R.string.tuning_feature_thermal_desc,
    R.string.tuning_feature_zram_title to R.string.tuning_feature_zram_desc,
    R.string.tuning_feature_swappiness_title to R.string.tuning_feature_swappiness_desc,
    R.string.tuning_feature_dirty_ratio_title to R.string.tuning_feature_dirty_ratio_desc,
    R.string.tuning_feature_dirty_bg_ratio_title to R.string.tuning_feature_dirty_bg_ratio_desc,
    R.string.tuning_feature_writeback_title to R.string.tuning_feature_writeback_desc,
    R.string.tuning_feature_dirty_expire_title to R.string.tuning_feature_dirty_expire_desc,
    R.string.tuning_feature_min_free_title to R.string.tuning_feature_min_free_desc,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TuningScreen(
    navController: NavController? = null,
    viewModel: TuningViewModel = hiltViewModel(LocalContext.current as ComponentActivity)
) {
    var showInfoDialog by remember { mutableStateOf(false) }
    val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val isLoading by viewModel.isLoading.collectAsState()
    
    val applyPerformanceOnBoot by viewModel.applyPerformanceModeOnBoot.collectAsStateWithLifecycle()
    val applyCpuOnBoot by viewModel.applyCpuOnBoot.collectAsStateWithLifecycle()
    val applyGpuOnBoot by viewModel.applyGpuOnBoot.collectAsStateWithLifecycle()
    val applyThermalOnBoot by viewModel.applyThermalOnBoot.collectAsStateWithLifecycle()
    val applyRamOnBoot by viewModel.applyRamOnBoot.collectAsStateWithLifecycle()

    // Data is now loaded lazily. The LaunchedEffect triggers data loading
    // after the UI is composed, making the screen appear instantly.
    LaunchedEffect(Unit) {
        delay(150) // Allow navigation animation to finish
        viewModel.loadAllData()
    }

    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 100.dp),
            contentAlignment = Alignment.Center
        ) {
            IndeterminateExpressiveLoadingIndicator()
        }
    } else {
        var showBootSettingsDialog by remember { mutableStateOf(false) }

        val expandedCards by viewModel.expandedCards.collectAsState()

        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 0.dp,
                bottom = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Hero Header
            item {
                HeroHeader(
                    onClick = { showInfoDialog = true }
                )
            }

                item {
                    PerformanceModeCard(viewModel = viewModel)
                }

                item {
                    CpuGovernorCard(
                        vm = viewModel,
                        isExpanded = expandedCards["cpu"] ?: false,
                        onExpandChange = { viewModel.toggleCardExpansion("cpu") }
                    )
                }

                item {
                    GpuControlCard(
                        tuningViewModel = viewModel,
                        isExpanded = expandedCards["gpu"] ?: false,
                        onExpandChange = { viewModel.toggleCardExpansion("gpu") }
                    )
                }

                item {
                    ThermalCard(
                        viewModel = viewModel,
                        isExpanded = expandedCards["thermal"] ?: false,
                        onExpandChange = { viewModel.toggleCardExpansion("thermal") }
                    )
                }

                item {
                    SwappinessCard(
                        vm = viewModel,
                        isExpanded = expandedCards["ram"] ?: false,
                        onExpandChange = { viewModel.toggleCardExpansion("ram") }
                    )
                }
                
                item {
                    BootSettingsCard(
                        onClick = { showBootSettingsDialog = true }
                    )
                }
            }

        if (showInfoDialog) {
            FeatureInfoDialog(
                onDismissRequest = { showInfoDialog = false },
                features = tuningFeatures
            )
        }

        if (showBootSettingsDialog) {
            BootSettingsDialog(
                onDismiss = { showBootSettingsDialog = false },
                viewModel = viewModel,
                applyPerformance = applyPerformanceOnBoot,
                applyCpu = applyCpuOnBoot,
                applyGpu = applyGpuOnBoot,
                applyThermal = applyThermalOnBoot,
                applyRam = applyRamOnBoot
            )
        }
    }
}

