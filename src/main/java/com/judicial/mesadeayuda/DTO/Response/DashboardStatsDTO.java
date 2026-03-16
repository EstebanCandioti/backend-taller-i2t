package com.judicial.mesadeayuda.DTO.Response;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsDTO {

    private TicketStats tickets;
    private HardwareStats hardware;
    private SoftwareStats software;
    private ContratoStats contratos;
    private List<TecnicoCargaDTO> tecnicosCarga;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TicketStats {
        private long total;
        private Map<String, Long> porEstado;
        private Map<String, Long> porPrioridad;
        private long sinAsignar;
        private long activos;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HardwareStats {
        private long total;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SoftwareStats {
        private long total;
        private long licenciasVencidas;
        private long licenciasProximasAVencer;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ContratoStats {
        private long total;
        private long vencidos;
        private long proximosAVencer;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TecnicoCargaDTO {
        private Integer tecnicoId;
        private String nombreCompleto;
        private long asignados;
        private long enCurso;
        private long totalActivos;
    }
}
