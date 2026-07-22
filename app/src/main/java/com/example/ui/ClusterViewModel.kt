package com.example.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Cluster
import com.example.data.ClusterRepository
import com.example.BuildConfig
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import org.json.JSONArray
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

sealed interface LocationUiState {
    object Idle : LocationUiState
    object Fetching : LocationUiState
    data class Success(val latitude: Double, val longitude: Double) : LocationUiState
    data class Error(val message: String) : LocationUiState
}

class ClusterViewModel(private val repository: ClusterRepository) : ViewModel() {

    val clustersState: StateFlow<List<Cluster>> = repository.allClusters
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _locationState = MutableStateFlow<LocationUiState>(LocationUiState.Idle)
    val locationState: StateFlow<LocationUiState> = _locationState.asStateFlow()

    private var firebaseAuth: FirebaseAuth? = null
    private var firestore: FirebaseFirestore? = null

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _cloudSyncStatus = MutableStateFlow<String>("Not Signed In")
    val cloudSyncStatus: StateFlow<String> = _cloudSyncStatus.asStateFlow()

    var isSimulatedUser by androidx.compose.runtime.mutableStateOf(false)
        private set

    var pendingImportLakaContent by androidx.compose.runtime.mutableStateOf<String?>(null)

    val userEmail: String
        get() = currentUser.value?.email ?: (if (isSimulatedUser) "demo.user@srilankamaps.lk" else "Not Signed In")

    val userDisplayName: String
        get() = currentUser.value?.displayName ?: (if (isSimulatedUser) "Sri Lankan Map Guide" else "Guest Mode")

    init {
        try {
            firebaseAuth = FirebaseAuth.getInstance()
            firestore = FirebaseFirestore.getInstance()
            _currentUser.value = firebaseAuth?.currentUser
            _cloudSyncStatus.value = if (_currentUser.value != null) "Ready to Sync" else "Sign In Required"
        } catch (e: Exception) {
            _cloudSyncStatus.value = "Firebase Not Initialized"
        }
    }

