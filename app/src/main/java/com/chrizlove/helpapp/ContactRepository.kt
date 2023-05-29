package com.chrizlove.helpapp

import androidx.lifecycle.LiveData

class ContactRepository(private val contactDAO: ContactDAO) {

    val allContacts: LiveData<List<Contact>> = contactDAO.getAllContacts()

    suspend fun insert(contact: Contact){
        contactDAO.insert(contact)
    }

    suspend fun delete(contact: Contact){
        contactDAO.delete(contact)
    }
}