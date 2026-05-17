package cn.kafei.quietly;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Properties;
import org.bukkit.plugin.java.JavaPlugin;

public final class QuietlyMessages {
    private static final String DEFAULT_LANGUAGE = "zh_cn";
    private static final String FALLBACK_LANGUAGE = "en_us";

    private final Properties selectedLanguage;
    private final Properties defaultLanguage;
    private final Properties fallbackLanguage;

    private QuietlyMessages(Properties selectedLanguage, Properties defaultLanguage, Properties fallbackLanguage) {
        this.selectedLanguage = selectedLanguage;
        this.defaultLanguage = defaultLanguage;
        this.fallbackLanguage = fallbackLanguage;
    }

    public static QuietlyMessages load(JavaPlugin plugin, String language) {
        Properties defaultLanguage = loadProperties(plugin, DEFAULT_LANGUAGE);
        Properties fallbackLanguage = loadProperties(plugin, FALLBACK_LANGUAGE);
        Properties selectedLanguage = DEFAULT_LANGUAGE.equals(language)
            ? defaultLanguage
            : loadProperties(plugin, normalize(language));
        return new QuietlyMessages(selectedLanguage, defaultLanguage, fallbackLanguage);
    }

    public String progress(double remainingSeconds) {
        return format("quietly.silent_open.progress", remainingSeconds);
    }

    private String format(String key, Object... args) {
        return String.format(Locale.ROOT, resolve(key), args);
    }

    private String resolve(String key) {
        if (selectedLanguage.containsKey(key)) {
            return selectedLanguage.getProperty(key);
        }
        if (defaultLanguage.containsKey(key)) {
            return defaultLanguage.getProperty(key);
        }
        return fallbackLanguage.getProperty(key, key);
    }

    private static Properties loadProperties(JavaPlugin plugin, String language) {
        Properties properties = new Properties();
        String resourcePath = "messages/" + language + ".properties";

        try (InputStreamReader reader = new InputStreamReader(plugin.getResource(resourcePath), StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException | NullPointerException exception) {
            plugin.getLogger().warning("Failed to load language file " + resourcePath);
        }

        return properties;
    }

    private static String normalize(String language) {
        if (language == null) {
            return DEFAULT_LANGUAGE;
        }
        return language.trim().toLowerCase(Locale.ROOT).replace('-', '_');
    }
}
