package com.saferoom.transport;

public interface TransportFactory {
    String getId();
    boolean supports(Object rawTransport);
    TransportEndpoint create(Object rawTransport, TransportContext context);
}

