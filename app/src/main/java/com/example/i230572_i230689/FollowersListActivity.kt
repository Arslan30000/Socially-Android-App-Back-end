package com.example.i230572_i230689

import android.graphics.BitmapFactory
import android.util.Base64
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.DefaultRetryPolicy
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class FollowersListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: UserListAdapter
    private lateinit var userList: MutableList<User>
    private lateinit var sessionManager: SessionManager
    private var listType: String = "followers"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_followers_list)

        recyclerView = findViewById(R.id.recyclerViewFollowers)
        recyclerView.layoutManager = LinearLayoutManager(this)
        userList = mutableListOf()

        sessionManager = SessionManager(this)
        listType = intent.getStringExtra("type") ?: "followers"
        
        adapter = UserListAdapter(userList, listType, sessionManager.getUserId())
        recyclerView.adapter = adapter

        loadUsers()
    }

    private fun loadUsers() {
        val token = sessionManager.getToken() ?: return
        val userId = intent.getStringExtra("userId")
        val url = if (listType == "followers") {
            BuildConfig.BASE_URL + "get_followers.php?user_id=${userId ?: ""}"
        } else {
            BuildConfig.BASE_URL + "get_following_list.php?user_id=${userId ?: ""}"
        }

        val rq = Volley.newRequestQueue(this)
        val req = object : StringRequest(Method.GET, url,
            { response ->
                try {
                    val obj = JSONObject(response.trim())
                    if (obj.optBoolean("success", false)) {
                        val arrName = if (listType == "followers") "followers" else "list"
                        val arr = obj.optJSONArray(arrName)
                        userList.clear()
                        if (arr != null) {
                            for (i in 0 until arr.length()) {
                                val u = arr.getJSONObject(i)
                                val id = u.optInt("id").toString()
                                val username = u.optString("username")
                                val image = u.optString("imageBase64", "")
                                userList.add(User(uid = id, username = username, imageBase64 = image))
                            }
                        }
                        adapter.notifyDataSetChanged()
                    }
                } catch (e: Exception) { e.printStackTrace() }
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
}
