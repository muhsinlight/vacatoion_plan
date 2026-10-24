package com.vacationplan.api.config;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "vacation")
public class VacationProperties {

	/**
	 * Her /preview çağrısında bütçe kalemlerini price_records tablosuna yaz.
	 */
	private boolean savePriceSnapshots = false;

	/** Scraper → API: /api/v1/scraper/** uçlarında beklenen anahtar. Boşsa bu uçlar 503 döner. */
	private String scraperApiKey = "";

	/** Boşsa tüm kaynak adresler kabul. Doluysa yalnız bu IP'ler X-Scraper-Key ile yazabilir. */
	private List<String> scraperAllowedIps = List.of();

	/** API → scraper: /routes/bus ve /lodging/search çağrılarında gönderilen anahtar. Boşsa scraper çağrılmaz. */
	private String scraperTriggerKey = "";

	/** Scraper HTTP. */
	private String scraperBaseUrl = "http://localhost:8081";

	/** Scraper iki bacağı sayfa başına 60 sn ve aralarda 5 sn bekleyerek çeker. */
	private Duration scraperReadTimeout = Duration.ofSeconds(210);

	private Cors cors = new Cors();

	/** Bu süreden yeni route_quotes kaydı varsa otobüs fiyatı yeniden aranmaz. */
	private Duration routeQuoteMaxAge = Duration.ofHours(12);

	/** Bu süreden yeni lodging_offers kaydı varsa konaklama scrape yeniden çağrılmaz. */
	private Duration lodgingQuoteMaxAge = Duration.ofHours(12);

	/** Bu süreden yeni Google yemek bandı varsa Places yeniden çağrılmaz. */
	private Duration foodQuoteMaxAge = Duration.ofDays(7);

	/** Önizleme, canlı restoran listesini bu süre kadar bekler. */
	private Duration foodRefreshWait = Duration.ofSeconds(60);

	/** checked_at bu süreden eski price_observations satırları ingest başında silinir. 0 veya negatif ise temizlik yapılmaz. */
	private Duration priceObservationRetention = Duration.ofDays(365);

	/** Bu süreden yeni fuel_prices satırı araç bütçesine yazılır. */
	private Duration carFuelMaxAge = Duration.ofDays(14);

	/** Tek araç gidiş-dönüş litre bandının alt ucu. İl mesafesi yoktur. */
	private BigDecimal carFuelRoundTripLitersMin = new BigDecimal("25");

	/** Tek araç gidiş-dönüş litre bandının üst ucu. */
	private BigDecimal carFuelRoundTripLitersMax = new BigDecimal("55");

	private Groq groq = new Groq();

	private Quota quota = new Quota();

	private Auth auth = new Auth();

	public boolean isSavePriceSnapshots() {
		return savePriceSnapshots;
	}

	public void setSavePriceSnapshots(boolean savePriceSnapshots) {
		this.savePriceSnapshots = savePriceSnapshots;
	}

	public String getScraperApiKey() {
		return scraperApiKey;
	}

	public void setScraperApiKey(String scraperApiKey) {
		this.scraperApiKey = scraperApiKey;
	}

	public List<String> getScraperAllowedIps() {
		return scraperAllowedIps;
	}

	public void setScraperAllowedIps(List<String> scraperAllowedIps) {
		if (scraperAllowedIps == null) {
			this.scraperAllowedIps = List.of();
			return;
		}
		List<String> allowed = new ArrayList<>();
		for (String ip : scraperAllowedIps) {
			if (ip == null) {
				continue;
			}
			String trimmed = ip.trim();
			if (!trimmed.isEmpty()) {
				allowed.add(trimmed);
			}
		}
		this.scraperAllowedIps = List.copyOf(allowed);
	}

	public String getScraperTriggerKey() {
		return scraperTriggerKey;
	}

	public void setScraperTriggerKey(String scraperTriggerKey) {
		this.scraperTriggerKey = scraperTriggerKey;
	}

	public String getScraperBaseUrl() {
		return scraperBaseUrl;
	}

	public void setScraperBaseUrl(String scraperBaseUrl) {
		this.scraperBaseUrl = scraperBaseUrl;
	}

	public Duration getScraperReadTimeout() {
		return scraperReadTimeout;
	}

	public void setScraperReadTimeout(Duration scraperReadTimeout) {
		this.scraperReadTimeout = scraperReadTimeout == null ? Duration.ofSeconds(210) : scraperReadTimeout;
	}

	public Cors getCors() {
		return cors;
	}

	public void setCors(Cors cors) {
		this.cors = cors == null ? new Cors() : cors;
	}

	public Duration getRouteQuoteMaxAge() {
		return routeQuoteMaxAge;
	}

	public void setRouteQuoteMaxAge(Duration routeQuoteMaxAge) {
		this.routeQuoteMaxAge = routeQuoteMaxAge == null ? Duration.ofHours(12) : routeQuoteMaxAge;
	}

	public Duration getLodgingQuoteMaxAge() {
		return lodgingQuoteMaxAge;
	}

	public void setLodgingQuoteMaxAge(Duration lodgingQuoteMaxAge) {
		this.lodgingQuoteMaxAge = lodgingQuoteMaxAge == null ? Duration.ofHours(12) : lodgingQuoteMaxAge;
	}

	public Duration getFoodQuoteMaxAge() {
		return foodQuoteMaxAge;
	}

