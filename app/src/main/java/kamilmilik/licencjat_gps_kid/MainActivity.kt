package kamilmilik.licencjat_gps_kid

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_main.*
import kamilmilik.licencjat_gps_kid.Login.LoginActivity
import kamilmilik.licencjat_gps_kid.Login.RegistrationActivity


class MainActivity : AppCompatActivity() {
    private val TAG = MainActivity::class.java.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        var firebaseAuth = FirebaseAuth.getInstance()
        if(firebaseAuth.currentUser != null){
            finish()
            val intent = Intent(this, ListOnline::class.java)
            startActivity(intent)
        }
        registrationButton.setOnClickListener(View.OnClickListener {
            Log.i(TAG,"registrationButton listener: registration button Action")
            val intent = Intent(this@MainActivity, RegistrationActivity::class.java)
            startActivity(intent)
        })

        loginButton.setOnClickListener(View.OnClickListener {
            Log.i(TAG,"loginButton listener: login button Action")
            val intent = Intent(this@MainActivity, LoginActivity::class.java)
            startActivity(intent)
        })

    }

}
