# 🚀 OAuth Kurulum Rehberi

OAuth entegrasyonu tamamlandı! Artık son adımları takip ederek Google ve GitHub ile giriş yapabilirsiniz.

## 📋 Google OAuth Setup

### 1. Google Cloud Console'a Git
- https://console.cloud.google.com/ adresine git
- Yeni proje oluştur veya mevcut projeyi seç

### 2. OAuth Consent Screen Konfigürasyonu
- **APIs & Services** > **OAuth consent screen**
- **External** seç
- **App name**: `SafeRoom`
- **User support email**: Kendi email'in
- **Developer contact email**: Kendi email'in

### 3. OAuth Credentials Oluştur
- **APIs & Services** > **Credentials**
- **+ CREATE CREDENTIALS** > **OAuth 2.0 Client IDs**
- **Application type**: `Desktop application`
- **Name**: `SafeRoom Desktop`
- **Authorized redirect URIs**: `http://localhost:8080/callback`

### 4. Client ID ve Secret Al
- Client ID ve Client Secret'ı kopyala
- `OAuthManager.java` dosyasında değiştir:
```java
private static final String GOOGLE_CLIENT_ID = "senin-google-client-id.apps.googleusercontent.com";
private static final String GOOGLE_CLIENT_SECRET = "senin-google-client-secret";
```

## 📋 GitHub OAuth Setup

### 1. GitHub Settings'e Git
- GitHub'da **Settings** > **Developer settings** > **OAuth Apps**

### 2. New OAuth App Oluştur
- **Application name**: `SafeRoom`
- **Homepage URL**: `https://github.com/yourusername/SafeRoomV2`
- **Authorization callback URL**: `http://localhost:8080/callback`

### 3. Client ID ve Secret Al
- Client ID ve Client Secret'ı kopyala
- `OAuthManager.java` dosyasında değiştir:
```java
private static final String GITHUB_CLIENT_ID = "senin-github-client-id";
private static final String GITHUB_CLIENT_SECRET = "senin-github-client-secret";
```

## 🎯 Test Etme

1. **SafeRoom'u çalıştır**:
   ```bash
   ./gradlew run
   ```

2. **OAuth butonlarını test et**:
   - Google ile Giriş butonuna tıkla
   - Tarayıcı açılacak ve Google OAuth sayfasına gidecek
   - İzin ver ve geri dön
   - Aynı şekilde GitHub için de test et

3. **Console loglarını kontrol et**:
   - `Starting Google OAuth...` mesajını gör
   - Authorization code alındığını kontrol et
   - User info'nun başarıyla parse edildiğini kontrol et

## 🔧 Sorun Giderme

### Port 8080 Kullanımda Hatası
Eğer port 8080 zaten kullanımda ise:
1. `OAuthManager.java`'da `CALLBACK_PORT`'u değiştir (örn: 8081)
2. Google/GitHub redirect URI'larını da güncelle

### Tarayıcı Açılmıyor
Linux sistemlerde `xdg-open` çalışmıyorsa:
```bash
sudo apt install xdg-utils
```

### CORS Hatası
Callback server'da CORS hatası alırsan, `CallbackHandler`'a şunu ekle:
```java
exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
```

## ✅ Başarılı Kurulum Kontrolü

OAuth başarıyla kurulmuşsa:
- ✅ Google/GitHub butonları aktif çalışıyor
- ✅ Tarayıcı OAuth sayfasını açıyor
- ✅ Callback sayfasında "Authentication Successful" görüyorsun
- ✅ Console'da user email/name bilgileri görünüyor
- ✅ Ana sayfaya yönlendiriliyor

## 🎉 Tebrikler!

OAuth entegrasyonu tamamlandı! Artık kullanıcılar Google ve GitHub hesaplarıyla SafeRoom'a giriş yapabilir.

### Sonraki Adımlar (Opsiyonel):
1. **Database Entegrasyonu**: OAuth kullanıcılarını veritabanında sakla
2. **Profil Fotoğrafı**: OAuth'dan gelen profil fotoğrafını kullan
3. **Auto-Registration**: OAuth kullanıcıları için otomatik hesap oluştur
4. **Token Refresh**: Access token'ları güncelle
