package com.judicial.mesadeayuda.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.judicial.mesadeayuda.DTO.Response.ApiResponse;
import com.judicial.mesadeayuda.DTO.Response.RolResponseDTO;
import com.judicial.mesadeayuda.Service.RolService;

import java.util.List;

/**
 * Controller de Roles.
 * Solo lectura: usado para popular selects en el frontend
 * (ej: al crear/editar usuario, elegir el rol).
 *
 * Endpoints:
 *   GET /api/roles → Listar todos los roles
 *
 * Autorización: Admin y Operario.
 */
@RestController
@RequestMapping("/api/roles")
public class RolController {

    private final RolService rolService;

    public RolController(RolService rolService) {
        this.rolService = rolService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RolResponseDTO>>> listarTodos() {
        List<RolResponseDTO> roles = rolService.listarTodos();
        return ResponseEntity.ok(ApiResponse.success("Roles obtenidos", roles));
    }
}