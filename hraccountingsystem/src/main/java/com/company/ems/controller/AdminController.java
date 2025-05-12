package com.company.ems.controller;

import com.company.ems.dto.EmployeeDTO;
import com.company.ems.dto.UserDTO;
import com.company.ems.model.Employee;
import com.company.ems.model.TimeEntry;
import com.company.ems.model.User;
import com.company.ems.service.EmployeeService;
import com.company.ems.service.TimeTrackingService;
import com.company.ems.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;
    private final EmployeeService employeeService;
    private final TimeTrackingService timeTrackingService;

    @Autowired
    public AdminController(UserService userService,
                           EmployeeService employeeService,
                           TimeTrackingService timeTrackingService) {
        this.userService = userService;
        this.employeeService = employeeService;
        this.timeTrackingService = timeTrackingService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // Dashboard statistics
        long totalEmployees = employeeService.countActiveEmployees();
        List<String> departments = employeeService.getAllDepartments();

        // Get department distribution
        Map<String, Long> departmentCounts = employeeService.findAllActive().stream()
                .collect(Collectors.groupingBy(
                        employee -> employee.getDepartment() != null ? employee.getDepartment() : "Unassigned",
                        Collectors.counting()
                ));

        // Get recent time entries
        List<TimeEntry> recentTimeEntries = timeTrackingService.findAll().stream()
                .sorted((e1, e2) -> e2.getCreatedAt().compareTo(e1.getCreatedAt()))
                .limit(10)
                .toList();

        // Add attributes to model
        model.addAttribute("totalEmployees", totalEmployees);
        model.addAttribute("departments", departments);
        model.addAttribute("departmentCounts", departmentCounts);
        model.addAttribute("recentTimeEntries", recentTimeEntries);

        return "admin/dashboard";
    }

    // ========== User Management ==========

    @GetMapping("/users")
    public String listUsers(Model model) {
        List<User> users = userService.findAll();
        model.addAttribute("users", users);
        return "admin/users";
    }

    @GetMapping("/users/new")
    public String createUserForm(Model model) {
        UserDTO userDTO = new UserDTO();
        // Fix the attribute name - was "templates/user", should be "user"
        model.addAttribute("user", userDTO);
        model.addAttribute("roles", List.of("ADMIN", "USER", "MANAGER"));
        return "admin/user-form";
    }

    @PostMapping("/users/new")
    public String createUser(@Valid @ModelAttribute("user") UserDTO userDTO,
                             BindingResult result,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        // Check for validation errors
        if (result.hasErrors()) {
            model.addAttribute("roles", List.of("ADMIN", "USER", "MANAGER"));
            return "admin/user-form";
        }

        // Check if username or email already exists
        if (userService.existsByUsername(userDTO.getUsername())) {
            result.rejectValue("username", "error.user", "Username is already taken");
            model.addAttribute("roles", List.of("ADMIN", "USER", "MANAGER"));
            return "admin/user-form";
        }

        if (userService.existsByEmail(userDTO.getEmail())) {
            result.rejectValue("email", "error.user", "Email is already registered");
            model.addAttribute("roles", List.of("ADMIN", "USER", "MANAGER"));
            return "admin/user-form";
        }

        // Save the user
        userService.save(userDTO);

        redirectAttributes.addFlashAttribute("successMessage", "User created successfully!");
        return "redirect:/admin/users";
    }

    @GetMapping("/users/edit/{id}")
    public String editUserForm(@PathVariable Long id, Model model) {
        User user = userService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid user ID: " + id));

        UserDTO userDTO = userService.convertToDTO(user);
        userDTO.setUpdatePassword(false); // Don't update password by default

        // Fix the attribute name - was "templates/user", should be "user"
        model.addAttribute("user", userDTO);
        model.addAttribute("roles", List.of("ADMIN", "USER", "MANAGER"));
        return "admin/user-form";
    }

    @PostMapping("/users/edit/{id}")
    public String updateUser(@PathVariable Long id,
                             @Valid @ModelAttribute("user") UserDTO userDTO,
                             BindingResult result,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        // Check for validation errors
        if (result.hasErrors()) {
            model.addAttribute("roles", List.of("ADMIN", "USER", "MANAGER"));
            return "admin/user-form";
        }

        // Update the user
        userService.update(id, userDTO);

        redirectAttributes.addFlashAttribute("successMessage", "User updated successfully!");
        return "redirect:/admin/users";
    }

    @GetMapping("/users/delete/{id}")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        userService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "User deleted successfully!");
        return "redirect:/admin/users";
    }

    // ========== Employee Management ==========

    @GetMapping("/employees")
    public String listEmployees(Model model) {
        List<Employee> employees = employeeService.findAll();
        model.addAttribute("employees", employees);
        return "admin/employees";
    }

    @GetMapping("/employees/new")
    public String createEmployeeForm(Model model) {
        EmployeeDTO employeeDTO = new EmployeeDTO();

        // Add available users for linking
        List<User> availableUsers = userService.findAll();

        model.addAttribute("employee", employeeDTO);
        model.addAttribute("departments", employeeService.getAllDepartments());
        model.addAttribute("availableUsers", availableUsers);

        return "admin/employee-form";
    }

    @PostMapping("/employees/new")
    public String createEmployee(@Valid @ModelAttribute("employee") EmployeeDTO employeeDTO,
                                 BindingResult result,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        // Check for validation errors
        if (result.hasErrors()) {
            model.addAttribute("departments", employeeService.getAllDepartments());
            model.addAttribute("availableUsers", userService.findAll());
            return "admin/employee-form";
        }

        // Check if email already exists
        if (employeeService.findByEmail(employeeDTO.getEmail()).isPresent()) {
            result.rejectValue("email", "error.employee", "Email is already registered");
            model.addAttribute("departments", employeeService.getAllDepartments());
            model.addAttribute("availableUsers", userService.findAll());
            return "admin/employee-form";
        }

        // Save the employee
        employeeService.save(employeeDTO);

        redirectAttributes.addFlashAttribute("successMessage", "Employee created successfully!");
        return "redirect:/admin/employees";
    }

    @GetMapping("/employees/edit/{id}")
    public String editEmployeeForm(@PathVariable Long id, Model model) {
        Employee employee = employeeService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid employee ID: " + id));

        EmployeeDTO employeeDTO = employeeService.convertToDTO(employee);

        // Add available users for linking
        List<User> availableUsers = userService.findAll();

        model.addAttribute("employee", employeeDTO);
        model.addAttribute("departments", employeeService.getAllDepartments());
        model.addAttribute("availableUsers", availableUsers);

        return "admin/employee-form";
    }

    @PostMapping("/employees/edit/{id}")
    public String updateEmployee(@PathVariable Long id,
                                 @Valid @ModelAttribute("employee") EmployeeDTO employeeDTO,
                                 BindingResult result,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        // Check for validation errors
        if (result.hasErrors()) {
            model.addAttribute("departments", employeeService.getAllDepartments());
            model.addAttribute("availableUsers", userService.findAll());
            return "admin/employee-form";
        }

        // Update the employee
        employeeService.update(id, employeeDTO);

        redirectAttributes.addFlashAttribute("successMessage", "Employee updated successfully!");
        return "redirect:/admin/employees";
    }

    @GetMapping("/employees/delete/{id}")
    public String deleteEmployee(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        employeeService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Employee deleted successfully!");
        return "redirect:/admin/employees";
    }

    @GetMapping("/employees/deactivate/{id}")
    public String deactivateEmployee(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        employeeService.softDelete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Employee deactivated successfully!");
        return "redirect:/admin/employees";
    }

    // ========== Time Tracking Management ==========

    @GetMapping("/time-entries")
    public String listTimeEntries(Model model) {
        List<TimeEntry> timeEntries = timeTrackingService.findAll().stream()
                .sorted((e1, e2) -> e2.getCreatedAt().compareTo(e1.getCreatedAt()))
                .toList();

        model.addAttribute("timeEntries", timeEntries);
        return "admin/time-entries";
    }

    @GetMapping("/time-entries/approve/{id}")
    public String approveTimeEntry(@PathVariable Long id,
                                   @RequestParam Long approverId,
                                   RedirectAttributes redirectAttributes) {
        timeTrackingService.approveTimeEntry(id, approverId);
        redirectAttributes.addFlashAttribute("successMessage", "Time entry approved successfully!");
        return "redirect:/admin/time-entries";
    }

    @GetMapping("/time-entries/delete/{id}")
    public String deleteTimeEntry(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        timeTrackingService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Time entry deleted successfully!");
        return "redirect:/admin/time-entries";
    }
}