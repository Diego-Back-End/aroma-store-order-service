package com.aromastore.orderservice.entity;

import com.aromastore.orderservice.enums.EstadoPedido;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "pedidos")
@Data
public class Pedido {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long usuarioId;
    
    @Column(nullable = false)
    private Long productoId;
    
    @Column(nullable = false)
    private Integer cantidad;
    
    @Column(nullable = false)
    private Double total;
    
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private EstadoPedido estado;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;
    
    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
        if (estado == null) {
            estado = EstadoPedido.PENDIENTE;
        }
    }
}
