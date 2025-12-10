package com.saferoom.gui.dialog;

import com.saferoom.gui.components.ScreenSourceItem;
import com.saferoom.system.PlatformDetector;
import com.saferoom.webrtc.screenshare.ScreenShareController;
import com.saferoom.webrtc.screenshare.ScreenSourceOption;
import dev.onvoid.webrtc.media.video.desktop.DesktopSource;
import dev.onvoid.webrtc.media.video.desktop.ScreenCapturer;
import dev.onvoid.webrtc.media.video.desktop.WindowCapturer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.robot.Robot;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Zoom/Chrome style modal that enumerates capturable sources and lets the user pick one.
 * The modal is self-contained and returns an {@link Optional} describing the selection.
 */
public final class ScreenSourcePickerDialog implements AutoCloseable {

    private static final Logger LOGGER = Logger.getLogger(ScreenSourcePickerDialog.class.getName());
    private static final Image WINDOW_PLACEHOLDER = createWindowPlaceholder();

    private final ScreenShareController controller;
    private final ExecutorService loaderExecutor =
        Executors.newSingleThreadExecutor(r -> Thread.ofVirtual().name("screen-source-loader")
            .factory().newThread(r));
    private final AtomicBoolean closed = new AtomicBoolean();

    private Stage stage;
    private FlowPane grid;
    private Button shareButton;
    private Label statusLabel;
    private ScreenSourceItem selectedItem;
    private ScreenSourceOption selectedOption;
    private Robot fxRobot;

    public ScreenSourcePickerDialog(ScreenShareController controller) {
        this.controller = Objects.requireNonNull(controller, "controller");
    }

    public Optional<ScreenSourceOption> showAndWait(Window owner) {
        stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Share your screen");
        stage.setResizable(false);

        Scene scene = new Scene(createRoot(), 920, 620);
        scene.getStylesheets().add(Objects.requireNonNull(
            getClass().getResource("/styles/call-view.css")).toExternalForm());

        stage.setScene(scene);
        stage.setOnCloseRequest(e -> {
            closeStage();
            e.consume();
        });

        loadSourcesAsync();

        stage.showAndWait();
        return Optional.ofNullable(selectedOption);
    }

