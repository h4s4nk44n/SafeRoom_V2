package com.saferoom.gui.view.cell;

import com.saferoom.gui.model.FileAttachment;
import com.saferoom.gui.model.Message;
import com.saferoom.gui.model.MessageType;
import com.saferoom.system.PlatformDetector;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.value.ChangeListener;
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
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

public class MessageCell extends ListCell<Message> {
    private static final double THUMB_SIZE = 140;

    // Layout Temelleri
    private final HBox hbox = new HBox(10);
    private final Label avatar = new Label();
    private final Pane spacer = new Pane();

    // Ana İçerik Taşıyıcısı (Hata veren bubbleContentWrapper buydu)
    private final StackPane bubbleContentWrapper;

    // --- METİN MESAJI BİLEŞENLERİ (TextFlow) ---
    private final VBox textContainer;
    private final TextFlow textFlow; // Baloncuk
    private final Text textNode; // Yazı

    // --- DOSYA MESAJI BİLEŞENLERİ (Hata veren fileContainer buydu) ---
    private final VBox fileContainer;
    private final HBox fileMetaRow;
    private final Label fileName;
    private final Label fileSize;
    private final Label fileIcon;
    private final StackPane filePreviewWrapper;
    private final ImageView fileImageView;

    // Durum Değişkenleri
    private final String currentUserId;
    private ProgressBar boundProgressBar;
    private Label boundStatusLabel;
    private Message boundMessage;
    private ChangeListener<MessageType> typeListener;

    // Highlight support
    private boolean isHighlighted = false;

    // Sabit genişlik yedeği (ListView yüklenmezse kullanılır)
    private static final double MAX_TEXT_WIDTH = 350;

    public MessageCell(String currentUserId) {
        super();
        this.currentUserId = currentUserId;

        avatar.getStyleClass().add("message-avatar");
        HBox.setHgrow(spacer, Priority.ALWAYS);
        getStyleClass().add("message-cell");

        // 1. METİN MESAJI BİLEŞENLERİO (TextFlow Yapısı)
        textNode = new Text();
        textNode.getStyleClass().add("message-text"); // CSS: .message-text

        textFlow = new TextFlow(textNode);
        textFlow.getStyleClass().add("message-bubble-text"); // CSS: .message-bubble-text
        textFlow.setPadding(new javafx.geometry.Insets(8, 12, 8, 12));

        textContainer = new VBox(textFlow);
        textContainer.getStyleClass().add("message-bubble-container");

        // 2. DOSYA MESAJI BİLEŞENLERİ
        fileIcon = new Label();
        fileIcon.getStyleClass().add("file-card-icon");

        fileName = new Label();
        fileName.getStyleClass().add("file-name");

        fileSize = new Label();
        fileSize.getStyleClass().add("file-size");

        fileImageView = new ImageView();
        fileImageView.setPreserveRatio(true);
        fileImageView.setFitWidth(THUMB_SIZE);
        fileImageView.setFitHeight(THUMB_SIZE);
        fileImageView.setMouseTransparent(true);

        filePreviewWrapper = new StackPane(fileImageView);
        filePreviewWrapper.getStyleClass().add("file-thumbnail");
        filePreviewWrapper.setFocusTraversable(false);
        filePreviewWrapper.setPickOnBounds(true);

        VBox metaText = new VBox(2, fileName, fileSize);
        fileMetaRow = new HBox(8, fileIcon, metaText);

        fileContainer = new VBox(6);
        fileContainer.getStyleClass().add("file-message-bubble");

        // 3. ANA WRAPPER OLUŞTURMA
        // Hem metin hem dosya konteynerini içine alır
        bubbleContentWrapper = new StackPane(textContainer, fileContainer);

        // HBox içine yerleştirme
        hbox.getChildren().setAll(avatar, spacer, bubbleContentWrapper);
    }

