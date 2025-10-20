package com.example.i230572_i230689

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
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
        listenForNewRequests()
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
                                            timestamp = System.currentTimeMillis()
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

    private fun listenForNewRequests() {
        val uid = auth.currentUser?.uid ?: return
        db.child("users").child(uid).child("followRequests")
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    val requesterId = snapshot.key ?: return
                    if (snapshot.value == true) {
                        db.child("users").child(requesterId).child("username").get()
                            .addOnSuccessListener {
                                val name = it.value?.toString() ?: "Someone"
                                showNotification("New Follow Request", "$name sent you a follow request")
                            }
                    }
                }
                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onChildRemoved(snapshot: DataSnapshot) {}
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
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

    private fun showNotification(title: String, body: String) {
        val channelId = "follow_notifications"
        val notificationId = System.currentTimeMillis().toInt()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Follow Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(this)) {
            notify(notificationId, builder.build())
        }
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
