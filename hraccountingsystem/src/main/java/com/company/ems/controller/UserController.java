package com.company.ems.controller;

import com.company.ems.dto.TimeEntryDTO;
import com.company.ems.model.Employee;
import com.company.ems.model.TimeEntry;
import com.company.ems.model.User;
import com.company.ems.service.EmployeeService;
import com.company.ems.service.TimeTrackingService;
import com.company.ems.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/user") // Changed from "/templates/user"
public class UserController {

    private final UserService userService;
    private final EmployeeService employeeService;
    private final TimeTrackingService timeTrackingService;

    @Autowired
    public UserController(UserService userService,
                          EmployeeService employeeService,
                          TimeTrackingService timeTrackingService) {
        this.userService = userService;
        this.employeeService = employeeService;
        this.timeTrackingService = timeTrackingService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // Get current user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) userService.loadUserByUsername(auth.getName());

        // Find associated employee
        Optional<Employee> employeeOpt = employeeService.findAll().stream()
                .filter(e -> e.getUser() != null && e.getUser().getId().equals(currentUser.getId()))
                .findFirst();

        if (employeeOpt.isEmpty()) {
            model.addAttribute("noEmployeeRecord", true);
        } else {
            Employee employee = employeeOpt.get();
            model.addAttribute("noEmployeeRecord", false);
            model.addAttribute("employee", employee);

            // Check if there's an active time entry
            Optional<TimeEntry> activeTimeEntry = timeTrackingService.findActiveTimeEntry(employee.getId());

            // Get today's time entries
            List<TimeEntry> todayEntries = timeTrackingService.findByEmployeeAndDate(
                    employee.getId(), LocalDate.now());

            // Calculate today's total hours, including active entry if it exists
            long todayMinutes = todayEntries.stream()
                    .filter(entry -> entry.getEndTime() != null)
                    .mapToLong(entry -> entry.getDurationMinutes())
                    .sum();

            // Add the duration of the active entry if it exists
            if (activeTimeEntry.isPresent()) {
                TimeEntry active = activeTimeEntry.get();
                // Calculate minutes from start time until now
                LocalDateTime now = LocalDateTime.now();
                long activeMinutes = java.time.Duration.between(active.getStartTime(), now).toMinutes();
                todayMinutes += activeMinutes;
            }

            double todayHours = todayMinutes / 60.0;

            // Get this week's total hours
            LocalDate startOfWeek = LocalDate.now().minusDays(LocalDate.now().getDayOfWeek().getValue() - 1);
            LocalDate endOfWeek = startOfWeek.plusDays(6);

            LocalDateTime weekStart = startOfWeek.atStartOfDay();
            LocalDateTime weekEnd = endOfWeek.atTime(LocalTime.MAX);

            Optional<Long> weekMinutesOpt = timeTrackingService.getTotalDuration(
                    employee.getId(), weekStart, weekEnd);

            double weekHours = weekMinutesOpt.orElse(0L) / 60.0;

            // Add attributes to model
            model.addAttribute("activeTimeEntry", activeTimeEntry.orElse(null));
            model.addAttribute("todayEntries", todayEntries);
            model.addAttribute("todayHours", String.format("%.2f", todayHours));
            model.addAttribute("weekHours", String.format("%.2f", weekHours));
        }

        return "user/dashboard";
    }

    @GetMapping("/profile")
    public String profile(Model model) {
        // Get current user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) userService.loadUserByUsername(auth.getName());

        // Find associated employee
        Optional<Employee> employeeOpt = employeeService.findAll().stream()
                .filter(e -> e.getUser() != null && e.getUser().getId().equals(currentUser.getId()))
                .findFirst();

        model.addAttribute("user", currentUser); // Changed from "templates/user"
        model.addAttribute("employee", employeeOpt.orElse(null));

        return "user/profile"; // Changed from "templates/user/profile"
    }

