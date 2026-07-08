$(document).ready(function() {
    let dataTable;
    let ncDetailModal;

    const API_BASE = '/ventas/nota-credito/api';
    const ENDPOINTS = {
        list: `${API_BASE}/listar`,
        detail: (id) => `${API_BASE}/detalle/${id}`,
        pdf: (id) => `/ventas/nota-credito/${id}/pdf`,
        refresh: (ncId) => `/ventas/nota-credito/refrescar/${ncId}`
    };

    initializeDataTable();
    ncDetailModal = new bootstrap.Modal(document.getElementById('ncDetailModal'));
    setupEventListeners();

    function initializeDataTable(url = ENDPOINTS.list) {
        if (dataTable) dataTable.destroy();
        dataTable = $('#tablaNotasCredito').DataTable({
            responsive: true,
            processing: true,
            ajax: { url: url, dataSrc: 'data' },
            columns: [
                { data: 'id' },
                {
                    data: 'serieCorrelativo',
                    render: data => data ? data : '-'
                },
                {
                    data: 'tipoNota',
                    render: function(data, type, row) {
                        const descripcion = row.descripcionTipo || '';
                        let badgeClass = 'badge-nc-otros';
                        let label = data || '-';
                        if (data === '01' || data === '02') {
                            badgeClass = 'badge-nc-anulacion';
                            label = descripcion || 'Anulación';
                        } else if (data === '06' || data === '07') {
                            badgeClass = 'badge-nc-devolucion';
                            label = descripcion || 'Devolución';
                        } else if (data === '09') {
                            badgeClass = 'badge-nc-descuento';
                            label = descripcion || 'Descuento';
                        }
                        return `<span class="badge ${badgeClass}">${label}</span>`;
                    }
                },
                {
                    data: 'numeroVenta',
                    render: data => data ? `<a href="/ventas/nota-credito/${data}/ver" class="text-decoration-none fw-bold">#${data}</a>` : '-'
                },
                { data: 'nombreCliente' },
                {
                    data: 'serieCorrelativoVenta',
                    render: data => data ? data : '-'
                },
                {
                    data: 'motivo',
                    render: data => {
                        if (!data) return '-';
                        return data.length > 50 ? `<span title="${data.replace(/"/g, '"')}">${data.substring(0, 50)}...</span>` : data;
                    }
                },
                {
                    data: 'totalAcreditado',
                    render: data => `S/ ${parseFloat(data || 0).toFixed(2)}`
                },
                {
                    data: 'estadoSunat',
                    render: data => {
                        if (!data) return '<span class="badge bg-secondary">-</span>';
                        const estado = data.toLowerCase();
                        if (estado === 'aceptado') return '<span class="badge bg-success">Aceptado</span>';
                        if (estado === 'rechazado') return '<span class="badge bg-danger">Rechazado</span>';
                        if (estado === 'pendiente') return '<span class="badge bg-warning text-dark">Pendiente</span>';
                        return `<span class="badge bg-info">${data}</span>`;
                    }
                },
                {
                    data: 'fechaEmision',
                    render: data => data ? new Date(data).toLocaleString('es-PE') : '-'
                },
                {
                    data: 'emitidoPor',
                    render: data => data ? data : '-'
                },
                {
                    data: null,
                    orderable: false,
                    searchable: false,
                    render: function(data, type, row) {
                        let html = '<div class="d-flex gap-1">';

                        if (row.pdfUrl) {
                            html += `<a href="${row.pdfUrl}" target="_blank" class="btn btn-sm btn-outline-danger" title="Ver PDF"><i class="bi bi-file-earmark-pdf-fill"></i></a>`;
                        }
                        if (row.xmlUrl) {
                            html += `<a href="${row.xmlUrl}" target="_blank" class="btn btn-sm btn-outline-primary" title="Ver XML"><i class="bi bi-file-earmark-code-fill"></i></a>`;
                        }

                        // Botón detalle
                        html += `<button class="btn btn-sm btn-outline-info action-detail" data-id="${row.id}" title="Ver detalle"><i class="bi bi-eye-fill"></i></button>`;

                        // Botón refrescar si no está aceptado
                        const estado = (row.estadoSunat || '').toLowerCase();
                        if (estado !== 'aceptado') {
                            html += `<button class="btn btn-sm btn-outline-secondary action-refresh" data-id="${row.id}" title="Refrescar estado SUNAT"><i class="bi bi-arrow-clockwise"></i></button>`;
                        }

                        html += '</div>';
                        return html;
                    }
                }
            ],
            language: { url: "//cdn.datatables.net/plug-ins/1.13.6/i18n/es-ES.json" },
            order: [[0, 'desc']]
        });
    }

    function setupEventListeners() {
        $('#btnFiltrar').on('click', handleFiltrar);
        $('#btnLimpiar').on('click', handleLimpiar);
        $('#tablaNotasCredito tbody').on('click', '.action-detail', handleDetail);
        $('#tablaNotasCredito tbody').on('click', '.action-refresh', handleRefresh);
    }

    function handleFiltrar() {
        const desde = $('#fechaDesde').val();
        const hasta = $('#fechaHasta').val();
        if (desde && hasta) {
            initializeDataTable(`${ENDPOINTS.list}?desde=${desde}&hasta=${hasta}`);
            showNotification('Mostrando notas de crédito para el rango seleccionado.', 'info');
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

    function handleDetail() {
        const ncId = $(this).data('id');
        if (!ncId) {
            showNotification('No se pudo obtener el ID de la nota de crédito.', 'error');
            return;
        }

        $('#ncDetailBody').html(`
            <div class="text-center py-4">
                <div class="spinner-border text-primary" role="status">
                    <span class="visually-hidden">Cargando...</span>
                </div>
                <p class="mt-2">Cargando detalle...</p>
            </div>
        `);
        ncDetailModal.show();

        $.ajax({
            url: ENDPOINTS.detail(ncId),
            type: 'GET',
            success: function(response) {
                if (response.success && response.data) {
                    renderDetail(response.data);
                } else {
                    $('#ncDetailBody').html(`<div class="alert alert-danger">${response.message || 'Error al cargar detalle.'}</div>`);
                }
            },
            error: function(xhr) {
                $('#ncDetailBody').html(`<div class="alert alert-danger">${xhr.responseJSON?.message || 'Error al consultar el detalle.'}</div>`);
            }
        });
    }

    function renderDetail(data) {
        const estadoClass = (data.estadoSunat || '').toLowerCase() === 'aceptado' ? 'success' :
                           (data.estadoSunat || '').toLowerCase() === 'rechazado' ? 'danger' : 'warning';

        const html = `
            <div class="row">
                <div class="col-md-6">
                    <table class="table table-sm table-bordered">
                        <tr><th class="w-40">ID</th><td>${data.id || '-'}</td></tr>
                        <tr><th>Serie-Correlativo</th><td><strong>${data.serieCorrelativo || '-'}</strong></td></tr>
                        <tr><th>Tipo Nota</th><td>${data.descripcionTipo || data.tipoNota || '-'}</td></tr>
                        <tr><th>Motivo</th><td>${data.motivo || '-'}</td></tr>
                        <tr><th>Total Acreditado</th><td><strong>S/ ${parseFloat(data.totalAcreditado || 0).toFixed(2)}</strong></td></tr>
                        <tr><th>Estado SUNAT</th><td><span class="badge bg-${estadoClass}">${data.estadoSunat || 'Pendiente'}</span></td></tr>
                        <tr><th>Fecha Emisión</th><td>${data.fechaEmision ? new Date(data.fechaEmision).toLocaleString('es-PE') : '-'}</td></tr>
                        <tr><th>Emitido por</th><td>${data.emitidoPor || '-'}</td></tr>
                    </table>
                </div>
                <div class="col-md-6">
                    <table class="table table-sm table-bordered">
                        <tr><th class="w-40">Venta Origen</th><td>#${data.numeroVenta || '-'}</td></tr>
                        <tr><th>Cliente</th><td>${data.nombreCliente || '-'}</td></tr>
                        <tr><th>Doc. Cliente</th><td>${data.docCliente || '-'}</td></tr>
                        <tr><th>Comprobante Origen</th><td>${data.serieCorrelativoVenta || '-'}</td></tr>
                        <tr><th>Total Venta</th><td>S/ ${parseFloat(data.totalVenta || 0).toFixed(2)}</td></tr>
                    </table>
                </div>
            </div>

            <div class="mt-2 d-flex gap-2">
                ${data.pdfUrl ? `<a href="${data.pdfUrl}" target="_blank" class="btn btn-sm btn-danger"><i class="bi bi-file-earmark-pdf-fill"></i> Ver PDF</a>` : ''}
                ${data.xmlUrl ? `<a href="${data.xmlUrl}" target="_blank" class="btn btn-sm btn-primary"><i class="bi bi-file-earmark-code-fill"></i> Ver XML</a>` : ''}
                <button type="button" class="btn btn-sm btn-secondary" data-bs-dismiss="modal">Cerrar</button>
            </div>
        `;
        $('#ncDetailBody').html(html);
    }

    function handleRefresh() {
        const ncId = $(this).data('id');
        const btn = $(this);

        btn.prop('disabled', true).html('<span class="spinner-border spinner-border-sm"></span>');

        $.ajax({
            url: ENDPOINTS.refresh(ncId),
            type: 'GET',
            success: function(response) {
                if (response.estadoSunat) {
                    showNotification(`Estado actualizado: ${response.estadoSunat}`, 'success');
                } else {
                    showNotification('Estado consultado.', 'info');
                }
                dataTable.ajax.reload(null, false);
            },
            error: function(xhr) {
                const msg = xhr.responseJSON?.error || 'Error al refrescar estado.';
                showNotification(msg, 'error');
                btn.prop('disabled', false).html('<i class="bi bi-arrow-clockwise"></i>');
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