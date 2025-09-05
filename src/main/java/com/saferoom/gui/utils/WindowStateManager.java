package com.saferoom.gui.utils;

import javafx.scene.Cursor;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

/**
 * WindowStateManager pencere durumu yönetimi ve sürükleme işlevi sağlar.
 * Bu sınıf görünüm geçişleri arasında pencere konumu sürekliliğini sağlar.
 */
public class WindowStateManager {
    
    // Pencere konumunu görünüm geçişleri arasında saklamak için static değişkenler
    private static double savedX = -1;
    private static double savedY = -1;
    private static double savedWidth = -1;
    private static double savedHeight = -1;
    
    // Pencere sürükleme değişkenleri (instance-specific)
    private double xOffset = 0;
    private double yOffset = 0;
    
    // Pencere yeniden boyutlandırma değişkenleri
    private boolean isResizing = false;
    private double resizeStartX = 0;
    private double resizeStartY = 0;
    private double resizeStartWidth = 0;
    private double resizeStartHeight = 0;
    private ResizeDirection resizeDirection = ResizeDirection.NONE;
    
    private enum ResizeDirection {
        NONE, N, S, E, W, NE, NW, SE, SW
    }
    
    /**
     * Mevcut pencere konumunu ve boyutunu kaydeder
     */
    public static void saveWindowState(Stage stage) {
        if (stage != null) {
            savedX = stage.getX();
            savedY = stage.getY();
            savedWidth = stage.getWidth();
            savedHeight = stage.getHeight();
        }
    }
    
    /**
     * Önceden kaydedilen pencere konumunu ve boyutunu geri yükler
     */
    public static void restoreWindowState(Stage stage) {
        if (stage != null && savedX != -1 && savedY != -1) {
            stage.setX(savedX);
            stage.setY(savedY);
            
            if (savedWidth != -1 && savedHeight != -1) {
                stage.setWidth(savedWidth);
                stage.setHeight(savedHeight);
            }
        }
    }
    
    /**
     * Kaydedilen pencere durumunun geçerli olup olmadığını kontrol eder
     */
    public static boolean hasValidState() {
        return savedX != -1 && savedY != -1;
    }
    
    /**
     * Verilen root pane için pencere sürükleme işlevini kurar.
     * Bu metod mouse event handler'larını ekleyerek pencere sürükleme ve yeniden boyutlandırmayı etkinleştirir.
     */
    public void setupWindowDrag(Pane rootPane) {
        if (rootPane == null) return;
        
        final int RESIZE_BORDER = 5; // Yeniden boyutlandırma sınırı pixel cinsinden

        rootPane.setOnMousePressed(event -> {
            Stage stage = (Stage) rootPane.getScene().getWindow();
            
            // Kenarlardan yeniden boyutlandırma kontrolü
            resizeDirection = getResizeDirection(event.getSceneX(), event.getSceneY(), 
                                               rootPane.getWidth(), rootPane.getHeight(), RESIZE_BORDER);
            
            if (resizeDirection != ResizeDirection.NONE) {
                isResizing = true;
                resizeStartX = stage.getX();
                resizeStartY = stage.getY();
                resizeStartWidth = stage.getWidth();
                resizeStartHeight = stage.getHeight();
            } else {
                isResizing = false;
                xOffset = event.getSceneX();
                yOffset = event.getSceneY();
            }
        });
        
        rootPane.setOnMouseDragged(event -> {
            Stage stage = (Stage) rootPane.getScene().getWindow();
            
            if (isResizing) {
                handleResize(stage, event.getScreenX(), event.getScreenY());
            } else {
                // Normal pencere sürükleme
                double newX = event.getScreenX() - xOffset;
                double newY = event.getScreenY() - yOffset;
                
                stage.setX(newX);
                stage.setY(newY);
                
                // Sürükleme sırasında konumu gerçek zamanlı kaydet
                saveWindowState(stage);
            }
        });
        
        rootPane.setOnMouseMoved(event -> {
            // Pozisyona göre cursor'u güncelle
            Cursor cursor = getCursorForPosition(event.getSceneX(), event.getSceneY(), 
                                               rootPane.getWidth(), rootPane.getHeight(), RESIZE_BORDER);
            rootPane.setCursor(cursor);
        });
        
        rootPane.setOnMouseReleased(event -> {
            isResizing = false;
            resizeDirection = ResizeDirection.NONE;
            
            // Son konumu kaydet
            Stage stage = (Stage) rootPane.getScene().getWindow();
            saveWindowState(stage);
        });
    }
    
