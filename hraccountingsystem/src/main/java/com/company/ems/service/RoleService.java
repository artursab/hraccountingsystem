package com.company.ems.service;

import com.company.ems.model.Role;
import com.company.ems.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for Role management
 */
public interface RoleService {

    /**
     * Find all roles
     * @return List of all roles
     */
    List<Role> findAll();

    /**
     * Find a role by ID
     * @param id The role ID
     * @return Optional containing the role, or empty if not found
     */
    Optional<Role> findById(Long id);

    /**
     * Find a role by name
     * @param name The role name
     * @return Optional containing the role, or empty if not found
     */
    Optional<Role> findByName(String name);

    /**
     * Check if a role with the given name exists
     * @param name The role name
     * @return true if exists, false otherwise
     */
    boolean existsByName(String name);

    /**
     * Save a role
     * @param role The role to save
     * @return The saved role
     */
    Role save(Role role);

    /**
     * Delete a role by ID
     * @param id The role ID
     */
    void delete(Long id);
}

/**
 * Implementation of the RoleService interface
 */
