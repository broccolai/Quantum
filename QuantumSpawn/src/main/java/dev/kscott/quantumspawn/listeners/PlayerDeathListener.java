package dev.kscott.quantumspawn.listeners;

import com.google.inject.Inject;
import dev.kscott.quantum.location.LocationProvider;
import dev.kscott.quantum.location.QuantumLocation;
import dev.kscott.quantum.rule.ruleset.QuantumRuleset;
import dev.kscott.quantumspawn.config.Config;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.concurrent.CompletableFuture;

public class PlayerDeathListener implements Listener {

    /**
     * Config reference.
     */
    private final @NonNull Config config;

    /**
     * JavaPlugin reference.
     */
    private final @NonNull JavaPlugin plugin;

    /**
     * LocationProvider reference.
     */
    private final @NonNull LocationProvider locationProvider;

    /**
     * Constructs PlayerJoinListener.
     *
     * @param locationProvider LocationProvider reference.
     */
    @Inject
    public PlayerDeathListener(
            final @NonNull JavaPlugin plugin,
            final @NonNull Config config,
            final @NonNull LocationProvider locationProvider
    ) {
        this.config = config;
        this.plugin = plugin;
        this.locationProvider = locationProvider;
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        final @NonNull Player player = event.getPlayer();

        if (!player.hasPlayedBefore()) {
            return;
        }

        final @NonNull World world = player.getWorld();

        final @Nullable QuantumRuleset ruleset = config.getRuleset(world);

        if (ruleset == null) {
            return;
        }

        final @NonNull CompletableFuture<QuantumLocation> cf = this.locationProvider.getSpawnLocation(ruleset);

        cf.exceptionally(err -> {
            this.plugin.getLogger().warning("Failed to generate a spawn (spawn-on-death) for "+player.getName()+": "+err.getMessage());
            return null;
        });

        if (!cf.isCompletedExceptionally()) {
            cf.thenAccept(quantumLocation -> {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        player.teleportAsync(quantumLocation.getLocation());
                    }
                }.runTask(plugin);
            });
        }
    }

}
