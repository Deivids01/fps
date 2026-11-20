package com.davidlg.iptvplayer.ui.classic

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalConfiguration
import com.davidlg.iptvplayer.MainViewModel
import com.davidlg.iptvplayer.Screen
import com.davidlg.iptvplayer.data.Channel
import com.davidlg.iptvplayer.data.EpgStore
import coil.compose.AsyncImage
import com.davidlg.iptvplayer.ui.Sans
import com.davidlg.iptvplayer.ui.dlClickable
import com.davidlg.iptvplayer.ui.profiles.ProfileAvatar
import com.davidlg.iptvplayer.ui.vod.ClassicVodTab
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class CTab(val label: String) { Live("TV en vivo"), Guide("Guía"), Vod("Películas y series"), Settings("Ajustes") }

/** Modo clásico (azul, estilo TV tradicional). */
@Composable
fun ClassicShell(vm: MainViewModel) {
    var tab by remember { mutableStateOf(CTab.Live) }
    val profile by vm.activeProfile.collectAsState()
    val playbackPrefs by vm.playbackPrefs.collectAsState()
    SideEffect { ClassicTimePrefs.use24h = playbackPrefs.str("timeFormat", "12") == "24" }
    val live by vm.live.collectAsState()
    val ids by vm.activeIds.collectAsState()
    val src by vm.sourceFilter.collectAsState()
    var clock by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { while (true) { clock = SimpleDateFormat(if (ClassicTimePrefs.use24h) "HH:mm" else "h:mm a", Locale("es", "MX")).format(Date()); delay(1000) } }

    val compact = LocalConfiguration.current.screenWidthDp < 900
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0B1E33), Color(0xFF070F1B), Color(0xFF050A12))))) {
        if (compact) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 5.dp)) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    CTab.values().forEach { t ->
                        val on = t == tab
                        Box(Modifier.clip(RoundedCornerShape(8.dp)).dlClickable(RoundedCornerShape(8.dp), Color.White, 2.dp, 1f) { if (t == CTab.Settings) vm.screen = Screen.Settings else tab = t }
                            .background(if (on) Color(0xFF173D63) else Color.Transparent).padding(horizontal = 9.dp, vertical = 8.dp)) {
                            Text(t.label, color = if (on) Color.White else Color(0xFFA0AEBF), fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = Sans, maxLines = 1)
                        }
                    }
                    Text(clock, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = Sans, modifier = Modifier.padding(horizontal = 7.dp))
                    Box(Modifier.clip(RoundedCornerShape(8.dp)).dlClickable(RoundedCornerShape(8.dp), Color.White, 2.dp, 1f) { vm.showProfileSwitcher() }.background(Color(0x1AFFFFFF)).padding(horizontal = 8.dp, vertical = 6.dp)) {
                        Text(profile.name, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = Sans)
                    }
                }
                Text(if (src == "all") "Fuente: Todas las listas · ${ids.size}" else "Fuente: ${vm.playlists.value.firstOrNull { it.id == src }?.name ?: "Lista"}",
                    color = ClassicDim, fontSize = 10.sp, fontFamily = Sans, modifier = Modifier.padding(top = 3.dp).dlClickable(RoundedCornerShape(6.dp), Color.White, 1.dp, 1f) { vm.cycleSource() })
            }
        } else {
            Row(Modifier.fillMaxWidth().height(100.dp).padding(horizontal = 56.dp), verticalAlignment = Alignment.CenterVertically) {
                CTab.values().forEach { t ->
                    val on = t == tab
                    Column(Modifier.padding(end = 28.dp).dlClickable(RoundedCornerShape(10.dp), Color.White, 3.dp, 1f) { if (t == CTab.Settings) vm.screen = Screen.Settings else tab = t }.padding(horizontal = 24.dp, vertical = 8.dp)) {
                        Text(t.label, color = if (on) Color.White else Color(0xFFA0AEBF), fontSize = 34.sp, fontWeight = FontWeight.Bold, fontFamily = Sans)
                        Box(Modifier.padding(top = 4.dp).width(if (on) 152.dp else 0.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(ClassicAcc))
                    }
                }
                Box(Modifier.weight(1f))
                Box(Modifier.dlClickable(RoundedCornerShape(14.dp), Color.White, 3.dp, 1f) { vm.cycleSource() }.background(Color(0x1AFFFFFF)).padding(horizontal = 24.dp, vertical = 10.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("FUENTE", color = ClassicDim, fontSize = 13.sp, fontFamily = Sans, letterSpacing = 3.sp)
                        Text(if (src == "all") "Todas las listas · ${ids.size}" else (vm.playlists.value.firstOrNull { it.id == src }?.name ?: "Lista"), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = Sans)
                    }
                }
                Text(clock, color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Bold, fontFamily = Sans, modifier = Modifier.padding(horizontal = 30.dp))
                Row(Modifier.dlClickable(RoundedCornerShape(14.dp), Color.White, 3.dp, 1f) { vm.showProfileSwitcher() }.background(Color(0x1AFFFFFF)).padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ProfileAvatar(profile.av, 44.dp)
                    Text(profile.name, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = Sans)
                }
            }
        }
        Box(Modifier.fillMaxSize()) {
            when (tab) {
                CTab.Live -> ClassicLive(vm, live)
                CTab.Guide -> ClassicGuide(vm, live)
                CTab.Vod -> ClassicVodTab(vm)
                CTab.Settings -> {}
            }
        }
    }
}

