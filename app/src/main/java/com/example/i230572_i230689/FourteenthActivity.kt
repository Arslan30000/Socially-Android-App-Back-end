package com.example.i230572_i230689

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.DefaultRetryPolicy
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.io.ByteArrayOutputStream

class FourteenthActivity : AppCompatActivity() {

    private lateinit var nameField: EditText
    private lateinit var usernameField: EditText
    private lateinit var bioField: EditText
    private lateinit var emailField: EditText
    private lateinit var phoneField: EditText
    private lateinit var genderField: EditText
    private lateinit var profilePic: de.hdodenhof.circleimageview.CircleImageView
    private lateinit var sessionManager: SessionManager

    private var encodedImage = ""
    private val PICK_IMAGE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.edit_profile)

        nameField = findViewById(R.id.name_field)
        usernameField = findViewById(R.id.username_field)
        bioField = findViewById(R.id.bio_field)
        emailField = findViewById(R.id.email_field)
        phoneField = findViewById(R.id.phone_field)
        genderField = findViewById(R.id.field_gender)
        profilePic = findViewById(R.id.profile_pic)

        sessionManager = SessionManager(this)

        if (sessionManager.isLoggedIn()) {
            loadUserData()
        }

        findViewById<TextView>(R.id.cancel_btn).setOnClickListener {
            startActivity(Intent(this, LastActivity::class.java))
            finish()
        }

        findViewById<TextView>(R.id.done_btn).setOnClickListener {
            saveChanges()
        }

        findViewById<TextView>(R.id.change_photo).setOnClickListener {
            pickImageFromGallery()
        }
    }

    private fun loadUserData() {
        val token = sessionManager.getToken() ?: return
        val userId = sessionManager.getUserId()
        val url = "https://nonactinically-unkindhearted-shelli.ngrok-free.dev/instagram_api/get_profile.php?user_id=$userId"
        val rq = Volley.newRequestQueue(this)

        val req = object : StringRequest(Method.GET, url,
            { response ->
                try {
                    val obj = JSONObject(response.trim())
                    if (obj.optBoolean("success", false)) {
                        val userObj = obj.getJSONObject("user")

                        val name = userObj.optString("name", "")
                        val username = userObj.optString("username", "")
                        val email = userObj.optString("email", "")
                        val bio = userObj.optString("bio", "")
                        val phone = userObj.optString("phone", "")
                        val gender = userObj.optString("gender", "")
                        val imageBase64 = userObj.optString("imageBase64", "")

                        nameField.setText(name)
                        usernameField.setText(username)
                        emailField.setText(email)
                        bioField.setText(bio)
                        phoneField.setText(phone)
                        genderField.setText(gender)

                        if (imageBase64.isNotEmpty()) {
                            val bytes = Base64.decode(imageBase64, Base64.DEFAULT)
                            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            profilePic.setImageBitmap(bmp)
                            encodedImage = imageBase64
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            { error -> error.printStackTrace() }) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer $token"
                return headers
            }
        }

        req.retryPolicy = DefaultRetryPolicy(15000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        rq.add(req)
    }

    private fun saveChanges() {
        val token = sessionManager.getToken() ?: run { Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show(); return }
        val userId = sessionManager.getUserId()
        
        val name = nameField.text.toString().trim()
        val username = usernameField.text.toString().trim()
        val email = emailField.text.toString().trim()
        val bio = bioField.text.toString().trim()
        val phone = phoneField.text.toString().trim()
        val gender = genderField.text.toString().trim()

        if (name.isEmpty() || username.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Required fields cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        val url = "https://nonactinically-unkindhearted-shelli.ngrok-free.dev/instagram_api/update_profile.php"
        val rq = Volley.newRequestQueue(this)

        val req = object : StringRequest(Method.POST, url,
            { response ->
                try {
                    val obj = JSONObject(response.trim())
                    if (obj.optBoolean("success", false)) {
                        // update stored username so UI shows new value immediately
                        try {
                            sessionManager.saveSession(token, userId, username)
                        } catch (_: Exception) {}
                        Toast.makeText(this@FourteenthActivity, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@FourteenthActivity, LastActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this@FourteenthActivity, obj.optString("message", "Update failed"), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this@FourteenthActivity, "Error updating profile", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                error.printStackTrace()
                Toast.makeText(this@FourteenthActivity, "Network error", Toast.LENGTH_SHORT).show()
            }) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["name"] = name
                params["username"] = username
                params["email"] = email
                params["bio"] = bio
                params["phone"] = phone
                params["gender"] = gender
                params["imageBase64"] = encodedImage
                return params
            }

            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer $token"
                return headers
            }
        }

        req.retryPolicy = DefaultRetryPolicy(15000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        rq.add(req)
    }

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            val imageUri: Uri? = data?.data
            imageUri?.let {
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, it)
                profilePic.setImageBitmap(bitmap)
                encodedImage = encodeImage(bitmap)
            }
        }
    }

    private fun encodeImage(bitmap: Bitmap): String {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
        return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)
    }
}
