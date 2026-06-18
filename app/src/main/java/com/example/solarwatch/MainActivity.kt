package com.example.solarwatch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Text
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

val ColorProd = Color(0xFFEAB308) 
val ColorCons = Color(0xFFF87171) 
val ColorNet = Color(0xFF22D3EE)  
val ColorSurp = Color(0xFF4ADE80) 
val ColorBgGauge = Color(0x404A5568) 
val ColorTextMuted = Color(0xFFA0AEC0) 

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SolarWatchApp() }
    }
}

data class SolarData(
    val produccion: Double = 0.0,
    val consumo: Double = 0.0,
    val balance: Double = 0.0,
    val excedente: Double = 0.0,
    val isConnected: Boolean = false
)

@Composable
fun SolarWatchApp() {
    var solarData by remember { mutableStateOf(SolarData()) }
    var currentTime by remember { mutableStateOf(getCurrentTimeString()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = getCurrentTimeString()
            solarData = fetchSolarDataFromNodeRed()
            delay(5000)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.align(Alignment.TopCenter).offset(y = 10.dp).size(6.dp)
                .background(color = if (solarData.isConnected) ColorSurp else ColorCons, shape = androidx.compose.foundation.shape.CircleShape)
        )
        Text(text = "MONITOR SOLAR - $currentTime", color = ColorTextMuted, fontSize = 10.sp, modifier = Modifier.align(Alignment.TopCenter).offset(y = 22.dp))

        Column(
            modifier = Modifier.fillMaxSize().padding(top = 40.dp, bottom = 10.dp, start = 15.dp, end = 15.dp),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                GaugeComponent("PRODUCCIÓN (kW)", solarData.produccion, 5.0, ColorProd, Modifier.weight(1f))
                GaugeComponent("CONSUMO (kW)", solarData.consumo, 5.0, ColorCons, Modifier.weight(1f))
            }
            Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                GaugeComponent("BALANCE (kW)", solarData.balance, 4.0, ColorNet, Modifier.weight(1f), isNet = true)
                GaugeComponent("EXCEDENTE (kW)", solarData.excedente, 4.0, ColorSurp, Modifier.weight(1f))
            }
        }
        Box(modifier = Modifier.align(Alignment.Center).size(24.dp).background(Color.Black, shape = androidx.compose.foundation.shape.CircleShape), contentAlignment = Alignment.Center) {
            Text("🔆", fontSize = 12.sp)
        }
    }
}

@Composable
fun GaugeComponent(title: String, valueW: Double, maxKw: Double, color: Color, modifier: Modifier = Modifier, isNet: Boolean = false) {
    val valueKw = valueW / 1000.0
    val formatKw = if (isNet && valueKw > 0) "+%.2f kW".format(valueKw) else "%.2f kW".format(valueKw)
    val percentage = if (isNet) ((valueKw + maxKw) / (2 * maxKw)).coerceIn(0.0, 1.0) else (valueKw / maxKw).coerceIn(0.0, 1.0)
    val sweepAngle = (percentage * 180f).toFloat()

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = modifier.padding(2.dp)) {
        Text(text = title, color = ColorTextMuted, fontSize = 8.sp, maxLines = 1)
        Spacer(modifier = Modifier.height(4.dp))
        Box(contentAlignment = Alignment.BottomCenter, modifier = Modifier.height(45.dp).width(80.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeW = 6.dp.toPx()
                drawArc(ColorBgGauge, 180f, 180f, false, Offset(strokeW / 2, strokeW / 2), Size(size.width - strokeW, size.height * 2 - strokeW), style = Stroke(width = strokeW, cap = StrokeCap.Round))
                drawArc(color, 180f, sweepAngle, false, Offset(strokeW / 2, strokeW / 2), Size(size.width - strokeW, size.height * 2 - strokeW), style = Stroke(width = strokeW, cap = StrokeCap.Round))
                rotate(degrees = 180f + sweepAngle, pivot = Offset(size.width / 2, size.height)) {
                    drawLine(color, Offset(size.width / 2, size.height), Offset(size.width / 2 + 10f, 10f), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
                    drawCircle(color, 4.dp.toPx(), Offset(size.width / 2, size.height))
                }
            }
            Text(text = if (isNet) "-${maxKw.toInt()}" else "0", color = ColorTextMuted, fontSize = 8.sp, modifier = Modifier.align(Alignment.BottomStart).offset(y = 10.dp))
            Text(text = if (isNet) "+${maxKw.toInt()}" else maxKw.toString(), color = ColorTextMuted, fontSize = 8.sp, modifier = Modifier.align(Alignment.BottomEnd).offset(y = 10.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = formatKw, color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(text = "${valueW.toInt()} W", color = ColorTextMuted, fontSize = 10.sp)
    }
}

fun getCurrentTimeString(): String {
    return SimpleDateFormat("EEE dd MMM, HH:mm", Locale("es", "ES")).format(Date()).uppercase()
}

suspend fun fetchSolarDataFromNodeRed(): SolarData {
    return withContext(Dispatchers.IO) {
        try {
            val url = URL("[http://192.168.1.59:1880/api/solar](http://192.168.1.59:1880/api/solar)")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 3000
            
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                SolarData(json.optDouble("produccion", 0.0), json.optDouble("consumo", 0.0), json.optDouble("balance", 0.0), json.optDouble("excedente", 0.0), true)
            } else { getSimulatedData() }
        } catch (e: Exception) { getSimulatedData() }
    }
}

fun getSimulatedData(): SolarData {
    val prod = (2000..5000).random().toDouble()
    val cons = (1000..4000).random().toDouble()
    return SolarData(prod, cons, prod - cons, max(0.0, prod - cons), false)
}
