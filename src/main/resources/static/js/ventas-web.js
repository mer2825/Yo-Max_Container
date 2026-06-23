$(document).ready(function() {
    let dataTable;
    let dataTableAnuladas; // Nueva DataTable para ventas anuladas
    let editVentaModal;
    let voucherModal;

    const API_BASE = '/ventas_web/api';
    const ENDPOINTS = {
        list: `${API_BASE}/listar`,
        process: (id) => `${API_BASE}/procesar/${id}`,
        anular: (id) => `${API_BASE}/anular/${id}`, // Nuevo endpoint para anular
        eliminar: (id) => `${API_BASE}/eliminar/${id}`, // Endpoint para eliminar permanentemente
        approve: (id) => `${API_BASE}/aprobar/${id}`
    };

    initializeDataTable();
    initializeDataTableAnuladas(); // Inicializar la tabla de anuladas
    editVentaModal = new bootstrap.Modal(document.getElementById('editVentaModal'));
    voucherModal = new bootstrap.Modal(document.getElementById('voucherModal'));
    setupEventListeners();

    function initializeDataTable() {
        if (dataTable) dataTable.destroy();
        console.log('Inicializando DataTable principal con URL:', ENDPOINTS.list);
        dataTable = $('#tablaPedidosWeb').DataTable({
            responsive: true,
            processing: true,
            serverSide: true,
            ajax: {
                url: ENDPOINTS.list,
                type: 'GET',
                data: function(d) {
                    d.estado = $('#filtroEstado').val();
                    d.fechaDesde = $('#filtroFechaDesde').val();
                    d.fechaHasta = $('#filtroFechaHasta').val();
                    d.busqueda = $('#filtroBusqueda').val();
                    console.log('Parámetros enviados al backend (principal):', d);
                },
                dataSrc: function(json) {
                    console.log('Datos recibidos (principal):', json);
                    return json.data;
                },
                xhrFields: { withCredentials: true },
                error: function(xhr, status, error) {
                    console.error('Error en AJAX (principal):', error);
                    showNotification('Error al cargar los pedidos: ' + (xhr.responseJSON?.message || error), 'error');
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
                    render: (data, type, row) => {
                        let buttons = `
                            <button class="btn btn-sm btn-info action-view-voucher" data-id="${row.id}" data-voucher="${row.voucherImagen}" title="Ver Voucher"><i class="bi bi-eye"></i></button>
                            <button class="btn btn-sm btn-primary action-view-detail" data-id="${row.id}" title="Ver Detalle"><i class="bi bi-list-ul"></i></button>
                        `;
                        // Mostrar botones de aprobar, anular y eliminar solo si el estado no es APROBADO o ANULADO
                        if (row.estado !== 'APROBADO' && row.estado !== 'ANULADO') {
                            buttons += `
                                <button class="btn btn-sm btn-success action-approve" data-id="${row.id}" title="Aprobar"><i class="bi bi-check-lg"></i></button>
                                <button class="btn btn-sm btn-warning action-anular" data-id="${row.id}" title="Anular"><i class="bi bi-x-lg"></i></button>
                                <button class="btn btn-sm btn-danger action-eliminar" data-id="${row.id}" title="Eliminar"><i class="bi bi-trash"></i></button>
                            `;
                        } else if (row.estado === 'ANULADO') { // Si está anulado, solo permitir eliminar permanentemente
                             buttons += `
                                <button class="btn btn-sm btn-danger action-eliminar" data-id="${row.id}" title="Eliminar"><i class="bi bi-trash"></i></button>
                            `;
                        }
                        return buttons;
                    }
                }
            ],
            language: { url: "/js/i18n/es-ES.json" },
            order: [[0, 'desc']]
        });
    }

    function initializeDataTableAnuladas() {
        if (dataTableAnuladas) dataTableAnuladas.destroy();
        console.log('Inicializando DataTable de anuladas con URL:', ENDPOINTS.list);
        dataTableAnuladas = $('#tablaPedidosWebAnulados').DataTable({
            responsive: true,
            processing: true,
            serverSide: true,
            ajax: {
                url: ENDPOINTS.list,
                type: 'GET',
                data: function(d) {
                    d.estado = 'ANULADO'; // Siempre filtrar por ANULADO
                    d.fechaDesde = $('#filtroFechaDesde').val(); // Mantener filtros de fecha y búsqueda
                    d.fechaHasta = $('#filtroFechaHasta').val();
                    d.busqueda = $('#filtroBusqueda').val();
                    console.log('Parámetros enviados al backend (anuladas):', d);
                },
                dataSrc: function(json) {
                    console.log('Datos recibidos (anuladas):', json);
                    return json.data;
                },
                xhrFields: { withCredentials: true },
                error: function(xhr, status, error) {
                    console.error('Error en AJAX (anuladas):', error);
                    showNotification('Error al cargar los pedidos anulados: ' + (xhr.responseJSON?.message || error), 'error');
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
                        <button class="btn btn-sm btn-danger action-eliminar" data-id="${row.id}" title="Eliminar"><i class="bi bi-trash"></i></button>
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
        $('#tablaPedidosWeb tbody').on('click', '.action-anular', handleAnular); // Nuevo listener para anular
        $('#tablaPedidosWeb tbody').on('click', '.action-eliminar', handleEliminar); // Listener para eliminar

        $('#tablaPedidosWebAnulados tbody').on('click', '.action-view-voucher', handleViewVoucher);
        $('#tablaPedidosWebAnulados tbody').on('click', '.action-view-detail', handleViewDetail);
        $('#tablaPedidosWebAnulados tbody').on('click', '.action-eliminar', handleEliminar); // Listener para eliminar en tabla de anuladas


        // Event listeners para los filtros
        $('#filtroEstado, #filtroFechaDesde, #filtroFechaHasta').on('change', function() {
            dataTable.ajax.reload();
            dataTableAnuladas.ajax.reload();
        });
        $('#filtroBusqueda').on('keyup', function() {
            dataTable.ajax.reload();
            dataTableAnuladas.ajax.reload();
        });

        // Escuchar mensajes desde el iframe de edición para recargar la tabla
        window.addEventListener('message', function(event) {
            if (event.data === 'ventaActualizada') {
                editVentaModal.hide();
                dataTable.ajax.reload();
                dataTableAnuladas.ajax.reload();
                showNotification('Venta web actualizada con éxito.', 'success');
            }
        });
    }

    function handleViewVoucher() {
        const voucherPath = $(this).data('voucher');
        if (voucherPath) {
            $('#voucherImg').attr('src', voucherPath);
            $('#voucherDownloadLink').attr('href', voucherPath);
            voucherModal.show();
        } else {
            showNotification('No hay voucher disponible para este pedido.', 'error');
        }
    }

    function handleViewDetail() {
        const pedidoId = $(this).data('id');

        $.ajax({
            url: `/ventas_web/api/detalle/${pedidoId}`,
            method: 'GET',
            xhrFields: { withCredentials: true },
            success: function(response) {
                if (response.success) {
                    const pedido = response.data;
                    let itemsHtml = '';

                    pedido.items.forEach(item => {
                        itemsHtml += `
                            <tr>
                                <td>${item.producto.nombre}</td>
                                <td>${item.cantidad}</td>
                                <td>S/ ${parseFloat(item.precioUnitario).toFixed(2)}</td>
                                <td>S/ ${parseFloat(item.subtotal).toFixed(2)}</td>
                            </tr>
                        `;
                    });

                    Swal.fire({
                        title: `Detalle del Pedido #${pedido.numeroPedido}`,
                        html: `
                            <div style="text-align: left;">
                                <p><strong>Cliente:</strong> ${pedido.nombreCliente}</p>
                                <p><strong>DNI:</strong> ${pedido.dniCliente}</p>
                                <p><strong>Teléfono:</strong> ${pedido.telefonoCliente}</p>
                                <p><strong>Fecha:</strong> ${new Date(pedido.fechaPedido).toLocaleString('es-PE')}</p>
                                <p><strong>Método de Pago:</strong> ${pedido.metodoPago}</p>
                                <p><strong>Total:</strong> <span style="color: #28a745; font-weight: bold;">S/ ${parseFloat(pedido.total).toFixed(2)}</span></p>
                                <h6 class="mt-3 mb-2">Items:</h6>
                                <table class="table table-sm table-bordered">
                                    <thead>
                                        <tr>
                                            <th>Producto</th>
                                            <th>Cantidad</th>
                                            <th>Precio Unitario</th>
                                            <th>Subtotal</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        ${itemsHtml}
                                    </tbody>
                                </table>
                            </div>
                        `,
                        width: '600px',
                        confirmButtonText: 'Cerrar',
                        confirmButtonColor: '#0d6efd'
                    });
                } else {
                    Swal.fire({
                        icon: 'error',
                        title: 'Error',
                        text: 'Error al cargar el detalle: ' + response.message,
                        confirmButtonColor: '#0d6efd'
                    });
                }
            },
            error: function(xhr, status, error) {
                Swal.fire({
                    icon: 'error',
                    title: 'Error',
                    text: 'Error al cargar el detalle: ' + error,
                    confirmButtonColor: '#0d6efd'
                });
            }
        });
    }

    function handleApprove() {
        const pedidoId = $(this).data('id');
        Swal.fire({
            title: '¿Está seguro de aprobar este pedido?',
            text: 'El pedido será marcado como aprobado y podrá ser procesado.',
            icon: 'question',
            showCancelButton: true,
            confirmButtonColor: '#28a745',
            cancelButtonColor: '#6c757d',
            confirmButtonText: 'Sí, aprobar',
            cancelButtonText: 'Cancelar'
        }).then((result) => {
            if (result.isConfirmed) {
                $.ajax({
                    url: ENDPOINTS.approve(pedidoId),
                    method: 'PUT',
                    xhrFields: { withCredentials: true },
                    success: function(response) {
                        if (response.success) {
                            Swal.fire({
                                icon: 'success',
                                title: '¡Aprobado!',
                                text: 'Pedido aprobado con éxito. Redirigiendo a la lista de ventas...',
                                confirmButtonColor: '#0d6efd'
                            }).then(() => {
                                window.location.href = '/ventas/listar'; // Redirigir a la lista de ventas
                            });
                        } else {
                            Swal.fire({
                                icon: 'error',
                                title: 'Error',
                                text: 'Error al aprobar el pedido: ' + response.message,
                                confirmButtonColor: '#0d6efd'
                            });
                        }
                    },
                    error: function(xhr, status, error) {
                        Swal.fire({
                            icon: 'error',
                            title: 'Error',
                            text: 'Error al aprobar el pedido: ' + (xhr.responseJSON?.message || error),
                            confirmButtonColor: '#0d6efd'
                        });
                    }
                });
            }
        });
    }

    function handleAnular() {
        const ventaId = $(this).data('id');
        Swal.fire({
            title: '¿Está seguro de anular esta venta web?',
            text: "Esta acción cambiará el estado del pedido a 'ANULADO'. Podrá ser revisado en la sección de pedidos anulados.",
            icon: 'warning',
            showCancelButton: true,
            confirmButtonColor: '#ffc107', // Color para anular (amarillo)
            cancelButtonColor: '#6c757d',
            confirmButtonText: 'Sí, anular',
            cancelButtonText: 'Cancelar',
            input: 'text',
            inputPlaceholder: 'Motivo de anulación (opcional)',
            inputValidator: (value) => {
                return null;
            }
        }).then(result => {
            if (result.isConfirmed) {
                const motivo = result.value || '';
                $.ajax({
                    url: ENDPOINTS.anular(ventaId),
                    type: 'PUT', // Usamos PUT para actualizar el estado
                    contentType: 'application/json',
                    data: JSON.stringify({ motivo: motivo }),
                    xhrFields: { withCredentials: true },
                    success: function(response) {
                        showNotification(response.message, response.success ? 'success' : 'error');
                        if (response.success) {
                            dataTable.ajax.reload();
                            dataTableAnuladas.ajax.reload();
                        }
                    },
                    error: function(xhr, status, error) {
                        showNotification(xhr.responseJSON?.message || 'Error al anular la venta web.', 'error');
                    }
                });
            }
        });
    }

    function handleEliminar() {
        const ventaId = $(this).data('id');
        Swal.fire({
            title: '¿Está seguro de ELIMINAR esta venta web?',
            text: "Esta acción eliminará el pedido permanentemente de la base de datos, incluyendo el voucher asociado. ¡No se podrá recuperar!",
            icon: 'error', // Icono de error para una acción destructiva
            showCancelButton: true,
            confirmButtonColor: '#dc3545', // Color para eliminar (rojo)
            cancelButtonColor: '#6c757d',
            confirmButtonText: 'Sí, eliminarla',
            cancelButtonText: 'Cancelar',
            input: 'text',
            inputPlaceholder: 'Motivo de eliminación (opcional)',
            inputValidator: (value) => {
                return null;
            }
        }).then(result => {
            if (result.isConfirmed) {
                const motivo = result.value || '';
                $.ajax({
                    url: ENDPOINTS.eliminar(ventaId),
                    type: 'DELETE',
                    contentType: 'application/json',
                    data: JSON.stringify({ motivo: motivo }),
                    xhrFields: { withCredentials: true },
                    success: function(response) {
                        showNotification(response.message, response.success ? 'success' : 'error');
                        if (response.success) {
                            dataTable.ajax.reload();
                            dataTableAnuladas.ajax.reload();
                        }
                    },
                    error: function(xhr, status, error) {
                        showNotification(xhr.responseJSON?.message || 'Error al eliminar la venta web.', 'error');
                    }
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

        toastElement.addEventListener('hidden.bs.toast', () => {
            toastElement.remove();
        });
    }
});