package com.propease.repository;

import com.propease.domain.Enums;
import com.propease.domain.Role;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
  Optional<Role> findByName(Enums.RoleName name);
}
