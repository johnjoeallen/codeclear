package com.codeclear.rules;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Git for Windows may check files out with CRLF; a rule must hash, parse and run the same either way. */
class CCRuleLoaderLineEndingsTest {

    private static void copyWithCrlf(Path from, Path to) throws IOException {
        Files.createDirectories(to);
        try (Stream<Path> files = Files.list(from)) {
            for (Path f : files.filter(Files::isRegularFile).toList()) {
                String text = Files.readString(f).replace("\r\n", "\n").replace("\n", "\r\n");
                Files.writeString(to.resolve(f.getFileName()), text);
            }
        }
    }

    @Test
    void aRuleCheckedOutWithCrlfHashesAndLoadsLikeTheLfOriginal(@TempDir Path tmp) throws Exception {
        Path original = Path.of("review/java/interrupt-flag-not-restored");
        Path crlf = tmp.resolve("java/interrupt-flag-not-restored");
        copyWithCrlf(original, crlf);

        CCRuleDefinition lf = CCRuleLoader.load(original);
        CCRuleDefinition windows = CCRuleLoader.load(crlf);

        assertThat(Files.readString(crlf.resolve("rule.md"))).contains("\r\n"); // the copy really is CRLF
        assertThat(windows.ruleHash()).isEqualTo(lf.ruleHash());
        assertThat(windows.stale()).isFalse();
        assertThat(windows.metadata()).isEqualTo(lf.metadata());
        assertThat(windows.aiPrompt()).isEqualTo(lf.aiPrompt());
        assertThat(windows.dsl()).isEqualTo(lf.dsl());
        assertThat(windows.dsl().values()).noneMatch(source -> source.contains("\r"));
    }

    @Test
    void testsWrittenWithCrlfParseTheSameExpectations(@TempDir Path tmp) throws Exception {
        Path original = Path.of("review/java/interrupt-flag-not-restored");
        Path crlf = tmp.resolve("java/interrupt-flag-not-restored");
        copyWithCrlf(original, crlf);

        var lf = CCRuleTests.load(original);
        var windows = CCRuleTests.load(crlf);

        assertThat(windows.cases()).hasSameSizeAs(lf.cases());
        assertThat(windows.cases().get(0).files().get(0).source()).isEqualTo(lf.cases().get(0).files().get(0).source());
        assertThat(windows.cases()).extracting(c -> c.expect().size()).isEqualTo(lf.cases().stream().map(c -> c.expect().size()).toList());
    }
}
