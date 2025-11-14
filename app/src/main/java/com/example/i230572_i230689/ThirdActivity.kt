package com.example.i230572_i230689

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.DefaultRetryPolicy
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class ThirdActivity : AppCompatActivity() {

    private lateinit var profileImg: ImageView
    private lateinit var usernameTxt: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login)

        profileImg = findViewById(R.id.profile_image_1)
        usernameTxt = findViewById(R.id.name_text)
        val nextBtn: ImageButton = findViewById(R.id.login_button)
        val switchBtn: TextView = findViewById(R.id.Switch_btn)

        val sm = SessionManager(this)

        if (!sm.isLoggedIn()) {
            startActivity(Intent(this, FourthActivity::class.java))
            finish()
            return
        }

        usernameTxt.text = sm.getUsername() ?: "No Name"
        loadUserImage(sm.getToken()!!)

        nextBtn.setOnClickListener {
            startActivity(Intent(this, FifthActivity::class.java))
        }

        switchBtn.setOnClickListener {
            sm.clear()
            startActivity(Intent(this, FourthActivity::class.java))
            finish()
        }
    }

    private fun loadUserImage(token: String) {
        val url = "http://192.168.100.10/instagram_api/get_user.php"
        val rq = Volley.newRequestQueue(this)

        val req = object : StringRequest(Method.POST, url,
            { resp ->
                try {
                    val obj = JSONObject(resp)
                    if (obj.optBoolean("success", false)) {
                        val imgBase = obj.getJSONObject("user").optString("imageBase64", "")
                        if (imgBase.isNotEmpty()) {
                            val bytes = Base64.decode(imgBase, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            profileImg.setImageBitmap(bitmap)
                        }
                    } else {
                        Toast.makeText(this, obj.optString("message", "Failed to load user"), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "Invalid response", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(this, "Network error: ${error.message ?: "unknown"}", Toast.LENGTH_SHORT).show()
            }) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer $token"
                headers["Content-Type"] = "application/x-www-form-urlencoded"
                return headers
            }

            override fun getParams(): MutableMap<String, String> {
                return hashMapOf()
            }
        }


        req.retryPolicy = DefaultRetryPolicy(
            15000,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )

        rq.add(req)
    }
}
