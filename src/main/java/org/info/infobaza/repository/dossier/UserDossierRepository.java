package org.info.infobaza.repository.dossier;


import org.info.infobaza.model.dossier.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UserDossierRepository extends JpaRepository<User, Long> {
    @Query(value = "select * FROM users where iin like ?1", nativeQuery = true)
    User findByUsernameTwo(String iin);

}
