package kamilmilik.licencjat_gps_kid

import android.content.Context
import android.content.DialogInterface
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
import android.location.Location
import android.location.LocationManager
import android.os.PersistableBundle
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AlertDialog
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.google.firebase.auth.FirebaseUser
import kamilmilik.licencjat_gps_kid.Invite.EnterInviteActivity
import kamilmilik.licencjat_gps_kid.Invite.SendInviteActivity
import kamilmilik.licencjat_gps_kid.Login.LoginActivity
import kamilmilik.licencjat_gps_kid.models.User
import kamilmilik.licencjat_gps_kid.Utils.RecyclerViewAdapter
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GooglePlayServicesUtil
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import kamilmilik.licencjat_gps_kid.Utils.OnItemClickListener
import kamilmilik.licencjat_gps_kid.models.TrackingModel


class ListOnline : AppCompatActivity(),
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener,
        OnItemClickListener{


    override fun onConnected(p0: Bundle?) {
        //displayLocation()
        startLocationUpdates()
    }

    override fun onConnectionSuspended(p0: Int) {
        googleApiClient!!.connect()
    }

    override fun onConnectionFailed(p0: ConnectionResult) {
        Log.i(TAG,"onConnectionFailed: google maps" + p0.errorMessage)
    }

    override fun onLocationChanged(location: Location?) {
        lastLocation = location!!
        displayLocation()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            MY_PERMISSION_REQUEST_CODE ->{
                    if(grantResults.isNotEmpty() && grantResults.get(0) == PackageManager.PERMISSION_GRANTED){
                        if(checkPlayServices()){
                            buildGoogleApiClient()
                            createLocationRequest()
                            displayLocation()
                        }
                }
            }
        }
    }
    override fun onStart() {
        super.onStart()
        if(googleApiClient != null){
            googleApiClient!!.connect()
        }
    }

    override fun onStop() {
        if(googleApiClient != null){
            googleApiClient!!.disconnect()
        }
        super.onStop()
    }

    override fun onPostResume() {
        super.onPostResume()
        checkPlayServices()
    }

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        buildGoogleApiClient()
        createLocationRequest()
        displayLocation()
    }
    val TAG : String = "ListOnline"
    //Firebase
    lateinit var onlineRef : DatabaseReference
    lateinit var currentUserRef : DatabaseReference
    lateinit var counterRef : DatabaseReference
    lateinit var locations : DatabaseReference
    //view
    lateinit var adapter : RecyclerViewAdapter
    lateinit var recyclerView : RecyclerView
    lateinit var valueList :ArrayList<User>
    //Location
    private val MY_PERMISSION_REQUEST_CODE : Int = 7171
    private val PLAY_SERVICES_RES_REQUEST : Int = 7172
    private val UPDATE_INTERVAL : Int = 5000
    private val FASTEST_INTERVAL : Int = 3000
    private val DISTANCE : Int = 10

    lateinit private var locationRequest : LocationRequest
    private  var googleApiClient : GoogleApiClient? = null
    lateinit private var lastLocation : Location

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_online)

        setupRecyclerView()

        generateCodeButtonAction()
        enterCodeButtonAction()


        setupToolbar()

        findFollowersConnection()

        setupSystem()

        currentUserLocationAction()


    }



    //----------tracking-map-------------------------------------------------------------
    private fun currentUserLocationAction(){
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(this, arrayOf(
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
            ), MY_PERMISSION_REQUEST_CODE)
        } else {
            if (googleApiClient == null) {
                buildGoogleApiClient()
                createLocationRequest()
            }
            if (googleApiClient != null) {
                if (googleApiClient!!.isConnected) {
                    if (checkPlayServices()) {
                        buildGoogleApiClient()
                        createLocationRequest()
                        displayLocation()
                    }
                }
            } else {
                Log.i(TAG,"mGoogleApiClient == null")
            }
        }
    }

    private fun checkPlayServices(): Boolean{
        var resultCode : Int = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this)
        if(resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this, PLAY_SERVICES_RES_REQUEST).show()
            } else {
                Toast.makeText(this, "This device is not supported", Toast.LENGTH_SHORT).show()
                finish()
            }
            return false
        }
        return true
    }

    private fun buildGoogleApiClient() {
        googleApiClient = GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build()
        googleApiClient!!.connect()
    }
    private fun createLocationRequest() {
        locationRequest = LocationRequest()
        locationRequest.interval = UPDATE_INTERVAL.toLong()
        locationRequest.fastestInterval = FASTEST_INTERVAL.toLong()
        locationRequest.smallestDisplacement = DISTANCE.toFloat()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

    }

    private fun displayLocation() {
        if(ActivityCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ( ActivityCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)){
            return
        }
        checkLocationEnabled()
        try{
            lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient)
            if(lastLocation != null){
                var currentUser = FirebaseAuth.getInstance().currentUser
                //update to firebase
                locations.child(currentUser!!.uid)
                        .setValue(TrackingModel(currentUser.uid,
                                currentUser!!.email!!,
                                lastLocation.latitude.toString(),
                                lastLocation.longitude.toString()))


            }else{
                Log.i(TAG, "Couldn't get the location")
            }

        }catch (e: SecurityException) {
            Toast.makeText(this,
                    "SecurityException:\n" + e.toString(),
                    Toast.LENGTH_LONG).show()
        }
    }
    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
            Log.i(TAG,"no permission")
            return
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    private fun checkLocationEnabled(): Boolean {
        if(!isLocationEnabled())
            showAlert();
        return isLocationEnabled();
    }

    private fun isLocationEnabled(): Boolean {
        var locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun showAlert() {
        val dialog = AlertDialog.Builder(this)
        dialog.setTitle("Enable Location")
                .setMessage("Your Locations Settings is set to 'Off'.\nPlease Enable Location to " + "use this app")
                .setPositiveButton("Location Settings", DialogInterface.OnClickListener { paramDialogInterface, paramInt ->
                    val myIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    startActivity(myIntent)
                })
                .setNegativeButton("Cancel", DialogInterface.OnClickListener { paramDialogInterface, paramInt -> })
        dialog.show()
    }
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
        intent.putExtra("currentUserLat", lastLocation.latitude)
        intent.putExtra("currentUserLng", lastLocation.longitude)
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

    private fun findFollowersConnection(){
        Log.i(TAG,"findFollowersConnection, current user id : " + FirebaseAuth.getInstance().currentUser!!.uid)
        var currentUser = FirebaseAuth.getInstance().currentUser
        val reference = FirebaseDatabase.getInstance().reference
        findFollowersUser(reference, currentUser!!)
        findFollowingUser(reference, currentUser!!)

    }
    private fun findFollowersUser(reference : DatabaseReference, currentUser : FirebaseUser){
        val query = reference.child("followers")
                .orderByKey()
                .equalTo(currentUser!!.uid)
        query.addValueEventListener(object : ValueEventListener {
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
                    adapter = RecyclerViewAdapter(this@ListOnline, valueList)
                    recyclerView.adapter = adapter
                    adapter.setClickListener(this@ListOnline)
                    adapter.notifyDataSetChanged()
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {
                Log.i(TAG,"onCancelled: " + databaseError.message)
            }
        })

    }
    private fun findFollowingUser(reference : DatabaseReference, currentUser : FirebaseUser){
        val query = reference.child("following")
                .orderByKey()
                .equalTo(currentUser!!.uid)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
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
                    adapter = RecyclerViewAdapter(this@ListOnline, valueList)
                    recyclerView.adapter = adapter
                    adapter.setClickListener(this@ListOnline)

                    adapter.notifyDataSetChanged()
                }

            }
            override fun onCancelled(databaseError: DatabaseError) {
                Log.i(TAG,"onCancelled: " + databaseError.message)
            }
        })


    }


    private fun setupSystem() {
        Log.i(TAG, "setupSystem: set up online account to list")
        locations = FirebaseDatabase.getInstance().getReference("Locations")
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
