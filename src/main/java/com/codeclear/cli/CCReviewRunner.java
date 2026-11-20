package com.codeclear.cli;

import com.codeclear.review.CCReviewService;
import java.nio.file.Path;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Runs the {@code review} command when the application is started in command-line mode (see {@code CodeClearApplication}). */
@Component
@ConditionalOnProperty(name = "codeclear.cli", havingValue = "true")
public class CCReviewRunner implements ApplicationRunner, ExitCodeGenerator {

    private final CCReviewService service;
    private int exitCode;

    public CCReviewRunner(CCReviewService service) {
        this.service = service;
    }

    @Override
    public void run(ApplicationArguments args) {
        exitCode = new CCReviewCli(service).run(args.getSourceArgs(), Path.of("").toAbsolutePath(), System.out, System.err);
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }
}