@Composable
private fun ClassicLive(vm: MainViewModel, live: List<Channel>) {
    if (LocalConfiguration.current.screenWidthDp < 900) {
        ClassicLiveCompact(vm, live)
        return
    }
    val epgVer by vm.epgVersion.collectAsState()
    val groups = remember(live) { live.groupBy { it.group.ifBlank { "Sin grupo" } } }
    var group by remember { mutableStateOf("") }
    val list = remember(live, group) { if (group.isEmpty()) live else groups[group].orEmpty() }
    var sel by remember { mutableStateOf<Channel?>(null) }
    val cur = sel?.takeIf { c -> list.any { it.id == c.id } } ?: list.firstOrNull()
    val now = remember(cur, epgVer) { cur?.let { EpgStore.now(it) } }
    val next = remember(cur, epgVer) { cur?.let { EpgStore.next(it) } }

    Column(Modifier.fillMaxSize().padding(horizontal = 56.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Row(Modifier.fillMaxWidth().height(315.dp), horizontalArrangement = Arrangement.spacedBy(46.dp)) {
            Box(
                Modifier.width(560.dp).fillMaxHeight().clip(RoundedCornerShape(18.dp))
                    .background(Brush.verticalGradient(listOf(cur?.let { classicHue(it.name) } ?: Color(0xFF243A55), Color(0xFF07111F))))
                    .border(1.dp, Color(0x554AA3FF), RoundedCornerShape(18.dp))
            ) {
                if (cur != null) {
                    if (!now?.icon.isNullOrBlank()) AsyncImage(
                        model = now?.icon, contentDescription = "Imagen del programa ${now?.title}",
                        contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
                    )
                    // El cuadro principal es exclusivamente visual: arte EPG + logo superpuesto.
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x16030A14), Color(0x6607101C)))))
                    Box(
                        Modifier.align(Alignment.BottomStart).padding(18.dp).size(112.dp)
                            .clip(RoundedCornerShape(15.dp)).background(Color(0xD910243A))
                            .border(1.dp, Color(0x88FFFFFF), RoundedCornerShape(15.dp)),
                        contentAlignment = Alignment.Center,
                    ) { ClassicLogo(cur.logo, cur.name, 100.dp, 96.dp, 46f) }
                } else Text("Agrega una lista para comenzar", color = Color.White, fontSize = 22.sp, fontFamily = Sans, modifier = Modifier.align(Alignment.Center))
            }
            if (cur != null) Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Text(classicPad(cur.number), color = ClassicAcc, fontSize = 30.sp, fontWeight = FontWeight.Bold, fontFamily = Sans, modifier = Modifier.border(2.dp, ClassicAcc, RoundedCornerShape(10.dp)).padding(horizontal = 18.dp, vertical = 4.dp))
                    Text(cur.name, color = Color.White, fontSize = 52.sp, fontWeight = FontWeight.Bold, fontFamily = Sans, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, false))
                    Text(cur.group, color = Color(0xFFB5C2D3), fontSize = 22.sp, fontFamily = Sans, modifier = Modifier.clip(RoundedCornerShape(50)).background(Color(0x1AFFFFFF)).padding(horizontal = 14.dp, vertical = 4.dp))
                }
                Text(now?.title ?: "Sin información EPG", color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Bold, fontFamily = Sans, modifier = Modifier.padding(top = 22.dp), maxLines = 1)
                if (now != null) {
                    Text(classicHm(now.start) + " – " + classicHm(now.stop), color = Color(0xFFB5C2D3), fontSize = 26.sp, fontFamily = Sans, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
                    ClassicBar(classicProgress(now.start, now.stop), 8.dp, Modifier.width(760.dp))
                    Text(now.desc, color = Color(0xFFB5C2D3), fontSize = 24.sp, fontFamily = Sans, maxLines = 2, modifier = Modifier.padding(top = 14.dp))
                }
                next?.let { Text("A continuación · ${classicHm(it.start)} ${it.title}", color = ClassicAcc, fontSize = 26.sp, fontFamily = Sans, modifier = Modifier.padding(top = 16.dp)) }
            }
        }
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
            LazyColumn(Modifier.width(410.dp).fillMaxHeight().clip(RoundedCornerShape(16.dp)).background(Color(0x990A1424))) {
                itemsIndexed(listOf("") + groups.keys.toList()) { _, g ->
                    val on = g == group
                    Row(
                        Modifier.fillMaxWidth().height(74.dp).dlClickable(RoundedCornerShape(0.dp), Color.White, 3.dp, 1f, { if (it) { group = g; sel = null } }) { group = g }
                            .background(if (on) Color(0xFF16324F) else Color.Transparent).padding(horizontal = 30.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(if (g.isEmpty()) "Todos los canales" else g, Modifier.weight(1f), color = if (on) Color.White else Color(0xFFA9B6C6), fontSize = 30.sp, fontWeight = FontWeight.Bold, fontFamily = Sans, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text((if (g.isEmpty()) live.size else groups[g]?.size ?: 0).toString(), color = ClassicDim, fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = Sans)
                    }
                }
            }
            val st = rememberLazyListState()
            LazyColumn(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(16.dp)).background(Color(0x66081019)), state = st) {
                itemsIndexed(list, key = { _, c -> c.id }) { _, c ->
                    val p = remember(c, epgVer) { EpgStore.now(c) }
                    val on = c.id == cur?.id
                    Row(
                        Modifier.fillMaxWidth().height(102.dp).dlClickable(RoundedCornerShape(0.dp), ClassicAcc, 3.dp, 1f, { if (it) sel = c }) { vm.play(c, list) }
                            .background(if (on) Color(0xFF16324F) else Color.Transparent).padding(horizontal = 30.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(30.dp),
                    ) {
                        Text(classicPad(c.number), Modifier.width(72.dp), color = if (on) Color.White else Color(0xFFA9B6C6), fontSize = 28.sp, fontWeight = FontWeight.Bold, fontFamily = Sans)
                        ClassicLogo(c.logo, c.name, 92.dp, 70.dp, 36f)
                        Column(Modifier.weight(1f)) {
                            Text(c.name, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold, fontFamily = Sans, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(p?.let { classicHm(it.start) + " " + it.title } ?: "Sin información EPG", color = Color(0xFFA9B6C6), fontSize = 24.sp, fontFamily = Sans, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (p != null) ClassicBar(classicProgress(p.start, p.stop), 5.dp, Modifier.width(560.dp).padding(top = 4.dp))
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x14FFFFFF)))
                }
            }
        }
    }
}

