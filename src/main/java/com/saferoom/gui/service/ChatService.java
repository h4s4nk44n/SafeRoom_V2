package com.saferoom.gui.service;


import com.saferoom.gui.model.FileAttachment;
import com.saferoom.gui.model.Message;
import com.saferoom.gui.model.MessageType;
import com.saferoom.gui.model.User;
import com.saferoom.client.ClientMenu;
import com.saferoom.p2p.FileTransferObserver;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.application.Platform;
import javafx.scene.image.Image;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.nio.file.Path;

/**
 * Mesajları yöneten, gönderen ve alan servis.
 * Singleton deseni ile tasarlandı, yani uygulamanın her yerinden tek bir
 * nesnesine erişilebilir.
 */
public class ChatService {

    // Singleton deseni için statik nesne
    private static final ChatService instance = new ChatService();

    // Current user's username (set by ClientMenu during initialization)
    private String currentUsername = null;

    // Veri saklama alanı (eskiden kontrolcüdeydi)
    private final Map<String, ObservableList<Message>> channelMessages = new HashMap<>();

    // DİKKAT: Bu, yeni bir mesaj geldiğinde bunu dinleyenleri haberdar eden sihirli kısımdır.
    private final ObjectProperty<Message> newMessageProperty = new SimpleObjectProperty<>();

    private final Map<Long, Message> activeFileTransfers = new ConcurrentHashMap<>();

    private ChatService() {
        // Başlangıç için sahte verileri yükle
        setupDummyMessages();
    }

    // Servisin tek nesnesine erişim metodu
    public static ChatService getInstance() {
        return instance;
    }

    /**
     * Set the current user's username (called by ClientMenu during initialization)
     * @param username The current user's username
     */
    public void setCurrentUsername(String username) {
        this.currentUsername = username;
        System.out.printf("[ChatService] 👤 Current user set to: %s%n", username);
    }

    /**
     * Get the current user's username
     * @return The current user's username
     */
    public String getCurrentUsername() {
        return currentUsername;
    }

    /**
     * Belirtilen kanala yeni bir mesaj gönderir.
     * P2P bağlantı varsa P2P kullanır, yoksa server relay kullanır.
     * @param channelId Sohbet kanalının ID'si
     * @param text Gönderilecek mesaj metni
     * @param sender Mesajı gönderen kullanıcı
     */
    public void sendMessage(String channelId, String text, User sender) {
        if (text == null || text.trim().isEmpty()) return;

        Message newMessage = new Message(
            text,
            sender.getId(),
            sender.getName().isEmpty() ? "" : sender.getName().substring(0, 1)
        );
        newMessage.setType(MessageType.TEXT);
        newMessage.setOutgoing(true);

        // Mesajı ilgili kanalın listesine ekle
        ObservableList<Message> messages = getMessagesForChannel(channelId);
        messages.add(newMessage);

        // Try WebRTC DataChannel P2P messaging first
        boolean sentViaP2P = false;
        
        // Check if we have active WebRTC DataChannel connection
        com.saferoom.p2p.P2PConnectionManager p2pManager = 
            com.saferoom.p2p.P2PConnectionManager.getInstance();
        
        if (p2pManager.hasActiveConnection(channelId)) {
            try {
                System.out.printf("[Chat] 📡 Sending via WebRTC DataChannel to %s%n", channelId);
                
                java.util.concurrent.CompletableFuture<Boolean> future = 
                    p2pManager.sendMessage(channelId, text);
                
                // Wait for send completion (with timeout)
                sentViaP2P = future.get(2, java.util.concurrent.TimeUnit.SECONDS);
                
                if (sentViaP2P) {
                    System.out.printf("[Chat] ✅ Message sent via WebRTC DataChannel to %s%n", channelId);
                } else {
                    System.out.printf("[Chat] ⚠️ WebRTC DataChannel send failed to %s%n", channelId);
                }
            } catch (Exception e) {
                System.err.printf("[Chat] ❌ WebRTC DataChannel error: %s%n", e.getMessage());
                sentViaP2P = false;
            }
        }
        
        if (!sentViaP2P) {
            System.out.printf("[Chat] 📡 No P2P connection with %s - would use server relay%n", channelId);
            // TODO: Implement server relay messaging
        }

        // Update contact's last message (from me)
        try {
            com.saferoom.gui.service.ContactService.getInstance()
                .updateLastMessage(channelId, text, true);
        } catch (Exception e) {
            System.err.println("[Chat] Error updating contact last message: " + e.getMessage());
        }

        // Yeni mesaj geldiğini tüm dinleyenlere haber ver!
        newMessageProperty.set(newMessage);
    }

