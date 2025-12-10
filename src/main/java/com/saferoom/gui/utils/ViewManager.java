package com.saferoom.gui.utils;

import com.saferoom.gui.MainApp;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * FXML sahneleri arasında geçiş yapmayı yöneten sınıf.
 * 
 * <h2>Metaspace Leak Fix</h2>
 * <p>
 * Implemented caching for Views/Scenes. Previously, every switchScene call
 * loaded a new FXMLLoader, creating new classloaders/reflection data, leading
 * to Metaspace leaks.
 * </p>
 */
public class ViewManager {

    private static final Map<String, Scene> viewCache = new HashMap<>();

    /**
     * Mevcut sahneyi yeni bir FXML dosyasıyla değiştirir.
     * 
     * @param currentStage Mevcut pencere (Stage)
     * @param fxmlFile     Yüklenecek yeni FXML dosyasının adı (ör: "MainView.fxml")
     */
    public static void switchScene(Stage currentStage, String fxmlFile) throws IOException {
        Scene scene = viewCache.get(fxmlFile);

        if (scene == null) {
            System.out.println("[ViewManager] Loading new view: " + fxmlFile);
            // Yeni FXML dosyasını yükle
            Parent root = FXMLLoader.load(Objects.requireNonNull(MainApp.class.getResource("/view/" + fxmlFile)));

            // Yeni sahneyi oluştur ve stilleri uygula
            scene = new Scene(root);
            scene.getStylesheets()
                    .add(Objects.requireNonNull(MainApp.class.getResource("styles/styles.css")).toExternalForm());

            // Cache the scene
            viewCache.put(fxmlFile, scene);
        } else {
            System.out.println("[ViewManager] Reusing cached view: " + fxmlFile);
        }

        // Pencerenin sahnesini yeni sahne ile değiştir
        currentStage.setScene(scene);
        currentStage.centerOnScreen(); // Pencereyi ekranın ortasına al
    }

    /**
     * Clear the cache if needed (e.g. on logout or language change).
     */
    public static void clearCache() {
        viewCache.clear();
    }
}
