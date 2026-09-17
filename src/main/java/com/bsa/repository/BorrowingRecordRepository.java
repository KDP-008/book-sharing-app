package com.bsa.repository;

import com.bsa.model.BorrowingRecord;
import com.bsa.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BorrowingRecordRepository extends JpaRepository<BorrowingRecord, Long> {
    List<BorrowingRecord> findByBorrower(User borrower);

    List<BorrowingRecord> findByOwner(User owner);
}
