package com.codeclear.ai;

/** Readiness of a driver, surfaced by {@code GET /api/config/ai}. */
public record CCAiDriverStatus(boolean ready, String detail) {

    public static CCAiDriverStatus ok() {
        return new CCAiDriverStatus(true, "ready");
    }

    public static CCAiDriverStatus notReady(String detail) {
        return new CCAiDriverStatus(false, detail);
    }
}
