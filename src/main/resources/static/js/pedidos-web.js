// Pedidos Web - Gestión Admin

window.addEventListener('load', function() {
    alert('pedidos-web.js cargado - window.onload');
});

let tablaPedidosWeb;
let pedidoSeleccionado = null;

$(document).ready(function() {
    alert('$(document).ready ejecutado');
    inicializarTabla();
    cargarPedidos();
    
    // Event listeners para filtros
    $('#filtroEstado').change(function() {
        cargarPedidos();
    });
    
    $('#filtroFechaDesde').change(function() {
        cargarPedidos();
    });
    
    $('#filtroFechaHasta').change(function() {
        cargarPedidos();
    });
    
    $('#filtroBusqueda').on('input', function() {
        cargarPedidos();
    });
});

function inicializarTabla() {
    tablaPedidosWeb = $('#tablaPedidosWeb').DataTable({
        language: {
            url: '//cdn.datatables.net/plug-ins/1.13.6/i18n/es.json'
        },
        responsive: true,
        pageLength: 25,
        order: [[1, 'desc']], // Ordenar por fecha descendente
        columns: [
            { data: 'numeroPedido' },
            { 
                data: 'fechaPedido',
                render: function(data) {
                    if (!data) return '';
                    const fecha = new Date(data);
                    return fecha.toLocaleString('es-PE');
                }
            },
            { 
                data: null,
                render: function(data) {
                    return `${data.nombreCliente}<br><small class="text-muted">DNI: ${data.dniCliente}</small>`;
                }
            },
            { 
                data: 'total',
                render: function(data) {
                    return `S/ ${parseFloat(data).toFixed(2)}`;
                }
            },
            { 
                data: 'estado',
                render: function(data) {
                    const badges = {
                        'PENDIENTE': '<span class="badge bg-secondary">Pendiente</span>',
                        'EN_REVISION': '<span class="badge bg-warning">En Revisión</span>',
                        'APROBADO': '<span class="badge bg-success">Aprobado</span>',
                        'RECHAZADO': '<span class="badge bg-danger">Rechazado</span>',
                        'PROCESADO': '<span class="badge bg-info">Procesado</span>'
                    };
                    return badges[data] || data;
                }
            },
            {
                data: null,
                render: function(data) {
                    return `
                        <button class="btn btn-sm btn-primary" onclick="verDetalle(${data.id})">
                            <i class="bi bi-eye"></i> Ver
                        </button>
                    `;
                }
            }
        ],
        rowCallback: function(row, data) {
            // Destacar pedidos en estado EN_REVISION
            if (data.estado === 'EN_REVISION') {
                $(row).addClass('row-en-revision');
            }
        }
    });
}

function cargarPedidos() {
    alert('cargarPedidos llamado');
    const estado = $('#filtroEstado').val();
    const fechaDesde = $('#filtroFechaDesde').val();
    const fechaHasta = $('#filtroFechaHasta').val();
    const busqueda = $('#filtroBusqueda').val();

    let url = '/pedidos_web/api/listar';
    alert('URL: ' + url);

    if (estado) {
        url += `/estado/${estado}`;
    }

    alert('Haciendo AJAX request a: ' + url);
    $.ajax({
        url: url,
        method: 'GET',
        success: function(response) {
            alert('Response recibida: ' + JSON.stringify(response));
            let pedidos = response.data;
            alert('Pedidos recibidos: ' + pedidos.length);

            // Filtrar por fecha
            if (fechaDesde) {
                pedidos = pedidos.filter(p => new Date(p.fechaPedido) >= new Date(fechaDesde));
            }
            if (fechaHasta) {
                pedidos = pedidos.filter(p => new Date(p.fechaPedido) <= new Date(fechaHasta + 'T23:59:59'));
            }

            // Filtrar por búsqueda
            if (busqueda) {
                const busquedaLower = busqueda.toLowerCase();
                pedidos = pedidos.filter(p =>
                    p.nombreCliente.toLowerCase().includes(busquedaLower) ||
                    p.dniCliente.includes(busquedaLower)
                );
            }

            tablaPedidosWeb.clear();
            tablaPedidosWeb.rows.add(pedidos);
            tablaPedidosWeb.draw();
        },
        error: function(xhr, status, error) {
            alert('Error en AJAX request: ' + error);
            alert('Status: ' + xhr.status);
            alert('Response: ' + xhr.responseText);
            mostrarNotificacion('Error al cargar pedidos', 'danger');
        }
    });
}

