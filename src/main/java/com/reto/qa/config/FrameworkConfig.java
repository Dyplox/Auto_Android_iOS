package com.reto.qa.config;

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
 * Configuración inmutable por plataforma y ambiente.
 *
 * <p><b>Ambiente</b> ({@code local}, {@code ci}, {@code browserstack}, {@code saucelabs}…): se elige con la system
 * property {@code test.env} (en Gradle: {@code -Penv=…}), la variable {@code TEST_ENV} o el archivo {@code .env};
 * por defecto {@code local}.</p>
 *
 * <p><b>Precedencia</b> para una clave {@code k} en la plataforma {@code p} (gana la primera):</p>
 * <ol>
 *     <li>System property {@code p.k} (Gradle: {@code -Pios.device.name="iPhone 17"})</li>
 *     <li>System property {@code k}</li>
 *     <li>Variable de entorno {@code P_K} y luego {@code K} (también desde el archivo {@code .env})</li>
 *     <li>{@code config/env/<ambiente>-<plataforma>.properties}</li>
 *     <li>{@code config/env/<ambiente>.properties}</li>
 *     <li>{@code config/<plataforma>.properties}</li>
 *     <li>{@code config/framework.properties}</li>
 * </ol>
 *
 * <p><b>Secretos</b>: los valores admiten {@code ${VARIABLE}} y {@code ${VARIABLE:-por defecto}}, resueltos desde
 * system properties, variables de entorno o {@code .env}. Así las credenciales nunca se versionan. La resolución es
 * perezosa: una corrida local no exige credenciales de la nube.</p>
 */
public final class FrameworkConfig {

