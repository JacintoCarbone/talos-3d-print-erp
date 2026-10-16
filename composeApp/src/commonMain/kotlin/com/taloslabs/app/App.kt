package com.taloslabs.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.room.Room
import com.taloslabs.app.data.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalUriHandler
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlin.math.*

private val Bg = Color(0xFF14171C)
private val Surface = Color(0xFF1C2028)
private val Surface2 = Color(0xFF232833)
private val Border = Color(0xFF2A2F3A)
private val Text = Color(0xFFEDEFF2)
private val TextDim = Color(0xFF8B92A0)
private val Amber = Color(0xFFFFA23E)
private val Teal = Color(0xFF3FC9AF)
private val Red = Color(0xFFFF6B6B)
private val Green = Color(0xFF4ADE80)

private fun money(n: Double): String =
    "$" + String.format(Locale("es", "UY"), "%.2f", n)

private fun weight(g: Double): String =
    if (g >= 1000) String.format(Locale("es", "UY"), "%.2f kg", g / 1000.0)
    else "${round(g).toInt()} g"

private fun dateText(iso: String): String =
    iso.split("-").let { if (it.size == 3) "${it[2]}/${it[1]}/${it[0]}" else iso }

private fun hexToColor(hex: String): Color {
    return try {
        val color = Color(android.graphics.Color.parseColor(hex))
        // Si el color es demasiado oscuro, devolvemos un gris visible para UI si es necesario,
        // pero aquí mantenemos la integridad del color guardado.
        color
    } catch (e: Exception) { Color(0xFF6B7280) }
}

// Helper para asegurar que el color sea visible sobre el fondo oscuro
private fun Color.toUiColor(): Color {
    return if (this.luminance() < 0.05f) Color(0xFF333333) else this
}

private val talosColors = listOf(
    "Blanco" to Color(0xFFFFFFFF),
    "Negro" to Color(0xFF000000),
    "Rojo" to Color(0xFFE5484D),
    "Amarillo" to Color(0xFFF5D90A),
    "Naranja" to Color(0xFFF2762E),
    "Rosado" to Color(0xFFF783C7),
    "Rosa" to Color(0xFFFFC0CB),
    "Fucsia" to Color(0xFFFF00FF),
    "Celeste" to Color(0xFF7DD3FC),
    "Cyan" to Color(0xFF00FFFF),
    "Azul Oscuro" to Color(0xFF001F3F),
    "Azul" to Color(0xFF3B82F6),
    "Verde Oscuro" to Color(0xFF003300),
    "Verde" to Color(0xFF3DBE64),
    "Lima" to Color(0xFF32CD32),
    "Marrón" to Color(0xFF7A4E2D),
    "Púrpura" to Color(0xFF9B5DE5),
    "Lila" to Color(0xFFC8A2C8),
    "Violeta" to Color(0xFF8F00FF),
    "Gris" to Color(0xFF8A93A3),
    "Dorado" to Color(0xFFD4A94E),
    "Turquesa" to Color(0xFF00CED1),
    "Bordó" to Color(0xFF800000),
    "Beige" to Color(0xFFF5F5DC),
    "Crema" to Color(0xFFFFFDD0),
    "Bronce" to Color(0xFFCD7F32),
    "Cobre" to Color(0xFFB87333),
    "Plateado" to Color(0xFFC0C0C0),
    "Ocre" to Color(0xFFCC7722),
    "Lavanda" to Color(0xFFE6E6FA),
    "Salmón" to Color(0xFFFA8072),
    "Esmeralda" to Color(0xFF50C878)
)

private fun colorHex(name: String): Color {
    val n = name.trim().lowercase()
    return talosColors.find { it.first.lowercase() == n }?.second
        ?: talosColors.find { n.contains(it.first.lowercase()) }?.second
        ?: Color(0xFF6B7280)
}

private fun nameFromColor(color: Color): String {
    return talosColors.minByOrNull { (_, c) ->
        (color.red - c.red).toDouble().pow(2) + 
        (color.green - c.green).toDouble().pow(2) + 
        (color.blue - c.blue).toDouble().pow(2)
    }?.first ?: "Personalizado"
}

private fun Color.toHex(): String = String.format("#%02X%02X%02X", (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt())

@Composable
fun ColorWheelDialog(initialColor: Color, onColorPicked: (Color) -> Unit, onDismiss: () -> Unit) {
    var hsv by remember {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(initialColor.toArgb(), hsv)
        mutableStateOf(hsv)
    }
    val currentColor = Color.hsv(hsv[0], hsv[1], hsv[2])

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onColorPicked(currentColor); onDismiss() }) { Text("Seleccionar", color = Amber) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        title = { Text("Afinar color") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Rueda de Tono (Hue)
                Box(Modifier.size(180.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize().pointerInput(Unit) {
                        fun updateHue(pos: Offset) {
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val angle = atan2(pos.y - center.y, pos.x - center.x) * (180f / PI.toFloat())
                            val hue = (angle + 360f) % 360f
                            hsv = floatArrayOf(hue, hsv[1], hsv[2])
                        }
                        detectDragGestures { change, _ -> 
                            updateHue(change.position)
                            change.consume()
                        }
                        detectTapGestures { pos -> updateHue(pos) }
                    }) {
                        drawCircle(
                            brush = Brush.sweepGradient(
                                listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
                            )
                        )
                        // Indicador de posición en la rueda
                        val radius = size.minDimension / 2f
                        val angleRad = hsv[0] * (PI.toFloat() / 180f)
                        val x = center.x + cos(angleRad) * radius * 0.8f
                        val y = center.y + sin(angleRad) * radius * 0.8f
                        drawCircle(Color.White, radius = 10.dp.toPx(), center = Offset(x, y))
                        drawCircle(Color.Black, radius = 10.dp.toPx(), center = Offset(x, y), style = Stroke(2.dp.toPx()))
                    }
                }

                // Barras de ajuste
                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Intensidad", fontSize = 11.sp, color = TextDim)
                        Text("${(hsv[1] * 100).toInt()}%", fontSize = 11.sp, color = TextDim)
                    }
                    Slider(
                        value = hsv[1],
                        onValueChange = { hsv = floatArrayOf(hsv[0], it, hsv[2]) },
                        colors = SliderDefaults.colors(thumbColor = currentColor, activeTrackColor = currentColor)
                    )
                }

                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Brillo", fontSize = 11.sp, color = TextDim)
                        Text("${(hsv[2] * 100).toInt()}%", fontSize = 11.sp, color = TextDim)
                    }
                    Slider(
                        value = hsv[2],
                        onValueChange = { hsv = floatArrayOf(hsv[0], hsv[1], it) },
                        colors = SliderDefaults.colors(thumbColor = currentColor, activeTrackColor = currentColor)
                    )
                }
                
                // Muestra del color final
                Box(Modifier.fillMaxWidth().height(50.dp).background(currentColor, RoundedCornerShape(12.dp)).border(2.dp, Border, RoundedCornerShape(12.dp)))
            }
        }
    )
}

