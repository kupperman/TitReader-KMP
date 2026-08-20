package app.tit.reader.novel.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tit.content.core.model.ContentType
import app.tit.reader.novel.android.ui.theme.AccentOrange
import app.tit.reader.novel.android.ui.theme.TextPrimary
import app.tit.shared.model.MangaReadingMode
import app.tit.shared.model.NovelThemeType
import app.tit.shared.model.ReaderSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderSettingsSheet(
    contentType: ContentType,
    currentSettings: ReaderSettings,
    onSettingsChanged: (ReaderSettings) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFFFAF6EE),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Tùy Chỉnh Trình Đọc",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (contentType == ContentType.NOVEL) {
                // --- NOVEL SETTINGS ---

                // 1. Theme Selection
                Text(
                    text = "Màu nền",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val themes = listOf(
                        Triple(NovelThemeType.LIGHT, Color(0xFFFAF6EE), "Sáng"),
                        Triple(NovelThemeType.SEPIA, Color(0xFFEEE4CC), "Giấy ngà"),
                        Triple(NovelThemeType.DARK, Color(0xFF1E293B), "Tối"),
                        Triple(NovelThemeType.AMOLED, Color(0xFF000000), "AMOLED")
                    )

                    themes.forEach { (themeType, bgColor, name) ->
                        val isSelected = currentSettings.novelTheme == themeType
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onSettingsChanged(currentSettings.copy(novelTheme = themeType)) }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(bgColor)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) AccentOrange else Color.LightGray,
                                        shape = CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = name,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = TextPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 2. Font Size
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Cỡ chữ", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text(text = "${currentSettings.novelFontSize.toInt()} sp", fontSize = 13.sp, color = AccentOrange, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = currentSettings.novelFontSize,
                    onValueChange = { onSettingsChanged(currentSettings.copy(novelFontSize = it)) },
                    valueRange = 14f..30f,
                    steps = 8,
                    colors = SliderDefaults.colors(thumbColor = AccentOrange, activeTrackColor = AccentOrange)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Font Family
                Text(text = "Kiểu chữ (Font)", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val fonts = listOf(
                        "DEFAULT" to "Mặc định",
                        "SERIF" to "Sách in (Serif)",
                        "MONOSPACE" to "Máy tính",
                        "CURSIVE" to "Viết tay"
                    )
                    fonts.forEach { (fontKey, label) ->
                        val isSelected = currentSettings.novelFontFamily == fontKey
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) AccentOrange else Color(0xFFE8E0D5))
                                .clickable { onSettingsChanged(currentSettings.copy(novelFontFamily = fontKey)) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else TextPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 4. Line Height Multiplier
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Khoảng cách dòng", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text(text = "${"%.1f".format(currentSettings.novelLineHeightMultiplier)}x", fontSize = 13.sp, color = AccentOrange, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = currentSettings.novelLineHeightMultiplier,
                    onValueChange = { onSettingsChanged(currentSettings.copy(novelLineHeightMultiplier = it)) },
                    valueRange = 1.2f..2.2f,
                    steps = 5,
                    colors = SliderDefaults.colors(thumbColor = AccentOrange, activeTrackColor = AccentOrange)
                )

            } else {
                // --- MANGA SETTINGS ---
                Text(
                    text = "Chế độ đọc truyện tranh",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))

                MangaReadingMode.values().forEach { mode ->
                    val isSelected = currentSettings.mangaMode == mode
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFFFFF0EB) else Color(0xFFE8E0D5)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onSettingsChanged(currentSettings.copy(mangaMode = mode)) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { onSettingsChanged(currentSettings.copy(mangaMode = mode)) },
                                colors = RadioButtonDefaults.colors(selectedColor = AccentOrange)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = mode.title,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}