package com.saferoom.gui.dialog;

import dev.onvoid.webrtc.media.video.desktop.DesktopSource;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.util.List;
import java.util.Optional;

/**
 * Dialog for selecting screen/window to share
 */
public class ScreenSharePickerDialog {
    
    @FXML private TabPane sourceTabPane;
    @FXML private ListView<DesktopSource> screensListView;
    @FXML private ListView<DesktopSource> windowsListView;
    @FXML private Label infoLabel;
    @FXML private Button refreshButton;
    @FXML private Button cancelButton;
    @FXML private Button startButton;
    
    private Stage dialogStage;
    private DesktopSource selectedSource;
    private boolean isWindow;
    private boolean confirmed = false;
    
    // Data
    private List<DesktopSource> availableScreens;
    private List<DesktopSource> availableWindows;
    
    /**
     * Initialize the dialog
     */
    @FXML
    public void initialize() {
        System.out.println("[ScreenSharePicker] Initializing dialog...");
        
        // Configure ListViews with custom cell factory
        StringConverter<DesktopSource> converter = new StringConverter<DesktopSource>() {
            @Override
            public String toString(DesktopSource source) {
                if (source == null) return "";
                return String.format("%s (ID: %d)", source.title, source.id);
            }
            
            @Override
            public DesktopSource fromString(String string) {
                return null; // Not needed for display-only
            }
        };
        
        screensListView.setCellFactory(lv -> new TextFieldListCell<>(converter));
        windowsListView.setCellFactory(lv -> new TextFieldListCell<>(converter));
        
        // Initially disable start button
        startButton.setDisable(true);
        
        // Add selection listeners
        screensListView.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) {
                windowsListView.getSelectionModel().clearSelection();
                startButton.setDisable(false);
                System.out.printf("[ScreenSharePicker] Screen selected: %s%n", newVal.title);
            }
        });
        
        windowsListView.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) {
                screensListView.getSelectionModel().clearSelection();
                startButton.setDisable(false);
                System.out.printf("[ScreenSharePicker] Window selected: %s%n", newVal.title);
            }
        });
        
        System.out.println("[ScreenSharePicker] Dialog initialized");
    }
    
    /**
     * Set available sources and populate lists
     */
    public void setAvailableSources(List<DesktopSource> screens, List<DesktopSource> windows) {
        this.availableScreens = screens;
        this.availableWindows = windows;
        
        // Clear existing items first
        screensListView.getItems().clear();
        windowsListView.getItems().clear();
        
        // Set new items (safely handle null)
        if (screens != null && !screens.isEmpty()) {
            screensListView.getItems().setAll(screens);
        }
        
        if (windows != null && !windows.isEmpty()) {
            windowsListView.getItems().setAll(windows);
        }
        
        System.out.printf("[ScreenSharePicker] Sources loaded: %d screens, %d windows%n", 
            screens != null ? screens.size() : 0, 
            windows != null ? windows.size() : 0);
        
        // Auto-select first screen if available (SAFE)
        if (screens != null && !screens.isEmpty()) {
            try {
                screensListView.getSelectionModel().selectFirst();
                sourceTabPane.getSelectionModel().selectFirst(); // Select screens tab
                System.out.println("[ScreenSharePicker] Auto-selected first screen");
            } catch (Exception e) {
                System.err.printf("[ScreenSharePicker] ⚠️ Failed to auto-select screen: %s%n", e.getMessage());
            }
        } else if (windows != null && !windows.isEmpty()) {
            try {
                windowsListView.getSelectionModel().selectFirst();
                sourceTabPane.getSelectionModel().selectLast(); // Select windows tab
                System.out.println("[ScreenSharePicker] Auto-selected first window");
            } catch (Exception e) {
                System.err.printf("[ScreenSharePicker] ⚠️ Failed to auto-select window: %s%n", e.getMessage());
            }
        } else {
            System.out.println("[ScreenSharePicker] ⚠️ No sources available for auto-selection");
        }
    }
    
    /**
     * Set the dialog stage (for closing)
     */
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }
    
    /**
     * Handle refresh button click
     */
    @FXML
    private void handleRefresh() {
        System.out.println("[ScreenSharePicker] Refreshing sources...");
        infoLabel.setText("🔄 Yenileniyor...");
        
        // Trigger refresh through callback (will be set by caller)
        if (dialogStage != null) {
            // TODO: Add refresh callback mechanism
            infoLabel.setText("💡 Kaynaklar yenilendi");
        }
    }
    
    /**
     * Handle cancel button click
     */
    @FXML
    private void handleCancel() {
        System.out.println("[ScreenSharePicker] User cancelled");
        confirmed = false;
        dialogStage.close();
    }
    
    /**
     * Handle start sharing button click
     */
    @FXML
    private void handleStartSharing() {
        // Determine which source was selected
        DesktopSource screenSelection = screensListView.getSelectionModel().getSelectedItem();
        DesktopSource windowSelection = windowsListView.getSelectionModel().getSelectedItem();
        
        if (screenSelection != null) {
            selectedSource = screenSelection;
            isWindow = false;
            System.out.printf("[ScreenSharePicker] User confirmed screen: %s%n", selectedSource.title);
        } else if (windowSelection != null) {
            selectedSource = windowSelection;
            isWindow = true;
            System.out.printf("[ScreenSharePicker] User confirmed window: %s%n", selectedSource.title);
        } else {
            System.out.println("[ScreenSharePicker] No source selected!");
            showError("Lütfen paylaşmak istediğiniz ekranı veya pencereyi seçin.");
            return;
        }
        
        confirmed = true;
        dialogStage.close();
    }
    
    /**
     * Check if user confirmed selection
     */
    public boolean isConfirmed() {
        return confirmed;
    }
    
    /**
     * Get selected source
     */
    public DesktopSource getSelectedSource() {
        return selectedSource;
    }
    
    /**
     * Check if selected source is a window (vs screen)
     */
    public boolean isWindowSelected() {
        return isWindow;
    }
    
    /**
     * Show error alert
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Hata");
        alert.setHeaderText("Ekran Paylaşımı Hatası");
        alert.setContentText(message);
        alert.initOwner(dialogStage);
        alert.showAndWait();
    }
}
