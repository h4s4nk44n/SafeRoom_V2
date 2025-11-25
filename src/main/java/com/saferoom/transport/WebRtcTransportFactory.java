package com.saferoom.transport;

import com.saferoom.p2p.DataChannelWrapper;

import dev.onvoid.webrtc.RTCDataChannel;

public final class WebRtcTransportFactory implements TransportFactory {
    @Override
    public String getId() {
        return "webrtc";
    }

    @Override
    public boolean supports(Object rawTransport) {
        return rawTransport instanceof RTCDataChannel;
    }

    @Override
    public TransportEndpoint create(Object rawTransport, TransportContext context) {
        if (!(rawTransport instanceof RTCDataChannel channel)) {
            throw new IllegalArgumentException("WebRTC transport requires RTCDataChannel");
        }
        return new DataChannelWrapper(channel, context.localUser(), context.remoteUser());
    }
}

