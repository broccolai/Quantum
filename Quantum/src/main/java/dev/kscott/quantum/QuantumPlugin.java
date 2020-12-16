package dev.kscott.quantum;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import dev.kscott.quantum.command.QuantumCommand;
import dev.kscott.quantum.config.Config;
import dev.kscott.quantum.inject.*;
import dev.kscott.quantum.location.LocationProvider;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;

@Singleton
public final class QuantumPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        final @NonNull Injector injector = Guice.createInjector(
                new PluginModule(this),
                new CommandModule(this),
                new LocationModule(this),
                new RulesetModule(),
                new ConfigModule()
        );


        this.loadConfig(injector);

        injector.getInstance(QuantumCommand.class);

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    private void loadConfig(final @NonNull Injector injector) {
        final @NonNull Config config = injector.getInstance(Config.class);
    }
}