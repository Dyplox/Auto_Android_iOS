package com.reto.qa.driver;

import com.reto.qa.config.Platform;

import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Registro de {@link PlatformProvider}: el framework pide por plataforma, nunca hace {@code if/else} por SO. */
public final class PlatformProviders {

    private final Map<Platform, PlatformProvider> providers = new EnumMap<>(Platform.class);

    public PlatformProviders(Collection<? extends PlatformProvider> providers) {
        providers.forEach(provider -> {
            if (this.providers.putIfAbsent(provider.platform(), provider) != null) {
                throw new IllegalArgumentException("Proveedor duplicado para " + provider.platform());
            }
        });
    }

    public static PlatformProviders defaults() {
        return new PlatformProviders(List.of(new AndroidPlatformProvider(), new IosPlatformProvider()));
    }

    public PlatformProvider forPlatform(Platform platform) {
        return Optional.ofNullable(providers.get(platform))
                .orElseThrow(() -> new IllegalStateException("No hay PlatformProvider registrado para " + platform));
    }
}
