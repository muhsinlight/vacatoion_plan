package com.vacationplan.api.entity;

import com.vacationplan.api.model.ScrapeCatalogField;
import com.vacationplan.api.model.ScrapeParserType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "scrape_targets")
public class ScrapeTarget {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private boolean enabled = true;

	@Column(name = "city_key", nullable = false, length = 80)
	private String cityKey;

	@Enumerated(EnumType.STRING)
	@Column(name = "catalog_field", nullable = false, length = 40)
	private ScrapeCatalogField catalogField;

	@Column(name = "activity_key", length = 80)
	private String activityKey;

	@Column(name = "source_url", nullable = false, length = 500)
	private String sourceUrl;

	@Enumerated(EnumType.STRING)
	@Column(name = "parser_type", nullable = false, length = 30)
	private ScrapeParserType parserType;

	@Column(nullable = false, length = 200)
	private String label;

	public Long getId() {
		return id;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getCityKey() {
		return cityKey;
	}

	public void setCityKey(String cityKey) {
		this.cityKey = cityKey;
	}

	public ScrapeCatalogField getCatalogField() {
		return catalogField;
	}

	public void setCatalogField(ScrapeCatalogField catalogField) {
		this.catalogField = catalogField;
	}

	public String getActivityKey() {
		return activityKey;
	}

	public void setActivityKey(String activityKey) {
		this.activityKey = activityKey;
	}

	public String getSourceUrl() {
		return sourceUrl;
	}

	public void setSourceUrl(String sourceUrl) {
		this.sourceUrl = sourceUrl;
	}

	public ScrapeParserType getParserType() {
		return parserType;
	}

	public void setParserType(ScrapeParserType parserType) {
		this.parserType = parserType;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}
}
