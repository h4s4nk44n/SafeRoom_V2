package com.saferoom.gui.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

/**
 * Represents a viewer's request to access a live session.
 * 
 * When a non-whitelisted user clicks the Eye icon, an AccessRequest is created
 * and sent to the Host. The Host sees a popup: "[User_Name] wants to watch.
 * [Allow] / [Ignore]"
 * 
 * This object is transient and purged after the session ends
 * (zero-persistence).
 */
public class AccessRequest {

    private final String requestId;
    private final String sessionId;
    private final String requesterId;
    private final String requesterName;
    private final LocalDateTime requestTime;
    private final ObjectProperty<RequestStatus> status;

    /**
     * Creates a new access request.
     * 
     * @param sessionId     ID of the session being requested
     * @param requesterId   User ID of the requester
     * @param requesterName Display name of the requester
     */
    public AccessRequest(String sessionId, String requesterId, String requesterName) {
        this.requestId = UUID.randomUUID().toString();
        this.sessionId = Objects.requireNonNull(sessionId, "sessionId cannot be null");
        this.requesterId = Objects.requireNonNull(requesterId, "requesterId cannot be null");
        this.requesterName = Objects.requireNonNull(requesterName, "requesterName cannot be null");
        this.requestTime = LocalDateTime.now();
        this.status = new SimpleObjectProperty<>(RequestStatus.PENDING);
    }

    // ==================== Getters ====================

    public String getRequestId() {
        return requestId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getRequesterId() {
        return requesterId;
    }

    public String getRequesterName() {
        return requesterName;
    }

    public LocalDateTime getRequestTime() {
        return requestTime;
    }

    // ==================== Status Property ====================

    public RequestStatus getStatus() {
        return status.get();
    }

    public void setStatus(RequestStatus newStatus) {
        status.set(newStatus);
    }

    public ObjectProperty<RequestStatus> statusProperty() {
        return status;
    }

    // ==================== Status Helpers ====================

    public boolean isPending() {
        return status.get() == RequestStatus.PENDING;
    }

    public boolean isApproved() {
        return status.get() == RequestStatus.APPROVED;
    }

    public boolean isDenied() {
        return status.get() == RequestStatus.DENIED;
    }

    /**
     * Approves the request, allowing the requester to join the session.
     */
    public void approve() {
        status.set(RequestStatus.APPROVED);
    }

    /**
     * Denies (ignores) the request.
     */
    public void deny() {
        status.set(RequestStatus.DENIED);
    }

    // ==================== Object Methods ====================

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof AccessRequest other))
            return false;
        return requestId.equals(other.requestId);
    }

    @Override
    public int hashCode() {
        return requestId.hashCode();
    }

    @Override
    public String toString() {
        return "AccessRequest{" +
                "requestId='" + requestId + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", requesterName='" + requesterName + '\'' +
                ", status=" + status.get() +
                '}';
    }
}
