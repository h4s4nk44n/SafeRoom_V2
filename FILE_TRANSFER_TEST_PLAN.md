# 🧪 File Transfer Handshake Test Plan

## Test Hazırlığı

### 1. İki Client Başlat
```bash
# Terminal 1: Alice (sender)
./gradlew run

# Terminal 2: Bob (receiver)
./gradlew run
```

### 2. P2P Connection Kur
1. Alice ve Bob'u farklı kullanıcılar olarak login yap
2. İkisi de birbirini arkadaş olarak eklesin
3. Her iki client'ın da P2P connection'ı kurduğundan emin ol
4. DM chat açık olsun

---

## Test Case 1: Küçük Dosya (1-10 KB)

### Beklenen Log Sırası

```text
[Alice Tarafı]
[FT-SENDER] ═══════════════════════════════════════════════════
[FT-SENDER] sendFile() called: fileId=1732396800000, file=test.txt, size=5120 bytes
[FT-SENDER] ═══════════════════════════════════════════════════
[FT-CTRL-SEND] Sending: __FT_CTRL__|UR_RECEIVER|1732396800000|5120|dGVzdC50eHQ=
[FT-CTRL-SEND] Channel state: OPEN, Messaging ready: YES
[FT-CTRL-SEND] Control message dispatched successfully

[Bob Tarafı]
[MSG-CH-RECV] Received signal 0x20 (XX bytes) from alice
[FT-CTRL-RECV] Control message received: __FT_CTRL__|UR_RECEIVER|...
[FT-CTRL-RECV] Control type: UR_RECEIVER
[FT-CTRL-RECV] ╔════════════════════════════════════════════════
[FT-CTRL-RECV] ║ UR_RECEIVER: fileId=1732396800000, fileName=test.txt
[FT-CTRL-RECV] ╚════════════════════════════════════════════════
[FT-CTRL-RECV] 🚀 Sending OK_SNDFILE to unblock sender...
[FT-CTRL-SEND] Sending: __FT_CTRL__|OK_SNDFILE|1732396800000
[FT-CTRL-SEND] Control message dispatched successfully
[FT-CTRL-RECV] Starting receiver (will block in handshake)...
[FT-RECV] ╔════════════════════════════════════════════════
[FT-RECV] ║ startPreparedReceiver() called
[FT-RECV] ║ fileId: 1732396800000
[FT-RECV] ║ target: downloads/1732396800000_test.txt
[FT-RECV] ╚════════════════════════════════════════════════
[FT-RECV] ═══════════════════════════════════════════════════
[FT-RECV] startReceiver() called at 1732396800000
[FT-RECV] Thread: FileTransfer-bob
[FT-RECV] Download path: downloads/1732396800000_test.txt
[FT-RECV] ═══════════════════════════════════════════════════
[FT-RECV] Receiver thread started, calling ReceiveData()...
[RECEIVER-HANDSHAKE] ╔════════════════════════════════════════════════
[RECEIVER-HANDSHAKE] ║ handshake() ENTERED
[RECEIVER-HANDSHAKE] ║ Thread: FileTransfer-bob
[RECEIVER-HANDSHAKE] ║ Channel connected: true
[RECEIVER-HANDSHAKE] ║ Polling for SYN packet...
[RECEIVER-HANDSHAKE] ╚════════════════════════════════════════════════

[Alice Tarafı - Devam]
[MSG-CH-RECV] Received signal 0x20 (XX bytes) from bob
[FT-CTRL-RECV] Control message received: __FT_CTRL__|OK_SNDFILE|1732396800000
[FT-CTRL-RECV] Control type: OK_SNDFILE
[FT-CTRL-RECV] ╔════════════════════════════════════════════════
[FT-CTRL-RECV] ║ OK_SNDFILE received: fileId=1732396800000
[FT-CTRL-RECV] ║ Unblocking sender readyFuture...
[FT-CTRL-RECV] ╚════════════════════════════════════════════════
[SENDER-HANDSHAKE] ╔════════════════════════════════════════════════
[SENDER-HANDSHAKE] ║ handshake() ENTERED
[SENDER-HANDSHAKE] ║ Thread: ForkJoinPool.commonPool-worker-X
[SENDER-HANDSHAKE] ║ fileId=1732396800000, size=5120, chunks=4
[SENDER-HANDSHAKE] ║ Channel connected: true
[SENDER-HANDSHAKE] ╚════════════════════════════════════════════════
[FILE-HANDSHAKE] 🤝 Sending SYN for fileId=1732396800000, size=5120, chunks=4
[Wrapper] 📤 Sending signal 0x01 (21 bytes) to bob

[Bob Tarafı - Devam]
[Wrapper] 📥 PRIORITY signal 0x01 (21 bytes) from alice → FRONT of queue
[Wrapper] 📖 Reading signal 0x01 (21 bytes) from queue (remaining: 0)
[RECEIVER-HANDSHAKE] Packet received from: datachannel://alice (size: 21 bytes)
[RECEIVER-HANDSHAKE] SYN received: fileId=1732396800000, size=5120, chunks=4
[RECEIVER-HANDSHAKE] Sending ACK for fileId=1732396800000
[Wrapper] 📤 Sending signal 0x10 (21 bytes) to alice

[Alice Tarafı - Devam]
[Wrapper] 📥 PRIORITY signal 0x10 (21 bytes) from bob → FRONT of queue
[Wrapper] 📖 Reading signal 0x10 (21 bytes) from queue (remaining: 0)
[SENDER-HANDSHAKE] ✅ ACK received: fileId=1732396800000 (after 0 SYN retries)
[SENDER-HANDSHAKE] ✅ SYN_ACK sent successfully
[Wrapper] 📤 Sending signal 0x11 (9 bytes) to bob

[Bob Tarafı - Devam]
[Wrapper] 📥 PRIORITY signal 0x11 (9 bytes) from alice → FRONT of queue
[RECEIVER] Handshake complete, starting data transfer...
[RECEIVER] Transfer complete: test.txt (5120 bytes in 0.5s = 10.2 Mbps)
```

