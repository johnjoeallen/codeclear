package com.codeclear;

import com.codeclear.cli.CCCliMain;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CodeClearApplication {

    /** {@code review ...} runs the command-line review and exits without starting Spring; anything else starts the web application. */
    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("review")) {
            System.exit(CCCliMain.run(args));
        }
        SpringApplication.run(CodeClearApplication.class, args);
    }
}
