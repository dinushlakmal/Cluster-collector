package com.example

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AppDatabase
import com.example.data.Cluster
import com.example.data.ClusterRepository
import com.example.ui.ClusterViewModel
import com.example.ui.ClusterViewModelFactory
import com.example.ui.LocationUiState
import com.example.ui.theme.MyApplicationTheme
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.provider.MediaStore
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.launch
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.JavascriptInterface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.viewinterop.AndroidView
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.CustomCredential
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import androidx.credentials.exceptions.GetCredentialException

class MainActivity : ComponentActivity() {
    private lateinit var database: AppDatabase
    private lateinit var repository: ClusterRepository
    private lateinit var viewModel: ClusterViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        database = AppDatabase.getDatabase(applicationContext)
        repository = ClusterRepository(database.clusterDao())
        viewModel = ViewModelProvider(
            this,
            ClusterViewModelFactory(repository)
        )[ClusterViewModel::class.java]

        handleIncomingIntent(intent)

        setContent {
            val context = LocalContext.current
            val sharedPrefs = remember { context.getSharedPreferences("app_settings", Context.MODE_PRIVATE) }
            var selectedThemeName by remember { mutableStateOf(sharedPrefs.getString("selected_theme", "DEFAULT_TEAL") ?: "DEFAULT_TEAL") }
            var selectedBackgroundStyle by remember { mutableStateOf(sharedPrefs.getString("background_style", "NONE") ?: "NONE") }
            var selectedBackgroundImageUri by remember { mutableStateOf(sharedPrefs.getString("background_image_uri", "") ?: "") }

            MyApplicationTheme(themeName = selectedThemeName) {
                ClusterAppScreen(
                    viewModel = viewModel,
                    selectedThemeName = selectedThemeName,
                    onThemeChange = { newTheme ->
                        selectedThemeName = newTheme
                        sharedPrefs.edit().putString("selected_theme", newTheme).apply()
                    },
                    selectedBackgroundStyle = selectedBackgroundStyle,
                    onBackgroundStyleChange = { newStyle ->
                        selectedBackgroundStyle = newStyle
                        sharedPrefs.edit().putString("background_style", newStyle).apply()
                    },
                    selectedBackgroundImageUri = selectedBackgroundImageUri,
                    onBackgroundImageUriChange = { newUri ->
                        selectedBackgroundImageUri = newUri
                        sharedPrefs.edit().putString("background_image_uri", newUri).apply()
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        val dataUri = intent.data
        if (Intent.ACTION_VIEW == action && dataUri != null) {
            try {
                contentResolver.openInputStream(dataUri)?.use { inputStream ->
                    val bytes = inputStream.readBytes()
                    val rawContent = String(bytes, Charsets.UTF_8).trim()
                    if (rawContent.startsWith("LAKA:")) {
                        viewModel.pendingImportLakaContent = rawContent
                    } else {
                        Toast.makeText(this, "This file is not a valid .laka file format.", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to read file: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }
}

@Composable
fun ProceduralBackground(style: String, colorScheme: androidx.compose.material3.ColorScheme) {
    if (style == "NONE") return
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val primary = colorScheme.primary
        val outline = colorScheme.outline
        
        when (style) {
            "GRID" -> {
                val step = 40.dp.toPx()
                var y = 0f
                while (y < height) {
                    drawLine(
                        color = outline.copy(alpha = 0.05f),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                    y += step
                }
                var x = 0f
                while (x < width) {
                    drawLine(
                        color = outline.copy(alpha = 0.05f),
                        start = Offset(x, 0f),
                        end = Offset(x, height),
                        strokeWidth = 1.dp.toPx()
                    )
                    x += step
                }
            }
            "WAVE" -> {
                val path1 = androidx.compose.ui.graphics.Path()
                val path2 = androidx.compose.ui.graphics.Path()
                val points = 100
                val dx = width / points
                
                path1.moveTo(0f, height * 0.4f)
                path2.moveTo(0f, height * 0.6f)
                
                for (i in 1..points) {
                    val px = i * dx
                    val py1 = (height * 0.4f) + Math.sin(i * 0.15 + 0.5).toFloat() * 60f
                    val py2 = (height * 0.6f) + Math.cos(i * 0.12).toFloat() * 80f
                    path1.lineTo(px, py1)
                    path2.lineTo(px, py2)
                }
                
                drawPath(
                    path = path1,
                    color = primary.copy(alpha = 0.04f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                )
                drawPath(
                    path = path2,
                    color = colorScheme.secondary.copy(alpha = 0.04f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                )
            }
            "RADAR" -> {
                val cx = width
                val cy = height
                val maxRadius = Math.hypot(width.toDouble(), height.toDouble()).toFloat()
                var radius = 100.dp.toPx()
                while (radius < maxRadius) {
                    drawCircle(
                        color = primary.copy(alpha = 0.04f),
                        radius = radius,
                        center = Offset(cx, cy),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                    )
                    radius += 120.dp.toPx()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ClusterAppScreen(
    viewModel: ClusterViewModel,
    selectedThemeName: String,
    onThemeChange: (String) -> Unit,
    selectedBackgroundStyle: String,
    onBackgroundStyleChange: (String) -> Unit,
    selectedBackgroundImageUri: String,
    onBackgroundImageUriChange: (String) -> Unit
) {
    val context = LocalContext.current
    val clusters by viewModel.clustersState.collectAsStateWithLifecycle()
    val locationState by viewModel.locationState.collectAsStateWithLifecycle()

    val bgBitmap = remember(selectedBackgroundImageUri) {
        if (selectedBackgroundImageUri.isNotEmpty()) {
            try {
                val uri = android.net.Uri.parse(selectedBackgroundImageUri)
                val resolver = context.contentResolver
                val bitmap = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    android.graphics.ImageDecoder.decodeBitmap(android.graphics.ImageDecoder.createSource(resolver, uri))
                } else {
                    @Suppress("DEPRECATION")
                    android.provider.MediaStore.Images.Media.getBitmap(resolver, uri)
                }
                bitmap.copy(android.graphics.Bitmap.Config.ARGB_8888, true).asImageBitmap()
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        } else {
            null
        }
    }

    val pickBackgroundLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            try {
                val contentResolver = context.contentResolver
                val takeFlags: Int = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (e: Exception) {
                // Ignore if not a persistable document Uri
            }
            onBackgroundImageUriChange(uri.toString())
            onBackgroundStyleChange("IMAGE")
        }
    }

    var currentLanguage by remember { mutableStateOf("en") } // "en" or "si"
    
    // Translation helper
    fun t(en: String, si: String): String {
        return if (currentLanguage == "si") si else en
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var showImportExportDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Dialog state variables
    var clusterCodeInput by remember { mutableStateOf("") }
    var manualLatitudeInput by remember { mutableStateOf("") }
    var manualLongitudeInput by remember { mutableStateOf("") }
    var mapLinkInput by remember { mutableStateOf("") }
    var isParsingLink by remember { mutableStateOf(false) }
    var addMode by remember { mutableStateOf("gps") } // "gps", "manual", or "link"
    var selectedClusterOnMap by remember { mutableStateOf<Cluster?>(null) }

    // Storage Access Framework launchers for .laka files
    val exportLakaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            try {
                val lakaData = viewModel.exportClustersToLaka()
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(lakaData.toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(context, "Data successfully exported to locations.laka file!", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Export failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val importLakaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val bytes = inputStream.readBytes()
                    val content = String(bytes, Charsets.UTF_8)
                    viewModel.importClustersFromLaka(content) { success, count, msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        if (success) {
                            showImportExportDialog = false
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Import failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Search and filters
    var searchQuery by remember { mutableStateOf("") }
    var selectedSubAreaFilter by remember { mutableStateOf("ALL") }
    var selectedTab by remember { mutableIntStateOf(0) }
    var clusterToDelete by remember { mutableStateOf<Cluster?>(null) }
    var clusterToEdit by remember { mutableStateOf<Cluster?>(null) }
    var editClusterCodeInput by remember { mutableStateOf("") }
    var showPermissionRationale by remember { mutableStateOf(false) }

    // Confirmation dialog state
    var showConfirmSaveDialog by remember { mutableStateOf(false) }
    var confirmClusterCode by remember { mutableStateOf("") }
    var confirmLatitude by remember { mutableStateOf(0.0) }
    var confirmLongitude by remember { mutableStateOf(0.0) }
    var isOverwriteWarning by remember { mutableStateOf(false) }

    val permissionState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    // Sub-Areas detected dynamically from saved clusters
    val availableSubAreas = remember(clusters) {
        val detected = clusters
            .map { extractSubArea(it.clusterCode) }
            .filter { it.isNotBlank() && it != "OTHER" }
            .distinct()
            .sorted()
        listOf("ALL") + detected
    }

    val filteredClusters = remember(clusters, searchQuery, selectedSubAreaFilter) {
        clusters.filter { cluster ->
            val matchesSubArea = if (selectedSubAreaFilter == "ALL") true else {
                extractSubArea(cluster.clusterCode).equals(selectedSubAreaFilter, ignoreCase = true)
            }
            val query = searchQuery.trim()
            val matchesSearch = if (query.isEmpty()) true else {
                cluster.clusterCode.contains(query, ignoreCase = true)
            }
            matchesSubArea && matchesSearch
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = t("Cluster Collector", "ක්ලස්ටර් කලෙක්ටර්"),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = t("Field Location Coordinator", "ක්ෂේත්‍ර පිහිටීම් සම්බන්ධීකාරක"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    // Language Selector Globe Button
                    var showLanguageMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(
                            onClick = { showLanguageMenu = true },
                            modifier = Modifier.testTag("action_language")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Language,
                                contentDescription = "Change Language / භාෂාව වෙනස් කරන්න",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        DropdownMenu(
                            expanded = showLanguageMenu,
                            onDismissRequest = { showLanguageMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("English", fontWeight = FontWeight.Bold) },
                                onClick = {
                                    currentLanguage = "en"
                                    showLanguageMenu = false
                                },
                                leadingIcon = {
                                    if (currentLanguage == "en") {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("සිංහල (Sinhala)", fontWeight = FontWeight.Bold) },
                                onClick = {
                                    currentLanguage = "si"
                                    showLanguageMenu = false
                                },
                                leadingIcon = {
                                    if (currentLanguage == "si") {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            )
                        }
                    }

                    // Export/Import Button
                    IconButton(
                        onClick = { showImportExportDialog = true },
                        modifier = Modifier.testTag("action_import_export")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ImportExport,
                            contentDescription = "Import or Export Saved Clusters",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    // Themes & Customization Button
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier.testTag("action_settings")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Palette,
                            contentDescription = t("Themes & Backgrounds", "තේමා සහ පසුබිම්"),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    // Help Information Button
                    IconButton(
                        onClick = { showHelpDialog = true },
                        modifier = Modifier.testTag("action_help")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = "Help & Info",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                },
                modifier = Modifier.testTag("app_bar")
            )
        },
        floatingActionButton = {
            if (selectedTab == 0 || (selectedTab == 1 && selectedClusterOnMap == null)) {
                ExtendedFloatingActionButton(
                    onClick = {
                        clusterCodeInput = ""
                        manualLatitudeInput = ""
                        manualLongitudeInput = ""
                        addMode = "gps"
                        viewModel.resetLocationState()
                        if (permissionState.allPermissionsGranted) {
                            showAddDialog = true
                        } else {
                            showPermissionRationale = true
                        }
                    },
                    icon = { Icon(Icons.Rounded.AddLocationAlt, contentDescription = "Add Location") },
                    text = { Text(t("Add / Update Cluster", "ක්ලස්ටරයක් එක් කරන්න / යාවත්කාලීන කරන්න")) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .testTag("add_cluster_fab")
                )
            }
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (selectedBackgroundStyle == "IMAGE" && bgBitmap != null) {
                Image(
                    bitmap = bgBitmap,
                    contentDescription = "Custom Background Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alpha = 0.22f
                )
            } else {
                ProceduralBackground(style = selectedBackgroundStyle, colorScheme = MaterialTheme.colorScheme)
            }

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 840.dp)
                        .background(Color.Transparent)
                ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.fillMaxWidth().testTag("app_tab_row")
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(t("Clusters List", "ක්ලස්ටර් ලැයිස්තුව"), fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                    icon = { Icon(Icons.Rounded.List, contentDescription = "List View") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(t("Interactive Map", "සිතියම (Interactive Map)"), fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                    icon = { Icon(Icons.Rounded.Map, contentDescription = "Map View") }
                )
            }

            Crossfade(
                targetState = selectedTab,
                label = "MainTabCrossfade",
                modifier = Modifier.weight(1f)
            ) { tab ->
                if (tab == 0) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Search Bar & Statistics Card (Visible if database is not empty)
                        if (clusters.isNotEmpty()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Rounded.Explore,
                                                contentDescription = "Clusters Count",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Active Clusters",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = "${clusters.size} Saved",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    OutlinedTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        placeholder = { Text("Search by Cluster Code...") },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Rounded.Search,
                                                contentDescription = "Search icon",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                        },
                                        trailingIcon = {
                                            if (searchQuery.isNotEmpty()) {
                                                IconButton(onClick = { searchQuery = "" }) {
                                                    Icon(
                                                        Icons.Rounded.Close,
                                                        contentDescription = "Clear search",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("search_input")
                                    )

                                    if (availableSubAreas.size > 1) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = t("Sub-Area Filter:", "උප ප්‍රදේශ පෙරහන:"),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (selectedSubAreaFilter != "ALL") {
                                                TextButton(
                                                    onClick = { selectedSubAreaFilter = "ALL" },
                                                    contentPadding = PaddingValues(0.dp)
                                                ) {
                                                    Text(t("Show All", "සියල්ල පෙන්වන්න"), fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            availableSubAreas.forEach { area ->
                                                val count = if (area == "ALL") clusters.size else clusters.count { extractSubArea(it.clusterCode) == area }
                                                FilterChip(
                                                    selected = selectedSubAreaFilter == area,
                                                    onClick = { selectedSubAreaFilter = area },
                                                    label = {
                                                        Text(
                                                            text = if (area == "ALL") t("All ($count)", "සියල්ල ($count)") else "$area ($count)",
                                                            fontSize = 11.sp,
                                                            fontWeight = if (selectedSubAreaFilter == area) FontWeight.Bold else FontWeight.Normal
                                                        )
                                                    },
                                                    leadingIcon = if (selectedSubAreaFilter == area) {
                                                        { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(12.dp)) }
                                                    } else null,
                                                    colors = FilterChipDefaults.filterChipColors(
                                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Cluster List / Empty State Layouts
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            if (clusters.isEmpty()) {
                                EmptyStateLayout(
                                    onAddClick = {
                                        clusterCodeInput = ""
                                        manualLatitudeInput = ""
                                        manualLongitudeInput = ""
                                        addMode = "gps"
                                        viewModel.resetLocationState()
                                        if (permissionState.allPermissionsGranted) {
                                            showAddDialog = true
                                        } else {
                                            showPermissionRationale = true
                                        }
                                    }
                                )
                            } else if (filteredClusters.isEmpty()) {
                                NoSearchResultsLayout(query = searchQuery)
                            } else {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    LazyColumn(
                                        contentPadding = PaddingValues(bottom = 16.dp, start = 16.dp, end = 16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        items(filteredClusters, key = { it.clusterCode }) { cluster ->
                                            ClusterCardItem(
                                                cluster = cluster,
                                                onNavigateClick = {
                                                    viewModel.navigateToCluster(cluster, context)
                                                },
                                                onDeleteClick = {
                                                    clusterToDelete = cluster
                                                },
                                                onEditClick = {
                                                    clusterToEdit = cluster
                                                    editClusterCodeInput = cluster.clusterCode
                                                }
                                            )
                                        }
                                    }

                                    // Beautiful Bottom Credit Footer always visible when there are items
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(8.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Verified,
                                                contentDescription = "Credit Icon",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Built by EDO MANUPA DINUSHA LAKMAL",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                                textAlign = TextAlign.Center,
                                                letterSpacing = 0.5.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    InteractiveMapScreen(
                        clusters = clusters,
                        viewModel = viewModel,
                        selectedClusterOnMap = selectedClusterOnMap,
                        onSelectedClusterOnMapChange = { selectedClusterOnMap = it },
                        onSelectCluster = { cluster ->
                            selectedTab = 0
                            searchQuery = cluster.clusterCode
                        }
                    )
                }
            }
        }
    }
}
}

    // GPS Rationale Dialog
    if (showPermissionRationale) {
        AlertDialog(
            onDismissRequest = { showPermissionRationale = false },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.MyLocation,
                    contentDescription = "Location Access Needed",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            },
            title = {
                Text(
                    text = "GPS Access Required",
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "This app requires high-accuracy GPS permission to fetch your precise coordinate location when registering collection clusters. Please grant permission on the next screen.",
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionRationale = false
                        permissionState.launchMultiplePermissionRequest()
                    }
                ) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionRationale = false }) {
                    Text("Cancel")
                }
            },
            modifier = Modifier.testTag("permission_rationale_dialog")
        )
    }

    // Confirm Delete Dialog
    clusterToDelete?.let { cluster ->
        AlertDialog(
            onDismissRequest = { clusterToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.DeleteForever,
                    contentDescription = "Delete Cluster",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(40.dp)
                )
            },
            title = {
                Text(
                    text = "Delete Cluster Code",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to remove cluster '${cluster.clusterCode}' from your list? This operation is permanent.",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCluster(cluster)
                        clusterToDelete = null
                        Toast.makeText(context, "Cluster ${cluster.clusterCode} removed", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { clusterToDelete = null }) {
                    Text("Cancel")
                }
            },
            modifier = Modifier.testTag("delete_confirmation_dialog")
        )
    }

    // Edit Cluster Code Dialog
    clusterToEdit?.let { cluster ->
        AlertDialog(
            onDismissRequest = { clusterToEdit = null },
            modifier = Modifier
                .widthIn(max = 440.dp)
                .fillMaxWidth(0.92f)
                .testTag("edit_cluster_dialog"),
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "Edit Cluster Code",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = t("Edit Cluster Code", "කාණ්ඩ කේතය සංස්කරණය කරන්න"),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = t("Current Code: ", "වත්මන් කේතය: "),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = cluster.clusterCode,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    SmartClusterCodeInput(
                        value = editClusterCodeInput,
                        onValueChange = { editClusterCodeInput = it },
                        enabled = true,
                        currentLanguage = currentLanguage,
                        compactMode = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = editClusterCodeInput.trim().uppercase()
                        val newCode = when {
                            trimmed.startsWith("MN/") -> trimmed
                            trimmed.startsWith("MN") -> "MN/" + trimmed.removePrefix("MN").removePrefix("/")
                            trimmed.isNotEmpty() -> "MN/$trimmed"
                            else -> ""
                        }
                        if (newCode.isEmpty() || newCode == "MN/") {
                            Toast.makeText(context, t("Please enter a valid cluster code", "කරුණාකර වලංගු කාණ්ඩ කේතයක් ඇතුළත් කරන්න"), Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.updateClusterCode(cluster, newCode) { success, message ->
                                if (success) {
                                    Toast.makeText(context, t("Cluster code updated successfully", "කාණ්ඩ කේතය සාර්ථකව යාවත්කාලීන කරන ලදී"), Toast.LENGTH_SHORT).show()
                                    clusterToEdit = null
                                } else {
                                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(t("Save", "සුරකින්න"))
                }
            },
            dismissButton = {
                TextButton(onClick = { clusterToEdit = null }) {
                    Text(t("Cancel", "අවලංගු කරන්න"))
                }
            }
        )
    }

    // PRE-SAVE CONFIRMATION & OVERWRITE WARNING DIALOG
    if (showConfirmSaveDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmSaveDialog = false },
            icon = {
                Icon(
                    imageVector = if (isOverwriteWarning) Icons.Rounded.WarningAmber else Icons.Rounded.CheckCircleOutline,
                    contentDescription = "Confirm Save",
                    tint = if (isOverwriteWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp)
                )
            },
            title = {
                Text(
                    text = if (isOverwriteWarning) "Confirm Overwrite!" else "Verify Coordinates",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Review cluster registration details before writing to device database:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Cluster Code:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(confirmClusterCode, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Latitude:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("%.6f°".format(confirmLatitude), fontSize = 13.sp)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Longitude:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("%.6f°".format(confirmLongitude), fontSize = 13.sp)
                            }
                        }
                    }

                    if (isOverwriteWarning) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Warning,
                                    contentDescription = "Warning",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "WARNING: Cluster '$confirmClusterCode' already exists! Saving will replace the previous GPS coordinates.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveClusterDirect(confirmClusterCode, confirmLatitude, confirmLongitude)
                        showConfirmSaveDialog = false
                        showAddDialog = false
                        Toast.makeText(
                            context,
                            "Successfully saved '$confirmClusterCode'!",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isOverwriteWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(if (isOverwriteWarning) "Yes, Overwrite" else "Save Cluster")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmSaveDialog = false }) {
                    Text("Go Back")
                }
            },
            modifier = Modifier.testTag("save_confirmation_dialog")
        )
    }

    // MAIN ADD / UPDATE CLUSTER DIALOG (SUPPORTING BOTH GPS & MANUAL MODES)
    if (showAddDialog) {
        Dialog(onDismissRequest = { if (locationState !is LocationUiState.Fetching) showAddDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .testTag("add_cluster_dialog"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Title Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EditLocationAlt,
                            contentDescription = "Pin Location",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Register Location",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // MODE TABS selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (addMode == "gps") MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable(enabled = locationState !is LocationUiState.Fetching && !isParsingLink) { addMode = "gps" }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "GPS Satellite",
                                color = if (addMode == "gps") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1.1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (addMode == "manual") MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable(enabled = locationState !is LocationUiState.Fetching && !isParsingLink) { addMode = "manual" }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Manual Input",
                                color = if (addMode == "manual") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (addMode == "link") MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable(enabled = locationState !is LocationUiState.Fetching && !isParsingLink) { addMode = "link" }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Google Maps Sync",
                                color = if (addMode == "link") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Common Cluster Code field with static MN/ prefix & smart input helper
                    SmartClusterCodeInput(
                        value = clusterCodeInput,
                        onValueChange = { clusterCodeInput = it },
                        enabled = locationState !is LocationUiState.Fetching && !isParsingLink,
                        currentLanguage = currentLanguage
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // TAB CONTENT AREA
                    if (addMode == "gps") {
                        Text(
                            text = "Fetches coordinate Lat/Lng points using the phone's native GPS. Stand outdoors under open skies for high accuracy.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        AnimatedContent(
                            targetState = locationState,
                            label = "LocationStatusTransition"
                        ) { state ->
                            when (state) {
                                is LocationUiState.Idle -> {
                                    Button(
                                        onClick = {
                                            if (clusterCodeInput.trim().isEmpty()) {
                                                Toast.makeText(context, "Please enter a valid Cluster Code first.", Toast.LENGTH_SHORT).show()
                                            } else {
                                                viewModel.fetchGpsLocation(context) { success, lat, lng, msg ->
                                                    if (success) {
                                                        if (lat in 5.8..10.0 && lng in 79.5..82.2) {
                                                            // Load details and pop confirmation dialog
                                                            confirmClusterCode = clusterCodeInput.trim().uppercase()
                                                            confirmLatitude = lat
                                                            confirmLongitude = lng
                                                            viewModel.getClusterByCode(confirmClusterCode) { existing ->
                                                                isOverwriteWarning = (existing != null)
                                                                showConfirmSaveDialog = true
                                                            }
                                                        } else {
                                                            Toast.makeText(context, "Error: GPS coordinates (" + String.format("%.4f", lat) + ", " + String.format("%.4f", lng) + ") are outside Sri Lanka. Only Sri Lankan locations are supported.", Toast.LENGTH_LONG).show()
                                                        }
                                                    } else {
                                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp)
                                            .testTag("save_location_button"),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Rounded.MyLocation, contentDescription = "GPS")
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Fetch GPS Coordinates", fontWeight = FontWeight.Bold)
                                    }
                                }
                                is LocationUiState.Fetching -> {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        CircularProgressIndicator(
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "Connecting to high-accuracy GPS satellite...",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.primary,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = "Make sure device GPS is active.",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                                is LocationUiState.Success -> {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            Icons.Rounded.CheckCircle,
                                            contentDescription = "Success",
                                            tint = Color(0xFF10B981)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Coordinates Fetched Successfully!",
                                            color = Color(0xFF10B981),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                is LocationUiState.Error -> {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(
                                                Icons.Rounded.Error,
                                                contentDescription = "Error",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "GPS Signal Error",
                                                color = MaterialTheme.colorScheme.error,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = state.message,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Button(
                                            onClick = {
                                                viewModel.fetchGpsLocation(context) { success, lat, lng, msg ->
                                                    if (success) {
                                                        if (lat in 5.8..10.0 && lng in 79.5..82.2) {
                                                            confirmClusterCode = clusterCodeInput.trim().uppercase()
                                                            confirmLatitude = lat
                                                            confirmLongitude = lng
                                                            viewModel.getClusterByCode(confirmClusterCode) { existing ->
                                                                isOverwriteWarning = (existing != null)
                                                                showConfirmSaveDialog = true
                                                            }
                                                        } else {
                                                            Toast.makeText(context, "Error: GPS coordinates (" + String.format("%.4f", lat) + ", " + String.format("%.4f", lng) + ") are outside Sri Lanka. Only Sri Lankan locations are supported.", Toast.LENGTH_LONG).show()
                                                        }
                                                    } else {
                                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text("Retry GPS Connection")
                                        }
                                    }
                                }
                            }
                        }
                    } else if (addMode == "manual") {
                        // MANUAL ENTRY FORM
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = manualLatitudeInput,
                                onValueChange = { manualLatitudeInput = it },
                                label = { Text("Latitude") },
                                placeholder = { Text("e.g. 6.9271") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().testTag("manual_latitude_input"),
                                leadingIcon = {
                                    Icon(
                                        Icons.Rounded.PinDrop,
                                        contentDescription = "Lat",
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            )

                            OutlinedTextField(
                                value = manualLongitudeInput,
                                onValueChange = { manualLongitudeInput = it },
                                label = { Text("Longitude") },
                                placeholder = { Text("e.g. 79.8612") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().testTag("manual_longitude_input"),
                                leadingIcon = {
                                    Icon(
                                        Icons.Rounded.PinDrop,
                                        contentDescription = "Lng",
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    val code = clusterCodeInput.trim()
                                    val lat = manualLatitudeInput.trim().toDoubleOrNull()
                                    val lng = manualLongitudeInput.trim().toDoubleOrNull()

                                    if (code.isEmpty()) {
                                        Toast.makeText(context, "Please enter a valid Cluster Code.", Toast.LENGTH_SHORT).show()
                                    } else if (lat == null || lat < -90.0 || lat > 90.0) {
                                        Toast.makeText(context, "Please enter a valid Latitude (-90 to 90).", Toast.LENGTH_SHORT).show()
                                    } else if (lng == null || lng < -180.0 || lng > 180.0) {
                                        Toast.makeText(context, "Please enter a valid Longitude (-180 to 180).", Toast.LENGTH_SHORT).show()
                                    } else if (lat < 5.8 || lat > 10.0 || lng < 79.5 || lng > 82.2) {
                                        Toast.makeText(context, "Error: Coordinates (" + String.format("%.4f", lat) + ", " + String.format("%.4f", lng) + ") are outside Sri Lanka. Only Sri Lankan locations are supported.", Toast.LENGTH_LONG).show()
                                    } else {
                                        confirmClusterCode = code.uppercase()
                                        confirmLatitude = lat
                                        confirmLongitude = lng
                                        viewModel.getClusterByCode(confirmClusterCode) { existing ->
                                            isOverwriteWarning = (existing != null)
                                            showConfirmSaveDialog = true
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("verify_manual_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Rounded.Check, contentDescription = "Verify")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Verify & Save Cluster", fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        // MAPS LINK PARSING FORM
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = t(
                                    "Paste a Google Maps share link (e.g. from maps.app.goo.gl or standard map URL) to extract GPS coordinates automatically.",
                                    "GPS ඛණ්ඩාංක ස්වයංක්‍රීයව ලබා ගැනීමට ගූගල් මැප්ස් (Google Maps) සබැඳිය මෙතැනට ඇතුළත් කරන්න."
                                ),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp,
                                textAlign = TextAlign.Center
                            )

                            OutlinedTextField(
                                value = mapLinkInput,
                                onValueChange = { mapLinkInput = it },
                                label = { Text(t("Google Maps Link", "ගූගල් මැප්ස් සබැඳිය")) },
                                placeholder = { Text(t("Paste Google Maps link here...", "ගූගල් මැප්ස් සබැඳිය මෙතැනට ඇතුළත් කරන්න...")) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().testTag("map_link_input"),
                                leadingIcon = {
                                    Icon(
                                        Icons.Rounded.Link,
                                        contentDescription = "Link",
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                },
                                enabled = !isParsingLink
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            if (isParsingLink) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = t("Resolving link & extracting GPS points...", "සබැඳිය පරීක්ෂා කරමින් සහ GPS පිහිටීම ලබාගනිමින්..."),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                Button(
                                    onClick = {
                                        val code = clusterCodeInput.trim()
                                        val url = mapLinkInput.trim()

                                        if (code.isEmpty()) {
                                            Toast.makeText(context, t("Please enter a valid Cluster Code.", "කරුණාකර වලංගු ක්ලස්ටර් කේතයක් ඇතුළත් කරන්න."), Toast.LENGTH_SHORT).show()
                                        } else if (url.isEmpty()) {
                                            Toast.makeText(context, t("Please enter a Google Maps Link.", "කරුණාකර ගූගල් මැප්ස් සබැඳියක් ඇතුළත් කරන්න."), Toast.LENGTH_SHORT).show()
                                        } else {
                                            isParsingLink = true
                                            viewModel.resolveAndParseGoogleMapsUrl(url) { success, lat, lng, msg ->
                                                isParsingLink = false
                                                if (success) {
                                                    if (lat in 5.8..10.0 && lng in 79.5..82.2) {
                                                        confirmClusterCode = code.uppercase()
                                                        confirmLatitude = lat
                                                        confirmLongitude = lng
                                                        viewModel.getClusterByCode(confirmClusterCode) { existing ->
                                                            isOverwriteWarning = (existing != null)
                                                            showConfirmSaveDialog = true
                                                        }
                                                    } else {
                                                        Toast.makeText(context, "Error: Resolved coordinates (" + String.format("%.4f", lat) + ", " + String.format("%.4f", lng) + ") are outside Sri Lanka. Only Sri Lankan locations are supported.", Toast.LENGTH_LONG).show()
                                                    }
                                                } else {
                                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("verify_link_button"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Rounded.Search, contentDescription = "Parse")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(t("Parse & Verify Link", "සබැඳිය පරීක්ෂා කර තහවුරු කරන්න"), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    if (locationState !is LocationUiState.Fetching) {
                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(
                            onClick = { showAddDialog = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    // DEVICE TO DEVICE IMPORT & EXPORT MODAL DIALOG
    if (showImportExportDialog) {
        LakaImportExportDialog(
            onDismiss = { showImportExportDialog = false },
            currentLanguage = currentLanguage,
            exportLakaLauncher = exportLakaLauncher,
            importLakaLauncher = importLakaLauncher,
            onShareLaka = {
                try {
                    val lakaData = viewModel.exportClustersToLaka()
                    val cacheFile = java.io.File(context.cacheDir, "locations.laka")
                    cacheFile.writeText(lakaData, Charsets.UTF_8)
                    
                    val uri = androidx.core.content.FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        cacheFile
                    )
                    
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/octet-stream"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    
                    context.startActivity(Intent.createChooser(intent, t("Share .laka Backup File", ".laka උපස්ථ ගොනුව Share කරන්න")))
                } catch (e: Exception) {
                    Toast.makeText(context, "Sharing failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    // EXTERNAL LAKA FILE IMPORT CONFIRMATION DIALOG
    if (viewModel.pendingImportLakaContent != null) {
        AlertDialog(
            onDismissRequest = { viewModel.pendingImportLakaContent = null },
            title = {
                Text(
                    text = t("Import Locations?", "ස්ථාන ආනයනය කරන්නද?"),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Text(
                    text = t(
                        "An external '.laka' backup file was opened. Would you like to import all the location codes from this file into your application?",
                        "බාහිර '.laka' උපස්ථ ගොනුවක් විවෘත කරන ලදී. මෙම ගොනුවේ ඇති සියලුම ස්ථාන කේත මෙම ඇප් එකට ඇතුළත් කිරීමට ඔබට අවශ්‍යද?"
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    onClick = {
                        val content = viewModel.pendingImportLakaContent
                        if (content != null) {
                            viewModel.importClustersFromLaka(content) { success, count, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                viewModel.pendingImportLakaContent = null
                            }
                        }
                    }
                ) {
                    Text(t("Import", "ආනයනය කරන්න"), color = MaterialTheme.colorScheme.onPrimary)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.pendingImportLakaContent = null }
                ) {
                    Text(t("Cancel", "අවලංගු කරන්න"), color = MaterialTheme.colorScheme.primary)
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // THEMES & CUSTOMIZATION SETTINGS DIALOG
    if (showSettingsDialog) {
        Dialog(onDismissRequest = { showSettingsDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .testTag("settings_dialog"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Palette,
                            contentDescription = "Theme Icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = t("Themes & Customization", "තේමා සහ පසුබිම්"),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = t("Select Color Theme", "වර්ණ තේමාව තෝරන්න"),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val themes = listOf(
                        Triple("DEFAULT_TEAL", "Teal & Slate (Default)", "ටීල් සහ ස්ලේට් (පෙරනිමි)"),
                        Triple("ROYAL_PURPLE", "Royal Amethyst", "රාජකීය දම්"),
                        Triple("EMERALD_GREEN", "Emerald Mint", "කොළ සහ මින්ට්"),
                        Triple("SUNSET_CRIMSON", "Sunset Amber", "රතු සහ රන්"),
                        Triple("MIDNIGHT_NEON", "Cyberpunk Neon", "සයිබර්පන්ක් නියොන්")
                    )

                    themes.forEach { (key, enName, siName) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedThemeName == key) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent)
                                .clickable { onThemeChange(key) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedThemeName == key,
                                onClick = { onThemeChange(key) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (currentLanguage == "si") siName else enName,
                                fontSize = 14.sp,
                                fontWeight = if (selectedThemeName == key) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedThemeName == key) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = t("Select Background Art Style", "පසුබිම් කලා ශෛලිය තෝරන්න"),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val backgroundStyles = listOf(
                        Triple("NONE", "None (Clean Solid)", "කිසිවක් නැත (පිරිසිදු පසුබිම)"),
                        Triple("GRID", "Digital Coordinate Grid", "ඩිජිටල් ජාලකය (Digital Grid)"),
                        Triple("WAVE", "Aesthetic Coordinate Waves", "කලාත්මක තරංග (Aesthetic Waves)"),
                        Triple("RADAR", "Concentric Radar Range Rings", "රේඩාර් කව (Concentric Radar Rings)")
                    )

                    backgroundStyles.forEach { (key, enName, siName) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedBackgroundStyle == key) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f) else Color.Transparent)
                                .clickable { onBackgroundStyleChange(key) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedBackgroundStyle == key,
                                onClick = { onBackgroundStyleChange(key) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (currentLanguage == "si") siName else enName,
                                fontSize = 14.sp,
                                fontWeight = if (selectedBackgroundStyle == key) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedBackgroundStyle == key) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedBackgroundStyle == "IMAGE") MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f) else Color.Transparent)
                            .clickable {
                                pickBackgroundLauncher.launch("image/*")
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedBackgroundStyle == "IMAGE",
                            onClick = { pickBackgroundLauncher.launch("image/*") }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = t("Custom Background Image...", "පසුබිම් ඡායාරූපයක් තෝරන්න..."),
                                fontSize = 14.sp,
                                fontWeight = if (selectedBackgroundStyle == "IMAGE") FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedBackgroundStyle == "IMAGE") MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                            if (selectedBackgroundImageUri.isNotEmpty() && selectedBackgroundStyle == "IMAGE") {
                                Text(
                                    text = t("Custom image active", "ඡායාරූපය සක්‍රියයි"),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { showSettingsDialog = false },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(t("Close", "වසා දමන්න"), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // HELP & INFO SHEET WITH ATTRIBUTION
    if (showHelpDialog) {
        Dialog(onDismissRequest = { showHelpDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .testTag("help_dialog"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Rounded.VerifiedUser,
                        contentDescription = "Verified Seal",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(56.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = t("Cluster Collector", "ක්ලස්ටර් කලෙක්ටර්"),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = t("v1.0.2 - Premium Edition", "v1.0.2 - ප්‍රිමියම් සංස්කරණය"),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = t(
                            "This application assists microfinance field collectors by caching office loan collection sections (Clusters) and opening native high-speed GPS navigation in one tap.",
                            "මෙම යෙදුම මඟින් මයික්‍රොෆිනෑන්ස් (ක්ෂුද්‍ර මූල්‍ය) ක්ෂේත්‍ර එකතු කරන්නන්ට ණය එකතු කිරීමේ කලාප (Clusters) පහසුවෙන් මතක තබා ගැනීමට සහ එක් තට්ටු කිරීමකින් වේගවත් GPS සංචලනය විවෘත කිරීමට සහාය වේ."
                        ),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = t("DEVELOPER ATTRIBUTION", "සංවර්ධක තොරතුරු"),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "EDO MANUPA DINUSHA LAKMAL",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = t("Senior Mobile Engineer", "ජ්‍යෙෂ්ඨ ජංගම යෙදුම් ඉංජිනේරු"),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { showHelpDialog = false },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(t("Okay, Got It", "හරි, තේරුණා"), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ClusterCardItem(
    cluster: Cluster,
    onNavigateClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onEditClick: () -> Unit
) {
    val dateString = remember(cluster.updatedAt) {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        sdf.format(Date(cluster.updatedAt))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onNavigateClick,
                onLongClick = onDeleteClick
            )
            .testTag("cluster_card_${cluster.clusterCode}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Navigation,
                                contentDescription = "Cluster Area Icon",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = cluster.clusterCode,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag("cluster_title_${cluster.clusterCode}")
                        )

                        val subArea = remember(cluster.clusterCode) { extractSubArea(cluster.clusterCode) }
                        if (subArea.isNotBlank() && subArea != "OTHER") {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = subArea,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LocationOn,
                            contentDescription = "GPS Pin",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Lat: %.5f°  |  Lng: %.5f°".format(cluster.latitude, cluster.longitude),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.testTag("edit_button_${cluster.clusterCode}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "Edit Cluster Code",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                    }

                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.testTag("delete_button_${cluster.clusterCode}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "Remove Cluster",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.AccessTime,
                        contentDescription = "Timestamp",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Updated $dateString",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                Button(
                    onClick = onNavigateClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("navigate_button_${cluster.clusterCode}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.NearMe,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "NAVIGATE",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyStateLayout(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Aesthetic illustration area
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(140.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = Icons.Rounded.ExploreOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "No Clusters Registered",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Field officers can save office loan collection sections by registering current high-accuracy GPS coordinates under a Cluster Code.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onAddClick,
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
            modifier = Modifier.testTag("empty_state_add_button")
        ) {
            Icon(Icons.Rounded.AddLocation, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Register First Cluster", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Developer footer in empty state
        Text(
            text = "Built by EDO MANUPA DINUSHA LAKMAL",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun NoSearchResultsLayout(query: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Rounded.SearchOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(60.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No results for '$query'",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Try refining your search query or add a new cluster using the plus button below.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
    }
}

fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    return r * c
}

fun formatDistance(km: Double): String {
    return if (km < 1.0) {
        "${(km * 1000).toInt()}m"
    } else {
        "${"%.1f".format(km)}km"
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun InteractiveMapScreen(
    clusters: List<Cluster>,
    viewModel: ClusterViewModel,
    selectedClusterOnMap: Cluster?,
    onSelectedClusterOnMapChange: (Cluster?) -> Unit,
    onSelectCluster: (Cluster) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var zoom by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    val infiniteTransition = rememberInfiniteTransition(label = "RadarSweep")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SweepAngle"
    )

    // Track user live location
    var userLocation by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    val fusedLocationClient = remember { com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context) }
    
    val syncUserLocation = {
        try {
            fusedLocationClient.getCurrentLocation(
                com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                com.google.android.gms.tasks.CancellationTokenSource().token
            ).addOnSuccessListener { location ->
                if (location != null) {
                    userLocation = Pair(location.latitude, location.longitude)
                }
            }.addOnFailureListener {
                fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                    if (lastLoc != null) {
                        userLocation = Pair(lastLoc.latitude, lastLoc.longitude)
                    }
                }
            }
        } catch (e: SecurityException) {
            // Permission not granted yet
        } catch (e: Exception) {
            // Other exceptions
        }
    }

    // Auto sync location when screen launches and periodically every 10 seconds
    LaunchedEffect(Unit) {
        syncUserLocation()
        while (true) {
            kotlinx.coroutines.delay(10000)
            syncUserLocation()
        }
    }

    // Device location is strictly prioritized as the RADAR CENTER
    val centerLat = remember(userLocation, clusters) {
        userLocation?.first ?: if (clusters.isNotEmpty()) clusters.map { it.latitude }.average() else 7.8731
    }
    val centerLng = remember(userLocation, clusters) {
        userLocation?.second ?: if (clusters.isNotEmpty()) clusters.map { it.longitude }.average() else 80.7718
    }

    val baseScale = remember(clusters, centerLat, centerLng) {
        if (clusters.isEmpty()) {
            6000f
        } else {
            val maxLatDiff = clusters.maxOfOrNull { Math.abs(it.latitude - centerLat) } ?: 0.001
            val maxLngDiff = clusters.maxOfOrNull { Math.abs(it.longitude - centerLng) } ?: 0.001
            val maxDiff = maxOf(maxLatDiff, maxLngDiff)
            if (maxDiff > 0.0001) {
                // scale so that furthest cluster fits comfortably inside 180dp radius
                (180f / maxDiff.toFloat())
            } else {
                8000f
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
    ) {
        val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        val primaryColor = MaterialTheme.colorScheme.primary
        val secondaryColor = MaterialTheme.colorScheme.secondary

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = constraints.maxWidth.toFloat()
            val canvasHeight = constraints.maxHeight.toFloat()
            val centerX = canvasWidth / 2f
            val centerY = canvasHeight / 2f

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(clusters, zoom, panOffset) {
                        detectTapGestures { tapOffset ->
                            val tapThresholdPx = 36f * density
                            var foundCluster: Cluster? = null
                            var minDistance = Double.MAX_VALUE

                            clusters.forEach { cluster ->
                                val x = centerX + (cluster.longitude - centerLng).toFloat() * baseScale * zoom + panOffset.x
                                val y = centerY - (cluster.latitude - centerLat).toFloat() * baseScale * zoom + panOffset.y
                                val dist = Math.hypot((tapOffset.x - x).toDouble(), (tapOffset.y - y).toDouble())
                                if (dist < tapThresholdPx && dist < minDistance) {
                                    minDistance = dist
                                    foundCluster = cluster
                                }
                            }
                            onSelectedClusterOnMapChange(foundCluster)
                        }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            panOffset = Offset(panOffset.x + dragAmount.x, panOffset.y + dragAmount.y)
                        }
                    }
                    .testTag("interactive_map_canvas")
            ) {
                val radarX = centerX + panOffset.x
                val radarY = centerY + panOffset.y

                // A. Radar Scope Radial Gradient
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.18f),
                            Color.Transparent
                        ),
                        center = Offset(radarX, radarY),
                        radius = 650f * zoom
                    )
                )

                // B. Concentric Radar Range Rings
                val ringRadii = listOf(90f, 200f, 340f, 500f, 680f)
                ringRadii.forEach { r ->
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.22f),
                        radius = r * zoom,
                        center = Offset(radarX, radarY),
                        style = Stroke(width = 1.5f)
                    )
                }

                // C. Active Sweeping Radar Line
                val angleRad = Math.toRadians(sweepAngle.toDouble())
                val scanLineLength = 800f * zoom
                val scanEndX = radarX + (Math.cos(angleRad) * scanLineLength).toFloat()
                val scanEndY = radarY + (Math.sin(angleRad) * scanLineLength).toFloat()

                drawLine(
                    color = primaryColor.copy(alpha = 0.6f),
                    start = Offset(radarX, radarY),
                    end = Offset(scanEndX, scanEndY),
                    strokeWidth = 3f
                )

                for (i in 1..5) {
                    val trailAngleRad = Math.toRadians((sweepAngle - (i * 3.5f)).toDouble())
                    val trailEndX = radarX + (Math.cos(trailAngleRad) * scanLineLength).toFloat()
                    val trailEndY = radarY + (Math.sin(trailAngleRad) * scanLineLength).toFloat()
                    drawLine(
                        color = primaryColor.copy(alpha = 0.35f / i),
                        start = Offset(radarX, radarY),
                        end = Offset(trailEndX, trailEndY),
                        strokeWidth = 2.5f
                    )
                }

                // D. Tactical Grid
                val step = 120f
                val startX = (panOffset.x % step) - step
                val startY = (panOffset.y % step) - step

                for (x in generateSequence(startX) { it + step }.takeWhile { it < canvasWidth + step }) {
                    drawLine(color = gridColor, start = Offset(x, 0f), end = Offset(x, canvasHeight), strokeWidth = 1f)
                }
                for (y in generateSequence(startY) { it + step }.takeWhile { it < canvasHeight + step }) {
                    drawLine(color = gridColor, start = Offset(0f, y), end = Offset(canvasWidth, y), strokeWidth = 1f)
                }

                // Crosshair Center Target Lines
                drawLine(
                    color = primaryColor.copy(alpha = 0.3f),
                    start = Offset(radarX, 0f),
                    end = Offset(radarX, canvasHeight),
                    strokeWidth = 1.5f
                )
                drawLine(
                    color = primaryColor.copy(alpha = 0.3f),
                    start = Offset(0f, radarY),
                    end = Offset(canvasWidth, radarY),
                    strokeWidth = 1.5f
                )

                // E. Draw Network Mesh vector lines connecting Radar Center (User) to surrounding Clusters
                clusters.forEach { cluster ->
                    val cx = centerX + (cluster.longitude - centerLng).toFloat() * baseScale * zoom + panOffset.x
                    val cy = centerY - (cluster.latitude - centerLat).toFloat() * baseScale * zoom + panOffset.y

                    if (cx >= -100f && cx <= canvasWidth + 100f && cy >= -100f && cy <= canvasHeight + 100f) {
                        val isSelected = selectedClusterOnMap?.clusterCode == cluster.clusterCode
                        drawLine(
                            color = if (isSelected) primaryColor.copy(alpha = 0.8f) else Color(0xFFFF6B00).copy(alpha = 0.45f),
                            start = Offset(radarX, radarY),
                            end = Offset(cx, cy),
                            strokeWidth = if (isSelected) 2.5f else 1.2f
                        )
                    }
                }

                // F. RADAR CENTER: Device Location (User)
                val userPulseRadius = 18f + (sweepAngle % 90f) * 0.2f
                // Pulse Ring 1
                drawCircle(
                    color = Color(0xFF3B82F6).copy(alpha = 0.25f),
                    radius = userPulseRadius * zoom,
                    center = Offset(radarX, radarY)
                )
                // Pulse Ring 2
                drawCircle(
                    color = Color(0xFF2563EB).copy(alpha = 0.5f),
                    radius = 12f * zoom,
                    center = Offset(radarX, radarY)
                )
                // Core Blue Dot
                drawCircle(
                    color = Color(0xFF1D4ED8),
                    radius = 8f * zoom,
                    center = Offset(radarX, radarY)
                )
                // White Center Spot
                drawCircle(
                    color = Color.White,
                    radius = 3f * zoom,
                    center = Offset(radarX, radarY)
                )

                // G. Plot Surrounding Cluster Pins
                clusters.forEach { cluster ->
                    val x = centerX + (cluster.longitude - centerLng).toFloat() * baseScale * zoom + panOffset.x
                    val y = centerY - (cluster.latitude - centerLat).toFloat() * baseScale * zoom + panOffset.y

                    if (x >= -50f && x <= canvasWidth + 50f && y >= -50f && y <= canvasHeight + 50f) {
                        val isSelected = selectedClusterOnMap?.clusterCode == cluster.clusterCode
                        val pinColor = if (isSelected) primaryColor else Color(0xFF0F2942)

                        drawCircle(
                            color = if (isSelected) primaryColor.copy(alpha = 0.25f) else Color(0xFFFF6B00).copy(alpha = 0.2f),
                            radius = if (isSelected) 34f else 22f,
                            center = Offset(x, y)
                        )
                        drawCircle(
                            color = pinColor,
                            radius = if (isSelected) 10f else 8f,
                            center = Offset(x, y)
                        )
                        drawCircle(
                            color = Color(0xFFFF6B00),
                            radius = 3.5f,
                            center = Offset(x, y)
                        )
                    }
                }
            }

            // Overlay Badge for Radar Center (User Location)
            val userDpX = with(LocalDensity.current) { (centerX + panOffset.x).toDp() }
            val userDpY = with(LocalDensity.current) { (centerY + panOffset.y).toDp() }

            if ((centerX + panOffset.x) >= 10f && (centerX + panOffset.x) <= canvasWidth - 10f &&
                (centerY + panOffset.y) >= 10f && (centerY + panOffset.y) <= canvasHeight - 10f) {
                Box(
                    modifier = Modifier
                        .offset(userDpX - 55.dp, userDpY - 38.dp)
                        .width(110.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        color = Color(0xFF1D4ED8),
                        shape = RoundedCornerShape(8.dp),
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "MY LOCATION",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Overlay Badges for Surrounding Clusters with distance from Radar Center
            clusters.forEachIndexed { index, cluster ->
                val x = centerX + (cluster.longitude - centerLng).toFloat() * baseScale * zoom + panOffset.x
                val y = centerY - (cluster.latitude - centerLat).toFloat() * baseScale * zoom + panOffset.y

                val xDp = with(LocalDensity.current) { x.toDp() }
                val yDp = with(LocalDensity.current) { y.toDp() }

                if (x >= 20f && x <= canvasWidth - 20f && y >= 20f && y <= canvasHeight - 20f) {
                    val isSelected = selectedClusterOnMap?.clusterCode == cluster.clusterCode
                    val distKm = if (userLocation != null) {
                        calculateDistanceKm(userLocation!!.first, userLocation!!.second, cluster.latitude, cluster.longitude)
                    } else null
                    val distLabel = if (distKm != null) " • ${formatDistance(distKm)}" else ""

                    val yOffset = if (isSelected) {
                        yDp - 38.dp
                    } else if (index % 2 == 0) {
                        yDp - 38.dp
                    } else {
                        yDp + 14.dp
                    }

                    Box(
                        modifier = Modifier
                            .offset(xDp - 60.dp, yOffset)
                            .width(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 6.dp else 2.dp)
                        ) {
                            Text(
                                text = "${cluster.clusterCode}$distLabel",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp),
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Floating Map Controls Dashboard
            Card(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { zoom = (zoom + 0.5f).coerceAtMost(8f) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ZoomIn,
                            contentDescription = "Zoom In",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = { zoom = (zoom - 0.5f).coerceAtLeast(0.4f) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ZoomOut,
                            contentDescription = "Zoom Out",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = {
                            zoom = 1f
                            panOffset = Offset.Zero
                            onSelectedClusterOnMapChange(null)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FilterCenterFocus,
                            contentDescription = "Center on My Location",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = {
                            syncUserLocation()
                            panOffset = Offset.Zero
                            Toast.makeText(context, "Location synced to Radar Center / ලොකේෂනය රේඩාර් මැදට සමමුහුර්ත විය", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MyLocation,
                            contentDescription = "Sync GPS Location",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Radar HUD Status overlay top right
            Card(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Radar,
                            contentDescription = "Radar Scope",
                            tint = Color(0xFF1D4ED8),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "RADAR CENTER",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1D4ED8)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        if (userLocation != null) "Lat: ${"%.4f".format(userLocation!!.first)}" else "GPS Searching...",
                        fontSize = 8.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        if (userLocation != null) "Lng: ${"%.4f".format(userLocation!!.second)}" else "Acquiring...",
                        fontSize = 8.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Selected Pin Details Card overlay bottom
            AnimatedVisibility(
                visible = selectedClusterOnMap != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                selectedClusterOnMap?.let { cluster ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.LocationSearching,
                                            contentDescription = "Selected Pin",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = cluster.clusterCode,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Lat: %.5f° | Lng: %.5f°".format(cluster.latitude, cluster.longitude),
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { onSelectedClusterOnMapChange(null) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Close Details",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { onSelectCluster(cluster) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.List,
                                        contentDescription = "View in List",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("View in List", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        viewModel.navigateToCluster(cluster, context)
                                    },
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .height(42.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.NearMe,
                                        contentDescription = "Navigate",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Navigate", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LakaImportExportDialog(
    onDismiss: () -> Unit,
    currentLanguage: String,
    exportLakaLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    importLakaLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>,
    onShareLaka: () -> Unit
) {
    val t = { en: String, si: String -> if (currentLanguage == "si") si else en }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("import_export_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ImportExport,
                        contentDescription = "Sync icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = t("Backup & Restore Data", "දත්ත බැකප් සහ ප්‍රතිසාධනය"),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = t(
                        "Export your coordinate collection to a '.laka' backup file, or restore data from an existing backup file.",
                        "ඔබේ ඛණ්ඩාංක එකතුව '.laka' බැකප් ගොනුවකට අපනයනය කරන්න, නැතහොත් පවතින බැකප් ගොනුවකින් දත්ත ප්‍රතිසාධනය කරන්න."
                    ),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Export Button Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            exportLakaLauncher.launch("locations.laka")
                        }
                        .testTag("export_laka_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Upload,
                                contentDescription = "Export icon",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = t("Export to .laka File", ".laka ගොනුවකට අපනයනය කරන්න"),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = t("Save current coordinate data safely", "පවතින සියලුම දත්ත සුරක්ෂිතව තබාගන්න"),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Share Button Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onShareLaka()
                        }
                        .testTag("share_laka_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.tertiary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = "Share icon",
                                tint = MaterialTheme.colorScheme.onTertiary
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = t("Share .laka Backup File", ".laka ගොනුව Share කරන්න"),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = t("Directly share file to other applications", "මෙම ගොනුව සෘජුවම වෙනත් ඇප්ස් වෙත යොමු කරන්න"),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Import Button Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            importLakaLauncher.launch(arrayOf("*/*"))
                        }
                        .testTag("import_laka_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Download,
                                contentDescription = "Import icon",
                                tint = MaterialTheme.colorScheme.onSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = t("Import from .laka File", ".laka ගොනුවකින් ආනයනය කරන්න"),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = t("Restore coordinates from a backup file", "බැකප් ගොනුවකින් දත්ත නැවත ලබාගන්න"),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(t("Dismiss", "අවලංගු කරන්න"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

fun extractSubArea(clusterCode: String): String {
    val clean = clusterCode.trim().uppercase()
    val parts = clean.split("/", "-")
    return when {
        clean.startsWith("MN/") && parts.size >= 3 -> parts[1]
        clean.startsWith("MN/") && parts.size == 2 -> parts[1]
        parts.size >= 2 && parts[0] != "MN" -> parts[0]
        parts.size >= 2 -> parts[1]
        clean.startsWith("MN/") -> clean.removePrefix("MN/")
        else -> if (clean.isNotBlank()) clean else "OTHER"
    }
}

fun parseClusterCodeParts(input: String): Pair<String, String> {
    val clean = input.trim().uppercase()
    val withoutPrefix = when {
        clean.startsWith("MN/") -> clean.removePrefix("MN/")
        clean.startsWith("MN-") -> clean.removePrefix("MN-")
        clean.startsWith("MN") -> clean.removePrefix("MN").removePrefix("/").removePrefix("-")
        else -> clean
    }
    if (withoutPrefix.isBlank()) return Pair("", "")
    val parts = withoutPrefix.split("/", "-")
    return when {
        parts.size >= 2 -> Pair(parts[0].trim().uppercase(), parts[1].trim().uppercase())
        parts.size == 1 -> Pair(parts[0].trim().uppercase(), "")
        else -> Pair("", "")
    }
}

fun buildClusterCode(subArea: String, number: String): String {
    val sa = subArea.trim().uppercase()
    val num = number.trim().uppercase()
    return when {
        sa.isNotEmpty() && num.isNotEmpty() -> "MN/$sa/$num"
        sa.isNotEmpty() -> "MN/$sa"
        num.isNotEmpty() -> "MN/$num"
        else -> "MN/"
    }
}

@Composable
fun SmartClusterCodeInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    currentLanguage: String = "en",
    compactMode: Boolean = false
) {
    val t = { en: String, si: String -> if (currentLanguage == "si") si else en }

    var subAreaInput by remember { mutableStateOf(parseClusterCodeParts(value).first) }
    var numberInput by remember { mutableStateOf(parseClusterCodeParts(value).second) }

    LaunchedEffect(value) {
        val (s, n) = parseClusterCodeParts(value)
        val currentConstructed = buildClusterCode(subAreaInput, numberInput)
        if (value != currentConstructed) {
            subAreaInput = s
            numberInput = n
        }
    }

    val fullFormattedCode = buildClusterCode(subAreaInput, numberInput)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(if (compactMode) 6.dp else 8.dp)
    ) {
        if (!compactMode) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.QrCode,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = t("Cluster Code (Static Prefix: MN/)", "කාණ්ඩ කේතය (ස්ථාවර උපසර්ගය: MN/)"),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .height(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "MN /",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            OutlinedTextField(
                value = subAreaInput,
                onValueChange = { input ->
                    val upper = input.uppercase().replace(" ", "")
                    subAreaInput = upper
                    val newCode = buildClusterCode(upper, numberInput)
                    onValueChange(newCode)
                },
                label = { Text(t("Sub-Area", "උප ප්‍රදේශය"), fontSize = 11.sp) },
                placeholder = { Text("GD") },
                singleLine = true,
                enabled = enabled,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("sub_area_field"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )

            Text(
                text = "/",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = numberInput,
                onValueChange = { input ->
                    val upper = input.uppercase().replace(" ", "")
                    numberInput = upper
                    val newCode = buildClusterCode(subAreaInput, upper)
                    onValueChange(newCode)
                },
                label = { Text(t("Cluster No.", "කාණ්ඩ අංකය"), fontSize = 11.sp) },
                placeholder = { Text("01") },
                singleLine = true,
                enabled = enabled,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("cluster_number_field"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )
        }

        val popularSubAreas = listOf("GD", "CM", "KL", "GP", "MH", "KG")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = t("Preset:", "පෙරසිටුවම්:"),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            popularSubAreas.forEach { area ->
                FilterChip(
                    selected = subAreaInput == area,
                    onClick = {
                        subAreaInput = area
                        val newCode = buildClusterCode(area, numberInput)
                        onValueChange(newCode)
                    },
                    label = { Text(area, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.height(26.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = if (compactMode) 4.dp else 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Verified,
                        contentDescription = "Code Preview",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = t("Cluster Code:", "කාණ්ඩ කේතය:"),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Text(
                    text = fullFormattedCode.ifEmpty { "MN/" },
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}