package com.company.ems.service;

import com.company.ems.dto.EmployeeDTO;
import com.company.ems.model.Employee;
import com.company.ems.model.User;
import com.company.ems.repository.EmployeeRepository;
import com.company.ems.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;

    @Autowired
    public EmployeeService(EmployeeRepository employeeRepository, UserRepository userRepository) {
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
    }

    public List<Employee> findAll() {
        return employeeRepository.findAll();
    }

    public List<Employee> findAllActive() {
        return employeeRepository.findByActiveTrue();
    }

    public Optional<Employee> findById(Long id) {
        return employeeRepository.findById(id);
    }

    public Optional<Employee> findByEmail(String email) {
        return employeeRepository.findByEmail(email);
    }

    public List<Employee> findByDepartment(String department) {
        return employeeRepository.findByDepartment(department);
    }

    public List<Employee> searchByName(String name) {
        return employeeRepository.findByFullNameContaining(name);
    }

    @Transactional
    public Employee save(EmployeeDTO employeeDTO) {
        Employee employee = new Employee();
        mapDtoToEntity(employeeDTO, employee);

        // Link to user if userId is provided
        if (employeeDTO.getUserId() != null) {
            userRepository.findById(employeeDTO.getUserId())
                    .ifPresent(employee::setUser);
        }

        return employeeRepository.save(employee);
    }

    @Transactional
    public Employee update(Long id, EmployeeDTO employeeDTO) {
        return employeeRepository.findById(id)
                .map(existingEmployee -> {
                    mapDtoToEntity(employeeDTO, existingEmployee);

                    // Update user link if provided
                    if (employeeDTO.getUserId() != null) {
                        User currentUser = existingEmployee.getUser();

                        // Only update if different from current user
                        if (currentUser == null || !currentUser.getId().equals(employeeDTO.getUserId())) {
                            userRepository.findById(employeeDTO.getUserId())
                                    .ifPresent(existingEmployee::setUser);
                        }
                    } else {
                        // Remove user link if null
                        existingEmployee.setUser(null);
                    }

                    return employeeRepository.save(existingEmployee);
                })
                .orElseThrow(() -> new IllegalArgumentException("Employee not found with ID: " + id));
    }

    @Transactional
    public void delete(Long id) {
        employeeRepository.deleteById(id);
    }

    @Transactional
    public void softDelete(Long id) {
        employeeRepository.findById(id)
                .ifPresent(employee -> {
                    employee.setActive(false);
                    employeeRepository.save(employee);
                });
    }

    public long countActiveEmployees() {
        return employeeRepository.countActiveEmployees();
    }

    // Convert Employee entity to EmployeeDTO
    public EmployeeDTO convertToDTO(Employee employee) {
        EmployeeDTO dto = new EmployeeDTO();
        dto.setId(employee.getId());
        dto.setFirstName(employee.getFirstName());
        dto.setLastName(employee.getLastName());
        dto.setEmail(employee.getEmail());
        dto.setPhoneNumber(employee.getPhoneNumber());
        dto.setPosition(employee.getPosition());
        dto.setDepartment(employee.getDepartment());
        dto.setDateOfBirth(employee.getDateOfBirth());
        dto.setHireDate(employee.getHireDate());
        dto.setSalary(employee.getSalary());
        dto.setAddress(employee.getAddress());
        dto.setNotes(employee.getNotes());
        dto.setActive(employee.isActive());

        // Set user ID if user is linked
        if (employee.getUser() != null) {
            dto.setUserId(employee.getUser().getId());
        }

        return dto;
    }

    // Map DTO fields to entity
    private void mapDtoToEntity(EmployeeDTO dto, Employee entity) {
        entity.setFirstName(dto.getFirstName());
        entity.setLastName(dto.getLastName());
        entity.setEmail(dto.getEmail());
        entity.setPhoneNumber(dto.getPhoneNumber());
        entity.setPosition(dto.getPosition());
        entity.setDepartment(dto.getDepartment());
        entity.setDateOfBirth(dto.getDateOfBirth());
        entity.setHireDate(dto.getHireDate());
        entity.setSalary(dto.getSalary());
        entity.setAddress(dto.getAddress());
        entity.setNotes(dto.getNotes());
        entity.setActive(dto.isActive());
    }

    // Get list of all departments
    public List<String> getAllDepartments() {
        return employeeRepository.findAll().stream()
                .map(Employee::getDepartment)
                .filter(department -> department != null && !department.isEmpty())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Updates all employees' department from oldName to newName
     * This is used when a department name is changed
     *
     * @param oldName The old department name
     * @param newName The new department name
     */
    @Transactional
    public void updateDepartmentName(String oldName, String newName) {
        List<Employee> employees = employeeRepository.findByDepartment(oldName);
        for (Employee employee : employees) {
            employee.setDepartment(newName);
            employeeRepository.save(employee);
        }
    }

    /**
     * Clears the department field for all employees in the specified department
     * This is used when a department is deleted
     *
     * @param departmentName The name of the department to clear
     */
    @Transactional
    public void clearDepartment(String departmentName) {
        List<Employee> employees = employeeRepository.findByDepartment(departmentName);
        for (Employee employee : employees) {
            employee.setDepartment(null);
            employeeRepository.save(employee);
        }
    }
}