---

## Başarı Kriterleri (Checklist)

- [ ] **Sender sends UR_RECEIVER control message**
  - Log: `[FT-CTRL-SEND] Sending: __FT_CTRL__|UR_RECEIVER|...`

- [ ] **Receiver receives UR_RECEIVER**
  - Log: `[FT-CTRL-RECV] Control type: UR_RECEIVER`

- [ ] **Receiver sends OK_SNDFILE IMMEDIATELY (BEFORE handshake blocks)**
  - Log: `[FT-CTRL-SEND] Sending: __FT_CTRL__|OK_SNDFILE|...`
  - ⚠️ Bu log, receiver handshake'e girmeden ÖNCE görünmeli!

- [ ] **Receiver starts background thread**
  - Log: `[FT-RECV] Receiver thread started, calling ReceiveData()...`

- [ ] **Receiver enters handshake loop**
  - Log: `[RECEIVER-HANDSHAKE] ║ handshake() ENTERED`

- [ ] **Sender receives OK_SNDFILE**
  - Log: `[FT-CTRL-RECV] ║ OK_SNDFILE received`

- [ ] **Sender unblocks and starts handshake**
  - Log: `[SENDER-HANDSHAKE] ║ handshake() ENTERED`

- [ ] **Sender sends SYN**
  - Log: `[FILE-HANDSHAKE] 🤝 Sending SYN`
  - Log: `[Wrapper] 📤 Sending signal 0x01`

- [ ] **Receiver receives SYN (from queue)**
  - Log: `[Wrapper] 📥 PRIORITY signal 0x01`
  - Log: `[RECEIVER-HANDSHAKE] Packet received from: datachannel://...`

- [ ] **Receiver sends ACK**
  - Log: `[RECEIVER-HANDSHAKE] Sending ACK`
  - Log: `[Wrapper] 📤 Sending signal 0x10`

- [ ] **Sender receives ACK**
  - Log: `[SENDER-HANDSHAKE] ✅ ACK received`

- [ ] **Sender sends SYN_ACK**
  - Log: `[SENDER-HANDSHAKE] ✅ SYN_ACK sent successfully`
  - Log: `[Wrapper] 📤 Sending signal 0x11`

- [ ] **Receiver receives SYN_ACK**
  - Log: `[Wrapper] 📥 PRIORITY signal 0x11`

- [ ] **Data transfer begins**
  - Log: `[RECEIVER] Transfer complete: ...`

---

## Failure Senaryoları

### ❌ Senaryo 1: Deadlock hala var (eski davranış)

