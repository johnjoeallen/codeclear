package com.codeclear;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CodeClearApplication {

    /** {@code review ...} runs the command-line review and exits; anything else starts the web application. */
    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("review")) {
            SpringApplication app = new SpringApplication(CodeClearApplication.class);
            app.setWebApplicationType(WebApplicationType.NONE);
            app.setBannerMode(Banner.Mode.OFF);
            app.setLogStartupInfo(false);
            app.setDefaultProperties(Map.of("codeclear.cli", "true", "logging.level.root", "WARN"));
            System.exit(SpringApplication.exit(app.run(cliArguments(args))));
        }
        SpringApplication.run(CodeClearApplication.class, args);
    }

    /** {@code --rules=<dir>} is the command's name for the {@code codeclear.rules-dir} setting. */
    static String[] cliArguments(String[] args) {
        List<String> out = new ArrayList<>();
        for (String a : args) {
            out.add(a.startsWith("--rules=") ? "--codeclear.rules-dir=" + a.substring("--rules=".length()) : a);
        }
        return out.toArray(String[]::new);
    }
}
