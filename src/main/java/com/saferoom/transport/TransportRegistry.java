package com.saferoom.transport;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class TransportRegistry {
    private static final Map<String, TransportFactory> FACTORIES = new ConcurrentHashMap<>();

    static {
        register(new WebRtcTransportFactory());
    }

    private TransportRegistry() {}

    public static void register(TransportFactory factory) {
        FACTORIES.put(factory.getId(), factory);
    }

    public static TransportEndpoint create(String id, Object rawTransport, TransportContext context) {
        TransportFactory factory = FACTORIES.get(id);
        if (factory == null) {
            throw new IllegalArgumentException("Unknown transport factory: " + id);
        }
        if (!factory.supports(rawTransport)) {
            throw new IllegalArgumentException("Factory " + id + " does not support transport " + rawTransport);
        }
        return factory.create(rawTransport, context);
    }
}

