package org.catrobat.catroid.content

import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import org.catrobat.catroid.CatroidApplication

object FireBaseManager {

    // Убедитесь, что FirebaseApp инициализирован
    fun initializeFirebase() {
        Log.d("Firebase", "Initialization")
        if (!FirebaseApp.getApps(CatroidApplication.getAppContext()).isEmpty()) return
        FirebaseApp.initializeApp(CatroidApplication.getAppContext())
    }

    fun readFromDatabase(databaseUrl: String, key: String, callback: (String?) -> Unit) {
        initializeFirebase()
        Log.d("Firebase", "read from database")
        val database: DatabaseReference = FirebaseDatabase.getInstance(databaseUrl).reference
        Log.d("Firebase", "Initialization 2")
        database.child(key).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                callback(snapshot.value?.toString() ?: "No data")
            }

            override fun onCancelled(error: DatabaseError) {
                callback(null) // Обработка ошибок
            }
        })
        Log.d("Firebase", "End")
    }

    fun writeToDatabase(databaseUrl: String, key: String, value: String) {
        initializeFirebase()
        Log.d("Firebase", "write to database")
        val database: DatabaseReference = FirebaseDatabase.getInstance(databaseUrl).reference
        Log.d("Firebase", "Initialization 2")
        database.child(key).setValue(value)
        Log.d("Firebase", "End")
    }

    fun deleteFromDatabase(databaseUrl: String, key: String) {
        initializeFirebase()
        Log.d("Firebase", "delete fromdatabase")
        val database: DatabaseReference = FirebaseDatabase.getInstance(databaseUrl).reference
        Log.d("Firebase", "Initialization 2")
        database.child(key).removeValue()
        Log.d("Firebase", "End")
    }
}