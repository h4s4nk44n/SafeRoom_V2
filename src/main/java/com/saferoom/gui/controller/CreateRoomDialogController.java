package com.saferoom.gui.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class CreateRoomDialogController implements Initializable {

    @FXML
    private TextField roomNameField;
    @FXML
    private RadioButton publicRadio;
    @FXML
    private RadioButton privateRadio;
    @FXML
    private ToggleGroup privacyGroup;
    @FXML
    private StackPane imageUploadContainer;
    @FXML
    private Circle roomImagePreview;
    @FXML
    private Button cancelBtn;
    @FXML
    private Button createBtn;

    private boolean confirmed = false;
    private String resultName;
    private boolean isPrivate;
    private Image resultImage; // Store the selected image

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Setup buttons
        cancelBtn.setOnAction(e -> closeDialog());
        createBtn.setOnAction(e -> confirmCreation());

        // Setup Image Upload Handlers
        setupImageUploadHandlers();
    }

    private void setupImageUploadHandlers() {
        // Click to upload
        imageUploadContainer.setOnMouseClicked(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Room Icon");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif"));
            File selectedFile = fileChooser.showOpenDialog(imageUploadContainer.getScene().getWindow());
            if (selectedFile != null) {
                loadImage(selectedFile);
            }
        });

        // Drag over
        imageUploadContainer.setOnDragOver(event -> {
            if (event.getGestureSource() != imageUploadContainer && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            event.consume();
        });

        // Drag dropped
        imageUploadContainer.setOnDragDropped(event -> {
            boolean success = false;
            if (event.getDragboard().hasFiles()) {
                List<File> files = event.getDragboard().getFiles();
                if (!files.isEmpty()) {
                    File file = files.get(0);
                    // Check if it's an image
                    if (isImageFile(file)) {
                        loadImage(file);
                        success = true;
                    }
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    private boolean isImageFile(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".gif");
    }

    private void loadImage(File file) {
        try {
            Image image = new Image(file.toURI().toString());
            roomImagePreview.setFill(new ImagePattern(image));
            resultImage = image;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void confirmCreation() {
        if (roomNameField.getText() == null || roomNameField.getText().trim().isEmpty()) {
            roomNameField.setStyle("-fx-border-color: #ef4444;");
            return;
        }

        resultName = roomNameField.getText().trim();
        isPrivate = privateRadio.isSelected();

        confirmed = true;
        closeDialog();
    }

    private void closeDialog() {
        ((Stage) createBtn.getScene().getWindow()).close();
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public String getResultName() {
        return resultName;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public Image getResultImage() {
        return resultImage;
    }
}
