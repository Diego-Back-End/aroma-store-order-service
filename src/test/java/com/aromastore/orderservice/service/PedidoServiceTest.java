package com.aromastore.orderservice.service;

import com.aromastore.orderservice.entity.Pedido;
import com.aromastore.orderservice.entity.PedidoItem;
import com.aromastore.orderservice.enums.EstadoPedido;
import com.aromastore.orderservice.repository.PedidoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private PedidoService pedidoService;

    private Pedido testPedido;

    // Método auxiliar para crear un PedidoItem y asociarlo a su pedido
    private PedidoItem crearItem(Pedido pedido, Long productoId, Integer cantidad, Double precioUnitario) {
        PedidoItem item = new PedidoItem();
        item.setProductoId(productoId);
        item.setCantidad(cantidad);
        item.setPrecioUnitario(precioUnitario);
        item.setPedido(pedido);
        return item;
    }

    @BeforeEach
    void setUp() {
        testPedido = new Pedido();
        testPedido.setId(1L);
        testPedido.setUsuarioId(1L);
        testPedido.setTotal(199.98);
        testPedido.setEstado(EstadoPedido.PENDIENTE);

        List<PedidoItem> items = new ArrayList<>();
        items.add(crearItem(testPedido, 1L, 2, 99.99));
        testPedido.setItems(items);
    }

    @Test
    void testSave_WithRabbitMQEvent() {
        // Arrange
        Pedido savedPedido = new Pedido();
        savedPedido.setId(1L);
        savedPedido.setUsuarioId(1L);
        savedPedido.setTotal(199.98);
        savedPedido.setEstado(EstadoPedido.PENDIENTE);

        List<PedidoItem> items = new ArrayList<>();
        items.add(crearItem(savedPedido, 1L, 2, 99.99));
        savedPedido.setItems(items);

        when(pedidoRepository.save(any(Pedido.class))).thenReturn(savedPedido);

        // Act
        Pedido result = pedidoService.save(testPedido);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(1L, result.getUsuarioId());
        verify(pedidoRepository, times(1)).save(any(Pedido.class));
        verify(rabbitTemplate, times(1)).convertAndSend(eq("pedidos-queue"), anyString());
    }

    @Test
    void testSave_VerifyRabbitMQMessageFormat() {
        // Arrange
        Pedido savedPedido = new Pedido();
        savedPedido.setId(1L);
        savedPedido.setUsuarioId(1L);
        savedPedido.setTotal(199.98);
        savedPedido.setEstado(EstadoPedido.PENDIENTE);

        List<PedidoItem> items = new ArrayList<>();
        items.add(crearItem(savedPedido, 1L, 2, 99.99));
        savedPedido.setItems(items);

        when(pedidoRepository.save(any(Pedido.class))).thenReturn(savedPedido);

        // Act
        pedidoService.save(testPedido);

        // Assert
        verify(rabbitTemplate, times(1)).convertAndSend(eq("pedidos-queue"), anyString());
        verify(pedidoRepository, times(1)).save(any(Pedido.class));
    }

    @Test
    void testFindById_Success() {
        // Arrange
        Long id = 1L;
        when(pedidoRepository.findById(id)).thenReturn(Optional.of(testPedido));

        // Act
        Optional<Pedido> result = pedidoService.findById(id);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(id, result.get().getId());
        assertEquals(1L, result.get().getUsuarioId());
        verify(pedidoRepository, times(1)).findById(id);
    }

    @Test
    void testFindById_NotFound() {
        // Arrange
        Long id = 999L;
        when(pedidoRepository.findById(id)).thenReturn(Optional.empty());

        // Act
        Optional<Pedido> result = pedidoService.findById(id);

        // Assert
        assertFalse(result.isPresent());
        verify(pedidoRepository, times(1)).findById(id);
    }

    @Test
    void testFindByUsuarioId() {
        // Arrange
        Long usuarioId = 1L;
        Pedido pedido2 = new Pedido();
        pedido2.setId(2L);
        pedido2.setUsuarioId(1L);
        pedido2.setTotal(99.99);
        pedido2.setEstado(EstadoPedido.PENDIENTE);

        List<PedidoItem> items2 = new ArrayList<>();
        items2.add(crearItem(pedido2, 2L, 1, 99.99));
        pedido2.setItems(items2);

        when(pedidoRepository.findByUsuarioId(usuarioId)).thenReturn(Arrays.asList(testPedido, pedido2));

        // Act
        List<Pedido> result = pedidoService.findByUsuarioId(usuarioId);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getUsuarioId());
        assertEquals(1L, result.get(1).getUsuarioId());
        verify(pedidoRepository, times(1)).findByUsuarioId(usuarioId);
    }

    @Test
    void testFindAll() {
        // Arrange
        Pedido pedido2 = new Pedido();
        pedido2.setId(2L);
        pedido2.setUsuarioId(2L);
        pedido2.setTotal(99.99);
        pedido2.setEstado(EstadoPedido.PENDIENTE);

        List<PedidoItem> items2 = new ArrayList<>();
        items2.add(crearItem(pedido2, 1L, 1, 99.99));
        pedido2.setItems(items2);

        when(pedidoRepository.findAll()).thenReturn(Arrays.asList(testPedido, pedido2));

        // Act
        List<Pedido> result = pedidoService.findAll();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(pedidoRepository, times(1)).findAll();
    }
}