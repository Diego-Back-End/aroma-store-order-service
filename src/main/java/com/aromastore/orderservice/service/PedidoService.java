package com.aromastore.orderservice.service;

import com.aromastore.orderservice.entity.Pedido;
import com.aromastore.orderservice.entity.PedidoItem;
import com.aromastore.orderservice.repository.PedidoRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PedidoService {

    private static final Logger logger = LoggerFactory.getLogger(PedidoService.class);

    // Repositorio encargado de acceder a la base de datos de pedidos
    @Autowired
    private PedidoRepository pedidoRepository;
    
    // Componente utilizado para enviar mensajes a RabbitMQ
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    // Componente para hacer llamadas HTTP a otros microservicios
    @Autowired
    private RestTemplate restTemplate;
    
    // URL base del user-service
    @Value("${user.service.url}")
    private String userServiceUrl;

    // Obtiene todos los pedidos registrados en la base de datos
    public List<Pedido> findAll() {
        return pedidoRepository.findAll();
    }

    // Busca un pedido utilizando su ID
    public Optional<Pedido> findById(Long id) {
        return pedidoRepository.findById(id);
    }

    // Obtiene todos los pedidos asociados a un usuario específico
    public List<Pedido> findByUsuarioId(Long usuarioId) {
        return pedidoRepository.findByUsuarioId(usuarioId);
    }

    // Guarda un nuevo pedido
    public Pedido save(Pedido pedido) {

        // Establece la relación bidireccional para cada item
        if (pedido.getItems() != null) {
            for (PedidoItem item : pedido.getItems()) {
                item.setPedido(pedido);
            }
        }

        // Guarda el pedido en la base de datos
        Pedido savedPedido = pedidoRepository.save(pedido);
        
        // Publica un evento en RabbitMQ cuando se crea un pedido
        publishPedidoEvent(savedPedido);
        
        return savedPedido;
    }

    // Elimina un pedido por su ID
    public void deleteById(Long id) {
        pedidoRepository.deleteById(id);
    }
    
    // Obtiene el nombre del usuario desde el user-service
    private String obtenerNombreUsuario(Long usuarioId) {
        try {
            String url = userServiceUrl + "/api/usuarios/" + usuarioId;
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null && response.containsKey("nombre")) {
                return (String) response.get("nombre");
            }
        } catch (Exception e) {
            logger.warn("No se pudo obtener el nombre del usuario {}: {}", usuarioId, e.getMessage());
        }
        return "Usuario"; // Fallback si falla la llamada
    }
    
    private void publishPedidoEvent(Pedido pedido) {
    String nombreUsuario = obtenerNombreUsuario(pedido.getUsuarioId());

    String message = String.format(
        "Nuevo pedido creado - ID: %d, Usuario: %d, Nombre: %s, Items: %d productos, Total: %.2f, Estado: %s",
        pedido.getId(), pedido.getUsuarioId(), nombreUsuario, pedido.getItems().size(),
        pedido.getTotal(), pedido.getEstado()
    );

    try {
        rabbitTemplate.convertAndSend("pedidos-queue", message);
    } catch (Exception e) {
        logger.warn("No se pudo publicar el evento de pedido en RabbitMQ: {}", e.getMessage());
    }
}
}