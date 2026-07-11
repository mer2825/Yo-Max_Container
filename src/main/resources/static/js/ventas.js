
$(document).ready(function() {
    let dataTable;
    let boletaModal, editVentaModal;

    const API_BASE = '/ventas/api';
    const ENDPOINTS = {
        list: `${API_BASE}/listar`,
        delete: (id) => `${API_BASE}/eliminar/${id}`,
        print: (id) => `/ventas/imprimir/${id}`
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
            rowCallback: function(row, data) {
                // Cambio 7: Color de fila según estado NC
                const estadoNC = data.estadoNotaCredito;
                if (estadoNC) {
                    if (estadoNC.toUpperCase() === 'TOTAL') {
                        $(row).addClass('fila-nc-total');
                    } else if (estadoNC.toUpperCase() === 'PARCIAL') {
                        $(row).addClass('fila-nc-parcial');
                    }
                }
                return row;
            },
            columns: [
                { data: 'id' }, { data: 'nombreCliente' },
                { data: 'fechaVenta', render: data => new Date(data).toLocaleString('es-PE') },
                { data: 'metodoPago' },
                {
                    data: 'tipoComprobante', render: data => {
                        const tipo = data ? data.toLowerCase() : '';
                        if (tipo.includes('boleta')) return '<span class="badge bg-success">Boleta</span>';
                        if (tipo.includes('factura')) return '<span class="badge bg-primary">Factura</span>';
                        if (tipo.includes('nota')) return '<span class="badge bg-secondary">Nota de Venta</span>';
                        return `<span class="badge bg-secondary">${data || 'N/A'}</span>`;
                    }
                },
                { data: 'serieCorrelativo', render: data => data ? data : '-' },
                {
                    data: 'estadoSunat', render: data => {
                        if (!data) return '-';
                        const estado = data.toLowerCase();
                        if (estado === 'aceptado') return '<span class="badge bg-success">Aceptado</span>';
                        if (estado === 'rechazado') return '<span class="badge bg-danger">Rechazado</span>';
                        return '<span class="badge bg-warning text-dark">Pendiente</span>';
                    }
                },
                {
                    data: null, orderable: false, searchable: false,
                    render: (data, type, row) => {
                        let html = '';
                        if (row.pdfUrl) {
                            html += `<a href="${row.pdfUrl}" target="_blank" class="me-1 text-decoration-none" title="PDF Original" style="display:inline-flex;align-items:center;gap:2px;background:#fcebeb;color:#a32d2d;border-radius:4px;padding:3px 6px;font-size:9px"><i class="bi bi-file-earmark-pdf-fill"></i> PDF</a>`;
                        }
                        if (row.xmlUrl) {
                            html += `<a href="${row.xmlUrl}" target="_blank" class="me-1 text-decoration-none" title="XML Original" style="display:inline-flex;align-items:center;gap:2px;background:#e8f0fe;color:#1a56db;border-radius:4px;padding:3px 6px;font-size:9px"><i class="bi bi-file-earmark-code-fill"></i> XML</a>`;
                        }
                        // Agregar botones de NC si existe
                        if (row.estadoNotaCredito && row.ncPdfUrl) {
                            html += `<a href="${row.ncPdfUrl}" target="_blank" class="text-decoration-none" title="PDF Nota de Crédito" style="display:inline-flex;align-items:center;gap:2px;background:#fce4f3;color:#a3006b;border-radius:4px;padding:3px 6px;font-size:9px"><i class="bi bi-file-earmark-pdf-fill"></i> NC</a>`;
                        }
                        return html || '-';
                    }
                },
                { data: 'descuento', render: data => `S/ ${parseFloat(data).toFixed(2)}` },
                {
                    data: null, orderable: false, searchable: false,
                    render: (data, type, row) => {
                        const estadoNC = row.estadoNotaCredito;
                        const total = parseFloat(row.total).toFixed(2);
                        const isTotal = estadoNC && estadoNC.toUpperCase() === 'TOTAL';

                        let totalHtml = '';
                        if (isTotal) {
                            totalHtml = `<span style="text-decoration:line-through;color:#aaa">S/ ${total}</span>`;
                        } else {
                            totalHtml = `S/ ${total}`;
                        }

                        // Monto acreditado si hay NC
                        if (estadoNC && row.ncPdfUrl) {
                            const totalAcreditado = row.ncTotalAcreditado ? parseFloat(row.ncTotalAcreditado).toFixed(2) : parseFloat(row.total).toFixed(2);
                            totalHtml += `<div style="font-size:9px;color:#a32d2d;margin-top:2px">NC: −S/ ${totalAcreditado}</div>`;
                        }

                        return totalHtml;
                    }
                },
                { data: 'nota' },
                {
                    data: null, orderable: false, searchable: false,
                    render: (data, type, row) => {
                        const estadoNC = row.estadoNotaCredito;
                        const ncSerieCorrelativo = row.ncSerieCorrelativo;
                        const ncPdfUrl = row.ncPdfUrl;

                        if (!estadoNC) {
                            return '<span class="text-muted">-</span>';
                        }

                        const isTotal = estadoNC.toUpperCase() === 'TOTAL';
                        const isParcial = estadoNC.toUpperCase() === 'PARCIAL';

                        if (isTotal) {
                            const badge = '<span class="badge bg-secondary">NC Total</span>';
                            if (ncPdfUrl) {
                                return `${badge} <a href="${ncPdfUrl}" target="_blank" class="ms-1" title="Ver PDF NC"><i class="bi bi-file-earmark-pdf-fill text-danger"></i></a>`;
                            }
                            return badge;
                        }

                        if (isParcial) {
                            const badge = '<span class="badge bg-warning text-dark">NC Parcial</span>';
                            if (ncPdfUrl) {
                                return `${badge} <a href="${ncPdfUrl}" target="_blank" class="ms-1" title="Ver PDF NC"><i class="bi bi-file-earmark-pdf-fill text-danger"></i></a>`;
                            }
                            return badge;
                        }

                        return `<span class="badge bg-info">${estadoNC}</span>`;
                    }
                },
                {
                    data: null, orderable: false, searchable: false,
                    render: (data, type, row) => {
                        const tipo = (row.tipoComprobante || '').toLowerCase();
                        const estadoNC = row.estadoNotaCredito;
                        const estadoSunat = (row.estadoSunat || '').toLowerCase();
                        const isNotaVenta = tipo.includes('nota_venta') || tipo.includes('nota de venta');
                        const isParcial = estadoNC && estadoNC.toUpperCase() === 'PARCIAL';
                        const isTotal = estadoNC && estadoNC.toUpperCase() === 'TOTAL';
                        const isRechazado = estadoSunat === 'rechazado';

                        // Para Notas de Venta: editar y anular normalmente
                        if (isNotaVenta) {
                            return `
                                <button class="btn btn-sm btn-info action-edit" data-id="${row.id}" title="Editar Venta"><i class="bi bi-pencil-fill"></i></button>
                                <button class="btn btn-sm btn-danger action-delete" data-id="${row.id}" title="Anular Venta"><i class="bi bi-trash-fill"></i></button>
                                <a href="/ventas/imprimir/${row.id}" target="_blank" class="btn btn-sm btn-primary" title="Imprimir" style="margin-left:3px"><i class="bi bi-printer-fill"></i></a>
                            `;
                        }

                        // Para Boletas/Facturas con NC Total: todo deshabilitado
                        if (isTotal) {
                            const verNcLink = row.ncPdfUrl ? 
                                `<a href="/ventas/nota-credito/${row.id}/ver" target="_blank" class="btn btn-sm btn-success" title="Ver NC" style="margin-left:3px"><i class="bi bi-printer-fill"></i></a>` : '';
                            return `
                                <span style="background:#e0e0e0;color:#999;border-radius:4px;padding:4px 7px;font-size:9px;cursor:not-allowed" title="No editable">✏</span>
                                <span style="background:#e0e0e0;color:#999;border-radius:4px;padding:4px 7px;font-size:9px;cursor:not-allowed;margin-left:3px" title="NC total emitida">NC emitida</span>
                                ${verNcLink}
                            `;
                        }

                        // Para Boletas/Facturas con NC Parcial: mostrar "Ver NC"
                        if (isParcial) {
                            const verNcLink = row.ncPdfUrl ? 
                                `<a href="/ventas/nota-credito/${row.id}/ver" target="_blank" class="btn btn-sm" style="background:#fce4f3;color:#a3006b;border-radius:4px;padding:4px 7px;font-size:9px;text-decoration:none;margin-left:3px" title="Ver NC">Ver NC</a>` : '';
                            return `
                                <span style="background:#e0e0e0;color:#999;border-radius:4px;padding:4px 7px;font-size:9px;cursor:not-allowed" title="No editable — tiene NC parcial">✏</span>
                                <a href="/ventas/nota-credito/${row.id}" class="btn btn-sm" style="background:#f0ad4e;color:#000;border-radius:4px;padding:4px 7px;font-size:9px;text-decoration:none;margin-left:3px" title="Emitir NC adicional">📋 NC Parcial</a>
                                ${verNcLink}
                                <a href="/ventas/imprimir/${row.id}" target="_blank" class="btn btn-sm btn-primary" title="Imprimir" style="margin-left:3px"><i class="bi bi-printer-fill"></i></a>
                            `;
                        }

                        // Para Boletas/Facturas sin NC y no rechazadas: botón de emitir NC y cambiar producto
                        if (!isRechazado) {
                            return `
                                <span style="background:#e0e0e0;color:#999;border-radius:4px;padding:4px 7px;font-size:9px;cursor:not-allowed" title="No editable — comprobante emitido a SUNAT">✏</span>
                                <a href="/ventas/nota-credito/${row.id}" class="btn btn-sm" style="background:#d63384;color:#fff;border-radius:4px;padding:4px 7px;font-size:9px;text-decoration:none;margin-left:3px" title="Emitir Nota de Crédito">📋 Emitir NC</a>
                                <a href="/ventas/nota-credito/${row.id}/cambiar-producto" class="btn btn-sm" style="background:#ffc107;color:#000;border-radius:4px;padding:4px 7px;font-size:9px;text-decoration:none;margin-left:3px" title="Cambiar Producto">🔄 Cambiar</a>
                                <a href="/ventas/imprimir/${row.id}" target="_blank" class="btn btn-sm btn-primary" title="Imprimir" style="margin-left:3px"><i class="bi bi-printer-fill"></i></a>
                            `;
                        }

                        // Para Boletas/Facturas rechazadas: solo imprimir
                        return `
                            <span style="background:#e0e0e0;color:#999;border-radius:4px;padding:4px 7px;font-size:9px;cursor:not-allowed" title="Comprobante rechazado">✏</span>
                            <a href="/ventas/imprimir/${row.id}" target="_blank" class="btn btn-sm btn-primary" title="Imprimir" style="margin-left:3px"><i class="bi bi-printer-fill"></i></a>
                        `;
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
        $('#tablaVentas tbody').on('click', '.action-print', handlePrint);
        $('#tablaVentas tbody').on('click', '.action-delete', handleDelete);
        $('#tablaVentas tbody').on('click', '.action-edit', handleEdit);
        $('#btnPrintBoleta').on('click', () => document.getElementById('boletaIframe').contentWindow.print());

        window.addEventListener('message', function(event) {
            if (event.data === 'ventaActualizada') {
                editVentaModal.hide();
                dataTable.ajax.reload();
                showNotification('Venta actualizada con éxito.', 'success');
            } else if (event.data === 'cerrarModal') {
                editVentaModal.hide();
            }
        });

        // Limpiar src del iframe al cerrar el modal
        $('#editVentaModal').on('hidden.bs.modal', function () {
            $('#editVentaIframe').attr('src', '');
        });
    }

    function handleEdit() {
        const ventaId = $(this).data('id');
        if (ventaId) {
            $('#editVentaIframe').attr('src', `/ventas/modificar/${ventaId}`);
            editVentaModal.show();
        } else {
            showNotification('No se pudo obtener el ID de la venta para editar.', 'error');
        }
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