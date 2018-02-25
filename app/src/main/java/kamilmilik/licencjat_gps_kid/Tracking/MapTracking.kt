package kamilmilik.licencjat_gps_kid.Tracking

import android.location.Location
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

/**
 * Created by kamil on 25.02.2018.
 */
class MapTracking(var mapFragment : SupportMapFragment): OnMapReadyCallback {
    private val TAG : String = MapTracking::class.java.simpleName
    private lateinit var map: GoogleMap

    private lateinit var locations: DatabaseReference
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
    }

    fun initMapAsynch(){
        this.mapFragment.getMapAsync(this)
    }
    fun loadLocationsFromDatabaseForCurrentUser(followingUserId : String, currentUserLat : Double, currentUserLng : Double){

        locations = FirebaseDatabase.getInstance().getReference("Locations")
        var query : Query = locations.orderByChild("userId").equalTo(followingUserId)
        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot?) {
                for(singleSnapshot in dataSnapshot!!.children){
                    var followingUserTracking = singleSnapshot.getValue(TrackingModel::class.java)
                    var followingUserLocation = LatLng(followingUserTracking.lat!!.toDouble(), followingUserTracking.lng!!.toDouble())

                    var followingUserLoc  = Location("")
                    followingUserLoc.latitude = followingUserTracking.lat!!.toDouble()
                    followingUserLoc.longitude = followingUserTracking.lng!!.toDouble()

                    var currentUserLocation = Location("")
                    currentUserLocation.latitude = currentUserLat!!
                    currentUserLocation.longitude = currentUserLng!!

                    map.clear()
                    //add followingUser marker
                    var measure : String?
                    var distance : Float = currentUserLocation.distanceTo(followingUserLoc)
                    if (distance > 1000){
                        distance = (distance / 1000)
                        measure = " km"
                    }else{
                        measure = " m"
                    }
                    Log.i(TAG,"ustawiam marker na pozycje: " + followingUserLocation + " dla " + followingUserTracking.email)
                    map.addMarker(MarkerOptions()
                            .position(followingUserLocation)
                            .title(followingUserTracking.email)
                            .snippet("Distance " + DecimalFormat("#.#").format(distance) + measure)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)))
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(currentUserLat!!, currentUserLng!!),12.0f))
                }
                if(dataSnapshot.value == null){//nothing found
                    Log.i(TAG,"nothing found in onDataChange")
                }
                //add currentUser marker
                Log.i(TAG,"ustawiam marker na pozycje: " + currentUserLat +" " + currentUserLng + " dla " + FirebaseAuth.getInstance().currentUser!!.email)
                map.addMarker(MarkerOptions()
                        .position(LatLng(currentUserLat!!, currentUserLng!!))
                        .title(FirebaseAuth.getInstance().currentUser!!.email))
            }

            override fun onCancelled(databaseError: DatabaseError?) {
            }


        })
    }
}