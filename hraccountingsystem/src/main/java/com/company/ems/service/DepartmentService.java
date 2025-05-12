package com.company.ems.service;

import com.company.ems.model.Department;
import com.company.ems.repository.DepartmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for Department management
 */
public interface DepartmentService {

    /**
     * Find all departments
     * @return List of all departments
     */
    List<Department> findAll();

    /**
     * Find a department by ID
     * @param id The department ID
     * @return Optional containing the department, or empty if not found
     */
    Optional<Department> findById(Long id);

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

    /**
     * Save a department
     * @param department The department to save
     * @return The saved department
     */
    Department save(Department department);

    /**
     * Delete a department by ID
     * @param id The department ID
     */
    void delete(Long id);
}

/**
 * Implementation of the DepartmentService interface
 */
