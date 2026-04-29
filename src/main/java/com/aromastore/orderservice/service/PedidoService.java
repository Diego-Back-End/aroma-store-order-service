package com.aromastore.orderservice.service;

import com.aromastore.orderservice.entity.Pedido;
import com.aromastore.orderservice.repository.PedidoRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PedidoService {

    @Autowired
    private PedidoRepository pedidoRepository;
    
    @Autowired
    private RabbitTemplate rabbitTemplate;

    public List<Pedido> findAll() {
        return pedidoRepository.findAll();
    }

    public Optional<Pedido> findById(Long id) {
        return pedidoRepository.findById(id);
    }

    public List<Pedido> findByUsuarioId(Long usuarioId) {
        return pedidoRepository.findByUsuarioId(usuarioId);
    }

    @CircuitBreaker(name = "pedidoService", fallbackMethod = "savePedidoFallback")
    public Pedido save(Pedido pedido) {
        Pedido savedPedido = pedidoRepository.save(pedido);
        
        // Publicar evento a RabbitMQ cuando se crea un pedido
        publishPedidoEvent(savedPedido);
        
        return savedPedido;
    }

    public void deleteById(Long id) {
        pedidoRepository.deleteById(id);
    }
    
    private void publishPedidoEvent(Pedido pedido) {
        String message = String.format(
            "Nuevo pedido creado - ID: %d, Usuario: %d, Producto: %d, Cantidad: %d, Total: %.2f, Estado: %s",
            pedido.getId(), pedido.getUsuarioId(), pedido.getProductoId(), 
            pedido.getCantidad(), pedido.getTotal(), pedido.getEstado()
        );
        
        rabbitTemplate.convertAndSend("pedidos-queue", message);
    }
    
    /**
     * Método fallback para el Circuit Breaker
     * Se ejecuta cuando el circuito está abierto o hay fallas
     */
    public Pedido savePedidoFallback(Pedido pedido, Exception ex) {
        // Crear un pedido de fallback con mensaje de error
        Pedido fallbackPedido = new Pedido();
        fallbackPedido.setUsuarioId(pedido.getUsuarioId());
        fallbackPedido.setProductoId(pedido.getProductoId());
        fallbackPedido.setCantidad(pedido.getCantidad());
        fallbackPedido.setTotal(pedido.getTotal());
        fallbackPedido.setEstado(pedido.getEstado());
        
        // Guardar el pedido sin publicar evento a RabbitMQ
        Pedido savedPedido = pedidoRepository.save(fallbackPedido);
        
        // Log del error para debugging
        System.err.println("Circuit Breaker activado - Servicio temporalmente no disponible: " + ex.getMessage());
        
        return savedPedido;
    }
}
