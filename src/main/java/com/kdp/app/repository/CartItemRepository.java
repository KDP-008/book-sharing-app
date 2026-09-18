package com.kdp.app.repository;

import com.kdp.app.model.Book;
import com.kdp.app.model.CartItem;
import com.kdp.app.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByUser(User user);
    void deleteByUser(User user);
    boolean existsByUserAndBook(User user, Book book);
    Optional<CartItem> findByIdAndUserId(Long id, Long userId);
}
