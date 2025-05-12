package com.company.ems.service;

import com.company.ems.dto.TimeEntryDTO;
import com.company.ems.model.Employee;
import com.company.ems.model.TimeEntry;
import com.company.ems.model.User;
import com.company.ems.repository.EmployeeRepository;
import com.company.ems.repository.TimeEntryRepository;
import com.company.ems.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TimeTrackingService {

    private final TimeEntryRepository timeEntryRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;

    @Autowired
    public TimeTrackingService(TimeEntryRepository timeEntryRepository,
                               EmployeeRepository employeeRepository,
                               UserRepository userRepository) {
        this.timeEntryRepository = timeEntryRepository;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
    }

    public List<TimeEntry> findAll() {
        return timeEntryRepository.findAll();
    }

    public Optional<TimeEntry> findById(Long id) {
        return timeEntryRepository.findById(id);
    }

    public List<TimeEntry> findByEmployee(Employee employee) {
        return timeEntryRepository.findByEmployee(employee);
    }

    public List<TimeEntry> findByEmployeeId(Long employeeId) {
        return employeeRepository.findById(employeeId)
                .map(employee -> {
                    List<TimeEntry> results = findByEmployee(employee);
                    return new java.util.ArrayList<>(results);
                })
                .orElse(new java.util.ArrayList<>());
    }

    public List<TimeEntry> findByEmployeeAndDateRange(Long employeeId, LocalDateTime start, LocalDateTime end) {
        try {
            return employeeRepository.findById(employeeId)
                    .map(employee -> {
                        List<TimeEntry> entries = timeEntryRepository.findByEmployeeAndStartTimeBetween(employee, start, end);
                        System.out.println("Found " + entries.size() + " entries for employee " + employeeId +
                                " between " + start + " and " + end);
                        // Make sure to return a mutable list
                        return new ArrayList<>(entries);
                    })
                    .orElse(new ArrayList<>());
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public List<TimeEntry> findByEmployeeAndDate(Long employeeId, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
        return findByEmployeeAndDateRange(employeeId, startOfDay, endOfDay);
    }

    public Optional<TimeEntry> findActiveTimeEntry(Long employeeId) {
        return timeEntryRepository.findActiveTimeEntryByEmployeeId(employeeId);
    }
    @Transactional
    public TimeEntry save(TimeEntryDTO timeEntryDTO) {
        TimeEntry timeEntry = new TimeEntry();
        return saveOrUpdate(timeEntry, timeEntryDTO);
    }

    @Transactional
    public TimeEntry update(Long id, TimeEntryDTO timeEntryDTO) {
        return timeEntryRepository.findById(id)
                .map(existingTimeEntry -> saveOrUpdate(existingTimeEntry, timeEntryDTO))
                .orElseThrow(() -> new IllegalArgumentException("Time entry not found with ID: " + id));
    }

    private TimeEntry saveOrUpdate(TimeEntry timeEntry, TimeEntryDTO dto) {
        // Set basic fields
        timeEntry.setStartTime(dto.getStartTime());
        timeEntry.setEndTime(dto.getEndTime());
        timeEntry.setDescription(dto.getDescription());
        timeEntry.setCategory(dto.getCategory());
        timeEntry.setApproved(dto.isApproved());
        timeEntry.setApprovedAt(dto.getApprovedAt());

        // Set employee
        Employee employee = employeeRepository.findById(dto.getEmployeeId())
                .orElseThrow(() -> new IllegalArgumentException("Employee not found with ID: " + dto.getEmployeeId()));
        timeEntry.setEmployee(employee);

        // Set approved by user if provided
        if (dto.getApprovedById() != null) {
            userRepository.findById(dto.getApprovedById())
                    .ifPresent(timeEntry::setApprovedBy);
        }

        return timeEntryRepository.save(timeEntry);
    }

    @Transactional
    public void delete(Long id) {
        timeEntryRepository.deleteById(id);
    }

    @Transactional
    public TimeEntry startTimeTracking(Long employeeId, String category, String description) {
        try {
            // Check if employee already has an active time entry
            Optional<TimeEntry> activeTimeEntry = findActiveTimeEntry(employeeId);
            if (activeTimeEntry.isPresent()) {
                throw new IllegalStateException("Employee already has an active time entry");
            }

            // Create new time entry
            TimeEntryDTO dto = new TimeEntryDTO();
            dto.setEmployeeId(employeeId);
            dto.setStartTime(LocalDateTime.now());
            dto.setCategory(category);
            dto.setDescription(description);

            return save(dto);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to start time tracking: " + e.getMessage(), e);
        }
    }

    @Transactional
    public TimeEntry stopTimeTracking(Long employeeId) {
        try {
            // Find active time entry
            TimeEntry timeEntry = findActiveTimeEntry(employeeId)
                    .orElseThrow(() -> new IllegalStateException("No active time entry found for employee"));

            // Set end time
            timeEntry.setEndTime(LocalDateTime.now());

            return timeEntryRepository.save(timeEntry);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to stop time tracking: " + e.getMessage(), e);
        }
    }

    @Transactional
    public TimeEntry approveTimeEntry(Long timeEntryId, Long approverId) {
        TimeEntry timeEntry = timeEntryRepository.findById(timeEntryId)
                .orElseThrow(() -> new IllegalArgumentException("Time entry not found"));

        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new IllegalArgumentException("Approver not found"));

        timeEntry.setApproved(true);
        timeEntry.setApprovedAt(LocalDateTime.now());
        timeEntry.setApprovedBy(approver);

        return timeEntryRepository.save(timeEntry);
    }

    public Optional<Long> getTotalDuration(Long employeeId, LocalDateTime startDate, LocalDateTime endDate) {
        return timeEntryRepository.sumDurationByEmployeeIdAndDateRange(employeeId, startDate, endDate);
    }

    // Convert TimeEntry entity to TimeEntryDTO
    public TimeEntryDTO convertToDTO(TimeEntry timeEntry) {
        TimeEntryDTO dto = new TimeEntryDTO();
        dto.setId(timeEntry.getId());
        dto.setEmployeeId(timeEntry.getEmployee().getId());
        dto.setStartTime(timeEntry.getStartTime());
        dto.setEndTime(timeEntry.getEndTime());
        dto.setDescription(timeEntry.getDescription());
        dto.setCategory(timeEntry.getCategory());
        dto.setApproved(timeEntry.isApproved());
        dto.setApprovedAt(timeEntry.getApprovedAt());

        // Set employee name for display
        dto.setEmployeeName(timeEntry.getEmployee().getFullName());

        // Set approver id if present
        if (timeEntry.getApprovedBy() != null) {
            dto.setApprovedById(timeEntry.getApprovedBy().getId());
        }

        // Set calculated fields
        dto.setDurationMinutes(timeEntry.getDurationMinutes());
        dto.setDurationHours(timeEntry.getDurationHours());
        dto.setActive(timeEntry.isActive());

        return dto;
    }
}