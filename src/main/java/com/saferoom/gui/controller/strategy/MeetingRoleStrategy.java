package com.saferoom.gui.controller.strategy;

import com.saferoom.gui.model.Participant;
import javafx.scene.control.MenuItem;
import java.util.List;

public interface MeetingRoleStrategy {
    List<MenuItem> createParticipantMenuItems(Participant currentUser, Participant targetParticipant, Runnable onStateChanged);
}