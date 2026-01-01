package com.saferoom.gui.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a Hub (formerly Server/Community) in the SafeRoom ecosystem.
 * A Hub is a collection of members who share access to rooms and channels.
 */
public class Hub {
    private String id;
    private String name;
    private String iconLiteral;
    private boolean isPrivate;
    private List<User> members;

    public Hub(String id, String name, String iconLiteral, boolean isPrivate) {
        this.id = id;
        this.name = name;
        this.iconLiteral = iconLiteral;
        this.isPrivate = isPrivate;
        this.members = Collections.synchronizedList(new ArrayList<>());
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getIconLiteral() {
        return iconLiteral;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public List<User> getMembers() {
        return new ArrayList<>(members); // Return copy to prevent modification issues
    }

    public void addMember(User user) {
        if (!members.contains(user)) {
            members.add(user);
        }
    }

    public void removeMember(User user) {
        members.remove(user);
    }

    // Mock method to seed with dummy data
    public static Hub createMockHub(String id, String name) {
        Hub hub = new Hub(id, name, "fas-server", false);
        hub.addMember(new User("user1", "Alice"));
        hub.addMember(new User("user2", "Bob"));
        hub.addMember(new User("user3", "Charlie"));
        hub.addMember(new User("user4", "Dave"));
        // Add current user (mock)
        hub.addMember(new User("me", "Commander"));
        return hub;
    }
}
