package com.vacationplan.api.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Şehir anahtarı 81 il slug'ıdır. Liste ve ilçe takma adları {@code tr_provinces.json} içindedir.
 * Scraper gelen anahtarı yeniden üretmez; il slug'ı {@code city_slug} altında değişmez.
 */
public final class CityKeyNormalizer {

	private static final ObjectMapper MAPPER = new ObjectMapper();
	private static final Set<String> PROVINCES;
	private static final Map<String, String> ALIASES;

	static {
		try (InputStream in = CityKeyNormalizer.class.getResourceAsStream("/tr_provinces.json")) {
			if (in == null) {
				throw new IllegalStateException("tr_provinces.json classpath'te yok");
			}
			JsonNode root = MAPPER.readTree(in);
			Set<String> provinces = new java.util.LinkedHashSet<>();
			root.get("provinces").forEach(node -> provinces.add(node.asText()));
			Map<String, String> aliases = new LinkedHashMap<>();
			root.get("aliases").properties().forEach(entry -> aliases.put(entry.getKey(), entry.getValue().asText()));
			PROVINCES = Set.copyOf(provinces);
			ALIASES = Map.copyOf(aliases);
		} catch (IOException ex) {
			throw new IllegalStateException("tr_provinces.json okunamadı", ex);
		}
	}

	private CityKeyNormalizer() {
	}

	public static String normalize(String city) {
		if (city == null) {
			return "";
		}
		List<String> tokens = tokens(fold(city));
		for (int i = 0; i < tokens.size(); i++) {
			if (i + 1 < tokens.size()) {
				String joined = lookup(tokens.get(i) + tokens.get(i + 1));
				if (joined != null) {
					return joined;
				}
			}
			String single = lookup(tokens.get(i));
			if (single != null) {
				return single;
			}
		}
		return String.join("-", tokens);
	}

	private static String lookup(String token) {
		if (PROVINCES.contains(token)) {
			return token;
		}
		String alias = ALIASES.get(token);
		if (alias != null && PROVINCES.contains(alias)) {
			return alias;
		}
		return null;
	}

	private static List<String> tokens(String folded) {
		List<String> tokens = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		for (int i = 0; i < folded.length(); i++) {
			char ch = folded.charAt(i);
			if ((ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9')) {
				current.append(ch);
			} else if (!current.isEmpty()) {
				tokens.add(current.toString());
				current.setLength(0);
			}
		}
		if (!current.isEmpty()) {
			tokens.add(current.toString());
		}
		return tokens;
	}

	private static String fold(String city) {
		String value = city.trim()
				.replace('İ', 'i')
				.replace('I', 'i')
				.toLowerCase(Locale.ROOT)
				.replace("\u0307", "");
		return value
				.replace("ı", "i")
				.replace("ğ", "g")
				.replace("ü", "u")
				.replace("ş", "s")
				.replace("ö", "o")
				.replace("ç", "c");
	}
}
