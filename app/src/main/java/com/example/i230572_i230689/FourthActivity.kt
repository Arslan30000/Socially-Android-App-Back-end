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
        val passwordInput = findViewById<EditText>(R.id.password_input)
        val loginBtn = findViewById<TextView>(R.id.login_button)
        val signupBtn = findViewById<TextView>(R.id.sign_up_button)

        loginBtn.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Enter username and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val data = HashMap<String, String>()
            data["username"] = username
            data["password"] = password

            val url = "https://nonactinically-unkindhearted-shelli.ngrok-free.dev/instagram_api/login.php"
            val rq = Volley.newRequestQueue(this)

            val req = object : StringRequest(Method.POST, url,
                { response ->
                    try {
                        val obj = JSONObject(response.trim())
                        if (obj.optBoolean("success", false)) {
                            val token = obj.optString("token")
                            val user = obj.getJSONObject("user")
                            val userId = user.optInt("id")
                            val usernameSaved = user.optString("username")

                            if (!token.isNullOrEmpty()) {
                                SessionManager(this).saveSession(token, userId, usernameSaved)
                            }

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
                    Toast.makeText(this, "Network error: ${error.message}", Toast.LENGTH_LONG).show()
                }) {
                override fun getParams(): MutableMap<String, String> = data
            }
            req.retryPolicy = DefaultRetryPolicy(15000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
            rq.add(req)
        }

        signupBtn.setOnClickListener {
            startActivity(Intent(this, SecondActivity::class.java))
        }
    }
}