	public void setFoodQuoteMaxAge(Duration foodQuoteMaxAge) {
		this.foodQuoteMaxAge = foodQuoteMaxAge == null ? Duration.ofDays(7) : foodQuoteMaxAge;
	}

	public Duration getFoodRefreshWait() {
		return foodRefreshWait;
	}

	public void setFoodRefreshWait(Duration foodRefreshWait) {
		this.foodRefreshWait = foodRefreshWait == null ? Duration.ofSeconds(60) : foodRefreshWait;
	}

	public Duration getPriceObservationRetention() {
		return priceObservationRetention;
	}

	public void setPriceObservationRetention(Duration priceObservationRetention) {
		this.priceObservationRetention = priceObservationRetention == null
				? Duration.ofDays(365)
				: priceObservationRetention;
	}

	public Duration getCarFuelMaxAge() {
		return carFuelMaxAge;
	}

	public void setCarFuelMaxAge(Duration carFuelMaxAge) {
		this.carFuelMaxAge = carFuelMaxAge == null ? Duration.ofDays(14) : carFuelMaxAge;
	}

	public BigDecimal getCarFuelRoundTripLitersMin() {
		return carFuelRoundTripLitersMin;
	}

	public void setCarFuelRoundTripLitersMin(BigDecimal carFuelRoundTripLitersMin) {
		this.carFuelRoundTripLitersMin = carFuelRoundTripLitersMin == null
				? new BigDecimal("25")
				: carFuelRoundTripLitersMin;
	}

	public BigDecimal getCarFuelRoundTripLitersMax() {
		return carFuelRoundTripLitersMax;
	}

	public void setCarFuelRoundTripLitersMax(BigDecimal carFuelRoundTripLitersMax) {
		this.carFuelRoundTripLitersMax = carFuelRoundTripLitersMax == null
				? new BigDecimal("55")
				: carFuelRoundTripLitersMax;
	}

	public Groq getGroq() {
		return groq;
	}

	public void setGroq(Groq groq) {
		this.groq = groq == null ? new Groq() : groq;
	}

	public Quota getQuota() {
		return quota;
	}

	public void setQuota(Quota quota) {
		this.quota = quota == null ? new Quota() : quota;
	}

	public Auth getAuth() {
		return auth;
	}

	public void setAuth(Auth auth) {
		this.auth = auth == null ? new Auth() : auth;
	}

	public static class Groq {

		private String baseUrl = "https://api.groq.com/openai/v1";
		private String model = "openai/gpt-oss-20b";
		private String apiKey = "";

		public String getBaseUrl() {
			return baseUrl;
		}

		public void setBaseUrl(String baseUrl) {
			this.baseUrl = baseUrl;
		}

		public String getModel() {
			return model;
		}

		public void setModel(String model) {
			this.model = model;
		}

		public String getApiKey() {
			return apiKey;
		}

		public void setApiKey(String apiKey) {
			this.apiKey = apiKey;
		}
	}

	public static class Cors {

		/** Tarayıcıdan /api/** çağırabilecek kaynaklar. Aynı kökendeki sayfalar CORS kullanmaz. */
		private List<String> allowedOrigins = List.of(
				"http://localhost:5173",
				"http://127.0.0.1:5173",
				"http://localhost:19006");

		public List<String> getAllowedOrigins() {
			return allowedOrigins;
		}

		public void setAllowedOrigins(List<String> allowedOrigins) {
			this.allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
		}
	}

	public static class Quota {

		/** 0 = sınırsız. Otobüs ve konaklama araması. */
		private int scraperMaxPerHour = 20;

		/** 0 = sınırsız. Groq etkinlik önerisi. */
		private int groqMaxPerHour = 30;

		public int getScraperMaxPerHour() {
			return scraperMaxPerHour;
		}

		public void setScraperMaxPerHour(int scraperMaxPerHour) {
			this.scraperMaxPerHour = Math.max(0, scraperMaxPerHour);
		}

		public int getGroqMaxPerHour() {
			return groqMaxPerHour;
		}

		public void setGroqMaxPerHour(int groqMaxPerHour) {
			this.groqMaxPerHour = Math.max(0, groqMaxPerHour);
		}
	}

	public static class Auth {

		private String jwtSecret = "";
		private Duration accessTtl = Duration.ofMinutes(15);
		private Duration refreshTtl = Duration.ofDays(30);
		private String adminEmail = "";
		private String adminPassword = "";

		public String getJwtSecret() {
			return jwtSecret;
		}

		public void setJwtSecret(String jwtSecret) {
			this.jwtSecret = jwtSecret == null ? "" : jwtSecret;
		}

		public Duration getAccessTtl() {
			return accessTtl;
		}

		public void setAccessTtl(Duration accessTtl) {
			this.accessTtl = accessTtl == null ? Duration.ofMinutes(15) : accessTtl;
		}

		public Duration getRefreshTtl() {
			return refreshTtl;
		}

		public void setRefreshTtl(Duration refreshTtl) {
			this.refreshTtl = refreshTtl == null ? Duration.ofDays(30) : refreshTtl;
		}

		public String getAdminEmail() {
			return adminEmail;
		}

		public void setAdminEmail(String adminEmail) {
			this.adminEmail = adminEmail == null ? "" : adminEmail;
		}

		public String getAdminPassword() {
			return adminPassword;
		}

		public void setAdminPassword(String adminPassword) {
			this.adminPassword = adminPassword == null ? "" : adminPassword;
		}
	}
}
