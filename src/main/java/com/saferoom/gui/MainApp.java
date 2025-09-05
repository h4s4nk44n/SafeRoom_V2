package com.saferoom.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.net.URL; // URL import'u eklendi
import java.util.Objects;

public class MainApp extends Application {

    private double xOffset = 0;
    private double yOffset = 0;

    @Override
    public void start(Stage primaryStage) throws IOException {
        Parent root = FXMLLoader.load(Objects.requireNonNull(MainApp.class.getResource("/view/LoginView.fxml")));

        // Use TRANSPARENT for better cross-platform compatibility and to remove white corners
        primaryStage.setTitle("SafeRoom - Login");

        Scene scene = new Scene(root);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        
        // Stage'i de transparent yap (beyaz köşeleri önlemek için)
        primaryStage.initStyle(StageStyle.TRANSPARENT);

        // DEGISIKLIK: CSS dosyasinin adi, projenizdeki 'styles.css' ile eslesmesi icin guncellendi.
        String cssPath = "/styles/styles.css";
        URL cssUrl = MainApp.class.getResource(cssPath);

        if (cssUrl == null) {
            System.err.println("HATA: CSS dosyası bulunamadı. Lütfen 'resources/styles/' klasöründe 'styles.css' adında bir dosya olduğundan emin olun.");
        } else {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }

        // Pencereyi fare ile sürüklenebilir hale getiriyoruz.
        root.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        root.setOnMouseDragged(event -> {
            primaryStage.setX(event.getScreenX() - xOffset);
            primaryStage.setY(event.getScreenY() - yOffset);
        });

        primaryStage.setResizable(false);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
