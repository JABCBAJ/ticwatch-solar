package com.ticwatch.solar

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
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import kotlinx.coroutines.delay
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

data class SolarData(
    val produccion: Float,
    val excedentes: Float,
    val consumo: Float,
    val balanceNeto: Float
)

interface SolarApiService {
    @GET("api/solar")
    suspend fun getSolarData(): SolarData
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.1.59:1880/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val solarApi = retrofit.create(SolarApiService::class.java)

        setContent {
            SolarDashboardScreen(solarApi)
        }
    }
}

@Composable
fun SolarDashboardScreen(solarApi: SolarApiService) {
    var solarData by remember { mutableStateOf(SolarData(0f, 0f, 0f, 0f)) }

    LaunchedEffect(Unit) {
        while (true) {
            try {
                solarData = solarApi.getSolarData()
            } catch (e: Exception) {
                // Silenciamos errores de red en la UI para no interrumpir
            }
            delay(5000)
        }
    }

    Scaffold(modifier = Modifier.background(Color.Black)) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item { Spacer(modifier = Modifier.height(30.dp)) }
            item { Gauge180(solarData.produccion, 8000f, "Prod", Color.Yellow) }
            item { Spacer(modifier = Modifier.height(10.dp)) }
            item { Gauge180(solarData.consumo, 8000f, "Cons", Color.Red) }
            item { Spacer(modifier = Modifier.height(10.dp)) }
            item { Gauge180(solarData.excedentes, 8000f, "Exced", Color.Green) }
            item { Spacer(modifier = Modifier.height(10.dp)) }
            item { Gauge180(solarData.balanceNeto, 8000f, "Red", Color.Blue) }
            item { Spacer(modifier = Modifier.height(30.dp)) }
        }
    }
}

@Composable
fun Gauge180(value: Float, maxValue: Float, label: String, color: Color) {
    val percentage = (value / maxValue).coerceIn(0f, 1f)
    val needleAngle = -90f + (180f * percentage)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(modifier = Modifier.size(90.dp, 45.dp)) {
            drawArc(
                color = Color.DarkGray, startAngle = 180f, sweepAngle = 180f,
                useCenter = false, style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round),
                size = Size(size.width, size.height * 2)
            )
            drawArc(
                color = color, startAngle = 180f, sweepAngle = 180f * percentage,
                useCenter = false, style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round),
                size = Size(size.width, size.height * 2)
            )
            rotate(degrees = needleAngle, pivot = Offset(size.width / 2, size.height)) {
                drawLine(
                    color = Color.White, start = Offset(size.width / 2, size.height),
                    end = Offset(size.width / 2, size.height - 35.dp.toPx()),
                    strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round
                )
            }
        }
        Text(text = "$label: ${value.toInt()}W", color = Color.White)
    }
}
