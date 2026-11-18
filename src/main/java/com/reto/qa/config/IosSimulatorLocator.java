package com.reto.qa.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Elige un simulador de iOS disponible cuando no se configuró {@code device.udid} ni {@code platform.version}.
 * Así la suite corre "out of the box" en cualquier Mac sin adivinar la versión de Xcode instalada.
 *
 * <p>Prioridad: 1) un iPhone ya encendido, 2) el {@code device.name} configurado en el runtime más nuevo,
 * 3) el primer iPhone del runtime más nuevo.</p>
 */
public final class IosSimulatorLocator {

    private static final Pattern RUNTIME_HEADER = Pattern.compile("^-- iOS ([0-9.]+) --$");
    private static final Pattern DEVICE_LINE =
            Pattern.compile("^\\s+(.+) \\(([0-9A-Fa-f-]{36})\\) \\(([A-Za-z ]+)\\)\\s*$");

    private final Supplier<Optional<String>> simctlOutput;

    public IosSimulatorLocator() {
        this(IosSimulatorLocator::runSimctl);
    }

    /** Permite inyectar la salida de {@code simctl} (pruebas unitarias sin Mac). */
    IosSimulatorLocator(Supplier<Optional<String>> simctlOutput) {
        this.simctlOutput = simctlOutput;
    }

    public Optional<Simulator> locate(String preferredName) {
        return simctlOutput.get().flatMap(output -> choose(parse(output), preferredName));
    }

    static List<Simulator> parse(String simctlOutput) {
        List<Simulator> simulators = new ArrayList<>();
        String runtime = null;
        for (String line : simctlOutput.split("\\R")) {
            if (line.startsWith("--")) {
                Matcher header = RUNTIME_HEADER.matcher(line.trim());
                runtime = header.matches() ? header.group(1) : null;
                continue;
            }
            Matcher device = DEVICE_LINE.matcher(line);
            if (runtime != null && device.matches() && device.group(1).startsWith("iPhone")) {
                simulators.add(new Simulator(device.group(1), device.group(2), runtime,
                        "Booted".equalsIgnoreCase(device.group(3))));
            }
        }
        return simulators;
    }

    static Optional<Simulator> choose(List<Simulator> simulators, String preferredName) {
        Comparator<Simulator> newestRuntimeFirst = Comparator.comparing(Simulator::runtime, IosSimulatorLocator::compareVersions).reversed();
        Optional<Simulator> booted = simulators.stream().filter(Simulator::booted).findFirst();
        if (booted.isPresent()) {
            return booted;
        }
        Optional<Simulator> preferred = simulators.stream()
                .filter(sim -> sim.name().equals(preferredName))
                .min(newestRuntimeFirst);
        return preferred.isPresent() ? preferred : simulators.stream().min(newestRuntimeFirst);
    }

    static int compareVersions(String left, String right) {
        String[] l = left.split("\\.");
        String[] r = right.split("\\.");
        for (int i = 0; i < Math.max(l.length, r.length); i++) {
            int diff = Integer.compare(i < l.length ? Integer.parseInt(l[i]) : 0, i < r.length ? Integer.parseInt(r[i]) : 0);
            if (diff != 0) {
                return diff;
            }
        }
        return 0;
    }

    private static Optional<String> runSimctl() {
        if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("mac")) {
            return Optional.empty();
        }
        try {
            Process process = new ProcessBuilder("xcrun", "simctl", "list", "devices", "available")
                    .redirectErrorStream(true)
                    .start();
            String output;
            try (InputStream stream = process.getInputStream()) {
                output = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            }
            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            return finished && process.exitValue() == 0 ? Optional.of(output) : Optional.empty();
        } catch (IOException e) {
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    /** Simulador disponible según {@code simctl}. */
    public record Simulator(String name, String udid, String runtime, boolean booted) {
    }
}
