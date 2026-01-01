package com.saferoom.gui.model;

/**
 * Status of a viewer's access request to join a live session.
 */
public enum RequestStatus {
    /**
     * Request has been sent and awaiting host response.
     */
    PENDING,

    /**
     * Host approved the request - viewer can now connect.
     */
    APPROVED,

    /**
     * Host denied (ignored) the request.
     */
    DENIED
}
