package com.saferoom.gui.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Polygon;
import javafx.util.converter.IntegerStringConverter;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class EditChannelPopupController implements Initializable {

    @FXML
    private Polygon arrow;
    @FXML
    private Label headerLabel;
    @FXML
    private TextField nameField;
    @FXML
    private VBox limitBox;
    @FXML
    private Spinner<Integer> limitSpinner;
    @FXML
    private Button deleteBtn;
    @FXML
    private Button cancelBtn;
    @FXML
    private Button saveBtn;

    private Consumer<Result> onSave;
    private Runnable onDelete;
    private Runnable onCancel;

    public static class Result {
        public String name;
        public int userLimit;

        public Result(String name, int userLimit) {
            this.name = name;
            this.userLimit = userLimit;
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Setup spinner validation
        SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 12, 0);
        limitSpinner.setValueFactory(valueFactory);
        limitSpinner.setEditable(true);

        TextFormatter<Integer> formatter = new TextFormatter<>(
                new IntegerStringConverter(),
                0,
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
        limitSpinner.getEditor().setTextFormatter(formatter);

        // Button actions
        saveBtn.setOnAction(e -> {
            if (onSave != null) {
                onSave.accept(new Result(nameField.getText(), limitSpinner.getValue()));
            }
        });

        deleteBtn.setOnAction(e -> {
            if (onDelete != null) {
                onDelete.run();
            }
        });

        cancelBtn.setOnAction(e -> {
            if (onCancel != null) {
                onCancel.run();
            }
        });
    }

    public void setData(String name, boolean isCategory, boolean isVoice, int currentLimit) {
        headerLabel.setText(isCategory ? "Edit Category" : "Edit Channel");
        nameField.setText(name);

        if (isVoice) {
            limitBox.setVisible(true);
            limitBox.setManaged(true);
            limitSpinner.getValueFactory().setValue(currentLimit > 0 ? currentLimit : 0);
        } else {
            limitBox.setVisible(false);
            limitBox.setManaged(false);
        }

        // Deselect text
        javafx.application.Platform.runLater(() -> {
            nameField.requestFocus();
            nameField.positionCaret(nameField.getText().length());
            nameField.deselect();
        });
    }

    public void setOnSave(Consumer<Result> onSave) {
        this.onSave = onSave;
    }

    public void setOnDelete(Runnable onDelete) {
        this.onDelete = onDelete;
    }

    public void setOnCancel(Runnable callback) {
        cancelBtn.setOnAction(e -> callback.run());
    }

    public void setDeleteDisabled(boolean disabled) {
        deleteBtn.setDisable(disabled);
        if (disabled) {
            deleteBtn.getStyleClass().add("disabled");
            deleteBtn.setOpacity(0.5);
        } else {
            deleteBtn.getStyleClass().remove("disabled");
            deleteBtn.setOpacity(1.0);
        }
    }
}
