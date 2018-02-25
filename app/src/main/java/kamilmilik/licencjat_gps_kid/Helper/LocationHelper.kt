package kamilmilik.licencjat_gps_kid.Helper

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AlertDialog
import android.util.Log
import android.widget.Toast
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GooglePlayServicesUtil
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kamilmilik.licencjat_gps_kid.models.TrackingModel

/**
 * Created by kamil on 24.02.2018.
 */
class LocationHelper(
        var context: Context,
        var activity : Activity):GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener{
    var TAG : String = LocationHelper::class.java.simpleName
    private val MY_PERMISSION_REQUEST_CODE : Int = 7171
    private val PLAY_SERVICES_RES_REQUEST : Int = 7172
    private val UPDATE_INTERVAL : Int = 5000
    private val FASTEST_INTERVAL : Int = 3000
    private val DISTANCE : Int = 10

    lateinit private var locationRequest : LocationRequest
    var googleApiClient : GoogleApiClient? = null
    lateinit var lastLocation : Location
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


    private fun isPermissionChecked() : Boolean {
        return ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
    }

    private fun displayPermissionCheck() {
        ActivityCompat.requestPermissions(activity, arrayOf(
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_FINE_LOCATION
        ), MY_PERMISSION_REQUEST_CODE)
    }

    fun currentUserLocationAction() {
        if (isPermissionChecked()) {
            displayPermissionCheck()
        } else {
            if (googleApiClient == null) {
                buildGoogleApiClient()
                createLocationRequest()
            }
            if (googleApiClient != null) {
                if (googleApiClient!!.isConnected) {
                    setupCurrentLocation()
                }
            } else {
                Log.i(TAG, "googleApiClient == null")
            }
        }
    }
    fun setupCurrentLocation(){
        if (checkPlayServices()) {
            buildGoogleApiClient()
            createLocationRequest()
            displayLocation()
        }
    }
     fun checkPlayServices(): Boolean{
        var resultCode : Int = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context)
        if(resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, activity, PLAY_SERVICES_RES_REQUEST).show()
            } else {
                Toast.makeText(context, "This device is not supported", Toast.LENGTH_SHORT).show()
                activity.finish()
            }
            return false
        }
        return true
    }

     private fun buildGoogleApiClient() {
        googleApiClient = GoogleApiClient.Builder(context)
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
    fun connectToGoogleApi() {
        if (googleApiClient != null) {
            googleApiClient!!.connect()
        }
    }
    fun disconnectGoogleApi(){
        if (googleApiClient != null) {
            googleApiClient!!.disconnect()
        }
    }

    private fun displayLocation() {
        if (isPermissionChecked()) {
            return
        }
        checkLocationEnabled()

        try {
            lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient)
            if (lastLocation != null) {
                addLocationToFirebase()
            } else {
                Log.i(TAG, "Couldn't get the location")
            }

        } catch (e: SecurityException) {
            Log.i(TAG,"SecurityException:\n" + e.toString())
        }
    }
    private fun addLocationToFirebase(){
        var locations = FirebaseDatabase.getInstance().getReference("Locations")
        var currentUser = FirebaseAuth.getInstance().currentUser

        locations.child(currentUser!!.uid)
                .setValue(TrackingModel(currentUser.uid,
                        currentUser!!.email!!,
                        lastLocation.latitude.toString(),
                        lastLocation.longitude.toString()))
    }
     @SuppressLint("MissingPermission")
     private fun startLocationUpdates() {
        if (isPermissionChecked()) {
            return
        }
         checkLocationEnabled()
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }

     private fun checkLocationEnabled(): Boolean {
        if(!isLocationEnabled()){
            showAlert()
        }
        return isLocationEnabled()
    }

    private fun isLocationEnabled(): Boolean {
        var locationManager = activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER )|| locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun showAlert() {
        val dialog = AlertDialog.Builder(context)
        dialog.setTitle("Enable Location")
                .setMessage("Your Locations Settings is set to 'Off'.\nPlease Enable Location to " + "use this app")
                .setPositiveButton("Location Settings", DialogInterface.OnClickListener { paramDialogInterface, paramInt ->
                    val myIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    activity.startActivity(myIntent)
                })
                .setNegativeButton("Cancel", DialogInterface.OnClickListener { paramDialogInterface, paramInt ->
                    paramDialogInterface.cancel()
                })
        dialog.show()
    }
}