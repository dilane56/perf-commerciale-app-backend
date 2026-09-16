package com.cbcbourse.backend.role;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.cbcbourse.backend.common.exception.DuplicateResourceException;
import com.cbcbourse.backend.common.exception.ResourceNotFoundException;
import com.cbcbourse.backend.common.exception.RoleInUseException;
import com.cbcbourse.backend.permission.Permission;
import com.cbcbourse.backend.permission.PermissionRepository;
import com.cbcbourse.backend.role.dto.CreateRoleRequest;
import com.cbcbourse.backend.role.dto.UpdateRoleRequest;
import com.cbcbourse.backend.user.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;

    public RoleService(RoleRepository roleRepository, PermissionRepository permissionRepository,
                        UserRepository userRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<Role> findAll() {
        return roleRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Role findById(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role introuvable: " + id));
    }

    public Role create(CreateRoleRequest request) {
        if (roleRepository.existsByName(request.name())) {
            throw new DuplicateResourceException("Un role avec ce nom existe deja: " + request.name());
        }
        Role role = new Role();
        role.setName(request.name());
        role.setDescription(request.description());
        role.setPermissions(resolvePermissions(request.permissionIds()));
        return roleRepository.save(role);
    }

    public Role update(Long id, UpdateRoleRequest request) {
        Role role = findById(id);
        if (!role.getName().equals(request.name()) && roleRepository.existsByName(request.name())) {
            throw new DuplicateResourceException("Un role avec ce nom existe deja: " + request.name());
        }
        role.setName(request.name());
        role.setDescription(request.description());
        return roleRepository.save(role);
    }

    public Role assignPermissions(Long id, Set<Long> permissionIds) {
        Role role = findById(id);
        role.setPermissions(resolvePermissions(permissionIds));
        return roleRepository.save(role);
    }

    public void delete(Long id) {
        Role role = findById(id);
        if (userRepository.existsByRoles_Id(role.getId())) {
            throw new RoleInUseException("Impossible de supprimer le role '" + role.getName()
                    + "' : il est encore assigne a des utilisateurs");
        }
        roleRepository.delete(role);
    }

    private Set<Permission> resolvePermissions(Set<Long> permissionIds) {
        if (permissionIds == null || permissionIds.isEmpty()) {
            return new HashSet<>();
        }
        List<Permission> found = permissionRepository.findAllById(permissionIds);
        if (found.size() != permissionIds.size()) {
            throw new ResourceNotFoundException("Une ou plusieurs permissions sont introuvables");
        }
        return new HashSet<>(found);
    }
}
