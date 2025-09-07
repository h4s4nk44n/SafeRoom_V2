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
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class OAuthManager {
    
    // OAuth Configuration - Bu değerleri Google/GitHub'dan alacaksın
    private static final String GOOGLE_CLIENT_ID = "your-google-client-id.apps.googleusercontent.com";
    private static final String GOOGLE_CLIENT_SECRET = "your-google-client-secret";
    private static final String GITHUB_CLIENT_ID = "your-github-client-id";
    private static final String GITHUB_CLIENT_SECRET = "your-github-client-secret";
    
    private static final String REDIRECT_URI = "http://localhost:8080/callback";
    private static final int CALLBACK_PORT = 8080;
    
    private static HttpServer callbackServer;
    private static CompletableFuture<String> authCodeFuture;

    /**
     * Google OAuth Authentication
     */
    public static void authenticateWithGoogle(Consumer<UserInfo> callback) {
        try {
            // Start callback server
            startCallbackServer();
            
            // Build Google OAuth URL
            String googleAuthUrl = "https://accounts.google.com/oauth/authorize?" +
                "client_id=" + GOOGLE_CLIENT_ID + "&" +
                "redirect_uri=" + REDIRECT_URI + "&" +
                "scope=email profile&" +
                "response_type=code&" +
                "access_type=offline";
            
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
            
            // Build GitHub OAuth URL
            String githubAuthUrl = "https://github.com/login/oauth/authorize?" +
                "client_id=" + GITHUB_CLIENT_ID + "&" +
                "redirect_uri=" + REDIRECT_URI + "&" +
                "scope=user:email";
            
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
        authCodeFuture = new CompletableFuture<>();
        
        callbackServer = HttpServer.create(new InetSocketAddress(CALLBACK_PORT), 0);
        callbackServer.createContext("/callback", new CallbackHandler());
        callbackServer.start();
        
        System.out.println("OAuth callback server started on port " + CALLBACK_PORT);
    }

    /**
     * Stop callback server
     */
    private static void stopCallbackServer() {
        if (callbackServer != null) {
            callbackServer.stop(0);
            callbackServer = null;
            System.out.println("OAuth callback server stopped");
        }
    }

    /**
     * Open system browser
     */
    private static void openBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI.create(url));
            } else {
                // Fallback for Linux systems
                Runtime.getRuntime().exec("xdg-open " + url);
            }
        } catch (Exception e) {
            System.err.println("Failed to open browser: " + e.getMessage());
        }
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
                        userInfo.setId(parseJsonValue(userBody, "id"));
                        userInfo.setEmail(parseJsonValue(userBody, "email"));
                        userInfo.setName(parseJsonValue(userBody, "name"));
                        userInfo.setProvider("GitHub");
                        
                        Platform.runLater(() -> callback.accept(userInfo));
                    } else {
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
            String query = exchange.getRequestURI().getQuery();
            String authCode = null;
            
            if (query != null && query.contains("code=")) {
                // Extract auth code
                String[] params = query.split("&");
                for (String param : params) {
                    if (param.startsWith("code=")) {
                        authCode = param.substring(5);
                        break;
                    }
                }
            }
            
            // Send success response to browser
            String response = authCode != null ? 
                "<html><body><h2>✅ Authentication Successful!</h2><p>You can close this window and return to SafeRoom.</p></body></html>" :
                "<html><body><h2>❌ Authentication Failed!</h2><p>Please try again.</p></body></html>";
            
            exchange.sendResponseHeaders(200, response.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
            
            // Complete the future
            authCodeFuture.complete(authCode);
        }
    }
}
