package org.info.infobaza.repository.ser;

import org.info.infobaza.model.ser.UserSer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserSerRepository extends JpaRepository<UserSer, Long> {

}