    fun signInWithGoogleToken(idToken: String, onComplete: (Boolean, String) -> Unit) {
        val auth = firebaseAuth
        if (auth == null) {
            onComplete(false, "Firebase is not configured.")
            return
        }
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    isSimulatedUser = false
                    _currentUser.value = auth.currentUser
                    _cloudSyncStatus.value = "Ready to Sync"
                    syncUserDataToFirestore()
                    onComplete(true, "Successfully signed in!")
                } else {
                    onComplete(false, "Firebase Sign-In failed: ${task.exception?.localizedMessage}")
                }
            }
    }

    fun signInAnonymously(onComplete: (Boolean, String) -> Unit) {
        val auth = firebaseAuth
        if (auth == null) {
            onComplete(false, "Firebase is not configured.")
            return
        }
        _cloudSyncStatus.value = "Signing in anonymously..."
        auth.signInAnonymously()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    isSimulatedUser = false
                    _currentUser.value = auth.currentUser
                    _cloudSyncStatus.value = "Ready to Sync"
                    syncUserDataToFirestore()
                    onComplete(true, "Successfully signed in anonymously!")
                } else {
                    onComplete(false, "Anonymous Sign-In failed: ${task.exception?.localizedMessage}")
                }
            }
    }

    fun enableDemoSession() {
        isSimulatedUser = true
        _currentUser.value = null
        _cloudSyncStatus.value = "Demo Cloud Active"
    }

    fun signOut(onComplete: () -> Unit) {
        try {
            isSimulatedUser = false
            firebaseAuth?.signOut()
            _currentUser.value = null
            _cloudSyncStatus.value = "Sign In Required"
            onComplete()
        } catch (e: Exception) {
            onComplete()
        }
    }

    fun syncUserDataToFirestore() {
        val user = currentUser.value ?: return
        val db = firestore ?: return
        val currentClusters = clustersState.value

        _cloudSyncStatus.value = "Syncing..."

        // 1. Save user profile metadata
        val userDocRef = db.collection("users").document(user.uid)
        val userData = mapOf(
            "uid" to user.uid,
            "email" to (user.email ?: "Anonymous"),
            "displayName" to (user.displayName ?: "Anonymous User"),
            "lastActive" to System.currentTimeMillis(),
            "totalClustersSaved" to currentClusters.size
        )

        userDocRef.set(userData)
            .addOnSuccessListener {
                // 2. Upload all clusters to user's collection in Firestore
                if (currentClusters.isEmpty()) {
                    _cloudSyncStatus.value = "Synced (0 clusters)"
                    return@addOnSuccessListener
                }

                val batch = db.batch()
                currentClusters.forEach { cluster ->
                    val clusterDocRef = db.collection("users").document(user.uid)
                        .collection("clusters").document(cluster.clusterCode)
                    val clusterData = mapOf(
                        "clusterCode" to cluster.clusterCode,
                        "latitude" to cluster.latitude,
                        "longitude" to cluster.longitude,
                        "updatedAt" to cluster.updatedAt
                    )
                    batch.set(clusterDocRef, clusterData)
                }

                batch.commit()
                    .addOnSuccessListener {
                        _cloudSyncStatus.value = "Synced with Cloud"
                    }
                    .addOnFailureListener { e ->
                        _cloudSyncStatus.value = "Cluster Sync Failed: ${e.localizedMessage}"
                    }
            }
            .addOnFailureListener { e ->
                _cloudSyncStatus.value = "Profile Sync Failed: ${e.localizedMessage}"
            }
    }

    fun restoreClustersFromFirestore(onResult: (Boolean, Int, String) -> Unit) {
        val user = currentUser.value
        val db = firestore
        if (user == null || db == null) {
            onResult(false, 0, "Please sign in with Google first.")
            return
        }

        _cloudSyncStatus.value = "Restoring..."
        db.collection("users").document(user.uid)
            .collection("clusters").get()
            .addOnSuccessListener { querySnapshot ->
                var restoredCount = 0
                viewModelScope.launch {
                    querySnapshot.documents.forEach { doc ->
                        val code = doc.getString("clusterCode")
                        val lat = doc.getDouble("latitude")
                        val lng = doc.getDouble("longitude")
                        val updated = doc.getLong("updatedAt") ?: System.currentTimeMillis()

                        if (!code.isNullOrEmpty() && lat != null && lng != null) {
                            val cluster = Cluster(
                                clusterCode = code,
                                latitude = lat,
                                longitude = lng,
                                updatedAt = updated
                            )
                            repository.insert(cluster)
                            restoredCount++
                        }
                    }
                    _cloudSyncStatus.value = "Restored $restoredCount clusters"
                    onResult(true, restoredCount, "Successfully restored $restoredCount clusters from Firestore!")
                }
            }
            .addOnFailureListener { e ->
                _cloudSyncStatus.value = "Restore Failed: ${e.localizedMessage}"
                onResult(false, 0, "Restore failed: ${e.localizedMessage}")
            }
    }

    @SuppressLint("MissingPermission")
    fun fetchGpsLocation(
        context: Context,
        onComplete: (Boolean, Double, Double, String) -> Unit
    ) {
        _locationState.value = LocationUiState.Fetching
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        val cts = CancellationTokenSource()

        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            cts.token
        ).addOnSuccessListener { location ->
            if (location != null) {
                _locationState.value = LocationUiState.Success(location.latitude, location.longitude)
                onComplete(true, location.latitude, location.longitude, "Coordinates fetched successfully.")
            } else {
                // Fallback to last known location
                fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                    if (lastLoc != null) {
                        _locationState.value = LocationUiState.Success(lastLoc.latitude, lastLoc.longitude)
                        onComplete(true, lastLoc.latitude, lastLoc.longitude, "Coordinates fetched from last known location.")
                    } else {
                        val errMsg = "GPS location could not be determined. Please ensure your device's location settings are turned ON."
                        _locationState.value = LocationUiState.Error(errMsg)
                        onComplete(false, 0.0, 0.0, errMsg)
                    }
                }.addOnFailureListener { e ->
                    val errMsg = "GPS location access failed: ${e.localizedMessage}"
                    _locationState.value = LocationUiState.Error(errMsg)
                    onComplete(false, 0.0, 0.0, errMsg)
                }
            }
        }.addOnFailureListener { e ->
            val errMsg = "GPS location access failed: ${e.localizedMessage}"
            _locationState.value = LocationUiState.Error(errMsg)
            onComplete(false, 0.0, 0.0, errMsg)
        }
    }

    fun getClusterByCode(clusterCode: String, onResult: (Cluster?) -> Unit) {
        viewModelScope.launch {
            val cluster = repository.getClusterByCode(clusterCode.trim().uppercase())
            onResult(cluster)
        }
    }

    fun saveClusterDirect(clusterCode: String, latitude: Double, longitude: Double) {
        viewModelScope.launch {
            val cluster = Cluster(
                clusterCode = clusterCode.trim().uppercase(),
                latitude = latitude,
                longitude = longitude,
                updatedAt = System.currentTimeMillis()
            )
            repository.insert(cluster)
        }
    }

    fun exportClustersToJson(): String {
        val list = clustersState.value
        val jsonArray = org.json.JSONArray()
        for (cluster in list) {
            val jsonObj = org.json.JSONObject()
            jsonObj.put("clusterCode", cluster.clusterCode)
            jsonObj.put("latitude", cluster.latitude)
            jsonObj.put("longitude", cluster.longitude)
            jsonObj.put("updatedAt", cluster.updatedAt)
            jsonArray.put(jsonObj)
        }
        return jsonArray.toString(4)
    }

    fun exportClustersToLaka(): String {
        val list = clustersState.value
        val jsonArray = org.json.JSONArray()
        for (cluster in list) {
            val jsonObj = org.json.JSONObject()
            jsonObj.put("clusterCode", cluster.clusterCode)
            jsonObj.put("latitude", cluster.latitude)
            jsonObj.put("longitude", cluster.longitude)
            jsonObj.put("updatedAt", cluster.updatedAt)
            jsonArray.put(jsonObj)
        }
        val rawJson = jsonArray.toString()
        val base64Data = android.util.Base64.encodeToString(rawJson.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
        return "LAKA:$base64Data"
    }

    fun importClustersFromJson(jsonStr: String, onResult: (Boolean, Int, String) -> Unit) {
        viewModelScope.launch {
            try {
                val jsonArray = org.json.JSONArray(jsonStr)
                var count = 0
                var skippedOutsideCount = 0
                for (i in 0 until jsonArray.length()) {
                    val jsonObj = jsonArray.getJSONObject(i)
                    val code = jsonObj.getString("clusterCode").trim().uppercase()
                    val lat = jsonObj.getDouble("latitude")
                    val lng = jsonObj.getDouble("longitude")
                    val updated = if (jsonObj.has("updatedAt")) jsonObj.getLong("updatedAt") else System.currentTimeMillis()

                    if (code.isNotEmpty()) {
                        if (lat in 5.8..10.0 && lng in 79.5..82.2) {
                            val cluster = Cluster(
                                clusterCode = code,
                                latitude = lat,
                                longitude = lng,
                                updatedAt = updated
                            )
                            repository.insert(cluster)
                            count++
                        } else {
                            skippedOutsideCount++
                        }
                    }
                }
                val msg = if (skippedOutsideCount > 0) {
                    "Successfully imported $count clusters! (Skipped $skippedOutsideCount locations outside Sri Lanka)"
                } else {
                    "Successfully imported $count clusters!"
                }
                onResult(true, count, msg)
            } catch (e: Exception) {
                onResult(false, 0, "Failed to parse data: ${e.localizedMessage}")
            }
        }
    }

    fun importClustersFromLaka(lakaStr: String, onResult: (Boolean, Int, String) -> Unit) {
        val trimmed = lakaStr.trim()
        if (!trimmed.startsWith("LAKA:")) {
            onResult(false, 0, "Invalid file format. This file cannot be read by this application.")
            return
        }
        try {
            val base64Part = trimmed.substring(5)
            val decodedBytes = android.util.Base64.decode(base64Part, android.util.Base64.NO_WRAP)
            val decodedJson = String(decodedBytes, Charsets.UTF_8)
            importClustersFromJson(decodedJson, onResult)
        } catch (e: Exception) {
            onResult(false, 0, "Failed to decode locations.laka data: ${e.localizedMessage}")
        }
    }

    fun resolveAndParseGoogleMapsUrl(urlStr: String, onResult: (Boolean, Double, Double, String) -> Unit) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                var finalUrl = urlStr.trim()
                
                // Try direct coordinate parsing first (e.g. "7.8731, 80.7718" or "7.8731 80.7718")
                val cleanedDirectInput = finalUrl.replace(",", " ").split("\\s+".toRegex())
                if (cleanedDirectInput.size == 2) {
                    val lat = cleanedDirectInput[0].toDoubleOrNull()
                    val lng = cleanedDirectInput[1].toDoubleOrNull()
                    if (lat != null && lng != null) {
                        launch(kotlinx.coroutines.Dispatchers.Main) {
                            onResult(true, lat, lng, "Successfully parsed direct coordinates!")
                        }
                        return@launch
                    }
                }

                var resolvedHtmlBody = ""
                // If it's a URL, follow redirects robustly (up to 5 levels) setting a standard User-Agent
                if (finalUrl.startsWith("http://") || finalUrl.startsWith("https://")) {
                    var redirectCount = 0
                    while (redirectCount < 5) {
                        val url = java.net.URL(finalUrl)
                        val connection = url.openConnection() as java.net.HttpURLConnection
                        connection.instanceFollowRedirects = false
                        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        connection.connectTimeout = 10000
                        connection.readTimeout = 10000
                        connection.connect()
                        
                        val responseCode = connection.responseCode
                        if (responseCode in 300..399) {
                            val loc = connection.getHeaderField("Location")
                            if (!loc.isNullOrEmpty()) {
                                finalUrl = loc
                                redirectCount++
                                connection.disconnect()
                                continue
                            }
                        } else if (responseCode == 200) {
                            try {
                                resolvedHtmlBody = connection.inputStream.bufferedReader().use { it.readText() }
                            } catch (e: Exception) {
                                // ignore stream read error
                            }
                        }
                        connection.disconnect()
                        break
                    }
                }

                // Decode URL-encoded parts just in case
                val decodedUrl = try {
                    java.net.URLDecoder.decode(finalUrl, "UTF-8")
                } catch (e: Exception) {
                    finalUrl
                }

                // 1. Try to find the precise marker pin coordinates !3dLat!4dLng (Google Maps specific selected place)
                val pinRegex = """!3d(-?\d+\.\d+)!4d(-?\d+\.\d+)""".toRegex()
                val pinMatch = pinRegex.find(decodedUrl)
                if (pinMatch != null) {
                    val lat = pinMatch.groupValues[1].toDoubleOrNull()
                    val lng = pinMatch.groupValues[2].toDoubleOrNull()
                    if (lat != null && lng != null) {
                        launch(kotlinx.coroutines.Dispatchers.Main) {
                            onResult(true, lat, lng, "Successfully extracted exact marker location!")
                        }
                        return@launch
                    }
                }

                // 2. Try q=Lat,Lng or query=Lat,Lng or center=Lat,Lng or ll=Lat,Lng
                val queryParamsRegex = """(?:[?&]q|[?&]query|[?&]center|[?&]ll)=(-?\d+\.\d+),(-?\d+\.\d+)""".toRegex()
                val queryParamsMatch = queryParamsRegex.find(decodedUrl)
                if (queryParamsMatch != null) {
                    val lat = queryParamsMatch.groupValues[1].toDoubleOrNull()
                    val lng = queryParamsMatch.groupValues[2].toDoubleOrNull()
                    if (lat != null && lng != null) {
                        launch(kotlinx.coroutines.Dispatchers.Main) {
                            onResult(true, lat, lng, "Successfully parsed location query coordinates!")
                        }
                        return@launch
                    }
                }

                // 3. Try camera / view center coordinates @Lat,Lng
                val cameraRegex = """@(-?\d+\.\d+),(-?\d+\.\d+)""".toRegex()
                val cameraMatch = cameraRegex.find(decodedUrl)
                if (cameraMatch != null) {
                    val lat = cameraMatch.groupValues[1].toDoubleOrNull()
                    val lng = cameraMatch.groupValues[2].toDoubleOrNull()
                    if (lat != null && lng != null) {
                        launch(kotlinx.coroutines.Dispatchers.Main) {
                            onResult(true, lat, lng, "Successfully parsed map view camera center!")
                        }
                        return@launch
                    }
                }

                // 4. Try place/Lat,Lng
                val placeRegex = """place/(-?\d+\.\d+),(-?\d+\.\d+)""".toRegex()
                val placeMatch = placeRegex.find(decodedUrl)
                if (placeMatch != null) {
                    val lat = placeMatch.groupValues[1].toDoubleOrNull()
                    val lng = placeMatch.groupValues[2].toDoubleOrNull()
                    if (lat != null && lng != null) {
                        launch(kotlinx.coroutines.Dispatchers.Main) {
                            onResult(true, lat, lng, "Successfully parsed coordinates from place route!")
                        }
                        return@launch
                    }
                }

                // 5. Parse DMS coordinates in URL if present (e.g., 7°51'23.9"N+80°38'53.2"E)
                val dmsRegex = """(\d+)°(\d+)'(\d+(?:\.\d+)?)"([NS])\s*(\d+)°(\d+)'(\d+(?:\.\d+)?)"([EW])""".toRegex()
                val dmsMatch = dmsRegex.find(decodedUrl)
                if (dmsMatch != null) {
                    val latDeg = dmsMatch.groupValues[1].toDouble()
                    val latMin = dmsMatch.groupValues[2].toDouble()
                    val latSec = dmsMatch.groupValues[3].toDouble()
                    val latDir = dmsMatch.groupValues[4]
                    
                    val lngDeg = dmsMatch.groupValues[5].toDouble()
                    val lngMin = dmsMatch.groupValues[6].toDouble()
                    val lngSec = dmsMatch.groupValues[7].toDouble()
                    val lngDir = dmsMatch.groupValues[8]

                    val lat = latDeg + (latMin / 60.0) + (latSec / 3600.0)
                    val finalLat = if (latDir == "S") -lat else lat
                    val lng = lngDeg + (lngMin / 60.0) + (lngSec / 3600.0)
                    val finalLng = if (lngDir == "W") -lng else lng

                    launch(kotlinx.coroutines.Dispatchers.Main) {
                        onResult(true, finalLat, finalLng, "Parsed DMS coordinates from location link!")
                    }
                    return@launch
                }

                // Scan download HTML body for coordinates (OpenGraph metadata or static map links)
                if (resolvedHtmlBody.isNotEmpty()) {
                    // Look for static map URLs in page source: staticmap?center=7.856627%2C80.648119
                    val staticMapRegex = """staticmap\?center=(-?\d+\.\d+)%2C(-?\d+\.\d+)""".toRegex()
                    val staticMapMatch = staticMapRegex.find(resolvedHtmlBody)
                    if (staticMapMatch != null) {
                        val lat = staticMapMatch.groupValues[1].toDoubleOrNull()
                        val lng = staticMapMatch.groupValues[2].toDoubleOrNull()
                        if (lat != null && lng != null) {
                            launch(kotlinx.coroutines.Dispatchers.Main) {
                                onResult(true, lat, lng, "Successfully extracted coordinates from maps webpage!")
                            }
                            return@launch
                        }
                    }

                    // Look for meta descriptions like <meta content="... · 7.856627, 80.648119" name="description">
                    val metaDescRegex = """meta content="[^"]*?·\s*(-?\d+\.\d+)\s*,\s*(-?\d+\.\d+)""".toRegex()
                    val metaDescMatch = metaDescRegex.find(resolvedHtmlBody)
                    if (metaDescMatch != null) {
                        val lat = metaDescMatch.groupValues[1].toDoubleOrNull()
                        val lng = metaDescMatch.groupValues[2].toDoubleOrNull()
                        if (lat != null && lng != null) {
                            launch(kotlinx.coroutines.Dispatchers.Main) {
                                onResult(true, lat, lng, "Successfully extracted coordinates from metadata!")
                            }
                            return@launch
                        }
                    }

                    // Look for general href coordinates like ?q=7.856627,80.648119 or &ll=7.856627,80.648119
                    val hrefCoordsRegex = """(?:q=|ll=)(-?\d+\.\d+),(-?\d+\.\d+)""".toRegex()
                    val hrefCoordsMatch = hrefCoordsRegex.find(resolvedHtmlBody)
                    if (hrefCoordsMatch != null) {
                        val lat = hrefCoordsMatch.groupValues[1].toDoubleOrNull()
                        val lng = hrefCoordsMatch.groupValues[2].toDoubleOrNull()
                        if (lat != null && lng != null) {
                            launch(kotlinx.coroutines.Dispatchers.Main) {
                                onResult(true, lat, lng, "Extracted coordinates from page hyperlinks!")
                            }
                            return@launch
                        }
                    }
                }

                // Last-resort fallback general coordinates regex on the URL string itself
                val generalCoordsRegex = """(-?\d+\.\d+)\s*,\s*(-?\d+\.\d+)""".toRegex()
                val generalMatch = generalCoordsRegex.find(finalUrl)
                if (generalMatch != null) {
                    val lat = generalMatch.groupValues[1].toDoubleOrNull()
                    val lng = generalMatch.groupValues[2].toDoubleOrNull()
                    if (lat != null && lng != null) {
                        launch(kotlinx.coroutines.Dispatchers.Main) {
                            onResult(true, lat, lng, "Parsed coordinates from resolved URL parameters!")
                        }
                        return@launch
                    }
                }

                // Fallback: Call Gemini API with Google Maps Grounding to resolve coordinates!
                callGeminiToResolveLocation(urlStr, onResult)

            } catch (e: Exception) {
                // If resolving redirect fails, fall back to Gemini on the raw query
                callGeminiToResolveLocation(urlStr, onResult)
            }
        }
    }

    private fun callGeminiToResolveLocation(query: String, onResult: (Boolean, Double, Double, String) -> Unit) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    launch(kotlinx.coroutines.Dispatchers.Main) {
                        onResult(false, 0.0, 0.0, "Gemini API key is not configured. Please set GEMINI_API_KEY in the Secrets panel.")
                    }
                    return@launch
                }

                val client = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .build()

                // Construct request JSON with googleMaps grounding enabled
                val prompt = "Find the exact GPS latitude and longitude coordinates for this location or Google Maps link: \"$query\". " +
                        "Use Google Maps/Search grounding to find the actual location coordinates if it is a place name, address, or search URL. " +
                        "Return ONLY a raw JSON object with keys: \"success\" (boolean), \"latitude\" (double), \"longitude\" (double), \"error_message\" (string). " +
                        "Do not wrap in markdown or any code fence blocks."

                val jsonRequest = org.json.JSONObject().apply {
                    put("contents", org.json.JSONArray().put(
                        org.json.JSONObject().put("parts", org.json.JSONArray().put(
                            org.json.JSONObject().put("text", prompt)
                        ))
                    ))
                    // Enable googleMaps tool grounding (Gemini Google Maps Grounding API)
                    put("tools", org.json.JSONArray().put(
                        org.json.JSONObject().put("googleMaps", org.json.JSONObject())
                    ))
                    put("generationConfig", org.json.JSONObject().apply {
                        put("responseMimeType", "application/json")
                    })
                }

                val requestBody = okhttp3.RequestBody.create(
                    "application/json; charset=utf-8".toMediaTypeOrNull(),
                    jsonRequest.toString()
                )

                val request = okhttp3.Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        launch(kotlinx.coroutines.Dispatchers.Main) {
                            onResult(false, 0.0, 0.0, "Gemini API request failed with code ${response.code}")
                        }
                        return@launch
                    }

                    val responseBody = response.body?.string() ?: ""
                    val jsonResponse = org.json.JSONObject(responseBody)
                    val candidates = jsonResponse.optJSONArray("candidates")
                    if (candidates == null || candidates.length() == 0) {
                        launch(kotlinx.coroutines.Dispatchers.Main) {
                            onResult(false, 0.0, 0.0, "No result returned from Google Maps grounding sync.")
                        }
                        return@launch
                    }

                    val text = candidates
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")

                    val resultJson = org.json.JSONObject(text.trim())
                    val success = resultJson.optBoolean("success", false)
                    val lat = resultJson.optDouble("latitude", 0.0)
                    val lng = resultJson.optDouble("longitude", 0.0)
                    val errMsg = resultJson.optString("error_message", "Could not resolve coordinates")

                    launch(kotlinx.coroutines.Dispatchers.Main) {
                        if (success && lat != 0.0 && lng != 0.0) {
                            if (lat in 5.8..10.0 && lng in 79.5..82.2) {
                                onResult(true, lat, lng, "Successfully synced coordinates using Google Maps grounding!")
                            } else {
                                onResult(false, 0.0, 0.0, "The resolved location (${String.format("%.4f", lat)}, ${String.format("%.4f", lng)}) is outside Sri Lanka. Only Sri Lankan locations are supported in this app.")
                            }
                        } else {
                            onResult(false, 0.0, 0.0, errMsg.ifEmpty { "Failed to resolve coordinates using Google Maps grounding." })
                        }
                    }
                }
            } catch (e: Exception) {
                launch(kotlinx.coroutines.Dispatchers.Main) {
                    onResult(false, 0.0, 0.0, "Google Maps Sync Error: ${e.localizedMessage}")
                }
            }
        }
    }

    fun deleteCluster(cluster: Cluster) {
        viewModelScope.launch {
            repository.delete(cluster)
        }
    }

    fun navigateToCluster(cluster: Cluster, context: Context) {
        val uriString = "google.navigation:q=${cluster.latitude},${cluster.longitude}"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString)).apply {
            setPackage("com.google.android.apps.maps")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to geo URI
            val fallbackUri = "geo:${cluster.latitude},${cluster.longitude}?q=${cluster.latitude},${cluster.longitude}"
            val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUri)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(fallbackIntent)
            } catch (e2: Exception) {
                // Fallback to web browser Google Maps
                val webUri = "https://www.google.com/maps/search/?api=1&query=${cluster.latitude},${cluster.longitude}"
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUri)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    context.startActivity(webIntent)
                } catch (e3: Exception) {
                    // Ignore or handle
                }
            }
        }
    }

    fun resetLocationState() {
        _locationState.value = LocationUiState.Idle
    }

    fun updateClusterCode(oldCluster: Cluster, newCode: String, onResult: (Boolean, String) -> Unit) {
        val trimmedNewCode = newCode.trim().uppercase()
        if (trimmedNewCode.isEmpty()) {
            onResult(false, "Cluster code cannot be empty")
            return
        }
        viewModelScope.launch {
            try {
                val existing = repository.getClusterByCode(trimmedNewCode)
                if (existing != null && existing.clusterCode != oldCluster.clusterCode) {
                    onResult(false, "Cluster code already exists")
                    return@launch
                }
                
                // Delete old one and insert new one
                repository.delete(oldCluster)
                val newCluster = Cluster(
                    clusterCode = trimmedNewCode,
                    latitude = oldCluster.latitude,
                    longitude = oldCluster.longitude,
                    updatedAt = System.currentTimeMillis()
                )
                repository.insert(newCluster)
                onResult(true, "Cluster code updated successfully")
            } catch (e: Exception) {
                onResult(false, "Error updating cluster code: ${e.localizedMessage}")
            }
        }
    }
}

class ClusterViewModelFactory(private val repository: ClusterRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ClusterViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ClusterViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
