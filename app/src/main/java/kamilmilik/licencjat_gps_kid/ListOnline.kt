package kamilmilik.licencjat_gps_kid

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.Menu
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_list_online.*
import com.google.firebase.database.DatabaseError
import android.content.Intent
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.google.firebase.auth.FirebaseUser
import kamilmilik.licencjat_gps_kid.Invite.EnterInviteActivity
import kamilmilik.licencjat_gps_kid.Invite.SendInviteActivity
import kamilmilik.licencjat_gps_kid.Login.LoginActivity
import kamilmilik.licencjat_gps_kid.Utils.ListOnlineViewHolder
import kamilmilik.licencjat_gps_kid.models.User
import kamilmilik.licencjat_gps_kid.models.UserUniqueKey
import java.util.Collections.frequency




class ListOnline : AppCompatActivity() {
    val TAG : String = "ListOnline"
    //Firebase
    lateinit var onlineRef : DatabaseReference
    lateinit var currentUserRef : DatabaseReference
    lateinit var counterRef : DatabaseReference
    var adapter: FirebaseRecyclerAdapter<User, ListOnlineViewHolder>? = null

    lateinit var listOnline : RecyclerView
    lateinit var layoutManager : RecyclerView.LayoutManager

    var clickedUser : User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_online)

        generateCodeButtonAction()
        enterCodeButtonAction()

        setupListOnlineRecycler()

        setupToolbar()

        setupFirebaseDatabaseReference()

        setupSystem()

        updateList()

        findFollowersConnection()
    }

    private fun setupToolbar(){
        toolbar.title = "Presence System"
        setSupportActionBar(toolbar)
    }
    private fun generateCodeButtonAction(){
        buttonToActivityGenerateCode.setOnClickListener({
            var intent  = Intent(this, SendInviteActivity::class.java)
            startActivity(intent)
        })
    }
    private fun enterCodeButtonAction(){
        buttonToActivityEnterInvite.setOnClickListener({
            var intent = Intent(this, EnterInviteActivity::class.java)
            startActivity(intent)
        })
    }
    private fun setupListOnlineRecycler(){
        layoutManager = LinearLayoutManager(this)
        listOnline = findViewById(R.id.listOnline)
        listOnline.setHasFixedSize(true)
        listOnline.layoutManager = layoutManager
    }
    private fun setupFirebaseDatabaseReference(){
        onlineRef = FirebaseDatabase.getInstance().reference.child(".info/connected")
        counterRef = FirebaseDatabase.getInstance().getReference("last_online")
        currentUserRef = FirebaseDatabase.getInstance().getReference("last_online").child(FirebaseAuth.getInstance().currentUser!!.uid)
    }
    private fun updateList() {
        Log.i(TAG,"updateList: update recycler view list")
        var currentUser = FirebaseAuth.getInstance().currentUser
        val reference = FirebaseDatabase.getInstance().reference
        val query = reference.child("followers")
                .orderByKey()
                .equalTo(currentUser!!.uid)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (singleSnapshot in dataSnapshot.children) {
                    for(childSingleSnapshot in singleSnapshot.children){
                        Log.i(TAG,"value followers: " + childSingleSnapshot.value)
                    }
                }
                if(dataSnapshot.value == null){//nothing found
                    Log.i(TAG,"nothing found in onDataChange in followers")
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {
                Log.i(TAG,"onCancelled: " + databaseError.message)
            }
        })
        adapter = object : FirebaseRecyclerAdapter<User, ListOnlineViewHolder>(
                User::class.java, R.layout.user_layout, ListOnlineViewHolder::class.java, query
        ) {
            protected override fun populateViewHolder(viewHolder: ListOnlineViewHolder, model: User, position: Int) {
                //viewHolder.txtEmail.text = model.email + " " + model.userId

//                viewHolder.itemView.setOnClickListener(View.OnClickListener { v ->
//                    findUserByIdInAccountSettings(model)
//                })
            }

        }
        adapter!!.notifyDataSetChanged()
        listOnline.adapter = adapter
    }
    private fun findFollowersConnection(){
        Log.i(TAG,"findFollowersConnection, current user id : " + FirebaseAuth.getInstance().currentUser!!.uid)
        var currentUser = FirebaseAuth.getInstance().currentUser
        val reference = FirebaseDatabase.getInstance().reference
        findFollowersUser(reference, currentUser!!)
        findFollowingUser(reference, currentUser!!)

    }
    private fun findFollowersUser(reference : DatabaseReference, currentUser : FirebaseUser){
        var valueList :ArrayList<User> = ArrayList()
        val query = reference.child("followers")
                .orderByKey()
                .equalTo(currentUser!!.uid)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (singleSnapshot in dataSnapshot.children) {
                    for(childSingleSnapshot in singleSnapshot.children){
                         var userFollowers = childSingleSnapshot.child("user").getValue(User::class.java)
                         Log.i(TAG,"value followers: " + userFollowers.userId + " " + userFollowers.email)
                        valueList.add(userFollowers)
                    }
                }
                if(dataSnapshot.value == null){//nothing found
                    Log.i(TAG,"nothing found in onDataChange in followers")
                }else{
                    for(user in valueList){
                        Log.i(TAG,"user complete : " + user.email)
                    }
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {
                Log.i(TAG,"onCancelled: " + databaseError.message)
            }
        })

    }
    private fun findFollowingUser(reference : DatabaseReference, currentUser : FirebaseUser){
        var valueList :ArrayList<User> = ArrayList()
        val query2 = reference.child("following")
                .orderByKey()
                .equalTo(currentUser!!.uid)
        query2.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (singleSnapshot in dataSnapshot.children) {
                    for(childSingleSnapshot in singleSnapshot.children){
                        var userFollowing = childSingleSnapshot.child("user").getValue(User::class.java)
                        Log.i(TAG,"value following: " + userFollowing.userId + " " + userFollowing.email)
                        valueList.add(userFollowing)
                    }
                }
                if(dataSnapshot.value == null){//nothing found
                    Log.i(TAG,"nothing found in onDataChange in following")
                }else{
                    for(user in valueList){
                        Log.i(TAG,"user complete : " + user.email)
                    }
                }

            }
            override fun onCancelled(databaseError: DatabaseError) {
                Log.i(TAG,"onCancelled: " + databaseError.message)
            }
        })


    }
    /**
     * find given user in database
     * @param user
     */
    private fun findUserByIdInAccountSettings(user : User){
        Log.i(TAG, " findUserByIdInAccountSettings: find given user in db")
        val reference = FirebaseDatabase.getInstance().reference
        val query = reference.child(getString(R.string.db_user_account_settings_node_name))
                .orderByChild("userId").equalTo(user.userId)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (singleSnapshot in dataSnapshot.children) {
                    Log.d(TAG, "onDataChange: found user:" + singleSnapshot.value)
                    clickedUser = singleSnapshot.getValue(User::class.java)
                }
                Log.i(TAG,"found user: "  + clickedUser!!.email + " " + clickedUser!!.userId)
                if(!FirebaseAuth.getInstance().currentUser!!.uid.equals(clickedUser!!.userId)){ // if user not clicked self
                    addConnectedUserToDatabase()
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {
            }
        })
    }

    /**
     * add new connection between two account
     */
    private fun addConnectedUserToDatabase(){
        Log.i(TAG, "addConnectedUserToDatabase: add user data following and followers to database")
        FirebaseDatabase.getInstance().reference
                .child("following")
                .child(FirebaseAuth.getInstance().getCurrentUser()!!.uid)
                .child(clickedUser!!.userId)
                .child("userId")
                .setValue(clickedUser!!.userId);

        FirebaseDatabase.getInstance().reference
                .child("followers")
                .child(clickedUser!!.userId)
                .child(FirebaseAuth.getInstance().getCurrentUser()!!.uid)
                .child("userId")
                .setValue(FirebaseAuth.getInstance().getCurrentUser()!!.uid)
    }
    private fun setupSystem() {
        Log.i(TAG, "setupSystem: set up online account to list")
        //To make your app data update in realtime, you should add a ValueEventListener to the reference you just created.
//        counterRef.addValueEventListener(object : ValueEventListener {
//            override fun onCancelled(p0: DatabaseError?) {
//                System.err.println("Listener was cancelled")
//            }
//            //The onDataChange() method in this class is triggered once when the listener is attached and again every time the data changes,
//            // including the children.
//            override fun onDataChange(dataSnapshot: DataSnapshot) {
//                for (postSnapshot in dataSnapshot.children) {
//                    val user = postSnapshot.getValue(User::class.java)
//                    Log.i("ListOnline", "" + user.email)
//                }
//            }
//        })

        onlineRef = FirebaseDatabase.getInstance().reference.child(".info/connected")
        counterRef = FirebaseDatabase.getInstance().getReference("last_online")
        currentUserRef = FirebaseDatabase.getInstance().getReference("last_online").child(FirebaseAuth.getInstance().currentUser!!.uid)
        onlineRef.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError?) {
                System.err.println("Listener was cancelled")
            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                var connected = dataSnapshot.getValue(Boolean::class.java)
                if (connected) {
                    currentUserRef.onDisconnect().removeValue()//Remove the value at this location when the client disconnects
                    //add to last_online current user
                    counterRef.child(FirebaseAuth.getInstance().currentUser!!.uid).setValue(User(FirebaseAuth.getInstance().currentUser!!.uid,FirebaseAuth.getInstance().currentUser!!.email!!))
                    adapter!!.notifyDataSetChanged()
                }
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when(item!!.itemId){
            R.id.action_join->{//now it not work since if we click logout we are moved to login activity
                counterRef.child(FirebaseAuth.getInstance().currentUser!!.uid).setValue(User(FirebaseAuth.getInstance().getCurrentUser()!!.uid,FirebaseAuth.getInstance().getCurrentUser()!!.getEmail()!!))
            }
            R.id.action_logout->{
                currentUserRef.onDisconnect().removeValue()
                counterRef.onDisconnect().removeValue()
                currentUserRef.removeValue()
                FirebaseAuth.getInstance().signOut()
                startActivity(Intent(this, LoginActivity::class.java))
            }
        }
        return super.onOptionsItemSelected(item)
    }


}
