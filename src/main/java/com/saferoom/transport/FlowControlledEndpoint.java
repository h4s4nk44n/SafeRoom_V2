package com.saferoom.transport;

public interface FlowControlledEndpoint extends TransportEndpoint {
    long bufferedAmount();

    default long droppedPackets() {
        return -1;
    }
}

