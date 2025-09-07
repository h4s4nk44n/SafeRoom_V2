package com.saferoom.oauth;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import javafx.application.Platform;

import java.awt.Desktop;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class OAuthManager {
    
    // OAuth Configuration - loaded from properties file
    private static String GOOGLE_CLIENT_ID;
    private static String GOOGLE_CLIENT_SECRET;
    private static String GITHUB_CLIENT_ID;
    private static String GITHUB_CLIENT_SECRET;
    private static String REDIRECT_URI;
    private static int CALLBACK_PORT;
    
    private static HttpServer callbackServer;
    private static CompletableFuture<String> authCodeFuture;
    
    // Static block to load configuration
    static {
        loadOAuthConfiguration();
    }
    
    /**
     * Load OAuth configuration from properties file
     */
    private static void loadOAuthConfiguration() {
        try {
            Properties props = new Properties();
            props.load(OAuthManager.class.getResourceAsStream("/oauthconfig.properties"));
            
            GOOGLE_CLIENT_ID = props.getProperty("google.client.id");
            GOOGLE_CLIENT_SECRET = props.getProperty("google.client.secret");
            GITHUB_CLIENT_ID = props.getProperty("github.client.id");
            GITHUB_CLIENT_SECRET = props.getProperty("github.client.secret");
            REDIRECT_URI = props.getProperty("oauth.redirect.uri");
            CALLBACK_PORT = Integer.parseInt(props.getProperty("oauth.callback.port"));
            
            System.out.println("✅ OAuth configuration loaded successfully");
            
        } catch (Exception e) {
            System.err.println("❌ Failed to load OAuth configuration: " + e.getMessage());
            // Fallback values
            REDIRECT_URI = "http://localhost:8080/callback";
            CALLBACK_PORT = 8080;
        }
    }

    /**
     * Google OAuth Authentication
     */
    public static void authenticateWithGoogle(Consumer<UserInfo> callback) {
        try {
            // Start callback server
            startCallbackServer();
            
            // Wait for server to be ready (reduced from 1000ms to 300ms)
            Thread.sleep(300); // Faster startup
            
            // Build Google OAuth URL with proper encoding
            String redirectUri = URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8);
            String scope = URLEncoder.encode("email profile", StandardCharsets.UTF_8);
            
            String googleAuthUrl = "https://accounts.google.com/oauth/authorize?" +
                "client_id=" + GOOGLE_CLIENT_ID + "&" +
                "redirect_uri=" + redirectUri + "&" +
                "scope=" + scope + "&" +
                "response_type=code&" +
                "access_type=offline";
            
            System.out.println("Opening Google OAuth URL: " + googleAuthUrl);
            
            // Open browser
            openBrowser(googleAuthUrl);
            
            // Wait for callback
            authCodeFuture.thenAccept(authCode -> {
                stopCallbackServer();
                
                if (authCode != null) {
                    // Exchange code for token
                    exchangeGoogleCodeForUserInfo(authCode, callback);
                } else {
                    Platform.runLater(() -> callback.accept(null));
                }
            });
            
        } catch (Exception e) {
            System.err.println("Google OAuth error: " + e.getMessage());
            Platform.runLater(() -> callback.accept(null));
        }
    }

    /**
     * GitHub OAuth Authentication
     */
    public static void authenticateWithGitHub(Consumer<UserInfo> callback) {
        try {
            // Start callback server
            startCallbackServer();
            
            // Wait for server to be ready (reduced from 1000ms to 300ms)
            Thread.sleep(300); // Faster startup
            
            // Build GitHub OAuth URL with proper encoding
            String redirectUri = URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8);
            String scope = URLEncoder.encode("user:email", StandardCharsets.UTF_8);
            
            String githubAuthUrl = "https://github.com/login/oauth/authorize?" +
                "client_id=" + GITHUB_CLIENT_ID + "&" +
                "redirect_uri=" + redirectUri + "&" +
                "scope=" + scope;
            
            System.out.println("Opening GitHub OAuth URL: " + githubAuthUrl);
            
            // Open browser
            openBrowser(githubAuthUrl);
            
            // Wait for callback
            authCodeFuture.thenAccept(authCode -> {
                stopCallbackServer();
                
                if (authCode != null) {
                    // Exchange code for token
                    exchangeGitHubCodeForUserInfo(authCode, callback);
                } else {
                    Platform.runLater(() -> callback.accept(null));
                }
            });
            
        } catch (Exception e) {
            System.err.println("GitHub OAuth error: " + e.getMessage());
            Platform.runLater(() -> callback.accept(null));
        }
    }

    /**
     * Start localhost callback server
     */
    private static void startCallbackServer() throws IOException {
        try {
            authCodeFuture = new CompletableFuture<>();
            
            // Stop any existing server first
            stopCallbackServer();
            
            // Create server - use localhost (127.0.0.1) for binding but localhost URL for display
            InetSocketAddress address = new InetSocketAddress("127.0.0.1", CALLBACK_PORT);
            callbackServer = HttpServer.create(address, 0);
            callbackServer.createContext("/callback", new CallbackHandler());
            
            // Use a more stable thread pool
            ExecutorService executor = Executors.newFixedThreadPool(4);
            callbackServer.setExecutor(executor);
            callbackServer.start();
            
            System.out.println("✅ OAuth callback server STARTED on http://localhost:" + CALLBACK_PORT + "/callback");
            System.out.println("✅ Server will stay alive until callback received or 5 minutes timeout");
            
            // Quick server verification (reduced from 500ms to 100ms)
            Thread.sleep(100); // Minimal delay for port binding
            
            // Server timeout - 5 minutes
            CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(300000); // 5 minutes
                    if (!authCodeFuture.isDone()) {
                        System.out.println("⏰ OAuth server timeout - stopping server");
                        stopCallbackServer();
                        authCodeFuture.complete(null);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, executor);
            
        } catch (Exception e) {
            System.err.println("❌ Failed to start callback server: " + e.getMessage());
            e.printStackTrace();
            throw new IOException("Cannot start OAuth callback server", e);
        }
    }

    /**
     * Stop callback server (optimized for speed)
     */
    private static void stopCallbackServer() {
        try {
            if (callbackServer != null) {
                // Fast shutdown without extra delays
                callbackServer.stop(1); // Reduced from 3 to 1 second
                callbackServer = null;
                System.out.println("✅ OAuth callback server stopped");
            }
        } catch (Exception e) {
            System.err.println("⚠️ Error stopping callback server: " + e.getMessage());
            if (callbackServer != null) {
                try {
                    callbackServer.stop(0); // Immediate force stop
                    callbackServer = null;
                } catch (Exception ex) {
                    // Silent fail for speed
                }
            }
        }
    }

    /**
     * Open system browser (optimized for speed)
     */
    private static void openBrowser(String url) {
        // Immediate browser opening without extra logging for speed
        CompletableFuture.runAsync(() -> {
            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(URI.create(url));
                } else {
                    // Fast fallback for Linux systems
                    new ProcessBuilder("xdg-open", url).start();
                }
            } catch (Exception e) {
                System.err.println("Failed to open browser: " + e.getMessage());
                System.out.println("Please manually open: " + url);
            }
        });
    }

    /**
     * Exchange Google auth code for user info
     */
    private static void exchangeGoogleCodeForUserInfo(String authCode, Consumer<UserInfo> callback) {
        try {
            // Step 1: Exchange auth code for access token
            String tokenRequestBody = "code=" + authCode +
                "&client_id=" + GOOGLE_CLIENT_ID +
                "&client_secret=" + GOOGLE_CLIENT_SECRET +
                "&redirect_uri=" + REDIRECT_URI +
                "&grant_type=authorization_code";

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest tokenRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://oauth2.googleapis.com/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(tokenRequestBody))
                .build();

            client.sendAsync(tokenRequest, HttpResponse.BodyHandlers.ofString())
                .thenCompose(tokenResponse -> {
                    if (tokenResponse.statusCode() == 200) {
                        // Parse access token from JSON response
                        String responseBody = tokenResponse.body();
                        String accessToken = parseJsonValue(responseBody, "access_token");
                        
                        if (accessToken != null) {
                            // Step 2: Get user info using access token
                            HttpRequest userRequest = HttpRequest.newBuilder()
                                .uri(URI.create("https://www.googleapis.com/oauth2/v2/userinfo"))
                                .header("Authorization", "Bearer " + accessToken)
                                .GET()
                                .build();
                            
                            return client.sendAsync(userRequest, HttpResponse.BodyHandlers.ofString());
                        }
                    }
                    return CompletableFuture.completedFuture(null);
                })
                .thenAccept(userResponse -> {
                    if (userResponse != null && userResponse.statusCode() == 200) {
                        String userBody = userResponse.body();
                        
                        UserInfo userInfo = new UserInfo();
                        userInfo.setId(parseJsonValue(userBody, "id"));
                        userInfo.setEmail(parseJsonValue(userBody, "email"));
                        userInfo.setName(parseJsonValue(userBody, "name"));
                        userInfo.setProvider("Google");
                        
                        Platform.runLater(() -> callback.accept(userInfo));
                    } else {
                        Platform.runLater(() -> callback.accept(null));
                    }
                })
                .exceptionally(throwable -> {
                    System.err.println("Google token exchange error: " + throwable.getMessage());
                    Platform.runLater(() -> callback.accept(null));
                    return null;
                });
                
        } catch (Exception e) {
            System.err.println("Google OAuth exchange error: " + e.getMessage());
            Platform.runLater(() -> callback.accept(null));
        }
    }

    /**
     * Exchange GitHub auth code for user info
     */
    private static void exchangeGitHubCodeForUserInfo(String authCode, Consumer<UserInfo> callback) {
        try {
            // Step 1: Exchange auth code for access token
            String tokenRequestBody = "code=" + authCode +
                "&client_id=" + GITHUB_CLIENT_ID +
                "&client_secret=" + GITHUB_CLIENT_SECRET +
                "&redirect_uri=" + REDIRECT_URI;

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest tokenRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://github.com/login/oauth/access_token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(tokenRequestBody))
                .build();

            client.sendAsync(tokenRequest, HttpResponse.BodyHandlers.ofString())
                .thenCompose(tokenResponse -> {
                    if (tokenResponse.statusCode() == 200) {
                        // Parse access token from JSON response
                        String responseBody = tokenResponse.body();
                        String accessToken = parseJsonValue(responseBody, "access_token");
                        
                        if (accessToken != null) {
                            // Step 2: Get user info using access token
                            HttpRequest userRequest = HttpRequest.newBuilder()
                                .uri(URI.create("https://api.github.com/user"))
                                .header("Authorization", "token " + accessToken)
                                .header("Accept", "application/vnd.github.v3+json")
                                .GET()
                                .build();
                            
                            return client.sendAsync(userRequest, HttpResponse.BodyHandlers.ofString());
                        }
                    }
                    return CompletableFuture.completedFuture(null);
                })
                .thenAccept(userResponse -> {
                    if (userResponse != null && userResponse.statusCode() == 200) {
                        String userBody = userResponse.body();
                        
                        UserInfo userInfo = new UserInfo();
                        userInfo.setId(parseJsonValue(userBody, "node_id")); // GitHub uses node_id
                        userInfo.setEmail(parseJsonValue(userBody, "email"));
                        userInfo.setName(parseJsonValue(userBody, "name"));
                        userInfo.setProvider("GitHub");
                        
                        System.out.println("GitHub OAuth successful: " + userInfo);
                        System.out.println("Successfully logged in with GitHub: " + userInfo.getEmail());
                        
                        Platform.runLater(() -> callback.accept(userInfo));
                    } else {
                        System.err.println("Failed to get GitHub user info. Status: " + 
                            (userResponse != null ? userResponse.statusCode() : "null"));
                        Platform.runLater(() -> callback.accept(null));
                    }
                })
                .exceptionally(throwable -> {
                    System.err.println("GitHub token exchange error: " + throwable.getMessage());
                    Platform.runLater(() -> callback.accept(null));
                    return null;
                });
                
        } catch (Exception e) {
            System.err.println("GitHub OAuth exchange error: " + e.getMessage());
            Platform.runLater(() -> callback.accept(null));
        }
    }

    /**
     * Simple JSON value parser
     */
    private static String parseJsonValue(String json, String key) {
        try {
            String searchKey = "\"" + key + "\"";
            int startIndex = json.indexOf(searchKey);
            if (startIndex == -1) return null;
            
            startIndex = json.indexOf(":", startIndex) + 1;
            startIndex = json.indexOf("\"", startIndex) + 1;
            int endIndex = json.indexOf("\"", startIndex);
            
            return json.substring(startIndex, endIndex);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * HTTP callback handler
     */
    private static class CallbackHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                System.out.println("🔄 Received OAuth callback request: " + exchange.getRequestURI());
                System.out.println("🔍 Request method: " + exchange.getRequestMethod());
                System.out.println("🔍 Remote address: " + exchange.getRemoteAddress());
                
                String query = exchange.getRequestURI().getQuery();
                String authCode = null;
                String error = null;
                
                if (query != null) {
                    System.out.println("🔍 Query parameters: " + query);
                    // Parse parameters
                    String[] params = query.split("&");
                    for (String param : params) {
                        if (param.startsWith("code=")) {
                            authCode = param.substring(5);
                            System.out.println("Authorization code received successfully");
                        } else if (param.startsWith("error=")) {
                            error = param.substring(6);
                            System.err.println("OAuth error received: " + error);
                        }
                    }
                }
                
                // Send response to browser
                String response;
                int statusCode;
                
                if (authCode != null) {
                    response = """
                        <html>
                        <head><title>SafeRoom OAuth</title></head>
                        <body style="font-family: Arial, sans-serif; text-align: center; padding: 50px;">
                            <h2 style="color: green;">✅ Authentication Successful!</h2>
                            <p>You can close this window and return to SafeRoom.</p>
                            <p>Redirecting to SafeRoom...</p>
                            <script>setTimeout(() => window.close(), 3000);</script>
                        </body>
                        </html>
                        """;
                    statusCode = 200;
                } else {
                    response = """
                        <html>
                        <head><title>SafeRoom OAuth</title></head>
                        <body style="font-family: Arial, sans-serif; text-align: center; padding: 50px;">
                            <h2 style="color: red;">❌ Authentication Failed!</h2>
                            <p>Error: """ + (error != null ? error : "Unknown error") + """
                            </p>
                            <p>Please try again.</p>
                            <script>setTimeout(() => window.close(), 5000);</script>
                        </body>
                        </html>
                        """;
                    statusCode = 400;
                }
                
                // Set response headers
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                exchange.getResponseHeaders().set("Cache-Control", "no-cache");
                
                // Send response
                byte[] responseBytes = response.getBytes("UTF-8");
                exchange.sendResponseHeaders(statusCode, responseBytes.length);
                
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBytes);
                    os.flush();
                }
                
                // Complete the future with result
                authCodeFuture.complete(authCode);
                
                System.out.println("OAuth callback response sent successfully");
                
            } catch (Exception e) {
                System.err.println("Error in OAuth callback handler: " + e.getMessage());
                e.printStackTrace();
                
                // Complete with null in case of error
                if (authCodeFuture != null && !authCodeFuture.isDone()) {
                    authCodeFuture.complete(null);
                }
            }
        }
    }
}
