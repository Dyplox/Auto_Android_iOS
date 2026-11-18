package com.reto.qa.config;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Immutable configuration for one platform and one environment.
 *
 * <p><b>Environment</b> ({@code local}, {@code ci}, {@code browserstack}, {@code saucelabs}…): chosen with the
 * {@code test.env} system property (in Gradle: {@code -Penv=…}), the {@code TEST_ENV} variable or the {@code .env}
 * file; {@code local} by default.</p>
 *
 * <p><b>Precedence</b> for a key {@code k} on platform {@code p} (the first match wins):</p>
 * <ol>
 *     <li>System property {@code p.k} (Gradle: {@code -Pios.device.name="iPhone 17"})</li>
 *     <li>System property {@code k}</li>
 *     <li>Environment variable {@code P_K}, then {@code K} (also read from the {@code .env} file)</li>
 *     <li>{@code config/env/<environment>-<platform>.properties}</li>
 *     <li>{@code config/env/<environment>.properties}</li>
 *     <li>{@code config/<platform>.properties}</li>
 *     <li>{@code config/framework.properties}</li>
 * </ol>
 *
 * <p><b>Secrets</b>: values support {@code ${VARIABLE}} and {@code ${VARIABLE:-default}}, resolved from system
 * properties, environment variables or {@code .env}, so credentials are never committed. Resolution is lazy: a local
 * run does not require cloud credentials.</p>
 */
public final class FrameworkConfig {

