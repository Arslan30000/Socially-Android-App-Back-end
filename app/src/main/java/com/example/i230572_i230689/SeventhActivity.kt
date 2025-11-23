package com.example.i230572_i230689

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.DefaultRetryPolicy
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class SeventhActivity : AppCompatActivity() {

    private lateinit var searchInput: EditText
    private lateinit var resultsRecyclerView: RecyclerView
    private lateinit var searchAdapter: SearchUserAdapter
    private val searchResults = mutableListOf<User>()

    private lateinit var tabAll: TextView
    private lateinit var tabFollowers: TextView
    private lateinit var tabFollowing: TextView
    private var currentFilter = "all"

    private lateinit var sessionManager: SessionManager
    private val searchHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.searching)

        sessionManager = SessionManager(this)

        searchInput = findViewById(R.id.search_text)
        resultsRecyclerView = findViewById(R.id.results_recyclerview)
        tabAll = findViewById(R.id.tab_all)
        tabFollowers = findViewById(R.id.tab_followers)
        tabFollowing = findViewById(R.id.tab_following)

        resultsRecyclerView.layoutManager = LinearLayoutManager(this)
        searchAdapter = SearchUserAdapter(this, searchResults) { user ->
            val intent = Intent(this, TwentyActivity::class.java)
            intent.putExtra("userId", user.uid)
            startActivity(intent)
        }
        resultsRecyclerView.adapter = searchAdapter

        setupListeners()
        highlightTab(tabAll)
    }

    private fun setupListeners() {
        findViewById<TextView>(R.id.clear_text).setOnClickListener {
            finish()
        }

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchRunnable?.let { searchHandler.removeCallbacks(it) }
            }
            override fun afterTextChanged(s: Editable?) {
                searchRunnable = Runnable { performSearch() }
                searchHandler.postDelayed(searchRunnable!!, 300) // 300ms debounce
            }
        })

        tabAll.setOnClickListener {
            currentFilter = "all"
            highlightTab(tabAll)
            performSearch()
        }
        tabFollowers.setOnClickListener {
            currentFilter = "followers"
            highlightTab(tabFollowers)
            performSearch()
        }
        tabFollowing.setOnClickListener {
            currentFilter = "following"
            highlightTab(tabFollowing)
            performSearch()
        }
    }

    private fun highlightTab(selectedTab: TextView) {
        tabAll.setTextColor(if (selectedTab == tabAll) Color.BLACK else Color.LTGRAY)
        tabFollowers.setTextColor(if (selectedTab == tabFollowers) Color.BLACK else Color.LTGRAY)
        tabFollowing.setTextColor(if (selectedTab == tabFollowing) Color.BLACK else Color.LTGRAY)
    }

    private fun performSearch() {
        val query = searchInput.text.toString().trim()
        if (query.isEmpty()) {
            searchResults.clear()
            searchAdapter.notifyDataSetChanged()
            return
        }

        val token = sessionManager.getToken() ?: return
        val url = "${BuildConfig.BASE_URL}search_users.php?q=${java.net.URLEncoder.encode(query, "UTF-8")}&filter=$currentFilter"
        val rq = Volley.newRequestQueue(this)

        val req = object : StringRequest(Method.GET, url,
            { response ->
                try {
                    val obj = JSONObject(response.trim())
                    searchResults.clear()
                    if (obj.optBoolean("success", false)) {
                        val usersArray = obj.optJSONArray("users")
                        if (usersArray != null) {
                            for (i in 0 until usersArray.length()) {
                                val userObj = usersArray.getJSONObject(i)
                                searchResults.add(User(
                                    uid = userObj.optString("id"),
                                    username = userObj.optString("username"),
                                    name = userObj.optString("name"),
                                    lastname = userObj.optString("lastname"),
                                    imageBase64 = userObj.optString("imageBase64"),
                                    followers = mapOf("is_following" to userObj.optBoolean("is_following")),
                                    followRequests = mapOf("already_requested" to userObj.optBoolean("already_requested"))
                                ))
                            }
                        }
                    }
                    searchAdapter.notifyDataSetChanged()
                    if (searchResults.isEmpty()) {
                        // Optional: Show a "No users found" message only if the query is not empty
                        // Toast.makeText(this, "No users found.", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            { error -> error.printStackTrace() }) {
            override fun getHeaders(): MutableMap<String, String> {
                return mutableMapOf("Authorization" to "Bearer $token")
            }
        }
        req.retryPolicy = DefaultRetryPolicy(15000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        rq.add(req)
    }
}
