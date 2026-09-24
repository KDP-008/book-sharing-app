package com.bsa.repository;

import com.bsa.model.ReservationAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReservationAuditLogRepository extends JpaRepository<ReservationAuditLog, Long> {
    List<ReservationAuditLog> findByReservationIdOrderByChangedAtAsc(Long reservationId);
}