function verDetalle(pedidoId) {
    $.ajax({
        url: `/pedidos_web/api/detalle/${pedidoId}`,
        method: 'GET',
        success: function(response) {
            if (response.success) {
                pedidoSeleccionado = response.data;
                mostrarModalDetalle(pedidoSeleccionado);
            } else {
                mostrarNotificacion(response.message, 'danger');
            }
        },
        error: function(error) {
            mostrarNotificacion('Error al cargar detalle del pedido', 'danger');
        }
    });
}

function mostrarModalDetalle(pedido) {
    // Información del pedido
    $('#modalNumeroPedido').text(pedido.numeroPedido);
    $('#modalFechaPedido').text(new Date(pedido.fechaPedido).toLocaleString('es-PE'));
    
    const badges = {
        'PENDIENTE': 'bg-secondary',
        'EN_REVISION': 'bg-warning',
        'APROBADO': 'bg-success',
        'RECHAZADO': 'bg-danger',
        'PROCESADO': 'bg-info'
    };
    $('#modalEstado').removeClass().addClass('badge ' + badges[pedido.estado]).text(pedido.estado);
    $('#modalTotal').text(`S/ ${parseFloat(pedido.total).toFixed(2)}`);
    
    // Datos del cliente
    $('#modalNombreCliente').text(pedido.nombreCliente);
    $('#modalDniCliente').text(pedido.dniCliente);
    $('#modalTelefonoCliente').text(pedido.telefonoCliente);
    
    // Productos
    $('#modalProductosBody').empty();
    pedido.items.forEach(item => {
        const subtotal = item.cantidad * item.precioUnitario;
        $('#modalProductosBody').append(`
            <tr>
                <td>${item.producto.nombre}</td>
                <td>${item.cantidad}</td>
                <td>S/ ${parseFloat(item.precioUnitario).toFixed(2)}</td>
                <td>S/ ${subtotal.toFixed(2)}</td>
            </tr>
        `);
    });
    
    // Voucher
    $('#modalVoucherImagen').attr('src', pedido.voucherImagen);
    
    // Mostrar/ocultar botones según estado
    const puedeAprobar = pedido.estado === 'PENDIENTE' || pedido.estado === 'EN_REVISION';
    const puedeRechazar = pedido.estado === 'PENDIENTE' || pedido.estado === 'EN_REVISION';
    
    $('#btnAprobarPedido').toggle(puedeAprobar);
    $('#btnRechazarPedido').toggle(puedeRechazar);
    $('#modalAcciones').toggle(puedeAprobar || puedeRechazar);
    
    // Ocultar formulario de rechazo
    $('#formularioRechazo').hide();
    $('#motivoRechazo').val('');
    
    // Mostrar modal
    const modal = new bootstrap.Modal(document.getElementById('detallePedidoModal'));
    modal.show();
}

function abrirImagenFullscreen() {
    const src = $('#modalVoucherImagen').attr('src');
    $('#imagenFullscreen').attr('src', src);
    const modal = new bootstrap.Modal(document.getElementById('imagenFullscreenModal'));
    modal.show();
}

function mostrarFormularioRechazo() {
    $('#formularioRechazo').show();
    $('#modalAcciones').hide();
}

function cancelarRechazo() {
    $('#formularioRechazo').hide();
    $('#modalAcciones').show();
    $('#motivoRechazo').val('');
}