    static final String COMMON_FILE = "config/framework.properties";
    static final String ENVIRONMENT_PROPERTY = "test.env";
    static final String ENVIRONMENT_VARIABLE = "TEST_ENV";
    static final String DEFAULT_ENVIRONMENT = "local";

    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([A-Za-z0-9_.]+)(?::-([^}]*))?}");
    private static final Map<Platform, FrameworkConfig> CACHE = new ConcurrentHashMap<>();

    private final Platform platform;
    private final String environment;
    /** Archivos de configuración, del de mayor al de menor prioridad. */
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

    /** Configuración real: classpath + {@code .env} + entorno + system properties. Se cachea por plataforma. */
    public static FrameworkConfig forPlatform(Platform platform) {
        return CACHE.computeIfAbsent(platform, p -> {
            Map<String, String> variables = new HashMap<>(loadDotEnv(Paths.get(System.getProperty("user.dir"), ".env")));
            variables.putAll(System.getenv());
            Properties system = System.getProperties();
            String environment = resolveEnvironment(system, variables);
            return new FrameworkConfig(p, environment, loadLayers(p, environment), variables, system);
        });
    }

    /** Archivos de configuración del classpath para una plataforma y ambiente, de mayor a menor prioridad. */
    static List<Properties> loadLayers(Platform platform, String environment) {
        List<Properties> layers = new ArrayList<>();
        loadOptional("config/env/" + environment + "-" + platform.key() + ".properties").ifPresent(layers::add);
        layers.add(loadOptional("config/env/" + environment + ".properties").orElseThrow(() -> new ConfigurationException(
                "No existe el ambiente '" + environment + "': falta src/test/resources/config/env/" + environment
                        + ".properties. Ambientes incluidos: local, ci, browserstack, saucelabs")));
        layers.add(loadRequired("config/" + platform.key() + ".properties"));
        layers.add(loadRequired(COMMON_FILE));
        return layers;
    }

    /** Fábrica pura (sin estado global) para pruebas unitarias; {@code fileLayers} va de mayor a menor prioridad. */
    static FrameworkConfig of(Platform platform,
                              String environment,
                              List<Properties> fileLayers,
                              Map<String, String> environmentVariables,
                              Properties systemProperties) {
        return new FrameworkConfig(platform, environment, fileLayers, environmentVariables, systemProperties);
    }

    /** Atajo para pruebas: solo archivo común y de plataforma, ambiente {@code local}. */
    static FrameworkConfig of(Platform platform,
                              Properties common,
                              Properties platformSpecific,
                              Map<String, String> environmentVariables,
                              Properties systemProperties) {
        return of(platform, DEFAULT_ENVIRONMENT, List.of(platformSpecific, common), environmentVariables, systemProperties);
    }

    public Platform platform() {
        return platform;
    }

    /** Nombre del ambiente activo (local, ci, browserstack, saucelabs…). */
    public String environment() {
        return environment;
    }

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

    public String get(String key) {
        return find(key).orElseThrow(() -> new ConfigurationException(
                "Falta la propiedad obligatoria '" + key + "' para " + platform.w3cName() + " en el ambiente '"
                        + environment + "'. Defínela en config/" + platform.key() + ".properties, en config/env/"
                        + environment + ".properties o con -P" + key + "=..."));
    }

    public int getInt(String key) {
        String value = get(key);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new ConfigurationException("'" + key + "' debe ser un entero y es '" + value + "'", e);
        }
    }

    public boolean getBoolean(String key) {
        String value = get(key).toLowerCase(Locale.ROOT);
        if (!value.equals("true") && !value.equals("false")) {
            throw new ConfigurationException("'" + key + "' debe ser true o false y es '" + value + "'");
        }
        return Boolean.parseBoolean(value);
    }

    public Duration getSeconds(String key) {
        return Duration.ofSeconds(getInt(key));
    }

    public Duration getMillis(String key) {
        return Duration.ofMillis(getInt(key));
    }

    public URL getUrl(String key) {
        String value = get(key);
        try {
            return new URI(value).toURL();
        } catch (URISyntaxException | MalformedURLException | IllegalArgumentException e) {
            throw new ConfigurationException("'" + key + "' no es una URL válida: '" + value + "'", e);
        }
    }

    /**
     * Devuelve todas las claves que empiezan por {@code prefix} (sin el prefijo), resueltas con la misma
     * precedencia que {@link #find(String)}. Útil para capabilities adicionales declaradas en archivos.
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

    static String resolveEnvironment(Properties systemProperties, Map<String, String> environmentVariables) {
        return Stream.of(systemProperties.getProperty(ENVIRONMENT_PROPERTY), environmentVariables.get(ENVIRONMENT_VARIABLE))
                .filter(FrameworkConfig::hasText)
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .findFirst()
                .orElse(DEFAULT_ENVIRONMENT);
    }

    /** Sustituye {@code ${NOMBRE}} y {@code ${NOMBRE:-defecto}}; si falta la variable, falla indicando cuál es. */
    private String interpolate(String key, String value) {
        Matcher matcher = PLACEHOLDER.matcher(value);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String name = matcher.group(1);
            String fromSystem = systemProperties.getProperty(name);
            String fromEnvironment = environmentVariables.get(name);
            String fallback = matcher.group(2);
            String resolved = hasText(fromSystem) ? fromSystem : hasText(fromEnvironment) ? fromEnvironment : fallback;
            if (resolved == null) {
                throw new ConfigurationException("'" + key + "' necesita la variable " + name + " (ambiente '"
                        + environment + "'). Defínela como variable de entorno o en el archivo .env");
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(resolved));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    static String toEnvironmentName(String key) {
        return key.toUpperCase(Locale.ROOT).replace('.', '_').replace('-', '_');
    }

    /** Formato {@code .env}: {@code CLAVE=valor}, comentarios con {@code #}, {@code export} y comillas opcionales. */
    static Map<String, String> parseDotEnv(String content) {
        Map<String, String> variables = new LinkedHashMap<>();
        for (String rawLine : content.split("\\R")) {
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
                    && (value.startsWith("\"") && value.endsWith("\"") || value.startsWith("'") && value.endsWith("'"));
            variables.put(name, quoted ? value.substring(1, value.length() - 1) : value);
        }
        return variables;
    }

    static Map<String, String> loadDotEnv(Path file) {
        if (!Files.isRegularFile(file)) {
            return Map.of();
        }
        try {
            return parseDotEnv(Files.readString(file, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer " + file, e);
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static Properties loadRequired(String resource) {
        return loadOptional(resource)
                .orElseThrow(() -> new ConfigurationException("No se encontró '" + resource + "' en el classpath"));
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
            throw new ConfigurationException("No se pudo leer '" + resource + "'", e);
        }
    }

    private static Properties copyOf(Properties source) {
        Properties copy = new Properties();
        source.stringPropertyNames().forEach(name -> copy.setProperty(name, source.getProperty(name)));
        return copy;
    }
}
