package com.example.aiarticlesummarizer.api.controller;

import com.example.aiarticlesummarizer.api.dto.AdminStatsDto;
import com.example.aiarticlesummarizer.api.dto.UserDto;
import com.example.aiarticlesummarizer.model.User;
import com.example.aiarticlesummarizer.repository.SummaryRepository;
import com.example.aiarticlesummarizer.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final SummaryRepository summaryRepository;

    public AdminController(UserRepository userRepository, SummaryRepository summaryRepository) {
        this.userRepository = userRepository;
        this.summaryRepository = summaryRepository;
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserDto>> listUsers() {
        List<UserDto> users = userRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @GetMapping("/stats")
    public ResponseEntity<AdminStatsDto> getStats() {
        long totalUsers = userRepository.count();
        long totalSummaries = summaryRepository.count();
        return ResponseEntity.ok(new AdminStatsDto(totalUsers, totalSummaries));
    }

    private UserDto toDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setRole(user.getRole().name());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }
}
