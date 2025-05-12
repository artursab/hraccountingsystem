package com.company.ems.controller;

import com.company.ems.dto.TimeEntryDTO;
import com.company.ems.model.Employee;
import com.company.ems.model.TimeEntry;
import com.company.ems.model.User;
import com.company.ems.service.EmployeeService;
import com.company.ems.service.TimeTrackingService;
import com.company.ems.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/time-tracking")
public class TimeTrackingController {

    private final TimeTrackingService timeTrackingService;
    private final EmployeeService employeeService;
    private final UserService userService;

    @Autowired
    public TimeTrackingController(TimeTrackingService timeTrackingService,
                                  EmployeeService employeeService,
                                  UserService userService) {
        this.timeTrackingService = timeTrackingService;
        this.employeeService = employeeService;
        this.userService = userService;
    }

    @GetMapping("/entries")
    public ResponseEntity<Map<String, Object>> getTimeEntries(
            @RequestParam(required = false) Long employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {

        try {
            // If no employeeId is provided, try to get current user's employee record
            if (employeeId == null) {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                User currentUser = (User) userService.loadUserByUsername(auth.getName());

                Optional<Employee> employeeOpt = employeeService.findAll().stream()
                        .filter(e -> e.getUser() != null && e.getUser().getId().equals(currentUser.getId()))
                        .findFirst();

                if (employeeOpt.isPresent()) {
                    employeeId = employeeOpt.get().getId();
                } else {
                    return ResponseEntity.ok(Map.of(
                            "success", false,
                            "message", "No employee record found for current user"
                    ));
                }
            }

            // Get time entries for the date range
            List<TimeEntry> entries = timeTrackingService.findByEmployeeAndDateRange(employeeId, start, end);

            // Log for debugging
            System.out.println("Found " + entries.size() + " entries for employee " + employeeId);

            // Convert entries to DTOs
            List<TimeEntryDTO> entryDTOs = entries.stream()
                    .map(timeTrackingService::convertToDTO)
                    .collect(Collectors.toList());

            // Calculate total hours in the period
            Optional<Long> totalMinutesOpt = timeTrackingService.getTotalDuration(employeeId, start, end);
            double totalHours = totalMinutesOpt.orElse(0L) / 60.0;

            // Create response map
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("entries", entryDTOs);
            response.put("totalHours", totalHours);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "Error retrieving time entries: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/active-entry")
    public ResponseEntity<Map<String, Object>> getActiveTimeEntry(
            @RequestParam(required = false) Long employeeId) {

        // If no employeeId is provided, try to get current user's employee record
        if (employeeId == null) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = (User) userService.loadUserByUsername(auth.getName());

            Optional<Employee> employeeOpt = employeeService.findAll().stream()
                    .filter(e -> e.getUser() != null && e.getUser().getId().equals(currentUser.getId()))
                    .findFirst();

            if (employeeOpt.isPresent()) {
                employeeId = employeeOpt.get().getId();
            } else {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "No employee record found for current user"
                ));
            }
        }

        // Get active time entry
        Optional<TimeEntry> activeEntryOpt = timeTrackingService.findActiveTimeEntry(employeeId);

        // Create response map
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);

        if (activeEntryOpt.isPresent()) {
            TimeEntry activeEntry = activeEntryOpt.get();
            response.put("hasActiveEntry", true);
            response.put("activeEntry", timeTrackingService.convertToDTO(activeEntry));

            // Calculate duration since start
            LocalDateTime now = LocalDateTime.now();
            long durationMinutes = java.time.Duration.between(activeEntry.getStartTime(), now).toMinutes();
            response.put("durationMinutes", durationMinutes);
            response.put("durationHours", durationMinutes / 60.0);
        } else {
            response.put("hasActiveEntry", false);
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/entries/{id}")
    public ResponseEntity<Map<String, Object>> updateTimeEntry(
            @PathVariable Long id,
            @RequestBody TimeEntryDTO timeEntryDTO) {

        try {
            TimeEntry updatedEntry = timeTrackingService.update(id, timeEntryDTO);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Time entry updated successfully",
                    "entry", timeTrackingService.convertToDTO(updatedEntry)
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Failed to update time entry: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startTimeTracking(
            @RequestParam Long employeeId,
            @RequestParam String category,
            @RequestParam(required = false) String description) {

        try {
            TimeEntry timeEntry = timeTrackingService.startTimeTracking(employeeId, category, description);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Time tracking started successfully",
                    "entry", timeTrackingService.convertToDTO(timeEntry)
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/stop")
    public ResponseEntity<Map<String, Object>> stopTimeTracking(
            @RequestParam Long employeeId) {

        try {
            TimeEntry timeEntry = timeTrackingService.stopTimeTracking(employeeId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Time tracking stopped successfully",
                    "entry", timeTrackingService.convertToDTO(timeEntry)
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
}