package com.reto.qa.config;

import com.reto.qa.config.IosSimulatorLocator.Simulator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class IosSimulatorLocatorTest {

    private static final String SIMCTL_OUTPUT = String.join("\n",
            "== Devices ==",
            "-- iOS 18.5 --",
            "    iPhone 16 (11111111-1111-1111-1111-111111111111) (Shutdown) ",
            "    iPhone 17 (22222222-2222-2222-2222-222222222222) (Shutdown) ",
            "-- iOS 26.1 --",
            "    iPhone 17 (33333333-3333-3333-3333-333333333333) (Shutdown) ",
            "    iPhone 17 Pro (44444444-4444-4444-4444-444444444444) (Shutdown) ",
            "    iPad Pro 11-inch (M4) (55555555-5555-5555-5555-555555555555) (Shutdown) ",
            "-- watchOS 26.1 --",
            "    Apple Watch Series 11 (46mm) (66666666-6666-6666-6666-666666666666) (Shutdown) ");

    @Test
    void parsesOnlyIphonesFromIosRuntimes() {
        List<Simulator> simulators = IosSimulatorLocator.parse(SIMCTL_OUTPUT);

        assertThat(simulators).extracting(Simulator::name)
                .containsExactly("iPhone 16", "iPhone 17", "iPhone 17", "iPhone 17 Pro");
        assertThat(simulators.get(2).runtime()).isEqualTo("26.1");
    }

    @Test
    void prefersConfiguredNameOnNewestRuntime() {
        Optional<Simulator> chosen = IosSimulatorLocator.choose(IosSimulatorLocator.parse(SIMCTL_OUTPUT), "iPhone 17");

        assertThat(chosen).map(Simulator::udid).contains("33333333-3333-3333-3333-333333333333");
    }

    @Test
    void prefersAnAlreadyBootedSimulator() {
        String output = SIMCTL_OUTPUT.replace("(11111111-1111-1111-1111-111111111111) (Shutdown)",
                "(11111111-1111-1111-1111-111111111111) (Booted)");

        Optional<Simulator> chosen = IosSimulatorLocator.choose(IosSimulatorLocator.parse(output), "iPhone 17");

        assertThat(chosen).map(Simulator::name).contains("iPhone 16");
    }

    @Test
    void fallsBackToNewestRuntimeWhenNameIsUnknown() {
        Optional<Simulator> chosen = IosSimulatorLocator.choose(IosSimulatorLocator.parse(SIMCTL_OUTPUT), "iPhone 99");

        assertThat(chosen).map(Simulator::runtime).contains("26.1");
    }

    @Test
    void returnsEmptyWhenSimctlIsNotAvailable() {
        assertThat(new IosSimulatorLocator(Optional::empty).locate("iPhone 17")).isEmpty();
    }

    @Test
    void comparesVersionsNumerically() {
        assertThat(IosSimulatorLocator.compareVersions("26.10", "26.9")).isPositive();
        assertThat(IosSimulatorLocator.compareVersions("18.5", "26.0")).isNegative();
        assertThat(IosSimulatorLocator.compareVersions("26", "26.0")).isZero();
    }
}
