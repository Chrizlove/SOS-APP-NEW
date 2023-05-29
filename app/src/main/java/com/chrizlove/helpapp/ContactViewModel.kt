package com.chrizlove.helpapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import kotlinx.coroutines.Dispatchers
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class ContactViewModel (application: Application): AndroidViewModel(application){

    val contacts : LiveData<List<Contact>>
    val contactsRepository: ContactRepository

    init{
        val dao = ContactDataBase.getDatabase(application).getContactDao()
        contactsRepository = ContactRepository(dao)
        contacts = contactsRepository.allContacts
    }
    fun delete(contact: Contact) = viewModelScope.launch(Dispatchers.IO) {
        contactsRepository.delete(contact)
    }
    fun insert(contact: Contact) = viewModelScope.launch(Dispatchers.IO) {
        contactsRepository.insert(contact)
    }

}