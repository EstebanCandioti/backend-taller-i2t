package com.judicial.mesadeayuda.Repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.judicial.mesadeayuda.Entities.SoftwareJuzgado;

@Repository
public interface SoftwareJuzgadoRepository extends JpaRepository<SoftwareJuzgado, Integer> {

    List<SoftwareJuzgado> findBySoftwareIdAndEliminadoFalse(Integer softwareId);

    long countBySoftwareIdAndEliminadoFalse(Integer softwareId);

    List<SoftwareJuzgado> findByJuzgadoIdAndEliminadoFalse(Integer juzgadoId);

    Optional<SoftwareJuzgado> findBySoftwareIdAndJuzgadoIdAndEliminadoFalse(Integer softwareId, Integer juzgadoId);

    @Query(value = """
        SELECT * FROM software_juzgado
        WHERE software_id = :softwareId
          AND juzgado_id = :juzgadoId
        LIMIT 1
    """, nativeQuery = true)
    Optional<SoftwareJuzgado> findAnyBySoftwareIdAndJuzgadoId(Integer softwareId, Integer juzgadoId);
}
