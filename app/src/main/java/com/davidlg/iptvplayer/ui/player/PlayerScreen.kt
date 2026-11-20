@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.davidlg.iptvplayer.ui.player

import android.view.ViewGroup
import android.util.Base64
import android.util.Log
import android.net.Uri
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebChromeClient
import android.webkit.WebViewClient
import org.json.JSONObject
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.davidlg.iptvplayer.MainViewModel
import com.davidlg.iptvplayer.data.EpgStore
import com.davidlg.iptvplayer.data.Channel
import com.davidlg.iptvplayer.player.PlaybackFactory
import com.davidlg.iptvplayer.ui.Sans
import com.davidlg.iptvplayer.ui.dlClickable
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Blue = Color(0xFF56AAFF)
private val Cyan = Color(0xFF7EE7FF)
private val Gold = Color(0xFFE0C38A)

private fun hm(ms: Long): String = SimpleDateFormat("hh:mm a", Locale.US).format(Date(ms)).replace("AM", "A.M.").replace("PM", "P.M.")
private fun clock(): String = SimpleDateFormat("hh:mm:ss a", Locale.US).format(Date()).uppercase(Locale.US)
private fun mmss(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0); val h = s / 3600; val m = (s % 3600) / 60; val x = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, x) else "%02d:%02d".format(m, x)
}

/** Shaka Player experimental. Pasa URL, cabeceras y DRM desde los metadatos M3U del canal. */
@Composable
fun ShakaSurface(channel: Channel, modifier: Modifier = Modifier) {
    val url = channel.url
    val config = remember(channel.id, url, channel.props) { shakaPayload(channel) }
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.loadsImagesAutomatically = true
                settings.blockNetworkLoads = false
                setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(message: android.webkit.ConsoleMessage): Boolean {
                        Log.d("DavidLG-Shaka", "${message.message()} (${message.sourceId()}:${message.lineNumber()})")
                        return true
                    }
                }
                webViewClient = object : WebViewClient() {
                    override fun onReceivedError(view: WebView, request: android.webkit.WebResourceRequest, error: android.webkit.WebResourceError) {
                        super.onReceivedError(view, request, error)
                        if (request.isForMainFrame) Log.e("DavidLG-Shaka", "WebView load error: ${error.description}")
                    }
                }
                setBackgroundColor(android.graphics.Color.BLACK)
                loadShakaDocument(this, config)
            }
        },
        update = { view -> if (view.tag != config.toString()) loadShakaDocument(view, config) }
    )
}

private fun shakaHeaders(raw: String): Map<String, String> {
    val result = linkedMapOf<String, String>()
    raw.split('&').forEach { part ->
        val i = part.indexOf('=')
        if (i > 0) {
            val key = part.substring(0, i).trim()
            val value = try { java.net.URLDecoder.decode(part.substring(i + 1), "UTF-8") }
                catch (_: Exception) { part.substring(i + 1) }
            if (key.isNotBlank()) result[key] = value
        }
    }
    return result
}

