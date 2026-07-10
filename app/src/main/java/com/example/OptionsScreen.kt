package com.example

import android.content.Context
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.ScoreEntity
import java.util.Locale
import kotlinx.coroutines.launch
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.geometry.Offset

@Composable
fun OptionsScreen(
  speed: Float,
  pingleFriction: Float,
  isManualUnlocked: Boolean,
  pingleTintId: String,
  pingleCustomColorInt: Int,
  totalSpinDuration: Long,
  foldAngleThreshold: Float,
  useCustomImage: Boolean,
  pingleCustomImageUri: String?,
  isDebugUnlocked: Boolean,
  easterRainbowNeon: Boolean,
  easterMatrixBg: Boolean,
  easterReverseSpin: Boolean,
  easterSpaceStars: Boolean,
  isRainbowNeonUnlocked: Boolean,
  isMatrixBgUnlocked: Boolean,
  isReverseSpinUnlocked: Boolean,
  isSpaceStarsUnlocked: Boolean,
  onSpeedChange: (Float) -> Unit,
  onFrictionChange: (Float) -> Unit,
  onTintSelect: (String) -> Unit,
  onCustomColorChange: (Int) -> Unit,
  onFoldAngleThresholdChange: (Float) -> Unit,
  onCustomImageUriChange: (String?) -> Unit,
  onUseCustomImageChange: (Boolean) -> Unit,
  onEasterRainbowNeonChange: (Boolean) -> Unit,
  onEasterMatrixBgChange: (Boolean) -> Unit,
  onEasterReverseSpinChange: (Boolean) -> Unit,
  onEasterSpaceStarsChange: (Boolean) -> Unit,
  onCreateDebugScore: (Long) -> Unit,
  onBackClicked: () -> Unit
) {
  val context = LocalContext.current
  val viewModel: PingleViewModel = viewModel()
  val scope = rememberCoroutineScope()
  val easterSpaceStarsState by viewModel.easterSpaceStars.collectAsState()
  val easterMatrixBgState by viewModel.easterMatrixBg.collectAsState()
  val loadEverythingState by viewModel.loadEverything.collectAsState()
  val buttonVibrationMode by viewModel.buttonVibrationMode.collectAsState()
  val spinVibrationMode by viewModel.spinVibrationMode.collectAsState()
  val spinVibrationInterval by viewModel.spinVibrationInterval.collectAsState()

  val visibleTabs = remember {
    listOf("customisation", "special", "easter eggs", "debug", "request features")
  }
  var passwordInput by remember { mutableStateOf(if (isDebugUnlocked) "tetopearbro" else "") }
  var codeInputText by remember { mutableStateOf("") }
  var blankCount by remember { mutableIntStateOf(0) }
  LaunchedEffect(isDebugUnlocked) {
    if (isDebugUnlocked) {
      if (passwordInput != "tetopearbro") {
        passwordInput = "tetopearbro"
      }
    } else {
      passwordInput = ""
    }
  }
  val pagerState = rememberPagerState(
    initialPage = 0,
    pageCount = { visibleTabs.size }
  )
  val selectedTabIdx = pagerState.currentPage

  val density = LocalDensity.current
  val pagerOffset: Float = pagerState.currentPage + pagerState.currentPageOffsetFraction
  val headerScrollState = rememberScrollState()
  var colorPickerMode by remember { mutableStateOf("wheel") }

  LaunchedEffect(pagerOffset) {
    val scrollTargetPx = with(density) { (pagerOffset * 80.dp.toPx()).toInt() }
    headerScrollState.scrollTo(scrollTargetPx)
  }

  // Helper to extract file name from Uri
  fun getFileName(context: android.content.Context, uri: android.net.Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
      val cursor = context.contentResolver.query(uri, null, null, null, null)
      try {
        if (cursor != null && cursor.moveToFirst()) {
          val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
          if (index != -1) {
            result = cursor.getString(index)
          }
        }
      } finally {
        cursor?.close()
      }
    }
    if (result == null) {
      result = uri.path
      val cut = result?.lastIndexOf('/')
      if (cut != null && cut != -1) {
        result = result.substring(cut + 1)
      }
    }
    return result
  }

  // Custom Image/Video/File picker contract
  val imagePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri: android.net.Uri? ->
    if (uri != null) {
      try {
        val contentResolver = context.contentResolver
        val mimeType = contentResolver.getType(uri) ?: ""
        val fileName = getFileName(context, uri) ?: "file"
        val extension = fileName.substringAfterLast('.', "").lowercase()

        val isImage = mimeType.startsWith("image/") || listOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic").contains(extension)
        val isVideo = mimeType.startsWith("video/") || listOf("mp4", "mkv", "webm", "3gp", "mov", "avi").contains(extension)

        val mediaType = if (isImage) "image" else if (isVideo) "video" else "unrecognized"
        val resolvedExt = if (extension.isNotEmpty()) extension else {
          if (isImage) "png" else if (isVideo) "mp4" else "bin"
        }

        val inputStream = contentResolver.openInputStream(uri)
        if (inputStream != null) {
          val file = java.io.File(context.filesDir, "custom_pingle_media.$resolvedExt")
          file.outputStream().use { outputStream ->
            inputStream.use { it.copyTo(outputStream) }
          }
          onCustomImageUriChange(file.absolutePath)
          viewModel.setCustomMediaType(mediaType)
          onUseCustomImageChange(true)

          if (mediaType == "image") {
            showToast(context, "Custom image updated successfully!")
          } else if (mediaType == "video") {
            showToast(context, "Custom video updated successfully!")
          } else {
            showToast(context, "File format not recognized. Displaying Question Mark Pingle!")
          }
        }
      } catch (e: Exception) {
        e.printStackTrace()
        onCustomImageUriChange(uri.toString())
        viewModel.setCustomMediaType("unrecognized")
        onUseCustomImageChange(true)
        showToast(context, "File chosen. Displaying Question Mark Pingle!")
      }
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .pingleBackground(easterSpaceStarsState, easterMatrixBgState)
      .statusBarsPadding()
      .navigationBarsPadding()
      .testTag("options_screen")
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
      // 1. Tiny Windows Phone style section header
      PingleText(
        text = "PINGLESPIN SETTINGS",
        style = TextStyle(
          fontFamily = FontFamily.SansSerif,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          color = Color.White.copy(alpha = 0.6f),
          letterSpacing = 2.5.sp
        ),
        modifier = Modifier
          .graphicsLayer {
            translationX = -pagerOffset * 60f
          }
          .padding(bottom = 8.dp)
      )

      // 2. Horizontal pivot/panorama header list
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 20.dp)
          .horizontalScroll(headerScrollState),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.Bottom
      ) {
        visibleTabs.forEachIndexed { idx, tab ->
          val isSelected = (idx == selectedTabIdx)
          val alpha = if (isSelected) 1f else 0.4f
          val scale = if (isSelected) 32.sp else 20.sp
          val fontWeight = if (isSelected) FontWeight.Light else FontWeight.ExtraLight

          PingleText(
            text = tab,
            style = TextStyle(
              fontFamily = FontFamily.SansSerif,
              fontSize = scale,
              fontWeight = fontWeight,
              color = Color.White.copy(alpha = alpha)
            ),
            modifier = Modifier
              .clickable {
                scope.launch {
                  pagerState.animateScrollToPage(idx)
                }
              }
              .padding(vertical = 4.dp)
          )
        }
      }

      // 3. Page Content based on selected tab
      HorizontalPager(
        state = pagerState,
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
      ) { pageIdx ->
        val activeTab = visibleTabs.getOrElse(pageIdx) { "" }
        when (activeTab) {
          "customisation" -> {
            Column(
              modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
              verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
              // Custom Pingle Media replacement
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                  .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
                  .padding(16.dp)
              ) {
                PingleText(
                  text = "CUSTOM PINGLE MEDIA",
                  style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 1.sp
                  ),
                  modifier = Modifier.padding(bottom = 4.dp)
                )

                PingleText(
                  text = "Supports images, videos, and other formats. Unrecognized formats will show a ? pingle.",
                  style = TextStyle(
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp
                  ),
                  modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(16.dp),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  // Thumbnail preview
                  Box(
                    modifier = Modifier
                      .size(60.dp)
                      .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                      .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                  ) {
                    PingleImage(
                      useCustomImage = useCustomImage,
                      customImageUri = pingleCustomImageUri,
                      modifier = Modifier.size(48.dp)
                    )
                  }

                  Column(modifier = Modifier.weight(1f)) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                      androidx.compose.material3.Checkbox(
                        checked = useCustomImage,
                        onCheckedChange = { onUseCustomImageChange(it) },
                        colors = androidx.compose.material3.CheckboxDefaults.colors(
                          checkedColor = Color(0xFF0078D7),
                          uncheckedColor = Color.White.copy(alpha = 0.5f)
                        )
                      )
                      PingleText(
                        text = "Use custom media",
                        style = TextStyle(color = Color.White, fontSize = 13.sp)
                      )
                    }

                    Box(
                      modifier = Modifier
                        .border(1.dp, Color(0xFF0078D7), RoundedCornerShape(8.dp))
                        .clickable { imagePickerLauncher.launch("*/*") }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                      PingleText(
                        text = "SELECT MEDIA",
                        style = TextStyle(
                          color = Color(0xFF0078D7),
                          fontSize = 11.sp,
                          fontWeight = FontWeight.Bold
                        )
                      )
                    }
                  }
                }
              }

              // PINGUI Layout customisation row
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                  .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
                  .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  PingleText(
                    text = "PINGUI",
                    style = TextStyle(
                      fontFamily = FontFamily.SansSerif,
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Bold,
                      color = Color.White,
                      letterSpacing = 1.sp
                    )
                  )
                  Spacer(modifier = Modifier.height(2.dp))
                  PingleText(
                    text = "Adjust sizes, locations & tilts",
                    style = TextStyle(
                      fontFamily = FontFamily.SansSerif,
                      fontSize = 11.sp,
                      color = Color.White.copy(alpha = 0.5f)
                    )
                  )
                }

                Box(
                  modifier = Modifier
                    .border(1.dp, Color(0xFF0078D7), RoundedCornerShape(8.dp))
                    .clickable { viewModel.setScreen(Screen.PINGUI_SETUP) }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .testTag("pingui_edit_button")
                ) {
                  PingleText(
                    text = "EDIT",
                    style = TextStyle(
                      color = Color(0xFF0078D7),
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold,
                      letterSpacing = 1.sp
                    )
                  )
                }
              }

              // Speed slider
              Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  PingleText(
                    text = "SPIN MULTIPLIER",
                    style = TextStyle(
                      fontFamily = FontFamily.SansSerif,
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Bold,
                      color = Color.White,
                      letterSpacing = 1.sp
                    )
                  )
                  PingleText(
                    text = String.format(Locale.US, "%.1fx", speed),
                    style = TextStyle(
                      fontFamily = FontFamily.Monospace,
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Bold,
                      color = Color(0xFF0078D7)
                    )
                  )
                }
                Slider(
                  value = speed,
                  onValueChange = onSpeedChange,
                  valueRange = 0.5f..10.0f,
                  colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF0078D7),
                    activeTrackColor = Color(0xFF0078D7),
                    inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                  )
                )
              }

              // Friction slider (only visible if manual spin mode unlocked)
              if (isManualUnlocked) {
                Column(modifier = Modifier.fillMaxWidth()) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    PingleText(
                      text = "MANUAL DECAY (FRICTION)",
                      style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 1.sp
                      )
                    )
                    PingleText(
                      text = String.format(Locale.US, "%.0f%%", pingleFriction),
                      style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0078D7)
                      )
                    )
                  }
                  Slider(
                    value = pingleFriction,
                    onValueChange = onFrictionChange,
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                      thumbColor = Color(0xFF0078D7),
                      activeTrackColor = Color(0xFF0078D7),
                      inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                    )
                  )
                }
              }

              // Vibration & Haptics Section
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                  .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
                  .padding(16.dp)
              ) {
                PingleText(
                  text = "VIBRATION & HAPTICS",
                  style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 1.sp
                  ),
                  modifier = Modifier.padding(bottom = 12.dp)
                )

                // Button Vibration Selector
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
                  PingleText(
                    text = "BUTTON PRESS VIBRATION",
                    style = TextStyle(
                      fontFamily = FontFamily.SansSerif,
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold,
                      color = Color.White.copy(alpha = 0.6f),
                      letterSpacing = 0.5.sp
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                  )
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    val modes = listOf("none" to "OFF", "light" to "SOFT", "medium" to "MEDIUM", "strong" to "STRONG")
                    modes.forEach { (modeId, label) ->
                      val isSelected = buttonVibrationMode == modeId
                      Box(
                        modifier = Modifier
                          .weight(1f)
                          .height(36.dp)
                          .border(
                            width = 1.dp,
                            color = if (isSelected) Color(0xFF0078D7) else Color.White.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                          )
                          .background(
                            if (isSelected) Color(0xFF0078D7).copy(alpha = 0.15f) else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                          )
                          .clickable {
                            viewModel.setButtonVibrationMode(modeId)
                            VibrationHelper.vibrate(context, modeId)
                          },
                        contentAlignment = Alignment.Center
                      ) {
                        PingleText(
                          text = label,
                          style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color(0xFF0078D7) else Color.White.copy(alpha = 0.7f)
                          )
                        )
                      }
                    }
                  }
                }

                if (isManualUnlocked) {
                  // Spin Vibration Selector
                  Column(modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
                    PingleText(
                      text = "PRINGLE SPIN VIBRATION",
                      style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.6f),
                        letterSpacing = 0.5.sp
                      ),
                      modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                      val modes = listOf("none" to "OFF", "light" to "SOFT", "medium" to "MEDIUM", "strong" to "STRONG")
                      modes.forEach { (modeId, label) ->
                        val isSelected = spinVibrationMode == modeId
                        Box(
                          modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .border(
                              width = 1.dp,
                              color = if (isSelected) Color(0xFF0078D7) else Color.White.copy(alpha = 0.15f),
                              shape = RoundedCornerShape(8.dp)
                            )
                            .background(
                              if (isSelected) Color(0xFF0078D7).copy(alpha = 0.15f) else Color.Transparent,
                              shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                              viewModel.setSpinVibrationMode(modeId)
                              VibrationHelper.vibrate(context, modeId)
                            },
                          contentAlignment = Alignment.Center
                        ) {
                          PingleText(
                            text = label,
                            style = TextStyle(
                              fontFamily = FontFamily.SansSerif,
                              fontSize = 11.sp,
                              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                              color = if (isSelected) Color(0xFF0078D7) else Color.White.copy(alpha = 0.7f)
                            )
                          )
                        }
                      }
                    }
                  }

                  // Spin Vibration Interval
                  if (spinVibrationMode != "none") {
                    Column(modifier = Modifier.fillMaxWidth()) {
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        PingleText(
                          text = "SPIN VIBRATE INTERVAL",
                          style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.6f),
                            letterSpacing = 0.5.sp
                          )
                        )
                        PingleText(
                          text = String.format(Locale.US, "Every %.1f°", spinVibrationInterval),
                          style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0078D7)
                          )
                        )
                      }
                      Slider(
                        value = spinVibrationInterval,
                        onValueChange = { viewModel.setSpinVibrationInterval(it) },
                        valueRange = 5.0f..90.0f,
                        colors = SliderDefaults.colors(
                          thumbColor = Color(0xFF0078D7),
                          activeTrackColor = Color(0xFF0078D7),
                          inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                        )
                      )
                    }
                  }
                }
              }

              // Color Tint Selector Row
              Column(modifier = Modifier.fillMaxWidth()) {
                PingleText(
                  text = "PINGLE COLOR TINT",
                  style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 1.sp
                  ),
                  modifier = Modifier.padding(bottom = 12.dp)
                )

                // Row of pre-configured colors
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  PINGLE_TINTS.forEach { option ->
                    val isSelected = (pingleTintId == option.id)
                    Box(
                      modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(option.color ?: Color.DarkGray)
                        .border(
                          width = if (isSelected) 2.5.dp else 1.dp,
                          color = if (isSelected) Color.White else Color.White.copy(alpha = 0.15f),
                          shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onTintSelect(option.id) },
                      contentAlignment = Alignment.Center
                    ) {
                      if (option.id == "none") {
                        PingleText(
                          text = "∅",
                          style = TextStyle(color = Color.White, fontSize = 14.sp)
                        )
                      }
                    }
                  }

                  // Custom Color block
                  val hoursUnlocked = totalSpinDuration / 3600000L
                  val isCustomColorUnlocked = (hoursUnlocked >= 5) || isManualUnlocked
                  val isSelectedCustom = (pingleTintId == "custom")

                  Box(
                    modifier = Modifier
                      .size(36.dp)
                      .clip(RoundedCornerShape(8.dp))
                      .background(
                        if (isCustomColorUnlocked) {
                          Brush.sweepGradient(
                            listOf(
                              Color(0xFFFF3B30),
                              Color(0xFFFF9500),
                              Color(0xFFFFCC00),
                              Color(0xFF34C759),
                              Color(0xFF5AC8FA),
                              Color(0xFF007AFF),
                              Color(0xFFA252FF),
                              Color(0xFFFF3B30)
                            )
                          )
                        } else {
                          Brush.linearGradient(listOf(Color.Gray, Color.DarkGray))
                        }
                      )
                      .border(
                        width = if (isSelectedCustom) 2.5.dp else 1.dp,
                        color = if (isSelectedCustom) Color.White else Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                      )
                      .clickable {
                        if (isCustomColorUnlocked) {
                          onTintSelect("custom")
                        } else {
                          showToast(context, "Requires 5 hours total spin time!")
                        }
                      },
                    contentAlignment = Alignment.Center
                  ) {
                    if (!isCustomColorUnlocked) {
                      Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked custom color",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                      )
                    } else {
                      // Center indicator circle of actual chosen custom color
                      Box(
                        modifier = Modifier
                          .size(14.dp)
                          .clip(androidx.compose.foundation.shape.CircleShape)
                          .background(Color(pingleCustomColorInt))
                          .border(1.dp, Color.White.copy(alpha = 0.5f), androidx.compose.foundation.shape.CircleShape)
                      )
                      if (isSelectedCustom) {
                        Icon(
                          imageVector = Icons.Default.Check,
                          contentDescription = "Selected",
                          tint = Color.White,
                          modifier = Modifier.size(16.dp)
                        )
                      }
                    }
                  }
                }

                // If Custom is selected and unlocked, show the chosen color picker view
                val hoursUnlocked = totalSpinDuration / 3600000L
                val isCustomColorUnlocked = (hoursUnlocked >= 5) || isManualUnlocked
                if (pingleTintId == "custom" && isCustomColorUnlocked) {
                  Spacer(modifier = Modifier.height(16.dp))
                  
                  when (colorPickerMode) {
                    "wheel" -> {
                      HSVColorPicker(
                        pingleCustomColorInt = pingleCustomColorInt,
                        onCustomColorChange = onCustomColorChange
                      )
                      
                      Spacer(modifier = Modifier.height(8.dp))
                      
                      Button(
                        onClick = { colorPickerMode = "rgb" },
                        colors = ButtonDefaults.buttonColors(
                          containerColor = Color.Transparent,
                          contentColor = Color(0xFF0078D7)
                        ),
                        modifier = Modifier
                          .fillMaxWidth()
                          .border(1.dp, Color(0xFF0078D7).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                          .height(44.dp)
                      ) {
                        PingleText(
                          text = "ADVANCED",
                          style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                          )
                        )
                      }
                    }
                    
                    "rgb" -> {
                      val redVal = (pingleCustomColorInt shr 16 and 0xFF).toFloat()
                      val greenVal = (pingleCustomColorInt shr 8 and 0xFF).toFloat()
                      val blueVal = (pingleCustomColorInt and 0xFF).toFloat()

                      FancyRGBSlider(
                        label = "R",
                        value = redVal,
                        gradient = Brush.horizontalGradient(listOf(Color.Black, Color.Red)),
                        thumbColor = Color.Red,
                        onValueChange = { r ->
                          val updated = (0xFF shl 24) or (r.toInt() shl 16) or (greenVal.toInt() shl 8) or blueVal.toInt()
                          onCustomColorChange(updated)
                        },
                        valueText = String.format(Locale.US, "%d", redVal.toInt())
                      )

                      FancyRGBSlider(
                        label = "G",
                        value = greenVal,
                        gradient = Brush.horizontalGradient(listOf(Color.Black, Color.Green)),
                        thumbColor = Color.Green,
                        onValueChange = { g ->
                          val updated = (0xFF shl 24) or (redVal.toInt() shl 16) or (g.toInt() shl 8) or blueVal.toInt()
                          onCustomColorChange(updated)
                        },
                        valueText = String.format(Locale.US, "%d", greenVal.toInt())
                      )

                      FancyRGBSlider(
                        label = "B",
                        value = blueVal,
                        gradient = Brush.horizontalGradient(listOf(Color.Black, Color.Blue)),
                        thumbColor = Color.Blue,
                        onValueChange = { b ->
                          val updated = (0xFF shl 24) or (redVal.toInt() shl 16) or (greenVal.toInt() shl 8) or b.toInt()
                          onCustomColorChange(updated)
                        },
                        valueText = String.format(Locale.US, "%d", blueVal.toInt())
                      )
                      
                      Spacer(modifier = Modifier.height(12.dp))
                      
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                      ) {
                        Button(
                          onClick = { colorPickerMode = "hex" },
                          colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color(0xFF0078D7)
                          ),
                          modifier = Modifier
                            .weight(1f)
                            .border(1.dp, Color(0xFF0078D7).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .height(44.dp),
                          contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                          PingleText(
                            text = "GIMME THE NERD ONE",
                            style = TextStyle(
                              fontFamily = FontFamily.SansSerif,
                              fontSize = 10.sp,
                              fontWeight = FontWeight.Bold,
                              letterSpacing = 0.5.sp
                            )
                          )
                        }
                        
                        Button(
                          onClick = { colorPickerMode = "wheel" },
                          colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color.White.copy(alpha = 0.6f)
                          ),
                          modifier = Modifier
                            .weight(1f)
                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .height(44.dp)
                        ) {
                          PingleText(
                            text = "BACK TO WHEEL",
                            style = TextStyle(
                              fontFamily = FontFamily.SansSerif,
                              fontSize = 10.sp,
                              fontWeight = FontWeight.Bold,
                              letterSpacing = 0.5.sp
                            )
                          )
                        }
                      }
                    }
                    
                    "hex" -> {
                      var hexInputText by remember(pingleCustomColorInt) {
                        mutableStateOf(String.format(Locale.US, "%06X", pingleCustomColorInt and 0xFFFFFF))
                      }
                      var parseError by remember { mutableStateOf(false) }
                      
                      Column(
                        modifier = Modifier
                          .fillMaxWidth()
                          .padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                      ) {
                        Row(
                          modifier = Modifier.fillMaxWidth(),
                          verticalAlignment = Alignment.CenterVertically,
                          horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                          Box(
                            modifier = Modifier
                              .size(40.dp)
                              .clip(RoundedCornerShape(8.dp))
                              .background(Color(pingleCustomColorInt))
                              .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                          )
                          
                          OutlinedTextField(
                            value = hexInputText,
                            onValueChange = { input ->
                              val filtered = input.uppercase(Locale.US).filter { it.isLetterOrDigit() }.take(8)
                              hexInputText = filtered
                              parseError = false
                            },
                            prefix = { Text("#", color = Color.White.copy(alpha = 0.5f)) },
                            placeholder = { Text("RRGGBB", color = Color.White.copy(alpha = 0.3f)) },
                            singleLine = true,
                            isError = parseError,
                            colors = OutlinedTextFieldDefaults.colors(
                              focusedTextColor = Color.White,
                              unfocusedTextColor = Color.White,
                              focusedBorderColor = Color(0xFF0078D7),
                              unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                              errorBorderColor = Color.Red,
                              cursorColor = Color(0xFF0078D7)
                            ),
                            textStyle = TextStyle(
                              fontFamily = FontFamily.Monospace,
                              fontSize = 14.sp,
                              fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.weight(1f)
                          )
                        }
                        
                        if (parseError) {
                          PingleText(
                            text = "Invalid hex code! Must be 6 or 8 digits (AARRGGBB).",
                            style = TextStyle(color = Color.Red, fontSize = 11.sp)
                          )
                        }
                        
                        Row(
                          modifier = Modifier.fillMaxWidth(),
                          horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                          Button(
                            onClick = {
                              val clean = hexInputText.trim()
                              val parsed = try {
                                if (clean.length == 6) {
                                  (0xFF shl 24) or clean.toLong(16).toInt()
                                } else if (clean.length == 8) {
                                  clean.toLong(16).toInt()
                                } else {
                                  null
                                }
                              } catch (e: Exception) {
                                null
                              }
                              if (parsed != null) {
                                onCustomColorChange(parsed)
                                showToast(context, "Hex color applied successfully!")
                              } else {
                                parseError = true
                              }
                            },
                            colors = ButtonDefaults.buttonColors(
                              containerColor = Color(0xFF0078D7),
                              contentColor = Color.White
                            ),
                            modifier = Modifier
                              .weight(1f)
                              .height(44.dp)
                          ) {
                            PingleText(
                              text = "APPLY HEX",
                              style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                              )
                            )
                          }
                          
                          Button(
                            onClick = { colorPickerMode = "rgb" },
                            colors = ButtonDefaults.buttonColors(
                              containerColor = Color.Transparent,
                              contentColor = Color.White.copy(alpha = 0.6f)
                            ),
                            modifier = Modifier
                              .weight(1f)
                              .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                              .height(44.dp)
                          ) {
                            PingleText(
                              text = "BACK TO SLIDERS",
                              style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                              )
                            )
                          }
                        }
                        
                        Button(
                          onClick = { colorPickerMode = "wheel" },
                          colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color.White.copy(alpha = 0.4f)
                          ),
                          modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                        ) {
                          PingleText(
                            text = "BACK TO COLOR WHEEL",
                            style = TextStyle(
                              fontFamily = FontFamily.SansSerif,
                              fontSize = 9.sp,
                              fontWeight = FontWeight.Medium,
                              letterSpacing = 0.5.sp
                            )
                          )
                        }
                      }
                    }
                  }
                }
              }

              // Spotify Connection
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                  .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
                  .padding(16.dp)
              ) {
                PingleText(
                  text = "SPOTIFY SOUNDTRACKS",
                  style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 1.sp
                  ),
                  modifier = Modifier.padding(bottom = 6.dp)
                )

                val spotifyManager = remember { SpotifyManager.getInstance(context) }
                val isConnected by spotifyManager.isConnected.collectAsState()
                val isSpotifyUnlocked = (totalSpinDuration >= 600000L) || isManualUnlocked // 10 minutes or manual unlocked

                if (!isSpotifyUnlocked) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    Icon(
                      imageVector = Icons.Default.Lock,
                      contentDescription = "Locked",
                      tint = Color.White.copy(alpha = 0.5f),
                      modifier = Modifier.size(16.dp)
                    )
                    PingleText(
                      text = "Unlocked at 10 minutes total spin time!",
                      style = TextStyle(color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                    )
                  }
                } else {
                  if (isConnected) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      PingleText(
                        text = "Status: CONNECTED",
                        style = TextStyle(color = Color(0xFF1DB954), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                      )
                      Box(
                        modifier = Modifier
                          .border(1.dp, Color.Red, RoundedCornerShape(8.dp))
                          .clickable { spotifyManager.disconnect() }
                          .padding(horizontal = 12.dp, vertical = 6.dp)
                      ) {
                        PingleText(
                          text = "LOGOUT",
                          style = TextStyle(color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        )
                      }
                    }
                  } else {
                    var showAuthWebView by remember { mutableStateOf(false) }
                    if (!showAuthWebView) {
                      Box(
                        modifier = Modifier
                          .border(1.dp, Color(0xFF1DB954), RoundedCornerShape(8.dp))
                          .clickable { showAuthWebView = true }
                          .padding(horizontal = 16.dp, vertical = 8.dp)
                      ) {
                        PingleText(
                          text = "CONNECT SPOTIFY ACCOUNT",
                          style = TextStyle(color = Color(0xFF1DB954), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        )
                      }
                    } else {
                      // Inline WebView for Spotify OAuth
                      Box(
                        modifier = Modifier
                          .fillMaxWidth()
                          .height(280.dp)
                          .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                      ) {
                        AndroidView(
                          modifier = Modifier.fillMaxSize(),
                          factory = { webViewContext ->
                            WebView(webViewContext).apply {
                              webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                  view: WebView?,
                                  request: WebResourceRequest?
                                ): Boolean {
                                  val url = request?.url?.toString() ?: ""
                                  if (url.startsWith(SpotifyManager.REDIRECT_URI)) {
                                    val uri = android.net.Uri.parse(url)
                                    val code = uri.getQueryParameter("code")
                                    if (code != null) {
                                      scope.launch {
                                        spotifyManager.handleAuthCode(code)
                                      }
                                      showAuthWebView = false
                                    }
                                    return true
                                  }
                                  return false
                                }
                              }
                              settings.javaScriptEnabled = true
                              loadUrl(spotifyManager.getAuthorizeUrl())
                            }
                          }
                        )
                      }
                    }
                  }
                }
              }

              Spacer(modifier = Modifier.height(20.dp))
            }
          }
          "special" -> {
            Column(
              modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
              verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
              if (isFoldableDevice()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    PingleText(
                      text = "FOLD THRESHOLD ANGLE",
                      style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 1.sp
                      )
                    )
                    PingleText(
                      text = String.format(Locale.US, "%.0f°", foldAngleThreshold),
                      style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0078D7)
                      )
                    )
                  }
                  Slider(
                    value = foldAngleThreshold,
                    onValueChange = onFoldAngleThresholdChange,
                    valueRange = 0f..180f,
                    colors = SliderDefaults.colors(
                      thumbColor = Color(0xFF0078D7),
                      activeTrackColor = Color(0xFF0078D7),
                      inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                    )
                  )
                  PingleText(
                    text = "Adjust the degree threshold to calibrate fold recognition for manual or automated modes.",
                    style = TextStyle(color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp),
                    modifier = Modifier.padding(top = 4.dp)
                  )
                }
              }

              // "Load Everything" preloading option
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                  .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
                  .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  PingleText(
                    text = "LOAD EVERYTHING",
                    style = TextStyle(
                      fontFamily = FontFamily.SansSerif,
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Bold,
                      color = Color.White,
                      letterSpacing = 1.sp
                    )
                  )
                  Spacer(modifier = Modifier.height(2.dp))
                  PingleText(
                    text = "Pre-load and cache every asset into RAM to completely eliminate load times. Defaults to optimized streaming.",
                    style = TextStyle(
                      fontFamily = FontFamily.SansSerif,
                      fontSize = 11.sp,
                      color = Color.White.copy(alpha = 0.5f)
                    )
                  )
                }

                androidx.compose.material3.Switch(
                  checked = loadEverythingState,
                  onCheckedChange = { checked ->
                    val success = viewModel.setLoadEverything(checked, context)
                    if (!success) {
                      showToast(context, "Sorry not enough RAM for this one buddy")
                    }
                  },
                  colors = androidx.compose.material3.SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF0078D7),
                    checkedTrackColor = Color(0xFF0078D7).copy(alpha = 0.5f)
                  )
                )
              }
            }
          }
          "easter eggs" -> {
            val invisiblePingleEnabledState by viewModel.invisiblePingleEnabled.collectAsState()
            Column(
              modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
              verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
              // Code Entry Card at the top of the easter eggs page
              Card(
                modifier = Modifier
                  .fillMaxWidth()
                  .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                  .testTag("codes_container_card"),
                colors = CardDefaults.cardColors(
                  containerColor = Color.White.copy(alpha = 0.03f)
                ),
                shape = RoundedCornerShape(16.dp)
              ) {
                Column(
                  modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                  PingleText(
                    text = "ENTER SECRET CODE",
                    style = TextStyle(
                      fontFamily = FontFamily.SansSerif,
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Bold,
                      color = Color.White.copy(alpha = 0.8f),
                      letterSpacing = 2.sp
                    )
                  )

                  androidx.compose.material3.OutlinedTextField(
                    value = codeInputText,
                    onValueChange = { codeInputText = it },
                    label = { PingleText("Secret Code") },
                    placeholder = { PingleText("Enter code...") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                      focusedBorderColor = Color.White,
                      unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                      focusedLabelColor = Color.White,
                      unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                      focusedTextColor = Color.White,
                      unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                      .fillMaxWidth()
                      .testTag("secret_code_input")
                  )

                  Button(
                    onClick = {
                      val cleanInput = codeInputText.trim()
                      if (cleanInput.equals("heavenischinese", ignoreCase = true) ||
                          cleanInput.equals("futureischinese", ignoreCase = true) ||
                          cleanInput.equals("idahoischinese", ignoreCase = true)) {
                        viewModel.triggerChineseCrash()
                        showToast(context, "游戏崩溃！")
                        codeInputText = ""
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                          (context as? android.app.Activity)?.finishAffinity()
                        }, 1000)
                      } else {
                        showToast(context, "Unknown code!")
                      }
                    },
                    colors = ButtonDefaults.buttonColors(
                      containerColor = Color.White.copy(alpha = 0.1f),
                      contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                      .fillMaxWidth()
                      .testTag("secret_code_enter_button")
                      .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                  ) {
                    PingleText(
                      text = "ENTER",
                      style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                      )
                    )
                  }
                }
              }
            }
          }
          "debug" -> {
            Column(
              modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
              verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
              Card(
                modifier = Modifier
                  .fillMaxWidth()
                  .border(
                    width = 1.dp,
                    color = if (isDebugUnlocked) Color.Green.copy(alpha = 0.3f) else Color.Red.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(16.dp)
                  )
                  .testTag("debug_password_card"),
                colors = CardDefaults.cardColors(
                  containerColor = Color.White.copy(alpha = 0.05f)
                ),
                shape = RoundedCornerShape(16.dp)
              ) {
                Column(
                  modifier = Modifier.padding(16.dp),
                  verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    PingleText(
                      text = "PASSWORD PROTECTION",
                      style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDebugUnlocked) Color.Green else Color.Red,
                        letterSpacing = 1.sp
                      )
                    )
                    
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                      Icon(
                        imageVector = if (isDebugUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                        contentDescription = if (isDebugUnlocked) "Unlocked" else "Locked",
                        tint = if (isDebugUnlocked) Color.Green else Color.Red,
                        modifier = Modifier.size(20.dp)
                      )
                      PingleText(
                        text = if (isDebugUnlocked) "UNLOCKED" else "LOCKED",
                        style = TextStyle(
                          fontSize = 11.sp,
                          fontWeight = FontWeight.Bold,
                          color = if (isDebugUnlocked) Color.Green else Color.Red
                        )
                      )
                    }
                  }

                  androidx.compose.material3.OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { newValue ->
                      passwordInput = newValue
                    },
                    label = { PingleText("Password") },
                    placeholder = { PingleText("Enter password") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                      focusedBorderColor = if (isDebugUnlocked) Color.Green else Color.Red,
                      unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                      focusedLabelColor = if (isDebugUnlocked) Color.Green else Color.Red,
                      unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                      focusedTextColor = Color.White,
                      unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                      .fillMaxWidth()
                      .testTag("debug_password_input")
                  )

                  Button(
                    onClick = {
                      if (isDebugUnlocked) {
                        viewModel.setDebugUnlocked(false)
                        passwordInput = ""
                        showToast(context, "Developer menu locked.")
                      } else {
                        if (passwordInput == "tetopearbro") {
                          viewModel.setDebugUnlocked(true)
                          showToast(context, "Developer menu unlocked!")
                        } else {
                          showToast(context, "Incorrect password!")
                        }
                      }
                    },
                    colors = ButtonDefaults.buttonColors(
                      containerColor = if (isDebugUnlocked) Color.Green.copy(alpha = 0.15f) else Color.Red.copy(alpha = 0.15f),
                      contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                      .fillMaxWidth()
                      .testTag("debug_password_enter_button")
                      .border(
                        width = 1.dp,
                        color = if (isDebugUnlocked) Color.Green else Color.Red,
                        shape = RoundedCornerShape(10.dp)
                      )
                  ) {
                    PingleText(
                      text = if (isDebugUnlocked) "LOCK DEBUG" else "ENTER",
                      style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                      )
                    )
                  }
                }
              }

              if (isDebugUnlocked) {
                PingleText(
                  text = "DEVELOPER COMMANDS",
                  style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Red,
                    letterSpacing = 1.sp
                  )
                )

                // Easter Egg override switches
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.Red.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .background(Color.Red.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
                    .padding(14.dp),
                  verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                  PingleText(
                    text = "FORCE UNLOCK OVERRIDES",
                    style = TextStyle(color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                  )

                  val debugStates = listOf(
                    Triple("Pinglemanualspin Mode", isManualUnlocked) { viewModel.unlockManual() },
                    Triple("Reverse Spin Egg", isReverseSpinUnlocked) { viewModel.setReverseSpinUnlocked(true) },
                    Triple("Matrix Rain Egg", isMatrixBgUnlocked) { viewModel.setMatrixBgUnlocked(true) },
                    Triple("Rainbow Neon Egg", isRainbowNeonUnlocked) { viewModel.setRainbowNeonUnlocked(true) },
                    Triple("Cosmic Starfield Egg", isSpaceStarsUnlocked) { viewModel.setSpaceStarsUnlocked(true) }
                  )

                  debugStates.forEach { (label, flow, action) ->
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      PingleText(
                        text = label,
                        style = TextStyle(color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                      )

                      Box(
                        modifier = Modifier
                          .border(
                            width = 1.dp,
                            color = if (flow) Color.Green.copy(alpha = 0.5f) else Color.Red.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(6.dp)
                          )
                          .clickable { action() }
                          .padding(horizontal = 10.dp, vertical = 4.dp)
                      ) {
                        PingleText(
                          text = if (flow) "UNLOCKED" else "LOCK",
                          style = TextStyle(
                            color = if (flow) Color.Green else Color.Red,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                          )
                        )
                      }
                    }
                  }
                }

                // Custom Score creator
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.Red.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .background(Color.Red.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
                    .padding(14.dp),
                  verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                  PingleText(
                    text = "CUSTOM SCORE CREATOR (RED WATERMARK)",
                    style = TextStyle(color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                  )

                  var debugScoreSeconds by remember { mutableFloatStateOf(60f) }

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    PingleText(
                      text = "Duration to add:",
                      style = TextStyle(color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    )
                    PingleText(
                      text = formatDuration((debugScoreSeconds * 1000).toLong()),
                      style = TextStyle(color = Color.Red, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    )
                  }

                  Slider(
                    value = debugScoreSeconds,
                    onValueChange = { debugScoreSeconds = it },
                    valueRange = 5f..600f,
                    colors = SliderDefaults.colors(
                      thumbColor = Color.Red,
                      activeTrackColor = Color.Red,
                      inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                    )
                  )

                  Box(
                    modifier = Modifier
                      .fillMaxWidth()
                      .border(1.dp, Color.Red, RoundedCornerShape(10.dp))
                      .clickable {
                        onCreateDebugScore((debugScoreSeconds * 1000).toLong())
                        showToast(context, "Added custom score with db watermark!")
                      }
                      .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                  ) {
                    PingleText(
                      text = "CREATE DEBUG SCORE",
                      style = TextStyle(color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    )
                  }
                }

                // Reset Button
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.Gray, RoundedCornerShape(10.dp))
                    .clickable {
                      viewModel.clearAllScores()
                      showToast(context, "All high scores cleared.")
                    }
                    .padding(vertical = 12.dp),
                  contentAlignment = Alignment.Center
                ) {
                  PingleText(
                    text = "RESET ALL SCORES",
                    style = TextStyle(color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                  )
                }
              }
            }
          }

          "request features" -> {
            Box(
              modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 40.dp),
              contentAlignment = Alignment.Center
            ) {
              PingleText(
                text = "COMING SOON MAN",
                style = TextStyle(
                  fontFamily = FontFamily.SansSerif,
                  fontSize = 24.sp,
                  fontWeight = FontWeight.Black,
                  color = Color.White,
                  letterSpacing = 2.sp
                )
              )
            }
          }
        }
      }

      // 4. Windows Phone Style back button centered
      Spacer(modifier = Modifier.height(12.dp))
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
          .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
          .clickable {
            VibrationHelper.vibrate(context, buttonVibrationMode)
            onBackClicked()
          }
          .testTag("save_back_button"),
        contentAlignment = Alignment.Center
      ) {
        PingleText(
          text = "SAVE & BACK",
          style = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            letterSpacing = 2.sp
          )
        )
      }
    }
  }
}

