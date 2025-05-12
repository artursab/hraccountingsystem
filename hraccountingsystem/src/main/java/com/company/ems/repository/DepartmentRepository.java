package com.company.ems.repository;

import com.company.ems.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for Department entity
 */
@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    /**
     * Find a department by name
     * @param name The department name
     * @return Optional containing the department, or empty if not found
     */
    Optional<Department> findByName(String name);

    /**
     * Check if a department with the given name exists
     * @param name The department name
     * @return true if exists, false otherwise
     */
    boolean existsByName(String name);
}