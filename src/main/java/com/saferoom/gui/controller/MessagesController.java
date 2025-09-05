package com.saferoom.gui.controller;

import com.saferoom.gui.view.cell.ContactCell;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;

public class MessagesController {

    @FXML private SplitPane mainSplitPane;
    @FXML private ListView<Contact> contactListView;

    @FXML private ChatViewController chatViewController;

    @FXML
    public void initialize() {
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
            }
        });
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