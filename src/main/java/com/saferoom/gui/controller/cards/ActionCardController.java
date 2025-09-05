package com.saferoom.gui.controller.cards;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.kordamp.ikonli.javafx.FontIcon;

public class ActionCardController {

    @FXML
    private FontIcon icon;

    @FXML
    private Label titleLabel;

    @FXML
    private Label descriptionLabel;

    /**
     * Bu metod, DashboardController tarafından çağrılarak
     * her bir kartın ikonunu, başlığını ve açıklamasını ayarlar.
     * @param iconLiteral FontAwesome ikon kodu (örn: "fas-plus")
     * @param title Kartın başlığı
     * @param description Kartın açıklaması
     */
    public void setData(String iconLiteral, String title, String description) {
        icon.setIconLiteral(iconLiteral);
        titleLabel.setText(title);
        descriptionLabel.setText(description);
    }
}
