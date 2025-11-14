package com.example.i230572_i230689

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.DefaultRetryPolicy
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class FourthActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_2)

        val usernameInput = findViewById<EditText>(R.id.username_input)
        val passInput = findViewById<EditText>(R.id.password_input)
        val loginBtn = findViewById<TextView>(R.id.login_button)
        val signupBtn = findViewById<TextView>(R.id.sign_up_button)
        val forgotBtn = findViewById<TextView>(R.id.forgot_password)

        loginBtn.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            val password = passInput.text.toString().trim()
            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val data = HashMap<String, String>()
            data["username"] = username
            data["password"] = password
            val url = "http://192.168.100.10/instagram_api/login.php"
            val rq = Volley.newRequestQueue(this)

            val req = object : StringRequest(Method.POST, url,
                { response ->
                    try {
                        val obj = JSONObject(response.trim())
                        val ok = obj.optBoolean("success", false)
                        if (ok) {
                            val token = obj.optString("token", null)
                            if (!token.isNullOrEmpty()) SessionManager(this).saveToken(token)
                            val prefs = getSharedPreferences("user", MODE_PRIVATE)
                            prefs.edit().putString("token", obj.getString("token"))
                                .putInt("user_id", obj.getInt("user_id"))
                                .apply()
                            startActivity(Intent(this, ThirdActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(this, obj.optString("message", "Login failed"), Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this, "Invalid server response", Toast.LENGTH_SHORT).show()
                    }
                },
                { error ->
                    val msg = error.networkResponse?.statusCode?.toString() ?: error.message
                    Toast.makeText(this, "Network error: $msg", Toast.LENGTH_LONG).show()
                }) {
                override fun getParams(): MutableMap<String, String> {
                    return data
                }
            }

            req.retryPolicy = DefaultRetryPolicy(15000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
            rq.add(req)
        }

        signupBtn.setOnClickListener {
            startActivity(Intent(this, SecondActivity::class.java))
        }

        forgotBtn.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            if (username.isEmpty()) {
                Toast.makeText(this, "Enter your username to reset password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val data = HashMap<String, String>()
            data["username"] = username
            val rq = Volley.newRequestQueue(this)
            val req = object : StringRequest(Method.POST, "http://192.168.100.10/instagram_api/forgot_password.php",
                { response ->
                    try {
                        val obj = JSONObject(response.trim())
                        Toast.makeText(this, obj.optString("message", "Done"), Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(this, "Invalid server response", Toast.LENGTH_SHORT).show()
                    }
                },
                { error -> Toast.makeText(this, "Network error: ${error.message}", Toast.LENGTH_SHORT).show() }) {
                override fun getParams(): MutableMap<String, String> = data
            }
            rq.add(req)
        }
    }
}
