package com.saferoom.gui.service;


import com.saferoom.gui.model.Message;
import com.saferoom.gui.model.User;
import com.saferoom.client.ClientMenu;
import com.saferoom.gui.utils.UserSession;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.HashMap;
import java.util.Map;

/**
 * Mesajları yöneten, gönderen ve alan servis.
 * Singleton deseni ile tasarlandı, yani uygulamanın her yerinden tek bir
 * nesnesine erişilebilir.
 */
public class ChatService {

    // Singleton deseni için statik nesne
    private static final ChatService instance = new ChatService();

    // Veri saklama alanı (eskiden kontrolcüdeydi)
    private final Map<String, ObservableList<Message>> channelMessages = new HashMap<>();

    // DİKKAT: Bu, yeni bir mesaj geldiğinde bunu dinleyenleri haberdar eden sihirli kısımdır.
    private final ObjectProperty<Message> newMessageProperty = new SimpleObjectProperty<>();

    // Constructor'ı private yaparak dışarıdan yeni nesne oluşturulmasını engelliyoruz.
    private ChatService() {
        // Başlangıç için sahte verileri yükle
        setupDummyMessages();
    }

    // Servisin tek nesnesine erişim metodu
    public static ChatService getInstance() {
        return instance;
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

        // Mesajı ilgili kanalın listesine ekle
        ObservableList<Message> messages = getMessagesForChannel(channelId);
        messages.add(newMessage);

        // Try P2P messaging first (check if specific peer connection exists)
        String currentUsername = UserSession.getInstance().getDisplayName();
        boolean sentViaP2P = false;
        
        // Check if we have active P2P connection with this specific user
        if (ClientMenu.isP2PMessagingAvailable(channelId)) {
            sentViaP2P = ClientMenu.sendP2PMessage(currentUsername, channelId, text);
        }
        
        if (sentViaP2P) {
            System.out.println("[Chat] ✅ Message sent via P2P to " + channelId);
        } else {
            System.out.printf("[Chat] 📡 No P2P connection with %s - would use server relay%n", channelId);
            // TODO: Implement server relay messaging
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
        System.out.printf("[Chat] 📥 P2P message received: %s -> %s: \"%s\"%n", sender, receiver, messageText);
        
        Message incomingMessage = new Message(
            messageText,
            sender,
            sender.isEmpty() ? "?" : sender.substring(0, 1).toUpperCase()
        );
        
        // Mesajı doğru channel'a ekle
        ObservableList<Message> messages = getMessagesForChannel(sender);
        messages.add(incomingMessage);
        
        // GUI'yi güncelle
        newMessageProperty.set(incomingMessage);
        
        System.out.printf("[Chat] ✅ P2P message added to channel: %s%n", sender);
    }

    // Sahte verileri oluşturan özel metot
    private void setupDummyMessages() {
        channelMessages.put("zeynep_kaya", FXCollections.observableArrayList(
                new Message("Selam, projenin son durumu hakkında bilgi alabilir miyim?", "zeynep1", "Z"),
                new Message("Tabii, raporu hazırlıyorum. Yarın sabah sende olur.", "currentUser123", "Y"),
                new Message("Harika, teşekkürler! Kolay gelsin.", "zeynep1", "Z")
        ));
        channelMessages.put("ahmet_celik", FXCollections.observableArrayList(
                new Message("Raporu yarın sabah gönderirim.", "ahmet1", "A")
        ));
        channelMessages.put("meeting_phoenix", FXCollections.observableArrayList(
                new Message("Toplantı 15:00'te başlıyor arkadaşlar.", "zeynep1", "Z"),
                new Message("Ben hazır ve beklemedeyim.", "ahmet1", "A")
        ));
    }
}