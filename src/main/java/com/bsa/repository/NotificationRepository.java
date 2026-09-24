package com.bsa.repository;

import com.bsa.model.Notification;
import com.bsa.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserOrderByDueDateAsc(User user);
}