**Belirtiler:**
```text
[FT-CTRL-RECV] Control type: UR_RECEIVER
[FT-RECV] Receiver thread started, calling ReceiveData()...
[RECEIVER-HANDSHAKE] ║ handshake() ENTERED
[RECEIVER-HANDSHAKE] Still waiting for SYN... (attempt 100)
[RECEIVER-HANDSHAKE] Still waiting for SYN... (attempt 200)
...
[RECEIVER-HANDSHAKE] Still waiting for SYN... (attempt 1000)
...
Handshake timeout after 30 seconds
```

**Neden:** OK_SNDFILE hiç gönderilmedi, sender hala bekliyor.

**Çözüm:** `sendControlMessage(buildOkControl(fileId))` satırının `startPreparedReceiver()` **ÖNCESİNDE** olduğundan emin ol.

---

### ❌ Senaryo 2: OK_SNDFILE gönderildi ama sender almadı

**Belirtiler:**
```text
[Bob]
[FT-CTRL-SEND] Sending: __FT_CTRL__|OK_SNDFILE|...
[FT-CTRL-SEND] Control message dispatched successfully

[Alice]
(NO LOG - nothing happens)

(30 saniye sonra timeout)
```

**Neden:** Reliable messaging katmanında sorun var (LLS protocol, ACK/NACK kayıp).

**Çözüm:** Reliable messaging debugging gerekli (başka bir issue).

---

### ❌ Senaryo 3: SYN gönderildi ama receiver almadı

**Belirtiler:**
```text
[Alice]
[SENDER-HANDSHAKE] ║ handshake() ENTERED
[FILE-HANDSHAKE] 🤝 Sending SYN
[Wrapper] 📤 Sending signal 0x01 (21 bytes) to bob

[Bob]
[RECEIVER-HANDSHAKE] Still waiting for SYN... (attempt 100)
[RECEIVER-HANDSHAKE] Still waiting for SYN... (attempt 200)
...
```

**Neden:** DataChannelWrapper queue'ya mesaj ulaşmıyor (channel routing sorunu).

**Çözüm:** `attachFileChannel()` ve `fileTransfer.handleIncomingMessage()` pipeline'ını kontrol et.

---

## Debug Komutları

### Log filtering (terminal'de)
```bash
# Sadece handshake log'ları
grep -E "(HANDSHAKE|FT-CTRL|FT-RECV|FT-SENDER)" saferoom.log

# Sadece wrapper queue işlemleri
grep "Wrapper" saferoom.log

# Timestamp ile ordering
grep -E "(HANDSHAKE|FT-CTRL)" saferoom.log | awk '{print $1, $2, $3, $4, $5}'
```

### Dosya gönderme kısa yol
```java
// Test için GUI yerine doğrudan API call
P2PConnectionManager.getInstance()
    .sendFile("bob", Paths.get("test.txt"))
    .thenAccept(success -> System.out.println("Transfer: " + success));
```

---

## Test Adımları

### Adım 1: Ortam hazırlığı
1. İki terminal aç
2. Her ikisinde de SafeRoom çalıştır
3. Farklı kullanıcılar ile login ol
4. Birbirlerini arkadaş ekleyin
5. DM aç
6. Log dosyalarını temizle (opsiyonel): `> logs/saferoom.log`

### Adım 2: Küçük dosya oluştur
```bash
echo "Hello SafeRoom!" > test.txt
```

### Adım 3: Gönder
1. Alice'den Bob'a `test.txt` gönder
2. Terminal log'larını izle
3. Yukarıdaki checklist'i takip et

### Adım 4: Sonuç değerlendir
- ✅ **BAŞARILI**: Dosya Bob'un `downloads/` klasörüne ulaştı
- ❌ **BAŞARISIZ**: 30s timeout veya handshake takıldı

---

## İleri Seviye Test (Sonraki Sprint)

- [ ] Büyük dosya (100+ MB)
- [ ] Eş zamanlı birden fazla dosya
- [ ] Ağ kesintisi simülasyonu
- [ ] Receiver offline iken dosya gönderme
- [ ] Pause/Resume mekanizması

---

**Test Date**: 2024-11-23  
**Branch**: `fix/file-transfer-handshake`  
**Expected Result**: Handshake deadlock çözülmüş olmalı, dosyalar başarıyla transfer edilmeli
