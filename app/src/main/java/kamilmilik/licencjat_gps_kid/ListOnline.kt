package kamilmilik.licencjat_gps_kid

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.Menu
import kotlinx.android.synthetic.main.activity_list_online.*
import android.content.Intent
import android.content.pm.PackageManager
import android.os.PersistableBundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import kamilmilik.licencjat_gps_kid.Helper.OnlineUserHelper
import kamilmilik.licencjat_gps_kid.Invite.EnterInviteActivity
import kamilmilik.licencjat_gps_kid.Invite.SendInviteActivity
import kamilmilik.licencjat_gps_kid.Login.LoginActivity
import kamilmilik.licencjat_gps_kid.models.User
import kamilmilik.licencjat_gps_kid.Utils.RecyclerViewAdapter
import kamilmilik.licencjat_gps_kid.Helper.FinderUserConnectionHelper
import kamilmilik.licencjat_gps_kid.Helper.LocationHelper
import kamilmilik.licencjat_gps_kid.Utils.OnItemClickListener


class ListOnline : AppCompatActivity(),
        OnItemClickListener{
    val TAG : String = ListOnline::class.java.simpleName


    //Firebase
    private var onlineUserHelper: OnlineUserHelper? = null
    //view
    lateinit var adapter : RecyclerViewAdapter
    lateinit var recyclerView : RecyclerView
    lateinit var valueList :ArrayList<User>
    //permission
    private val MY_PERMISSION_REQUEST_CODE : Int = 7171
    //Location
    private  var locationHelper : LocationHelper? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_online)

        setupRecyclerView()

        generateCodeButtonAction()
        enterCodeButtonAction()


        setupToolbar()

        setupFinderUserConnectionHelper()

        setupAddOnlineUserToDatabaseHelper()

        setupLocationHelper()

    }
    private fun setupFinderUserConnectionHelper(){
        var finderUserConnectionHelper  = FinderUserConnectionHelper(this@ListOnline, this, valueList, adapter, recyclerView)
        finderUserConnectionHelper.findFollowersConnection()
    }
    private fun setupLocationHelper(){
        locationHelper = LocationHelper(this, this@ListOnline)
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


    private fun setupAddOnlineUserToDatabaseHelper(){
        onlineUserHelper = OnlineUserHelper()
        onlineUserHelper!!.addOnlineUserToDatabase()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when(item!!.itemId){
            R.id.action_join->{//now it not work since if we click logout we are moved to login activity
                onlineUserHelper!!.joinUserAction()
            }
            R.id.action_logout->{
                onlineUserHelper!!.logoutUser()
                startActivity(Intent(this, LoginActivity::class.java))
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setupToolbar(){
        toolbar.title = "Presence System"
        setSupportActionBar(toolbar)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            MY_PERMISSION_REQUEST_CODE ->{
                    if(grantResults.isNotEmpty() && grantResults.get(0) == PackageManager.PERMISSION_GRANTED){
                        locationHelper!!.setupCurrentLocation()
                    }else{
                        Toast.makeText(this,"Permission denied", Toast.LENGTH_LONG).show()
                    }
            }
        }
    }
    override fun onStart() {
        super.onStart()
        locationHelper!!.connectToGoogleApi()
    }

    override fun onStop() {
        locationHelper!!.disconnectGoogleApi()
        super.onStop()
    }

    override fun onPostResume() {
        super.onPostResume()
        locationHelper!!.checkPlayServices()
    }

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        locationHelper!!.setupCurrentLocation()
    }
}
