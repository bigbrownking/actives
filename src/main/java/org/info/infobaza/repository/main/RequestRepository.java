package org.info.infobaza.repository.main;

import org.info.infobaza.model.main.Request;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface RequestRepository extends JpaRepository<Request, Long> {
   @Query("SELECT r FROM Request r " +
           "WHERE r.user.id = :userId " +
           "AND (:iin IS NULL OR r.iinBin = :iin) " +
           "AND (:dateFrom IS NULL OR r.timestamp >= :dateFrom) " +
           "AND (:dateTo IS NULL OR r.timestamp <= :dateTo)")
   Page<Request> getRequestsBy(
           @Param("userId") Long userId,
           @Param("iin") String iin,
           @Param("dateFrom") LocalDate dateFrom,
           @Param("dateTo") LocalDate dateTo,
           Pageable pageable);
}