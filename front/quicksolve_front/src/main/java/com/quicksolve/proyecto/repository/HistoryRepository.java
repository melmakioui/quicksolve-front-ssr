package com.quicksolve.proyecto.repository;

import com.quicksolve.proyecto.entity.History;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistoryRepository extends JpaRepository<History, Long> {

    History findFirstByIncidenceIdOrderByChangeDateDesc(Long id);
    List<History> findByIncidenceIdOrderByChangeDateDesc(Long id);


}