    /**
     * Belirtilen kanalın mesaj listesini döndürür.
     * @param channelId Sohbet kanalının ID'si
     * @return O kanala ait ObservableList<Message>
     */
    public ObservableList<Message> getMessagesForChannel(String channelId) {
        return channelMessages.computeIfAbsent(channelId, k -> FXCollections.observableArrayList());
    }

    // Yeni mesaj dinleyicisi için property'e erişim metodu
    public ObjectProperty<Message> newMessageProperty() {
        return newMessageProperty;
    }
    
    /**
     * P2P'den gelen mesajı al ve GUI'de göster
     */
    public void receiveP2PMessage(String sender, String receiver, String messageText) {
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.printf("[Chat] 📥 P2P message received: %s -> %s: \"%s\"%n", sender, receiver, messageText);
        System.out.printf("[Chat] 🔍 Stack trace:%n");
        for (StackTraceElement elem : Thread.currentThread().getStackTrace()) {
            if (elem.getClassName().contains("saferoom")) {
                System.out.printf("    at %s.%s(%s:%d)%n", 
                    elem.getClassName(), elem.getMethodName(), 
                    elem.getFileName(), elem.getLineNumber());
            }
        }
        System.out.println("═══════════════════════════════════════════════════════════════");
        
        Message incomingMessage = new Message(
            messageText,
            sender,
            sender.isEmpty() ? "?" : sender.substring(0, 1).toUpperCase()
        );
        incomingMessage.setType(MessageType.TEXT);
        
        // Mesajı doğru channel'a ekle
        ObservableList<Message> messages = getMessagesForChannel(sender);
        messages.add(incomingMessage);
        
        System.out.printf("[Chat] 📬 Updated contact last message for %s%n", sender);
        System.out.printf("[Chat] ✅ P2P message added to channel: %s%n", sender);
        
        // Update contact's last message (not from me - will increment unread if not active)
        try {
            com.saferoom.gui.service.ContactService contactService = 
                com.saferoom.gui.service.ContactService.getInstance();
            
            // Add contact if doesn't exist
            if (!contactService.hasContact(sender)) {
                contactService.addNewContact(sender);
            }
            
            // Update last message (isFromMe = false)
            contactService.updateLastMessage(sender, messageText, false);
            
            System.out.printf("[Chat] 📬 Updated contact last message for %s%n", sender);
            
        } catch (Exception e) {
            System.err.println("[Chat] Error updating contact for P2P message: " + e.getMessage());
        }
        
        // GUI'yi güncelle
        newMessageProperty.set(incomingMessage);
        
        System.out.printf("[Chat] ✅ P2P message added to channel: %s%n", sender);
    }
    