@Composable
fun ColorPaletteSelector(selectedHex: String, onColorSelected: (String?) -> Unit) {
    var showWheel by remember { mutableStateOf(false) }
    val currentColor = hexToColor(selectedHex)

    if (showWheel) {
        ColorWheelDialog(currentColor, onColorPicked = { onColorSelected(it.toHex()) }, onDismiss = { showWheel = false })
    }

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier
                .size(56.dp)
                .background(currentColor, RoundedCornerShape(8.dp))
                .border(2.dp, Border, RoundedCornerShape(8.dp))
                .clickable { showWheel = true },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Palette, null, tint = if (currentColor.luminance() > 0.5f) Color.Black else Color.White)
        }
        Column {
            Text("Personalizar tono", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("Ajusta el brillo y color exacto", color = TextDim, fontSize = 12.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutocompleteTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    suggestions: List<String>,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val filteredSuggestions = suggestions.filter { it.contains(value, ignoreCase = true) && it != value }.distinct()
    val focusManager = LocalFocusManager.current

    Box(modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                expanded = it.isNotEmpty()
            },
            label = { Text(label) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        if (expanded && filteredSuggestions.isNotEmpty()) {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                properties = PopupProperties(focusable = false),
                modifier = Modifier.fillMaxWidth(.8f).background(Surface2)
            ) {
                filteredSuggestions.take(5).forEach { suggestion ->
                    DropdownMenuItem(
                        text = { Text(suggestion, color = Text) },
                        onClick = {
                            onValueChange(suggestion)
                            expanded = false
                            focusManager.clearFocus()
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    value: String,
    onDateSelected: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    if (showDialog) {
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.of("UTC"))
                            .toLocalDate()
                            .toString()
                        onDateSelected(date)
                    }
                    showDialog = false
                }) { Text("Seleccionar") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Box(modifier.clickable { showDialog = true }) {
        OutlinedTextField(
            value = dateText(value),
            onValueChange = { },
            label = { Text(label) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            enabled = false,
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = Text,
                disabledBorderColor = Border,
                disabledLabelColor = TextDim,
                disabledLeadingIconColor = TextDim,
                disabledTrailingIconColor = TextDim
            ),
            trailingIcon = { Icon(Icons.Default.CalendarToday, null, tint = Amber) }
        )
        Box(Modifier.matchParentSize().clickable { showDialog = true })
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent { TalosApp() }
    }
}

@Composable
fun TalosApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val db = remember { TalosDatabase.get(context) }
    val dao = db.dao()
    val scope = rememberCoroutineScope()
    val filamentos by dao.filamentos().collectAsState(initial = null)
    val movimientos by dao.movimientos().collectAsState(initial = null)
    val pedidos by dao.pedidos().collectAsState(initial = null)
    val config by dao.config().collectAsState(initial = null)
    var tab by remember { mutableStateOf(0) }
    var toast by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (dao.countFilamentos() == 0) {
            dao.saveConfig(ConfigEntity())
        }
    }

    val filamentosList = filamentos ?: emptyList()
    val movimientosList = movimientos ?: emptyList()
    val pedidosList = pedidos ?: emptyList()
    val configEntity = config ?: ConfigEntity()

    val ventas = movimientosList.filter { it.tipo == "venta" }.sumOf { it.monto }
    val gastos = movimientosList.filter { it.tipo == "gasto" }.sumOf { it.monto }
    val balance = ventas - gastos

    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Bg,
            surface = Surface,
            primary = Amber,
            onBackground = Text,
            onSurface = Text
        )
    ) {
        Scaffold(
            containerColor = Bg,
            topBar = { Header(balance) },
            bottomBar = { NavigationBar(containerColor = Surface, modifier = Modifier.navigationBarsPadding()) {
                NavigationBarItem(tab == 0, { tab = 0 }, icon = { Icon(Icons.Default.Inventory2, null) }, label = { Text("Filamento") })
                NavigationBarItem(tab == 1, { tab = 1 }, icon = { Icon(Icons.Default.Assignment, null) }, label = { Text("Pedidos") })
                NavigationBarItem(tab == 2, { tab = 2 }, icon = { Icon(Icons.Default.AccountBalanceWallet, null) }, label = { Text("Registros") })
                NavigationBarItem(tab == 3, { tab = 3 }, icon = { Icon(Icons.Default.Calculate, null) }, label = { Text("Cotizar") })
            } },
            floatingActionButton = {}
        ) { pad ->
            Box(Modifier.padding(pad).fillMaxSize()) {
                when (tab) {
                    0 -> InventarioScreen(filamentosList, dao, scope, { toast = it })
                    1 -> PedidosScreen(pedidosList, dao, scope, { toast = it })
                    2 -> RegistrosScreen(movimientosList, pedidosList, filamentosList, dao, scope)
                    3 -> CotizarScreen(filamentosList, pedidosList, configEntity, dao, scope, { toast = it })
                }
                toast?.let {
                    LaunchedEffect(it) {
                        kotlinx.coroutines.delay(2200)
                        toast = null
                    }
                    Text(
                        it,
                        modifier = Modifier.align(Alignment.BottomCenter)
                            .padding(bottom = 72.dp)
                            .background(Surface2, RoundedCornerShape(20.dp))
                            .padding(horizontal = 16.dp, vertical = 9.dp),
                        color = Text,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun Header(balance: Double) {
    Row(
        Modifier.fillMaxWidth().background(Bg).statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LabyrinthMark(72.dp)
            Spacer(Modifier.width(14.dp))
            Column {
                Text("TALOS", fontSize = 22.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
                Text("Taller de impresión 3D", fontSize = 12.sp, color = TextDim)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("BALANCE", fontSize = 10.sp, color = TextDim)
            Text(money(balance), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (balance >= 0) Green else Red)
        }
    }
}

@Composable
fun StatusTag(estado: String) {
    val color = when(estado) {
        "pendiente" -> Red
        "imprimiendo" -> Amber
        "listo" -> Teal
        "entregado" -> Green
        else -> TextDim
    }
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(0.5.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = estado.uppercase(),
            color = color,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun StatusChip(label: String, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        color = if (active) Amber.copy(0.1f) else Surface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (active) Amber else Border)
    ) {
        Box(Modifier.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
            Text(label.substring(0, 1).uppercase() + label.substring(1), fontSize = 10.sp, color = if (active) Amber else TextDim)
        }
    }
}

@Composable
fun LabyrinthMark(size: androidx.compose.ui.unit.Dp) {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(id = R.drawable.ic_talos_brand),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(alpha = alpha)
        )
    }
}

@Composable
fun DashboardView(movimientos: List<MovimientoEntity>, pedidos: List<PedidoEntity>) {
    val totalVentas = movimientos.filter { it.tipo == "venta" }.sumOf { it.monto }
    val totalGastos = movimientos.filter { it.tipo == "gasto" }.sumOf { it.monto }
    
    val estadosCount = pedidos.groupBy { it.estado }
    
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(colors = CardDefaults.cardColors(Surface), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("DISTRIBUCIÓN DE CAJA", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Amber)
                Spacer(Modifier.height(16.dp))
                val maxVal = max(totalVentas, totalGastos).coerceAtLeast(1.0)
                StatBar("Ventas", totalVentas, Green, maxVal)
                StatBar("Gastos", totalGastos, Red, maxVal)
            }
        }
        
        Card(colors = CardDefaults.cardColors(Surface), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("ESTADO DE PEDIDOS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Amber)
                Spacer(Modifier.height(16.dp))
                listOf("pendiente", "imprimiendo", "listo", "entregado").forEach { st ->
                    val count = estadosCount[st]?.size ?: 0
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(st.uppercase(), fontSize = 11.sp, color = TextDim)
                        Text(count.toString(), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Text)
                    }
                }
            }
        }
        
        Card(colors = CardDefaults.cardColors(Surface), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("EFICIENCIA", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Amber)
                Spacer(Modifier.height(8.dp))
                val margen = if (totalVentas > 0) ((totalVentas - totalGastos) / totalVentas * 100) else 0.0
                Text(String.format(Locale.getDefault(), "%.1f%%", margen), fontSize = 32.sp, fontWeight = FontWeight.Black, color = Green)
                Text("Margen de ganancia", fontSize = 11.sp, color = TextDim)
            }
        }
    }
}

@Composable
fun StatBar(label: String, value: Double, color: Color, max: Double) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 11.sp, color = TextDim)
            Text(money(value), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Text)
        }
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().height(8.dp).background(Surface2, RoundedCornerShape(4.dp))) {
            Box(
                Modifier
                    .fillMaxWidth((value / max).toFloat())
                    .fillMaxHeight()
                    .background(color, RoundedCornerShape(4.dp))
            )
        }
    }
}

