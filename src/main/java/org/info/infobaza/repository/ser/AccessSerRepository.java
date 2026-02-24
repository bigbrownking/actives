package org.info.infobaza.repository.ser;

import org.info.infobaza.model.ser.AccessSer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccessSerRepository extends JpaRepository<AccessSer, Long> {
    Optional<AccessSer> findById(Long userId);
}
