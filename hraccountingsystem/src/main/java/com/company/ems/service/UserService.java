package com.company.ems.service;

import com.company.ems.dto.UserDTO;
import com.company.ems.model.Role;
import com.company.ems.model.User;
import com.company.ems.repository.RoleRepository;
import com.company.ems.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        System.out.println("Attempting to load user: " + username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    System.out.println("User not found: " + username);
                    return new UsernameNotFoundException("User not found with username: " + username);
                });
        System.out.println("User found: " + user.getUsername());
        System.out.println("Password (encoded): " + user.getPassword());
        System.out.println("Is enabled: " + user.isEnabled());
        System.out.println("Is account non-expired: " + user.isAccountNonExpired());
        System.out.println("Is account non-locked: " + user.isAccountNonLocked());
        System.out.println("Is credentials non-expired: " + user.isCredentialsNonExpired());
        System.out.println("User roles: " + user.getRoles().stream()
                .map(role -> role.getName())
                .collect(Collectors.joining(", ")));
        System.out.println("Authorities: " + user.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .collect(Collectors.joining(", ")));
        return user;
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Find all users that have a specific role
     * @param roleName The name of the role to search for
     * @return A list of users with the specified role
     */
    public List<User> findByRoleName(String roleName) {
        List<User> allUsers = userRepository.findAll();
        return allUsers.stream()
                .filter(user -> user.getRoles().stream()
                        .anyMatch(role -> role.getName().equals(roleName)))
                .collect(Collectors.toList());
    }

    /**
     * Update the role name for all users that have the old role
     * @param oldRoleName The old role name
     * @param newRoleName The new role name
     */
    @Transactional
    public void updateUserRoles(String oldRoleName, String newRoleName) {
        // First find the old and new role objects
        Role oldRole = roleRepository.findByName(oldRoleName)
                .orElseThrow(() -> new IllegalArgumentException("Old role not found: " + oldRoleName));

        Role newRole = roleRepository.findByName(newRoleName)
                .orElse(null);

        // If the new role doesn't exist yet, create it
        if (newRole == null) {
            newRole = new Role(newRoleName);
            roleRepository.save(newRole);
        }

        final Role finalNewRole = newRole; // Need final reference for lambda

        // Find all users with the old role
        List<User> usersWithOldRole = findByRoleName(oldRoleName);

        // For each user, replace the old role with the new one
        for (User user : usersWithOldRole) {
            List<Role> updatedRoles = user.getRoles().stream()
                    .map(role -> role.getName().equals(oldRoleName) ? finalNewRole : role)
                    .collect(Collectors.toList());

            user.setRoles(updatedRoles);
            userRepository.save(user);
        }
    }

    @Transactional
    public User save(UserDTO userDTO) {
        User user = new User();
        user.setFirstName(userDTO.getFirstName());
        user.setLastName(userDTO.getLastName());
        user.setUsername(userDTO.getUsername());
        user.setEmail(userDTO.getEmail());
        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        user.setEnabled(userDTO.isEnabled());

        // Set default role to USER if none provided
        List<Role> roles = new ArrayList<>();
        if (userDTO.getRoles() != null && !userDTO.getRoles().isEmpty()) {
            for (String roleName : userDTO.getRoles()) {
                roleRepository.findByName(roleName)
                        .ifPresent(roles::add);
            }
        }

        // If no valid roles found or none provided, assign default USER role
        if (roles.isEmpty()) {
            roleRepository.findByName("ROLE_USER")
                    .ifPresent(roles::add);
        }

        user.setRoles(roles);
        return userRepository.save(user);
    }

    @Transactional
    public User update(Long id, UserDTO userDTO) {
        return userRepository.findById(id)
                .map(existingUser -> {
                    existingUser.setFirstName(userDTO.getFirstName());
                    existingUser.setLastName(userDTO.getLastName());
                    existingUser.setEmail(userDTO.getEmail());
                    existingUser.setEnabled(userDTO.isEnabled());

                    // Update username if provided
                    if (userDTO.getUsername() != null && !userDTO.getUsername().isEmpty()) {
                        existingUser.setUsername(userDTO.getUsername());
                    }

                    // Update password if specified
                    if (userDTO.isUpdatePassword() && userDTO.getPassword() != null
                            && !userDTO.getPassword().isEmpty()) {
                        existingUser.setPassword(passwordEncoder.encode(userDTO.getPassword()));
                    }

                    // Update roles if provided
                    if (userDTO.getRoles() != null && !userDTO.getRoles().isEmpty()) {
                        List<Role> roles = new ArrayList<>();
                        for (String roleName : userDTO.getRoles()) {
                            roleRepository.findByName(roleName)
                                    .ifPresent(roles::add);
                        }

                        // Only update roles if valid roles were found
                        if (!roles.isEmpty()) {
                            existingUser.setRoles(roles);
                        }
                    }

                    return userRepository.save(existingUser);
                })
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + id));
    }

    @Transactional
    public void delete(Long id) {
        userRepository.deleteById(id);
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    // Convert User entity to UserDTO
    public UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setEnabled(user.isEnabled());

        // Convert roles to role names
        if (user.getRoles() != null) {
            dto.setRoles(user.getRoles().stream()
                    .map(Role::getName)
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    // Initialize the roles in the database
    @Transactional
    public void initRoles() {
        if (roleRepository.count() == 0) {
            roleRepository.save(new Role("ROLE_ADMIN"));
            roleRepository.save(new Role("ROLE_USER"));
            roleRepository.save(new Role("ROLE_HR"));
        }
    }

    // Initialize the admin user
    @Transactional
    public void initAdminUser() {
        if (!userRepository.existsByUsername("admin")) {
            UserDTO adminDto = new UserDTO();
            adminDto.setFirstName("Admin");
            adminDto.setLastName("User");
            adminDto.setUsername("admin");
            adminDto.setEmail("admin@example.com");
            adminDto.setPassword("admin123"); // In production, use a secure password
            List<String> roles = new ArrayList<>();
            roles.add("ROLE_ADMIN");
            adminDto.setRoles(roles);
            save(adminDto);
        }
    }
}