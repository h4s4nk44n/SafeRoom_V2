package com.saferoom.gui.view.cell;

import com.saferoom.gui.model.FileAttachment;
import com.saferoom.gui.model.Message;
import com.saferoom.gui.model.MessageType;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ProgressBar;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

public class MessageCell extends ListCell<Message> {
    private final HBox hbox = new HBox(10);
    private final Label avatar = new Label();
    private final Pane spacer = new Pane();
    private final String currentUserId;
    private ProgressBar boundProgressBar;
    private Label boundStatusLabel;
    private Message boundMessage;
    private ChangeListener<MessageType> typeListener;

    public MessageCell(String currentUserId) {
        super();
        this.currentUserId = currentUserId;

        avatar.getStyleClass().add("message-avatar");
        HBox.setHgrow(spacer, Priority.ALWAYS);
    }

    @Override
    protected void updateItem(Message message, boolean empty) {
        super.updateItem(message, empty);
        unbindFileBindings();
        detachTypeListener();
        if (empty || message == null || currentUserId == null) {
            boundMessage = null;
            setGraphic(null);
        } else {
            Node bubble = createBubble(message);
            avatar.setText(message.getSenderAvatarChar());

            final boolean isSentByMe = message.getSenderId().equals(currentUserId);

            if (isSentByMe) {
                hbox.getChildren().setAll(spacer, bubble, avatar);
                bubble.getStyleClass().add("message-bubble-sent");
            } else {
                hbox.getChildren().setAll(avatar, bubble, spacer);
                bubble.getStyleClass().add("message-bubble-received");
            }
            if (bubble.getStyleClass().contains("file-message-bubble")) {
                bubble.getStyleClass().add(isSentByMe ? "file-bubble-sent" : "file-bubble-received");
            }
            boundMessage = message;
            attachTypeListener(bubble, message);
            setGraphic(hbox);
        }
    }

    private Node createBubble(Message message) {
        if (message.getType() == MessageType.TEXT) {
            Label textLabel = new Label(message.getText());
            textLabel.setWrapText(true);
            textLabel.getStyleClass().add("message-bubble");
            textLabel.maxWidthProperty().bind(widthProperty().subtract(120));
            return textLabel;
        }
        return createFileBubble(message);
    }

    private Node createFileBubble(Message message) {
        FileAttachment attachment = message.getAttachment();
        VBox container = new VBox(6);
        container.getStyleClass().add("file-message-bubble");

        Node previewNode = buildPreviewNode(message, attachment);
        if (previewNode != null) {
            container.getChildren().add(previewNode);
        }

        HBox metaRow = new HBox(8);
        Label icon = new Label(iconForType(attachment != null ? attachment.getTargetType() : MessageType.FILE));
        icon.getStyleClass().add("file-card-icon");

        Label name = new Label(attachment != null ? attachment.getFileName() : "File");
        name.getStyleClass().add("file-name");
        Label size = new Label(attachment != null ? attachment.getFormattedSize() : "");
        size.getStyleClass().add("file-size");
        VBox metaText = new VBox(2, name, size);
        metaRow.getChildren().addAll(icon, metaText);
        container.getChildren().add(metaRow);

        if (message.getType() == MessageType.FILE_PLACEHOLDER) {
            ProgressBar bar = new ProgressBar();
            bar.setPrefWidth(200);
            bar.progressProperty().bind(message.progressProperty());
            boundProgressBar = bar;
            Label status = new Label();
            status.textProperty().bind(message.statusTextProperty());
            boundStatusLabel = status;
            status.getStyleClass().add("file-status");
            container.getChildren().addAll(bar, status);
        } else {
            Label status = new Label(message.getStatusText().isEmpty() ? "Sent" : message.getStatusText());
            status.getStyleClass().add("file-status");
            container.getChildren().add(status);
            animateFadeIn(container);
        }

        return container;
    }

