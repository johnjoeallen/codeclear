package com.codeclear.cli;

import com.codeclear.ai.CCDriverCandidateConfirmer;
import com.codeclear.ai.DummyAiDriver;
import com.codeclear.review.CCReviewService;
import com.codeclear.rules.CCRuleEngine;
import com.codeclear.rules.CCRuleLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/**
 * Entry point of the {@code review} command. It loads the rules and reviews the target folder: no Spring context, no web
 * server, nothing started that the review does not need. (The AI driver is the dummy one until a real driver exists, so
 * the review never calls an AI.)
 */
public final class CCCliMain {

    private CCCliMain() { }

    /** @param args the full argument list, starting with {@code review} */
    public static int run(String[] args) {
        return new CCReviewCli(CCCliMain::service).run(args, Path.of("").toAbsolutePath(), System.out, System.err);
    }

    static CCReviewService service(Path rulesDir) {
        if (!Files.isDirectory(rulesDir)) {
            throw new IllegalArgumentException("rules folder not found: " + rulesDir + " (use --rules=<dir>)");
        }
        return new CCReviewService(CCRuleLoader.loadAll(rulesDir), new CCRuleEngine(),
                new CCDriverCandidateConfirmer(new DummyAiDriver(), Duration.ofSeconds(300)), DummyAiDriver.ID);
    }
}
