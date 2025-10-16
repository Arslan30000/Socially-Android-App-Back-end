package com.example.i230572_i230689

import android.graphics.BitmapFactory
import android.util.Base64
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class FollowersListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: UserListAdapter
    private lateinit var userList: MutableList<User>
    private lateinit var dbRef: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private var listType: String = "followers"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_followers_list)

        recyclerView = findViewById(R.id.recyclerViewFollowers)
        recyclerView.layoutManager = LinearLayoutManager(this)
        userList = mutableListOf()
        adapter = UserListAdapter(userList)
        recyclerView.adapter = adapter

        dbRef = FirebaseDatabase.getInstance().getReference("users")
        auth = FirebaseAuth.getInstance()

        listType = intent.getStringExtra("type") ?: "followers"
        loadUsers()
    }

    private fun loadUsers() {
        val currentUid = auth.currentUser?.uid ?: return

        dbRef.child(currentUid).child(listType).get().addOnSuccessListener { snapshot ->
            val uids = snapshot.children.mapNotNull { it.key }

            userList.clear()
            for (uid in uids) {
                dbRef.child(uid).get().addOnSuccessListener { userSnap ->
                    val username = userSnap.child("username").value?.toString() ?: return@addOnSuccessListener
                    val imageBase64 = userSnap.child("imageBase64").value?.toString() ?: ""

                    val user = User(
                        uid = uid,
                        username = username,
                        imageBase64 = imageBase64
                    )

                    userList.add(user)
                    adapter.notifyDataSetChanged()
                }
            }
        }
    }
}
