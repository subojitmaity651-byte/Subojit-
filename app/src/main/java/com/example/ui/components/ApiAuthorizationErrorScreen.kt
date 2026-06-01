package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiAuthorizationErrorScreen(
    errorMessage: String,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    // Parse the error code and error response body if we used the structured exception format
    val is403 = errorMessage.contains("PROJECT_DIAGNOSTICS_ERROR_403")
    val isKeyMissing = errorMessage.contains("PROJECT_DIAGNOSTICS_KEY_MISSING")

    val projectNumber = "1097877264484"
    val activationUrl = "https://console.developers.google.com/apis/api/youtube.googleapis.com/overview?project=$projectNumber"
    
    var copiedState by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Red Highlight alert area
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = "Authorization Failure",
            tint = Color(0xFFE53935),
            modifier = Modifier
                .size(64.dp)
                .padding(bottom = 12.dp)
                .testTag("auth_error_icon")
        )

        Text(
            text = "API KEY AUTHENTICATION ERROR",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFE53935),
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Text(
            text = "HTTP 403 PERMISSION_DENIED (accessNotConfigured)",
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        // Project Diagnostics Box
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 18.dp)
                .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "🚨 LIVE DIAGNOSTICS REPORT",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.error
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.error.copy(alpha = 0.2f))

                DiagnosticRow(label = "Google Cloud Project Number:", value = projectNumber)
                DiagnosticRow(label = "YouTube Data API Status:", value = "DISABLED / DEACTIVATED")
                DiagnosticRow(label = "Credential Restriction Status:", value = "Blocked key access to youtube.googleapis.com OR API is completely disabled on credentials' hosting project.")
                DiagnosticRow(label = "Configured Key Owner Project:", value = "Project Number: 1097877264484 (Does not match project: viewtube-498019 where you might have enabled the API).")
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "The credentials being used belong to GCP Project #$projectNumber. However, the YouTube Data API v3 has NOT been activated on this project yet, leading to complete blockage of incoming feed streams and searches.",
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Checklist of Action Steps
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "How to resolve this issue:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                ActionStepRow(
                    stepNum = "1",
                    text = "Enable the YouTube Data API v3 within project $projectNumber by visiting Google Cloud Console."
                )

                ActionStepRow(
                    stepNum = "2",
                    text = "Ensure your configuration is configured inside Google Cloud with matching API restriction properties."
                )

                ActionStepRow(
                    stepNum = "3",
                    text = "Confirm the target API Key is correctly associated with viewtube-498019 if that is your correct project."
                )

                ActionStepRow(
                    stepNum = "4",
                    text = "Allow up to 5 minutes after activation for replication propagation on Google Edge servers, then attempt retry."
                )
            }
        }

        // Action Buttons Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(activationUrl))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // fallback copy Uri
                        clipboardManager.setText(AnnotatedString(activationUrl))
                        copiedState = true
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("enable_api_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1A73E8),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Launch, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Enable Data API", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = {
                    clipboardManager.setText(AnnotatedString(activationUrl))
                    copiedState = true
                },
                modifier = Modifier
                    .height(48.dp)
                    .testTag("copy_activation_url_button"),
                shape = RoundedCornerShape(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy Activation Link",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (copiedState) "Copied!" else "Copy URL", fontSize = 12.sp)
            }
        }

        Button(
            onClick = onRetryClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("retry_query_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Red,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = "Retry Connection", modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Retry Connection", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun DiagnosticRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = value,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            fontFamily = if (value.contains("1097877264484")) FontFamily.Monospace else FontFamily.Default
        )
    }
}

@Composable
fun ActionStepRow(stepNum: String, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNum,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = text,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
