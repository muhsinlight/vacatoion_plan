package com.vacationplan.api.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

/**
 * {@code api/.env} ve kök {@code .env} dosyalarını Spring Environment'a yükler.
 * Mevcut işletim sistemi değişkenlerini ezmez.
 */
public class DotEnvEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

	static final String PROPERTY_SOURCE_NAME = "dotenvFile";

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		Map<String, Object> values = new LinkedHashMap<>();
		for (Path envFile : findEnvFiles()) {
			parse(readLines(envFile)).forEach(values::putIfAbsent);
		}
		values.entrySet().removeIf(entry -> hasOsEnv(entry.getKey()));
		if (values.isEmpty()) {
			return;
		}
		MapPropertySource source = new MapPropertySource(PROPERTY_SOURCE_NAME, values);
		var sources = environment.getPropertySources();
		if (sources.contains(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME)) {
			sources.addAfter(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, source);
		} else {
			sources.addFirst(source);
		}
	}

	/** Önce gelen dosya aynı anahtarda kazanır: api/.env, sonra kök .env (scraper anahtarları). */
	static List<Path> findEnvFiles() {
		Path cwd = Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
		Set<Path> candidates = new LinkedHashSet<>();
		candidates.add(cwd.resolve(".env"));
		candidates.add(cwd.resolve("api").resolve(".env"));
		if (cwd.getParent() != null && cwd.getFileName() != null && "api".equals(cwd.getFileName().toString())) {
			candidates.add(cwd.getParent().resolve(".env"));
		}
		return candidates.stream().filter(Files::isRegularFile).toList();
	}

	static Map<String, Object> parse(List<String> lines) {
		Map<String, Object> values = new LinkedHashMap<>();
		for (String raw : lines) {
			String line = raw.replace("\uFEFF", "").strip();
			if (line.isEmpty() || line.startsWith("#")) {
				continue;
			}
			if (line.startsWith("export ")) {
				line = line.substring("export ".length()).strip();
			}
			int eq = line.indexOf('=');
			if (eq <= 0) {
				continue;
			}
			String key = line.substring(0, eq).strip();
			if (!isEnvKey(key)) {
				continue;
			}
			values.put(key, unquote(line.substring(eq + 1).strip()));
		}
		return values;
	}

	private static boolean hasOsEnv(String key) {
		String existing = System.getenv(key);
		return existing != null && !existing.isBlank();
	}

	private static boolean isEnvKey(String key) {
		if (key.isEmpty()) {
			return false;
		}
		int first = key.charAt(0);
		if (!isLetterOrUnderscore(first)) {
			return false;
		}
		return key.chars().skip(1).allMatch(ch -> isLetterOrUnderscore(ch) || (ch >= '0' && ch <= '9'));
	}

	private static boolean isLetterOrUnderscore(int ch) {
		return (ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z') || ch == '_';
	}

	private static String unquote(String value) {
		if (value.length() >= 2) {
			char first = value.charAt(0);
			char last = value.charAt(value.length() - 1);
			if ((first == '"' || first == '\'') && first == last) {
				return value.substring(1, value.length() - 1);
			}
		}
		return value;
	}

	private static List<String> readLines(Path envFile) {
		try {
			return Files.readAllLines(envFile, StandardCharsets.UTF_8);
		} catch (IOException ex) {
			throw new IllegalStateException(".env okunamadı: " + envFile, ex);
		}
	}
}
