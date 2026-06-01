// Mis Pedidos - Vista del Cliente

document.addEventListener('DOMContentLoaded', function() {
    cargarPedidos();
    updateCartCount();
});

function cargarPedidos() {
    $.ajax({
        url: '/api/mis-pedidos',
        method: 'GET',
        success: function(response) {
            if (response.success && response.data.length > 0) {
                renderizarPedidos(response.data);
            } else {
                document.getElementById('listaPedidos').style.display = 'none';
                document.getElementById('sinPedidos').style.display = 'block';
            }
        },
        error: function(error) {
            console.error('Error al cargar pedidos:', error);
            document.getElementById('listaPedidos').style.display = 'none';
            document.getElementById('sinPedidos').style.display = 'block';
        }
    });
}

function renderizarPedidos(pedidos) {
    const listaPedidos = document.getElementById('listaPedidos');
    listaPedidos.innerHTML = '';
    
    // Ordenar por fecha descendente (más reciente primero)
    pedidos.sort((a, b) => new Date(b.fechaPedido) - new Date(a.fechaPedido));
    
    pedidos.forEach(pedido => {
        const badgeClass = `badge-${pedido.estado}`;
        const estadoTexto = formatearEstado(pedido.estado);
        
        // Crear lista de productos como texto
        const productosTexto = pedido.items.map(item => 
            `${item.producto.nombre} (x${item.cantidad})`
        ).join(', ');
        
        const pedidoCard = document.createElement('div');
        pedidoCard.className = 'pedido-card';
        pedidoCard.onclick = () => verDetalle(pedido.id);
        
        pedidoCard.innerHTML = `
            <div class="pedido-header">
                <div>
                    <h5 class="mb-1">${pedido.numeroPedido}</h5>
                    <small class="text-muted">${formatearFecha(pedido.fechaPedido)}</small>
                </div>
                <span class="badge ${badgeClass}">${estadoTexto}</span>
            </div>
            <div class="pedido-productos">
                <p class="mb-1">${productosTexto}</p>
                <strong>Total: S/ ${parseFloat(pedido.total).toFixed(2)}</strong>
            </div>
        `;
        
        listaPedidos.appendChild(pedidoCard);
    });
}

function formatearEstado(estado) {
    const estados = {
        'PENDIENTE': 'Pendiente',
        'EN_REVISION': 'En Revisión',
        'APROBADO': 'Aprobado',
        'RECHAZADO': 'Rechazado',
        'PROCESADO': 'Procesado'
    };
    return estados[estado] || estado;
}

function formatearFecha(fecha) {
    const date = new Date(fecha);
    return date.toLocaleString('es-PE');
}

function verDetalle(pedidoId) {
    $.ajax({
        url: `/pedidos_web/api/detalle/${pedidoId}`,
        method: 'GET',
        success: function(response) {
            if (response.success) {
                mostrarModalDetalle(response.data);
            } else {
                alert('Error al cargar el detalle del pedido');
            }
        },
        error: function(error) {
            console.error('Error al cargar detalle:', error);
            alert('Error al cargar el detalle del pedido');
        }
    });
}

function mostrarModalDetalle(pedido) {
    // Información básica
    document.getElementById('modalNumeroPedido').textContent = pedido.numeroPedido;
    document.getElementById('modalFechaPedido').textContent = formatearFecha(pedido.fechaPedido);
    document.getElementById('modalTotal').textContent = `S/ ${parseFloat(pedido.total).toFixed(2)}`;
    document.getElementById('modalMetodoPago').textContent = pedido.metodoPago;
    
    // Estado
    const badgeClass = `badge-${pedido.estado}`;
    const estadoBadge = document.getElementById('modalEstado');
    estadoBadge.className = `badge ${badgeClass}`;
    estadoBadge.textContent = formatearEstado(pedido.estado);
    
    // Banner de rechazo
    const bannerRechazo = document.getElementById('bannerRechazo');
    if (pedido.estado === 'RECHAZADO' && pedido.motivoRechazo) {
        bannerRechazo.style.display = 'block';
        document.getElementById('motivoRechazoTexto').textContent = pedido.motivoRechazo;
    } else {
        bannerRechazo.style.display = 'none';
    }
    
    // Banner de revisión
    const bannerRevision = document.getElementById('bannerRevision');
    if (pedido.estado === 'EN_REVISION' || pedido.estado === 'PENDIENTE') {
        bannerRevision.style.display = 'block';
    } else {
        bannerRevision.style.display = 'none';
    }
    
    // Productos
    const productosBody = document.getElementById('modalProductosBody');
    productosBody.innerHTML = '';
    
    pedido.items.forEach(item => {
        const subtotal = item.cantidad * item.precioUnitario;
        const row = document.createElement('tr');
        row.innerHTML = `
            <td>${item.producto.nombre}</td>
            <td>${item.cantidad}</td>
            <td>S/ ${parseFloat(item.precioUnitario).toFixed(2)}</td>
            <td>S/ ${subtotal.toFixed(2)}</td>
        `;
        productosBody.appendChild(row);
    });
    
    // Voucher
    document.getElementById('modalVoucherImagen').src = pedido.voucherImagen;
    
    // Mostrar modal
    const modal = new bootstrap.Modal(document.getElementById('detallePedidoModal'));
    modal.show();
}

function abrirImagenFullscreen() {
    const src = document.getElementById('modalVoucherImagen').src;
    document.getElementById('imagenFullscreen').src = src;
    const modal = new bootstrap.Modal(document.getElementById('imagenFullscreenModal'));
    modal.show();
}

function updateCartCount() {
    const cart = localStorage.getItem('cart');
    const count = cart ? JSON.parse(cart).length : 0;
    const cartCountElement = document.getElementById('cart-count');
    if (cartCountElement) {
        cartCountElement.textContent = count;
    }
}
