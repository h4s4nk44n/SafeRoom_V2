package com.saferoom.gui.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class CreateChannelDialogController implements Initializable {

    @FXML
    private RadioButton createCategoryRadio;
    @FXML
    private RadioButton createChannelRadio;
    @FXML
    private ToggleGroup creationTypeGroup;

    @FXML
    private VBox channelOptionsBox;
    @FXML
    private RadioButton textChannelRadio;
    @FXML
    private RadioButton voiceChannelRadio;
    @FXML
    private ToggleGroup channelTypeGroup;
    @FXML
    private VBox voiceLimitBox;
    @FXML
    private Spinner<Integer> userLimitSpinner;

    @FXML
    private TextField nameField;
    @FXML
    private Button cancelBtn;
    @FXML
    private Button createBtn;

    private boolean confirmed = false;
    private String resultName;
    private boolean isCategory;
    private boolean isVoice;
    private int userLimit;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Setup spinner
        SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 12, 5);
        userLimitSpinner.setValueFactory(valueFactory);
        userLimitSpinner.setEditable(true);

        // Restrict input to numbers only and max 12
        javafx.scene.control.TextFormatter<Integer> formatter = new javafx.scene.control.TextFormatter<>(
                new javafx.util.converter.IntegerStringConverter(),
                5,
                change -> {
                    String newText = change.getControlNewText();
                    if (newText.isEmpty()) {
                        return change;
                    }
                    if (newText.matches("\\d*")) {
                        try {
                            int value = Integer.parseInt(newText);
                            if (value <= 12) {
                                return change;
                            }
                        } catch (NumberFormatException e) {
                            // Ignore
                        }
                    }
                    return null;
                });
        userLimitSpinner.getEditor().setTextFormatter(formatter);

        // Bind visibility/disable properties
        channelOptionsBox.disableProperty().bind(createCategoryRadio.selectedProperty());
        voiceLimitBox.disableProperty().bind(voiceChannelRadio.selectedProperty().not());

        // Setup buttons
        cancelBtn.setOnAction(e -> closeDialog());
        createBtn.setOnAction(e -> confirmCreation());
    }

    private void confirmCreation() {
        if (nameField.getText() == null || nameField.getText().trim().isEmpty()) {
            nameField.setStyle("-fx-border-color: red;");
            return;
        }

        resultName = nameField.getText().trim();
        isCategory = createCategoryRadio.isSelected();
        isVoice = createChannelRadio.isSelected() && voiceChannelRadio.isSelected();
        userLimit = userLimitSpinner.getValue();

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

    public boolean isCategory() {
        return isCategory;
    }

    public boolean isVoice() {
        return isVoice;
    }

    public int getUserLimit() {
        return userLimit;
    }

    public void setInitialCategory(boolean isCategory) {
        if (isCategory) {
            createCategoryRadio.setSelected(true);
        } else {
            createChannelRadio.setSelected(true);
        }
    }

    public void setInitialVoice(boolean isVoice) {
        if (isVoice) {
            voiceChannelRadio.setSelected(true);
        } else {
            textChannelRadio.setSelected(true);
        }
    }
}
