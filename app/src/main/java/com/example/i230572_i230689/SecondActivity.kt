package com.example.i230572_i230689

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.DefaultRetryPolicy
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.Calendar

class SecondActivity : AppCompatActivity() {

    private lateinit var usernameInput: EditText
    private lateinit var nameInput: EditText
    private lateinit var lastnameInput: EditText
    private lateinit var dateInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var profileImg: ImageView
    private lateinit var passwordToggle: ImageView
    private lateinit var signupBtn: ImageButton

    private var encodedImage: String = ""
    private var isPasswordVisible = false
    private val PICK_IMAGE = 1001
    private var selectedImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sign_up)


        usernameInput = findViewById(R.id.username_input)
        nameInput = findViewById(R.id.name_input)
        lastnameInput = findViewById(R.id.lastname_input)
        dateInput = findViewById(R.id.date_input)
        emailInput = findViewById(R.id.email_input)
        passwordInput = findViewById(R.id.password_input)
        profileImg = findViewById(R.id.profile_image)
        passwordToggle = findViewById(R.id.password_toggle)
        signupBtn = findViewById(R.id.sign_in_icon)


        dateInput.setOnClickListener {
            val c = Calendar.getInstance()
            DatePickerDialog(this,
                { _, y, m, d -> dateInput.setText("$d/${m + 1}/$y") },
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        passwordToggle.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            passwordInput.inputType =
                if (isPasswordVisible)
                    android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                else
                    android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            passwordInput.setSelection(passwordInput.text.length)
        }

        profileImg.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE)
        }

        signupBtn.setOnClickListener { signupUser() }
    }

    private fun signupUser() {
        val username = usernameInput.text.toString().trim()
        val name = nameInput.text.toString().trim()
        val lastname = lastnameInput.text.toString().trim()
        val dob = dateInput.text.toString().trim()
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()

        if (username.isEmpty() || name.isEmpty() || lastname.isEmpty() ||
            dob.isEmpty() || email.isEmpty() || password.isEmpty()
        ) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (encodedImage.isEmpty()) {
            val bmp = BitmapFactory.decodeResource(resources, R.drawable.profile_image)
            encodedImage = encodeImage(bmp)
        }

        val url = "http://192.168.100.10/instagram_api/signup.php"
        val rq = Volley.newRequestQueue(this)

        val req = object : StringRequest(Method.POST, url,
            { response ->
                try {
                    val obj = JSONObject(response.trim())
                    val ok = obj.optBoolean("success", false) || obj.optString("status", "") == "success"
                    if (ok) {
                        val token = obj.optString("token", "")
                        if (token.isNotEmpty()) {
                            val sm = SessionManager(this)
                            sm.saveSession(
                                token,
                                obj.optInt("user_id", 0),
                                "$username"
                            )
                        }

                        Toast.makeText(this, "Account created!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, ThirdActivity::class.java))
                        finish()
                    } else {
                        val msg = obj.optString("message", obj.optString("error", "Signup failed"))
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Invalid server response", Toast.LENGTH_LONG).show()
                    e.printStackTrace()
                }
            },
            { error ->
                val code = error.networkResponse?.statusCode?.toString() ?: "No status code"
                val body = error.networkResponse?.data?.let { String(it) } ?: ""
                Toast.makeText(this, "Network Error: $code\n$body", Toast.LENGTH_LONG).show()
            }) {

            override fun getParams(): MutableMap<String, String> {
                val map = HashMap<String, String>()
                map["username"] = username
                map["name"] = name
                map["lastname"] = lastname
                map["dob"] = dob
                map["email"] = email
                map["password"] = password
                map["imageBase64"] = encodedImage
                return map
            }
        }

        req.retryPolicy = DefaultRetryPolicy(
            15000,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )
        rq.add(req)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.data
            try {
                val stream: InputStream? = contentResolver.openInputStream(selectedImageUri!!)
                val bmp = BitmapFactory.decodeStream(stream)
                profileImg.setImageBitmap(bmp)
                encodedImage = encodeImage(bmp)
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun encodeImage(bmp: android.graphics.Bitmap): String {
        val out = ByteArrayOutputStream()
        bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 60, out)
        return Base64.encodeToString(out.toByteArray(), Base64.DEFAULT)
    }
}
