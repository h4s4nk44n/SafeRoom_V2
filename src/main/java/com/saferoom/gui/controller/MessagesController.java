package com.saferoom.gui.controller;

import com.saferoom.gui.view.cell.ContactCell;
import com.saferoom.p2p.P2PConnectionManager;
import com.saferoom.p2p.P2PConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.application.Platform;
import java.util.concurrent.CompletableFuture;

public class MessagesController {

    @FXML private SplitPane mainSplitPane;
    @FXML private ListView<Contact> contactListView;

    @FXML private ChatViewController chatViewController;
    
    // Singleton instance için
    private static MessagesController instance;
    
    // P2P Manager
    private P2PConnectionManager p2pManager;

    @FXML
    public void initialize() {
        instance = this;
        p2pManager = P2PConnectionManager.getInstance();
        
        mainSplitPane.setDividerPositions(0.30);

        setupModelAndListViews();
        setupContactSelectionListener();

        if (!contactListView.getItems().isEmpty()) {
            contactListView.getSelectionModel().selectFirst();
        }
    }

    private void setupModelAndListViews() {
        ObservableList<Contact> contacts = FXCollections.observableArrayList(
                new Contact("zeynep_kaya", "Zeynep Kaya", "Online", "Harika, teşekkürler!", "5m", 2, false),
                new Contact("ahmet_celik", "Ahmet Çelik", "Offline", "Raporu yarın sabah gönderirim.", "1d", 0, false),
                new Contact("sarah_idle", "Sarah Johnson", "Idle", "I'll be back in 10 minutes", "15m", 0, false),
                new Contact("mike_busy", "Mike Davis", "Busy", "In a meeting right now", "30m", 1, false),
                new Contact("lisa_dnd", "Lisa Chen", "Do Not Disturb", "Working on important project", "1h", 0, false),
                new Contact("meeting_phoenix", "Proje Phoenix Grubu", "3 Online", "Toplantı 15:00'te.", "2h", 5, true)
        );
        contactListView.setItems(contacts);
        contactListView.setCellFactory(param -> new ContactCell());
    }

    private void setupContactSelectionListener() {
        contactListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null && chatViewController != null) {
                chatViewController.initChannel(newSelection.getId());
                chatViewController.setHeader(
                        newSelection.getName(),
                        newSelection.getStatus(),
                        newSelection.getAvatarChar(),
                        newSelection.isGroup()
                );
                
                // P2P connection status bildirilim
                checkP2PConnectionStatus(newSelection.getId());
            }
        });
    }
    
    /**
     * External controllers'dan çağrılır - belirli kullanıcıyla sohbet başlat
     */
    public static void openChatWithUser(String username) {
        if (instance != null) {
            Platform.runLater(() -> {
                instance.selectOrAddUser(username);
            });
        }
    }
    
    /**
     * Kullanıcıyı contact listesinde seç veya ekle
     */
    private void selectOrAddUser(String username) {
        // Önce mevcut kontaklarda ara
        for (Contact contact : contactListView.getItems()) {
            if (contact.getId().equals(username)) {
                contactListView.getSelectionModel().select(contact);
                System.out.println("📱 Selected existing contact: " + username);
                return;
            }
        }
        
        // Bulunamazsa yeni contact ekle
        Contact newContact = new Contact(username, username, "Online", "P2P bağlantı kuruluyor...", "now", 0, false);
        contactListView.getItems().add(0, newContact); // En üste ekle
        contactListView.getSelectionModel().select(newContact);
        System.out.println("📱 Added new contact: " + username);
        
        // P2P bağlantı durumunu güncelle
        updateContactStatus(username, "P2P bağlantı kuruluyor...");
    }
    
    /**
     * P2P bağlantı durumunu kontrol et ve bildir
     */
    private void checkP2PConnectionStatus(String username) {
        if (p2pManager.hasActiveConnection(username)) {
            System.out.println("✅ Active P2P connection with: " + username);
            updateContactStatus(username, "P2P Aktif");
        } else if (p2pManager.hasPendingConnection(username)) {
            System.out.println("⏳ Pending P2P connection with: " + username);
            updateContactStatus(username, "P2P bağlanıyor...");
        } else {
            System.out.println("📡 No P2P connection with: " + username);
            updateContactStatus(username, "Server üzerinden");
        }
    }
    
    /**
     * Contact status güncelle
     */
    private void updateContactStatus(String username, String newStatus) {
        for (int i = 0; i < contactListView.getItems().size(); i++) {
            Contact contact = contactListView.getItems().get(i);
            if (contact.getId().equals(username)) {
                Contact updatedContact = new Contact(
                    contact.getId(),
                    contact.getName(),
                    newStatus,
                    contact.getLastMessage(),
                    contact.getTime(),
                    contact.getUnreadCount(),
                    contact.isGroup()
                );
                contactListView.getItems().set(i, updatedContact);
                contactListView.refresh();
                break;
            }
        }
    }

    // Geçici olarak Contact modelini burada tutuyoruz. İdealde bu da model paketinde olmalı.
    public static class Contact {
        private final String id, name, status, lastMessage, time;
        private final int unreadCount;
        private final boolean isGroup;
        public Contact(String id, String name, String status, String lastMessage, String time, int unreadCount, boolean isGroup) {
            this.id = id; this.name = name; this.status = status; this.lastMessage = lastMessage; this.time = time; this.unreadCount = unreadCount; this.isGroup = isGroup;
        }
        public String getId() { return id; }
        public String getName() { return name; }
        public String getStatus() { return status; }
        public String getLastMessage() { return lastMessage; }
        public String getTime() { return time; }
        public int getUnreadCount() { return unreadCount; }
        public boolean isGroup() { return isGroup; }
        public String getAvatarChar() { return name.isEmpty() ? "" : name.substring(0, 1); }
        public boolean isOnline() { return status.equalsIgnoreCase("online"); }
    }
}