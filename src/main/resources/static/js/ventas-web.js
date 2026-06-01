$(document).ready(function() {
    let dataTable;
    let editVentaModal;

    const API_BASE = '/ventas_web/api';
    const ENDPOINTS = {
        list: `${API_BASE}/listar`,
        process: (id) => `${API_BASE}/procesar/${id}`,
        delete: (id) => `${API_BASE}/eliminar/${id}`,
    };

    initializeDataTable();
    editVentaModal = new bootstrap.Modal(document.getElementById('editVentaModal'));
    setupEventListeners();

    function initializeDataTable() {
        if (dataTable) dataTable.destroy();
        console.log('Inicializando DataTable con URL:', ENDPOINTS.list);
        dataTable = $('#tablaPedidosWeb').DataTable({
            responsive: true,
            processing: true,
            ajax: {
                url: ENDPOINTS.list,
                dataSrc: function(data) {
                    console.log('Datos recibidos:', data);
                    console.log('Cantidad de datos:', data.data ? data.data.length : 0);
                    return data.data;
                },
                error: function(xhr, status, error) {
                    console.error('Error en AJAX:', error);
                    console.error('Status:', xhr.status);
                    console.error('Response:', xhr.responseText);
                }
            },
            columns: [
                { data: 'numeroPedido' },
                { data: 'fechaPedido', render: data => new Date(data).toLocaleString('es-PE') },
                { data: 'nombreCliente' },
                { data: 'total', render: data => `S/ ${parseFloat(data).toFixed(2)}` },
                { data: 'estado' },
                {
                    data: null, orderable: false, searchable: false,
                    render: (data, type, row) => `
                        <button class="btn btn-sm btn-info action-view-voucher" data-id="${row.id}" data-voucher="${row.voucherImagen}" title="Ver Voucher"><i class="bi bi-eye"></i></button>
                        <button class="btn btn-sm btn-primary action-view-detail" data-id="${row.id}" title="Ver Detalle"><i class="bi bi-list-ul"></i></button>
                        <button class="btn btn-sm btn-success action-approve" data-id="${row.id}" title="Aprobar"><i class="bi bi-check-lg"></i></button>
                        <button class="btn btn-sm btn-danger action-reject" data-id="${row.id}" title="Rechazar"><i class="bi bi-x-lg"></i></button>
                    `
                }
            ],
            language: { url: "//cdn.datatables.net/plug-ins/1.13.6/i18n/es-ES.json" },
            order: [[0, 'desc']]
        });
    }

    function setupEventListeners() {
        $('#tablaPedidosWeb tbody').on('click', '.action-view-voucher', handleViewVoucher);
        $('#tablaPedidosWeb tbody').on('click', '.action-view-detail', handleViewDetail);
        $('#tablaPedidosWeb tbody').on('click', '.action-approve', handleApprove);
        $('#tablaPedidosWeb tbody').on('click', '.action-reject', handleReject);

        // Escuchar mensajes desde el iframe de edición para recargar la tabla
        window.addEventListener('message', function(event) {
            if (event.data === 'ventaActualizada') {
                editVentaModal.hide();
                dataTable.ajax.reload();
                showNotification('Venta web actualizada con éxito.', 'success');
            }
        });
    }

    function handleViewVoucher() {
        const voucherPath = $(this).data('voucher');
        if (voucherPath) {
            window.open(voucherPath, '_blank');
        } else {
            alert('No hay voucher disponible');
        }
    }

    function handleViewDetail() {
        const pedidoId = $(this).data('id');
        
        $.ajax({
            url: `/ventas_web/api/detalle/${pedidoId}`,
            method: 'GET',
            success: function(response) {
                if (response.success) {
                    const pedido = response.data;
                    let detalleHtml = `
                        <h5>Detalle del Pedido #${pedido.numeroPedido}</h5>
                        <p><strong>Cliente:</strong> ${pedido.nombreCliente}</p>
                        <p><strong>DNI:</strong> ${pedido.dniCliente}</p>
                        <p><strong>Teléfono:</strong> ${pedido.telefonoCliente}</p>
                        <p><strong>Fecha:</strong> ${new Date(pedido.fechaPedido).toLocaleString('es-PE')}</p>
                        <p><strong>Método de Pago:</strong> ${pedido.metodoPago}</p>
                        <p><strong>Total:</strong> S/ ${parseFloat(pedido.total).toFixed(2)}</p>
                        <h6 class="mt-3">Items:</h6>
                        <table class="table table-sm">
                            <thead>
                                <tr>
                                    <th>Producto</th>
                                    <th>Cantidad</th>
                                    <th>Precio Unitario</th>
                                    <th>Subtotal</th>
                                </tr>
                            </thead>
                            <tbody>
                    `;
                    
                    pedido.items.forEach(item => {
                        detalleHtml += `
                            <tr>
                                <td>${item.producto.nombre}</td>
                                <td>${item.cantidad}</td>
                                <td>S/ ${parseFloat(item.precioUnitario).toFixed(2)}</td>
                                <td>S/ ${parseFloat(item.subtotal).toFixed(2)}</td>
                            </tr>
                        `;
                    });
                    
                    detalleHtml += `
                            </tbody>
                        </table>
                    `;
                    
                    alert(detalleHtml.replace(/<[^>]*>/g, '\n').replace(/\n+/g, '\n'));
                } else {
                    alert('Error al cargar el detalle: ' + response.message);
                }
            },
            error: function(xhr, status, error) {
                alert('Error al cargar el detalle: ' + error);
            }
        });
    }

    function handleApprove() {
        const pedidoId = $(this).data('id');
        if (confirm('¿Está seguro de aprobar este pedido?')) {
            $.ajax({
                url: `/ventas_web/api/aprobar/${pedidoId}`,
                method: 'PUT',
                success: function(response) {
                    dataTable.ajax.reload();
                    alert('Pedido aprobado con éxito');
                },
                error: function(xhr, status, error) {
                    alert('Error al aprobar el pedido: ' + error);
                }
            });
        }
    }

    function handleReject() {
        const pedidoId = $(this).data('id');
        const motivo = prompt('Por favor, ingrese el motivo de rechazo:');
        if (motivo) {
            $.ajax({
                url: `/ventas_web/api/rechazar/${pedidoId}`,
                method: 'PUT',
                contentType: 'application/json',
                data: JSON.stringify({ motivo: motivo }),
                success: function(response) {
                    dataTable.ajax.reload();
                    alert('Pedido rechazado con éxito');
                },
                error: function(xhr, status, error) {
                    alert('Error al rechazar el pedido: ' + error);
                }
            });
        }
    }

    function handleEdit() {
        const ventaId = $(this).data('id');
        const iframe = $('#editVentaIframe');
        iframe.attr('src', `/ventas_web/modificar/${ventaId}`);
        editVentaModal.show();
    }

    function handleProcesar() {
        const ventaId = $(this).data('id');
        Swal.fire({
            title: '¿Estás seguro?',
            text: "La venta será procesada y el stock de los productos será actualizado. Esta acción no se puede revertir.",
            icon: 'info',
            showCancelButton: true, confirmButtonColor: '#3085d6', cancelButtonColor: '#d33',
            confirmButtonText: 'Sí, procesar', cancelButtonText: 'Cancelar'
        }).then(result => {
            if (result.isConfirmed) {
                $.ajax({
                    url: ENDPOINTS.process(ventaId), type: 'POST',
                    success: function(response) {
                        showNotification(response.message, response.success ? 'success' : 'error');
                        if (response.success) dataTable.ajax.reload();
                    },
                    error: (xhr) => showNotification(xhr.responseJSON?.message || 'Error al procesar la venta.', 'error')
                });
            }
        });
    }

    function handleDelete() {
        const ventaId = $(this).data('id');
        Swal.fire({
            title: '¿Estás seguro de eliminar esta venta web?',
            text: "Esta acción eliminará el registro permanentemente de la base de datos. No se podrá recuperar.",
            icon: 'warning',
            showCancelButton: true,
            confirmButtonColor: '#d33',
            cancelButtonColor: '#3085d6',
            confirmButtonText: 'Sí, eliminarla',
            cancelButtonText: 'Cancelar'
        }).then(result => {
            if (result.isConfirmed) {
                $.ajax({
                    url: ENDPOINTS.delete(ventaId),
                    type: 'DELETE',
                    success: function(response) {
                        showNotification(response.message, response.success ? 'success' : 'error');
                        if (response.success) {
                            dataTable.ajax.reload();
                        }
                    },
                    error: (xhr) => showNotification(xhr.responseJSON?.message || 'Error al eliminar la venta web.', 'error')
                });
            }
        });
    }

    function showNotification(message, type = 'success') {
        const toastContainer = document.getElementById('notification-container');
        if (!toastContainer) return;

        const toastClass = type === 'success' ? 'text-bg-success' : 'text-bg-danger';
        const toastElement = document.createElement('div');
        toastElement.className = `toast align-items-center ${toastClass} border-0`;
        toastElement.setAttribute('role', 'alert');
        toastElement.setAttribute('aria-live', 'assertive');
        toastElement.setAttribute('aria-atomic', 'true');

        toastElement.innerHTML = `
            <div class="d-flex">
                <div class="toast-body">${message}</div>
                <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
            </div>`;

        toastContainer.appendChild(toastElement);

        const toast = new bootstrap.Toast(toastElement, { delay: 3000 });
        toast.show();

        // Eliminar el toast del DOM después de que se oculte
        toastElement.addEventListener('hidden.bs.toast', () => {
            toastElement.remove();
        });
    }
});
