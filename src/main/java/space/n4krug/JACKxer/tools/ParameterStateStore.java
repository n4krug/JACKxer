package space.n4krug.JACKxer.tools;

import space.n4krug.JACKxer.control.ControlParameter;
import space.n4krug.JACKxer.control.ParameterRegistry;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Persists a subset of {@link ControlParameter} values between launches.
 * <p>
 * Values are stored as normalized floats in a {@link Properties} file under the user's
 * config directory.
 */
public final class ParameterStateStore {

    private static final String APP_DIR_NAME = "JACKxer";
    private static final String FILE_PREFIX = "state-";
    private static final String FILE_SUFFIX = ".properties";

    private static final String META_VERSION = "__version";
    private static final String META_SAVED_AT = "__savedAtEpochMillis";

    private static final int VERSION = 1;

    private static final Pattern EQ_KNOB_PATTERN =
            Pattern.compile("^(.*)\\.band(\\d+)\\.(freq|gain|q|type)$");

    private static final Pattern COMP_KNOB_PATTERN =
            Pattern.compile("^(.*)\\.(threshold|ratio|attack|release|makeup)$");

    private ParameterStateStore() {}

    public static void loadAndApply(String configName, ParameterRegistry params) {
        loadAndApply(resolveDefaultBaseDir(), configName, params);
    }

    public static void save(String configName, ParameterRegistry params) {
        save(resolveDefaultBaseDir(), configName, params);
    }

    static void loadAndApply(Path baseDir, String configName, ParameterRegistry params) {
        Path path = resolveStatePath(baseDir, configName);
        if (!Files.exists(path)) {
            return;
        }

        Properties p = new Properties();
        try (InputStream in = new BufferedInputStream(Files.newInputStream(path))) {
            p.load(in);
        } catch (Exception e) {
            System.err.println("Failed to load state from " + path + ": " + e.getMessage());
            return;
        }

        Map<String, ControlParameter<?>> snapshot = params.snapshot();
        Set<String> persistableClientIds = persistableClientIds(snapshot);

        for (String key : p.stringPropertyNames()) {
            if (key.startsWith("__")) {
                continue;
            }

            if (!isAllowedKey(key, persistableClientIds)) {
                continue;
            }

            ControlParameter<?> param = snapshot.get(key);
            if (param == null) {
                continue;
            }

            float value;
            try {
                value = Float.parseFloat(p.getProperty(key));
            } catch (Exception ignored) {
                continue;
            }

            param.setNormalized(value);
        }
    }

    static void save(Path baseDir, String configName, ParameterRegistry params) {
        Map<String, ControlParameter<?>> snapshot = params.snapshot();
        Set<String> persistableClientIds = persistableClientIds(snapshot);

        Properties out = new Properties();
        out.setProperty(META_VERSION, Integer.toString(VERSION));
        out.setProperty(META_SAVED_AT, Long.toString(Instant.now().toEpochMilli()));

        for (Map.Entry<String, ControlParameter<?>> e : snapshot.entrySet()) {
            String id = e.getKey();
            ControlParameter<?> param = e.getValue();
            if (param == null) {
                continue;
            }

            if (!isAllowedKey(id, persistableClientIds)) {
                continue;
            }

            out.setProperty(id, Float.toString(param.getNormalized()));
        }

        Path path = resolveStatePath(baseDir, configName);
        try {
            Files.createDirectories(path.getParent());
        } catch (IOException e) {
            System.err.println("Failed to create config dir " + path.getParent() + ": " + e.getMessage());
            return;
        }

        Path tmp = path.resolveSibling(path.getFileName().toString() + ".tmp");
        try (OutputStream os = new BufferedOutputStream(Files.newOutputStream(tmp))) {
            out.store(os, "JACKxer state (auto-generated)");
        } catch (Exception e) {
            System.err.println("Failed to write state to " + tmp + ": " + e.getMessage());
            return;
        }

        try {
            Files.move(tmp, path,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException atomicMoveFailed) {
            try {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException moveFailed) {
                System.err.println("Failed to replace state file " + path + ": " + moveFailed.getMessage());
            }
        }
    }

    private static boolean isAllowedKey(String id, Set<String> persistableClientIds) {
        if (id.endsWith(".gain") || id.endsWith(".on")) {
            return false;
        }

        if (isPersistedKnobId(id)) {
            return true;
        }

        if (id.endsWith(".bypass")) {
            String clientId = id.substring(0, id.length() - ".bypass".length());
            return persistableClientIds.contains(clientId);
        }

        return false;
    }

    private static boolean isPersistedKnobId(String id) {
        return EQ_KNOB_PATTERN.matcher(id).matches() || COMP_KNOB_PATTERN.matcher(id).matches();
    }

    private static Set<String> persistableClientIds(Map<String, ControlParameter<?>> snapshot) {
        Set<String> out = new HashSet<>();
        for (String id : snapshot.keySet()) {
            String clientId = clientIdForKnobId(id);
            if (clientId != null) {
                out.add(clientId);
            }
        }
        return out;
    }

    private static String clientIdForKnobId(String id) {
        Matcher comp = COMP_KNOB_PATTERN.matcher(id);
        if (comp.matches()) {
            return comp.group(1);
        }

        Matcher eq = EQ_KNOB_PATTERN.matcher(id);
        if (eq.matches()) {
            return eq.group(1);
        }

        return null;
    }

    private static Path resolveDefaultBaseDir() {
        String xdg = System.getenv("XDG_CONFIG_HOME");
        if (xdg != null && !xdg.isBlank()) {
            return Path.of(xdg);
        }
        String home = System.getenv("HOME");
        if (home == null || home.isBlank()) {
            // Last-resort fallback.
            return Path.of(".").toAbsolutePath().normalize();
        }
        return Path.of(home, ".config");
    }

    private static Path resolveStatePath(Path baseDir, String configName) {
        String safe = sanitizeConfigName(configName);
        return baseDir.resolve(APP_DIR_NAME).resolve(FILE_PREFIX + safe + FILE_SUFFIX);
    }

    private static String sanitizeConfigName(String configName) {
        StringBuilder sb = new StringBuilder(configName.length());
        for (int i = 0; i < configName.length(); i++) {
            char c = configName.charAt(i);
            boolean ok =
                    (c >= 'a' && c <= 'z') ||
                    (c >= 'A' && c <= 'Z') ||
                    (c >= '0' && c <= '9') ||
                    c == '.' || c == '_' || c == '-';
            sb.append(ok ? c : '_');
        }
        return sb.toString();
    }
}