@Composable
fun InventarioScreen(
    filamentos: List<FilamentoEntity>,
    dao: TalosDao,
    scope: kotlinx.coroutines.CoroutineScope,
    toast: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var edit by remember { mutableStateOf<FilamentoEntity?>(null) }
    var newItem by remember { mutableStateOf(false) }

    val list = filamentos.filter {
        val q = query.lowercase()
        q.isBlank() || it.tipo.lowercase().contains(q) || it.marca.lowercase().contains(q) || it.color.lowercase().contains(q)
    }
    val total = filamentos.sumOf { it.gramos }

    Column(Modifier.fillMaxSize().padding(14.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("Carretes", filamentos.size.toString(), Modifier.weight(1f))
            StatCard("Stock total", weight(total), Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), placeholder = { Text("Buscar por tipo, marca o color…") }, singleLine = true)
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxSize()) {
            if (list.isEmpty()) {
                Column(
                    Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Inventory, null, modifier = Modifier.size(64.dp), tint = Surface2)
                    Spacer(Modifier.height(16.dp))
                    Text("No hay filamentos registrados", color = TextDim)
                    Text("Tocá el botón + para empezar", color = TextDim, fontSize = 12.sp)
                }
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 80.dp)) {
                items(list, key = { it.id }) { f ->
                    FilamentoCard(f, { edit = f }, {
                        scope.launch { dao.deleteFilamento(f) }
                        toast("Filamento eliminado")
                    })
                }
            }
            FloatingActionButton(
                onClick = { newItem = true },
                containerColor = Amber,
                modifier = Modifier.align(Alignment.BottomEnd)
            ) { Icon(Icons.Default.Add, null, tint = Color(0xFF1A1208)) }
        }
    }

    if (newItem) FilamentoDialog(null, filamentos, dao, scope) { newItem = false; toast("Filamento agregado") }
    edit?.let { item -> FilamentoDialog(item, filamentos, dao, scope) { edit = null; toast("Filamento guardado") } }
}

