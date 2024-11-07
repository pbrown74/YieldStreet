package com.yieldstreet.repository;

import com.yieldstreet.entity.AccreditationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccreditationHistoryRepository extends JpaRepository<AccreditationHistory, String> {

    List<AccreditationHistory> findByAccreditationIdIs(String accreditationId);

}