    private Node buildPreviewNode(Message message, FileAttachment attachment) {
        if (attachment == null) {
            return null;
        }
        if (attachment.getTargetType() == MessageType.IMAGE && attachment.getThumbnail() != null) {
            Image image = attachment.getThumbnail();
            ImageView preview = new ImageView(image);
            preview.setFitWidth(140);
            preview.setFitHeight(140);
            preview.setPreserveRatio(false);
            applyCenterCrop(preview, image);
            if (message.getType() == MessageType.FILE_PLACEHOLDER) {
                preview.setEffect(new GaussianBlur(8));
            } else {
                preview.setEffect(null);
                preview.getStyleClass().add("interactive-thumb");
                preview.setOnMouseClicked(e -> openPreviewModal(image));
            }
            StackPane thumbWrapper = new StackPane(preview);
            thumbWrapper.getStyleClass().add("file-thumbnail");
            if (message.getType() == MessageType.FILE_PLACEHOLDER) {
                StackPane overlay = createProgressOverlay(message);
                thumbWrapper.getChildren().add(overlay);
            }
            return thumbWrapper;
        }

        StackPane placeholder = new StackPane();
        placeholder.getStyleClass().add("file-generic-thumb");
        Label symbol = new Label(iconForType(attachment.getTargetType()));
        symbol.getStyleClass().add("file-card-icon");
        placeholder.getChildren().add(symbol);
        if (message.getType() == MessageType.FILE_PLACEHOLDER) {
            placeholder.getChildren().add(createProgressOverlay(message));
        }
        return placeholder;
    }

    private StackPane createProgressOverlay(Message message) {
        Rectangle dim = new Rectangle(120, 120, Color.color(0, 0, 0, 0.45));
        Label percent = new Label();
        StringBinding percentText = Bindings.createStringBinding(
            () -> String.format("%.0f%%", message.getProgress() * 100),
            message.progressProperty()
        );
        percent.textProperty().bind(percentText);
        percent.getStyleClass().add("image-overlay-text");
        StackPane overlay = new StackPane(dim, percent);
        overlay.getStyleClass().add("image-overlay");
        return overlay;
    }

    private void applyCenterCrop(ImageView view, Image image) {
        double w = image.getWidth();
        double h = image.getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        double size = Math.min(w, h);
        double x = (w - size) / 2;
        double y = (h - size) / 2;
        view.setViewport(new Rectangle2D(x, y, size, size));
    }

    private void animateFadeIn(Node node) {
        node.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(150), node);
        ft.setToValue(1);
        ft.play();
    }

    private void attachTypeListener(Node bubble, Message message) {
        typeListener = (obs, oldType, newType) -> {
            if (oldType == MessageType.FILE_PLACEHOLDER && newType != MessageType.FILE_PLACEHOLDER) {
                Platform.runLater(() -> animateFadeIn(bubble));
            }
        };
        message.typeProperty().addListener(typeListener);
    }

    private void detachTypeListener() {
        if (boundMessage != null && typeListener != null) {
            boundMessage.typeProperty().removeListener(typeListener);
            typeListener = null;
        }
    }

    private void unbindFileBindings() {
        if (boundProgressBar != null) {
            boundProgressBar.progressProperty().unbind();
            boundProgressBar = null;
        }
        if (boundStatusLabel != null) {
            boundStatusLabel.textProperty().unbind();
            boundStatusLabel = null;
        }
    }

    private String iconForType(MessageType type) {
        if (type == null) return "\uD83D\uDCCE";
        return switch (type) {
            case IMAGE -> "\uD83D\uDDBC";
            case VIDEO -> "\u25B6";
            case DOCUMENT -> "\uD83D\uDCC4";
            case FILE, FILE_PLACEHOLDER, TEXT -> "\uD83D\uDCCE";
        };
    }

    private void openPreviewModal(Image image) {
        if (image == null) {
            return;
        }
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Preview");

        ImageView full = new ImageView(image);
        full.setPreserveRatio(true);
        full.setFitWidth(720);
        full.setFitHeight(720);

        BorderPane root = new BorderPane(full);
        root.setStyle("-fx-background-color: rgba(0,0,0,0.85);");

        Scene scene = new Scene(root, 740, 740, Color.TRANSPARENT);
        stage.setScene(scene);

        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                stage.close();
            }
        });
        root.setOnMouseClicked(e -> stage.close());
        stage.show();
    }
}