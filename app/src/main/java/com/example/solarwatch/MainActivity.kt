package com.ticwatch.solar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.google.gson.annotations.SerializedName // Importante añadir esto
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay // Importante para el bucle

// Modelo de datos con mapeo correcto a tus variables de Node-RED
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
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.1.59:1880/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val solarApi = retrofit.create(SolarApiService::class.java)

        setContent {
            SolarDashboardScreen(solarApi)
        }
    }
}

@Composable
fun SolarDashboardScreen(api: SolarApiService) {
    var solarData by remember { mutableStateOf<SolarData?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Bucle infinito que refresca cada 5 segundos
    LaunchedEffect(Unit) {
        while (true) {
            try {
                solarData = api.getSolarData()
                errorMessage = null // Si va bien, borramos el error
            } catch (e: Exception) {
                errorMessage = "Error: ${e.message}"
            }
            delay(5000) // Espera 5 segundos antes de la siguiente petición
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (errorMessage != null) {
            Text(text = errorMessage!!)
        } else if (solarData != null) {
            Text(text = "Prod: ${solarData!!.produccion} W")
            Text(text = "Cons: ${solarData!!.consumo} W")
            Text(text = "Exc: ${solarData!!.excedentes} W")
            Text(text = "Bal: ${solarData!!.balanceNeto} W")
        } else {
            Text(text = "Conectando...")
        }
    }
}
