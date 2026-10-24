package com.vacationplan.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(
		@NotBlank(message = "Ad zorunlu")
		@Size(min = 2, max = 60, message = "Ad 2 ile 60 karakter arasında olmalı")
		@Pattern(regexp = "^[\\p{L}][\\p{L} .'-]*$", message = "Ad yalnızca harf içermelidir")
		String firstName,
		@NotBlank(message = "Soyad zorunlu")
		@Size(min = 2, max = 60, message = "Soyad 2 ile 60 karakter arasında olmalı")
		@Pattern(regexp = "^[\\p{L}][\\p{L} .'-]*$", message = "Soyad yalnızca harf içermelidir")
		String lastName
) {
	public ProfileUpdateRequest {
		firstName = tidy(firstName);
		lastName = tidy(lastName);
	}

	private static String tidy(String value) {
		if (value == null) {
			return null;
		}
		return value.trim().replaceAll("\\s+", " ");
	}
}
