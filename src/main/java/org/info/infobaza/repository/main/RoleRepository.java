package org.info.infobaza.repository.main;

import org.info.infobaza.model.main.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface  RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);
}