@Composable
fun HSVColorPicker(
  pingleCustomColorInt: Int,
  onCustomColorChange: (Int) -> Unit,
  modifier: Modifier = Modifier
) {
  var hue by remember(pingleCustomColorInt) {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(pingleCustomColorInt, hsv)
    mutableStateOf(hsv[0])
  }
  var saturation by remember(pingleCustomColorInt) {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(pingleCustomColorInt, hsv)
    mutableStateOf(hsv[1])
  }
  var value by remember(pingleCustomColorInt) {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(pingleCustomColorInt, hsv)
    mutableStateOf(hsv[2])
  }

  val updateColor = { h: Float, s: Float, v: Float ->
    hue = h
    saturation = s
    value = v
    val updatedColor = android.graphics.Color.HSVToColor(floatArrayOf(h, s, v))
    onCustomColorChange(updatedColor)
  }

  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = 12.dp),
    horizontalArrangement = Arrangement.Center,
    verticalAlignment = Alignment.CenterVertically
  ) {
    // Brightness bar (Vertical slider) on left
    val brightnessBarWidth = 32.dp
    val brightnessBarHeight = 180.dp
    
    Box(
      modifier = Modifier
        .width(brightnessBarWidth)
        .height(brightnessBarHeight)
        .clip(RoundedCornerShape(6.dp))
        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
        .pointerInput(hue, saturation) {
          detectTapGestures { offset ->
            val v = (1f - (offset.y / size.height)).coerceIn(0f, 1f)
            updateColor(hue, saturation, v)
          }
        }
        .pointerInput(hue, saturation) {
          detectDragGestures { change, _ ->
            change.consume()
            val v = (1f - (change.position.y / size.height)).coerceIn(0f, 1f)
            updateColor(hue, saturation, v)
          }
        }
    ) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val topColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, 1f)))
        val brush = Brush.verticalGradient(listOf(topColor, Color.Black))
        drawRect(brush = brush)
        
        val y = (1f - value) * size.height
        drawLine(
          color = Color.White,
          start = Offset(0f, y),
          end = Offset(size.width, y),
          strokeWidth = 3.dp.toPx()
        )
        drawLine(
          color = Color.Black,
          start = Offset(0f, y),
          end = Offset(size.width, y),
          strokeWidth = 1.dp.toPx()
        )
      }
    }

    Spacer(modifier = Modifier.width(36.dp))

    // Color Wheel on right
    val wheelSize = 180.dp
    Box(
      modifier = Modifier
        .size(wheelSize)
        .pointerInput(value) {
          detectTapGestures { offset ->
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val dx = offset.x - centerX
            val dy = offset.y - centerY
            val radius = size.width / 2f
            
            var angleRad = Math.atan2(dy.toDouble(), dx.toDouble()).toFloat()
            if (angleRad < 0) {
              angleRad += (2 * Math.PI).toFloat()
            }
            val h = (angleRad * 180f / Math.PI).toFloat()
            val dist = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
            val s = (dist / radius).coerceIn(0f, 1f)
            updateColor(h, s, value)
          }
        }
        .pointerInput(value) {
          detectDragGestures { change, _ ->
            change.consume()
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val dx = change.position.x - centerX
            val dy = change.position.y - centerY
            val radius = size.width / 2f
            
            var angleRad = Math.atan2(dy.toDouble(), dx.toDouble()).toFloat()
            if (angleRad < 0) {
              angleRad += (2 * Math.PI).toFloat()
            }
            val h = (angleRad * 180f / Math.PI).toFloat()
            val dist = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
            val s = (dist / radius).coerceIn(0f, 1f)
            updateColor(h, s, value)
          }
        }
    ) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val radius = size.width / 2f

        // 1. Draw sweep gradient of Hue
        val hueColors = listOf(
          Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red
        )
        drawCircle(
          brush = Brush.sweepGradient(hueColors, center = Offset(centerX, centerY)),
          radius = radius,
          center = Offset(centerX, centerY)
        )

        // 2. Draw radial gradient of Saturation (White in center, Transparent at edge)
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(Color.White, Color.Transparent),
            center = Offset(centerX, centerY),
            radius = radius
          ),
          radius = radius,
          center = Offset(centerX, centerY)
        )

        // 3. Draw current selection circle handle
        val handleAngleRad = (hue * Math.PI / 180f).toFloat()
        val handleDist = saturation * radius
        val handleX = centerX + handleDist * Math.cos(handleAngleRad.toDouble()).toFloat()
        val handleY = centerY + handleDist * Math.sin(handleAngleRad.toDouble()).toFloat()

        drawCircle(
          color = Color.White,
          radius = 8.dp.toPx(),
          center = Offset(handleX, handleY)
        )
        drawCircle(
          color = Color.Black,
          radius = 8.dp.toPx(),
          center = Offset(handleX, handleY),
          style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
        )
        drawCircle(
          color = Color.Red,
          radius = 3.dp.toPx(),
          center = Offset(handleX, handleY)
        )
      }
    }
  }
}
