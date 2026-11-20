package com.codeclear.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ConfigFileAnalyzerTest {

    private static CCConfigModel read(String path, String text) {
        return new ConfigFileAnalyzer().analyze(path, text, CCChangedLines.all()).config();
    }

    @Test
    void yamlBecomesKeyPathsWithLinesInKebabCase() {
        CCConfigModel m = read("application.yml", "server:\n  sslEnabled: false\n  ports:\n    - 80\n    - 443\nname:\n");

        assertThat(m.settings()).extracting(CCConfigModel.Setting::key)
                .containsExactly("server.ssl-enabled", "server.ports[0]", "server.ports[1]", "name");
        assertThat(m.settings()).extracting(CCConfigModel.Setting::line).containsExactly(2, 4, 5, 6);
        assertThat(m.settings().get(0).rawKey()).isEqualTo("server.sslEnabled");
        assertThat(m.settings().get(3).value()).isEmpty();
    }

    @Test
    void jsonIsReadAsYaml() {
        CCConfigModel m = read("a.json", "{\n  \"a\": { \"b\": 1 },\n  \"list\": [\"x\"]\n}");

        assertThat(m.settings()).extracting(CCConfigModel.Setting::key).containsExactly("a.b", "list[0]");
        assertThat(m.settings()).extracting(CCConfigModel.Setting::line).containsExactly(2, 3);
    }

    @Test
    void propertiesHandleCommentsSeparatorsAndContinuations() {
        CCConfigModel m = read("a.properties", "# c\n! c\na.b=1\nc.d: two\ne.f three\ng=one\\\n  two\n\nempty=\n");

        assertThat(m.settings()).extracting(CCConfigModel.Setting::key).containsExactly("a.b", "c.d", "e.f", "g", "empty");
        assertThat(m.settings()).extracting(CCConfigModel.Setting::value).containsExactly("1", "two", "three", "onetwo", "");
        assertThat(m.settings()).extracting(CCConfigModel.Setting::line).containsExactly(3, 4, 5, 6, 9);
    }

    @Test
    void theProfileComesFromTheFileNameOrTheDocument() {
        assertThat(read("src/main/resources/application-prod.yml", "a: 1").settings().get(0).profile()).isEqualTo("prod");
        assertThat(read("config/my-service.yml", "a: 1").settings().get(0).profile()).isEmpty();
        CCConfigModel m = read("application.yml", "a: 1\n---\nspring.config.activate.on-profile: dev\nb: 2\n");
        assertThat(m.settings()).extracting(CCConfigModel.Setting::profile).containsExactly("", "dev", "dev");
    }

    @Test
    void aFileThatIsNotYamlIsRejected() {
        assertThatThrownBy(() -> read("a.yml", "a: [1, 2\nb: }")).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot parse a.yml");
    }
}
