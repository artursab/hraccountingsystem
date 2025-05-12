package com.company.ems.controller;

import com.company.ems.model.Department;
import com.company.ems.model.Employee;
import com.company.ems.service.DepartmentService;
import com.company.ems.service.EmployeeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/departments")
@PreAuthorize("hasRole('ADMIN')")
public class DepartmentController {

    private static final Logger logger = LoggerFactory.getLogger(DepartmentController.class);

    private final DepartmentService departmentService;
    private final EmployeeService employeeService;

    @Autowired
    public DepartmentController(DepartmentService departmentService, EmployeeService employeeService) {
        this.departmentService = departmentService;
        this.employeeService = employeeService;
    }

    @GetMapping
    public String listDepartments(Model model) {
        // Log entry into the method for debugging
        logger.info("Entering listDepartments method");

        List<Department> departments = departmentService.findAll();
        logger.info("Retrieved {} departments from database", departments.size());

        // Log department details for debugging
        departments.forEach(dept ->
                logger.info("Department: ID={}, Name={}, Description={}",
                        dept.getId(), dept.getName(), dept.getDescription()));

        // Count employees per department
        List<Employee> activeEmployees = employeeService.findAllActive();
        Map<String, Long> departmentCounts = activeEmployees.stream()
                .filter(employee -> employee.getDepartment() != null)
                .collect(Collectors.groupingBy(
                        Employee::getDepartment,
                        Collectors.counting()
                ));

        // Log department counts
        departmentCounts.forEach((dept, count) ->
                logger.info("Department: {} has {} employees", dept, count));

        model.addAttribute("departments", departments);
        model.addAttribute("departmentCounts", departmentCounts);

        logger.info("Exiting listDepartments method");
        return "admin/departments";
    }

    @PostMapping("/add")
    public String addDepartment(@RequestParam String name,
                                @RequestParam(required = false) String description,
                                RedirectAttributes redirectAttributes) {
        // Log entry into the method for debugging
        logger.info("Entering addDepartment method with name={}, description={}", name, description);

        try {
            // Check if department already exists
            if (departmentService.existsByName(name)) {
                logger.warn("Department with name '{}' already exists", name);
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Department '" + name + "' already exists!");
                return "redirect:/admin/departments";
            }

            Department department = new Department();
            department.setName(name);
            department.setDescription(description);

            Department savedDepartment = departmentService.save(department);
            logger.info("Department created successfully: ID={}, Name={}",
                    savedDepartment.getId(), savedDepartment.getName());

            redirectAttributes.addFlashAttribute("successMessage",
                    "Department '" + name + "' has been created successfully!");
        } catch (Exception e) {
            logger.error("Error creating department: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Error creating department: " + e.getMessage());
        }

        logger.info("Exiting addDepartment method");
        return "redirect:/admin/departments";
    }

    @PostMapping("/update")
    public String updateDepartment(@RequestParam Long id,
                                   @RequestParam String name,
                                   @RequestParam(required = false) String description,
                                   RedirectAttributes redirectAttributes) {
        // Log entry into the method for debugging
        logger.info("Entering updateDepartment method with id={}, name={}, description={}",
                id, name, description);

        try {
            // Check if department exists
            Department department = departmentService.findById(id)
                    .orElseThrow(() -> {
                        logger.error("Department with ID {} not found", id);
                        return new IllegalArgumentException("Department not found");
                    });

            logger.info("Found department for update: ID={}, OldName={}",
                    department.getId(), department.getName());

            // Check if another department with the same name exists
            if (!department.getName().equals(name) && departmentService.existsByName(name)) {
                logger.warn("Another department with name '{}' already exists", name);
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Department '" + name + "' already exists!");
                return "redirect:/admin/departments";
            }

            // Capture old name for message
            String oldName = department.getName();

            // Update fields
            department.setName(name);
            department.setDescription(description);

            Department updatedDepartment = departmentService.save(department);
            logger.info("Department updated successfully: ID={}, NewName={}",
                    updatedDepartment.getId(), updatedDepartment.getName());

            // Update employee department names if name changed
            if (!oldName.equals(name)) {
                logger.info("Updating department name from '{}' to '{}' for all employees",
                        oldName, name);
                employeeService.updateDepartmentName(oldName, name);
            }

            redirectAttributes.addFlashAttribute("successMessage",
                    "Department '" + oldName + "' has been updated to '" + name + "' successfully!");
        } catch (Exception e) {
            logger.error("Error updating department: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Error updating department: " + e.getMessage());
        }

        logger.info("Exiting updateDepartment method");
        return "redirect:/admin/departments";
    }

    @PostMapping("/delete")
    public String deleteDepartment(@RequestParam Long id, RedirectAttributes redirectAttributes) {
        // Log entry into the method for debugging
        logger.info("Entering deleteDepartment method with id={}", id);

        try {
            // Check if department exists
            Department department = departmentService.findById(id)
                    .orElseThrow(() -> {
                        logger.error("Department with ID {} not found", id);
                        return new IllegalArgumentException("Department not found");
                    });

            logger.info("Found department for deletion: ID={}, Name={}",
                    department.getId(), department.getName());

            // Get employees in this department
            List<Employee> employeesInDepartment = employeeService.findByDepartment(department.getName());
            logger.info("Found {} employees in department '{}'",
                    employeesInDepartment.size(), department.getName());

            // If employees exist in this department, update them first
            if (!employeesInDepartment.isEmpty()) {
                logger.info("Clearing department '{}' from all employees", department.getName());
                // Update employees to have no department
                employeeService.clearDepartment(department.getName());
            }

            logger.info("Deleting department with ID={}, Name={}",
                    department.getId(), department.getName());
            departmentService.delete(id);

            redirectAttributes.addFlashAttribute("successMessage",
                    "Department '" + department.getName() + "' has been deleted successfully!");
        } catch (Exception e) {
            logger.error("Error deleting department: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Error deleting department: " + e.getMessage());
        }

        logger.info("Exiting deleteDepartment method");
        return "redirect:/admin/departments";
    }
}