private fun shakaKeyHex(value: String): String {
    val hex = value.trim().removePrefix("0x").removePrefix("0X")
        .filter { it in "0123456789abcdefABCDEF" }.lowercase()
    if (hex.length == 32) return hex
    return try {
        val decoded = Base64.decode(value.trim(), Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
        decoded.joinToString("") { "%02x".format(it) }
    } catch (_: Exception) { "" }
}

private fun shakaB64Url(hex: String): String {
    val bytes = ByteArray(hex.length / 2) { hex.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
    return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
}

private fun shakaPayload(channel: Channel): JSONObject {
    val props = channel.props
    val streamHeaders = linkedMapOf<String, String>()
    streamHeaders.putAll(shakaHeaders(props["inputstream.adaptive.stream_headers"].orEmpty()))
    props["referer"]?.takeIf { it.isNotBlank() }?.let { streamHeaders["Referer"] = it }
    props["origin"]?.takeIf { it.isNotBlank() }?.let { streamHeaders["Origin"] = it }
    props["user_agent"]?.takeIf { it.isNotBlank() }?.let { streamHeaders["User-Agent"] = it }

    val type = (props["inputstream.adaptive.license_type"] ?: props["license_type"] ?: "").lowercase()
    val licenseValue = (props["inputstream.adaptive.license_key"] ?: props["license_key"]
        ?: props["clearkey"] ?: props["clear_key"] ?: "").trim()
    val licenseParts = licenseValue.split('|')
    val licenseUrl = licenseParts.firstOrNull().orEmpty()
    val licenseHeaders = linkedMapOf<String, String>()
    if (licenseParts.size > 1) licenseHeaders.putAll(shakaHeaders(licenseParts[1]))
    licenseHeaders.putAll(streamHeaders)

    val drm = JSONObject()
    if ((type.contains("widevine") || type == "com.widevine.alpha") && licenseUrl.startsWith("http", true)) {
        drm.put("servers", JSONObject().put("com.widevine.alpha", licenseUrl))
    }
    val clearKeys = JSONObject()
    fun addKey(kidRaw: String?, keyRaw: String?) {
        if (kidRaw.isNullOrBlank() || keyRaw.isNullOrBlank()) return
        val kid = shakaKeyHex(kidRaw); val key = shakaKeyHex(keyRaw)
        if (kid.length == 32 && key.length == 32) clearKeys.put(shakaB64Url(kid), shakaB64Url(key))
    }
    if (licenseValue.isNotEmpty() && !licenseUrl.startsWith("http", true)) {
        licenseValue.split(',').forEach { pair ->
            val i = pair.indexOf(':')
            if (i > 0) addKey(pair.substring(0, i), pair.substring(i + 1))
        }
    }
    if (clearKeys.length() == 0) addKey(props["kid"], props["key"])
    if (clearKeys.length() > 0) drm.put("clearKeys", clearKeys)

    return JSONObject()
        .put("url", channel.url)
        .put("headers", JSONObject(streamHeaders))
        .put("licenseHeaders", JSONObject(licenseHeaders))
        .put("drm", drm)
}

private fun loadShakaDocument(view: WebView, payload: JSONObject) {
    val safePayload = JSONObject.quote(payload.toString())
    val streamUri = runCatching { Uri.parse(payload.optString("url")) }.getOrNull()
    // Usar el origen real del stream evita forzar https://localhost como origen CORS.
    // Muchos servidores IPTV permiten el mismo origen, pero rechazan solicitudes desde localhost.
    val baseUrl = streamUri?.let { uri ->
        if (!uri.scheme.isNullOrBlank() && !uri.authority.isNullOrBlank()) "${uri.scheme}://${uri.authority}/" else null
    } ?: "https://localhost/"
    val html = """<!doctype html><html><head><meta name=viewport content='width=device-width,initial-scale=1,maximum-scale=1'>
<style>html,body{margin:0;width:100%;height:100%;background:#000;color:#fff;font:14px sans-serif}video{width:100%;height:100%;object-fit:contain}#status{position:absolute;left:12px;right:12px;bottom:12px;padding:9px 12px;background:#b0000000;border-radius:8px;color:#fff;white-space:pre-wrap;font-size:12px}</style>
</head><body><video id='v' autoplay playsinline controls></video><div id='status'>Iniciando Shaka Player…</div><script>
const cfg=JSON.parse($safePayload); const statusEl=document.getElementById('status');
function status(t){statusEl.textContent=t;statusEl.style.display=t?'block':'none';}
function loadShaka(src, fallback){return new Promise((resolve,reject)=>{const s=document.createElement('script');s.src=src;s.onload=resolve;s.onerror=()=>{s.remove();if(fallback)loadShaka(fallback,null).then(resolve).catch(reject);else reject(new Error('No se pudo descargar la biblioteca Shaka desde los CDN'));};document.head.appendChild(s);});}
async function start(){
 const v=document.getElementById('v');
 try {
   if(!window.shaka) await loadShaka('https://cdnjs.cloudflare.com/ajax/libs/shaka-player/4.12.8/shaka-player.compiled.js','https://cdn.jsdelivr.net/npm/shaka-player@4.12.8/dist/shaka-player.compiled.js');
   if(!window.shaka) throw new Error('Biblioteca Shaka no disponible');
   shaka.polyfill.installAll();
   if(!shaka.Player.isBrowserSupported()) throw new Error('Este WebView no ofrece MSE/EME necesario para Shaka. Actualiza Android System WebView/Chrome.');
   const p=new shaka.Player(); await p.attach(v);
   p.addEventListener('error',e=>{const d=e.detail||{};status('Error Shaka '+(d.code||'')+' · '+(d.message||d.category||'revisa el registro'));console.error('Shaka player error',JSON.stringify({code:d.code,category:d.category,severity:d.severity,data:d.data}));});
   p.addEventListener('loading',()=>status('Conectando con el canal…'));
   p.addEventListener('loaded',()=>status(''));
   p.configure({drm:cfg.drm||{},streaming:{retryParameters:{maxAttempts:3,baseDelay:500,backoffFactor:2}}});
   const net=p.getNetworkingEngine();
   net.registerRequestFilter((type,request)=>{
     const common=cfg.headers||{}; Object.keys(common).forEach(k=>request.headers[k]=common[k]);
     if(type===shaka.net.NetworkingEngine.RequestType.LICENSE){const lic=cfg.licenseHeaders||{};Object.keys(lic).forEach(k=>request.headers[k]=lic[k]);}
   });
   status('Cargando stream…'); await p.load(cfg.url); status('');
   const playPromise=v.play(); if(playPromise&&playPromise.catch)playPromise.catch(e=>{status('El dispositivo bloqueó el inicio automático: pulsa reproducir.');console.warn(e);});
 } catch(e){status('No se pudo iniciar Shaka: '+(e.message||e));console.error('Shaka startup failure',e);}
}
start();
</script></body></html>"""
    view.tag = payload.toString()
    view.loadDataWithBaseURL(baseUrl, html, "text/html", "UTF-8", null)
}

/** Reproductor a pantalla completa con ExoPlayer (único para todo el zapping → cambio instantáneo). */
@Composable
fun PlayerScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    val hostView = LocalView.current
    DisposableEffect(hostView) {
        hostView.keepScreenOn = true
        onDispose { hostView.keepScreenOn = false }
    }
    val screenWidth = LocalConfiguration.current.screenWidthDp
    val compact = screenWidth < 600
    val tvLayout = screenWidth >= 840
    val pp by vm.playbackPrefs.collectAsState()
    val epgVer by vm.epgVersion.collectAsState()
    val bufferMode = pp.str("bufferMode", "default")
    val bitrateMode = pp.str("bitrateMode", "auto")
    val playerEngine = pp.str("playerEngine", "auto")
    val useShaka = playerEngine == "shaka"
    val player = remember(bufferMode, bitrateMode) { PlaybackFactory.newPlayer(ctx, bufferMode, bitrateMode) }
    val c = vm.current
    var overlay by remember { mutableStateOf(true) }
    var tick by remember { mutableLongStateOf(0L) }
    var buffering by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var digits by remember { mutableStateOf("") }
    var drawer by remember { mutableStateOf(false) }
    var guideDrawer by remember { mutableStateOf(false) }
    var playerOptions by remember { mutableStateOf(false) }
    var okLongTriggered by remember { mutableStateOf(false) }
    var okHoldJob by remember { mutableStateOf<Job?>(null) }
    var hideAt by remember { mutableLongStateOf(0L) }
    var streamQuality by remember { mutableStateOf("—") }
    val scope = rememberCoroutineScope()
    var zapJob by remember { mutableStateOf<Job?>(null) }
    var pendingZapSteps by remember { mutableStateOf(0) }
    val focus = remember { FocusRequester() }

    fun show() { overlay = true; hideAt = System.currentTimeMillis() + 5000 }
    fun requestZap(direction: Int) {
        val wait = pp.str("zapDelayMs", "0").toLongOrNull()?.coerceIn(0L, 30_000L) ?: 0L
        zapJob?.cancel()
        if (wait == 0L) {
            pendingZapSteps = 0
            drawer = false
            vm.zap(direction)
        } else {
            pendingZapSteps += direction
            zapJob = scope.launch {
                delay(wait)
                val steps = pendingZapSteps
                pendingZapSteps = 0
                drawer = false
                if (steps != 0) vm.zap(steps)
            }
        }
    }

    // Preferencias de reproducción (audio/subtítulos) como playbackPrefs de la versión web.
    LaunchedEffect(pp, useShaka) {
        if (useShaka) return@LaunchedEffect
        val a = pp.str("audio", "es"); val s = pp.str("subs", "off")
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon().apply {
            if (a != "auto") setPreferredAudioLanguage(a)
            if (s == "off") setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true) else { setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false); setPreferredTextLanguage(s) }
        }.build()
    }

    DisposableEffect(player) {
        val l = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) { buffering = state == Player.STATE_BUFFERING; if (state == Player.STATE_READY) error = null }
            override fun onPlayerError(e: PlaybackException) { error = "No se pudo reproducir (${e.errorCodeName})" }
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                val h = videoSize.height
                streamQuality = when {
                    h >= 2160 -> "4K"
                    h >= 1080 -> "FHD"
                    h >= 720 -> "HD"
                    h > 0 -> "SD"
                    else -> "—"
                }
            }
        }
        player.addListener(l)
        onDispose {
            vm.current?.let { if (it.isVod) vm.saveVodProgress(it.id, player.currentPosition, player.duration.coerceAtLeast(0)) }
            player.removeListener(l); player.release()
        }
    }

    // Cada cambio de canal reutiliza el mismo ExoPlayer: sin recrear pantalla ni vista.
    LaunchedEffect(c?.id, player, useShaka) {
        if (c != null) {
            error = null; buffering = true; streamQuality = "—"
            vm.markWatched(c)
            if (!useShaka) PlaybackFactory.play(ctx, player, c, vm.startPositionMs) else player.stop()
            vm.startPositionMs = 0L
            show()
        }
    }
    LaunchedEffect(drawer) { if (!drawer) runCatching { focus.requestFocus() } }
    LaunchedEffect(Unit) { while (true) { delay(500); tick++; if (overlay && System.currentTimeMillis() > hideAt && !drawer && !guideDrawer && !playerOptions) overlay = false } }
    // Entrada numérica de canal (como en la TV): se aplica tras 1.5 s sin teclear.
    LaunchedEffect(digits) {
        if (digits.isNotEmpty()) {
            delay(1500)
            val n = digits.toIntOrNull(); digits = ""
            vm.queue.firstOrNull { it.number == n }?.let { vm.current = it }
        }
    }
    BackHandler { when { playerOptions -> playerOptions = false; drawer -> drawer = false; guideDrawer -> guideDrawer = false; else -> vm.closePlayer() } }

    val isVod = c?.isVod == true
    val now = remember(c, epgVer, tick / 20) { c?.let { EpgStore.now(it) } }
    val next = remember(c, epgVer, tick / 20) { c?.let { EpgStore.next(it) } }

    Box(
        Modifier.fillMaxSize().background(Color.Black).focusRequester(focus).focusable()
            .pointerInput(pp) {
                var dy = 0f
                detectVerticalDragGestures(
                    onDragStart = { dy = 0f }, onDragEnd = { if (!isVod) { if (dy < -120) requestZap(1) else if (dy > 120) requestZap(-1) } },
                ) { _, d -> dy += d }
            }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { if (overlay) overlay = false else show() }
            .onPreviewKeyEvent { e ->
                val k = e.key
                val isOk = k == Key.DirectionCenter || k == Key.Enter || k == Key.NumPadEnter
                if (e.type == KeyEventType.KeyUp && isOk) {
                    if (okHoldJob?.isActive == true) {
                        okHoldJob?.cancel(); okHoldJob = null
                        if (!drawer && !guideDrawer) {
                            if (isVod) player.playWhenReady = !player.playWhenReady
                            if (overlay && !isVod) overlay = false else show()
                        }
                    } else if (okLongTriggered) okLongTriggered = false
                    return@onPreviewKeyEvent true
                }
                if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                // Cuando un panel está abierto, el D-pad pertenece a sus controles internos.
                if (drawer || guideDrawer) return@onPreviewKeyEvent false
                val d = when (k) {
                    Key.Zero -> "0"; Key.One -> "1"; Key.Two -> "2"; Key.Three -> "3"; Key.Four -> "4"
                    Key.Five -> "5"; Key.Six -> "6"; Key.Seven -> "7"; Key.Eight -> "8"; Key.Nine -> "9"; else -> null
                }
                when {
                    isOk && !drawer && !guideDrawer -> {
                        if (okHoldJob == null && !okLongTriggered) {
                            okLongTriggered = false
                            okHoldJob = scope.launch {
                                delay(650)
                                okLongTriggered = true
                                playerOptions = true
                                show()
                                okHoldJob = null
                            }
                        }
                        true
                    }
                    d != null -> { if (digits.length < 4) digits += d; true }
                    k == Key.ChannelUp -> { requestZap(1); true }
                    k == Key.ChannelDown -> { requestZap(-1); true }
                    k == Key.DirectionUp && !drawer && !guideDrawer && !isVod -> { requestZap(1); true }
                    k == Key.DirectionDown && !drawer && !guideDrawer && !isVod -> { requestZap(-1); true }
                    k == Key.DirectionLeft && isVod -> { player.seekTo((player.currentPosition - 10_000).coerceAtLeast(0)); show(); true }
                    k == Key.DirectionRight && isVod -> { player.seekTo(player.currentPosition + 10_000); show(); true }
                    k == Key.DirectionLeft && !isVod && !drawer -> { guideDrawer = false; drawer = true; show(); true }
                    k == Key.DirectionRight && !isVod && !drawer && !guideDrawer -> { drawer = false; guideDrawer = true; show(); true }
                    k == Key.DirectionCenter && drawer -> false
                    k == Key.MediaPlayPause -> { player.playWhenReady = !player.playWhenReady; true }
                    else -> false
                }
            }
    ) {
        if (useShaka && c != null) {
            ShakaSurface(c, Modifier.fillMaxSize())
        } else {
            AndroidView(
                factory = { x ->
                    PlayerView(x).apply {
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        setShutterBackgroundColor(android.graphics.Color.BLACK)
                        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                        isFocusable = false
                        isFocusableInTouchMode = false
                        this.player = player
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { view ->
                    if (view.player !== player) view.player = player
                    view.resizeMode = when (pp.str("aspectRatio", "fit")) {
                        "fill" -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                        "zoom" -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                },
            )
        }

        if (!useShaka && buffering && error == null && pp.str("showBuffer", "on") != "off") Box(Modifier.align(Alignment.Center)) {
            CircularProgressIndicator(color = Blue, strokeWidth = 4.dp, modifier = Modifier.size(64.dp))
        }
        error?.let {
            Column(Modifier.align(Alignment.Center).clip(RoundedCornerShape(18.dp)).background(Color(0xE6101830)).padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(it, color = Color.White, fontSize = 26.sp, fontFamily = Sans, fontWeight = FontWeight.Bold)
                Text("Pulsa ▲/▼ para cambiar de canal o Atrás para salir", color = Color(0xFFB9C7E6), fontSize = 18.sp, fontFamily = Sans, modifier = Modifier.padding(top = 8.dp))
            }
        }
        if (digits.isNotEmpty()) Text(
            digits, color = Color.White, fontSize = 72.sp, fontWeight = FontWeight.Black, fontFamily = Sans,
            modifier = Modifier.align(Alignment.TopEnd).padding(48.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xB3000000)).padding(horizontal = 28.dp, vertical = 10.dp),
        )

        if (overlay && c != null) {
            val videoFormat = if (useShaka) null else player.videoFormat
            val fps = videoFormat?.frameRate?.takeIf { it > 0f }?.let { "%.0f FPS".format(it) } ?: "FPS —"
            val rate = videoFormat?.bitrate?.takeIf { it > 0 }?.let { "${it / 1000} kbps" } ?: "Bitrate —"
            val osdInfo = PlayerOsdInfo(
                channelNumber = if (isVod) "VOD" else "%03d".format(c.number),
                channelName = c.name,
                qualityBadge = streamQuality,
                fpsBadge = fps,
                bitrateBadge = rate,
                currentTimeRange = now?.let { hm(it.start) + " – " + hm(it.stop) } ?: "",
                currentProgram = if (isVod) "Reproducción de contenido" else now?.title ?: "Sin información EPG",
                nextTimeRange = next?.let { hm(it.start) + " – " + hm(it.stop) } ?: "",
                nextProgram = if (isVod) "" else next?.title ?: "Sin siguiente programa",
                currentClock = clock(),
                remainingTimeText = now?.let { "Restan ${mmss((it.stop - System.currentTimeMillis()).coerceAtLeast(0))}" } ?: "",
                progress = EpgStore.progress(now),
            )
            LivePlayerOsdOverlay(visible = true, channel = c, info = osdInfo, modifier = Modifier.align(Alignment.BottomCenter))
        }

        if (drawer) ChannelDrawer(vm) { drawer = false }
        if (guideDrawer) GuideDrawer(vm) { guideDrawer = false }
        if (playerOptions) PlayerOptionsDialog(vm, useShaka) { playerOptions = false }
    }
}

private data class PlayerOsdInfo(
    val channelNumber: String,
    val channelName: String,
    val qualityBadge: String,
    val fpsBadge: String,
    val bitrateBadge: String,
    val currentTimeRange: String,
    val currentProgram: String,
    val nextTimeRange: String,
    val nextProgram: String,
    val currentClock: String,
    val remainingTimeText: String,
    val progress: Float,
)

@Composable
private fun LivePlayerOsdOverlay(visible: Boolean, channel: Channel, info: PlayerOsdInfo, modifier: Modifier = Modifier) {
    val compact = LocalConfiguration.current.screenWidthDp < 600
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
        modifier = modifier,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = if (compact) 7.dp else 24.dp, vertical = if (compact) 7.dp else 18.dp)
                .clip(RoundedCornerShape(if (compact) 12.dp else 18.dp))
                .background(Brush.horizontalGradient(listOf(Color(0xF20A1D37), Color(0xE6102A4C), Color(0xF2081629))))
                .border(1.dp, Color.White.copy(alpha = .16f), RoundedCornerShape(if (compact) 12.dp else 18.dp)),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = if (compact) 10.dp else 20.dp, vertical = if (compact) 9.dp else 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (compact) 9.dp else 18.dp),
            ) {
                Box(
                    Modifier.size(width = if (compact) 52.dp else 84.dp, height = if (compact) 48.dp else 64.dp)
                        .clip(RoundedCornerShape(9.dp)).background(Color(0xFF0E294B))
                        .border(1.dp, Color(0x6638BDF8), RoundedCornerShape(9.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (channel.logo.isNotBlank()) AsyncImage(channel.logo, null, Modifier.fillMaxSize().padding(5.dp), contentScale = ContentScale.Fit)
                    else Text(channel.name.take(2).uppercase(), color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 8.dp)) {
                        OsdBadge(info.channelNumber, compact)
                        Text(info.channelName, color = Color.White, fontSize = if (compact) 13.sp else 19.sp, fontWeight = FontWeight.Bold, fontFamily = Sans, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                        if (!compact) {
                            OsdBadge(info.qualityBadge, compact)
                            OsdBadge(info.fpsBadge, compact)
                            OsdBadge(info.bitrateBadge, compact)
                        }
                    }
                    Text(info.currentTimeRange + (if (info.currentTimeRange.isNotBlank()) "  ·  " else "") + info.currentProgram,
                        color = Color(0xFFBAE6FD), fontSize = if (compact) 10.sp else 15.sp, fontWeight = FontWeight.Bold, fontFamily = Sans, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (!compact) Text(info.nextTimeRange + (if (info.nextTimeRange.isNotBlank()) "  ·  " else "") + info.nextProgram,
                        color = Color.White.copy(alpha = .72f), fontSize = 12.sp, fontFamily = Sans, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(info.currentClock, color = Color.White, fontSize = if (compact) 11.sp else 18.sp, fontWeight = FontWeight.SemiBold, fontFamily = Sans, maxLines = 1)
                    if (info.remainingTimeText.isNotBlank()) Text(info.remainingTimeText, color = Color.White.copy(alpha = .78f), fontSize = if (compact) 9.sp else 12.sp, fontFamily = Sans, maxLines = 1)
                }
            }
            Box(Modifier.fillMaxWidth().height(if (compact) 3.dp else 4.dp).background(Color.White.copy(alpha = .16f))) {
                Box(Modifier.fillMaxWidth(info.progress.coerceIn(0f, 1f)).fillMaxHeight().background(Color(0xFFFACC15)))
            }
        }
    }
}

@Composable
private fun OsdBadge(text: String, compact: Boolean) {
    if (text.isBlank() || text == "—") return
    Box(Modifier.clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = .14f)).padding(horizontal = if (compact) 4.dp else 6.dp, vertical = 2.dp)) {
        Text(text, color = Color.White.copy(alpha = .92f), fontSize = if (compact) 8.sp else 10.sp, fontWeight = FontWeight.SemiBold, fontFamily = Sans, maxLines = 1)
    }
}

