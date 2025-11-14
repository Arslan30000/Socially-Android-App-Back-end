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
import com.android.volley.Request
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
        val signupBtn: TextView = findViewById(R.id.sign_up_button)

        val sm = SessionManager(this)
        val token = sm.getToken()

        if (token.isNullOrEmpty()) {
            startActivity(Intent(this, FourthActivity::class.java))
            finish()
            return
        }

        loadUserData(token)

        nextBtn.setOnClickListener {
            startActivity(Intent(this, FifthActivity::class.java))
        }

        switchBtn.setOnClickListener {
            sm.clear()
            startActivity(Intent(this, FourthActivity::class.java))
            finish()
        }

        signupBtn.setOnClickListener {
            startActivity(Intent(this, SecondActivity::class.java))
        }
    }

    private fun loadUserData(token: String) {
        val url = "http://192.168.100.10/instagram_api/get_user.php"
        val rq = Volley.newRequestQueue(this)
        val req = object : StringRequest(Request.Method.GET, url,
            { resp ->
                try {
                    val obj = JSONObject(resp)
                    if (obj.optBoolean("success", false)) {
                        val user = obj.getJSONObject("user")
                        usernameTxt.text = user.optString("username", "No Name")

                        val imgBase = user.optString("imageBase64", "")
                        if (imgBase.isNotEmpty()) {
                            val bytes = Base64.decode(imgBase, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            profileImg.setImageBitmap(bitmap)
                        }
                    } else {
                        Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Parse error", Toast.LENGTH_SHORT).show()
                }
            },
            {
                Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show()
            }) {
            override fun getHeaders(): MutableMap<String, String> {
                return hashMapOf("Authorization" to "Bearer $token")
            }
        }
        rq.add(req)
    }
}
