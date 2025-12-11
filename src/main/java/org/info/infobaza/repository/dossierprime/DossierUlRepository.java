package org.info.infobaza.repository.dossierprime;

import org.info.infobaza.model.dossierprime.MvUl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DossierUlRepository extends JpaRepository<MvUl, String> {
    @Query(value= "select * from imp_kfm_ul.mv_ul where mv_ul.bin = ?1 ORDER BY TO_DATE(org_reg_date, 'DD-MM-YYYY') DESC limit 1 ", nativeQuery = true)
    Optional<MvUl> getUlByBin(String bin);
}
