$(document).ready(function() {
    let dataTable;
    let boletaModal, editVentaModal;

    const API_BASE = '/ventas/api';
    const ENDPOINTS = {
        list: `${API_BASE}/listar`,
        delete: (id) => `${API_BASE}/eliminar/${id}`,
        print: (id) => `/ventas/imprimir/${id}`,
    };

    initializeDataTable();
    boletaModal = new bootstrap.Modal(document.getElementById('boletaModal'));
    editVentaModal = new bootstrap.Modal(document.getElementById('editVentaModal'));
    setupEventListeners();

    function initializeDataTable(url = ENDPOINTS.list) {
        if (dataTable) dataTable.destroy();
        dataTable = $('#tablaVentas').DataTable({
            responsive: true,
            processing: true,
            ajax: { url: url, dataSrc: 'data' },
            columns: [
                { data: 'id' }, { data: 'numeroVenta' }, { data: 'nombreCliente' },
                { data: 'fechaVenta', render: data => new Date(data).toLocaleString('es-PE') },
                { data: 'metodoPago' }, { data: 'tipoComprobante' },
                { data: 'descuento', render: data => `S/ ${parseFloat(data).toFixed(2)}` }, // Nueva columna
                { data: 'total', render: data => `S/ ${parseFloat(data).toFixed(2)}` },
                { data: 'nota' },
                {
                    data: null, orderable: false, searchable: false,
                    render: (data, type, row) => `
                        <button class="btn btn-sm btn-info action-edit" data-id="${row.id}" title="Editar Venta"><i class="bi bi-pencil-fill"></i></button>
                        <button class="btn btn-sm btn-danger action-delete" data-id="${row.id}" title="Anular Venta"><i class="bi bi-trash-fill"></i></button>
                        <button class="btn btn-sm btn-primary action-print" data-id="${row.id}" title="Imprimir Boleta"><i class="bi bi-printer-fill"></i></button>
                    `
                }
            ],
            language: { url: "//cdn.datatables.net/plug-ins/1.13.6/i18n/es-ES.json" },
            order: [[0, 'desc']]
        });
    }

    function setupEventListeners() {
        $('#btnFiltrar').on('click', handleFiltrar);
        $('#btnLimpiar').on('click', handleLimpiar);
        $('#tablaVentas tbody').on('click', '.action-print', handlePrint);
        $('#tablaVentas tbody').on('click', '.action-delete', handleDelete);
        $('#tablaVentas tbody').on('click', '.action-edit', handleEdit);
        $('#btnPrintBoleta').on('click', () => document.getElementById('boletaIframe').contentWindow.print());

        // Escuchar mensajes desde el iframe de edición
        window.addEventListener('message', function(event) {
            if (event.data === 'ventaActualizada') {
                editVentaModal.hide();
                dataTable.ajax.reload();
                showNotification('Venta actualizada con éxito.', 'success');
            }
        });
    }

    function handleEdit() {
        const ventaId = $(this).data('id');
        const iframe = $('#editVentaIframe');
        iframe.attr('src', `/ventas/modificar/${ventaId}`);
        editVentaModal.show();
    }

    function handleFiltrar() {
        const desde = $('#fechaDesde').val(), hasta = $('#fechaHasta').val();
        if (desde && hasta) {
            initializeDataTable(`${ENDPOINTS.list}?desde=${desde}&hasta=${hasta}`);
            showNotification('Mostrando ventas para el rango seleccionado.', 'info');
        } else {
            showNotification('Por favor, seleccione ambas fechas para filtrar.', 'error');
        }
    }

    function handleLimpiar() {
        $('#fechaDesde').val('');
        $('#fechaHasta').val('');
        initializeDataTable();
        showNotification('Filtros limpiados.', 'success');
    }

    function handlePrint() {
        const ventaId = $(this).data('id');
        if (ventaId) {
            $('#boletaIframe').attr('src', ENDPOINTS.print(ventaId));
            boletaModal.show();
        } else {
            showNotification('No se pudo obtener el ID de la venta para imprimir.', 'error');
        }
    }

    function handleDelete() {
        const ventaId = $(this).data('id');
        Swal.fire({
            title: '¿Estás seguro?',
            text: "La venta será marcada como eliminada lógicamente y dejará de mostrarse en el listado de ventas. No podrás revertir esto.",
            icon: 'warning',
            showCancelButton: true, confirmButtonColor: '#d33', cancelButtonColor: '#3085d6',
            confirmButtonText: 'Sí, anular', cancelButtonText: 'Cancelar'
        }).then(result => {
            if (result.isConfirmed) {
                $.ajax({
                    url: ENDPOINTS.delete(ventaId), type: 'DELETE',
                    success: function(response) {
                        showNotification(response.message, response.success ? 'success' : 'error');
                        if (response.success) dataTable.ajax.reload();
                    },
                    error: (xhr) => showNotification(xhr.responseJSON?.message || 'Error al anular la venta.', 'error')
                });
            }
        });
    }

    function showNotification(message, type = 'success') {
        const toastContainer = $('#notification-container');
        const toastClass = type === 'success' ? 'text-bg-success' : (type === 'error' ? 'text-bg-danger' : 'text-bg-info');
        const toastHTML = `
            <div class="toast align-items-center ${toastClass} border-0" role="alert" aria-live="assertive" aria-atomic="true">
                <div class="d-flex">
                    <div class="toast-body">${message}</div>
                    <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
                </div>
            </div>`;
        toastContainer.append(toastHTML);
        const toast = new bootstrap.Toast(toastContainer.children().last(), { delay: 3000 });
        toast.show();
    }
});