@Composable
private fun ClassicLiveCompact(vm: MainViewModel, live: List<Channel>) {
    val epgVer by vm.epgVersion.collectAsState()
    val groups = remember(live) { live.groupBy { it.group.ifBlank { "Sin grupo" } } }
    var group by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<Channel?>(null) }
    val width = LocalConfiguration.current.screenWidthDp
    val list = remember(live, group) { if (group.isBlank()) live else groups[group].orEmpty() }
    val current = selected?.takeIf { c -> list.any { it.id == c.id } } ?: list.firstOrNull()
    val now = remember(current, epgVer) { current?.let { EpgStore.now(it) } }
    val next = remember(current, epgVer) { current?.let { EpgStore.next(it) } }

    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
        // Inicio: artwork EPG como fondo y logotipo del canal en primer plano. La guía EPG no usa este artwork.
        Box(Modifier.fillMaxWidth().height(if (width < 600) 174.dp else 210.dp)
            .clip(RoundedCornerShape(17.dp)).background(Brush.horizontalGradient(listOf(Color(0xFF12365A), Color(0xFF0B1728))))
            .border(1.dp, Color(0x554AA3FF), RoundedCornerShape(17.dp))) {
            if (!now?.icon.isNullOrBlank()) AsyncImage(model = now?.icon, contentDescription = "Imagen del programa", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0x2207101C), Color(0x5507101C)))))
            Box(
                Modifier.align(Alignment.BottomStart).padding(12.dp)
                    .size(if (width < 600) 86.dp else 112.dp)
                    .clip(RoundedCornerShape(13.dp)).background(Color(0xD910243A))
                    .border(1.dp, Color(0x88FFFFFF), RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (current != null) ClassicLogo(current.logo, current.name, if (width < 600) 76.dp else 100.dp, if (width < 600) 76.dp else 100.dp, 38f)
                else Text("TV", color = ClassicAcc, fontSize = 28.sp, fontWeight = FontWeight.Black)
            }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            (listOf("" to "Todos (${live.size})") + groups.map { it.key to "${it.key} (${it.value.size})" }).forEach { (key, label) ->
                val active = group == key
                val shape = RoundedCornerShape(18.dp)
                Box(Modifier.clip(shape).background(if (active) Color(0xFF267CCB) else Color(0xFF172A43))
                    .border(1.dp, if (active) Color(0xFF75BCFF) else Color(0x223F9FFF), shape)
                    .dlClickable(shape, Color.White, 2.dp, 1f) { group = key; selected = null }
                    .padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text(label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = Sans, maxLines = 1)
                }
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(if (width < 600) 2 else 3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            gridItems(list, key = { it.id }) { c ->
                val program = remember(c, epgVer) { EpgStore.now(c) }
                val active = c.id == current?.id
                Column(
                    Modifier.fillMaxWidth().height(if (width < 600) 112.dp else 126.dp)
                        .clip(RoundedCornerShape(13.dp)).background(if (active) Color(0xFF174B79) else Color(0xFF10243A))
                        .border(1.dp, if (active) ClassicAcc else Color(0x222F80C8), RoundedCornerShape(13.dp))
                        .dlClickable(RoundedCornerShape(13.dp), ClassicAcc, 2.dp, 1.02f, { if (it) selected = c }) { vm.play(c, list) }
                        .padding(9.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        ClassicLogo(c.logo, c.name, 37.dp, 37.dp, 17f)
                        Column(Modifier.weight(1f)) {
                            Text(classicPad(c.number), color = ClassicAcc, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = Sans)
                            Text(c.name, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = Sans, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Text(program?.title ?: "Sin información EPG", color = ClassicDim, fontSize = 9.sp, fontFamily = Sans, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (program != null) ClassicBar(classicProgress(program.start, program.stop), 3.dp, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun ClassicGuide(vm: MainViewModel, live: List<Channel>) {
    val epgVer by vm.epgVersion.collectAsState()
    val compact = LocalConfiguration.current.screenWidthDp < 900
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(15_000)
        }
    }

    // Parrilla de seis horas en bloques fijos de 30 minutos. Los programas se
    // posicionan según sus horas reales, no como tarjetas consecutivas.
    val halfHour = 30L * 60_000L
    val from = (now / halfHour) * halfHour
    val until = from + 12L * halfHour
    val channelWidth = if (compact) 132.dp else 250.dp
    val slotWidth = if (compact) 132.dp else 180.dp
    val rowHeight = if (compact) 86.dp else 104.dp
    val timelineWidth = slotWidth * 12
    val pixelsPerMinute = slotWidth.value / 30f
    val channels = remember(live) { live.take(250) }
    val timelineScroll = rememberScrollState()

    Column(Modifier.fillMaxSize().padding(horizontal = if (compact) 8.dp else 26.dp, vertical = if (compact) 7.dp else 14.dp)) {
        Text(
            "GUÍA DE PROGRAMACIÓN",
            color = ClassicAcc,
            fontSize = if (compact) 17.sp else 25.sp,
            fontWeight = FontWeight.Black,
            fontFamily = Sans,
            letterSpacing = 2.sp,
        )
        Text(
            "Parrilla televisiva · ${channels.size} canales · programación en tiempo real",
            color = Color(0xFFB8C9DC),
            fontSize = if (compact) 11.sp else 15.sp,
            fontFamily = Sans,
            modifier = Modifier.padding(top = 3.dp, bottom = if (compact) 8.dp else 12.dp),
        )

        Row(Modifier.fillMaxWidth().height(if (compact) 48.dp else 58.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.width(channelWidth).fillMaxHeight().padding(end = 6.dp)
                    .clip(RoundedCornerShape(8.dp)).background(Color(0xFF142B45)),
                contentAlignment = Alignment.Center,
            ) {
                Text("CANALES", color = ClassicDim, fontSize = if (compact) 11.sp else 16.sp, fontWeight = FontWeight.Black, fontFamily = Sans)
            }
            Row(
                Modifier.weight(1f).fillMaxHeight().horizontalScroll(timelineScroll),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for (i in 0 until 12) {
                    Box(
                        Modifier.width(slotWidth).fillMaxHeight().padding(end = 2.dp)
                            .clip(RoundedCornerShape(7.dp)).background(Color(0xFF142B45)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            classicHm(from + i * halfHour),
                            color = if (from + i * halfHour <= now && now < from + (i + 1) * halfHour) Color.White else ClassicDim,
                            fontSize = if (compact) 11.sp else 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = Sans,
                        )
                    }
                }
            }
        }

        LazyColumn(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(if (compact) 5.dp else 7.dp),
        ) {
            itemsIndexed(channels, key = { _, c -> c.id }) { _, channel ->
                val programs = remember(channel, epgVer, from / 60_000L) {
                    EpgStore.schedule(channel, from, until)
                }
                Row(
                    Modifier.fillMaxWidth().height(rowHeight),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        Modifier.width(channelWidth).fillMaxHeight().padding(end = 6.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(Color(0xFF142B45))
                            .border(1.dp, Color(0x334AA3FF), RoundedCornerShape(9.dp))
                            .dlClickable(RoundedCornerShape(9.dp), ClassicAcc, 2.dp, 1f) { vm.play(channel, live) }
                            .padding(horizontal = if (compact) 5.dp else 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(if (compact) 5.dp else 9.dp),
                    ) {
                        Text(classicPad(channel.number), color = ClassicAcc, fontSize = if (compact) 10.sp else 14.sp, fontWeight = FontWeight.Bold, fontFamily = Sans)
                        ClassicLogo(channel.logo, channel.name, if (compact) 38.dp else 54.dp, if (compact) 36.dp else 50.dp, 16f)
                        Text(
                            channel.name,
                            color = Color.White,
                            fontSize = if (compact) 10.sp else 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = Sans,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    }

                    // La columna temporal conserva una anchura fija; cada bloque
                    // se ubica por su timestamp dentro de la ventana de seis horas.
                    Box(
                        Modifier.weight(1f).fillMaxHeight().horizontalScroll(timelineScroll),
                    ) {
                        Box(Modifier.width(timelineWidth).fillMaxHeight()) {
                            for (i in 0 until 12) {
                                Box(
                                    Modifier.offset(x = slotWidth * i).width(1.dp).fillMaxHeight()
                                        .background(Color(0x223F9FFF)),
                                )
                            }

                            if (programs.isEmpty()) {
                                Box(
                                    Modifier.fillMaxSize().padding(3.dp).clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF1A3554)),
                                    contentAlignment = Alignment.CenterStart,
                                ) {
                                    Text("Sin información EPG", color = ClassicDim, fontSize = if (compact) 10.sp else 13.sp, modifier = Modifier.padding(horizontal = 10.dp))
                                }
                            } else {
                                programs.forEach { program ->
                                    val safeStop = if (program.stop > program.start) program.stop else program.start + halfHour
                                    val clippedStart = maxOf(program.start, from)
                                    val clippedStop = minOf(safeStop, until)
                                    if (clippedStop > clippedStart) {
                                        val left = ((clippedStart - from) / 60_000f * pixelsPerMinute).coerceAtLeast(0f)
                                        val width = (((clippedStop - clippedStart) / 60_000f) * pixelsPerMinute).coerceAtLeast(38f)
                                        val isCurrent = now >= program.start && now < safeStop
                                        Column(
                                            Modifier.offset(x = left.dp).width(width.dp).fillMaxHeight()
                                                .padding(end = 3.dp, top = 2.dp, bottom = 2.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isCurrent) Color(0xFF176EAC) else Color(0xFF29496E))
                                                .border(1.dp, if (isCurrent) ClassicAcc else Color(0x334AA3FF), RoundedCornerShape(8.dp))
                                                .dlClickable(RoundedCornerShape(8.dp), Color.White, 2.dp, 1f) { vm.play(channel, live) }
                                                .padding(horizontal = if (compact) 6.dp else 9.dp, vertical = if (compact) 5.dp else 8.dp),
                                            verticalArrangement = Arrangement.Center,
                                        ) {
                                            Text(
                                                if (isCurrent) "AHORA · ${classicHm(program.start)}" else classicHm(program.start),
                                                color = if (isCurrent) Color.White else Color(0xFFD4E2F0),
                                                fontSize = if (compact) 8.sp else 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = Sans,
                                                maxLines = 1,
                                            )
                                            Text(
                                                program.title,
                                                color = Color.White,
                                                fontSize = if (compact) 10.sp else 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = Sans,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(top = 2.dp),
                                            )
                                            if (isCurrent) {
                                                ClassicBar(classicProgress(program.start, safeStop), 3.dp, Modifier.fillMaxWidth().padding(top = 4.dp))
                                            } else {
                                                Text(classicHm(safeStop), color = ClassicDim, fontSize = if (compact) 8.sp else 10.sp, fontFamily = Sans, maxLines = 1)
                                            }
                                        }
                                    }
                                }
                            }

                            val currentOffset = ((now - from) / 60_000f * pixelsPerMinute).coerceIn(0f, timelineWidth.value)
                            Box(
                                Modifier.offset(x = currentOffset.dp).width(2.dp).fillMaxHeight()
                                    .background(Color(0xFFFFC857)),
                            )
                        }
                    }
                }
            }
        }
    }
}
