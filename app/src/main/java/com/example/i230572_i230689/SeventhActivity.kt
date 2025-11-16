package com.example.i230572_i230689

import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Base64
import android.widget.*
import android.os.Bundle
import android.widget.LinearLayout.LayoutParams
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.DefaultRetryPolicy
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class SeventhActivity : AppCompatActivity() {

    private lateinit var searchInput: EditText
    private lateinit var resultsLayout: LinearLayout
    private lateinit var tabTop: TextView
    private lateinit var tabAccounts: TextView
    private lateinit var tabTags: TextView
    private lateinit var tabPlaces: TextView
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.searching)

        sessionManager = SessionManager(this)

        searchInput = findViewById(R.id.search_text)
        resultsLayout = findViewById(R.id.results_container)
        tabTop = findViewById(R.id.tab_top)
        tabAccounts = findViewById(R.id.tab_accounts)
        tabTags = findViewById(R.id.tab_tags)
        tabPlaces = findViewById(R.id.tab_places)

        val closeBtn: RelativeLayout = findViewById(R.id.close_button)
        closeBtn.setOnClickListener { finish() }

        highlightTab(tabAccounts)
        setupTabListeners()

        searchInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (tabAccounts.currentTextColor == getColor(R.color.black)) {
                    val query = s.toString().trim()
                    if (query.isNotEmpty()) searchUser(query)
                    else resultsLayout.removeAllViews()
                }
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun setupTabListeners() {
        val tabs = listOf(tabTop, tabAccounts, tabTags, tabPlaces)
        for (tab in tabs) {
            tab.setOnClickListener {
                highlightTab(tab)
                resultsLayout.removeAllViews()
                if (tab != tabAccounts) {
                    searchInput.setText("")
                    Toast.makeText(this, "Search only works in Accounts tab", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun highlightTab(selected: TextView) {
        val tabs = listOf(tabTop, tabAccounts, tabTags, tabPlaces)
        for (tab in tabs) {
            tab.setTextColor(getColor(if (tab == selected) R.color.black else R.color.light_grey))
        }
    }

    private fun searchUser(query: String) {
        resultsLayout.removeAllViews()
        val token = sessionManager.getToken() ?: run { Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show(); return }

        val url = BuildConfig.BASE_URL + "search_users.php?q=${java.net.URLEncoder.encode(query, "UTF-8") }"
        val rq = Volley.newRequestQueue(this)

        val req = object : StringRequest(Method.GET, url,
            { response ->
                try {
                    val obj = JSONObject(response.trim())
                    if (obj.optBoolean("success", false)) {
                        val users = obj.optJSONArray("users")
                        resultsLayout.removeAllViews()
                        if (users != null && users.length() > 0) {
                            // show only first result
                            val u = users.getJSONObject(0)
                            val uname = u.optString("username", "")
                            val img = u.optString("imageBase64", "")
                            val uid = u.optInt("id", -1)
                            val alreadyRequested = u.optBoolean("already_requested", false)
                            val isFollowing = u.optBoolean("is_following", false)
                            if (uid != -1) addUserResult(uname, img, uid, alreadyRequested, isFollowing)
                        }
                    } else {
                        resultsLayout.removeAllViews()
                        val msg = obj.optString("message", "No users found")
                        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            { error -> error.printStackTrace() }) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer ${sessionManager.getToken()}"
                return headers
            }
        }

        req.retryPolicy = DefaultRetryPolicy(15000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        rq.add(req)
    }

    private fun addUserResult(username: String, imageBase64: String?, targetUid: Int, alreadyRequested: Boolean, isFollowing: Boolean) {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.setPadding(20, 20, 20, 20)
        row.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

        val imgView = de.hdodenhof.circleimageview.CircleImageView(this)
        imgView.layoutParams = LayoutParams(100, 100)
        if (!imageBase64.isNullOrEmpty()) {
            val bytes = Base64.decode(imageBase64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            imgView.setImageBitmap(bitmap)
        } else {
            imgView.setImageResource(R.drawable.profile_image)
        }

        val textView = TextView(this)
        textView.text = username
        textView.textSize = 16f
        textView.setPadding(20, 0, 0, 0)
        textView.setTextColor(getColor(R.color.black))
        textView.layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)

        val followBtn = Button(this)
        followBtn.textSize = 14f
        followBtn.setPadding(20, 10, 20, 10)

        when {
            isFollowing -> {
                followBtn.text = "Already Following"
                followBtn.isEnabled = false
                followBtn.setBackgroundColor(getColor(R.color.light_grey))
                followBtn.setTextColor(Color.BLACK)
            }
            alreadyRequested -> {
                followBtn.text = "Requested"
                followBtn.isEnabled = false
                followBtn.setBackgroundColor(getColor(R.color.light_grey))
                followBtn.setTextColor(Color.BLACK)
            }
            else -> {
                followBtn.text = "Follow"
                followBtn.setBackgroundColor(getColor(R.color.brown))
                followBtn.setTextColor(Color.WHITE)
                followBtn.setOnClickListener {
                    sendFollowRequest(targetUid)
                    followBtn.text = "Requested"
                    followBtn.isEnabled = false
                    followBtn.setBackgroundColor(getColor(R.color.light_grey))
                    followBtn.setTextColor(Color.BLACK)
                }
            }
        }

        row.addView(imgView)
        row.addView(textView)
        row.addView(followBtn)
        // clicking row opens profile view
        row.setOnClickListener {
            val intent = Intent(this, TwentyActivity::class.java)
            intent.putExtra("userId", targetUid.toString())
            startActivity(intent)
        }
        resultsLayout.addView(row)
    }

    private fun sendFollowRequest(targetUid: Int) {
        val token = sessionManager.getToken() ?: run { Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show(); return }
        val url = BuildConfig.BASE_URL + "send_follow_request.php"
        val rq = Volley.newRequestQueue(this)

        val req = object : StringRequest(Method.POST, url,
            { response ->
                try {
                    val obj = JSONObject(response.trim())
                    if (obj.optBoolean("success", false)) {
                        // success - just silent update, button already changed
                    } else {
                        Toast.makeText(this, obj.optString("message", "Failed to send request"), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) { e.printStackTrace() }
            },
            { error -> error.printStackTrace() }) {
            override fun getParams(): MutableMap<String, String> {
                val p = HashMap<String, String>()
                p["to_user_id"] = targetUid.toString()
                return p
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
}
