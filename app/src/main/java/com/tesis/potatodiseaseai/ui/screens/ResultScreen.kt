package com.tesis.potatodiseaseai.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.tesis.potatodiseaseai.R
import com.tesis.potatodiseaseai.data.database.AppDatabase
import com.tesis.potatodiseaseai.data.model.DiseaseDatabase
import com.tesis.potatodiseaseai.utils.FileUtils
import com.tesis.potatodiseaseai.utils.ImageLoaderConfig
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    imageUri: String,
    disease: String,
    confidence: Float,
    detectionId: Long?,
    onBack: () -> Unit,
    onDeleted: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database = remember { AppDatabase.getDatabase(context) }
    
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    val diseaseName = DiseaseDatabase.getDiseaseName(disease)
    val recommendations = DiseaseDatabase.getRecommendations(disease)
    val isHealthy = disease.lowercase().contains("healthy")
    
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.result_delete_title)) },
            text = { Text(stringResource(R.string.result_delete_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            try {
                                // Eliminar de Room
                                detectionId?.let {
                                    database.detectionDao().deleteById(it)
                                }
                                // Eliminar imagen
                                FileUtils.deleteImage(Uri.parse(imageUri))
                                
                                showDeleteDialog = false
                                onDeleted()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                ) {
                    Text(stringResource(R.string.history_delete_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.history_delete_cancel))
                }
            }
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.result_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, stringResource(R.string.result_back))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Imagen analizada
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(Uri.parse(imageUri))
                            .crossfade(true)
                            .memoryCacheKey(imageUri)
                            .diskCacheKey(imageUri)
                            .build(),
                        contentDescription = stringResource(R.string.cd_analyzed_image),
                        imageLoader = ImageLoaderConfig.getImageLoader(context),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            // Resultado del diagnóstico
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isHealthy) 
                            MaterialTheme.colorScheme.primaryContainer 
                        else 
                            MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isHealthy) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = if (isHealthy) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = diseaseName,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
    text = stringResource(
        R.string.result_confidence,
        String.format("%.1f", confidence * 100)
    ),
    style = MaterialTheme.typography.bodyLarge
)
                        }
                    }
                }
            }

            // Grad-CAM (placeholder)
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.result_gradcam_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.result_gradcam_placeholder),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Recomendaciones
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.result_recommendations_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            items(recommendations) { recommendation ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "•",
                        modifier = Modifier.padding(end = 8.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = recommendation,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            // Botón para eliminar
            item {
                Button(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.result_delete_button))
                }
            }
        }
    }
}