@Composable
fun FilamentoCard(f: FilamentoEntity, onEdit: () -> Unit, onDelete: () -> Unit) {
    val low = f.gramos <= 150
    val progress = (f.gramos / f.totalGramos).coerceIn(0.0, 1.0).toFloat()
    Row(
        Modifier.fillMaxWidth().background(Surface, RoundedCornerShape(14.dp)).clickable { onEdit() }.padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(contentAlignment = Alignment.Center) {
            val filamentColor = hexToColor(f.colorHex)
            val isVeryDark = filamentColor.luminance() < 0.05f
            
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(48.dp),
                color = filamentColor, // Usamos el color real (negro si es negro)
                strokeWidth = 3.dp,
                // Si es negro, usamos un fondo de barra gris claro para que el negro se vea por contraste
                trackColor = if (isVeryDark) Color(0xFF444444) else Border, 
                strokeCap = StrokeCap.Round
            )
            Box(
                Modifier
                    .size(34.dp)
                    .background(filamentColor, CircleShape)
                    .border(1.dp, if (isVeryDark) Color.White.copy(0.3f) else Border, CircleShape)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("${f.tipo} · ${f.marca}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(f.color, color = TextDim, fontSize = 12.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(weight(f.gramos), color = if (low) Red else Text, fontSize = 13.sp)
            if (low) Text("⚠ bajo", color = Red, fontSize = 10.sp)
        }
        IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = Red) }
    }
}

@Composable
fun PedidosScreen(
    pedidos: List<PedidoEntity>,
    dao: TalosDao,
    scope: kotlinx.coroutines.CoroutineScope,
    toast: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("todos") }
    var edit by remember { mutableStateOf<PedidoEntity?>(null) }
    var newItem by remember { mutableStateOf(false) }

    val list = pedidos.filter {
        val q = query.lowercase()
        (q.isBlank() || it.cliente.lowercase().contains(q) || it.descripcion.lowercase().contains(q)) &&
        (statusFilter == "todos" || it.estado == statusFilter)
    }
    val montoPendiente = pedidos.filter { it.estado != "entregado" }.sumOf { it.montoTotal }

    Column(Modifier.fillMaxSize().padding(14.dp)) {
        StatCard("Pendiente de cobro", money(montoPendiente), Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), placeholder = { Text("Buscar por cliente o descripción…") }, singleLine = true)
        Spacer(Modifier.height(12.dp))
        
        Row(
            Modifier.fillMaxWidth().padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Estado:", color = TextDim, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            listOf("todos", "pendiente", "imprimiendo", "listo", "entregado").forEach { st ->
                val color = when(st) {
                    "pendiente" -> Red
                    "imprimiendo" -> Amber
                    "listo" -> Teal
                    "entregado" -> Green
                    else -> TextDim
                }
                val isSelected = statusFilter == st
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .background(if (isSelected) color else color.copy(alpha = 0.2f), CircleShape)
                        .border(if (isSelected) 2.dp else 1.dp, if (isSelected) Text else color.copy(alpha = 0.4f), CircleShape)
                        .clickable { statusFilter = st },
                    contentAlignment = Alignment.Center
                ) {
                    if (st == "todos" && !isSelected) {
                        Icon(Icons.Default.FilterList, null, modifier = Modifier.size(14.dp), tint = TextDim)
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxSize()) {
            if (list.isEmpty()) {
                Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Assignment, null, modifier = Modifier.size(64.dp), tint = Surface2)
                    Spacer(Modifier.height(16.dp))
                    Text("No hay pedidos activos", color = TextDim)
                }
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 80.dp)) {
                items(list, key = { it.id }) { p ->
                    PedidoCard(p, { edit = p }, {
                        scope.launch {
                            val cotizacion = dao.filamentosDePedido(p.id)
                            cotizacion.forEach { dao.ajustarStock(it.filamentoId, it.gramos) }
                            dao.deletePedido(p)
                        }
                        toast("Pedido eliminado")
                    })
                }
            }
            FloatingActionButton(onClick = { newItem = true }, containerColor = Amber, modifier = Modifier.align(Alignment.BottomEnd)) {
                Icon(Icons.Default.Add, null, tint = Color(0xFF1A1208))
            }
        }
    }

    if (newItem) PedidoDialog(null, pedidos, dao, scope) { newItem = false; toast("Pedido creado") }
    edit?.let { item -> PedidoDialog(item, pedidos, dao, scope) { edit = null; toast("Pedido actualizado") } }
}

@Composable
fun PedidoCard(p: PedidoEntity, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(colors = CardDefaults.cardColors(Surface), modifier = Modifier.fillMaxWidth().clickable { onEdit() }) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(p.cliente, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.width(8.dp))
                    StatusTag(p.estado)
                }
                Text(p.descripcion, fontSize = 13.sp, color = Text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (p.telefono.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Phone, null, modifier = Modifier.size(12.dp), tint = TextDim)
                        Spacer(Modifier.width(4.dp))
                        Text(p.telefono, fontSize = 12.sp, color = TextDim)
                    }
                }
                Text("Fecha: ${dateText(p.fecha)}", fontSize = 11.sp, color = TextDim)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = money(p.montoTotal),
                    color = if (p.montoTotal > 0) Green else Red,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = Red) }
            }
        }
    }
}

