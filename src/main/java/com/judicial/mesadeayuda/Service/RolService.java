package com.judicial.mesadeayuda.Service;

import com.judicial.mesadeayuda.Repositories.RolRepository;
import com.judicial.mesadeayuda.DTO.Response.RolResponseDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service para Roles.
 * Solo lectura: los roles son datos de configuración estática (Admin, Operario, Técnico).
 * Usado principalmente para popular selects en el frontend.
 */
@Service
@Transactional(readOnly = true)
public class RolService {

    private final RolRepository rolRepository;

    public RolService(RolRepository rolRepository) {
        this.rolRepository = rolRepository;
    }

    public List<RolResponseDTO> listarTodos() {
        return rolRepository.findAll().stream()
                .map(rol -> RolResponseDTO.builder()
                        .id(rol.getId())
                        .nombre(rol.getNombre())
                        .descripcion(rol.getDescripcion())
                        .build())
                .collect(Collectors.toList());
    }
}