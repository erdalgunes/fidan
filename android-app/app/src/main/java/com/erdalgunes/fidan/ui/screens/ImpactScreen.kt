package com.erdalgunes.fidan.ui.screens

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import com.erdalgunes.fidan.config.AppConfig
import com.erdalgunes.fidan.repository.ImpactRepository
import com.erdalgunes.fidan.ui.components.*
import com.erdalgunes.fidan.ui.theme.Dimensions
import com.erdalgunes.fidan.ui.viewmodel.ImpactViewModel
import com.erdalgunes.fidan.ui.viewmodel.ImpactViewModelFactory
import com.erdalgunes.fidan.ui.viewmodel.ImpactUiState
import com.erdalgunes.fidan.ui.viewmodel.ErrorType
import com.erdalgunes.fidan.data.ImpactData
import com.erdalgunes.fidan.utils.UrlUtils

/**
 * Impact screen extracted from MainActivity following KISS principle
 * Single responsibility: Display environmental impact and sponsorship information
 */
@Composable
fun ImpactScreen(
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { ImpactRepository() }
    val viewModel: ImpactViewModel = viewModel(
        factory = ImpactViewModelFactory(repository)
    )
    val uiState by viewModel.uiState.collectAsState()
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(Dimensions.PaddingLarge)
    ) {
        Text(
            text = "Real Environmental Impact",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = Dimensions.PaddingXXLarge),
            color = MaterialTheme.colorScheme.primary
        )
        
        when (uiState) {
            is ImpactUiState.Loading -> {
                ImpactLoadingState()
            }
            is ImpactUiState.Error -> {
                ImpactErrorState(
                    errorMessage = uiState.message,
                    errorType = uiState.errorType,
                    onRetry = { viewModel.refresh() }
                )
            }
            is ImpactUiState.Success -> {
                ImpactSuccessContent(
                    impactData = uiState.data,
                    githubSponsorsUrl = AppConfig.GITHUB_SPONSORS_URL,
                    transparencyReportUrl = AppConfig.TRANSPARENCY_REPORT_URL,
                    context = context
                )
            }
        }
    }
}

@Composable
private fun ImpactLoadingState() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(Dimensions.PaddingLarge))
        Text(
            text = "Loading impact data...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ImpactErrorState(
    errorMessage: String,
    errorType: ErrorType = ErrorType.GENERIC,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val errorIcon = when (errorType) {
            ErrorType.NETWORK -> "📶"
            ErrorType.TIMEOUT -> "⏰"
            ErrorType.GENERIC -> "⚠️"
        }
        
        Text(
            text = errorIcon,
            fontSize = 48.sp,
            modifier = Modifier.padding(bottom = Dimensions.PaddingLarge)
        )
        Text(
            text = "Unable to load impact data",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = Dimensions.PaddingMedium)
        )
        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = Dimensions.PaddingLarge)
        )
        
        FidanPrimaryButton(
            onClick = onRetry,
            text = "Retry"
        )
    }
}

@Composable
private fun ImpactSuccessContent(
    impactData: ImpactData,
    githubSponsorsUrl: String,
    transparencyReportUrl: String,
    context: android.content.Context
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Real Trees Counter
        RealTreesCard(impactData.realTreesPlanted)
        
        Spacer(modifier = Modifier.height(Dimensions.PaddingLarge))
        
        // Sponsorship Information
        SponsorshipCard(
            githubSponsorsUrl = githubSponsorsUrl,
            context = context
        )
        
        Spacer(modifier = Modifier.height(Dimensions.PaddingLarge))
        
        // Transparency Section
        TransparencyCard(
            impactData = impactData,
            transparencyReportUrl = transparencyReportUrl,
            context = context
        )
        
        Spacer(modifier = Modifier.height(Dimensions.PaddingLarge))
        
        // Partner Organizations
        PartnerOrganizationsCard()
    }
}

@Composable
private fun RealTreesCard(realTreesPlanted: Int) {
    FidanCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = Color(0xFFE8F5E9)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🌳",
                fontSize = 48.sp,
                modifier = Modifier.padding(bottom = Dimensions.PaddingMedium)
            )
            AnimatedTreeCount(
                targetCount = realTreesPlanted,
                modifier = Modifier.semantics { 
                    contentDescription = "Real trees planted counter showing $realTreesPlanted trees" 
                }
            )
            Text(
                text = "Real Trees Planted",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Through GitHub Sponsors",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Dimensions.SpacingSmall)
            )
        }
    }
}

@Composable
private fun SponsorshipCard(
    githubSponsorsUrl: String,
    context: android.content.Context
) {
    FidanCard(modifier = Modifier.fillMaxWidth()) {
        ImpactSectionHeader("💚", "Support Fidan")
        
        Text(
            text = "Every GitHub sponsorship plants real trees! 75% of proceeds go directly to verified tree-planting organizations.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = Dimensions.PaddingMedium)
        )
        Text(
            text = "⚡ 75% trees + 25% maintenance = Sustainable impact",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = Dimensions.PaddingLarge)
        )
        
        // Sponsorship Tiers
        SponsorshipTier("🌱 Seedling", "$3/month", "1 tree planted")
        SponsorshipTier("🌿 Sapling", "$10/month", "5 trees planted")  
        SponsorshipTier("🌳 Forest Guardian", "$25/month", "15 trees planted")
        
        ImpactSectionSpacer()
        
        FidanPrimaryButton(
            onClick = { UrlUtils.openUrl(context, githubSponsorsUrl) },
            text = "Become a Sponsor",
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Become a sponsor button - opens GitHub sponsors page" }
        )
    }
}

@Composable
private fun TransparencyCard(
    impactData: ImpactData,
    transparencyReportUrl: String,
    context: android.content.Context
) {
    FidanCard(modifier = Modifier.fillMaxWidth()) {
        ImpactSectionHeader("📊", "Transparency")
        
        TransparencyItem("Total Donations", "$%.2f".format(impactData.totalDonations))
        TransparencyItem("Tree Planting Fund", "75% of proceeds")
        TransparencyItem("Maintenance Fund", "25% for development")
        TransparencyItem("Partner Organizations", "${impactData.partnersCount} active")
        TransparencyItem("Monthly Growth", "+${impactData.monthlyGrowth}%")
        TransparencyItem("Planting Certificates", "${impactData.certificates} verified")
        TransparencyItem("Last Update", impactData.lastUpdated)
        
        ImpactSectionSpacer()
        
        FidanOutlinedButton(
            onClick = { UrlUtils.openUrl(context, transparencyReportUrl) },
            text = "View Monthly Report",
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PartnerOrganizationsCard() {
    FidanCard(modifier = Modifier.fillMaxWidth()) {
        ImpactSectionHeader("🤝", "Our Partners")
        
        PartnerItem("One Tree Planted", "North America & Global")
        PartnerItem("Eden Reforestation Projects", "Madagascar, Haiti, Nepal")
        PartnerItem("Trees for the Future", "Sub-Saharan Africa")
        
        ImpactSectionSpacer()
        
        Text(
            text = "All partners are verified 501(c)(3) organizations with transparent impact reporting. 75% of sponsorship funds are donated to these organizations, 25% supports app development and maintenance.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun AnimatedTreeCount(
    targetCount: Int,
    modifier: Modifier = Modifier
) {
    require(targetCount >= 0) { "Tree count must be non-negative" }
    
    val animatedCount by animateIntAsState(
        targetValue = targetCount,
        animationSpec = tween(durationMillis = 2000),
        label = "tree_count_animation"
    )
    
    Text(
        text = animatedCount.toString(),
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
    )
}