@Composable
fun PedidoDialog(
    item: PedidoEntity?,
    allPedidos: List<PedidoEntity>,
    dao: TalosDao,
    scope: kotlinx.coroutines.CoroutineScope,
    close: () -> Unit
) {
    var cliente by remember { mutableStateOf(item?.cliente ?: "") }
    var descripcion by remember { mutableStateOf(item?.descripcion ?: "") }
    var telefono by remember { mutableStateOf(item?.telefono ?: "") }
    var monto by remember { mutableStateOf(item?.montoTotal?.toString() ?: "0") }
    var fecha by remember { mutableStateOf(item?.fecha ?: LocalDate.now().toString()) }
    var estado by remember { mutableStateOf(item?.estado ?: "pendiente") }

    val clientesSug = allPedidos.map { it.cliente }.distinct()

    AlertDialog(
        onDismissRequest = close,
        title = { Text(if (item == null) "Nuevo pedido" else "Editar pedido") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                AutocompleteTextField(cliente, { cliente = it }, "Cliente", clientesSug)
                OutlinedTextField(
                    value = telefono,
                    onValueChange = { if (it.all { c -> c.isDigit() }) telefono = it },
                    label = { Text("Teléfono / Celular") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(descripcion, { descripcion = it }, label = { Text("Descripción del trabajo") })
                OutlinedTextField(
                    value = monto,
                    onValueChange = { monto = it },
                    label = { Text("Monto total ($)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                DatePickerField(fecha, { fecha = it }, "Fecha")
                
                Text("Estado", fontWeight = FontWeight.Bold, color = Amber)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("pendiente", "imprimiendo", "listo", "entregado").forEach { st ->
                        StatusChip(st, estado == st, Modifier.weight(1f)) { estado = st }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (cliente.isNotBlank() && descripcion.isNotBlank()) {
                    scope.launch {
                        val newMonto = monto.toDoubleOrNull() ?: 0.0
                        val newPedido = PedidoEntity(item?.id ?: java.util.UUID.randomUUID().toString(), descripcion, cliente, telefono, fecha, estado, newMonto)
                        dao.upsertPedido(newPedido)
                        
                        if (estado == "entregado" && item?.estado != "entregado") {
                            val existingVenta = dao.movimientoDePedido(newPedido.id)
                            if (existingVenta == null) {
                                val configEntity = dao.config().first() ?: ConfigEntity()
                                val movimientoId = java.util.UUID.randomUUID().toString()
                                val cotizacion = dao.filamentosDePedido(newPedido.id)
                                
                                val totalGramosStr = if (cotizacion.isNotEmpty()) " (${cotizacion.sumOf { it.gramos }}g)" else ""
                                
                                dao.upsertMovimiento(
                                    MovimientoEntity(
                                        movimientoId,
                                        "venta",
                                        LocalDate.now().toString(),
                                        newPedido.montoTotal,
                                        "Pedido entregado: ${newPedido.cliente}$totalGramosStr · ${newPedido.descripcion}",
                                        newPedido.id
                                    )
                                )
                                
                                if (cotizacion.isNotEmpty()) {
                                    cotizacion.forEach { pf ->
                                        val costo = pf.gramos * configEntity.costoPorKiloFilamento / 1000.0
                                        dao.insertUso(VentaFilamentoEntity(java.util.UUID.randomUUID().toString(), movimientoId, pf.filamentoId, pf.gramos, costo))
                                    }
                                }
                            }
                        }
                    }
                    close()
                }
            }) { Text("Guardar", color = Amber) }
        },
        dismissButton = { TextButton(onClick = close) { Text("Cancelar") } }
    )
}

@Composable
fun FilamentoDialog(
    item: FilamentoEntity?,
    allFilamentos: List<FilamentoEntity>,
    dao: TalosDao,
    scope: kotlinx.coroutines.CoroutineScope,
    close: () -> Unit
) {
    var tipo by remember { mutableStateOf(item?.tipo ?: "") }
    var marca by remember { mutableStateOf(item?.marca ?: "") }
    var colorName by remember { mutableStateOf(item?.color ?: "") }
    var colorHexVal by remember { mutableStateOf(item?.colorHex ?: "#6B7280") }
    var gramos by remember { mutableStateOf((item?.gramos ?: 1000.0).toString()) }
    var totalGramos by remember { mutableStateOf((item?.totalGramos ?: 1000.0).toString()) }

    val tiposSug = allFilamentos.map { it.tipo }.distinct()
    val marcasSug = allFilamentos.map { it.marca }.distinct()

    AlertDialog(
        onDismissRequest = close,
        title = { Text(if (item == null) "Nuevo filamento" else "Editar filamento") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                AutocompleteTextField(tipo, { tipo = it }, "Tipo (ej. PLA, PETG)", tiposSug)
                AutocompleteTextField(marca, { marca = it }, "Marca (ej. Grilon, PrintALot)", marcasSug)
                
                OutlinedTextField(
                    value = colorName,
                    onValueChange = { 
                        colorName = it
                        val suggested = colorHex(it)
                        if (suggested != Color(0xFF6B7280)) {
                            colorHexVal = suggested.toHex()
                        }
                    },
                    label = { Text("Nombre del color") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                ColorPaletteSelector(colorHexVal) { hex ->
                    if (hex != null) {
                        colorHexVal = hex
                        colorName = nameFromColor(hexToColor(hex))
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = gramos,
                        onValueChange = { gramos = it },
                        label = { Text("Gramos hoy") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = totalGramos,
                        onValueChange = { totalGramos = it },
                        label = { Text("Total carrete") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (tipo.isNotBlank() && marca.isNotBlank() && colorName.isNotBlank()) {
                    scope.launch {
                        dao.upsertFilamento(
                            FilamentoEntity(
                                item?.id ?: java.util.UUID.randomUUID().toString(),
                                tipo,
                                marca,
                                colorName,
                                gramos.toDoubleOrNull() ?: 0.0,
                                totalGramos.toDoubleOrNull() ?: 1000.0,
                                colorHexVal
                            )
                        )
                    }
                    close()
                }
            }) { Text("Guardar", color = Amber) }
        },
        dismissButton = { TextButton(onClick = close) { Text("Cancelar") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrosScreen(
    movimientos: List<MovimientoEntity>,
    pedidos: List<PedidoEntity>,
    filamentos: List<FilamentoEntity>,
    dao: TalosDao,
    scope: kotlinx.coroutines.CoroutineScope
) {
    var filter by remember { mutableStateOf("todos") }
    var query by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<MovimientoEntity?>(null) }
    var adding by remember { mutableStateOf(false) }
    var viewMode by remember { mutableStateOf("lista") } // lista | dashboard

    val list = movimientos.filter {
        (filter == "todos" || it.tipo == filter) &&
        (query.isBlank() || it.comentario.contains(query, ignoreCase = true)) &&
        (selectedDate == null || it.fecha == selectedDate)
    }
    val ventas = movimientos.filter { it.tipo == "venta" }.sumOf { it.monto }
    val gastos = movimientos.filter { it.tipo == "gasto" }.sumOf { it.monto }
    val balance = ventas - gastos

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        selectedDate = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.of("UTC"))
                            .toLocalDate()
                            .toString()
                    }
                    showDatePicker = false
                }) { Text("Filtrar") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(Modifier.fillMaxSize().padding(14.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            StatCard("Ventas", money(ventas), Modifier.weight(1f), Green)
            StatCard("Gastos", money(gastos), Modifier.weight(1f), Red)
            StatCard("Balance", money(balance), Modifier.weight(1f), if (balance >= 0) Teal else Red)
        }
        Spacer(Modifier.height(12.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Buscar cliente…") },
                singleLine = true,
                trailingIcon = { if (query.isNotBlank()) IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, null) } }
            )
            
            OutlinedIconButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.size(54.dp),
                shape = RoundedCornerShape(12.dp),
                colors = IconButtonDefaults.outlinedIconButtonColors(
                    contentColor = if (selectedDate != null) Amber else TextDim,
                    containerColor = if (selectedDate != null) Amber.copy(.1f) else Color.Transparent
                ),
                border = BorderStroke(1.dp, if (selectedDate != null) Amber else Border)
            ) {
                Icon(if (selectedDate != null) Icons.Default.CalendarMonth else Icons.Default.CalendarToday, null)
            }

            if (selectedDate != null) {
                IconButton(onClick = { selectedDate = null }) {
                    Icon(Icons.Default.FilterListOff, "Quitar fecha", tint = Red)
                }
            }
        }

        if (selectedDate != null) {
            Text("Filtrando fecha: ${dateText(selectedDate!!)}", color = Amber, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            listOf("todos" to "Todos", "venta" to "Ventas", "gasto" to "Gastos").forEach { (id, label) ->
                FilterButton(label, filter == id, Modifier.weight(1f)) { filter = id }
            }
        }
        Spacer(Modifier.height(10.dp))
        
        // Selector de vista: Lista vs Dashboard
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { viewMode = "lista" },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (viewMode == "lista") Surface2 else Surface,
                    contentColor = Text
                )
            ) {
                Icon(Icons.AutoMirrored.Filled.List, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Lista", fontSize = 12.sp)
            }
            Button(
                onClick = { viewMode = "dashboard" },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (viewMode == "dashboard") Surface2 else Surface,
                    contentColor = Text
                )
            ) {
                Icon(Icons.Default.BarChart, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Panel", fontSize = 12.sp)
            }
        }
        
        Spacer(Modifier.height(10.dp))

        Box(Modifier.fillMaxSize()) {
            if (viewMode == "lista") {
                if (list.isEmpty()) {
                    Column(
                        Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.History, null, modifier = Modifier.size(64.dp), tint = Surface2)
                        Spacer(Modifier.height(16.dp))
                        Text("Sin registros de movimientos", color = TextDim)
                        Text("Tus ventas y gastos aparecerán aquí", color = TextDim, fontSize = 12.sp)
                    }
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 80.dp)) {
                    items(list, key = { it.id }) { m ->
                        MovimientoCard(m, {
                            editing = m
                        }, {
                            scope.launch { dao.deleteMovimiento(m) }
                        })
                    }
                }
            } else {
                DashboardView(movimientos, pedidos)
            }
            
            FloatingActionButton(onClick = { adding = true }, containerColor = Amber, modifier = Modifier.align(Alignment.BottomEnd)) {
                Icon(Icons.Default.Add, null, tint = Color(0xFF1A1208))
            }
        }
    }

    if (adding) MovimientoDialog(null, filamentos, dao, scope) { adding = false }
    editing?.let { MovimientoDialog(it, filamentos, dao, scope) { editing = null } }
}

@Composable
fun MovimientoCard(m: MovimientoEntity, edit: () -> Unit, delete: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(Surface, RoundedCornerShape(14.dp)).clickable { edit() }.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(34.dp).background(if (m.tipo == "venta") Green.copy(.12f) else Red.copy(.12f), RoundedCornerShape(9.dp)), contentAlignment = Alignment.Center) {
            Icon(if (m.tipo == "venta") Icons.Default.TrendingUp else Icons.Default.TrendingDown, null, tint = if (m.tipo == "venta") Green else Red, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(m.comentario.ifBlank { "(sin comentario)" }, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(dateText(m.fecha), color = TextDim, fontSize = 11.sp)
        }
        Text(if (m.tipo == "venta") "+" + money(m.monto) else "−" + money(m.monto), color = if (m.tipo == "venta") Green else Red, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        IconButton(onClick = delete) { Icon(Icons.Default.Delete, null, tint = Red) }
    }
}

data class UsoDraft(val filamentoId: String, var gramos: String)

@Composable
fun MovimientoDialog(
    item: MovimientoEntity?,
    filamentos: List<FilamentoEntity>,
    dao: TalosDao,
    scope: kotlinx.coroutines.CoroutineScope,
    close: () -> Unit
) {
    var tipo by remember { mutableStateOf(item?.tipo ?: "gasto") }
    var fecha by remember { mutableStateOf(item?.fecha ?: LocalDate.now().toString()) }
    var monto by remember { mutableStateOf(item?.monto?.toString() ?: "") }
    var comentario by remember { mutableStateOf(item?.comentario ?: "") }
    var usos by remember { mutableStateOf(listOf<UsoDraft>()) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(item?.id) {
        if (item != null) {
            usos = dao.usosDeVenta(item.id).map { UsoDraft(it.filamentoId, it.gramos.toString()) }
        }
        loaded = true
    }

    AlertDialog(
        onDismissRequest = close,
        title = { Text(if (item == null) "Nuevo movimiento" else "Editar movimiento") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    FilterButton("Gasto", tipo == "gasto", Modifier.weight(1f)) { tipo = "gasto" }
                    FilterButton("Venta", tipo == "venta", Modifier.weight(1f)) { tipo = "venta" }
                }
                DatePickerField(fecha, { fecha = it }, "Fecha")
                OutlinedTextField(
                    value = monto,
                    onValueChange = { monto = it },
                    label = { Text("Monto (pesos)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(comentario, { comentario = it }, label = { Text("Comentario") }, singleLine = true)

                if (tipo == "venta" && loaded) {
                    Text("Filamentos usados", fontWeight = FontWeight.Bold, color = Amber)
                    if (usos.isEmpty()) {
                        Text("Agregá uno o más filamentos. Esto descuenta gramos del stock.", color = TextDim, fontSize = 12.sp)
                    }
                    usos.forEachIndexed { index, uso ->
                        UsoRow(
                            uso = uso,
                            filamentos = filamentos,
                            onChange = { nuevo -> usos = usos.toMutableList().also { it[index] = nuevo } },
                            onRemove = { usos = usos.filterIndexed { i, _ -> i != index } }
                        )
                    }
                    TextButton(onClick = {
                        val first = filamentos.firstOrNull()
                        if (first != null) usos = usos + UsoDraft(first.id, "")
                    }) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(5.dp))
                        Text("Agregar filamento")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val amount = monto.toDoubleOrNull()
                if (amount != null && amount >= 0) {
                    scope.launch {
                        if (item?.tipo == "venta") {
                            dao.usosDeVenta(item.id).forEach { dao.ajustarStock(it.filamentoId, it.gramos) }
                            dao.deleteUsosDeVenta(item.id)
                        }
                        val movementId = item?.id ?: java.util.UUID.randomUUID().toString()
                        dao.upsertMovimiento(MovimientoEntity(movementId, tipo, fecha, amount, comentario))
                        if (tipo == "venta") {
                            usos.mapNotNull { u ->
                                val g = u.gramos.toDoubleOrNull() ?: return@mapNotNull null
                                if (g <= 0) null else {
                                    val costo = 0.0
                                    dao.ajustarStock(u.filamentoId, -g)
                                    VentaFilamentoEntity(java.util.UUID.randomUUID().toString(), movementId, u.filamentoId, g, costo)
                                }
                            }.forEach { dao.insertUso(it) }
                            dao.eliminarFilamentosAgotados()
                        }
                    }
                    close()
                }
            }) { Text("Guardar", color = Amber) }
        },
        dismissButton = { TextButton(onClick = close) { Text("Cancelar") } }
    )
}

@Composable
fun UsoRow(
    uso: UsoDraft,
    filamentos: List<FilamentoEntity>,
    onChange: (UsoDraft) -> Unit,
    onRemove: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        var expanded by remember { mutableStateOf(false) }
        val selected = filamentos.find { it.id == uso.filamentoId }
        Box(Modifier.weight(1.4f)) {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                if (selected != null) {
                    Text(
                        buildAnnotatedString {
                            append("${selected.tipo} · ")
                            withStyle(SpanStyle(color = hexToColor(selected.colorHex).toUiColor())) {
                                append(selected.color)
                            }
                            append(" (${weight(selected.gramos)})")
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text("Filamento")
                }
            }
            DropdownMenu(expanded, { expanded = false }) {
                filamentos.forEach { f ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                buildAnnotatedString {
                                    append("${f.tipo} · ${f.marca} · ")
                                    withStyle(SpanStyle(color = hexToColor(f.colorHex).toUiColor())) {
                                        append(f.color)
                                    }
                                    append(" (${weight(f.gramos)})")
                                }
                            )
                        },
                        onClick = { onChange(uso.copy(filamentoId = f.id)); expanded = false }
                    )
                }
            }
        }
        OutlinedTextField(
            value = uso.gramos,
            onValueChange = { onChange(uso.copy(gramos = it)) },
            label = { Text("g") },
            modifier = Modifier.width(80.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        IconButton(onClick = onRemove) { Icon(Icons.Default.Delete, null, tint = Red) }
    }
}

@Composable
fun CotizarScreen(
    filamentos: List<FilamentoEntity>,
    pedidos: List<PedidoEntity>,
    config: ConfigEntity,
    dao: TalosDao,
    scope: kotlinx.coroutines.CoroutineScope,
    toast: (String) -> Unit
) {
    var horas by remember { mutableStateOf("0") }
    var extras by remember { mutableStateOf("0") }
    var cantidad by remember { mutableStateOf("1") }
    var showConfig by remember { mutableStateOf(false) }
    var usos by remember { mutableStateOf(listOf<UsoDraft>()) }
    var selectedPedidoId by remember { mutableStateOf<String?>(null) }
    var expandPedidos by remember { mutableStateOf(false) }
    var isPersonalized by remember { mutableStateOf(false) }

    val h = horas.toDoubleOrNull() ?: 0.0
    val x = max(1.0, cantidad.toDoubleOrNull() ?: 1.0)
    val costoHora = config.costoPorHora * h

    val costoFilamentos = usos.sumOf { u ->
        val g = u.gramos.toDoubleOrNull() ?: 0.0
        g * config.costoPorKiloFilamento / 1000.0
    }

    val costoTotal = costoHora + costoFilamentos + (extras.toDoubleOrNull() ?: 0.0)
    val recargo = if (isPersonalized) 1.0 + (config.porcentajePersonalizacion / 100.0) else 1.0
    val precioUnitario = (costoTotal * (1 + config.porcentajeGanancia / 100.0)) * recargo
    val precioTotal = precioUnitario * x
    val ganancia = precioUnitario - costoTotal

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp)) {
        TextButton(onClick = { showConfig = !showConfig }) {
            Text("Parámetros del taller", color = Text)
            Icon(Icons.Default.ExpandMore, null, modifier = Modifier.rotate(if (showConfig) 180f else 0f))
        }
        if (showConfig) {
            Card(colors = CardDefaults.cardColors(Surface), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    OutlinedTextField(
                        value = config.costoPorHora.toString(),
                        onValueChange = { scope.launch { dao.saveConfig(config.copy(costoPorHora = it.toDoubleOrNull() ?: config.costoPorHora)) } },
                        label = { Text("Costo por hora ($)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = config.costoPorKiloFilamento.toString(),
                        onValueChange = { scope.launch { dao.saveConfig(config.copy(costoPorKiloFilamento = it.toDoubleOrNull() ?: config.costoPorKiloFilamento)) } },
                        label = { Text("Costo por kilo de filamento ($)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = config.porcentajeGanancia.toString(),
                        onValueChange = { scope.launch { dao.saveConfig(config.copy(porcentajeGanancia = it.toDoubleOrNull() ?: config.porcentajeGanancia)) } },
                        label = { Text("Porcentaje de ganancia (%)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = config.porcentajePersonalizacion.toString(),
                        onValueChange = { scope.launch { dao.saveConfig(config.copy(porcentajePersonalizacion = it.toDoubleOrNull() ?: config.porcentajePersonalizacion)) } },
                        label = { Text("Porcentaje personalización (%)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    
                    val uriHandler = LocalUriHandler.current
                    TextButton(
                        onClick = { uriHandler.openUri("https://sites.google.com/view/taloslabs-talos-privacidad/inicio") },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Políticas de privacidad", fontSize = 10.sp, color = TextDim)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        Card(colors = CardDefaults.cardColors(Surface), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Filamentos usados", fontWeight = FontWeight.Bold)
                Text("Podés agregar varios colores y poner los gramos de cada uno.", color = TextDim, fontSize = 12.sp)
                usos.forEachIndexed { i, uso ->
                    UsoRow(uso, filamentos,
                        { n -> usos = usos.toMutableList().also { it[i] = n } },
                        { usos = usos.filterIndexed { idx, _ -> idx != i } }
                    )
                }
                TextButton(onClick = {
                    filamentos.firstOrNull()?.let { usos = usos + UsoDraft(it.id, "") }
                }) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(5.dp))
                    Text("Agregar filamento")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = horas,
                        onValueChange = { horas = it },
                        label = { Text("Horas") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = extras,
                        onValueChange = { extras = it },
                        label = { Text("Gastos extras ($)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = cantidad.toString(),
                        onValueChange = { cantidad = it },
                        label = { Text("Cantidad") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Spacer(Modifier.weight(1f))
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(isPersonalized, { isPersonalized = it }, colors = SwitchDefaults.colors(checkedThumbColor = Amber))
                    Spacer(Modifier.width(8.dp))
                    Text("Pieza personalizada (+${config.porcentajePersonalizacion.toInt()}%)", fontSize = 14.sp)
                }
            }
        }

        Spacer(Modifier.height(15.dp))
        Text("Relacionar con pedido (opcional)", fontWeight = FontWeight.Bold, color = Amber, fontSize = 12.sp)
        val selPed = pedidos.find { it.id == selectedPedidoId }
        Box {
            OutlinedButton(onClick = { expandPedidos = true }, modifier = Modifier.fillMaxWidth()) {
                Text(selPed?.let { "${it.cliente}: ${it.descripcion}" } ?: "Venta independiente (sin pedido)", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            DropdownMenu(expandPedidos, { expandPedidos = false }) {
                DropdownMenuItem(text = { Text("Venta independiente") }, onClick = { selectedPedidoId = null; expandPedidos = false })
                pedidos.filter { it.estado == "pendiente" }.forEach { p ->
                    DropdownMenuItem(text = { Text("${p.cliente}: ${p.descripcion}") }, onClick = { selectedPedidoId = p.id; expandPedidos = false })
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Card(colors = CardDefaults.cardColors(Surface), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                RowCalc("Costo de hora", money(costoHora))
                RowCalc("Costo de filamento", money(costoFilamentos))
                RowCalc("Gastos extras", money(extras.toDoubleOrNull() ?: 0.0))
                HorizontalDivider(color = Border, modifier = Modifier.padding(vertical = 7.dp))
                RowCalc("Costo total", money(costoTotal), true)
                RowCalc("Precio de venta", money(precioUnitario), true, Amber)
                RowCalc("Ganancia", money(ganancia), false, Green)
                if (x > 1) {
                    HorizontalDivider(color = Border, modifier = Modifier.padding(vertical = 7.dp))
                    RowCalc("Precio × ${x.toInt()} piezas", money(precioTotal), true, Amber)
                    RowCalc("Ganancia × ${x.toInt()} piezas", money(ganancia * x), false, Green)
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Button(
            onClick = {
                if (selectedPedidoId == null) {
                    toast("Seleccioná un pedido para guardar la cotización.")
                    return@Button
                }
                if (usos.isEmpty()) {
                    toast("Agregá al menos un filamento para la cotización.")
                    return@Button
                }
                for (u in usos) {
                    val g = u.gramos.toDoubleOrNull() ?: 0.0
                    val f = filamentos.find { it.id == u.filamentoId }
                    if (f != null && g > f.gramos) {
                        toast("Stock insuficiente para ${f.tipo} ${f.color} (disponible: ${f.gramos}g)")
                        return@Button
                    }
                }
                
                scope.launch {
                    val cotizacionAnterior = dao.filamentosDePedido(selectedPedidoId!!)
                    cotizacionAnterior.forEach { dao.ajustarStock(it.filamentoId, it.gramos) }
                    
                    dao.deletePedidoFilamentos(selectedPedidoId!!)
                    
                    usos.forEach { u ->
                        val g = u.gramos.toDoubleOrNull() ?: 0.0
                        if (g > 0) {
                            dao.ajustarStock(u.filamentoId, -g)
                            dao.insertPedidoFilamento(PedidoFilamentoEntity(java.util.UUID.randomUUID().toString(), selectedPedidoId!!, u.filamentoId, g))
                        }
                    }
                    dao.eliminarFilamentosAgotados()
                    dao.actualizarMontoPedido(selectedPedidoId!!, round(precioTotal * 100) / 100)
                    
                    toast("Cotización guardada y stock actualizado.")
                    usos = emptyList()
                    selectedPedidoId = null
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Amber, contentColor = Color(0xFF1A1208))
        ) {
            Icon(Icons.Default.Save, null)
            Spacer(Modifier.width(6.dp))
            Text("Guardar cotización en pedido", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun RowCalc(label: String, value: String, bold: Boolean = false, color: Color = Text) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = TextDim, fontSize = 13.sp)
        Text(value, color = color, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, fontSize = 14.sp)
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier, color: Color = Amber) {
    Card(colors = CardDefaults.cardColors(Surface), modifier = modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(label.uppercase(), fontSize = 10.sp, color = TextDim, fontWeight = FontWeight.Bold)
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Black, color = color)
        }
    }
}

@Composable
fun FilterButton(label: String, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        color = if (active) Amber.copy(0.12f) else Surface,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (active) Amber else Border)
    ) {
        Box(Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
            Text(label, fontSize = 12.sp, color = if (active) Amber else Text, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
        }
    }
}