@Composable private fun Badge(t: String, col: Color) {
    val compact = LocalConfiguration.current.screenWidthDp < 600
    Text(t, color = col, fontSize = if (compact) 9.sp else 18.sp, fontWeight = FontWeight.Black, fontFamily = Sans,
        modifier = Modifier.border(1.5.dp, col, RoundedCornerShape(8.dp)).padding(horizontal = if (compact) 4.dp else 10.dp, vertical = 2.dp))
}

@Composable private fun ProgressBar(f: Float) {
    Box(Modifier.padding(top = 8.dp).fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0x33FFFFFF))) {
        Box(Modifier.fillMaxWidth(f.coerceIn(0f, 1f)).fillMaxHeight().background(Gold))
    }
}

@Composable
private fun ProgramInfoRow(tag: String, title: String, range: String?, current: Boolean, progress: Float, compact: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 12.dp)) {
            Text(tag, color = if (current) Gold else Cyan, fontSize = if (compact) 9.sp else 13.sp,
                fontWeight = FontWeight.Black, fontFamily = Sans, modifier = Modifier.width(if (compact) 42.dp else 66.dp))
            Text(title, color = Color.White, fontSize = if (compact) 11.sp else 17.sp,
                fontWeight = if (current) FontWeight.Bold else FontWeight.Medium, fontFamily = Sans,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            if (range != null) Text(range, color = Color(0xFFB9C7E6), fontSize = if (compact) 9.sp else 13.sp, fontFamily = Sans)
        }
        if (current) ProgressBar(progress)
    }
}

