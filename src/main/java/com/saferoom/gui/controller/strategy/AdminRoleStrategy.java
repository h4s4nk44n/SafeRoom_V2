package com.saferoom.gui.controller.strategy;

import com.saferoom.gui.model.Participant;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AdminRoleStrategy implements MeetingRoleStrategy {

    @Override
    public List<MenuItem> createParticipantMenuItems(Participant currentUser, Participant targetParticipant, Runnable onStateChanged) {
        if (currentUser.equals(targetParticipant)) {
            return Collections.emptyList();
        }

        List<MenuItem> menuItems = new ArrayList<>();

        if (!targetParticipant.isMuted()) {
            MenuItem muteMicItem = new MenuItem("Mute Microphone");
            muteMicItem.setOnAction(e -> {
                targetParticipant.setMuted(true);
                onStateChanged.run();
            });
            menuItems.add(muteMicItem);
        }

        if (targetParticipant.isCameraOn()) {
            MenuItem stopCamItem = new MenuItem("Stop Camera");
            stopCamItem.setOnAction(e -> {
                targetParticipant.setCameraOn(false);
                onStateChanged.run();
            });
            menuItems.add(stopCamItem);
        }

        if (!menuItems.isEmpty()) {
            menuItems.add(new SeparatorMenuItem());
        }

        MenuItem kickItem = new MenuItem("Kick from Meeting");
        kickItem.getStyleClass().add("menu-item-danger");
        kickItem.setOnAction(e -> {
            System.out.println("ADMIN ACTION: Kicked " + targetParticipant.getName());
        });
        menuItems.add(kickItem);

        return menuItems;
    }
}