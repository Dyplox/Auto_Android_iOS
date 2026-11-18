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
 * Picks an available iOS simulator when neither {@code device.udid} nor {@code platform.version} is configured,
 * so the suite runs out of the box on any Mac without guessing which Xcode version is installed.
 *
 * <p>Priority: 1) an iPhone that is already booted, 2) the configured {@code device.name} on the newest runtime,
 * 3) the first iPhone on the newest runtime.</p>
 */
public final class IosSimulatorLocator {

    private static final Pattern VERSION_SEPARATOR = Pattern.compile("\\.");
    private static final Pattern RUNTIME_HEADER = Pattern.compile("^-- iOS ([0-9.]+) --$");
    private static final Pattern DEVICE_LINE = Pattern
            .compile("^\\s+(.+) \\(([0-9A-Fa-f-]{36})\\) \\(([A-Za-z ]+)\\)\\s*$");

    private final Supplier<Optional<String>> simctlOutput;

    /** Creates a locator that runs {@code xcrun simctl list devices available} (macOS only). */
    public IosSimulatorLocator() {
        this(IosSimulatorLocator::runSimctl);
    }

    /**
     * Lets tests inject the {@code simctl} output, so they run without a Mac.
     *
     * @param simctlOutput supplies the command output, or empty when it is not available
     */
    IosSimulatorLocator(Supplier<Optional<String>> simctlOutput) {
        this.simctlOutput = simctlOutput;
    }

    /**
     * Finds the best available iPhone simulator.
     *
     * @param preferredName device name to prefer, for example {@code iPhone 17}
     * @return the chosen simulator, or empty when not on macOS or no iPhone simulator is available
     */
    public Optional<Simulator> locate(String preferredName) {
        return simctlOutput.get().flatMap(output -> choose(parse(output), preferredName));
    }

    /**
     * Parses the output of {@code xcrun simctl list devices available}, keeping only iPhones on iOS runtimes.
     *
     * @param simctlOutput raw command output
     * @return the iPhone simulators found, in output order
     */
    static List<Simulator> parse(String simctlOutput) {
        List<Simulator> simulators = new ArrayList<>();
        String runtime = null;
        for (String line : simctlOutput.lines().toList()) {
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

    /**
     * Applies the selection priority described in the class Javadoc.
     *
     * @param simulators    candidates
     * @param preferredName device name to prefer
     * @return the chosen simulator, or empty when there are no candidates
     */
    static Optional<Simulator> choose(List<Simulator> simulators, String preferredName) {
        Comparator<Simulator> newestRuntimeFirst = Comparator
                .comparing(Simulator::runtime, IosSimulatorLocator::compareVersions).reversed();
        Optional<Simulator> booted = simulators.stream().filter(Simulator::booted).findFirst();
        if (booted.isPresent()) {
            return booted;
        }
        Optional<Simulator> preferred = simulators.stream()
                .filter(sim -> sim.name().equals(preferredName))
                .min(newestRuntimeFirst);
        return preferred.isPresent() ? preferred : simulators.stream().min(newestRuntimeFirst);
    }

    /**
     * Compares dotted numeric versions ({@code 18.2} &lt; {@code 26.0}); missing segments count as zero.
     *
     * @param left  first version
     * @param right second version
     * @return a negative number, zero or a positive number, as in {@link Comparator#compare}
     */
    static int compareVersions(String left, String right) {
        int[] l = VERSION_SEPARATOR.splitAsStream(left).mapToInt(Integer::parseInt).toArray();
        int[] r = VERSION_SEPARATOR.splitAsStream(right).mapToInt(Integer::parseInt).toArray();
        for (int i = 0; i < Math.max(l.length, r.length); i++) {
            int diff = Integer.compare(i < l.length ? l[i] : 0, i < r.length ? r[i] : 0);
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

    /**
     * An available simulator as reported by {@code simctl}.
     *
     * @param name    device name, for example {@code iPhone 17}
     * @param udid    simulator UDID
     * @param runtime iOS runtime version, for example {@code 26.0}
     * @param booted  whether the simulator is already running
     */
    public record Simulator(String name, String udid, String runtime, boolean booted) {
    }
}