    @Override
    protected void updateItem(Message message, boolean empty) {
        super.updateItem(message, empty);
        unbindFileBindings();
        detachTypeListener();

        getStyleClass().remove("message-highlight");
        isHighlighted = false;

        if (empty || message == null || currentUserId == null) {
            boundMessage = null;
            setGraphic(null);
        } else {
            boundMessage = message;
            avatar.setText(message.getSenderAvatarChar());
            final boolean isSentByMe = message.getSenderId().equals(currentUserId);

            // 1. Hizalama ve Stiller
            if (isSentByMe) {
                hbox.getChildren().setAll(spacer, bubbleContentWrapper, avatar);

                // TextFlow Stil Güncellemesi
                textFlow.getStyleClass().removeAll("message-bubble-received");
                if (!textFlow.getStyleClass().contains("message-bubble-sent"))
                    textFlow.getStyleClass().add("message-bubble-sent");
            } else {
                hbox.getChildren().setAll(avatar, bubbleContentWrapper, spacer);

                // TextFlow Stil Güncellemesi
                textFlow.getStyleClass().removeAll("message-bubble-sent");
                if (!textFlow.getStyleClass().contains("message-bubble-received"))
                    textFlow.getStyleClass().add("message-bubble-received");
            }

            // 2. İçerik Yönetimi (GİZLE / GÖSTER)
            textContainer.setVisible(false);
            textContainer.setManaged(false);
            fileContainer.setVisible(false);
            fileContainer.setManaged(false);

            if (message.getType() == MessageType.TEXT) {
                // --- METİN MESAJI ---
                textContainer.setVisible(true);
                textContainer.setManaged(true);

                textNode.setText(message.getText());

                // RESPONSIVE AYAR (ListView genişliğine göre)
                if (getListView() != null) {
                    textFlow.maxWidthProperty().unbind();
                    textFlow.maxWidthProperty().bind(
                            getListView().widthProperty().subtract(160));
                } else {
                    textFlow.setMaxWidth(MAX_TEXT_WIDTH);
                }

            } else {
                // --- DOSYA MESAJI ---
                fileContainer.setVisible(true);
                fileContainer.setManaged(true);

                fileContainer.getStyleClass().removeAll("file-bubble-sent", "file-bubble-received");
                fileContainer.getStyleClass().add(isSentByMe ? "file-bubble-sent" : "file-bubble-received");

                updateFileContent(message, isSentByMe);
            }

            attachTypeListener(bubbleContentWrapper, message);
            setGraphic(hbox);
        }
    }

    // --- DOSYA İÇERİK GÜNCELLEME METODU (Eksik olan buydu) ---
    private void updateFileContent(Message message, boolean isSentByMe) {
        FileAttachment attachment = message.getAttachment();

        // 1. Verileri Yaz
        fileName.setText(attachment != null ? attachment.getFileName() : "File");
        fileSize.setText(attachment != null ? attachment.getFormattedSize() : "");
        fileIcon.setText(iconForType(attachment != null ? attachment.getTargetType() : MessageType.FILE));

        // 2. Temizle ve Yeniden Diz
        fileContainer.getChildren().clear();

        fileContainer.getChildren().add(filePreviewWrapper);
        updateFilePreview(message, attachment); // Resmi güncelle

        fileContainer.getChildren().add(fileMetaRow);

        // 3. Progress Bar Yönetimi
        if (message.getType() == MessageType.FILE_PLACEHOLDER) {
            if (boundProgressBar == null) {
                boundProgressBar = new ProgressBar(200);
                boundStatusLabel = new Label();
                boundStatusLabel.getStyleClass().add("file-status");
            }
            boundProgressBar.progressProperty().bind(message.progressProperty());
            boundStatusLabel.textProperty().bind(message.statusTextProperty());

            fileContainer.getChildren().addAll(boundProgressBar, boundStatusLabel);
        } else {
            Label status = new Label(message.getStatusText().isEmpty() ? "Sent" : message.getStatusText());
            status.getStyleClass().add("file-status");
            fileContainer.getChildren().add(status);
        }
    }

    private void updateFilePreview(Message message, FileAttachment attachment) {
        filePreviewWrapper.getChildren().clear();

        // ImageView'i silinmemesi için geri ekle (Siyah Kutu Çözümü)
        filePreviewWrapper.getChildren().add(fileImageView);

        fileImageView.setImage(null);

        if (attachment == null)
            return;

        if (attachment.getTargetType() == MessageType.IMAGE || isPdfAttachment(attachment)) {
            fileImageView.setImage(attachment.getThumbnail());
            fileImageView.setEffect(message.getType() == MessageType.FILE_PLACEHOLDER ? PLACEHOLDER_BLUR : null);

            if (!filePreviewWrapper.getStyleClass().contains("interactive-thumb")) {
                filePreviewWrapper.getStyleClass().add("interactive-thumb");
            }

            if (message.getType() == MessageType.FILE_PLACEHOLDER) {
                filePreviewWrapper.getChildren().add(createProgressOverlay(message));
            } else {
                // Click Event
                filePreviewWrapper.setOnMousePressed(e -> {
                    if (e.isPrimaryButtonDown()) {
                        e.consume();
                        if (isPdfAttachment(attachment))
                            openPdfModal(attachment);
                        else
                            openPreviewModal(attachment);
                    }
                });
            }
        }
    }

