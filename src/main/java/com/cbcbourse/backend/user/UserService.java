package com.cbcbourse.backend.user;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.cbcbourse.backend.common.exception.BusinessRuleException;
import com.cbcbourse.backend.common.exception.DuplicateResourceException;
import com.cbcbourse.backend.common.exception.ResourceNotFoundException;
import com.cbcbourse.backend.role.Role;
import com.cbcbourse.backend.role.RoleRepository;
import com.cbcbourse.backend.user.dto.CreateUserRequest;
import com.cbcbourse.backend.user.dto.UpdateUserRequest;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<User> findAll(String roleName, Boolean active) {
        Specification<User> spec = Specification.unrestricted();
        if (roleName != null && !roleName.isBlank()) {
            spec = spec.and(UserSpecifications.hasRoleName(roleName));
        }
        if (active != null) {
            spec = spec.and(UserSpecifications.isActive(active));
        }
        return userRepository.findAll(spec);
    }

    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable: " + id));
    }

    public User create(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Un utilisateur avec cet email existe deja: " + request.email());
        }
        User user = new User();
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setActive(true);
        user.setRoles(resolveRoles(request.roleIds()));
        user.setManager(resolveManager(null, request.managerId()));
        return userRepository.save(user);
    }

    public User update(Long id, UpdateUserRequest request) {
        User user = findById(id);
        if (!user.getEmail().equals(request.email()) && userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Un utilisateur avec cet email existe deja: " + request.email());
        }
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEmail(request.email());
        user.setRoles(resolveRoles(request.roleIds()));
        user.setManager(resolveManager(id, request.managerId()));
        return userRepository.save(user);
    }

    public User setActive(Long id, boolean active) {
        User user = findById(id);
        user.setActive(active);
        return userRepository.save(user);
    }

    public void resetPassword(Long id, String newPassword) {
        User user = findById(id);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    /** Un utilisateur ne peut pas etre son propre responsable ; managerId nul signifie "sans equipe". */
    private User resolveManager(Long userId, Long managerId) {
        if (managerId == null) {
            return null;
        }
        if (managerId.equals(userId)) {
            throw new BusinessRuleException("Un utilisateur ne peut pas etre son propre responsable");
        }
        return userRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("Responsable introuvable: " + managerId));
    }

    private Set<Role> resolveRoles(Set<Long> roleIds) {
        List<Role> found = roleRepository.findAllById(roleIds);
        if (found.size() != roleIds.size()) {
            throw new ResourceNotFoundException("Un ou plusieurs roles sont introuvables");
        }
        return new HashSet<>(found);
    }
}
