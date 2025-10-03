package com.example.weatherapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.bumptech.glide.Glide
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*
import com.google.android.gms.tasks.CancellationTokenSource
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AlertDialog


class MainActivity : AppCompatActivity() {

    private val apiKey = "c2cfb03bac68bbf380f03cdbc32a83e4"
    private val unit = "metric"
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    private lateinit var weatherTextView: TextView
    private lateinit var weatherIconImageView: ImageView
    private lateinit var humidityTextView: TextView
    private lateinit var pressureTextView: TextView
    private lateinit var windTextView: TextView
    private lateinit var sunriseTextView: TextView
    private lateinit var sunsetTextView: TextView
    private lateinit var logoutButton: Button
    private lateinit var currentLocationButton: Button
    private lateinit var parentLayout: LinearLayout
    private lateinit var fusedLocationClient: FusedLocationProviderClient



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val cityEditText = findViewById<EditText>(R.id.cityEditText)
        val searchButton = findViewById<Button>(R.id.searchButton)

        currentLocationButton = findViewById(R.id.currentLocationButton)
        parentLayout = findViewById(R.id.parentLayout)

        weatherTextView = findViewById(R.id.weatherTextView)
        weatherIconImageView = findViewById(R.id.weatherIconImageView)
        humidityTextView = findViewById(R.id.humidityTextView)
        pressureTextView = findViewById(R.id.pressureTextView)
        windTextView = findViewById(R.id.windTextView)
        sunriseTextView = findViewById(R.id.sunriseTextView)
        sunsetTextView = findViewById(R.id.sunsetTextView)
        logoutButton = findViewById(R.id.logoutButton)

        weatherIconImageView.visibility = ImageView.GONE

        promptBiometricEnrollment()

        logoutButton.setOnClickListener {
            logoutUser()
        }

        searchButton.setOnClickListener {
            val city = cityEditText.text.toString().trim()
            if (city.isNotEmpty()) {
                getWeather(city)
            } else {
                Toast.makeText(this, "Please enter a city name", Toast.LENGTH_SHORT).show()
            }
        }

        currentLocationButton.setOnClickListener {
            checkLocationPermissionAndFetch()
        }
    }

    private fun logoutUser() {
        FirebaseAuth.getInstance().signOut()
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, SignInActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun checkLocationPermissionAndFetch() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            fetchLocationAndWeather()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchLocationAndWeather()
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun promptBiometricEnrollment() {
        val biometricManager = BiometricManager.from(this)
        if (biometricManager.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS) {
            val sharedPref = getSharedPreferences("app_prefs", MODE_PRIVATE)
            val isBiometricEnabled = sharedPref.getBoolean("biometric_enabled", false)

            if (!isBiometricEnabled) {
                AlertDialog.Builder(this)
                    .setTitle("Enable Biometric Login?")
                    .setMessage("Would you like to use biometric login for faster access next time?")
                    .setPositiveButton("Yes") { dialog, _ ->
                        sharedPref.edit().putBoolean("biometric_enabled", true).apply()
                        dialog.dismiss()
                    }
                    .setNegativeButton("No") { dialog, _ ->
                        sharedPref.edit().putBoolean("biometric_enabled", false).apply()
                        dialog.dismiss()
                    }
                    .show()
            }
        }
    }


    private fun fetchLocationAndWeather() {
        try {
            fusedLocationClient.getCurrentLocation(
                LocationRequest.PRIORITY_HIGH_ACCURACY,
                CancellationTokenSource().token
            ).addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val lat = location.latitude
                    val lon = location.longitude
                    getWeatherByCoordinates(lat, lon)
                } else {
                    Toast.makeText(this, "Unable to get location", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: SecurityException) {
            Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getWeather(cityInput: String) {
        val city = URLEncoder.encode(cityInput.trim(), "UTF-8")
        val cityUrl = "https://api.openweathermap.org/data/2.5/weather?q=$city&appid=$apiKey&units=$unit"
        fetchWeatherFromUrl(cityUrl)
    }

    private fun getWeatherByCoordinates(lat: Double, lon: Double) {
        val url = "https://api.openweathermap.org/data/2.5/weather?lat=$lat&lon=$lon&appid=$apiKey&units=$unit"
        fetchWeatherFromUrl(url)
    }

    private fun fetchWeatherFromUrl(url: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = URL(url).readText()
                val jsonObject = JSONObject(response)

                val weatherArray = jsonObject.getJSONArray("weather")
                val weatherObject = weatherArray.getJSONObject(0)
                val description = weatherObject.getString("description")
                val iconCode = weatherObject.getString("icon")

                val mainObject = jsonObject.getJSONObject("main")
                val temp = mainObject.getDouble("temp")
                val humidity = mainObject.getInt("humidity")
                val pressure = mainObject.getInt("pressure")

                val windObject = jsonObject.getJSONObject("wind")
                val windSpeed = windObject.getDouble("speed")
                val windDeg = windObject.optInt("deg", -1)
                val windDirStr = if (windDeg == -1) "N/A" else windDirection(windDeg)

                val sysObject = jsonObject.getJSONObject("sys")
                val sunriseUnix = sysObject.getLong("sunrise")
                val sunsetUnix = sysObject.getLong("sunset")
                val sunriseTime = formatTime(sunriseUnix)
                val sunsetTime = formatTime(sunsetUnix)

                val cityName = jsonObject.getString("name")

                val result = "City: $cityName\nTemperature: $temp°C\nCondition: $description"
                val iconUrl = "https://openweathermap.org/img/wn/${iconCode}.png"

                withContext(Dispatchers.Main) {
                    weatherTextView.text = result

                    humidityTextView.text = "Humidity: $humidity%"
                    pressureTextView.text = "Pressure: $pressure hPa"
                    windTextView.text = "Wind: $windSpeed m/s $windDirStr"
                    sunriseTextView.text = "Sunrise: $sunriseTime"
                    sunsetTextView.text = "Sunset: $sunsetTime"

                    weatherIconImageView.visibility = ImageView.VISIBLE
                    val scale = resources.displayMetrics.density
                    val newSize = (200 * scale).toInt()
                    val params = weatherIconImageView.layoutParams
                    params.width = newSize
                    params.height = newSize
                    weatherIconImageView.layoutParams = params
                    weatherIconImageView.requestLayout()

                    Glide.with(this@MainActivity)
                        .load(iconUrl)
                        .placeholder(android.R.drawable.progress_indeterminate_horizontal)
                        .error(android.R.drawable.stat_notify_error)
                        .into(weatherIconImageView)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    weatherTextView.text = "Error: ${e.localizedMessage}"
                    weatherIconImageView.visibility = ImageView.GONE
                    humidityTextView.text = ""
                    pressureTextView.text = ""
                    windTextView.text = ""
                    sunriseTextView.text = ""
                    sunsetTextView.text = ""
                }
            }
        }
    }

    private fun formatTime(unixTime: Long): String {
        val date = Date(unixTime * 1000)
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(date)
    }

    private fun windDirection(deg: Int): String {
        val directions = listOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")
        val index = ((deg / 22.5) + 0.5).toInt() % 16
        return directions[index]
    }
}
