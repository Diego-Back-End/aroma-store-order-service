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

    // Repositorio encargado de acceder a la base de datos de pedidos
    @Autowired
    private PedidoRepository pedidoRepository;
    
    // Componente utilizado para enviar mensajes a RabbitMQ
    @Autowired
    private RabbitTemplate rabbitTemplate;

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
    
    // Método encargado de publicar el evento en RabbitMQ
    private void publishPedidoEvent(Pedido pedido) {

        // Construye el mensaje con la información del pedido
        String message = String.format(
            "Nuevo pedido creado - ID: %d, Usuario: %d, Producto: %d, Cantidad: %d, Total: %.2f, Estado: %s",
            pedido.getId(), pedido.getUsuarioId(), pedido.getProductoId(), 
            pedido.getCantidad(), pedido.getTotal(), pedido.getEstado()
        );
        
        // Envía el mensaje a la cola "pedidos-queue"
        rabbitTemplate.convertAndSend("pedidos-queue", message);
    }
}