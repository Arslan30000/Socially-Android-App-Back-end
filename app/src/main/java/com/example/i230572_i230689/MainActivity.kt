package com.example.i230572_i230689

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    private val BASE = "http://192.168.100.10/instagram_api/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val sm = SessionManager(this)


        Handler(Looper.getMainLooper()).postDelayed({

            val token = sm.getToken()

            if (token.isNullOrEmpty()) {
                startActivity(Intent(this, FourthActivity::class.java))
                finish()
                return@postDelayed
            }

            val url = BASE + "validate_token.php"
            val rq = Volley.newRequestQueue(this)
            val req = object : StringRequest(Request.Method.GET, url,
                { resp ->
                    try {
                        val obj = JSONObject(resp)
                        val ok = obj.optBoolean("success", false)
                        if (ok) {
                            startActivity(Intent(this, ThirdActivity::class.java))
                        } else {
                            sm.clear()
                            startActivity(Intent(this, FourthActivity::class.java))
                        }
                    } catch (e: Exception) {
                        sm.clear()
                        startActivity(Intent(this, FourthActivity::class.java))
                    }
                    finish()
                },
                {
                    startActivity(Intent(this, FourthActivity::class.java))
                    finish()
                }) {
                override fun getHeaders(): MutableMap<String, String> {
                    return hashMapOf("Authorization" to "Bearer $token")
                }
            }
            rq.add(req)

        }, 5000)
    }
}
