package com.saferoom.gui.model;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class Meeting {
    private String meetingId;
    private String meetingName;
    private ObservableList<Participant> participants;

    public Meeting(String meetingId, String meetingName) {
        this.meetingId = meetingId;
        this.meetingName = meetingName;
        this.participants = FXCollections.observableArrayList();
    }

    public void addDummyParticipants() {
        participants.add(new Participant("Alice", UserRole.USER, true, false));
        participants.add(new Participant("Bob", UserRole.USER, false, true));
        participants.add(new Participant("Charlie", UserRole.USER, false, false));
    }

    public String getMeetingId() { return meetingId; }
    public String getMeetingName() { return meetingName; }
    public ObservableList<Participant> getParticipants() { return participants; }
}
