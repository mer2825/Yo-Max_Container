$(document).ready(function() {

    // Función para mostrar notificaciones (toast)
    function showNotification(message, type = 'success') {
        let toastClass;
        let btnCloseClass = 'btn-close-white';

        switch (type) {
            case 'success':
                toastClass = 'text-bg-success';
                break;
            case 'danger':
                toastClass = 'text-bg-danger';
                break;
            case 'warning':
                toastClass = 'text-bg-warning';
                btnCloseClass = ''; // Botón oscuro para fondo claro
                break;
            default:
                toastClass = 'text-bg-secondary';
        }

        const notification = $(`
            <div class="toast align-items-center ${toastClass} border-0" role="alert" aria-live="assertive" aria-atomic="true">
                <div class="d-flex">
                    <div class="toast-body">
                        ${message}
                    </div>
                    <button type="button" class="btn-close ${btnCloseClass} me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
                </div>
            </div>
        `);

        $('#notification-container').append(notification);

        const toast = new bootstrap.Toast(notification, {
            delay: 7000 // Mayor tiempo para alertas
        });
        toast.show();
    }

    function actualizarResumenInventario() {
        var total = 0;
        var lowStock = 0;
        var outOfStock = 0;

        $('#tablaInventario tbody tr').each(function() {
            total += 1;
            var stock = parseInt($(this).find('td').eq(3).text().trim(), 10);
            var stockMinimo = parseInt($(this).find('td').eq(4).text().trim(), 10);

            if (!isNaN(stock)) {
                if (stock === 0) {
                    outOfStock += 1;
                }
                if (!isNaN(stockMinimo) && stock <= stockMinimo) {
                    lowStock += 1;
                }
            }
        });

        $('#summaryTotalProductos').text(total);
        $('#summaryLowStock').text(lowStock);
        $('#summarySinStock').text(outOfStock);
    }

    var activeFilter = 'all';

    function refreshFilterButtons() {
        $('#filterAll').removeClass('btn-primary').addClass('btn-outline-secondary');
        $('#filterLowStock').removeClass('btn-primary').addClass('btn-outline-warning');
        $('#filterOutOfStock').removeClass('btn-primary').addClass('btn-outline-danger');

        switch (activeFilter) {
            case 'low':
                $('#filterLowStock').removeClass('btn-outline-warning').addClass('btn-primary');
                break;
            case 'out':
                $('#filterOutOfStock').removeClass('btn-outline-danger').addClass('btn-primary');
                break;
            default:
                $('#filterAll').removeClass('btn-outline-secondary').addClass('btn-primary');
                break;
        }
    }

    $.fn.dataTable.ext.search.push(function(settings, data, dataIndex) {
        if (settings.nTable.id !== 'tablaInventario') {
            return true;
        }

        var rowNode = settings.aoData[dataIndex] && settings.aoData[dataIndex].nTr;
        if (!rowNode) {
            return true;
        }

        var stock = parseInt($(rowNode).find('td').eq(3).text().trim(), 10);
        var stockMinimo = parseInt($(rowNode).find('td').eq(4).text().trim(), 10);

        if (activeFilter === 'low') {
            return !isNaN(stock) && !isNaN(stockMinimo) && stock > 0 && stock <= stockMinimo;
        }

        if (activeFilter === 'out') {
            return !isNaN(stock) && stock === 0;
        }

        return true;
    });

    // Inicializar DataTable
    var tablaInventario = $('#tablaInventario').on('init.dt', function() {
        // Este evento se dispara una vez que la tabla está completamente inicializada.
        // Es el lugar ideal para recorrer todos los datos y mostrar las alertas una sola vez.
        var api = new $.fn.dataTable.Api(this);
        api.rows().every(function() {
            var rowNode = this.node();
            var nombreProducto = $(rowNode).find('td').eq(1).text();
            var stock = parseInt($(rowNode).find('td').eq(3).text().trim(), 10);
            var stockMinimo = parseInt($(rowNode).find('td').eq(4).text().trim(), 10);

            if (!isNaN(stock) && !isNaN(stockMinimo) && stock <= stockMinimo) {
                showNotification(`Alerta de Stock: "${nombreProducto}" tiene ${stock} unidades (mínimo ${stockMinimo}).`, 'warning');
            }
        });
        refreshFilterButtons();
        actualizarResumenInventario();
    }).DataTable({
        "responsive": true,
        "autoWidth": false,
        "language": {
            "url": "//cdn.datatables.net/plug-ins/1.13.6/i18n/es-ES.json"
        },
        "columnDefs": [
            { "orderable": false, "targets": [0, 5] } // Deshabilitar ordenación
        ],
        "createdRow": function(row, data, dataIndex) {
            // Este callback es ideal para modificar la fila (TR) después de ser creada.
            // 'data' es un array con el contenido de las celdas de la fila.
            const stock = parseInt(data[3], 10);
            const stockMinimo = parseInt(data[4], 10);
            const stockCell = $('td', row).eq(3);

            if (!isNaN(stock) && !isNaN(stockMinimo)) {
                let colorClass = stock <= stockMinimo ? 'text-bg-danger' : 'text-bg-success';
                stockCell.html(`<span class="badge ${colorClass}">${stock}</span>`);
            }
        }
    });

    var currentProductId = null;

    $('#inputFiltroInventario').on('input', function() {
        tablaInventario.search(this.value).draw();
    });

    $('#filterAll').on('click', function() {
        activeFilter = 'all';
        refreshFilterButtons();
        tablaInventario.draw();
    });

    $('#filterLowStock').on('click', function() {
        activeFilter = 'low';
        refreshFilterButtons();
        tablaInventario.draw();
    });

    $('#filterOutOfStock').on('click', function() {
        activeFilter = 'out';
        refreshFilterButtons();
        tablaInventario.draw();
    });

    // Manejar el clic en el botón 'Movimientos'
    $('#tablaInventario tbody').on('click', '.btn-movimientos', function() {
        currentProductId = $(this).data('id');
        var productName = $(this).data('nombre');
        var currentStock = $(this).data('stock');
        var currentStockMinimo = $(this).data('stock-minimo');

        $('#modalProductoNombre').text(productName);
        $('#modalStockActual').text(currentStock);
        $('#modalStockMinimo').text(currentStockMinimo);
        $('#inputStock').val(currentStock);
        $('#inputStockMinimo').val(currentStockMinimo);
        $('#tablaMovimientos tbody').empty();

        cargarMovimientos(currentProductId);

        var movimientosModal = new bootstrap.Modal(document.getElementById('movimientosModal'));
        movimientosModal.show();
    });

    function cargarMovimientos(productId, callback) {
        $.ajax({
            url: '/inventario/api/movimientos/' + productId,
            method: 'GET',
            success: function(response) {
                if (response.success) {
                    var movimientos = response.data;
                    if (movimientos.length > 0) {
                        movimientos.forEach(function(movimiento) {
                            $('#tablaMovimientos tbody').append(
                                '<tr>' +
                                    '<td>' + movimiento.numeroVenta + '</td>' +
                                    '<td>' + (movimiento.fechaVenta ? new Date(movimiento.fechaVenta).toLocaleDateString() : '') + '</td>' +
                                    '<td>' + movimiento.precioVenta.toFixed(2) + '</td>' +
                                    '<td>' + movimiento.cantidad + '</td>' +
                                    '<td>' + movimiento.subtotal.toFixed(2) + '</td>' +
                                    '<td>' + (movimiento.comentario ? movimiento.comentario : '-') + '</td>' +
                                '</tr>'
                            );
                        });
                    } else {
                        $('#tablaMovimientos tbody').append('<tr><td colspan="5" class="text-center">No hay movimientos para este producto.</td></tr>');
                    }
                } else {
                    console.error('Error al cargar movimientos:', response.message);
                    $('#tablaMovimientos tbody').append('<tr><td colspan="5" class="text-center">Error al cargar movimientos.</td></tr>');
                }
                if (typeof callback === 'function') callback();
            },
            error: function(xhr, status, error) {
                console.error('Erro r en la petición AJAX para movimientos:', error);
                $('#tablaMovimientos tbody').append('<tr><td colspan="5" class="text-center">Error de conexión al cargar movimientos.</td></tr>');
                if (typeof callback === 'function') callback();
            }
        });
    }

    var pendingNewStock = null;
    var pendingNewStockMinimo = null;

    $('#btnAjustarStock').on('click', function() {
        if (!currentProductId) {
            showNotification('No se ha seleccionado ningún producto.', 'danger');
            return;
        }

        var newStock = parseInt($('#inputStock').val(), 10);
        var newStockMinimo = parseInt($('#inputStockMinimo').val(), 10);
        var newComentario = $('#inputStockComentario').val().trim();

        if (isNaN(newStock) || newStock < 0) {
            showNotification('Ingresa un valor de stock válido.', 'danger');
            return;
        }

        pendingNewStock = newStock;
        pendingNewStockMinimo = isNaN(newStockMinimo) ? null : newStockMinimo;
        pendingNewComentario = newComentario;

        $('#confirmSaveMessage').text(`Confirma guardar el ajuste: Stock ${pendingNewStock}` + (pendingNewStockMinimo !== null ? `, Stock mínimo ${pendingNewStockMinimo}` : '') + (pendingNewComentario ? `, Comentario: ${pendingNewComentario}` : ''));
        var confirmModal = new bootstrap.Modal(document.getElementById('confirmSaveModal'));
        confirmModal.show();
    });

    $('#confirmSaveBtn').on('click', function() {
        if (!currentProductId || pendingNewStock === null) {
            showNotification('No hay datos para guardar.', 'danger');
            return;
        }

        var $btn = $(this);
        $btn.prop('disabled', true);

        $.ajax({
            url: '/inventario/api/ajustar-stock/' + currentProductId,
            method: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({
                stock: pendingNewStock,
                stockMinimo: pendingNewStockMinimo,
                comentario: pendingNewComentario
            }),
            success: function(response) {
                $btn.prop('disabled', false);
                var confirmModalEl = document.getElementById('confirmSaveModal');
                var confirmInstance = bootstrap.Modal.getInstance(confirmModalEl);
                if (confirmInstance) confirmInstance.hide();

                if (response.success) {
                    showNotification(response.message, 'success');
                    actualizarFilaProducto(currentProductId, response.producto);
                    // Recargar movimientos y luego cerrar el modal principal
                    $('#tablaMovimientos tbody').empty();
                    cargarMovimientos(currentProductId, function() {
                        var movimientosModalEl = document.getElementById('movimientosModal');
                        var movimientosInstance = bootstrap.Modal.getInstance(movimientosModalEl);
                        if (movimientosInstance) movimientosInstance.hide();
                    });
                } else {
                    showNotification(response.message || 'No se pudo actualizar el stock.', 'danger');
                }

                pendingNewStock = null;
                pendingNewStockMinimo = null;
            },
            error: function(xhr, status, error) {
                $btn.prop('disabled', false);
                showNotification('Error al actualizar stock: ' + error, 'danger');
            }
        });
    });

    $('#btnIncrementarStock').on('click', function() {
        var currentValue = parseInt($('#inputStock').val(), 10);
        $('#inputStock').val(isNaN(currentValue) ? 10 : currentValue + 10);
    });

    $('#btnDecrementarStock').on('click', function() {
        var currentValue = parseInt($('#inputStock').val(), 10);
        if (isNaN(currentValue)) {
            $('#inputStock').val(0);
        } else {
            $('#inputStock').val(Math.max(0, currentValue - 10));
        }
    });

    function actualizarFilaProducto(productId, producto) {
        var fila = $('#tablaInventario tbody').find('button[data-id="' + productId + '"]').closest('tr');
        if (fila.length === 0) return;
        fila.find('td').eq(3).html('<span class="badge ' + (producto.stock <= producto.stockMinimo ? 'text-bg-danger' : 'text-bg-success') + '">' + producto.stock + '</span>');
        fila.find('td').eq(4).text(producto.stockMinimo != null ? producto.stockMinimo : '-');
        fila.find('.btn-movimientos').data('stock', producto.stock).data('stock-minimo', producto.stockMinimo);
        actualizarResumenInventario();
    }

    // Exportar a PDF
    $('#btnExportarPdf').on('click', function() {
        const { jsPDF } = window.jspdf;
        const doc = new jsPDF();
        const nombreProducto = $('#modalProductoNombre').text();

        doc.autoTable({
            html: '#tablaMovimientos',
            startY: 20,
            didDrawPage: function (data) {
                doc.text(`Movimientos de Inventario para: ${nombreProducto}`, 14, 15);
            }
        });

        doc.save(`movimientos_${nombreProducto.replace(/ /g, '_')}.pdf`);
    });

    // Exportar a Excel
    $('#btnExportarExcel').on('click', function() {
        const nombreProducto = $('#modalProductoNombre').text();
        const tabla = document.getElementById('tablaMovimientos');
        const wb = XLSX.utils.table_to_book(tabla, { sheet: "Movimientos" });
        XLSX.writeFile(wb, `movimientos_${nombreProducto.replace(/ /g, '_')}.xlsx`);
    });
});
