package com.codeclear.ai;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

/** Collects every {@link CCAiDriver} bean and resolves one by id. */
@Component
public class CCAiDriverRegistry {

    private final Map<String, CCAiDriver> drivers;

    public CCAiDriverRegistry(List<CCAiDriver> drivers) {
        this.drivers = drivers.stream().collect(Collectors.toUnmodifiableMap(CCAiDriver::id, Function.identity()));
    }

    /** The driver with this id, or empty when none is registered (for example a driver not yet implemented). */
    public java.util.Optional<CCAiDriver> find(String id) {
        return java.util.Optional.ofNullable(drivers.get(id));
    }

    public java.util.Set<String> ids() {
        return drivers.keySet();
    }

    public CCAiDriver get(String id) {
        CCAiDriver driver = drivers.get(id);
        if (driver == null) {
            throw new IllegalArgumentException("Unknown AI driver '" + id + "'; available: " + drivers.keySet());
        }
        return driver;
    }

    public Map<String, CCAiDriverStatus> statuses() {
        return drivers.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, e -> e.getValue().status()));
    }
}
