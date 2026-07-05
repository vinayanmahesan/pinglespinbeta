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

  LaunchedEffect(pagerOffset) {
    val scrollTargetPx = with(density) { (pagerOffset * 80.dp.toPx()).toInt() }
    headerScrollState.scrollTo(scrollTargetPx)
  }

  // Custom Image picker contract
  val imagePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri: android.net.Uri? ->
    if (uri != null) {
      try {
        val contentResolver = context.contentResolver
        val inputStream = contentResolver.openInputStream(uri)
        if (inputStream != null) {
          val file = java.io.File(context.filesDir, "custom_pingle_image.png")
          file.outputStream().use { outputStream ->
            inputStream.use { it.copyTo(outputStream) }
          }
          onCustomImageUriChange(file.absolutePath)
          onUseCustomImageChange(true)
          showToast(context, "Custom image updated successfully!")
        }
      } catch (e: Exception) {
        onCustomImageUriChange(uri.toString())
        onUseCustomImageChange(true)
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
      androidx.compose.material3.Text(
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

          androidx.compose.material3.Text(
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
              // Custom Pingle Image replacement
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                  .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
                  .padding(16.dp)
              ) {
                androidx.compose.material3.Text(
                  text = "CUSTOM PINGLE IMAGE",
                  style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 1.sp
                  ),
                  modifier = Modifier.padding(bottom = 8.dp)
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
                      androidx.compose.material3.Text(
                        text = "Use custom image",
                        style = TextStyle(color = Color.White, fontSize = 13.sp)
                      )
                    }

                    Box(
                      modifier = Modifier
                        .border(1.dp, Color(0xFF0078D7), RoundedCornerShape(8.dp))
                        .clickable { imagePickerLauncher.launch("image/*") }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                      androidx.compose.material3.Text(
                        text = "SELECT IMAGE",
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
                  androidx.compose.material3.Text(
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
                  androidx.compose.material3.Text(
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
                  androidx.compose.material3.Text(
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
                  androidx.compose.material3.Text(
                    text = "SPIN MULTIPLIER",
                    style = TextStyle(
                      fontFamily = FontFamily.SansSerif,
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Bold,
                      color = Color.White,
                      letterSpacing = 1.sp
                    )
                  )
                  androidx.compose.material3.Text(
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
                    androidx.compose.material3.Text(
                      text = "MANUAL DECAY (FRICTION)",
                      style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 1.sp
                      )
                    )
                    androidx.compose.material3.Text(
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

              // Color Tint Selector Row
              Column(modifier = Modifier.fillMaxWidth()) {
                androidx.compose.material3.Text(
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
                        androidx.compose.material3.Text(
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
                      .background(Color(pingleCustomColorInt))
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
                    } else if (pingleTintId == "custom") {
                      Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                      )
                    }
                  }
                }

                // If Custom is selected and unlocked, show the RGB Custom color sliders
                val hoursUnlocked = totalSpinDuration / 3600000L
                val isCustomColorUnlocked = (hoursUnlocked >= 5) || isManualUnlocked
                if (pingleTintId == "custom" && isCustomColorUnlocked) {
                  Spacer(modifier = Modifier.height(16.dp))
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
                androidx.compose.material3.Text(
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
                    androidx.compose.material3.Text(
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
                      androidx.compose.material3.Text(
                        text = "Status: CONNECTED",
                        style = TextStyle(color = Color(0xFF1DB954), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                      )
                      Box(
                        modifier = Modifier
                          .border(1.dp, Color.Red, RoundedCornerShape(8.dp))
                          .clickable { spotifyManager.disconnect() }
                          .padding(horizontal = 12.dp, vertical = 6.dp)
                      ) {
                        androidx.compose.material3.Text(
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
                        androidx.compose.material3.Text(
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
                .verticalScroll(rememberScrollState()),
              verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
              if (isFoldableDevice()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    androidx.compose.material3.Text(
                      text = "FOLD THRESHOLD ANGLE",
                      style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 1.sp
                      )
                    )
                    androidx.compose.material3.Text(
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
                  androidx.compose.material3.Text(
                    text = "Adjust the degree threshold to calibrate fold recognition for manual or automated modes.",
                    style = TextStyle(color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp),
                    modifier = Modifier.padding(top = 4.dp)
                  )
                }
              } else {
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                  contentAlignment = Alignment.Center
                ) {
                  androidx.compose.material3.Text(
                    text = "no features currently available",
                    style = TextStyle(
                      fontFamily = FontFamily.SansSerif,
                      fontSize = 16.sp,
                      fontWeight = FontWeight.ExtraLight,
                      color = Color.White.copy(alpha = 0.4f),
                      letterSpacing = 1.sp
                    )
                  )
                }
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
                  androidx.compose.material3.Text(
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
                    label = { androidx.compose.material3.Text("Secret Code") },
                    placeholder = { androidx.compose.material3.Text("Enter code...") },
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
                      if (cleanInput.equals("Pride", ignoreCase = true)) {
                        viewModel.setRainbowNeonUnlocked(true)
                        viewModel.setEasterRainbowNeon(true)
                        showToast(context, "Pride code activated! Rainbow tint unlocked and enabled.")
                        codeInputText = ""
                      } else if (cleanInput.isEmpty()) {
                        blankCount++
                        if (blankCount == 1) {
                          showToast(context, "dude type something in")
                        } else if (blankCount == 2) {
                          showToast(context, "dawg there isn't anything here")
                        } else {
                          viewModel.setInvisiblePingleEnabled(true)
                          showToast(context, "Invisible pingle unlocked! It's gone, and you can collect any time.")
                        }
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
                    androidx.compose.material3.Text(
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

              androidx.compose.material3.Text(
                text = "YOUR UNLOCKED EGGS",
                style = TextStyle(
                  fontFamily = FontFamily.SansSerif,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color.White.copy(alpha = 0.6f),
                  letterSpacing = 1.sp
                )
              )

              // Row helper for Easter egg item
              @Composable
              fun EasterEggItem(
                title: String,
                desc: String,
                isUnlocked: Boolean,
                isEnabled: Boolean,
                onCheckedChange: (Boolean) -> Unit
              ) {
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
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                      androidx.compose.material3.Text(
                        text = title,
                        style = TextStyle(
                          color = if (isUnlocked) Color.White else Color.White.copy(alpha = 0.4f),
                          fontSize = 14.sp,
                          fontWeight = FontWeight.Bold
                        )
                      )
                      if (!isUnlocked) {
                        Icon(
                          imageVector = Icons.Default.Lock,
                          contentDescription = "Locked",
                          tint = Color.White.copy(alpha = 0.4f),
                          modifier = Modifier.size(12.dp)
                        )
                      }
                    }
                    androidx.compose.material3.Text(
                      text = desc,
                      style = TextStyle(
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp
                      ),
                      modifier = Modifier.padding(top = 2.dp)
                    )
                  }

                  if (isUnlocked) {
                    androidx.compose.material3.Switch(
                      checked = isEnabled,
                      onCheckedChange = onCheckedChange,
                      colors = androidx.compose.material3.SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF0078D7)
                      )
                    )
                  }
                }
              }

              if (isReverseSpinUnlocked) {
                EasterEggItem(
                  title = "Reverse Spin direction",
                  desc = "Unlock condition: Spin for 15 seconds in a single run.",
                  isUnlocked = isReverseSpinUnlocked,
                  isEnabled = easterReverseSpin,
                  onCheckedChange = onEasterReverseSpinChange
                )
              }

              if (isMatrixBgUnlocked) {
                EasterEggItem(
                  title = "Matrix rain background",
                  desc = "Unlock condition: Spin for 30 seconds in a single run.",
                  isUnlocked = isMatrixBgUnlocked,
                  isEnabled = easterMatrixBg,
                  onCheckedChange = onEasterMatrixBgChange
                )
              }

              if (isRainbowNeonUnlocked) {
                EasterEggItem(
                  title = "Rainbow neon Pingle cycling",
                  desc = "Unlock condition: Reach 1 minute of cumulative total spin time.",
                  isUnlocked = isRainbowNeonUnlocked,
                  isEnabled = easterRainbowNeon,
                  onCheckedChange = onEasterRainbowNeonChange
                )
              }

              if (isSpaceStarsUnlocked) {
                EasterEggItem(
                  title = "Cosmic starfield background",
                  desc = "Unlock condition: Reach 5 minutes of cumulative total spin time.",
                  isUnlocked = isSpaceStarsUnlocked,
                  isEnabled = easterSpaceStars,
                  onCheckedChange = onEasterSpaceStarsChange
                )
              }

              if (blankCount >= 3 || invisiblePingleEnabledState) {
                EasterEggItem(
                  title = "Invisible Pingle",
                  desc = "Secret code unlock: Pingle is completely hidden.",
                  isUnlocked = true,
                  isEnabled = invisiblePingleEnabledState,
                  onCheckedChange = { viewModel.setInvisiblePingleEnabled(it) }
                )
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
                    androidx.compose.material3.Text(
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
                      androidx.compose.material3.Text(
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
                    label = { androidx.compose.material3.Text("Password") },
                    placeholder = { androidx.compose.material3.Text("Enter password") },
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
                    androidx.compose.material3.Text(
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
                androidx.compose.material3.Text(
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
                  androidx.compose.material3.Text(
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
                      androidx.compose.material3.Text(
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
                        androidx.compose.material3.Text(
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
                  androidx.compose.material3.Text(
                    text = "CUSTOM SCORE CREATOR (RED WATERMARK)",
                    style = TextStyle(color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                  )

                  var debugScoreSeconds by remember { mutableFloatStateOf(60f) }

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    androidx.compose.material3.Text(
                      text = "Duration to add:",
                      style = TextStyle(color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    )
                    androidx.compose.material3.Text(
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
                    androidx.compose.material3.Text(
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
                  androidx.compose.material3.Text(
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
              androidx.compose.material3.Text(
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
          .clickable { onBackClicked() }
          .testTag("save_back_button"),
        contentAlignment = Alignment.Center
      ) {
        androidx.compose.material3.Text(
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
