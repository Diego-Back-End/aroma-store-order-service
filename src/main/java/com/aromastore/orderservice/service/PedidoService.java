package com.aromastore.orderservice.service;

import com.aromastore.orderservice.entity.Pedido;
import com.aromastore.orderservice.repository.PedidoRepository;
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
}
