package com.chrizlove.helpapp

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName="contactsTable")
class Contact (@ColumnInfo(name="c_name")val name: String, @ColumnInfo(name="c_number") val c_number: String) {
    @PrimaryKey(autoGenerate = true) var id=0
}