function confirmarRechazo() {
    const motivo = $('#motivoRechazo').val().trim();
    
    if (!motivo) {
        mostrarNotificacion('Debes escribir el motivo del rechazo', 'warning');
        return;
    }
    
    if (!confirm('¿Estás seguro de rechazar este pedido?')) {
        return;
    }
    
    // Obtener ID del usuario actual de la sesión
    $.ajax({
        url: '/api/usuario-actual',
        method: 'GET',
        success: function(usuarioResponse) {
            const verificadoPorId = usuarioResponse.id;
            
            $.ajax({
                url: `/pedidos_web/api/rechazar/${pedidoSeleccionado.id}`,
                method: 'POST',
                data: {
                    verificadoPorId: verificadoPorId,
                    motivoRechazo: motivo
                },
                success: function(response) {
                    if (response.success) {
                        mostrarNotificacion('Pedido rechazado correctamente', 'success');
                        bootstrap.Modal.getInstance(document.getElementById('detallePedidoModal')).hide();
                        cargarPedidos();
                    } else {
                        mostrarNotificacion(response.message, 'danger');
                    }
                },
                error: function(error) {
                    mostrarNotificacion('Error al rechazar el pedido', 'danger');
                }
            });
        },
        error: function() {
            // Si no hay endpoint, usar ID 1 como fallback
            $.ajax({
                url: `/pedidos_web/api/rechazar/${pedidoSeleccionado.id}`,
                method: 'POST',
                data: {
                    verificadoPorId: 1,
                    motivoRechazo: motivo
                },
                success: function(response) {
                    if (response.success) {
                        mostrarNotificacion('Pedido rechazado correctamente', 'success');
                        bootstrap.Modal.getInstance(document.getElementById('detallePedidoModal')).hide();
                        cargarPedidos();
                    } else {
                        mostrarNotificacion(response.message, 'danger');
                    }
                },
                error: function(error) {
                    mostrarNotificacion('Error al rechazar el pedido', 'danger');
                }
            });
        }
    });
}

function aprobarPedido() {
    if (!confirm('¿Estás seguro de aprobar este pedido y crear la venta? Esto descontará el stock de los productos.')) {
        return;
    }
    
    // Obtener ID del usuario actual de la sesión
    $.ajax({
        url: '/api/usuario-actual',
        method: 'GET',
        success: function(usuarioResponse) {
            const verificadoPorId = usuarioResponse.id;
            
            $.ajax({
                url: `/pedidos_web/api/aprobar/${pedidoSeleccionado.id}`,
                method: 'POST',
                data: { verificadoPorId: verificadoPorId },
                success: function(response) {
                    if (response.success) {
                        mostrarNotificacion('Venta creada correctamente. Stock actualizado.', 'success');
                        bootstrap.Modal.getInstance(document.getElementById('detallePedidoModal')).hide();
                        cargarPedidos();
                    } else {
                        mostrarNotificacion(response.message, 'danger');
                    }
                },
                error: function(error) {
                    mostrarNotificacion('Error al aprobar el pedido', 'danger');
                }
            });
        },
        error: function() {
            // Si no hay endpoint, usar ID 1 como fallback
            $.ajax({
                url: `/pedidos_web/api/aprobar/${pedidoSeleccionado.id}`,
                method: 'POST',
                data: { verificadoPorId: 1 },
                success: function(response) {
                    if (response.success) {
                        mostrarNotificacion('Venta creada correctamente. Stock actualizado.', 'success');
                        bootstrap.Modal.getInstance(document.getElementById('detallePedidoModal')).hide();
                        cargarPedidos();
                    } else {
                        mostrarNotificacion(response.message, 'danger');
                    }
                },
                error: function(error) {
                    mostrarNotificacion('Error al aprobar el pedido', 'danger');
                }
            });
        }
    });
}

function mostrarNotificacion(mensaje, tipo) {
    const toastContainer = document.getElementById('notification-container');
    const toast = document.createElement('div');
    toast.className = `toast align-items-center text-white bg-${tipo} border-0`;
    toast.setAttribute('role', 'alert');
    toast.setAttribute('aria-live', 'assertive');
    toast.setAttribute('aria-atomic', 'true');
    
    toast.innerHTML = `
        <div class="d-flex">
            <div class="toast-body">
                ${mensaje}
            </div>
            <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
        </div>
    `;
    
    toastContainer.appendChild(toast);
    const bsToast = new bootstrap.Toast(toast);
    bsToast.show();
    
    toast.addEventListener('hidden.bs.toast', function() {
        toast.remove();
    });
}