    // --- YARDIMCI METOTLAR ---

    private StackPane createProgressOverlay(Message message) {
        Rectangle dim = new Rectangle(THUMB_SIZE, THUMB_SIZE, Color.color(0, 0, 0, 0.45));
        Label percent = new Label();
        StringBinding percentText = Bindings.createStringBinding(
                () -> String.format("%.0f%%", message.getProgress() * 100),
                message.progressProperty());
        percent.textProperty().bind(percentText);
        percent.getStyleClass().add("image-overlay-text");
        StackPane overlay = new StackPane(dim, percent);
        overlay.getStyleClass().add("image-overlay");
        return overlay;
    }

    private static final GaussianBlur PLACEHOLDER_BLUR = new GaussianBlur(8);

    private boolean isPdfAttachment(FileAttachment attachment) {
        return attachment != null
                && attachment.getFileName() != null
                && attachment.getFileName().toLowerCase().endsWith(".pdf");
    }

    private void attachTypeListener(Node bubble, Message message) {
        typeListener = (obs, oldType, newType) -> {
            if (newType != oldType && getListView() != null && boundMessage == message) {
                Platform.runLater(() -> {
                    if (boundMessage == message) {
                        updateItem(message, false);
                    }
                });
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
        if (type == null)
            return "\uD83D\uDCCE";
        return switch (type) {
            case IMAGE -> "\uD83D\uDDBC";
            case VIDEO -> "\u25B6";
            case DOCUMENT -> "\uD83D\uDCC4";
            case FILE, FILE_PLACEHOLDER, TEXT -> "\uD83D\uDCCE";
        };
    }

    // Modal ve Dosya Açma Metotları
    private void openPreviewModal(FileAttachment attachment) {
        if (attachment == null || attachment.getLocalPath() == null)
            return;

        Image image = new Image(attachment.getLocalPath().toUri().toString(), 0, 0, true, true, true);
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
            if (event.getCode() == KeyCode.ESCAPE)
                stage.close();
        });
        root.setOnMouseClicked(e -> stage.close());
        stage.show();
    }

    private void openPdfModal(FileAttachment attachment) {
        if (attachment == null || attachment.getLocalPath() == null)
            return;
        openWithSystemApp(attachment.getLocalPath());
    }

    private void openWithSystemApp(Path filePath) {
        CompletableFuture.runAsync(() -> {
            try {
                if (PlatformDetector.isLinux()) {
                    new ProcessBuilder("xdg-open", filePath.toAbsolutePath().toString())
                            .redirectErrorStream(true)
                            .start();
                } else {
                    if (java.awt.Desktop.isDesktopSupported()) {
                        java.awt.Desktop.getDesktop().open(filePath.toFile());
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to open file: " + e.getMessage());
            }
        });
    }

    private void openGenericFile(FileAttachment attachment) {
        if (attachment == null || attachment.getLocalPath() == null)
            return;
        Path path = attachment.getLocalPath();
        String fileName = attachment.getFileName();
        if (fileName == null)
            fileName = path.getFileName().toString();

        if (isTextFile(fileName.toLowerCase())) {
            openTextViewerModal(attachment);
        } else {
            openWithXdgOpen(path);
        }
    }

    private boolean isTextFile(String fileName) {
        return fileName.endsWith(".txt") || fileName.endsWith(".log") ||
                fileName.endsWith(".md") || fileName.endsWith(".json") ||
                fileName.endsWith(".xml") || fileName.endsWith(".csv") ||
                fileName.endsWith(".java") || fileName.endsWith(".py");
    }

    private void openTextViewerModal(FileAttachment attachment) {
        try {
            Path path = attachment.getLocalPath();
            String content = java.nio.file.Files.readString(path);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(attachment.getFileName());

            javafx.scene.control.TextArea textArea = new javafx.scene.control.TextArea(content);
            textArea.setEditable(false);
            textArea.setWrapText(true);
            textArea.setStyle(
                    "-fx-control-inner-background: #1a1d21; -fx-text-fill: #e5e5e5; -fx-font-family: monospace;");

            javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(textArea);
            root.setStyle("-fx-background-color: #0f111a; -fx-padding: 10;");
            javafx.scene.layout.VBox.setVgrow(textArea, javafx.scene.layout.Priority.ALWAYS);

            Scene scene = new Scene(root, 700, 500);
            scene.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.ESCAPE)
                    stage.close();
            });

            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            openWithXdgOpen(attachment.getLocalPath());
        }
    }

    private void openWithXdgOpen(Path filePath) {
        openWithSystemApp(filePath); // Reusing the safe method
    }
}