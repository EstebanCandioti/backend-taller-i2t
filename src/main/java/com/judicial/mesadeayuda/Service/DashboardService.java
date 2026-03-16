package com.judicial.mesadeayuda.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.judicial.mesadeayuda.DTO.Response.DashboardStatsDTO;
import com.judicial.mesadeayuda.Entities.Ticket;
import com.judicial.mesadeayuda.Repositories.ContratoRepository;
import com.judicial.mesadeayuda.Repositories.HardwareRepository;
import com.judicial.mesadeayuda.Repositories.SoftwareRepository;
import com.judicial.mesadeayuda.Repositories.TicketRepository;
import com.judicial.mesadeayuda.Repositories.UsuarioRepository;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private static final int SOFTWARE_ALERTA_DIAS = 30;

    private final TicketRepository ticketRepository;
    private final HardwareRepository hardwareRepository;
    private final SoftwareRepository softwareRepository;
    private final ContratoRepository contratoRepository;
    private final UsuarioRepository usuarioRepository;

    public DashboardService(TicketRepository ticketRepository,
            HardwareRepository hardwareRepository,
            SoftwareRepository softwareRepository,
            ContratoRepository contratoRepository,
            UsuarioRepository usuarioRepository) {
        this.ticketRepository = ticketRepository;
        this.hardwareRepository = hardwareRepository;
        this.softwareRepository = softwareRepository;
        this.contratoRepository = contratoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public DashboardStatsDTO obtenerStats() {
        LocalDate hoy = LocalDate.now();

        return DashboardStatsDTO.builder()
                .tickets(construirTicketStats())
                .hardware(DashboardStatsDTO.HardwareStats.builder()
                        .total(hardwareRepository.count())
                        .build())
                .software(DashboardStatsDTO.SoftwareStats.builder()
                        .total(softwareRepository.count())
                        .licenciasVencidas(softwareRepository.countLicenciasVencidas(hoy))
                        .licenciasProximasAVencer(softwareRepository.countLicenciasProximasAVencer(
                                hoy, hoy.plusDays(SOFTWARE_ALERTA_DIAS)))
                        .build())
                .contratos(DashboardStatsDTO.ContratoStats.builder()
                        .total(contratoRepository.count())
                        .vencidos(contratoRepository.countVencidos(hoy))
                        .proximosAVencer(contratoRepository.countProximosAVencer(hoy))
                        .build())
                .tecnicosCarga(construirTecnicosCarga())
                .build();
    }

    private DashboardStatsDTO.TicketStats construirTicketStats() {
        Map<String, Long> porEstado = new LinkedHashMap<>();
        for (Ticket.Estado estado : Ticket.Estado.values()) {
            porEstado.put(estado.name(), 0L);
        }
        for (Object[] fila : ticketRepository.countByEstado()) {
            porEstado.put(String.valueOf(fila[0]), ((Number) fila[1]).longValue());
        }

        Map<String, Long> porPrioridad = new LinkedHashMap<>();
        for (Ticket.Prioridad prioridad : Ticket.Prioridad.values()) {
            porPrioridad.put(prioridad.name(), 0L);
        }
        for (Object[] fila : ticketRepository.countByPrioridad()) {
            porPrioridad.put(String.valueOf(fila[0]), ((Number) fila[1]).longValue());
        }

        return DashboardStatsDTO.TicketStats.builder()
                .total(ticketRepository.count())
                .porEstado(porEstado)
                .porPrioridad(porPrioridad)
                .sinAsignar(ticketRepository.countSinAsignar())
                .activos(ticketRepository.countActivos())
                .build();
    }

    private List<DashboardStatsDTO.TecnicoCargaDTO> construirTecnicosCarga() {
        List<DashboardStatsDTO.TecnicoCargaDTO> tecnicos = new ArrayList<>();
        for (Object[] fila : usuarioRepository.findTecnicosCargaDashboard()) {
            tecnicos.add(DashboardStatsDTO.TecnicoCargaDTO.builder()
                    .tecnicoId(((Number) fila[0]).intValue())
                    .nombreCompleto(String.valueOf(fila[1]))
                    .asignados(((Number) fila[2]).longValue())
                    .enCurso(((Number) fila[3]).longValue())
                    .totalActivos(((Number) fila[4]).longValue())
                    .build());
        }
        return tecnicos;
    }
}
