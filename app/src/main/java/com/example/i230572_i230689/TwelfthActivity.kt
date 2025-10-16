package com.example.i230572_i230689

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class TwelfthActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RequestAdapter
    private val requestsList = mutableListOf<FollowRequest>()
    private lateinit var db: DatabaseReference
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.your_activity)

        db = FirebaseDatabase.getInstance().reference
        recyclerView = findViewById(R.id.followRequestRecycler)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = RequestAdapter(requestsList) { request ->
            acceptFollowRequest(request)
        }
        recyclerView.adapter = adapter

        loadFollowRequests()

        setupNavigation()
    }

    private fun loadFollowRequests() {
        val uid = auth.currentUser?.uid ?: return
        db.child("users").child(uid).child("followRequests").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                requestsList.clear()
                for (req in snapshot.children) {
                    val requesterId = req.key ?: continue
                    if (req.value == true) {
                        db.child("users").child(requesterId).addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(data: DataSnapshot) {
                                val user = data.getValue(User::class.java)
                                user?.let {
                                    requestsList.add(
                                        FollowRequest(
                                            uid = user.uid,
                                            username = user.username,
                                            imageBase64 = user.imageBase64,
                                            timestamp = System.currentTimeMillis() // Replace later with actual
                                        )
                                    )
                                    adapter.notifyDataSetChanged()
                                }
                            }
                            override fun onCancelled(error: DatabaseError) {}
                        })
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun acceptFollowRequest(request: FollowRequest) {
        val currentUid = auth.currentUser?.uid ?: return
        val requesterUid = request.uid


        db.child("users").child(currentUid).child("followers").child(requesterUid).setValue(true)
        db.child("users").child(requesterUid).child("following").child(currentUid).setValue(true)

        db.child("users").child(currentUid).child("followRequests").child(requesterUid).removeValue()

        requestsList.remove(request)
        adapter.notifyDataSetChanged()
    }

    private fun setupNavigation() {
        findViewById<RelativeLayout>(R.id.following_tab).setOnClickListener {
            startActivity(Intent(this, EleventhActivity::class.java))
            finish()
        }

        findViewById<ImageView>(R.id.search_icon).setOnClickListener {
            startActivity(Intent(this, SeventhActivity::class.java))
            finish()
        }

        findViewById<ImageView>(R.id.home_icon).setOnClickListener {
            startActivity(Intent(this, FifthActivity::class.java))
            finish()
        }

        findViewById<ImageView>(R.id.post_icon).setOnClickListener {
            startActivity(Intent(this, FifteenthActivity::class.java))
            finish()
        }

        findViewById<de.hdodenhof.circleimageview.CircleImageView>(R.id.profile_icon).setOnClickListener {
            startActivity(Intent(this, LastActivity::class.java))
        }
    }
}
