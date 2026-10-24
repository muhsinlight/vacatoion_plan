package com.vacationplan.api.controller;

import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vacationplan.api.dto.AuthUserResponse;
import com.vacationplan.api.dto.PageResponse;
import com.vacationplan.api.dto.TripSummaryResponse;
import com.vacationplan.api.entity.User;
import com.vacationplan.api.repository.UserRepository;
import com.vacationplan.api.service.TripService;
import com.vacationplan.api.util.PageParams;

/**
 * Admin JSON yüzeyi. Sınıf rolü ve filtre kuralı birlikte kapalıdır.
 */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

	private final UserRepository userRepository;
	private final TripService tripService;

	public AdminController(UserRepository userRepository, TripService tripService) {
		this.userRepository = userRepository;
		this.tripService = tripService;
	}

	@GetMapping("/users")
	public PageResponse<AuthUserResponse> users(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "" + PageParams.DEFAULT_SIZE) int size) {
		Page<User> userPage = userRepository.findAll(PageParams.tripList(page, size));
		var content = userPage.getContent().stream().map(AuthUserResponse::from).toList();
		return PageResponse.of(content, userPage);
	}

	@GetMapping("/trips")
	public PageResponse<TripSummaryResponse> trips(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "" + PageParams.DEFAULT_SIZE) int size) {
		return tripService.listAllForAdmin(page, size);
	}
}