    /**
     * Dosya transfer işlemi başlat (P2P)
     * @param targetUser Dosya gönderilecek kullanıcı
     * @param filePath Gönderilecek dosyanın yolu
     */
    public void sendFileMessage(String targetUser, java.nio.file.Path filePath, User sender) {
        if (targetUser == null || filePath == null) {
            System.err.println("[Chat] ❌ Invalid sendFile parameters");
            return;
        }
        
        System.out.printf("[Chat] 📁 Starting file transfer: %s -> %s%n", 
            filePath.getFileName(), targetUser);
        
        // Build UI placeholder before sending
        MessageType fileType = detectFileType(filePath);
        Image thumbnail = MessageType.IMAGE.equals(fileType) ? generateThumbnail(filePath) : null;
        FileAttachment attachment = new FileAttachment(
            fileType,
            filePath.getFileName().toString(),
            filePath.toFile().length(),
            filePath,
            thumbnail
        );
        Message placeholder = Message.createFilePlaceholder(
            sender.getId(),
            sender.getName().isEmpty() ? "" : sender.getName().substring(0, 1),
            attachment
        );
        placeholder.setOutgoing(true);

        ObservableList<Message> messages = getMessagesForChannel(targetUser);
        messages.add(placeholder);
        try {
            com.saferoom.gui.service.ContactService.getInstance()
                .updateLastMessage(targetUser, "📎 " + attachment.getFileName(), true);
        } catch (Exception e) {
            System.err.println("[Chat] Error updating contact for file placeholder: " + e.getMessage());
        }

        // Check P2P connection (WebRTC DataChannel)
        if (!ClientMenu.isP2PMessagingAvailable(targetUser)) {
            System.err.printf("[Chat] ❌ No P2P connection with %s%n", targetUser);
            Platform.runLater(() -> {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.ERROR);
                alert.setTitle("P2P Error");
                alert.setHeaderText("No P2P Connection");
                alert.setContentText("Cannot send file - no active P2P connection with " + targetUser);
                alert.showAndWait();
                placeholder.setStatusText("Failed (no connection)");
            });
            return;
        }
        
        try {
            AtomicReference<Long> transferIdRef = new AtomicReference<>(-1L);

            FileTransferObserver observer = new FileTransferObserver() {
                @Override
                public void onTransferStarted(long fileId, Path path, long totalBytes) {
                    transferIdRef.set(fileId);
                    activeFileTransfers.put(fileId, placeholder);
                    Platform.runLater(() -> {
                        placeholder.setTransferId(fileId);
                        placeholder.setStatusText("Sending…");
                        placeholder.setProgress(0);
                    });
                }

                @Override
                public void onTransferProgress(long fileId, long bytesSent, long totalBytes) {
                    double fraction = totalBytes == 0 ? 0 : (double) bytesSent / totalBytes;
                    Platform.runLater(() -> placeholder.setProgress(fraction));
                }

                @Override
                public void onTransferCompleted(long fileId) {
                    activeFileTransfers.remove(fileId);
                    Platform.runLater(() -> {
                        placeholder.setProgress(1.0);
                        placeholder.setType(attachment.getTargetType());
                        placeholder.setStatusText("Sent");
                    });
                }

                @Override
                public void onTransferFailed(long fileId, Throwable error) {
                    activeFileTransfers.remove(fileId);
                    Platform.runLater(() -> {
                        placeholder.setStatusText("Failed");
                    });
                }
            };

            com.saferoom.p2p.P2PConnectionManager.getInstance()
                .sendFile(targetUser, filePath, observer)
                .thenAccept(success -> {
                    if (!success) {
                        observer.onTransferFailed(transferIdRef.get(), null);
                    }
                });
            
        } catch (Exception e) {
            System.err.printf("[Chat] ❌ File transfer error: %s%n", e.getMessage());
            e.printStackTrace();
            
            Platform.runLater(() -> {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.ERROR);
                alert.setTitle("File Transfer Error");
                alert.setHeaderText("Failed to Send File");
                alert.setContentText(e.getMessage());
                alert.showAndWait();
                placeholder.setStatusText("Failed");
            });
        }
    }

    private MessageType detectFileType(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".bmp") || name.endsWith(".gif")) {
            return MessageType.IMAGE;
        }
        if (name.endsWith(".mp4") || name.endsWith(".mov") || name.endsWith(".mkv")) {
            return MessageType.VIDEO;
        }
        if (name.endsWith(".pdf") || name.endsWith(".doc") || name.endsWith(".docx") || name.endsWith(".txt")) {
            return MessageType.DOCUMENT;
        }
        return MessageType.FILE;
    }

    private Image generateThumbnail(Path path) {
        try {
            return new Image(path.toUri().toString(), 120, 120, true, true, false);
        } catch (Exception e) {
            System.err.println("[Chat] Thumbnail generation failed: " + e.getMessage());
            return null;
        }
    }

    // No dummy messages - start with clean slate
    private void setupDummyMessages() {
        // All chat channels start empty - real messages will be added via P2P
        System.out.println("[ChatService] 🧹 Started with clean message history - no dummy messages");
    }
}