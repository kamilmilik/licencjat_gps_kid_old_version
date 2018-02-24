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
import android.content.pm.PackageManager
import android.os.PersistableBundle
import android.view.MenuItem
import android.view.View
import kamilmilik.licencjat_gps_kid.Invite.EnterInviteActivity
import kamilmilik.licencjat_gps_kid.Invite.SendInviteActivity
import kamilmilik.licencjat_gps_kid.Login.LoginActivity
import kamilmilik.licencjat_gps_kid.models.User
import kamilmilik.licencjat_gps_kid.Utils.RecyclerViewAdapter
import kamilmilik.licencjat_gps_kid.Utils.FinderUserConnectionHelper
import kamilmilik.licencjat_gps_kid.Utils.LocationHelper
import kamilmilik.licencjat_gps_kid.Utils.OnItemClickListener


class ListOnline : AppCompatActivity(),
        OnItemClickListener{


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            MY_PERMISSION_REQUEST_CODE ->{
                    if(grantResults.isNotEmpty() && grantResults.get(0) == PackageManager.PERMISSION_GRANTED){
                        if(locationHelper!!.checkPlayServices()){
                            locationHelper!!.buildGoogleApiClient()
                            locationHelper!!.createLocationRequest()
                            locationHelper!!.displayLocation()
                        }
                }
            }
        }
    }
    override fun onStart() {
        super.onStart()
        if(locationHelper!!.googleApiClient != null){
            locationHelper!!.googleApiClient!!.connect()
        }
    }

    override fun onStop() {
        if(locationHelper!!.googleApiClient != null){
            locationHelper!!.googleApiClient!!.disconnect()
        }
        super.onStop()
    }

    override fun onPostResume() {
        super.onPostResume()
        locationHelper!!.checkPlayServices()
    }

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        locationHelper!!.buildGoogleApiClient()
        locationHelper!!.createLocationRequest()
        locationHelper!!.displayLocation()
    }
    val TAG : String = ListOnline::class.java.simpleName
    //Firebase
    lateinit var onlineRef : DatabaseReference
    lateinit var currentUserRef : DatabaseReference
    lateinit var counterRef : DatabaseReference
    //view
    lateinit var adapter : RecyclerViewAdapter
    lateinit var recyclerView : RecyclerView
    lateinit var valueList :ArrayList<User>
    //Location
    private val MY_PERMISSION_REQUEST_CODE : Int = 7171

    private  var locationHelper : LocationHelper? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_online)

        setupRecyclerView()

        generateCodeButtonAction()
        enterCodeButtonAction()


        setupToolbar()

        setupFinderUserConnectionHelper()

        addOnlineUserToDatabase()

        setupLocationHelper()

    }
    private fun setupFinderUserConnectionHelper(){
        var finderUserConnectionHelper  = FinderUserConnectionHelper(this@ListOnline,this,valueList,adapter,recyclerView)
        finderUserConnectionHelper.findFollowersConnection()
    }
    private fun setupLocationHelper(){
        locationHelper = LocationHelper(this,this@ListOnline)
        locationHelper!!.currentUserLocationAction()
    }


    //----------tracking-map-------------------------------------------------------------

    //-----------firebase--------------------------------------------------------------------
    private fun setupRecyclerView(){
        recyclerView =  findViewById(R.id.listOnline)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(this)

        valueList = ArrayList()
        adapter = RecyclerViewAdapter(this@ListOnline, valueList)
        recyclerView.adapter = adapter
        adapter.setClickListener(this)
    }
    override fun setOnItemClick(view: View, position: Int) {
        Log.i(TAG,"setOnItemClick: clicked to item view in RecyclerView : position: "+ position + " user " + valueList.get(position).email)
        var intent = Intent(this,MapTrackingActivity::class.java)
        intent.putExtra("followingUserId", valueList.get(position).userId)
        intent.putExtra("currentUserLat", locationHelper!!.lastLocation.latitude)
        intent.putExtra("currentUserLng", locationHelper!!.lastLocation.longitude)
        startActivity(intent)
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


    private fun addOnlineUserToDatabase() {
        Log.i(TAG, "addOnlineUserToDatabase: set up online account to list")
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
                   // adapter!!.notifyDataSetChanged()
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

    private fun setupToolbar(){
        toolbar.title = "Presence System"
        setSupportActionBar(toolbar)
    }

}
