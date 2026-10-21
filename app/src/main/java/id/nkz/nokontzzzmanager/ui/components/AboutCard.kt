package id.nkz.nokontzzzmanager.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import id.nkz.nokontzzzmanager.R

data class Developer(val name: Int, val role: Int, val githubUsername: String, val drawableResId: Int)
data class RepositoryContributor(val name: Int, val url: String, val description: Int, val drawableResId: Int)

// Special recognition for the main developer who led this rebrand
val leadDeveloper = Developer(R.string.pandeyji_name, R.string.extreme_developer_role, "PandeyJI-9", R.drawable.ekm_logo)

val individualContributors = listOf(
    Developer(R.string.viasco, R.string.nkm_developer, "bimoalfarrabi", R.drawable.viasco),
    Developer(R.string.gustyx_power, R.string.xkm_developer, "Gustyx-Power", R.drawable.gustyx_power),
    Developer(R.string.radika, R.string.rvkm_developer, "Rve27", R.drawable.radika),
    Developer(R.string.danda, R.string.help_and_support, "Danda420", R.drawable.danda),
    Developer(R.string.kenskuyy, R.string.nkm_contributor, "kenaidi01", R.drawable.kenksuyy)
)

val repositoryContributors = listOf(
    RepositoryContributor(R.string.xtra_kernel_manager_repo, "https://github.com/Gustyx-Power/Xtra-Kernel-Manager", R.string.original_project_repo, R.drawable.xkm),
    RepositoryContributor(R.string.rvkernel_manager_repo, "https://github.com/Rve27/RvKernel-Manager", R.string.feature_and_code_reference, R.drawable.rv)
)

@Composable
fun AboutCard(
    blur: Boolean,
    modifier: Modifier = Modifier,
    githubLink: String = stringResource(R.string.github_link),
    telegramLink: String = stringResource(R.string.telegram_link),
) {
    var showCreditsDialog by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp, 8.dp, 24.dp, 24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Section
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ekm_logo),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .padding(8.dp)
                            .size(40.dp)
                    )
                }
                Text(
                    text = stringResource(id = R.string.about),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Description
            Text(
                text = stringResource(id = R.string.desc_about),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 20.sp
            )

            // Action Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val uriHandler = LocalUriHandler.current
                
                // Social Links Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.follow_us),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )

                    IconButton(
                        onClick = { uriHandler.openUri(telegramLink) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.telegram),
                            contentDescription = stringResource(id = R.string.telegram),
                            modifier = Modifier.size(24.dp),
                            tint = Color.Unspecified // Keep original telegram colors
                        )
                    }
                }

                // Source Code and Credits group
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val sourceCodeLink = stringResource(R.string.source_code_link)
                    val supportGroupLink = stringResource(R.string.telegram_support_link)

                    // Support Group Button (Bugs & Requests)
                    FilledTonalButton(
                        onClick = { uriHandler.openUri(supportGroupLink) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.telegram),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Color.Unspecified
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Extreme+ Support (Bugs & Features)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.size(20.dp))
                    }

                    // Source Code Button
                    FilledTonalButton(
                        onClick = { uriHandler.openUri(sourceCodeLink) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(id = R.string.source_code),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.size(20.dp))
                    }

                    // Credits Button
                    FilledTonalButton(
                        onClick = { showCreditsDialog = true },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(id = R.string.credits),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }

    if (showCreditsDialog) {
        CreditsDialog(
            onDismiss = { showCreditsDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreditsDialog(
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)) + 
                        scaleIn(initialScale = 0.95f, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)),
                exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + 
                       scaleOut(targetScale = 0.95f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(0.9f).fillMaxHeight(0.85f),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Column {
                                Text(
                                    text = stringResource(R.string.credits),
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.individual_contributors),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // List of credits
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            // Developer Section
                            item {
                                Text(
                                    text = stringResource(id = R.string.developer),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                            item {
                                DeveloperCreditItem(developer = leadDeveloper, position = 0, totalItems = 1)
                            }

                            // Contributors Section
                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = stringResource(id = R.string.individual_contributors),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                            itemsIndexed(individualContributors) { index, developer ->
                                DeveloperCreditItem(developer = developer, position = index, totalItems = individualContributors.size)
                            }

                            // Repositories Section
                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = stringResource(id = R.string.repository_contributors),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                            itemsIndexed(repositoryContributors) { index, repo ->
                                RepositoryCreditItem(repository = repo, position = index, totalItems = repositoryContributors.size)
                            }
                        }

                        // Dismiss Button
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(stringResource(id = R.string.close), fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeveloperCreditItem(developer: Developer, position: Int, totalItems: Int) {
    val uriHandler = LocalUriHandler.current
    val githubProfileUrl = "https://github.com/${developer.githubUsername}"
    val developerName = stringResource(id = developer.name)
    
    val shape = when {
        totalItems == 1 -> RoundedCornerShape(16.dp)
        position == 0 -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
        position == totalItems - 1 -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
        else -> RoundedCornerShape(4.dp)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { uriHandler.openUri(githubProfileUrl) },
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Image(
                painter = painterResource(id = developer.drawableResId),
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = developerName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(id = developer.role),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(id = R.string.at_github, developer.githubUsername),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun RepositoryCreditItem(repository: RepositoryContributor, position: Int, totalItems: Int) {
    val uriHandler = LocalUriHandler.current
    
    val shape = when {
        totalItems == 1 -> RoundedCornerShape(16.dp)
        position == 0 -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
        position == totalItems - 1 -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
        else -> RoundedCornerShape(4.dp)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { uriHandler.openUri(repository.url) },
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(id = repository.drawableResId),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(id = repository.name),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(id = repository.description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(id = R.string.github),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
