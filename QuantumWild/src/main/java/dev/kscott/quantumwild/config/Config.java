package dev.kscott.quantumwild.config;

import dev.kscott.quantum.rule.ruleset.QuantumRuleset;
import dev.kscott.quantum.rule.ruleset.RulesetRegistry;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;

import java.io.File;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Stores the Quantum configuration and handles the loading and registration of rulesets
 */
public class Config {

    /**
     * JavaPlugin reference
     */
    private final @NonNull JavaPlugin plugin;

    /**
     * A map that stores a world's UUID and their associated ruleset
     */
    private final @NonNull Map<UUID, QuantumRuleset> worldRulesetMap;

    /**
     * The root quantum.conf config node
     */
    private @MonotonicNonNull CommentedConfigurationNode root;

    /**
     * RulesetRegistry reference
     */
    private final @NonNull RulesetRegistry rulesetRegistry;

    /**
     * How much cooldown should be applied if LP isn't enabled
     */
    private int FALLBACK_COOLDOWN;

    /**
     * Is the per-world cooldown system enabled?
     */
    private boolean PER_WORLD_COOLDOWN_ENABLED;

    /**
     * Is EssentialsX TP integration enabled?
     */
    private boolean IS_ESSENTIALS_TP_INTEGRATION_ENABLED;

    /**
     * Is warmup time enabled?
     */
    private boolean IS_WARMUP_ENABLED;

    /**
     * How long should teleport warmup be
     */
    private int WARMUP_TIME;

    /**
     * Is the default world enabled?
     */
    private boolean DEFAULT_WORLD_ENABLED;

    /**
     * The default world
     */
    private @Nullable World DEFAULT_WORLD;

    /**
     * Constructs the config, loads it, and loads rulesets.
     *
     * @param plugin          {@link this#plugin}
     * @param rulesetRegistry {@link this#rulesetRegistry}
     */
    public Config(final @NonNull RulesetRegistry rulesetRegistry, final @NonNull JavaPlugin plugin) {
        this.plugin = plugin;
        this.rulesetRegistry = rulesetRegistry;

        // Save config to file if it doesn't already exist
        if (!new File(this.plugin.getDataFolder(), "config.conf").exists()) {
            plugin.saveResource("config.conf", false);
        }

        this.worldRulesetMap = new HashMap<>();

        // Load the config
        this.loadConfig();
        this.loadConfigurationValues();
    }

    /**
     * Returns a registered QuantumRuleset that is associated to a /wild world
     *
     * @param world World
     * @return associated QuantumRuleset
     */
    public @Nullable QuantumRuleset getRuleset(final @NonNull World world) {
        return this.worldRulesetMap.get(world.getUID());
    }

    /**
     * Loads the config into the {@link this.root} node
     */
    private void loadConfig() {
        final HoconConfigurationLoader loader = HoconConfigurationLoader.builder()
                .path(Paths.get(plugin.getDataFolder().getAbsolutePath(), "config.conf"))
                .build();

        try {
            root = loader.load();
        } catch (ConfigurateException e) {
            throw new RuntimeException("Failed to load the configuration.", e);
        }
    }

    /**
     * Loads QuantumWild's configuration values
     */
    private void loadConfigurationValues() {
        this.PER_WORLD_COOLDOWN_ENABLED = this.root.node("wild").node("cooldown").node("per-world-cooldown").getBoolean(false);
        this.FALLBACK_COOLDOWN = this.root.node("wild").node("cooldown").node("cooldown-fallback-value").getInt(300);
        this.IS_ESSENTIALS_TP_INTEGRATION_ENABLED = this.root.node("wild").node("teleportation").node("essentialsx-integration").getBoolean(false);
        this.IS_WARMUP_ENABLED = this.root.node("wild").node("teleportation").node("warmup").node("enabled").getBoolean(false);
        this.WARMUP_TIME = this.root.node("wild").node("teleportation").node("warmup").node("warmup-time").getInt(5);
        this.DEFAULT_WORLD_ENABLED = this.root.node("wild").node("teleportation").node("default-world").node("enabled").getBoolean(false);
        this.DEFAULT_WORLD = Bukkit.getWorld(this.root.node("wild").node("teleportation").node("default-world").node("world").getString("world"));

        if (this.DEFAULT_WORLD_ENABLED && this.DEFAULT_WORLD == null) {
            this.plugin.getLogger().warning("The default-world configuration option is enabled, but the world name does not exist! Please review your QuantumWild configuration.");
            this.DEFAULT_WORLD_ENABLED = false;
        }

        this.worldRulesetMap.clear();

        for (final Map.Entry<Object, ? extends ConfigurationNode> entry : root.node("worlds").childrenMap().entrySet()) {
            final @NonNull Object key = entry.getKey();

            if (!(key instanceof String)) {
                this.plugin.getLogger().severe("Error loading world ruleset map.");
                continue;
            }

            final @NonNull String worldName = (String) key;
            final @Nullable World world = Bukkit.getWorld(worldName);

            if (world == null) {
                this.plugin.getLogger().severe("Error loading ruleset map: world was null. Are you sure you spelled '" + worldName + "' correctly?");
                continue;
            }

            final @NonNull ConfigurationNode value = entry.getValue();

            final @Nullable String rulesetId = value.node("ruleset").getString();

            if (rulesetId == null) {
                this.plugin.getLogger().severe("Error loading ruleset map: ruleset id was null.");
                continue;
            }

            final @Nullable QuantumRuleset ruleset = this.rulesetRegistry.getRuleset(rulesetId);

            if (ruleset == null) {
                this.plugin.getLogger().severe("Error loading ruleset map: RulesetRegistry returned null. Are you sure you spelled '" + rulesetId + "' correctly?");
                continue;
            }

            this.worldRulesetMap.put(world.getUID(), ruleset);
        }
    }

    /**
     * @return {@link this#PER_WORLD_COOLDOWN_ENABLED}
     */
    public boolean isPerWorldCooldownEnabled() {
        return PER_WORLD_COOLDOWN_ENABLED;
    }

    /**
     * @return {@link this#FALLBACK_COOLDOWN}
     */
    public int getFallbackCooldown() {
        return FALLBACK_COOLDOWN;
    }

    /**
     * @return {@link this#IS_ESSENTIALS_TP_INTEGRATION_ENABLED}
     */
    public boolean isEssentialsIntegrationEnabled() {
        return IS_ESSENTIALS_TP_INTEGRATION_ENABLED;
    }

    /**
     * @return {@link this#IS_WARMUP_ENABLED}
     */
    public boolean isWarmupEnabled() {
        return IS_WARMUP_ENABLED;
    }

    /**
     * @return {@link this#WARMUP_TIME}
     */
    public int getWarmupTime() {
        return WARMUP_TIME;
    }

    /**
     * @return {@link this#DEFAULT_WORLD_ENABLED}
     */
    public boolean isDefaultWorldEnabled() {
        return DEFAULT_WORLD_ENABLED;
    }

    /**
     * May be null if {@link this#DEFAULT_WORLD_ENABLED} is false.
     *
     * @return {@link this#DEFAULT_WORLD}
     */
    public @Nullable World getDefaultWorld() {
        return DEFAULT_WORLD;
    }
}
