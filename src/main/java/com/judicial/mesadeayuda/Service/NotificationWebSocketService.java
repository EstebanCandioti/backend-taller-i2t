package com.judicial.mesadeayuda.Service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.judicial.mesadeayuda.DTO.Response.WsNotificationDTO;
import com.judicial.mesadeayuda.Repositories.UsuarioRepository;

@Service
public class NotificationWebSocketService {

    private final SimpMessagingTemplate messagingTemplate;
    private final UsuarioRepository usuarioRepository;

    public NotificationWebSocketService(SimpMessagingTemplate messagingTemplate,
                                         UsuarioRepository usuarioRepository) {
        this.messagingTemplate = messagingTemplate;
        this.usuarioRepository = usuarioRepository;
    }

    public void notificarUsuario(String email,
                                 String tipo,
                                 String entidad,
                                 Integer registroId,
                                 String mensaje) {
        WsNotificationDTO notification = buildNotification(tipo, entidad, registroId, mensaje);
        messagingTemplate.convertAndSendToUser(email, "/queue/notificaciones", notification);
    }

    public void notificarPorRol(List<String> roles,
                                String tipo,
                                String entidad,
                                Integer registroId,
                                String mensaje) {
        WsNotificationDTO notification = buildNotification(tipo, entidad, registroId, mensaje);
        List<String> emails = usuarioRepository.findEmailsByRolesAndActivoTrue(roles);

        emails.forEach(email ->
                messagingTemplate.convertAndSendToUser(email, "/queue/notificaciones", notification));
    }

    private WsNotificationDTO buildNotification(String tipo, String entidad,
                                                 Integer registroId, String mensaje) {
        return WsNotificationDTO.builder()
                .tipo(tipo)
                .entidad(entidad)
                .registroId(registroId)
                .mensaje(mensaje)
                .fecha(LocalDateTime.now().toString())
                .build();
    }
}
