package com.saferoom.gui.utils;

import com.saferoom.gui.MainApp;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

/**
 * FXML sahneleri arasında geçiş yapmayı yöneten yardımcı sınıf.
 */
public class ViewManager {

    /**
     * Mevcut sahneyi yeni bir FXML dosyasıyla değiştirir.
     * @param currentStage Mevcut pencere (Stage)
     * @param fxmlFile Yüklenecek yeni FXML dosyasının adı (ör: "MainView.fxml")
     */
    public static void switchScene(Stage currentStage, String fxmlFile) throws IOException {
        // Yeni FXML dosyasını yükle
        Parent root = FXMLLoader.load(Objects.requireNonNull(MainApp.class.getResource("/view/" + fxmlFile)));

        // Yeni sahneyi oluştur ve stilleri uygula
        Scene scene = new Scene(root);
        scene.getStylesheets().add(Objects.requireNonNull(MainApp.class.getResource("styles/styles.css")).toExternalForm());

        // Pencerenin sahnesini yeni sahne ile değiştir
        currentStage.setScene(scene);
        currentStage.centerOnScreen(); // Pencereyi ekranın ortasına al
    }
}
