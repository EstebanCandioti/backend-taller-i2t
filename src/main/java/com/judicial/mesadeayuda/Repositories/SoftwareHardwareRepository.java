package com.judicial.mesadeayuda.Repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.judicial.mesadeayuda.Entities.SoftwareHardware;

@Repository
public interface SoftwareHardwareRepository extends JpaRepository<SoftwareHardware, Integer> {

    List<SoftwareHardware> findBySoftwareId(Integer softwareId);

    List<SoftwareHardware> findByHardwareId(Integer hardwareId);

    Optional<SoftwareHardware> findBySoftwareIdAndHardwareId(Integer softwareId, Integer hardwareId);

    @Query(value = """
        SELECT * FROM software_hardware
        WHERE software_id = :softwareId
          AND hardware_id = :hardwareId
        LIMIT 1
    """, nativeQuery = true)
    Optional<SoftwareHardware> findAnyBySoftwareIdAndHardwareId(Integer softwareId, Integer hardwareId);
}