    /**
     * Yeniden boyutlandırma olmadan temel pencere sürükleme kurar.
     * Yeniden boyutlandırılmaması gereken görünümler için kullanın.
     */
    public void setupBasicWindowDrag(Pane rootPane) {
        if (rootPane == null) return;

        rootPane.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        
        rootPane.setOnMouseDragged(event -> {
            Stage stage = (Stage) rootPane.getScene().getWindow();
            
            double newX = event.getScreenX() - xOffset;
            double newY = event.getScreenY() - yOffset;
            
            stage.setX(newX);
            stage.setY(newY);
            
            // Sürükleme sırasında konumu gerçek zamanlı kaydet
            saveWindowState(stage);
        });
        
        rootPane.setOnMouseReleased(event -> {
            // Son konumu kaydet
            Stage stage = (Stage) rootPane.getScene().getWindow();
            saveWindowState(stage);
        });
    }
    
    private ResizeDirection getResizeDirection(double x, double y, double width, double height, int border) {
        boolean isNorth = y <= border;
        boolean isSouth = y >= height - border;
        boolean isEast = x >= width - border;
        boolean isWest = x <= border;
        
        if (isNorth && isWest) return ResizeDirection.NW;
        if (isNorth && isEast) return ResizeDirection.NE;
        if (isSouth && isWest) return ResizeDirection.SW;
        if (isSouth && isEast) return ResizeDirection.SE;
        if (isNorth) return ResizeDirection.N;
        if (isSouth) return ResizeDirection.S;
        if (isEast) return ResizeDirection.E;
        if (isWest) return ResizeDirection.W;
        
        return ResizeDirection.NONE;
    }
    
    private Cursor getCursorForPosition(double x, double y, double width, double height, int border) {
        ResizeDirection direction = getResizeDirection(x, y, width, height, border);
        
        switch (direction) {
            case N:
            case S:
                return Cursor.N_RESIZE;
            case E:
            case W:
                return Cursor.E_RESIZE;
            case NE:
            case SW:
                return Cursor.NE_RESIZE;
            case NW:
            case SE:
                return Cursor.NW_RESIZE;
            default:
                return Cursor.DEFAULT;
        }
    }
    
    private void handleResize(Stage stage, double mouseX, double mouseY) {
        double deltaX = mouseX - (resizeStartX + resizeStartWidth);
        double deltaY = mouseY - (resizeStartY + resizeStartHeight);
        
        double newX = resizeStartX;
        double newY = resizeStartY;
        double newWidth = resizeStartWidth;
        double newHeight = resizeStartHeight;
        
        // Minimum pencere boyutu
        final double MIN_WIDTH = 800;
        final double MIN_HEIGHT = 600;
        
        switch (resizeDirection) {
            case N:
                newY = mouseY;
                newHeight = resizeStartHeight + (resizeStartY - mouseY);
                break;
            case S:
                newHeight = resizeStartHeight + deltaY;
                break;
            case E:
                newWidth = resizeStartWidth + deltaX;
                break;
            case W:
                newX = mouseX;
                newWidth = resizeStartWidth + (resizeStartX - mouseX);
                break;
            case NE:
                newY = mouseY;
                newHeight = resizeStartHeight + (resizeStartY - mouseY);
                newWidth = resizeStartWidth + deltaX;
                break;
            case NW:
                newX = mouseX;
                newY = mouseY;
                newWidth = resizeStartWidth + (resizeStartX - mouseX);
                newHeight = resizeStartHeight + (resizeStartY - mouseY);
                break;
            case SE:
                newWidth = resizeStartWidth + deltaX;
                newHeight = resizeStartHeight + deltaY;
                break;
            case SW:
                newX = mouseX;
                newWidth = resizeStartWidth + (resizeStartX - mouseX);
                newHeight = resizeStartHeight + deltaY;
                break;
            default:
                return;
        }
        
        // Minimum boyut kısıtlamalarını uygula
        if (newWidth < MIN_WIDTH) {
            if (resizeDirection == ResizeDirection.W || resizeDirection == ResizeDirection.NW || resizeDirection == ResizeDirection.SW) {
                newX = newX - (MIN_WIDTH - newWidth);
            }
            newWidth = MIN_WIDTH;
        }
        
        if (newHeight < MIN_HEIGHT) {
            if (resizeDirection == ResizeDirection.N || resizeDirection == ResizeDirection.NE || resizeDirection == ResizeDirection.NW) {
                newY = newY - (MIN_HEIGHT - newHeight);
            }
            newHeight = MIN_HEIGHT;
        }
        
        // Yeni boyut ve konumu uygula
        stage.setX(newX);
        stage.setY(newY);
        stage.setWidth(newWidth);
        stage.setHeight(newHeight);
        
        // Yeniden boyutlandırma sırasında durumu kaydet
        saveWindowState(stage);
    }
}