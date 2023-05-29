package com.chrizlove.helpapp

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = arrayOf(Contact::class), version = 1, exportSchema = false)
abstract class ContactDataBase: RoomDatabase() {

    abstract fun getContactDao(): ContactDAO

    companion object {
        // Singleton prevents multiple instances of database opening at the
        // same time.
        @Volatile
        private var INSTANCE: ContactDataBase? = null

        fun getDatabase(context: Context): ContactDataBase {
            // if the INSTANCE is not null, then return it,
            // if it is, then create the database
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ContactDataBase::class.java,
                    "contacts_database"
                ).build()
                INSTANCE = instance
                // return instance
                instance
            }
        }
    }
}