package cn.kafei.quietly;

import java.util.Locale;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public record QuietlyConfig(
    String language,
    long doubleClickWindowMs,
    int baseOpenTicks,
    int ticksPerItemKind,
    double interactionRange,
    double soundSuppressionRadius
) {
    private static final String DEFAULT_LANGUAGE = "zh_cn";

    public static QuietlyConfig load(JavaPlugin plugin) {
        FileConfiguration config = plugin.getConfig();
        String language = normalizeLanguage(config.getString("language", DEFAULT_LANGUAGE));
        config.set("language", language);

        return new QuietlyConfig(
            language,
            Math.max(150L, config.getLong("double-click-window-ms", 350L)),
            Math.max(1, config.getInt("base-open-ticks", 12)),
            Math.max(1, config.getInt("ticks-per-item-kind", 4)),
            Math.max(1.5D, config.getDouble("interaction-range", 5.0D)),
            Math.max(1.0D, config.getDouble("sound-suppression-radius", 16.0D))
        );
    }

    private static String normalizeLanguage(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_LANGUAGE;
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        return "en_us".equals(normalized) ? normalized : DEFAULT_LANGUAGE;
    }
}
