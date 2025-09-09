# P2P Messaging System - SafeRoom V2

Bu döküman SafeRoom V2'ye entegre edilen P2P (Peer-to-Peer) mesajlaşma sistemini açıklar.

## 📋 Genel Bakış

P2P sistemi, arkadaş olmuş kullanıcılar arasında NAT hole punching kullanarak direkt bağlantı kurar. Bu sayede mesajlar merkezi sunucu yerine direkt kullanıcılar arasında gönderilir.

## 🚀 Ana Özellikler

- **NAT Hole Punching**: Firewall ve NAT'ların arkasındaki kullanıcılar arası direkt bağlantı
- **Automatic Fallback**: P2P başarısız olursa sunucu üzerinden mesajlaşma
- **Keep-Alive Mechanism**: Bağlantıların sürekli aktif tutulması
- **Connection Management**: Otomatik bağlantı yönetimi ve temizleme
- **Thread-Safe**: Concurrent bağlantıları güvenli şekilde yönetme

## 🏗️ Sistem Mimarisi

### Ana Bileşenler

#### 1. P2PConnectionManager
- **Singleton pattern** ile tek instance
- Aktif bağlantıları yönetir
- Pending bağlantıları takip eder
- Otomatik cleanup ve connection pooling

#### 2. P2PConnection
- Tek bir kullanıcıyla bağlantıyı temsil eder
- DatagramChannel wrapper'ı
- Message queue ile asenkron mesaj işleme
- Heartbeat mekanizması

#### 3. P2PHolePuncher
- NAT hole punching implementasyonu
- NatAnalyzer'dan adapt edilmiş
- STUN server'ları kullanarak public IP/port discovery
- KeepAliveManager entegrasyonu

## 📡 NAT Hole Punching Süreci

### 1. STUN Discovery
```java
byte sig = analyzeNAT();
int holeCount = Math.max(NatAnalyzer.Public_PortList.size(), MIN_CHANNELS);
```

### 2. Channel Setup
```java
for (int i = 0; i < holeCount; i++) {
    DatagramChannel dc = DatagramChannel.open();
    dc.configureBlocking(false);
    dc.bind(new InetSocketAddress(0));
    channels.add(dc);
}
```

### 3. Server Coordination
```java
// HELLO paketleri gönder
dc.send(hello.duplicate(), serverAddr);
// FIN paketi gönder
channels.get(0).send(LLS.New_Fin_Packet(...), serverAddr);
```

### 4. Port Exchange
```java
if (type == LLS.SIG_PORT) {
    List<Object> info = LLS.parsePortInfo(buf.duplicate());
    InetSocketAddress peerAddr = new InetSocketAddress(...);
    KAM.register(chosen, peerAddr);
}
```

## 🔌 Integration Points

### FriendsController
```java
private static void openMessagesWithUser(String username) {
    startP2PConnection(username);
    // Messages tab'ine geç
    mainController.handleMessages();
}
```

### ProfileController
```java
private void handleMessage() {
    if ("friends".equals(friendStatus.toLowerCase())) {
        startP2PConnection(targetUsername);
    }
    // Message interface'i aç
}
```

## 📋 Protocol Details

### LLS (Local Link Signaling) Paketi
- **SIG_HELLO** (0x10): İlk bağlantı isteği
- **SIG_FIN** (0x11): Port bilgisi gönderimi tamamlandı
- **SIG_PORT** (0x12): Karşı tarafın port bilgisi
- **SIG_ALL_DONE** (0x13): Hole punching tamamlandı
- **SIG_KEEP** (0x1E): Keep-alive paketi

### Packet Structure
```
[Type:1][Length:2][Username:20][Target:20][IP:4][Port:4]
```

## 🔄 Connection Lifecycle

### 1. Establishment
```java
CompletableFuture<P2PConnection> future = 
    P2PConnectionManager.getInstance().connectToUser(username);
```

### 2. Messaging
```java
P2PConnection conn = manager.getConnection(username);
conn.sendMessage(messageBytes);
byte[] received = conn.receiveMessage();
```

### 3. Cleanup
```java
conn.close(); // KeepAliveManager da otomatik kapanır
manager.closeConnection(username);
```

## ⚙️ Configuration

### Timeouts
- **MATCH_TIMEOUT_MS**: 20,000ms (Hole punching timeout)
- **HEARTBEAT_TIMEOUT**: 30,000ms (Connection timeout)
- **RESEND_INTERVAL_MS**: 1,000ms (Packet resend interval)

### Channels
- **MIN_CHANNELS**: 4 (Minimum DatagramChannel sayısı)
- **KEEPALIVE_INTERVAL**: 2,000ms (Keep-alive gönderim sıklığı)

## 🛠️ Usage Examples

### Basic P2P Connection
```java
// Manager'ı al
P2PConnectionManager manager = P2PConnectionManager.getInstance();

// Async bağlantı kur
manager.connectToUser("targetUser").thenAccept(connection -> {
    if (connection != null) {
        System.out.println("✅ P2P ready!");
        // Mesaj gönder
        connection.sendMessage("Hello P2P!".getBytes());
    }
});
```

### Manual Connection Management
```java
// Direkt bağlantı kur
InetSocketAddress serverAddr = new InetSocketAddress("server.ip", 12345);
P2PConnection conn = P2PHolePuncher.establishConnection("target", serverAddr);

if (conn != null) {
    conn.sendMessage(data);
    byte[] response = conn.receiveMessage();
    conn.close();
}
```

## 🐛 Troubleshooting

### Common Issues

#### 1. Symmetric NAT
- P2P bağlantı başarısız olur
- Sistem otomatik olarak server relay'e düşer
- Console'da "❌ Failed to establish P2P connection" mesajı

#### 2. Firewall Blocking
- STUN discovery başarısız
- Keep-alive paketleri blocked
- Timeout errors

#### 3. Server Coordination Issues
- ALL_DONE paketi alınamıyor
- Port exchange incomplete
- Connection pending state'de takılı kalıyor

### Debug Logging
```java
System.setProperty("java.util.logging.config.file", "logging.properties");
// P2P log mesajları için console output'u takip edin:
// 🚀 Starting P2P connection
// 📡 Remote address learned
// ✅ P2P connection established
// ❌ P2P connection failed
```

## 🔮 Future Enhancements

### 1. Message Encryption
- End-to-end encryption for P2P messages
- Key exchange during hole punching

### 2. File Transfer
- P2P file sharing between friends
- Progress tracking and resume capability

### 3. Voice/Video Calls
- RTP over P2P connections
- WebRTC integration

### 4. Group P2P
- Multi-user P2P mesh networks
- Relay node selection

## 📚 Dependencies

- **NatAnalyzer**: STUN server communication
- **KeepAliveManager**: Connection maintenance
- **LLS Protocol**: Packet format and parsing
- **JavaFX Platform**: UI thread integration
- **CompletableFuture**: Async operations

---

## 💡 Implementation Notes

Bu P2P sistemi, mevcut NAT hole punching kodlarından (NatAnalyzer, KeepAliveManager, LLS) faydalanarak oluşturulmuştur. Sistem tamamen thread-safe'dir ve production kullanımına hazırdır.

Arkadaş olmuş kullanıcılar arasında mesaj butonu tıklandığında otomatik olarak P2P bağlantı kurulmaya başlar. Başarısız olursa sistem graceful bir şekilde server-based messaging'e geri döner.
