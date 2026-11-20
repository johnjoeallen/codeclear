package com.codeclear.rules;

import com.github.javaparser.ast.CompilationUnit;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;

/**
 * Reads a {@code .yml}, {@code .yaml}, {@code .json} or {@code .properties} file into the {@link CCConfigModel} the rules
 * receive: every scalar becomes a setting with its key path and line. JSON is read as YAML, which it is a subset of.
 */
public final class ConfigFileAnalyzer {

    private static final Pattern PROFILE_FILE = Pattern.compile("(?:^|/)(?:application|bootstrap)-([A-Za-z0-9_]+)\\.[A-Za-z]+$");

    /** @throws IllegalArgumentException when a YAML or JSON file does not parse; the caller skips the file */
    public CCFileAnalysis analyze(String path, String source, CCChangedLines changedLines) {
        String text = source.replace("\r\n", "\n").replace('\r', '\n');
        String fileProfile = profileOfFile(path);
        List<List<Raw>> documents = path.toLowerCase(Locale.ROOT).endsWith(".properties")
                ? List.of(properties(text)) : yaml(path, text);
        List<CCConfigModel.Setting> out = new ArrayList<>();
        for (List<Raw> doc : documents) {
            Map<String, String> byKey = new LinkedHashMap<>();
            doc.forEach(r -> byKey.putIfAbsent(kebab(r.key), r.value));
            String profile = fileProfile;
            for (String k : List.of("spring.config.activate.on-profile", "spring.profiles")) {
                if (byKey.containsKey(k) && !byKey.get(k).isEmpty()) {
                    profile = byKey.get(k);
                }
            }
            Map<String, String> frozen = Map.copyOf(byKey);
            for (Raw r : doc) {
                out.add(new CCConfigModel.Setting(r.line, kebab(r.key), r.key, r.value, profile, frozen));
            }
        }
        return new CCFileAnalysis(new CCFileInfo(path, language(path)), new CompilationUnit(), null, changedLines, null,
                new CCConfigModel(List.copyOf(out)));
    }

    private static String language(String path) {
        String p = path.toLowerCase(Locale.ROOT);
        return p.endsWith(".properties") ? "properties" : p.endsWith(".json") ? "json" : "yaml";
    }

    private static String profileOfFile(String path) {
        Matcher m = PROFILE_FILE.matcher(path);
        return m.find() ? m.group(1) : "";
    }

    /** {@code trustAllCertificates} to {@code trust-all-certificates}; {@code _} to {@code -}; lower case. List indexes stay. */
    static String kebab(String key) {
        return key.replaceAll("([a-z0-9])([A-Z])", "$1-$2").replace('_', '-').toLowerCase(Locale.ROOT);
    }

    private record Raw(int line, String key, String value) { }

    // ---- YAML and JSON ---------------------------------------------------------------------------------

    private static List<List<Raw>> yaml(String path, String text) {
        List<List<Raw>> docs = new ArrayList<>();
        try {
            LoaderOptions options = new LoaderOptions();
            options.setProcessComments(false);
            for (Node root : new Yaml(options).composeAll(new StringReader(text))) {
                List<Raw> doc = new ArrayList<>();
                if (root != null) {
                    walk(root, "", doc);
                }
                docs.add(doc);
            }
        } catch (YAMLException e) {
            throw new IllegalArgumentException("cannot parse " + path + ": " + e.getMessage().split("\\R", 2)[0]);
        }
        return docs;
    }

    private static void walk(Node node, String prefix, List<Raw> out) {
        if (node instanceof MappingNode map) {
            for (NodeTuple t : map.getValue()) {
                if (t.getKeyNode() instanceof ScalarNode key) {
                    walk(t.getValueNode(), prefix.isEmpty() ? key.getValue() : prefix + "." + key.getValue(), out);
                }
            }
        } else if (node instanceof SequenceNode seq) {
            int i = 0;
            for (Node item : seq.getValue()) {
                walk(item, prefix + "[" + i++ + "]", out);
            }
        } else if (node instanceof ScalarNode s) {
            String v = s.getValue() == null || s.getTag().getValue().endsWith(":null") ? "" : s.getValue();
            out.add(new Raw(s.getStartMark().getLine() + 1, prefix, v));
        }
    }

    // ---- properties ------------------------------------------------------------------------------------

    private static List<Raw> properties(String text) {
        List<Raw> out = new ArrayList<>();
        String[] lines = text.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].stripLeading();
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("!")) {
                continue;
            }
            int start = i + 1;
            StringBuilder full = new StringBuilder(line);
            while (endsWithContinuation(full) && i + 1 < lines.length) {
                full.setLength(full.length() - 1);
                full.append(lines[++i].stripLeading());
            }
            Matcher m = Pattern.compile("^((?:[^=:\\s\\\\]|\\\\.)+)\\s*(?:[=:]\\s*|\\s+)(.*)$").matcher(full);
            if (m.matches()) {
                out.add(new Raw(start, m.group(1).replace("\\", ""), m.group(2).strip()));
            } else {
                out.add(new Raw(start, full.toString().strip(), ""));
            }
        }
        return out;
    }

    private static boolean endsWithContinuation(CharSequence s) {
        int slashes = 0;
        for (int i = s.length() - 1; i >= 0 && s.charAt(i) == '\\'; i--) {
            slashes++;
        }
        return slashes % 2 == 1;
    }
}
