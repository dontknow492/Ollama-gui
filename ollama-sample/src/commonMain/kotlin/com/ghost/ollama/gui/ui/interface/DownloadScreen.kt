package com.ghost.ollama.gui.ui.`interface`

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import app.cash.paging.compose.itemKey
import com.ghost.ollama.enum.PullStatus
import com.ghost.ollama.gui.ModelEntity
import com.ghost.ollama.gui.TagEntity
import com.ghost.ollama.gui.models.rememberPlatformConfiguration
import com.ghost.ollama.gui.repository.ModelWithTags
import com.ghost.ollama.gui.viewmodel.download.*
import ollama_kmp.ollama_sample.generated.resources.*
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadScreen(
    viewModel: DownloadViewModel = koinViewModel(),
    onBack: () -> Unit,
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val sideEffects = viewModel.sideEffects
    val snackbarHostState = remember { SnackbarHostState() }
    val models = viewModel.pagedModels.collectAsLazyPagingItems()

    LaunchedEffect(sideEffects) {
        sideEffects.collect { effect ->
            val message = when (effect) {
                is DownloadSideEffect.ShowError -> "❌ ${effect.message}"
                is DownloadSideEffect.ShowSuccess -> "✅ ${effect.message}"
                is DownloadSideEffect.ShowSnackbar -> effect.message
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    DownloadScreenContent(
        uiState = uiState,
        pagingModel = models,
        onEvent = viewModel::onEvent,
        snackbarHostState = snackbarHostState,
        modifier = Modifier.fillMaxSize(),
        onBack = onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadScreenContent(
    uiState: DownloadUiState,
    pagingModel: LazyPagingItems<ModelEntity>,
    onEvent: (DownloadEvent) -> Unit,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = rememberPlatformConfiguration()
    val isExpanded = configuration.screenWidthDp >= 600

    var showDownloadsSheet by remember { mutableStateOf(false) }

    // Auto-close sheet if downloads disappear
    LaunchedEffect(uiState.activeDownloads.isEmpty()) {
        if (uiState.activeDownloads.isEmpty()) {
            showDownloadsSheet = false
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier,
        floatingActionButton = {
            if (uiState.activeDownloads.isNotEmpty()) {
                BadgedBox(
                    badge = {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ) {
                            Text(uiState.activeDownloads.size.toString())
                        }
                    }
                ) {
                    FloatingActionButton(
                        onClick = { showDownloadsSheet = !showDownloadsSheet },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(vectorResource(Res.drawable.download), contentDescription = "Active Downloads")
                    }
                }

            }
        }
    ) { paddingValues ->
        Row(
             modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
        ) {
            Column(modifier = Modifier.weight(3f)) {
                // Top bar with search and filter
                SearchAndFilterBar(
                    searchQuery = uiState.searchQuery,
                    activeCapability = uiState.activeCapability,
                    onSearchChange = { onEvent(DownloadEvent.SearchQueryChanged(it)) },
                    onCapabilityChange = { onEvent(DownloadEvent.CapabilityFilterChanged(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    onBack = onBack
                )

                // Main content: two-pane or single column
                if (isExpanded) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        // Left pane: model list
                        ModelListPane(
                            pagedModels = pagingModel,
                            selectedModel = uiState.selectedModel?.model,
                            onModelClick = { model -> onEvent(DownloadEvent.ModelSelected(model)) },
                            modifier = Modifier.weight(1f)
                        )

                        // Right pane: details + active downloads
                        RightPane(
                            selectedModel = uiState.selectedModel,
                            activeDownloads = uiState.activeDownloads,
                            onEvent = onEvent,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    // Mobile: show list or details based on selection
                    if (uiState.selectedModel == null) {
                        ModelListPane(
                            pagedModels = pagingModel,
                            selectedModel = null,
                            onModelClick = { model -> onEvent(DownloadEvent.ModelSelected(model)) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Column(modifier = Modifier.weight(1f)) {
                            // Back button to return to list
                            IconButton(
                                onClick = { onEvent(DownloadEvent.ModelSelected(null)) },
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Icon(vectorResource(Res.drawable.arrow_back), contentDescription = "Back")
                            }
                            RightPane(
                                selectedModel = uiState.selectedModel,
                                activeDownloads = uiState.activeDownloads,
                                onEvent = onEvent,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            if(isExpanded){
                AnimatedVisibility(
                visible = showDownloadsSheet && uiState.activeDownloads.isNotEmpty(),
//                    modifier = Modifier.weight(1f)
            ) {

                HorizontalDivider(
                    Modifier
                        .fillMaxHeight()
                        .width(1.dp), DividerDefaults.Thickness, DividerDefaults.color
                )

                DownloadsPanel(
                    downloads = uiState.activeDownloads,
                    onEvent = onEvent,
                    onClose = { showDownloadsSheet = false },
                    modifier = Modifier.width(340.dp)
                )
            }
            }

        }
    }

    if (showDownloadsSheet && !isExpanded) {
        ModalBottomSheet(
            onDismissRequest = { showDownloadsSheet = false },
        ) {
            DownloadsPanel(
                downloads = uiState.activeDownloads,
                onEvent = onEvent,
                onClose = { showDownloadsSheet = false },
                modifier = Modifier.fillMaxWidth()
            )
        }

    }
}

@Composable
fun DownloadsPanel(
    downloads: Map<String, ActiveDownload>,
    onEvent: (DownloadEvent) -> Unit,
    onClose: (() -> Unit)? = null, // optional (for desktop collapse)
    modifier: Modifier = Modifier
) {
    Surface(
        tonalElevation = 3.dp,
        modifier = modifier.fillMaxHeight()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {

            // =========================
            // Header
            // =========================
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {

                Text(
                    text = "Active Downloads",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )

                if (onClose != null) {
                    IconButton(onClick = onClose) {
                        Icon(
                            vectorResource(Res.drawable.close),
                            contentDescription = "Close"
                        )
                    }
                }
            }

            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

            // =========================
            // Downloads List
            // =========================
            if (downloads.isEmpty()) {

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No active downloads",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

            } else {

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    items(
                        downloads.values.toList(),
                        key = { it.tag }
                    ) { download ->

                        ActiveDownloadItem(
                            download = download,
                            onPause = {
                                onEvent(DownloadEvent.PauseDownload(download.tag))
                            },
                            onResume = {
                                onEvent(DownloadEvent.ResumeDownload(download.tag))
                            },
                            onCancel = {
                                onEvent(DownloadEvent.CancelDownload(download.tag))
                            },
                            onDismiss = {
                                onEvent(DownloadEvent.DismissDownload(download.tag))
                            }
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun SearchAndFilterBar(
    searchQuery: String,
    activeCapability: String?,
    onSearchChange: (String) -> Unit,
    onCapabilityChange: (String?) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Static list of example capabilities – in a real app this would come from the repository
    val capabilities = listOf("vision", "tools", "code", "embedding")
    Column(modifier = modifier.padding(16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    vectorResource(Res.drawable.arrow_back),
                    contentDescription = "Back",
                )
            }
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search models...") },
                leadingIcon = { Icon(vectorResource(Res.drawable.search), contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }


        // Filter chips row
        SecondaryScrollableTabRow(
            selectedTabIndex = capabilities.indexOfFirst { it == activeCapability }.let { if (it == -1) 0 else it + 1 },
            modifier = Modifier.padding(top = 8.dp),
            edgePadding = 0.dp,
            divider = {}
        ) {
            // "All" chip
            FilterChip(
                selected = activeCapability == null,
                onClick = { onCapabilityChange(null) },
                label = { Text("All") },
                modifier = Modifier.padding(end = 8.dp)
            )
            capabilities.forEach { capability ->
                FilterChip(
                    selected = capability == activeCapability,
                    onClick = { onCapabilityChange(capability) },
                    label = { Text(capability) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }
    }
}

@Composable
fun ModelListPane(
    pagedModels: LazyPagingItems<ModelEntity>,
    selectedModel: ModelEntity?,
    onModelClick: (ModelEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val lazyListState = rememberLazyListState()

    LazyColumn(
        state = lazyListState,
        modifier = modifier
            .fillMaxHeight()
            .padding(horizontal = 16.dp)
    ) {
        items(
            count = pagedModels.itemCount,
            key = pagedModels.itemKey { model ->
                model.slug   // or any unique field
            }
        ) { index ->
            val model = pagedModels[index]
            if (model != null) {
                ModelListItem(
                    model = model,
                    isSelected = model.slug == selectedModel?.slug,
                    onClick = { onModelClick(model) }
                )
            }
        }
    }

    // scrolling to selected item when it changes
}

@Composable
fun ModelListItem(
    model: ModelEntity,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = model.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = model.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Icon(vectorResource(Res.drawable.download), contentDescription = null, modifier = Modifier.size(16.dp))
                Text(
                    text = model.pullCount,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(start = 4.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Icon(vectorResource(Res.drawable.update), contentDescription = null, modifier = Modifier.size(16.dp))
                Text(
                    text = model.updated,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

@Composable
fun RightPane(
    selectedModel: ModelWithTags?,
    activeDownloads: Map<String, ActiveDownload>,
    onEvent: (DownloadEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(16.dp)
    ) {
        // Active downloads section
//        if (activeDownloads.isNotEmpty()) {
//            ActiveDownloadsSection(
//                activeDownloads = activeDownloads,
//                onEvent = onEvent,
//                modifier = Modifier.fillMaxWidth()
//            )
//            Spacer(modifier = Modifier.height(16.dp))
//        }

        // Model details section
        if (selectedModel != null) {
            ModelDetailsSection(
                modelWithTags = selectedModel,
                onDownloadTag = { tag -> onEvent(DownloadEvent.StartDownload(tag, selectedModel.model)) },
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Select a model to see details",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ActiveDownloadsSection(
    activeDownloads: Map<String, ActiveDownload>,
    onEvent: (DownloadEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Active Downloads",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            activeDownloads.values.forEach { download ->
                ActiveDownloadItem(
                    download = download,
                    onPause = { onEvent(DownloadEvent.PauseDownload(download.tag)) },
                    onResume = { onEvent(DownloadEvent.ResumeDownload(download.tag)) },
                    onCancel = { onEvent(DownloadEvent.CancelDownload(download.tag)) },
                    onDismiss = { onEvent(DownloadEvent.DismissDownload(download.tag)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun ActiveDownloadItem(
    download: ActiveDownload,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = when (download.status) {
        PullStatus.error -> MaterialTheme.colorScheme.errorContainer
        PullStatus.done -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        modifier = modifier
            .padding(vertical = 6.dp)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            // =========================
            // Header
            // =========================
            Row(verticalAlignment = Alignment.CenterVertically) {

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = download.modelEntity.name,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = download.tag,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                StatusChip(download.status)
            }

            Spacer(Modifier.height(8.dp))

            // =========================
            // Model Metadata
            // =========================
            Text(
                text = download.modelEntity.description,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2
            )

            Spacer(Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InfoLabel("⬇ ${download.modelEntity.pullCount}")
                InfoLabel("🕒 ${download.modelEntity.updated}")
                if (download.modelEntity.capabilities.isNotEmpty()) {
                    InfoLabel(download.modelEntity.capabilities.joinToString())
                }
            }

            // =========================
            // Progress Section
            // =========================
            if (download.status == PullStatus.pulling ||
                download.status == PullStatus.queued
            ) {
                Spacer(Modifier.height(12.dp))

                LinearProgressIndicator(
                    progress = { download.progress },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(4.dp))

                Row {
                    Text(
                        text = "${(download.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall
                    )

                    Spacer(Modifier.weight(1f))

                    download.message?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // =========================
            // Actions
            // =========================
            Spacer(Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.weight(1f))

                when (download.status) {

                    PullStatus.pulling -> {
                        ActionIcon(Res.drawable.pause, "Pause", onPause)
                        ActionIcon(Res.drawable.cancel, "Cancel", onCancel)
                    }

                    PullStatus.queued -> {
                        ActionIcon(Res.drawable.play_arrow, "Resume", onResume)
                        ActionIcon(Res.drawable.cancel, "Cancel", onCancel)
                    }

                    PullStatus.error,
                    PullStatus.done -> {
                        ActionIcon(Res.drawable.close, "Dismiss", onDismiss)
                    }
                }
            }
        }
    }
}


@Composable
private fun ActionIcon(
    iconRes: DrawableResource,
    description: String,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick) {
        Icon(
            vectorResource(iconRes),
            contentDescription = description
        )
    }
}

@Composable
private fun StatusChip(status: PullStatus) {
    val color = when (status) {
        PullStatus.pulling -> MaterialTheme.colorScheme.primary
        PullStatus.queued -> MaterialTheme.colorScheme.secondary
        PullStatus.done -> MaterialTheme.colorScheme.tertiary
        PullStatus.error -> MaterialTheme.colorScheme.error
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = status.name.uppercase(),
            color = color,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun InfoLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}


@Composable
fun ModelDetailsSection(
    modelWithTags: ModelWithTags,
    onDownloadTag: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = modelWithTags.model.name,
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = modelWithTags.model.description,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = modelWithTags.model.readme,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Available Tags",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))

            modelWithTags.tags.forEach { tag ->
                TagCard(
                    tag = tag,
                    onDownload = { onDownloadTag(tag.tag) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun TagCard(
    tag: TagEntity,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = tag.tag,
                    style = MaterialTheme.typography.titleSmall
                )
                Row(modifier = Modifier.padding(top = 4.dp)) {
                    Text(
                        text = "Size: ${tag.size}",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "Context: ${tag.contextWindow}",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                Text(
                    text = "Input: ${tag.inputType}",
                    style = MaterialTheme.typography.labelSmall
                )
            }
            Button(
                onClick = onDownload,
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(vectorResource(Res.drawable.download), contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Download")
            }
        }
    }
}