package kamilmilik.licencjat_gps_kid

import android.location.Location
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.google.android.gms.maps.CameraUpdateFactory

import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kamilmilik.licencjat_gps_kid.models.TrackingModel
import java.text.DecimalFormat

class MapTrackingActivity : AppCompatActivity(), OnMapReadyCallback {
    var TAG : String = "MapTrackingActivity"
    private lateinit var map: GoogleMap
    private lateinit var followingUserId: String
    private lateinit var locations: DatabaseReference

    private var lat: Double? = null
    private var lng : Double? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_tracking)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        loadLocationsFromDatabaseForCurrentUser()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

    }
    private fun loadLocationsFromDatabaseForCurrentUser(){
        followingUserId = intent.getStringExtra("userId")
        lat = intent.getDoubleExtra("lat", 0.0)
        lng = intent.getDoubleExtra("lng",0.0)

        locations = FirebaseDatabase.getInstance().getReference("Locations")
        var query : Query = locations.orderByChild("userId").equalTo(followingUserId)
        query.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(dataSnapshot: DataSnapshot?) {
               for(singleSnapshot in dataSnapshot!!.children){
                var followingUserTracking = singleSnapshot.getValue(TrackingModel::class.java)

                   var followingUserLocation = LatLng(followingUserTracking.lat!!.toDouble(), followingUserTracking.lng!!.toDouble())

                   var followingUserLoc  = Location("")
                   followingUserLoc.latitude = followingUserTracking.lat!!.toDouble()
                   followingUserLoc.longitude = followingUserTracking.lng!!.toDouble()

                   var currentUserLocation = Location("")
                   currentUserLocation.latitude = lat!!
                   currentUserLocation.longitude = lng!!

                   map.clear()
                   //add followingUser marker
                   var distance = calculateDistanceBetweenUsers(currentUserLocation,followingUserLoc)
                   map.addMarker(MarkerOptions()
                           .position(followingUserLocation)
                           .title(followingUserTracking.email)
                           .snippet("Distance " + DecimalFormat("#.#").format(distance) + " km")
                           .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)))
                   map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat!!,lng!!),12.0f))
               }
                if(dataSnapshot.value == null){//nothing found
                    Log.i(TAG,"nothing found in onDataChange")
                }
                //add currentUser marker
                map.addMarker(MarkerOptions()
                        .position(LatLng(lat!!,lng!!))
                        .title(FirebaseAuth.getInstance().currentUser!!.email))
            }

            override fun onCancelled(databaseError: DatabaseError?) {
            }


        })
    }
    private fun calculateDistanceBetweenUsers(currentUserLocation : Location, followingUserLocation : Location): Double{
        var theta : Double = currentUserLocation.longitude - followingUserLocation.longitude
        var dist : Double = Math.sin(deg2rad(currentUserLocation.latitude)) *
                            Math.sin(deg2rad(followingUserLocation.latitude)) *
                            Math.cos(deg2rad(currentUserLocation.latitude)) *
                            Math.cos(deg2rad(followingUserLocation.latitude)) *
                            Math.cos(deg2rad(theta))
        dist = Math.acos(dist)
        dist = rad2deg(dist)
        dist = dist * 60 * 1.1515
        return dist

    }
    private fun deg2rad(deg : Double) : Double{
        return (deg * Math.PI / 180.0)
    }
    private fun rad2deg(rad : Double):Double{
        return (rad * 180.0 / Math.PI)
    }
}
