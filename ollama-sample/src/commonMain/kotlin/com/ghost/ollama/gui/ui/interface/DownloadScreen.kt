package com.ghost.ollama.gui.ui.`interface`

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.paging.compose.collectAsLazyPagingItems
import app.cash.paging.PagingData
import com.ghost.ollama.enum.PullStatus
import com.ghost.ollama.gui.ModelEntity
import com.ghost.ollama.gui.TagEntity
import com.ghost.ollama.gui.models.rememberPlatformConfiguration
import com.ghost.ollama.gui.repository.ModelWithTags
import com.ghost.ollama.gui.viewmodel.download.*
import kotlinx.coroutines.flow.Flow
import ollama_kmp.ollama_sample.generated.resources.*
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadScreen(viewModel: DownloadViewModel = koinViewModel()) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val sideEffects = viewModel.sideEffects
    val snackbarHostState = remember { SnackbarHostState() }

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
        onEvent = viewModel::onEvent,
        snackbarHostState = snackbarHostState,
        modifier = Modifier.fillMaxSize()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadScreenContent(
    uiState: DownloadUiState,
    onEvent: (DownloadEvent) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val configuration = rememberPlatformConfiguration()
    val isExpanded = configuration.screenWidthDp >= 600

    var isBottomBarVisible by remember { mutableStateOf(false) }

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
                        onClick = { isBottomBarVisible = !isBottomBarVisible },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(vectorResource(Res.drawable.download), contentDescription = "Active Downloads")
                    }
                }

            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Top bar with search and filter
            SearchAndFilterBar(
                searchQuery = uiState.searchQuery,
                activeCapability = uiState.activeCapability,
                onSearchChange = { onEvent(DownloadEvent.SearchQueryChanged(it)) },
                onCapabilityChange = { onEvent(DownloadEvent.CapabilityFilterChanged(it)) },
                modifier = Modifier.fillMaxWidth()
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
                        pagedModels = uiState.pagedModels,
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
                        pagedModels = uiState.pagedModels,
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
    }

    if (isBottomBarVisible) {
        ModalBottomSheet(
            onDismissRequest = { isBottomBarVisible = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            uiState.activeDownloads.values.forEach { download ->
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
fun SearchAndFilterBar(
    searchQuery: String,
    activeCapability: String?,
    onSearchChange: (String) -> Unit,
    onCapabilityChange: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    // Static list of example capabilities – in a real app this would come from the repository
    val capabilities = listOf("vision", "tools", "code", "embedding")
    Column(modifier = modifier.padding(16.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search models...") },
            leadingIcon = { Icon(vectorResource(Res.drawable.search), contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

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
    pagedModels: Flow<PagingData<ModelEntity>>,
    selectedModel: ModelEntity?,
    onModelClick: (ModelEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val lazyPagingItems = pagedModels.collectAsLazyPagingItems()

    LazyColumn(
        modifier = modifier
            .fillMaxHeight()
            .padding(horizontal = 16.dp)
    ) {
        items(lazyPagingItems.itemCount, key = { index -> index }) { index ->
            lazyPagingItems[index]?.let {
                ModelListItem(
                    model = it,
                    isSelected = it == selectedModel,
                    onClick = { onModelClick(it) }
                )
            }
        }
    }
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
    Card(
        modifier = modifier.padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (download.status) {
                PullStatus.error -> MaterialTheme.colorScheme.errorContainer
                PullStatus.done -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.secondaryContainer
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = download.modelEntity.name,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = download.tag,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (download.status == PullStatus.pulling) {
                LinearProgressIndicator(
                    progress = { download.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(
                    text = download.status.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )

                // Action buttons
                when (download.status) {
                    PullStatus.pulling -> {
                        IconButton(onClick = onPause) {
                            Icon(vectorResource(Res.drawable.pause), contentDescription = "Pause")
                        }
                        IconButton(onClick = onCancel) {
                            Icon(vectorResource(Res.drawable.cancel), contentDescription = "Cancel")
                        }
                    }

                    PullStatus.queued -> {
                        IconButton(onClick = onResume) {
                            Icon(vectorResource(Res.drawable.play_arrow), contentDescription = "Resume")
                        }
                        IconButton(onClick = onCancel) {
                            Icon(vectorResource(Res.drawable.cancel), contentDescription = "Cancel")
                        }
                    }

                    else -> {
                        IconButton(onClick = onDismiss) {
                            Icon(vectorResource(Res.drawable.close), contentDescription = "Dismiss")
                        }
                    }
                }
            }
        }
    }
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