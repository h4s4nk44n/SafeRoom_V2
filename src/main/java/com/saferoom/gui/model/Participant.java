package com.saferoom.gui.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Participant {
    private final StringProperty name;
    private final BooleanProperty cameraOn;
    private final BooleanProperty muted;
    private final UserRole role;

    public Participant(String name, UserRole role, boolean cameraOn, boolean muted) {
        this.name = new SimpleStringProperty(name);
        this.role = role;
        this.cameraOn = new SimpleBooleanProperty(cameraOn);
        this.muted = new SimpleBooleanProperty(muted);
    }

    public UserRole getRole() {
        return role;
    }

    public String getName() { return name.get(); }
    public StringProperty nameProperty() { return name; }
    public boolean isCameraOn() { return cameraOn.get(); }
    public BooleanProperty cameraOnProperty() { return cameraOn; }
    public boolean isMuted() { return muted.get(); }
    public BooleanProperty mutedProperty() { return muted; }

    // ## ADDED METHODS ##
    /**
     * Updates the participant's camera state.
     */
    public void setCameraOn(boolean isOn) {
        this.cameraOn.set(isOn);
    }

    /**
     * Updates the participant's muted state.
     */
    public void setMuted(boolean isMuted) {
        this.muted.set(isMuted);
    }
}