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
        "pageLength": 50,
        "lengthMenu": [10, 25, 50, 100, -1],
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
        $('#modalStockDiferencia').text(currentStock - currentStockMinimo);
        $('#inputStockMinimo').val(currentStockMinimo);
        $('#productoId').val(currentProductId);

        // Actualizar colores de la tarjeta de stock actual
        var cardStockActual = $('#cardStockActual');
        cardStockActual.removeClass('bg-success bg-danger');
        if (currentStock < currentStockMinimo) {
            cardStockActual.addClass('bg-danger');
            $('#alertaStockBajo').removeClass('d-none');
        } else {
            cardStockActual.addClass('bg-success');
            $('#alertaStockBajo').addClass('d-none');
        }

        // Resetear formulario de movimientos
        resetearFormularioMovimiento();

        // Cargar movimientos
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
                            var tipo = movimiento.tipoMovimiento || movimiento.tipo || '-';
                            var badgeClass = '';
                            var cantidadSign = '';

                            // Determinar badge de color según tipo
                            if (tipo === 'INGRESO') {
                                badgeClass = 'bg-success';
                                cantidadSign = '+';
                            } else if (tipo === 'SALIDA' || tipo === 'VENTA') {
                                badgeClass = 'bg-danger';
                                cantidadSign = '-';
                            } else if (tipo === 'AJUSTE') {
                                badgeClass = 'bg-warning text-dark';
                                cantidadSign = movimiento.stockResultante > movimiento.stockAnterior ? '+' : '-';
                            } else {
                                badgeClass = 'bg-secondary';
                            }

                            // Determinar si es link a venta
                            var referenciaHtml = movimiento.referenciaDocumento || '-';
                            if (movimiento.numeroVenta) {
                                referenciaHtml = '<a href="/ventas/modificar/' + movimiento.numeroVenta + '" target="_blank" class="text-decoration-none">Venta #' + movimiento.numeroVenta + '</a>';
                            }

                            $('#tablaMovimientos tbody').append(
                                '<tr>' +
                                    '<td>' + (movimiento.fecha ? new Date(movimiento.fecha).toLocaleString() : (movimiento.fechaVenta ? new Date(movimiento.fechaVenta).toLocaleString() : '')) + '</td>' +
                                    '<td><span class="badge ' + badgeClass + '">' + tipo + '</span></td>' +
                                    '<td>' + (movimiento.motivo || '-') + '</td>' +
                                    '<td>' + cantidadSign + movimiento.cantidad + '</td>' +
                                    '<td>' + (movimiento.stockResultante || '-') + '</td>' +
                                    '<td>' + referenciaHtml + '</td>' +
                                    '<td>' + (movimiento.usuario || '-') + '</td>' +
                                '</tr>'
                            );
                        });
                    } else {
                        $('#tablaMovimientos tbody').append('<tr><td colspan="7" class="text-center">No hay movimientos para este producto.</td></tr>');
                    }
                } else {
                    console.error('Error al cargar movimientos:', response.message);
                    $('#tablaMovimientos tbody').append('<tr><td colspan="7" class="text-center">Error al cargar movimientos.</td></tr>');
                }
                if (typeof callback === 'function') callback();
            },
            error: function(xhr, status, error) {
                console.error('Error en la petición AJAX para movimientos:', error);
                $('#tablaMovimientos tbody').append('<tr><td colspan="7" class="text-center">Error de conexión al cargar movimientos.</td></tr>');
                if (typeof callback === 'function') callback();
            }
        });
    }

    function resetearFormularioMovimiento() {
        $('#movimientoTipoSeleccionado').val('');
        $('.tipo-movimiento-card').removeClass('selected');
        $('#formIngreso, #formSalida, #formAjuste').addClass('d-none');
        $('#formMovimiento')[0].reset();
        $('#btnConfirmarMovimiento').text('Registrar movimiento').prop('disabled', true);
    }

    // Manejar selección de tipo de movimiento
    $('.tipo-movimiento-card').on('click', function() {
        var tipo = $(this).data('tipo');
        $('#movimientoTipoSeleccionado').val(tipo);

        $('.tipo-movimiento-card').removeClass('selected');
        $(this).addClass('selected');

        // Ocultar todos los formularios
        $('#formIngreso, #formSalida, #formAjuste').addClass('d-none');

        // Mostrar formulario correspondiente
        var currentStock = parseInt($('#modalStockActual').text(), 10);

        switch(tipo) {
            case 'INGRESO':
                $('#formIngreso').removeClass('d-none');
                $('#btnConfirmarMovimiento').text('Registrar ingreso');
                $('#ingresoStockActual').text(currentStock);
                actualizarPreviewIngreso();
                break;
            case 'SALIDA':
                $('#formSalida').removeClass('d-none');
                $('#btnConfirmarMovimiento').text('Registrar salida');
                $('#salidaStockActual').text(currentStock);
                $('#salidaMaximo').text(currentStock);
                $('#salidaCantidad').attr('max', currentStock);
                $('#salidaCantidad').attr('data-stock-actual', currentStock);
                actualizarPreviewSalida();
                break;
            case 'AJUSTE':
                $('#formAjuste').removeClass('d-none');
                $('#btnConfirmarMovimiento').text('Aplicar ajuste');
                actualizarPreviewAjuste();
                break;
        }

        $('#btnConfirmarMovimiento').prop('disabled', false);
    });

    // Eventos para actualización de previews con validaciones
    $('#ingresoCantidad').on('input', function() {
        validarIngresoCantidad($(this));
        actualizarPreviewIngreso();
    });
    $('#salidaCantidad').on('input', function() {
        validarSalidaCantidad($(this));
        actualizarPreviewSalida();
    });
    $('#ajusteStockReal').on('input', function() {
        validarAjusteStockReal($(this));
        actualizarPreviewAjuste();
    });

    function actualizarPreviewIngreso() {
        var currentStock = parseInt($('#modalStockActual').text(), 10);
        var cantidad = parseInt($('#ingresoCantidad').val(), 10) || 0;
        var resultado = currentStock + cantidad;
        $('#ingresoCantidadPreview').text(cantidad);
        $('#ingresoResultado').text(resultado);
    }

    function actualizarPreviewSalida() {
        var currentStock = parseInt($('#modalStockActual').text(), 10);
        var cantidad = parseInt($('#salidaCantidad').val(), 10) || 0;
        var resultado = currentStock - cantidad;
        $('#salidaCantidadPreview').text(cantidad);
        $('#salidaResultado').text(resultado);
    }

    function actualizarPreviewAjuste() {
        var currentStock = parseInt($('#modalStockActual').text(), 10);
        var stockReal = parseInt($('#ajusteStockReal').val(), 10) || 0;
        var diferencia = stockReal - currentStock;
        $('#ajusteDiferencia').text(Math.abs(diferencia));
        $('#ajusteSigno').text(diferencia >= 0 ? '+' : '-');
        
        // Cambiar color según si aumenta o disminuye
        var $diferenciaContainer = $('#ajusteDiferencia').closest('.bg-warning');
        if (diferencia > 0) {
            $diferenciaContainer.removeClass('bg-warning').addClass('bg-success bg-opacity-10');
        } else if (diferencia < 0) {
            $diferenciaContainer.removeClass('bg-warning').addClass('bg-danger bg-opacity-10');
        } else {
            $diferenciaContainer.removeClass('bg-success bg-danger bg-opacity-10').addClass('bg-warning');
        }
    }

    // Funciones de validación para movimientos de inventario
    function validarIngresoCantidad($input) {
        var value = parseInt($input.val(), 10);
        var $error = $('#ingresoCantidad-error');
        
        if (isNaN(value) || value <= 0) {
            $input.addClass('is-invalid').removeClass('is-valid');
            $error.text('Debes ingresar al menos 1 unidad.');
            return false;
        }
        
        if (value > 999) {
            $input.addClass('is-invalid').removeClass('is-valid');
            $error.text('Máximo 999 unidades por movimiento.');
            return false;
        }
        
        $input.removeClass('is-invalid').addClass('is-valid');
        $error.text('');
        return true;
    }

    function validarSalidaCantidad($input) {
        var value = parseInt($input.val(), 10);
        var stockActual = parseInt($input.data('stock-actual'), 10) || 0;
        var $error = $('#salidaCantidad-error');
        
        if (isNaN(value) || value <= 0) {
            $input.addClass('is-invalid').removeClass('is-valid');
            $error.text('Debes retirar al menos 1 unidad.');
            return false;
        }
        
        if (value > stockActual) {
            $input.addClass('is-invalid').removeClass('is-valid');
            $error.text('Solo hay ' + stockActual + ' unidades disponibles.');
            return false;
        }
        
        $input.removeClass('is-invalid').addClass('is-valid');
        $error.text('');
        return true;
    }

    function validarAjusteStockReal($input) {
        var value = parseInt($input.val(), 10);
        var $error = $('#ajusteStockReal-error');
        
        if (isNaN(value) || value < 0) {
            $input.addClass('is-invalid').removeClass('is-valid');
            $error.text('El stock no puede ser negativo.');
            return false;
        }
        
        if (value > 9999) {
            $input.addClass('is-invalid').removeClass('is-valid');
            $error.text('El stock máximo es 9,999 unidades.');
            return false;
        }
        
        $input.removeClass('is-invalid').addClass('is-valid');
        $error.text('');
        return true;
    }

    // Función para mostrar mensajes inline
    function mostrarMensajeInline(mensaje, tipo) {
        var $mensajeDiv = $('#formMovimientoMensaje');
        $mensajeDiv.removeClass('d-none alert-success alert-danger alert-warning');
        
        if (tipo === 'success') {
            $mensajeDiv.addClass('alert alert-success');
        } else if (tipo === 'error') {
            $mensajeDiv.addClass('alert alert-danger');
        } else if (tipo === 'warning') {
            $mensajeDiv.addClass('alert alert-warning');
        }
        
        $mensajeDiv.html(mensaje);
    }

    function ocultarMensajeInline() {
        $('#formMovimientoMensaje').addClass('d-none');
    }

    // Envío del formulario de movimiento
    $('#formMovimiento').on('submit', function(e) {
        e.preventDefault();
        ocultarMensajeInline();

        var tipo = $('#movimientoTipoSeleccionado').val();
        console.log('Tipo de movimiento seleccionado:', tipo);
        console.log('Producto ID:', currentProductId);

        if (!tipo) {
            mostrarMensajeInline('Selecciona un tipo de movimiento.', 'error');
            return;
        }

        var currentStock = parseInt($('#modalStockActual').text(), 10);
        var stockMinimo = parseInt($('#inputStockMinimo').val(), 10) || null;
        var movimientoData = {
            productoId: currentProductId,
            tipoMovimiento: tipo,
            stockMinimo: stockMinimo
        };

        switch(tipo) {
            case 'INGRESO':
                var cantidadIngreso = parseInt($('#ingresoCantidad').val(), 10);
                if (!cantidadIngreso || cantidadIngreso <= 0) {
                    mostrarMensajeInline('La cantidad debe ser mayor a 0.', 'error');
                    return;
                }
                if (!Number.isInteger(cantidadIngreso)) {
                    mostrarMensajeInline('La cantidad debe ser un número entero.', 'error');
                    return;
                }
                movimientoData.cantidad = cantidadIngreso;
                movimientoData.motivo = $('#ingresoMotivo').val();
                movimientoData.referenciaDocumento = $('#ingresoReferencia').val();
                movimientoData.proveedor = $('#ingresoProveedor').val();
                movimientoData.observacion = $('#ingresoObservacion').val();
                movimientoData.stockAnterior = currentStock;
                movimientoData.stockResultante = currentStock + cantidadIngreso;
                break;

            case 'SALIDA':
                var cantidadSalida = parseInt($('#salidaCantidad').val(), 10);
                if (!cantidadSalida || cantidadSalida <= 0) {
                    mostrarMensajeInline('La cantidad debe ser mayor a 0.', 'error');
                    return;
                }
                if (!Number.isInteger(cantidadSalida)) {
                    mostrarMensajeInline('La cantidad debe ser un número entero.', 'error');
                    return;
                }
                if (cantidadSalida > currentStock) {
                    mostrarMensajeInline('No puedes retirar más unidades de las disponibles (stock actual: ' + currentStock + ').', 'error');
                    return;
                }
                movimientoData.cantidad = cantidadSalida;
                movimientoData.motivo = $('#salidaMotivo').val();
                movimientoData.observacion = $('#salidaObservacion').val();
                movimientoData.stockAnterior = currentStock;
                movimientoData.stockResultante = currentStock - cantidadSalida;
                break;

            case 'AJUSTE':
                var stockReal = parseInt($('#ajusteStockReal').val(), 10);
                if (isNaN(stockReal) || stockReal < 0) {
                    mostrarMensajeInline('Ingresa un stock real válido (mayor o igual a 0).', 'error');
                    return;
                }
                if (!Number.isInteger(stockReal)) {
                    mostrarMensajeInline('El stock debe ser un número entero.', 'error');
                    return;
                }
                var diferencia = stockReal - currentStock;
                movimientoData.cantidad = Math.abs(diferencia);
                movimientoData.motivo = $('#ajusteMotivo').val();
                movimientoData.stockAnterior = currentStock;
                movimientoData.stockResultante = stockReal;
                break;
        }

        var $btn = $('#btnConfirmarMovimiento');
        $btn.prop('disabled', true);

        console.log('Datos a enviar:', movimientoData);

        $.ajax({
            url: '/inventario/api/registrar-movimiento',
            method: 'POST',
            contentType: 'application/json',
            data: JSON.stringify(movimientoData),
            success: function(response) {
                console.log('Respuesta del servidor:', response);
                $btn.prop('disabled', false);

                if (response.success) {
                    mostrarMensajeInline('✓ Movimiento registrado. Nuevo stock: ' + response.producto.stock + ' unidades.', 'success');
                    actualizarFilaProducto(currentProductId, response.producto);

                    // Actualizar resumen de stock en el modal
                    $('#modalStockActual').text(response.producto.stock);
                    $('#modalStockDiferencia').text(response.producto.stock - response.producto.stockMinimo);

                    var cardStockActual = $('#cardStockActual');
                    cardStockActual.removeClass('bg-success bg-danger');
                    if (response.producto.stock < response.producto.stockMinimo) {
                        cardStockActual.addClass('bg-danger');
                        $('#alertaStockBajo').removeClass('d-none');
                    } else {
                        cardStockActual.addClass('bg-success');
                        $('#alertaStockBajo').addClass('d-none');
                    }

                    // Recargar movimientos sin recargar la página
                    $('#tablaMovimientos tbody').empty();
                    cargarMovimientos(currentProductId);

                    // Resetear formulario
                    resetearFormularioMovimiento();
                } else {
                    mostrarMensajeInline(response.message || 'No se pudo registrar el movimiento.', 'error');
                }
            },
            error: function(xhr, status, error) {
                console.log('Error en la petición:', xhr, status, error);
                $btn.prop('disabled', false);
                mostrarMensajeInline('Error al registrar movimiento: ' + error, 'error');
            }
        });
    });

    function actualizarFilaProducto(productId, producto) {
        var fila = $('#tablaInventario tbody').find('button[data-id="' + productId + '"]').closest('tr');
        if (fila.length === 0) return;
        fila.find('td').eq(3).html('<span class="badge ' + (producto.stock <= producto.stockMinimo ? 'text-bg-danger' : 'text-bg-success') + '">' + producto.stock + '</span>');
        fila.find('td').eq(4).text(producto.stockMinimo != null ? producto.stockMinimo : '-');
        fila.find('.btn-movimientos').data('stock', producto.stock).data('stock-minimo', producto.stockMinimo);
        actualizarResumenInventario();
    }

    // Manejar botón para actualizar stock mínimo
    $('#btnActualizarMinimo').on('click', function() {
        if (!currentProductId) {
            showNotification('No se ha seleccionado ningún producto.', 'danger');
            return;
        }

        var stockMinimo = parseInt($('#inputStockMinimo').val(), 10);
        if (isNaN(stockMinimo) || stockMinimo < 0) {
            showNotification('Ingresa un valor de stock mínimo válido.', 'danger');
            return;
        }

        var $btn = $(this);
        $btn.prop('disabled', true);

        $.ajax({
            url: '/inventario/api/actualizar-stock-minimo/' + currentProductId,
            method: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({ stockMinimo: stockMinimo }),
            success: function(response) {
                $btn.prop('disabled', false);

                if (response.success) {
                    showNotification('Stock mínimo actualizado correctamente.', 'success');
                    actualizarFilaProducto(currentProductId, response.producto);

                    // Actualizar resumen de stock en el modal
                    $('#modalStockMinimo').text(response.producto.stockMinimo);
                    $('#modalStockDiferencia').text(response.producto.stock - response.producto.stockMinimo);

                    var cardStockActual = $('#cardStockActual');
                    cardStockActual.removeClass('bg-success bg-danger');
                    if (response.producto.stock < response.producto.stockMinimo) {
                        cardStockActual.addClass('bg-danger');
                        $('#alertaStockBajo').removeClass('d-none');
                    } else {
                        cardStockActual.addClass('bg-success');
                        $('#alertaStockBajo').addClass('d-none');
                    }
                } else {
                    showNotification(response.message || 'No se pudo actualizar el stock mínimo.', 'danger');
                }
            },
            error: function(xhr, status, error) {
                $btn.prop('disabled', false);
                showNotification('Error al actualizar stock mínimo: ' + error, 'danger');
            }
        });
    });

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
