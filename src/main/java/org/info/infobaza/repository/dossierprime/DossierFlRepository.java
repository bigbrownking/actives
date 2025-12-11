package org.info.infobaza.repository.dossierprime;

import org.info.infobaza.model.dossierprime.MvFl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DossierFlRepository extends JpaRepository<MvFl, String> {
    @Query(
            value = "SELECT   mv_fl0_.* , \n               nat.\"RU_NAME\" AS nationality_ru_name\n \n               FROM  imp_kfm_fl.mv_fl mv_fl0_\n            INNER JOIN \n                dictionary.d_nationality_new as nat ON nat.\"ID\"::text = mv_fl0_.nationality_id \n            WHERE \n                mv_fl0_.iin = ?1",
            nativeQuery = true
    )
    List<MvFl> getUsersByLike(String iin);
}