    static final String COMMON_FILE = "config/framework.properties";
    private static final String PROPERTIES_EXTENSION = ".properties";
    static final String ENVIRONMENT_PROPERTY = "test.env";
    static final String ENVIRONMENT_VARIABLE = "TEST_ENV";
    static final String DEFAULT_ENVIRONMENT = "local";

    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([A-Za-z0-9_.]+)(?::-([^}]*))?}");
    private static final Map<Platform, FrameworkConfig> CACHE = new ConcurrentHashMap<>();

    private final Platform platform;
    private final String environment;
    /** Configuration files, from highest to lowest priority. */
    private final List<Properties> fileLayers;
    private final Map<String, String> environmentVariables;
    private final Properties systemProperties;

    private FrameworkConfig(Platform platform,
            String environment,
            List<Properties> fileLayers,
            Map<String, String> environmentVariables,
            Properties systemProperties) {
        this.platform = Objects.requireNonNull(platform, "platform");
        this.environment = Objects.requireNonNull(environment, "environment");
        this.fileLayers = fileLayers.stream().map(FrameworkConfig::copyOf).toList();
        this.environmentVariables = Map.copyOf(environmentVariables);
        this.systemProperties = copyOf(systemProperties);
    }

    /**
     * Returns the real configuration: classpath files + {@code .env} + environment variables + system properties.
     * The result is cached per platform.
     *
     * @param platform target platform
     * @return the configuration for the active environment
     * @throws ConfigurationException if the environment or a required file does not exist
     */
    public static FrameworkConfig forPlatform(Platform platform) {
        return CACHE.computeIfAbsent(platform, p -> {
            Map<String, String> variables = new HashMap<>(
                    loadDotEnv(Paths.get(System.getProperty("user.dir"), ".env")));
            variables.putAll(System.getenv());
            Properties system = System.getProperties();
            String environment = resolveEnvironment(system, variables);
            return new FrameworkConfig(p, environment, loadLayers(p, environment), variables, system);
        });
    }

    /**
     * Loads the classpath configuration files for a platform and environment.
     *
     * @param platform    target platform
     * @param environment environment name
     * @return the layers, from highest to lowest priority
     * @throws ConfigurationException if the environment file or a required file is missing
     */
    static List<Properties> loadLayers(Platform platform, String environment) {
        List<Properties> layers = new ArrayList<>();
        loadOptional("config/env/" + environment + "-" + platform.key() + PROPERTIES_EXTENSION).ifPresent(layers::add);
        layers.add(
                loadOptional("config/env/" + environment + PROPERTIES_EXTENSION)
                        .orElseThrow(() -> new ConfigurationException(
                                "Unknown environment '" + environment + "': src/test/resources/config/env/"
                                        + environment
                                        + ".properties is missing. Bundled environments: local, ci, browserstack, saucelabs")));
        layers.add(loadRequired("config/" + platform.key() + PROPERTIES_EXTENSION));
        layers.add(loadRequired(COMMON_FILE));
        return layers;
    }

    /**
     * Pure factory (no global state) for unit tests.
     *
     * @param platform             target platform
     * @param environment          environment name
     * @param fileLayers           configuration files, from highest to lowest priority
     * @param environmentVariables environment variables
     * @param systemProperties     system properties
     * @return the configuration
     */
    static FrameworkConfig of(Platform platform,
            String environment,
            List<Properties> fileLayers,
            Map<String, String> environmentVariables,
            Properties systemProperties) {
        return new FrameworkConfig(platform, environment, fileLayers, environmentVariables, systemProperties);
    }

    /**
     * Test shortcut: only the common and platform files, in the {@code local} environment.
     *
     * @param platform             target platform
     * @param common               contents of the common file
     * @param platformSpecific     contents of the platform file
     * @param environmentVariables environment variables
     * @param systemProperties     system properties
     * @return the configuration
     */
    static FrameworkConfig of(Platform platform,
            Properties common,
            Properties platformSpecific,
            Map<String, String> environmentVariables,
            Properties systemProperties) {
        return of(platform, DEFAULT_ENVIRONMENT, List.of(platformSpecific, common), environmentVariables,
                systemProperties);
    }

    /**
     * Returns the platform this configuration belongs to.
     *
     * @return the platform
     */
    public Platform platform() {
        return platform;
    }

    /**
     * Returns the name of the active environment (local, ci, browserstack, saucelabs…).
     *
     * @return the environment name
     */
    public String environment() {
        return environment;
    }

    /**
     * Looks up a key using the precedence described in the class Javadoc and resolves its placeholders.
     *
     * @param key property key, for example {@code device.name}
     * @return the trimmed value, or empty when no layer defines it
     * @throws ConfigurationException if a placeholder refers to an undefined variable
     */
    public Optional<String> find(String key) {
        String platformKey = platform.key() + "." + key;
        Stream<String> overrides = Stream.of(
                systemProperties.getProperty(platformKey),
                systemProperties.getProperty(key),
                environmentVariables.get(toEnvironmentName(platformKey)),
                environmentVariables.get(toEnvironmentName(key)));
        Stream<String> files = fileLayers.stream().map(layer -> layer.getProperty(key));
        return Stream.concat(overrides, files)
                .filter(FrameworkConfig::hasText)
                .map(String::trim)
                .findFirst()
                .map(value -> interpolate(key, value));
    }

    /**
     * Returns a required value.
     *
     * @param key property key
     * @return the value
     * @throws ConfigurationException if the key is not defined
     */
    public String get(String key) {
        return find(key).orElseThrow(() -> new ConfigurationException(
                "Missing required property '" + key + "' for " + platform.w3cName() + " in the '"
                        + environment + "' environment. Define it in config/" + platform.key()
                        + ".properties, in config/env/"
                        + environment + ".properties or with -P" + key + "=..."));
    }

    /**
     * Returns a required integer value.
     *
     * @param key property key
     * @return the parsed integer
     * @throws ConfigurationException if the key is missing or not an integer
     */
    public int getInt(String key) {
        String value = get(key);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new ConfigurationException("'" + key + "' must be an integer but is '" + value + "'", e);
        }
    }

    /**
     * Returns a required boolean value ({@code true} or {@code false}, case-insensitive).
     *
     * @param key property key
     * @return the parsed boolean
     * @throws ConfigurationException if the key is missing or not a boolean
     */
    public boolean getBoolean(String key) {
        String value = get(key).toLowerCase(Locale.ROOT);
        if (!value.equals("true") && !value.equals("false")) {
            throw new ConfigurationException("'" + key + "' must be true or false but is '" + value + "'");
        }
        return Boolean.parseBoolean(value);
    }

    /**
     * Returns a required duration expressed in seconds.
     *
     * @param key property key
     * @return the duration
     * @throws ConfigurationException if the key is missing or not an integer
     */
    public Duration getSeconds(String key) {
        return Duration.ofSeconds(getInt(key));
    }

    /**
     * Returns a required duration expressed in milliseconds.
     *
     * @param key property key
     * @return the duration
     * @throws ConfigurationException if the key is missing or not an integer
     */
    public Duration getMillis(String key) {
        return Duration.ofMillis(getInt(key));
    }

    /**
     * Returns a required URL value.
     *
     * @param key property key
     * @return the parsed URL
     * @throws ConfigurationException if the key is missing or not a valid URL
     */
    public URL getUrl(String key) {
        String value = get(key);
        try {
            return new URI(value).toURL();
        } catch (URISyntaxException | MalformedURLException | IllegalArgumentException e) {
            throw new ConfigurationException("'" + key + "' is not a valid URL: '" + value + "'", e);
        }
    }

    /**
     * Returns every key that starts with {@code prefix} (with the prefix removed), resolved with the same precedence
     * as {@link #find(String)}. Useful for additional capabilities declared in files.
     *
     * @param prefix key prefix, for example {@code capability.}
     * @return a sorted map of key (without prefix) to value
     */
    public Map<String, String> withPrefix(String prefix) {
        Map<String, String> result = new TreeMap<>();
        String platformPrefix = platform.key() + "." + prefix;
        Stream.concat(fileLayers.stream(), Stream.of(systemProperties))
                .flatMap(props -> props.stringPropertyNames().stream())
                .map(name -> name.startsWith(platformPrefix) ? name.substring(platform.key().length() + 1) : name)
                .filter(name -> name.startsWith(prefix))
                .distinct()
                .forEach(name -> find(name).ifPresent(value -> result.put(name.substring(prefix.length()), value)));
        return result;
    }

    // ---------------------------------------------------------------------------------------------------------------

    /**
     * Resolves the active environment: {@code test.env} system property, then {@code TEST_ENV}, then {@code local}.
     *
     * @param systemProperties     system properties
     * @param environmentVariables environment variables (including {@code .env})
     * @return the lower-case environment name
     */
    static String resolveEnvironment(Properties systemProperties, Map<String, String> environmentVariables) {
        return Stream
                .of(systemProperties.getProperty(ENVIRONMENT_PROPERTY), environmentVariables.get(ENVIRONMENT_VARIABLE))
                .filter(FrameworkConfig::hasText)
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .findFirst()
                .orElse(DEFAULT_ENVIRONMENT);
    }

    /** Replaces {@code ${NAME}} and {@code ${NAME:-default}}; if a variable is missing, fails naming it. */
    private String interpolate(String key, String value) {
        Matcher matcher = PLACEHOLDER.matcher(value);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String name = matcher.group(1);
            @Nullable String fromSystem = systemProperties.getProperty(name);
            @Nullable String fromEnvironment = environmentVariables.get(name);
            @Nullable String fallback = matcher.group(2);
            @Nullable String resolved = fallback;
            if (hasText(fromSystem)) {
                resolved = fromSystem;
            } else if (hasText(fromEnvironment)) {
                resolved = fromEnvironment;
            }
            if (resolved == null) {
                throw new ConfigurationException("'" + key + "' needs the variable " + name + " ('"
                        + environment + "' environment). Define it as an environment variable or in the .env file");
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(resolved));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * Converts a property key to its environment variable name ({@code ios.device.name} becomes
     * {@code IOS_DEVICE_NAME}).
     *
     * @param key property key
     * @return the environment variable name
     */
    static String toEnvironmentName(String key) {
        return key.toUpperCase(Locale.ROOT).replace('.', '_').replace('-', '_');
    }

    /**
     * Parses the {@code .env} format: {@code KEY=value}, {@code #} comments, optional {@code export} and quotes.
     *
     * @param content file contents
     * @return the variables, in file order
     */
    static Map<String, String> parseDotEnv(String content) {
        Map<String, String> variables = new LinkedHashMap<>();
        for (String rawLine : content.lines().toList()) {
            String line = rawLine.strip();
            if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) {
                continue;
            }
            if (line.startsWith("export ")) {
                line = line.substring("export ".length()).strip();
            }
            int separator = line.indexOf('=');
            String name = line.substring(0, separator).strip();
            String value = line.substring(separator + 1).strip();
            boolean quoted = value.length() >= 2
                    && ((value.startsWith("\"") && value.endsWith("\""))
                            || (value.startsWith("'") && value.endsWith("'")));
            variables.put(name, quoted ? value.substring(1, value.length() - 1) : value);
        }
        return variables;
    }

    /**
     * Reads a {@code .env} file if it exists.
     *
     * @param file path of the file
     * @return the variables, or an empty map when the file does not exist
     */
    static Map<String, String> loadDotEnv(Path file) {
        if (!Files.isRegularFile(file)) {
            return Map.of();
        }
        try {
            return parseDotEnv(Files.readString(file, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + file, e);
        }
    }

    private static boolean hasText(@Nullable String value) {
        return value != null && !value.isBlank();
    }

    private static Properties loadRequired(String resource) {
        return loadOptional(resource)
                .orElseThrow(() -> new ConfigurationException("'" + resource + "' was not found on the classpath"));
    }

    private static Optional<Properties> loadOptional(String resource) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try (InputStream stream = loader.getResourceAsStream(resource)) {
            if (stream == null) {
                return Optional.empty();
            }
            Properties properties = new Properties();
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                properties.load(reader);
            }
            return Optional.of(properties);
        } catch (IOException e) {
            throw new ConfigurationException("Could not read '" + resource + "'", e);
        }
    }

    private static Properties copyOf(Properties source) {
        Properties copy = new Properties();
        source.stringPropertyNames().forEach(name -> copy.setProperty(name, source.getProperty(name)));
        return copy;
    }
}
