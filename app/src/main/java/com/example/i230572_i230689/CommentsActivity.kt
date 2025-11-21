package com.example.i230572_i230689

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.DefaultRetryPolicy
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class CommentsActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CommentsAdapter
    private val comments = mutableListOf<Comment>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comments)

        val postId = intent.getStringExtra("POST_ID") ?: return

        recyclerView = findViewById(R.id.comments_recyclerview)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = CommentsAdapter(this, comments)
        recyclerView.adapter = adapter

        val commentInput = findViewById<EditText>(R.id.comment_input)
        val commentPostBtn = findViewById<Button>(R.id.comment_post_button)

        fetchComments(postId)

        commentPostBtn.setOnClickListener {
            val token = SessionManager(this).getToken() ?: return@setOnClickListener
            val text = commentInput.text.toString()
            if (text.isBlank()) return@setOnClickListener

            val url = BuildConfig.BASE_URL + "add_comment.php"
            val rq = Volley.newRequestQueue(this)
            val req = object: StringRequest(Method.POST, url,
                { response ->
                    try {
                        val obj = JSONObject(response.trim())
                        if (obj.optBoolean("success", false)) {
                            val commentObj = obj.optJSONObject("comment")
                            if (commentObj != null) {
                                val com = Comment(
                                    id = commentObj.optInt("id", 0).toString(),
                                    postId = commentObj.optInt("postId", 0).toString(),
                                    userId = commentObj.optInt("userId", 0).toString(),
                                    username = commentObj.optString("username", "User"),
                                    userProfileImage = commentObj.optString("userProfileImage", ""),
                                    text = commentObj.optString("text", ""),
                                    timestamp = commentObj.optString("timestamp", "")
                                )
                                comments.add(com)
                                adapter.notifyItemInserted(comments.size - 1)
                                commentInput.text.clear()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("Comments", "Error parsing response: ${e.message}")
                    }
                },
                { error ->
                    Log.e("Comments", "Network error: ${error.message}")
                }) {
                override fun getParams(): MutableMap<String, String> {
                    return hashMapOf("post_id" to postId, "text" to text)
                }

                override fun getHeaders(): MutableMap<String, String> {
                    val headers = HashMap<String, String>()
                    headers["Authorization"] = "Bearer ${SessionManager(this@CommentsActivity).getToken()}"
                    return headers
                }
            }

            req.retryPolicy = DefaultRetryPolicy(10000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
            rq.add(req)
        }
    }

    private fun fetchComments(postId: String) {
        val token = SessionManager(this).getToken() ?: return
        val url = BuildConfig.BASE_URL + "get_comments.php?post_id=$postId"
        val rq = Volley.newRequestQueue(this)

        val req = object : StringRequest(Method.GET, url,
            { response ->
                try {
                    val obj = JSONObject(response.trim())
                    if (obj.optBoolean("success", false)) {
                        comments.clear()
                        val arr = obj.optJSONArray("comments")
                        if (arr != null) {
                            for (i in 0 until arr.length()) {
                                val o = arr.getJSONObject(i)
                                comments.add(Comment(
                                    id = o.optInt("id", 0).toString(),
                                    postId = o.optInt("postId", 0).toString(),
                                    userId = o.optInt("userId", 0).toString(),
                                    username = o.optString("username", "User"),
                                    userProfileImage = o.optString("userProfileImage", ""),
                                    text = o.optString("text", ""),
                                    timestamp = o.optString("timestamp", "")
                                ))
                            }
                        }
                        adapter.notifyDataSetChanged()
                    }
                } catch (e: Exception) {
                    Log.e("Comments", "Error parsing: ${e.message}")
                }
            },
            { error -> Log.e("Comments", "Network error: ${error.message}") }) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer $token"
                return headers
            }
        }

        rq.add(req)
    }
}