    private BorderPane createRoot() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));
        root.getStyleClass().add("screen-picker-root");

        Label title = new Label("Choose what to share");
        title.getStyleClass().add("screen-picker-title");

        statusLabel = new Label("Loading available sources…");
        statusLabel.getStyleClass().add("screen-picker-status");

        VBox header = new VBox(title, statusLabel);
        header.setSpacing(8);
        root.setTop(header);

        grid = new FlowPane();
        grid.setHgap(18);
        grid.setVgap(18);
        grid.setPadding(new Insets(10));

        ScrollPane scrollPane = new ScrollPane(grid);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("screen-picker-scroll");
        root.setCenter(scrollPane);

        Button cancelButton = new Button("Cancel");
        cancelButton.getStyleClass().add("screen-picker-button");
        cancelButton.setOnAction(e -> {
            selectedOption = null;
            closeStage();
        });

        shareButton = new Button("Share");
        shareButton.setDisable(true);
        shareButton.getStyleClass().addAll("screen-picker-button", "primary");
        shareButton.setOnAction(e -> closeStage());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox footer = new HBox(12, cancelButton, spacer, shareButton);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(20, 0, 0, 0));
        root.setBottom(footer);

        if (controller.isSharing()) {
            statusLabel.setText("Screen share is currently active on another window.");
        }

        return root;
    }

    private void loadSourcesAsync() {
        if (loaderExecutor.isShutdown() || closed.get()) {
            return;
        }
        CompletableFuture
            .supplyAsync(this::collectSources, loaderExecutor)
            .whenComplete((sources, error) -> Platform.runLater(() -> {
                if (closed.get()) {
                    return;
                }
                if (error != null) {
                    LOGGER.log(Level.SEVERE, "Failed to enumerate screen sources", error);
                    statusLabel.setText("Unable to enumerate sources");
                    return;
                }
                populateSources(sources);
            }));
    }

    private List<SourceCandidate> collectSources() {
        if (closed.get()) {
            return List.of();
        }
        // Linux screen share is disabled - return empty list
        if (PlatformDetector.isLinux()) {
            return List.of();
        }
        return collectDesktopSources();
    }

    private List<SourceCandidate> collectDesktopSources() {
        List<SourceCandidate> candidates = new ArrayList<>();
        ScreenCapturer screenCapturer = null;
        WindowCapturer windowCapturer = null;
        try {
            screenCapturer = new ScreenCapturer();
            List<DesktopSource> screens = screenCapturer.getDesktopSources();
            if (screens != null) {
                for (int i = 0; i < screens.size(); i++) {
                    DesktopSource source = screens.get(i);
                    String subtitle = "Monitor " + (i + 1);
                    candidates.add(new SourceCandidate(ScreenSourceOption.onvoidScreen(source), subtitle, i));
                }
            }
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "Screen enumeration failed", t);
        } finally {
            if (screenCapturer != null) {
                try {
                    screenCapturer.dispose();
                } catch (Throwable ignored) {
                }
            }
        }

        try {
            windowCapturer = new WindowCapturer();
            List<DesktopSource> windows = windowCapturer.getDesktopSources();
            if (windows != null) {
                for (DesktopSource window : windows) {
                    String subtitle = window.title != null && !window.title.isBlank()
                        ? window.title
                        : "Window " + window.id;
                    candidates.add(new SourceCandidate(ScreenSourceOption.onvoidWindow(window), subtitle, -1));
                }
            }
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "Window enumeration failed", t);
        } finally {
            if (windowCapturer != null) {
                try {
                    windowCapturer.dispose();
                } catch (Throwable ignored) {
                }
            }
        }

        if (candidates.isEmpty()) {
            candidates.add(new SourceCandidate(ScreenSourceOption.auto(), "Automatic selection", -1));
        }
        return candidates;
    }

    private void populateSources(List<SourceCandidate> candidates) {
        grid.getChildren().clear();
        
        // Show special message for Linux
        if (PlatformDetector.isLinux()) {
            statusLabel.setText("⚠️ Screen sharing is temporarily disabled on Linux.");
            statusLabel.setStyle("-fx-text-fill: #f59e0b;"); // Warning color
            shareButton.setDisable(true);
            
            Label infoLabel = new Label(
                "Screen sharing on Linux requires native capture support\n" +
                "which is not yet available. This feature will be enabled\n" +
                "in a future update."
            );
            infoLabel.setStyle("-fx-text-fill: #94a1b2; -fx-font-size: 14px; -fx-text-alignment: center;");
            infoLabel.setWrapText(true);
            grid.getChildren().add(infoLabel);
            return;
        }
        
        if (candidates.isEmpty()) {
            statusLabel.setText("No shareable sources were found.");
            return;
        }

        for (SourceCandidate candidate : candidates) {
            ScreenSourceItem item = new ScreenSourceItem(candidate.option(), candidate.subtitle());
            item.setOnMouseClicked(e -> selectItem(item));
            grid.getChildren().add(item);
            scheduleThumbnailCapture(item, candidate);
        }

        statusLabel.setText(String.format("Select a source (%d available)", candidates.size()));
    }

    private void scheduleThumbnailCapture(ScreenSourceItem item, SourceCandidate candidate) {
        if (loaderExecutor.isShutdown() || closed.get()) {
            return;
        }
        captureThumbnailAsync(candidate)
            .thenAccept(image -> Platform.runLater(() -> {
                if (closed.get()) {
                    return;
                }
                Image resolved = image != null ? image : WINDOW_PLACEHOLDER;
                item.setThumbnail(resolved);
            }))
            .exceptionally(ex -> {
                LOGGER.log(Level.FINE, "Thumbnail capture failed", ex);
                return null;
            });
    }

    private CompletableFuture<Image> captureThumbnailAsync(SourceCandidate candidate) {
        if (candidate.monitorIndex() >= 0) {
            return prepareBoundsAsync(candidate.monitorIndex()).thenCompose(this::captureWithRobot);
        }
        return CompletableFuture.completedFuture(WINDOW_PLACEHOLDER);
    }

    private <T> CompletableFuture<T> scheduleOnExecutor(SupplierWithException<T> supplier) {
        if (loaderExecutor.isShutdown()) {
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.supplyAsync(() -> {
            if (closed.get()) {
                return null;
            }
            try {
                return supplier.get();
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Thumbnail task failed", ex);
                return null;
            }
        }, loaderExecutor);
    }

    private CompletableFuture<Rectangle2D> prepareBoundsAsync(int monitorIndex) {
        return scheduleOnExecutor(() -> {
            List<Screen> screens = Screen.getScreens();
            if (screens.isEmpty()) {
                return Screen.getPrimary().getBounds();
            }
            int index = Math.min(Math.max(monitorIndex, 0), screens.size() - 1);
            return screens.get(index).getBounds();
        });
    }

    private CompletableFuture<Image> captureWithRobot(Rectangle2D bounds) {
        CompletableFuture<Image> future = new CompletableFuture<>();
        if (bounds == null) {
            future.complete(null);
            return future;
        }
        Platform.runLater(() -> {
            if (closed.get()) {
                future.complete(null);
                return;
            }
            try {
                Robot robot = ensureRobot();
                future.complete(robot.getScreenCapture(null, bounds));
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Robot screenshot failed", ex);
                future.complete(null);
            }
        });
        return future;
    }

    private Robot ensureRobot() {
        if (fxRobot == null) {
            fxRobot = new Robot();
        }
        return fxRobot;
    }

    private static Image createWindowPlaceholder() {
        WritableImage image = new WritableImage(220, 130);
        PixelWriter writer = image.getPixelWriter();
        for (int y = 0; y < 130; y++) {
            for (int x = 0; x < 220; x++) {
                double gradient = 0.15 + (double) y / 130 * 0.35;
                writer.setColor(x, y, Color.color(0.1, 0.2 + gradient, 0.35 + gradient));
            }
        }
        return image;
    }

    private void selectItem(ScreenSourceItem item) {
        if (selectedItem != null) {
            selectedItem.setSelected(false);
        }
        selectedItem = item;
        selectedItem.setSelected(true);
        selectedOption = item.getSource();
        shareButton.setDisable(false);
        statusLabel.setText("Selected: " + selectedOption.title());
    }

    private void closeStage() {
        if (closed.compareAndSet(false, true) && stage != null) {
            stage.close();
        }
        if (!loaderExecutor.isShutdown()) {
            loaderExecutor.shutdownNow();
        }
    }

    @Override
    public void close() {
        closeStage();
    }

    @FunctionalInterface
    private interface SupplierWithException<T> {
        T get() throws Exception;
    }

    private record SourceCandidate(ScreenSourceOption option, String subtitle, int monitorIndex) {
    }
}
