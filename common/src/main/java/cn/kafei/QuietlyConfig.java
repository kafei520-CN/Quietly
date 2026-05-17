package cn.kafei;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

public final class QuietlyConfig {
	private static final String FILE_NAME = "quietly.properties";
	private static final String KEY_LANGUAGE = "language";
	private static final String DEFAULT_LANGUAGE = "zh_cn";

	private static String language = DEFAULT_LANGUAGE;

	private QuietlyConfig() {
	}

	public static void initialize(Path configDirectory) {
		if (configDirectory == null) {
			language = DEFAULT_LANGUAGE;
			return;
		}

		try {
			Files.createDirectories(configDirectory);
			Path configFile = configDirectory.resolve(FILE_NAME);
			Properties properties = new Properties();

			if (Files.exists(configFile)) {
				try (InputStream inputStream = Files.newInputStream(configFile)) {
					properties.load(inputStream);
				}
			}

			language = normalizeLanguage(properties.getProperty(KEY_LANGUAGE));
			properties.setProperty(KEY_LANGUAGE, language);

			try (OutputStream outputStream = Files.newOutputStream(configFile)) {
				properties.store(outputStream, "Quietly server configuration");
			}
		} catch (IOException exception) {
			language = DEFAULT_LANGUAGE;
			QuietlyCommon.LOGGER.warn("Failed to load Quietly config, using default language {}", language, exception);
		}
	}

	public static String language() {
		return language;
	}

	private static String normalizeLanguage(String value) {
		if (value == null || value.isBlank()) {
			return DEFAULT_LANGUAGE;
		}

		String normalized = value.trim().toLowerCase(Locale.ROOT).replace('-', '_');
		if (!QuietlyLocalization.hasLanguage(normalized)) {
			return DEFAULT_LANGUAGE;
		}
		return normalized;
	}
}
