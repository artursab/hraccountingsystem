package com.company.ems.controller;

import com.company.ems.dto.UserDTO;
import com.company.ems.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;

    @Autowired
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        UserDTO userDTO = new UserDTO();
        model.addAttribute("user", userDTO);
        return "auth/register";
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("user") UserDTO userDTO,
                               BindingResult result,
                               RedirectAttributes redirectAttributes) {
        // Check for validation errors
        if (result.hasErrors()) {
            return "auth/register";
        }

        // Check if username or email already exists
        if (userService.existsByUsername(userDTO.getUsername())) {
            result.rejectValue("username", "error.user", "Username is already taken");
            return "auth/register";
        }

        if (userService.existsByEmail(userDTO.getEmail())) {
            result.rejectValue("email", "error.user", "Email is already registered");
            return "auth/register";
        }

        // Set default role to USER
        List<String> roles = new ArrayList<>();
        roles.add("USER");
        userDTO.setRoles(roles);

        // Register the user
        userService.save(userDTO);

        // Redirect to login page with success message
        redirectAttributes.addFlashAttribute("successMessage", "Registration successful! Please login.");
        return "redirect:/auth/login";
    }
}