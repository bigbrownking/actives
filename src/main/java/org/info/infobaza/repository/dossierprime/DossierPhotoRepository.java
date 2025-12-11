package org.info.infobaza.repository.dossierprime;

import org.info.infobaza.model.dossierprime.PhotoDb;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DossierPhotoRepository extends JpaRepository<PhotoDb, Long> {
    @Query(value= "select distinct * from import_fl.photo where iin = ?1 order by \"date\" desc", nativeQuery = true)
    List<PhotoDb> findAllByIin(String iin);

}
