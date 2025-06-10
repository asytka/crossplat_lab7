import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.format.DateTimeFormatter

const val API_KEY = "4ba9d33490cb43949f8195245251105"

val cities = listOf(
    "Lviv", "Kyiv", "Donetsk", "Odesa", "Rivne",
    "Sumy", "Kharkiv"
)

val parameters = listOf("Temperature", "Humidity", "Precipitation")

@Serializable
data class WeatherResponse(
    val forecast: Forecast
)

@Serializable
data class Forecast(
    val forecastday: List<ForecastDay>
)

@Serializable
data class ForecastDay(
    val date: String,
    val day: Day
)

@Serializable
data class Day(
    val avgtemp_c: Float,
    val totalprecip_mm: Float,
    val avghumidity: Float,
    val condition: Condition
)

@Serializable
data class Condition(
    val text: String
)

val client = HttpClient(CIO) {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
}

suspend fun fetchWeather(city: String, date: String): ForecastDay? {
    val url = "https://api.weatherapi.com/v1/history.json?key=$API_KEY&q=$city&dt=$date"
    return try {
        val response: WeatherResponse = client.get(url).body()
        response.forecast.forecastday.firstOrNull()
    } catch (e: Exception) {
        println("Error fetching weather: $e")
        null
    }
}

@Composable
fun WeatherApp() {
    var selectedCity by remember { mutableStateOf(cities.first()) }
    var selectedParameter by remember { mutableStateOf(parameters.first()) }
    var weatherData by remember { mutableStateOf<Map<String, ForecastDay>>(emptyMap()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(selectedCity) {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        isLoading = true
        errorMessage = null
        weatherData = emptyMap()

        val map = mutableMapOf<String, ForecastDay>()
        for (i in 0..6) {
            val date = LocalDate.now().minusDays(i.toLong()).format(formatter)
            val data = fetchWeather(selectedCity, date)
            if (data != null) {
                map[date] = data
            } else {
                errorMessage = "Failed to fetch weather data for $selectedCity on $date"
            }
        }

        weatherData = map
        isLoading = false
    }

    Column(Modifier.padding(16.dp).fillMaxSize()) {
        Text("Weather in $selectedCity", style = MaterialTheme.typography.h5)
        Spacer(Modifier.height(8.dp))

        Row {
            DropdownMenuSelector("City", cities, selectedCity) { selectedCity = it }
            Spacer(Modifier.width(16.dp))
            DropdownMenuSelector("Parameter", parameters, selectedParameter) { selectedParameter = it }
        }

        Spacer(Modifier.height(16.dp))

        if (isLoading) {
            Text("Loading data...", style = MaterialTheme.typography.body1)
        } else if (errorMessage != null) {
            Text(errorMessage!!, color = MaterialTheme.colors.error)
        } else {
            LazyColumn {
                weatherData.entries.sortedBy { it.key }.forEach { (date, forecast) ->
                    item {
                        Card(Modifier.fillMaxWidth().padding(4.dp)) {
                            Column(Modifier.padding(8.dp)) {
                                Text("Date: $date")
                                Text("Condition: ${forecast.day.condition.text}")
                                when (selectedParameter) {
                                    "Temperature" -> Text("Avg Temp: ${forecast.day.avgtemp_c} °C")
                                    "Humidity" -> Text("Humidity: ${forecast.day.avghumidity}%")
                                    "Precipitation" -> Text("Precipitation: ${forecast.day.totalprecip_mm} mm")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun <T> DropdownMenuSelector(label: String, options: List<T>, selected: T, onSelect: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label)
        Box {
            Button(onClick = { expanded = true }) {
                Text(selected.toString())
            }
            DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
                options.forEach {
                    DropdownMenuItem(onClick = {
                        onSelect(it)
                        expanded = false
                    }) {
                        Text(it.toString())
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun AppPreview() {
    WeatherApp()
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Weather in Lviv Region") {
        MaterialTheme {
            WeatherApp()
        }
    }
}
