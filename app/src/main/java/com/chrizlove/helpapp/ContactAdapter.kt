package com.chrizlove.helpapp

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ContactAdapter(private val context: Context): RecyclerView.Adapter<ContactAdapter.ContactViewHolder>() {

    val contactList = ArrayList<Contact>()

    inner class ContactViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val contactNameAD = itemView?.findViewById<TextView>(R.id.contactNameCardView)
        val contactNumberAD = itemView?.findViewById<TextView>(R.id.contactNumberCardView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val viewholder = ContactViewHolder(LayoutInflater.from(context).inflate(R.layout.contact_layout, parent, false))
        return viewholder
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val currentContact = contactList[position]
        holder?.contactNameAD?.text = currentContact.name
        holder?.contactNumberAD?.text = currentContact.c_number
    }

    fun updateContacts(newContacts: List<Contact>)
    {
        contactList.clear()
        contactList.addAll(newContacts)
        notifyDataSetChanged()
    }

    fun deleteContacts(position: Int)
    {
        contactList.removeAt(position)
        notifyDataSetChanged()
    }
    override fun getItemCount(): Int {
        return contactList.size
    }
    fun getContact(position: Int): Contact
    {
        return contactList[position]
    }
}