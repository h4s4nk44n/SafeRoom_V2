package com.saferoom.gui.service;

import com.saferoom.gui.controller.MessagesController.Contact;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Persistent contact and chat management service
 * Handles contact storage, last messages, unread counts, etc.
 */
public class ContactService {
    private static ContactService instance;
    
    // Persistent storage for contacts
    private final Map<String, Contact> contacts = new ConcurrentHashMap<>();
    private final ObservableList<Contact> contactList = FXCollections.observableArrayList();
    
    // Track unread messages per contact
    private final Map<String, Integer> unreadCounts = new ConcurrentHashMap<>();
    
    // Track last message per contact
    private final Map<String, String> lastMessages = new ConcurrentHashMap<>();
    private final Map<String, String> lastMessageTimes = new ConcurrentHashMap<>();
    
    // Track active chat (for read/unread logic)
    private String activeChat = null;
    
    private ContactService() {
        initializeDummyContacts();
    }
    
    public static ContactService getInstance() {
        if (instance == null) {
            instance = new ContactService();
        }
        return instance;
    }
    
    /**
     * Initialize with some dummy contacts for demo
     */
    private void initializeDummyContacts() {
        addOrUpdateContact("zeynep_kaya", "Zeynep Kaya", "Online", "Harika, teşekkürler!", "5m", 2, false);
        addOrUpdateContact("ahmet_celik", "Ahmet Çelik", "Offline", "Raporu yarın sabah gönderirim.", "1d", 0, false);
        addOrUpdateContact("sarah_idle", "Sarah Johnson", "Idle", "I'll be back in 10 minutes", "15m", 0, false);
        addOrUpdateContact("mike_busy", "Mike Davis", "Busy", "In a meeting right now", "30m", 1, false);
        addOrUpdateContact("lisa_dnd", "Lisa Chen", "Do Not Disturb", "Working on important project", "1h", 0, false);
        addOrUpdateContact("meeting_phoenix", "Proje Phoenix Grubu", "3 Online", "Toplantı 15:00'te.", "2h", 5, true);
    }
    
    /**
     * Add or update a contact
     */
    public void addOrUpdateContact(String id, String name, String status, String lastMessage, 
                                  String time, int unreadCount, boolean isGroup) {
        Contact contact = new Contact(id, name, status, lastMessage, time, unreadCount, isGroup);
        
        // Update internal storage
        contacts.put(id, contact);
        unreadCounts.put(id, unreadCount);
        lastMessages.put(id, lastMessage);
        lastMessageTimes.put(id, time);
        
        // Update observable list
        updateObservableList();
        
        System.out.printf("[ContactService] 📝 Updated contact: %s (unread: %d)%n", name, unreadCount);
    }
    
    /**
     * Add a new contact (for P2P connections)
     */
    public void addNewContact(String username) {
        if (!contacts.containsKey(username)) {
            String currentTime = getCurrentTimeString();
            addOrUpdateContact(username, username, "Online", "Starting conversation...", currentTime, 0, false);
            System.out.printf("[ContactService] ➕ Added new contact: %s%n", username);
        }
    }
    
    /**
     * Update last message for a contact
     */
    public void updateLastMessage(String contactId, String message, boolean isFromMe) {
        Contact existingContact = contacts.get(contactId);
        if (existingContact != null) {
            String currentTime = getCurrentTimeString();
            int currentUnread = unreadCounts.getOrDefault(contactId, 0);
            
            // If message is not from me and chat is not active, increment unread
            if (!isFromMe && !contactId.equals(activeChat)) {
                currentUnread++;
            }
            
            // Update contact with new last message
            addOrUpdateContact(contactId, existingContact.getName(), existingContact.getStatus(), 
                             message, currentTime, currentUnread, existingContact.isGroup());
            
            System.out.printf("[ContactService] 💬 Updated last message for %s: \"%s\" (unread: %d)%n", 
                contactId, message, currentUnread);
        }
    }
    
    /**
     * Mark all messages as read for a contact
     */
    public void markAsRead(String contactId) {
        Contact existingContact = contacts.get(contactId);
        if (existingContact != null && existingContact.getUnreadCount() > 0) {
            // Update contact with 0 unread count
            addOrUpdateContact(contactId, existingContact.getName(), existingContact.getStatus(), 
                             existingContact.getLastMessage(), existingContact.getTime(), 0, existingContact.isGroup());
            
            System.out.printf("[ContactService] ✅ Marked as read: %s%n", contactId);
        }
    }
    
    /**
     * Set active chat (for read/unread logic)
     */
    public void setActiveChat(String contactId) {
        this.activeChat = contactId;
        if (contactId != null) {
            markAsRead(contactId);
            System.out.printf("[ContactService] 👁️ Set active chat: %s%n", contactId);
        }
    }
    
    /**
     * Clear active chat
     */
    public void clearActiveChat() {
        this.activeChat = null;
        System.out.println("[ContactService] 👁️ Cleared active chat");
    }
    
    /**
     * Get contact by ID
     */
    public Contact getContact(String contactId) {
        return contacts.get(contactId);
    }
    
    /**
     * Get observable contact list for UI
     */
    public ObservableList<Contact> getContactList() {
        return contactList;
    }
    
    /**
     * Update observable list from internal storage
     */
    private void updateObservableList() {
        // Clear and rebuild list (maintaining order by last message time)
        contactList.clear();
        
        // Sort contacts by last message time (most recent first)
        contacts.values().stream()
            .sorted((c1, c2) -> {
                // Simple time comparison (in real app, use proper timestamp)
                String time1 = c1.getTime();
                String time2 = c2.getTime();
                
                // For now, just maintain insertion order
                // TODO: Implement proper timestamp sorting
                return 0;
            })
            .forEach(contactList::add);
    }
    
    /**
     * Get current time as string
     */
    private String getCurrentTimeString() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
    }
    
    /**
     * Get total unread message count
     */
    public int getTotalUnreadCount() {
        return unreadCounts.values().stream().mapToInt(Integer::intValue).sum();
    }
    
    /**
     * Check if contact exists
     */
    public boolean hasContact(String contactId) {
        return contacts.containsKey(contactId);
    }
}
