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
        database.child(key).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                callback(snapshot.value?.toString() ?: "No data")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error reading data: ${error.message}", error.toException())
                callback(null) // Обработка ошибок
            }
        })
        Log.d("Firebase", "End")
    }

    fun writeToDatabase(databaseUrl: String, key: String, value: String) {
        initializeFirebase()
        Log.d("Firebase", "write to database")
        val database: DatabaseReference = FirebaseDatabase.getInstance(databaseUrl).reference
        database.child(key).setValue(value)
            .addOnSuccessListener {
                Log.d("Firebase", "Data written successfully")
            }
            .addOnFailureListener { error ->
                Log.e("Firebase", "Error writing data: ${error.message}", error)
            }
        Log.d("Firebase", "End")
    }

    fun deleteFromDatabase(databaseUrl: String, key: String) {
        initializeFirebase()
        Log.d("Firebase", "delete from database")
        val database: DatabaseReference = FirebaseDatabase.getInstance(databaseUrl).reference
        database.child(key).removeValue()
            .addOnSuccessListener {
                Log.d("Firebase", "Data deleted successfully")
            }
            .addOnFailureListener { error ->
                Log.e("Firebase", "Error deleting data: ${error.message}", error)
            }
        Log.d("Firebase", "End")
    }
}