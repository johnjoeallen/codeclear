package com.codeclear.ai;

import java.time.Duration;
import java.util.OptionalInt;
import org.springframework.stereotype.Component;

/**
 * A driver for when no AI backend is integrated yet. It reports itself as not ready, so the review skips every check
 * that needs the AI and says so, and if it is called anyway it returns nothing.
 */
@Component
public class DummyAiDriver implements CCAiDriver {

    public static final String ID = "dummy";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public CCAiDriverStatus status() {
        return CCAiDriverStatus.notReady("dummy driver: no AI backend is integrated, so checks that need the AI are skipped");
    }

    @Override
    public CCAiResponse complete(CCAiRequest request) {
        return new CCAiResponse("", ID, OptionalInt.empty(), OptionalInt.empty(), Duration.ZERO);
    }
}
