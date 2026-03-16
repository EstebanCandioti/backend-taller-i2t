package com.judicial.mesadeayuda.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.judicial.mesadeayuda.DTO.Response.ApiResponse;
import com.judicial.mesadeayuda.DTO.Response.DashboardStatsDTO;
import com.judicial.mesadeayuda.Service.DashboardService;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<DashboardStatsDTO>> obtenerStats() {
        DashboardStatsDTO stats = dashboardService.obtenerStats();
        return ResponseEntity.ok(ApiResponse.success("Estadisticas del dashboard obtenidas", stats));
    }
}