    @GetMapping("/time-tracking")
    public String timeTracking(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Model model) {

        try {
            // Get current user and associated employee
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = (User) userService.loadUserByUsername(auth.getName());

            Optional<Employee> employeeOpt = employeeService.findAll().stream()
                    .filter(e -> e.getUser() != null && e.getUser().getId().equals(currentUser.getId()))
                    .findFirst();

            if (employeeOpt.isEmpty()) {
                model.addAttribute("noEmployeeRecord", true);
                return "user/time-tracking";
            }

            // Explicitly set noEmployeeRecord to false when an employee is found
            model.addAttribute("noEmployeeRecord", false);

            Employee employee = employeeOpt.get();

            // Use today's date if not specified
            if (date == null) {
                date = LocalDate.now();
            }

            // Get time entries for the current month
            YearMonth yearMonth = YearMonth.from(date);
            LocalDateTime monthStart = yearMonth.atDay(1).atStartOfDay();
            LocalDateTime monthEnd = yearMonth.atEndOfMonth().atTime(LocalTime.MAX);

            List<TimeEntry> monthEntries = timeTrackingService.findByEmployeeAndDateRange(
                    employee.getId(), monthStart, monthEnd);

            // Prepare calendar data with mutable lists
            Map<String, List<TimeEntry>> calendarData = new HashMap<>();
            for (TimeEntry entry : monthEntries) {
                String dateKey = entry.getStartTime().toLocalDate().format(DateTimeFormatter.ISO_DATE);
                // Use a mutable list (ArrayList) instead of List.of()
                if (!calendarData.containsKey(dateKey)) {
                    calendarData.put(dateKey, new java.util.ArrayList<>());
                }
                calendarData.get(dateKey).add(entry);
            }

            // Check if there's an active time entry
            Optional<TimeEntry> activeTimeEntry = timeTrackingService.findActiveTimeEntry(employee.getId());

            // Calculate total hours for the current month
            Optional<Long> totalMonthMinutesOpt = timeTrackingService.getTotalDuration(
                    employee.getId(), monthStart, monthEnd);

            double totalMonthHours = totalMonthMinutesOpt.orElse(0L) / 60.0;

            // Add all data to model
            model.addAttribute("employee", employee);
            model.addAttribute("currentDate", date);
            model.addAttribute("currentMonth", yearMonth);
            model.addAttribute("calendarData", calendarData);
            model.addAttribute("activeTimeEntry", activeTimeEntry.orElse(null));
            model.addAttribute("categories", List.of("Regular Work", "Meeting", "Training", "Project", "Other"));
            model.addAttribute("totalHours", String.format("%.2f", totalMonthHours));

            // Add a new time entry for the form
            model.addAttribute("newTimeEntry", new TimeEntryDTO());

            // Initialize with today's date for the new entry form
            TimeEntryDTO newEntry = new TimeEntryDTO();
            newEntry.setStartTime(LocalDateTime.now());
            newEntry.setEndTime(LocalDateTime.now().plusHours(1)); // Default 1 hour
            newEntry.setEmployeeId(employee.getId());
            model.addAttribute("newTimeEntry", newEntry);

            return "user/time-tracking";
        } catch (Exception e) {
            // Add error handling to help with debugging
            e.printStackTrace();
            model.addAttribute("errorMessage", "Error loading time tracking page: " + e.getMessage());
            model.addAttribute("noEmployeeRecord", false);
            model.addAttribute("newTimeEntry", new TimeEntryDTO());
            return "user/time-tracking";
        }
    }

    // Rest of methods remain the same, just return paths need to be updated
    @PostMapping("/time-tracking/start")
    public String startTimeTracking(
            @RequestParam Long employeeId,
            @RequestParam String category,
            @RequestParam(required = false) String description,
            RedirectAttributes redirectAttributes) {

        try {
            timeTrackingService.startTimeTracking(employeeId, category, description);
            redirectAttributes.addFlashAttribute("successMessage", "Time tracking started successfully!");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/user/time-tracking";
    }

    // From UserController.java
    @GetMapping("/time-tracking/stop")
    public String stopTimeTracking(
            @RequestParam Long employeeId,
            RedirectAttributes redirectAttributes) {

        try {
            timeTrackingService.stopTimeTracking(employeeId);
            redirectAttributes.addFlashAttribute("successMessage", "Time tracking stopped successfully!");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/user/time-tracking";
    }

    @PostMapping("/time-tracking/add")
    public String addTimeEntry(
            @Valid @ModelAttribute("newTimeEntry") TimeEntryDTO timeEntryDTO,
            BindingResult result,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please correct the errors in the form.");
            return "redirect:/user/time-tracking";
        }

        timeTrackingService.save(timeEntryDTO);
        redirectAttributes.addFlashAttribute("successMessage", "Time entry added successfully!");

        return "redirect:/user/time-tracking";
    }

    @GetMapping("/time-tracking/delete/{id}")
    public String deleteTimeEntry(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        timeTrackingService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Time entry deleted successfully!");

        return "redirect:/user/time-tracking";
    }
}