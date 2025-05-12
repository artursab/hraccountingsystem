package com.company.ems.controller;

import com.company.ems.model.Role;
import com.company.ems.model.User;
import com.company.ems.service.RoleService;
import com.company.ems.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/roles")
@PreAuthorize("hasRole('ADMIN')")
public class RoleController {

    private final RoleService roleService;
    private final UserService userService;

    @Autowired
    public RoleController(RoleService roleService, UserService userService) {
        this.roleService = roleService;
        this.userService = userService;
    }

    /**
     * Display all roles
     */
    @GetMapping
    public String listRoles(Model model) {
        List<Role> roles = roleService.findAll();

        // Count users per role
        Map<String, Integer> roleCounts = new HashMap<>();

        // Get all users and count roles
        List<User> users = userService.findAll();
        for (User user : users) {
            for (Role role : user.getRoles()) {
                String roleName = role.getName();
                roleCounts.put(roleName, roleCounts.getOrDefault(roleName, 0) + 1);
            }
        }

        model.addAttribute("roles", roles);
        model.addAttribute("roleCounts", roleCounts);

        return "admin/roles";
    }

    /**
     * Add a new role
     */
    @PostMapping("/add")
    public String addRole(@RequestParam String name,
                          @RequestParam(defaultValue = "false") boolean addPrefix,
                          RedirectAttributes redirectAttributes) {
        try {
            // Format the role name
            String roleName = formatRoleName(name, addPrefix);

            // Check if role already exists
            if (roleService.existsByName(roleName)) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Role '" + roleName + "' already exists!");
                return "redirect:/admin/roles";
            }

            Role role = new Role();
            role.setName(roleName);

            roleService.save(role);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Role '" + roleName + "' has been created successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Error creating role: " + e.getMessage());
        }

        return "redirect:/admin/roles";
    }

    /**
     * Update an existing role
     */
    @PostMapping("/update")
    public String updateRole(@RequestParam Long id,
                             @RequestParam String name,
                             @RequestParam(defaultValue = "false") boolean addPrefix,
                             RedirectAttributes redirectAttributes) {
        try {
            // Format the role name
            String roleName = formatRoleName(name, addPrefix);

            // Check if role exists
            Role role = roleService.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Role not found"));

            // Check if another role with the same name exists
            if (!role.getName().equals(roleName) && roleService.existsByName(roleName)) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Role '" + roleName + "' already exists!");
                return "redirect:/admin/roles";
            }

            // Capture old name for messaging
            String oldName = role.getName();

            // Update role name
            role.setName(roleName);

            roleService.save(role);

            // Update users with this role
            userService.updateUserRoles(oldName, roleName);

            redirectAttributes.addFlashAttribute("successMessage",
                    "Role '" + oldName + "' has been updated to '" + roleName + "' successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Error updating role: " + e.getMessage());
        }

        return "redirect:/admin/roles";
    }

    /**
     * Delete a role
     */
    @PostMapping("/delete")
    public String deleteRole(@RequestParam Long id, RedirectAttributes redirectAttributes) {
        try {
            // Check if role exists
            Role role = roleService.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Role not found"));

            // Check if any users have this role
            List<User> usersWithRole = userService.findByRoleName(role.getName());

            if (!usersWithRole.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Cannot delete role '" + role.getName() + "' because it is assigned to " +
                                usersWithRole.size() + " users. Remove the role from all users first.");
                return "redirect:/admin/roles";
            }

            roleService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Role '" + role.getName() + "' has been deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Error deleting role: " + e.getMessage());
        }

        return "redirect:/admin/roles";
    }

    /**
     * Helper method to format role names according to Spring Security conventions
     */
    private String formatRoleName(String name, boolean addPrefix) {
        // Convert to uppercase
        String formattedName = name.toUpperCase();

        // Add prefix if specified and not already present
        if (addPrefix && !formattedName.startsWith("ROLE_")) {
            formattedName = "ROLE_" + formattedName;
        }

        return formattedName;
    }
}