@Composable
private fun ChannelDrawer(vm: MainViewModel, onClose: () -> Unit) {
    val q = vm.queue.filterNot { it.isVod }
    val epgVer by vm.epgVersion.collectAsState()
    val currentId = vm.current?.id
    val groups = remember(q) { q.groupBy { it.group.ifBlank { "Sin categoría" } } }
    var selectedGroup by remember(groups) { mutableStateOf(groups.keys.firstOrNull().orEmpty()) }
    val channels = groups[selectedGroup].orEmpty()
    val compact = LocalConfiguration.current.screenWidthDp < 600
    Row(
        Modifier.fillMaxSize().background(Color(0x99000000))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClose() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            Modifier.fillMaxHeight().fillMaxWidth(if (compact) 0.98f else 0.90f)
                .widthIn(max = 980.dp).background(Color(0xF2070C1A))
                .border(1.dp, Color(0x554AA3FF)).padding(if (compact) 9.dp else 18.dp),
        ) {
            Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("CANALES EN VIVO", color = Blue, fontSize = if (compact) 14.sp else 20.sp, fontWeight = FontWeight.Black, fontFamily = Sans)
                Spacer(Modifier.weight(1f))
                Text("${q.size} canales", color = Color(0xFF9EACC2), fontSize = 11.sp, fontFamily = Sans)
            }
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LazyColumn(
                    Modifier.fillMaxHeight().weight(if (compact) 0.38f else 0.30f)
                        .clip(RoundedCornerShape(12.dp)).background(Color(0xFF101D31)).padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    groups.forEach { (group, list) ->
                        item(key = "group_$group") {
                            val active = selectedGroup == group
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp))
                                    .background(if (active) Color(0xFF1B5689) else Color(0xFF17263B))
                                    .dlClickable(RoundedCornerShape(9.dp), Color.White, 2.dp, 1.02f) { selectedGroup = group }
                                    .padding(horizontal = 9.dp, vertical = if (compact) 10.dp else 13.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(group, color = Color.White, fontSize = if (compact) 10.sp else 13.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium, fontFamily = Sans, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                Text(list.size.toString(), color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                LazyColumn(
                    Modifier.fillMaxHeight().weight(if (compact) 0.62f else 0.70f),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    itemsIndexed(channels, key = { _, ch -> ch.id }) { _, ch ->
                        val program = remember(ch, epgVer) { EpgStore.now(ch) }
                        val selected = ch.id == currentId
                        Column(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp))
                                .background(if (selected) Color(0x553B9CEB) else Color(0xFF101D31))
                                .border(1.dp, if (selected) Blue else Color(0x1FFFFFFF), RoundedCornerShape(11.dp))
                                .dlClickable(RoundedCornerShape(11.dp), Color.White, 2.dp, 1.015f) { vm.current = ch; onClose() }
                                .padding(horizontal = if (compact) 7.dp else 12.dp, vertical = if (compact) 7.dp else 10.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                                Text("%03d".format(ch.number), color = Blue, fontSize = if (compact) 10.sp else 14.sp, fontWeight = FontWeight.Black, fontFamily = Sans)
                                Box(Modifier.size(if (compact) 34.dp else 48.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF142747)), contentAlignment = Alignment.Center) {
                                    if (ch.logo.isNotBlank()) AsyncImage(ch.logo, null, Modifier.fillMaxSize().padding(3.dp), contentScale = ContentScale.Fit)
                                    else Text(ch.name.take(1), color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(ch.name, color = Color.White, fontSize = if (compact) 11.sp else 15.sp, fontWeight = FontWeight.Bold, fontFamily = Sans, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(program?.let { "${hm(it.start)} · ${it.title}" } ?: "Sin información EPG", color = Color(0xFFB9C7E6), fontSize = if (compact) 9.sp else 11.sp, fontFamily = Sans, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                if (selected) Text("EN VIVO", color = Color(0xFF63D99A), fontSize = 9.sp, fontWeight = FontWeight.Black)
                            }
                            if (program != null) ProgressBar(EpgStore.progress(program))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GuideDrawer(vm: MainViewModel, onClose: () -> Unit) {
    val channels = vm.queue.filterNot { it.isVod }
    val epgVer by vm.epgVersion.collectAsState()
    val current = vm.current
    Row(Modifier.fillMaxSize().background(Color(0x99000000)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClose() }, horizontalArrangement = Arrangement.End) {
        Column(Modifier.fillMaxHeight().fillMaxWidth(0.92f).widthIn(max = 680.dp).background(Color(0xF2070C1A)).padding(16.dp)) {
            Text("GUÍA DEL CANAL", color = Blue, fontSize = 17.sp, fontWeight = FontWeight.Black, fontFamily = Sans)
            Text(current?.name ?: "Selecciona un canal", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = Sans, modifier = Modifier.padding(top = 6.dp, bottom = 12.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(channels, key = { _, ch -> ch.id }) { _, ch ->
                    val schedule = remember(ch, epgVer) { EpgStore.schedule(ch, System.currentTimeMillis() - 15 * 60_000L, System.currentTimeMillis() + 4 * 60 * 60_000L) }
                    val selected = ch.id == current?.id
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(if (selected) Color(0x443B9CEB) else Color(0x14FFFFFF))
                        .border(1.dp, if (selected) Blue else Color(0x1FFFFFFF), RoundedCornerShape(12.dp))
                        .dlClickable(RoundedCornerShape(12.dp), Color.White, 2.dp, 1.01f) { vm.current = ch }
                        .padding(11.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF142747)), contentAlignment = Alignment.Center) {
                                if (ch.logo.isNotBlank()) AsyncImage(ch.logo, null, Modifier.fillMaxSize().padding(4.dp), contentScale = ContentScale.Fit)
                                else Text(ch.name.take(1), color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Column(Modifier.weight(1f)) {
                                Text(ch.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = Sans)
                                Text(if (selected) "CANAL ACTUAL" else "Seleccionar canal", color = if (selected) Cyan else Color(0xFF9EACC2), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (schedule.isEmpty()) Text("Sin información EPG", color = Color(0xFF9EACC2), fontSize = 11.sp)
                        schedule.take(4).forEach { pg ->
                            val active = System.currentTimeMillis() in pg.start until pg.stop
                            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(hm(pg.start), color = if (active) Cyan else Color(0xFF9EACC2), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(pg.title, color = Color.White, fontSize = 12.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (active) Text("AHORA", color = Color(0xFF63D99A), fontSize = 9.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerOptionsDialog(vm: MainViewModel, shaka: Boolean, onClose: () -> Unit) {
    val prefs by vm.playbackPrefs.collectAsState()
    val options = listOf(
        Triple("bitrateMode", "CALIDAD DE VIDEO", listOf("low" to "Baja", "medium" to "Media", "high" to "Alta", "auto" to "Automática")),
        Triple("aspectRatio", "AJUSTE DE PANTALLA", listOf("fit" to "Ajustar", "fill" to "Estirar", "zoom" to "Zoom")),
        Triple("audio", "IDIOMA DE AUDIO", listOf("auto" to "Automático", "es" to "Español", "en" to "English")),
        Triple("subs", "SUBTÍTULOS", listOf("off" to "Desactivados", "es" to "Español", "en" to "English")),
    )
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxWidth(0.92f).widthIn(max = 620.dp).clip(RoundedCornerShape(22.dp)).background(Color(0xF20B1424)).border(1.dp, Color(0x6656AAFF), RoundedCornerShape(22.dp)).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("OPCIONES DE REPRODUCCIÓN", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Black, fontFamily = Sans)
            options.forEach { (key, label, values) ->
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(label, color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = Sans)
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        values.forEach { (value, title) ->
                            val selected = prefs.str(key, when (key) { "bitrateMode" -> "auto"; "aspectRatio" -> "fit"; "audio" -> "auto"; else -> "off" }) == value
                            Box(Modifier.weight(1f).clip(RoundedCornerShape(9.dp)).background(if (selected) Color(0xFF245B8B) else Color(0xFF17263B)).border(1.dp, if (selected) Blue else Color(0x223FFFFF), RoundedCornerShape(9.dp)).dlClickable(RoundedCornerShape(9.dp), Color.White, 2.dp, 1f) { vm.putPlaybackPref(key, value) }.padding(vertical = 11.dp, horizontal = 5.dp), contentAlignment = Alignment.Center) {
                                Text(title, color = Color.White, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, fontFamily = Sans, maxLines = 1)
                            }
                        }
                    }
                }
            }
            if (shaka) Text("En Shaka, la selección de calidad, audio y subtítulos depende de las pistas que exponga el manifiesto. El ajuste de pantalla de ExoPlayer no modifica el WebView.", color = Color(0xFF9EACC2), fontSize = 10.sp, fontFamily = Sans)
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xFF245B8B)).dlClickable(RoundedCornerShape(10.dp), Color.White, 2.dp, 1f, onClick = onClose).padding(13.dp), contentAlignment = Alignment.Center) {
                Text("LISTO", color = Color.White, fontWeight = FontWeight.Black, fontFamily = Sans)
            }
        }
    }
}

