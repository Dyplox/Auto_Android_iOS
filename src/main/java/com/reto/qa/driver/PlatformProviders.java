package com.reto.qa.driver;

import com.reto.qa.config.Platform;

import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Registry of {@link PlatformProvider}s: the framework asks by platform and never branches with {@code if/else} on the OS. */
public final class PlatformProviders {

    private final Map<Platform, PlatformProvider> providers = new EnumMap<>(Platform.class);

    /**
     * Registers the given providers.
     *
     * @param providers one provider per platform
     * @throws IllegalArgumentException if two providers serve the same platform
     */
    public PlatformProviders(Collection<? extends PlatformProvider> providers) {
        providers.forEach(provider -> {
            if (this.providers.putIfAbsent(provider.platform(), provider) != null) {
                throw new IllegalArgumentException("Duplicate provider for " + provider.platform());
            }
        });
    }

    /**
     * Returns the registry with the built-in Android and iOS providers.
     *
     * @return the default registry
     */
    public static PlatformProviders defaults() {
        return new PlatformProviders(List.of(new AndroidPlatformProvider(), new IosPlatformProvider()));
    }

    /**
     * Returns the provider for a platform.
     *
     * @param platform target platform
     * @return the registered provider
     * @throws IllegalStateException if no provider is registered for the platform
     */
    public PlatformProvider forPlatform(Platform platform) {
        return Optional.ofNullable(providers.get(platform))
                .orElseThrow(() -> new IllegalStateException("No PlatformProvider registered for " + platform));
    }
}
