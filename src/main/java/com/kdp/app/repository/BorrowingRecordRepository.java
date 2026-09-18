package com.kdp.app.repository;

import com.kdp.app.model.BorrowingRecord;
import com.kdp.app.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BorrowingRecordRepository extends JpaRepository<BorrowingRecord, Long> {
    List<BorrowingRecord> findByBorrower(User borrower);

    List<BorrowingRecord> findByOwner(User owner);

    Optional<BorrowingRecord> findByBookIdAndBorrowerIdAndActiveTrue(Long bookId, Long borrowerId);

    Optional<BorrowingRecord> findByBookIdAndActiveTrue(Long bookId);
}
