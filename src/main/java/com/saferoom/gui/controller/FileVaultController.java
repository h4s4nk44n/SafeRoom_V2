package com.saferoom.gui.controller;

import com.jfoenix.controls.JFXButton;
import com.saferoom.gui.model.VaultFile;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

public class FileVaultController {

    @FXML private VBox dropZone;
    @FXML private TilePane fileGrid;
    @FXML private HBox fileFilterBar;

    private final ObservableList<VaultFile> allFiles = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupDragAndDrop();
        loadSampleFiles();
        setupFilterButtons();

        // Başlangıçta "All" filtresini aktif yap ve tüm dosyaları göster
        if (!fileFilterBar.getChildren().isEmpty()) {
            fileFilterBar.getChildren().get(0).getStyleClass().add("active");
            updateFileGrid("All");
        }
    }

    private void setupDragAndDrop() {
        dropZone.setOnDragOver(event -> {
            if (event.getGestureSource() != dropZone && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            event.consume();
        });

        dropZone.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                List<File> files = db.getFiles();
                System.out.println("Dropped files: " + files);
                // TODO: Dosya şifreleme ve listeye ekleme mantığı buraya gelecek
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    private void loadSampleFiles() {
        // Örnek dosyalara tür bilgisi eklendi
        allFiles.addAll(
                new VaultFile("project_phoenix_brief.pdf", "2.1 MB", "2025-07-26", "Documents"),
                new VaultFile("quantum_encryption_keys.dat", "128 KB", "2025-07-24", "Documents"),
                new VaultFile("secure_room_logs_archive.zip", "15.8 MB", "2025-07-23", "Archives"),
                new VaultFile("meeting_notes_alpha.docx", "786 KB", "2025-07-22", "Documents"),
                new VaultFile("asset_pack_v2.rar", "1.2 GB", "2025-07-21", "Archives"),
                new VaultFile("logo_final.png", "1.5 MB", "2025-07-20", "Images")
        );
    }

    /**
     * Filtre butonlarına tıklama olaylarını ve 'active' stil yönetimini ekler.
     */
    private void setupFilterButtons() {
        for (Node node : fileFilterBar.getChildren()) {
            if (node instanceof JFXButton) {
                JFXButton button = (JFXButton) node;
                button.setOnAction(event -> {
                    // Önce tüm butonlardan 'active' stilini kaldır
                    fileFilterBar.getChildren().forEach(btn -> btn.getStyleClass().remove("active"));
                    // Sadece tıklanan butona 'active' stilini ekle
                    button.getStyleClass().add("active");
                    // Dosya gridini güncelle
                    updateFileGrid(button.getText());
                });
            }
        }
    }

    /**
     * Seçilen filtreye göre dosya gridini günceller.
     */
    private void updateFileGrid(String filter) {
        fileGrid.getChildren().clear();
        List<VaultFile> filteredFiles;

        if (filter.equalsIgnoreCase("All")) {
            filteredFiles = allFiles;
        } else {
            // Filtre metniyle dosya türünü karşılaştır
            filteredFiles = allFiles.stream()
                    .filter(file -> file.getType().equalsIgnoreCase(filter))
                    .collect(Collectors.toList());
        }

        for (VaultFile file : filteredFiles) {
            fileGrid.getChildren().add(createFileCard(file));
        }
    }

    private VBox createFileCard(VaultFile file) {
        VBox card = new VBox(10);
        card.getStyleClass().add("file-card");

        // Üst Kısım: İkon ve Butonlar
        HBox topPane = new HBox();
        FontIcon fileIcon = new FontIcon("fas-file-alt");
        fileIcon.getStyleClass().add("file-icon");

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox actions = new HBox(5);
        actions.setAlignment(Pos.CENTER);
        Button shareButton = new Button("", new FontIcon("fas-share-alt"));
        Button downloadButton = new Button("", new FontIcon("fas-download"));
        Button deleteButton = new Button("", new FontIcon("fas-trash-alt"));
        shareButton.getStyleClass().add("file-action-button");
        downloadButton.getStyleClass().add("file-action-button");
        deleteButton.getStyleClass().add("file-action-button");
        actions.getChildren().addAll(shareButton, downloadButton, deleteButton);

        topPane.getChildren().addAll(fileIcon, spacer, actions);

        // Orta Kısım: Dosya Adı
        Label fileName = new Label(file.getName());
        fileName.getStyleClass().add("file-name");
        fileName.setWrapText(true);

        // Alt Kısım: Dosya Bilgileri
        HBox metaPane = new HBox();
        Label fileSize = new Label(file.getSize());
        fileSize.getStyleClass().add("file-meta");
        Pane metaSpacer = new Pane();
        HBox.setHgrow(metaSpacer, Priority.ALWAYS);
        Label fileDate = new Label(file.getDate());
        fileDate.getStyleClass().add("file-meta");
        metaPane.getChildren().addAll(fileSize, metaSpacer, fileDate);

        card.getChildren().addAll(topPane, fileName, metaPane);
        return card;
    }
}
