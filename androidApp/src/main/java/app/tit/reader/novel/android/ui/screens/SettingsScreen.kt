package app.tit.reader.novel.android.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import app.tit.reader.novel.android.ui.theme.*
import app.tit.reader.novel.android.worker.ChapterUpdateWorker
import app.tit.shared.repository.AggregatorRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    repository: AggregatorRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isExporting by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isExporting = true
                val jsonContent = repository.createBackupJson(System.currentTimeMillis())
                withContext(Dispatchers.IO) {
                    try {
                        val outputStream: OutputStream? = context.contentResolver.openOutputStream(uri)
                        outputStream?.write(jsonContent.toByteArray(Charsets.UTF_8))
                        outputStream?.flush()
                        outputStream?.close()
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Sao lưu thành công!", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Lỗi sao lưu: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                isExporting = false
            }
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isImporting = true
                withContext(Dispatchers.IO) {
                    try {
                        val inputStream = context.contentResolver.openInputStream(uri)
                        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
                        val jsonString = reader.readText()
                        reader.close()

                        val success = repository.restoreFromBackupJson(jsonString)
                        withContext(Dispatchers.Main) {
                            if (success) {
                                Toast.makeText(context, "Khôi phục dữ liệu thành công!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "File sao lưu không hợp lệ!", Toast.LENGTH_LONG).show()
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Lỗi khôi phục: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                isImporting = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Cài Đặt & Dữ Liệu", fontWeight = FontWeight.Bold, color = TextPrimary)
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgCream)
            )
        },
        containerColor = BgCream,
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Section 1: Backup & Restore
            item {
                SettingsSection(title = "Sao Lưu & Phục Hồi Dữ Liệu") {
                    SettingsItem(
                        icon = Icons.Default.CloudUpload,
                        title = "Xuất file sao lưu (JSON)",
                        subtitle = "Lưu toàn bộ Tủ Sách, Lịch Sử và Cài Đặt ra file",
                        onClick = {
                            val defaultName = "TitReader_Backup_${System.currentTimeMillis()}.json"
                            createDocumentLauncher.launch(defaultName)
                        }
                    )
                    Divider(color = Color(0xFFE8E0D5), thickness = 0.8.dp)
                    SettingsItem(
                        icon = Icons.Default.CloudDownload,
                        title = "Khôi phục dữ liệu (JSON)",
                        subtitle = "Nhập dữ liệu từ file sao lưu đã lưu trước đó",
                        onClick = {
                            openDocumentLauncher.launch(arrayOf("application/json", "*/*"))
                        }
                    )
                }
            }

            // Section 2: Auto-Update Worker
            item {
                SettingsSection(title = "Tự Động Cập Nhật Chương Mới") {
                    SettingsItem(
                        icon = Icons.Default.Sync,
                        title = "Quét chương mới ngay bây giờ",
                        subtitle = "Chạy nền kiểm tra toàn bộ truyện trong Tủ Sách",
                        onClick = {
                            val req = OneTimeWorkRequestBuilder<ChapterUpdateWorker>().build()
                            WorkManager.getInstance(context).enqueue(req)
                            Toast.makeText(context, "Đã kích hoạt quét chương mới ngầm!", Toast.LENGTH_SHORT).show()
                        }
                    )
                    Divider(color = Color(0xFFE8E0D5), thickness = 0.8.dp)
                    SettingsItem(
                        icon = Icons.Default.NotificationsActive,
                        title = "Thông báo Android",
                        subtitle = "Tự động phát thông báo khi có chương mới ra mắt",
                        onClick = {
                            Toast.makeText(context, "Tính năng kiểm tra định kỳ mỗi 6 giờ đang bật", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            // Section 3: App info
            item {
                SettingsSection(title = "Thông Tin Ứng Dụng") {
                    SettingsItem(
                        icon = Icons.Default.Info,
                        title = "Phiên bản Tít Reader",
                        subtitle = "v1.2.0 (Kotlin Multiplatform + Compose)",
                        onClick = {}
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = AccentOrange,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(AccentOrange.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = AccentOrange, modifier = Modifier.size(22.dp))
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(text = subtitle, fontSize = 12.sp, color = MutedGray)
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MutedGray.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp)
        )
    }
}