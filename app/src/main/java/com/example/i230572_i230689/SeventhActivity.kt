package com.example.i230572_i230689

import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Base64
import android.widget.*
import android.os.Bundle
import android.widget.LinearLayout.LayoutParams
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class SeventhActivity : AppCompatActivity() {

    private lateinit var searchInput: EditText
    private lateinit var resultsLayout: LinearLayout
    private lateinit var tabTop: TextView
    private lateinit var tabAccounts: TextView
    private lateinit var tabTags: TextView
    private lateinit var tabPlaces: TextView
    private lateinit var dbRef: DatabaseReference
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.searching)

        auth = FirebaseAuth.getInstance()
        dbRef = FirebaseDatabase.getInstance().getReference("users")

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
        val addedUids = mutableSetOf<String>()
        val currentUid = auth.currentUser?.uid ?: return

        dbRef.get().addOnSuccessListener { snapshot ->
            for (userSnap in snapshot.children) {
                val username = userSnap.child("username").value?.toString() ?: continue
                val targetUid = userSnap.key ?: continue

                if (targetUid == currentUid) continue

                if (username.equals(query, ignoreCase = true) && !addedUids.contains(targetUid)) {
                    addedUids.add(targetUid)
                    val imageBase64 = userSnap.child("imageBase64").value?.toString()

                    val followRequests = userSnap.child("followRequests")
                    val alreadyRequested = followRequests.hasChild(currentUid)

                    addUserResult(username, imageBase64, targetUid, alreadyRequested)
                }
            }
        }
    }

    private fun addUserResult(username: String, imageBase64: String?, targetUid: String, alreadyRequested: Boolean) {
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

        if (alreadyRequested) {
            followBtn.text = "Requested"
            followBtn.isEnabled = false
            followBtn.setBackgroundColor(getColor(R.color.light_grey))
            followBtn.setTextColor(Color.BLACK)
        } else {
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

        row.addView(imgView)
        row.addView(textView)
        row.addView(followBtn)
        resultsLayout.addView(row)
    }

    private fun sendFollowRequest(targetUid: String) {
        val currentUid = auth.currentUser?.uid ?: return
        dbRef.child(targetUid).child("followRequests").child(currentUid).setValue(true)
        Toast.makeText(this, "Follow request sent", Toast.LENGTH_SHORT).show()
    }
}
