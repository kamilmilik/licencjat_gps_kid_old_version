package kamilmilik.licencjat_gps_kid.Helper

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import kamilmilik.licencjat_gps_kid.Utils.OnItemClickListener
import kamilmilik.licencjat_gps_kid.Utils.RecyclerViewAdapter
import kamilmilik.licencjat_gps_kid.models.User

/**
 * Created by kamil on 24.02.2018.
 */
class FinderUserConnectionHelper(var context : Context, var listener : OnItemClickListener, var valueList : ArrayList<User>, var adapter : RecyclerViewAdapter, var recyclerView: RecyclerView){
    private val TAG = FinderUserConnectionHelper::class.java.simpleName
    fun findFollowersConnection(){
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
                    updateRecyclerView()
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
                    updateRecyclerView()
                }

            }
            override fun onCancelled(databaseError: DatabaseError) {
                Log.i(TAG,"onCancelled: " + databaseError.message)
            }
        })
    }
    private fun updateRecyclerView(){
        adapter = RecyclerViewAdapter(context, valueList)
        recyclerView.adapter = adapter
        adapter.setClickListener(listener)

        adapter.notifyDataSetChanged()
    }

}