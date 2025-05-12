package com.company.ems.repository;

import com.company.ems.model.Employee;
import com.company.ems.model.TimeEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TimeEntryRepository extends JpaRepository<TimeEntry, Long> {
    List<TimeEntry> findByEmployee(Employee employee);
    List<TimeEntry> findByEmployeeAndStartTimeBetween(Employee employee, LocalDateTime start, LocalDateTime end);

    @Query("SELECT t FROM TimeEntry t WHERE t.employee.id = ?1 AND t.endTime IS NULL")
    Optional<TimeEntry> findActiveTimeEntryByEmployeeId(Long employeeId);

    @Query("SELECT SUM(FUNCTION('TIMESTAMPDIFF', MINUTE, t.startTime, t.endTime)) FROM TimeEntry t " +
            "WHERE t.employee.id = ?1 AND t.startTime >= ?2 AND t.endTime <= ?3 AND t.endTime IS NOT NULL")
    Optional<Long> sumDurationByEmployeeIdAndDateRange(Long employeeId, LocalDateTime startDate, LocalDateTime endDate);
}