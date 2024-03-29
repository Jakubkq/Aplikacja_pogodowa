package com.example.weatherapp

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherapp.adapter.RvAdapter
import com.example.weatherapp.data.forecastModels.ForecastData
import com.example.weatherapp.databinding.ActivityMainBinding
import com.example.weatherapp.databinding.BottomSheetLayoutBinding
import com.example.weatherapp.utils.RetrofitInstance
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.squareup.picasso.Picasso
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private lateinit var sheetLayoutBinding: BottomSheetLayoutBinding

    private lateinit var dialog: BottomSheetDialog

    lateinit var pollutionFragment : PollutionFragment

    private var city:String = "Chełm"





    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        sheetLayoutBinding = BottomSheetLayoutBinding.inflate(layoutInflater)
        dialog = BottomSheetDialog(this, R.style.BottomSheetTheme)
        dialog.setContentView(sheetLayoutBinding.root)
        setContentView(binding.root)

        pollutionFragment = PollutionFragment()



        binding.searchView.setOnQueryTextListener(object :androidx.appcompat.widget.SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query!= null){
                    city = query
                }
                getCurrentWeather(city)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }

        })



        getCurrentWeather(city)

        binding.tvForecast.setOnClickListener{

            openDialog()
        }


    }

    private fun openDialog() {
        getForecast()

        sheetLayoutBinding.rvForecast.apply {
            setHasFixedSize(true)
            layoutManager= GridLayoutManager(this@MainActivity,1,RecyclerView.HORIZONTAL,false)
        }

        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
        dialog.show()

    }

    private fun getForecast() {
        GlobalScope.launch(Dispatchers.IO) {
            val response = try {
                RetrofitInstance.api.getForecast(
                    city,
                    "metric",
                    applicationContext.getString(R.string.api_key)
                )
            } catch (e: IOException) {
                Toast.makeText(applicationContext, "app error ${e.message}", Toast.LENGTH_SHORT)
                    .show()
                return@launch
            } catch (e: HttpException) {
                Toast.makeText(applicationContext, "http error ${e.message}", Toast.LENGTH_SHORT)
                    .show()
                return@launch
            }

            if (response.isSuccessful && response.body() != null) {
                withContext(Dispatchers.Main) {

                    val data = response.body()!!

                    val forecastArray: ArrayList<ForecastData> = data.list as ArrayList<ForecastData>

                    val adapter = RvAdapter(forecastArray)
                    sheetLayoutBinding.rvForecast.adapter = adapter
                    sheetLayoutBinding.tvSheet.text = "Five days forecast in ${data.city.name}"

                }
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun getCurrentWeather(city:String) {
        GlobalScope.launch(Dispatchers.IO) {
            val response = try {
                RetrofitInstance.api.getCurrentWeather(
                    city,
                    "metric",
                    applicationContext.getString(R.string.api_key)
                )
            } catch (e: IOException) {
                Toast.makeText(applicationContext, "app error ${e.message}", Toast.LENGTH_SHORT)
                    .show()
                return@launch
            } catch (e: HttpException) {
                Toast.makeText(applicationContext, "http error ${e.message}", Toast.LENGTH_SHORT)
                    .show()
                return@launch
            }

            if (response.isSuccessful && response.body() != null) {
                withContext(Dispatchers.Main) {

                    val data = response.body()!!

                    val iconId = data.weather[0].icon

                    val imgUrl = "https://openweathermap.org/img/w/$iconId.png"

                    Picasso.get().load(imgUrl).into(binding.imgWeather)

                    binding.tvSunset.text =
                        dateFormatConvert(
                            data.sys.sunset.toLong()
                        )

                    binding.tvSunrise.text =
                        dateFormatConvert(
                            data.sys.sunrise.toLong()
                        )

                    binding.apply {
                        tvStatus.text = data.weather[0].description
                        tvWind.text = "${data.wind.speed} KM/H"
                        tvLocation.text = "${data.name}\n${data.sys.country}"
                        tvTemp.text = "${data.main.temp.toInt()}°C"
                        tvFeelsLike.text = "Feels like: ${data.main.feels_like.toInt()}°C"
                        tvMinTemp.text = "Min temp: ${data.main.temp_min.toInt()}°C"
                        tvMaxTemp.text = "Max temp: ${data.main.temp_max.toInt()}°C"
                        tvHumidity.text = "${data.main.humidity}%"
                        tvPressure.text = "${data.main.pressure}hPa"
                        tvUpdateTime.text = "Last Update: ${
                            dateFormatConvert(
                                data.dt.toLong()
                            )
                        }"

                        getPollultion(data.coord.lat,data.coord.lon)
                    }
                }
            }
        }

    }
    @OptIn(DelicateCoroutinesApi::class)
    private fun getPollultion(lat: Double, lon: Double) {
        GlobalScope.launch(Dispatchers.IO) {
            val response = try {
                RetrofitInstance.api.getPollution(
                    "metric",
                    applicationContext.getString(R.string.api_key),
                    lat,
                    lon,


                )
            } catch (e: IOException) {
                Toast.makeText(applicationContext, "app error ${e.message}", Toast.LENGTH_SHORT)
                    .show()
                return@launch
            } catch (e: HttpException) {
                Toast.makeText(applicationContext, "http error ${e.message}", Toast.LENGTH_SHORT)
                    .show()
                return@launch
            }

            if (response.isSuccessful && response.body() != null) {
                withContext(Dispatchers.Main) {

                    val data = response.body()!!

                    val num = data.list[0].main.aqi

                    binding.tvAirQual.text = when(num){
                        1 -> getString(R.string.good)
                        2 -> getString(R.string.fair)
                        3 -> getString(R.string.moderate)
                        4 -> getString(R.string.poor)
                        5 -> getString(R.string.very_poor)
                        else ->  "no data"

                    }
                    binding.layoutPollution.setOnClickListener{
                        val bundle = Bundle()
                        bundle.putDouble("co",data.list[0].components.co)
                        bundle.putDouble("co",data.list[0].components.co)
                        bundle.putDouble("nh3",data.list[0].components.nh3)
                        bundle.putDouble("no",data.list[0].components.no)
                        bundle.putDouble("no2",data.list[0].components.no2)
                        bundle.putDouble("o3",data.list[0].components.o3)
                        bundle.putDouble("pm10",data.list[0].components.pm10)
                        bundle.putDouble("pm2_5",data.list[0].components.pm2_5)
                        bundle.putDouble("so2",data.list[0].components.so2)

                        pollutionFragment.arguments=bundle

                        supportFragmentManager.beginTransaction().apply {
                            replace(R.id.frameLayout,pollutionFragment)
                                .addToBackStack(null)
                                .commit()
                        }
                        
                        
                    }
                }
            }
        }

    }

    private fun dateFormatConvert(date: Long): String {

        return SimpleDateFormat(
            "hh:mm a",
            Locale.ENGLISH
        ).format(Date(date * 1000))

    }
}



