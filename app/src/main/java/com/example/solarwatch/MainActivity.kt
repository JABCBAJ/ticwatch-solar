package com.ticwatch.solar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.MaterialTheme
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.util.concurrent.TimeUnit

// 1. Modelo de datos con mapeo exacto a Node-RED
data class SolarData(
    @SerializedName("P_PRODUCCION") val produccion: Float,
    @SerializedName("P_EXCEDENTE") val excedentes: Float,
    @SerializedName("P_AUTOCONSUMO") val consumo: Float,
    @SerializedName("P_BALANCENETO") val balanceNeto: Float
)

interface SolarApiService {
    @GET("api/solar")
    suspend fun getSolarData(): SolarData
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.1.59:1880/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val solarApi = retrofit.create(SolarApiService::class.java)

        setContent {
            MaterialTheme {
                SolarDashboardScreen(solarApi)
            }
        }
    }
}

@Composable
fun SolarDashboardScreen(api: SolarApiService) {
    var data by remember { mutableStateOf<SolarData?>(null) }
    var status by remember { mutableStateOf("Conectando...") }

    LaunchedEffect(Unit) {
        while (true) {
            try {
                data = api.getSolarData()
                status = "OK"
            } catch (e: Exception) {
                status = "Error: ${e.message?.take(15)}"
            }
            delay(5000)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (data != null) {
            Text(text = "Prod: ${data!!.produccion}W", color = Color.Yellow)
            Text(text = "Cons: ${data!!.consumo}W", color = Color.Red)
            Text(text = "Exc: ${data!!.excedentes}W", color = Color.Green)
            Text(text = "Bal: ${data!!.balanceNeto}W", color = Color.Cyan)
        } else {
            Text(text = status, color = Color.White)
        }
    }
}
