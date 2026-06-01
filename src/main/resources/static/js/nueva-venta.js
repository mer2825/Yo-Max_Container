$(document).ready(function() {
    let carrito = [];
    let productosCargados = {};

    cargarProductos();
    setupEventListeners();
    updateFormularioUI();
    renderizarCarrito();
    updateProgressSteps();
    aplicarValidacionDocumento();

    function cargarProductos() {
        $('.product-card').each(function() {
            const item = $(this);
            const producto = {
                id: item.data('product-id'),
                nombre: item.data('product-nombre'),
                precio: parseFloat(item.data('product-precio')),
                foto: item.data('product-foto'),
                stock: parseInt(item.data('product-stock'))
            };
            productosCargados[producto.id] = producto;
        });
    }

    function setupEventListeners() {
        $('#tipoComprobanteVenta').on('change', updateFormularioUI);
        $('#btnBuscarCliente').on('click', () => buscarOCrearCliente(false));
        $('#btnLimpiarCliente').on('click', () => limpiarCliente(true));
        $('#product-grid').on('click', '.btn-agregar', agregarAlCarrito);
        $('#product-grid').on('click', '.btn-increase', function() {
            const productoId = $(this).data('product-id');
            changeCantidadProducto(productoId, 1);
        });
        $('#product-grid').on('click', '.btn-decrease', function() {
            const productoId = $(this).data('product-id');
            changeCantidadProducto(productoId, -1);
        });
        $('#category-filters').on('click', '.category-pill', function() {
            setActiveCategory($(this).data('category'));
        });
        $('#carrito-items').on('change', '.cantidad-item', actualizarCantidad);
        $('#carrito-items').on('click', '.remover-item', removerDelCarrito);
        $('#btnFinalizarVenta').on('click', finalizarVenta);
        $('#filtroNombre, #filtroPrecioMin, #filtroPrecioMax').on('keyup input change', filtrarProductos);

        $('#tipoDocumento').on('change', aplicarValidacionDocumento);
        $('#numeroDocumento').on('input', function() {
            this.value = this.value.replace(/[^0-9]/g, '');
            const tipo = $('#tipoDocumento').val();
            const maxLength = (tipo === 'dni') ? 8 : 11;
            if (this.value.length > maxLength) {
                this.value = this.value.slice(0, maxLength);
            }
        });

        $('#descuentoVenta, #tipoDescuento').on('input change', renderizarCarrito);
    }

    function setActiveCategory(categoria) {
        $('.category-pill').removeClass('active btn-primary').addClass('btn-outline-secondary');
        $(`.category-pill[data-category='${categoria}']`).removeClass('btn-outline-secondary').addClass('active btn-primary');
        filtrarProductos();
    }

    function aplicarValidacionDocumento() {
        const tipo = $('#tipoDocumento').val();
        const numeroDocumentoInput = $('#numeroDocumento');
        numeroDocumentoInput.val('');
        if (tipo === 'dni') {
            numeroDocumentoInput.attr('maxlength', '8');
            numeroDocumentoInput.attr('placeholder', '8 dígitos (DNI)');
        } else {
            numeroDocumentoInput.attr('maxlength', '11');
            numeroDocumentoInput.attr('placeholder', '11 dígitos (RUC)');
        }
        updateClienteDisplay({ nombre: 'Consumidor Final', direccion: '' }, tipo);
    }

    function updateFormularioUI() {
        const tipoComprobante = $('#tipoComprobanteVenta').val();
        const seccionCliente = $('#seccionCliente');
        const tipoDocumentoSelect = $('#tipoDocumento');
        const inputComprobanteFinal = $('#tipoComprobante');

        seccionCliente.hide();
        tipoDocumentoSelect.prop('disabled', false);

        if (tipoComprobante === 'Factura') {
            inputComprobanteFinal.val('Factura');
            tipoDocumentoSelect.val('ruc').prop('disabled', true);
            seccionCliente.show();
        } else if (tipoComprobante === 'Boleta') {
            inputComprobanteFinal.val('Boleta');
            // Permitir seleccionar DNI o RUC para boleta, pero por defecto DNI
            if (tipoDocumentoSelect.val() !== 'ruc') {
                tipoDocumentoSelect.val('dni');
            }
            seccionCliente.show();
        } else {
            inputComprobanteFinal.val('Nota de Venta');
            // En Nota de Venta ocultamos la sección cliente para agilidad, 
            // pero si necesitas registrar cliente podrías mostrarlo. 
            // Para tu flujo, lo mantendré oculto.
            limpiarCliente(false);
        }
        aplicarValidacionDocumento();
        updateProgressSteps();
    }

    function limpiarCliente(resetearSeleccion = false) {
        $('#numeroDocumento').val('');
        $('#clienteId').val('');
        updateClienteDisplay({ nombre: 'Consumidor Final', direccion: '' }, $('#tipoDocumento').val());
        if (resetearSeleccion) {
            $('#tipoComprobanteVenta').val('Nota de Venta');
            updateFormularioUI();
        }
    }

    function updateClienteDisplay(cliente, tipoDocumento) {
        $('#nombreCliente').text(cliente.nombre);
        if (tipoDocumento === 'dni') {
            $('#direccionCliente').text('-');
        } else {
            $('#direccionCliente').text(cliente.direccion || '-');
        }
    }

    async function buscarOCrearCliente(calledFromFinalizarVenta = false) {
        const tipo = $('#tipoDocumento').val();
        const numero = $('#numeroDocumento').val();
        if (!numero) {
            if (!calledFromFinalizarVenta) showNotification('Ingrese un número de documento', 'error');
            return false;
        }

        const maxLength = (tipo === 'dni') ? 8 : 11;
        if (numero.length !== maxLength) {
            if (!calledFromFinalizarVenta) showNotification(`El ${tipo.toUpperCase()} debe tener ${maxLength} dígitos.`, 'error');
            return false;
        }

        try {
            const url = `/clientes/api/buscar-o-crear?tipo=${tipo}&numero=${numero}&forceCreate=false`;
            const response = await fetch(url);
            const result = await response.json();

            if (response.ok && result.success) {
                if (result.isNewClient) {
                    const clienteNuevo = result.cliente;
                    await new Promise(resolve => setTimeout(resolve, 1000));
                    const confirmCreate = await Swal.fire({
                        title: 'Cliente no registrado',
                        html: `El cliente <strong>${clienteNuevo.nombre}</strong> no se encuentra registrado.<br>¿Desea registrarlo y asignarlo a la venta?`,
                        icon: 'question',
                        showCancelButton: true,
                        confirmButtonText: 'Sí, registrar',
                        cancelButtonText: 'No, cancelar'
                    });
                    if (confirmCreate.isConfirmed) {
                        showLoading(true);
                        const forceCreateUrl = `/clientes/api/buscar-o-crear?tipo=${tipo}&numero=${numero}&forceCreate=true`;
                        const createResponse = await fetch(forceCreateUrl);
                        const createResult = await createResponse.json();
                        if (createResponse.ok && createResult.success) {
                            const clienteGuardado = createResult.cliente;
                            $('#clienteId').val(clienteGuardado.id);
                            updateClienteDisplay(clienteGuardado, tipo);
                            showNotification(createResult.message || 'Cliente registrado y asignado.', 'success');
                            return true;
                        } else {
                            showNotification(createResult.message || 'Error al registrar el nuevo cliente.', 'error');
                            limpiarCliente(false);
                            return false;
                        }
                    } else {
                        showNotification('Registro de cliente cancelado.', 'info');
                        limpiarCliente(false);
                        return false;
                    }
                } else {
                    const clienteExistente = result.cliente;
                    $('#clienteId').val(clienteExistente.id);
                    updateClienteDisplay(clienteExistente, tipo);
                    showNotification('Cliente asignado con éxito.', 'success');
                    return true;
                }
            } else {
                if (!calledFromFinalizarVenta) showNotification(result.message || 'Error al buscar el cliente.', 'error');
                limpiarCliente(false);
                return false;
            }
        } catch (error) {
            if (!calledFromFinalizarVenta) showNotification(`Error de conexión: ${error.message}`, 'error');
            limpiarCliente(false);
            return false;
        } finally {
            showLoading(false);
        }
    }

    function filtrarProductos() {
        const nombreFiltro = $('#filtroNombre').val().toLowerCase();
        const precioMin = parseFloat($('#filtroPrecioMin').val()) || 0;
        const precioMax = parseFloat($('#filtroPrecioMax').val()) || Infinity;
        const categoriaSeleccionada = $('.category-pill.active').data('category');

        $('#product-grid .product-card').each(function() {
            const card = $(this);
            const nombre = card.data('product-nombre').toLowerCase();
            const precio = parseFloat(card.data('product-precio')) || 0;
            const categoria = card.data('product-category');
            const cumpleCategoria = categoriaSeleccionada === 'Todos' || categoriaSeleccionada === categoria;
            const cumpleTexto = nombre.includes(nombreFiltro);
            const cumplePrecio = precio >= precioMin && precio <= precioMax;
            const visible = cumpleCategoria && cumpleTexto && cumplePrecio;
            card.closest('.col').toggle(visible);
        });
    }

    function agregarAlCarrito(e) {
        const productoId = $(e.currentTarget).data('product-id');
        const producto = productosCargados[productoId];
        if (!producto) return showNotification('Error: Producto no encontrado', 'error');
        if (producto.stock <= 0) return showNotification('Este producto está agotado.', 'error');

        const itemExistente = carrito.find(item => item.producto.id === productoId);
        if (itemExistente) {
            if (itemExistente.cantidad + 1 > producto.stock) {
                return showNotification(`No puedes agregar más. Stock disponible: ${producto.stock}`, 'warning');
            }
            itemExistente.cantidad++;
        } else {
            carrito.push({ producto: producto, cantidad: 1 });
        }
        renderizarCarrito();
        flashCarritoItem(productoId);
    }

    function changeCantidadProducto(productoId, delta) {
        const item = carrito.find(item => item.producto.id === productoId);
        if (!item) return;
        const producto = productosCargados[productoId];
        const nuevaCantidad = item.cantidad + delta;

        if (nuevaCantidad <= 0) {
            carrito = carrito.filter(p => p.producto.id !== productoId);
        } else if (nuevaCantidad > producto.stock) {
            showNotification(`Cantidad excede el stock disponible (${producto.stock}).`, 'warning');
        } else {
            item.cantidad = nuevaCantidad;
        }
        renderizarCarrito();
    }

    function actualizarCantidad(e) {
        const productoId = $(e.currentTarget).data('id');
        const nuevaCantidad = parseInt($(e.currentTarget).val());
        const item = carrito.find(item => item.producto.id === productoId);
        const producto = productosCargados[productoId];
        if (item && nuevaCantidad > 0) {
            if (nuevaCantidad > producto.stock) {
                $(e.currentTarget).val(item.cantidad);
                return showNotification(`Cantidad excede el stock disponible (${producto.stock}).`, 'warning');
            }
            item.cantidad = nuevaCantidad;
        } else if (item) {
            $(e.currentTarget).val(item.cantidad);
        }
        renderizarCarrito();
    }

    function removerDelCarrito(e) {
        const productoId = $(e.currentTarget).data('id');
        carrito = carrito.filter(item => item.producto.id !== productoId);
        renderizarCarrito();
    }

    function renderizarCarrito() {
        const tbody = $('#carrito-items');
        tbody.empty();
        let subtotalVenta = 0;

        if (carrito.length === 0) {
            $('#cart-empty-message').removeClass('d-none');
            $('#cart-table-container').addClass('d-none');
            $('#venta-summary').addClass('d-none');
            $('#venta-total-wrapper').addClass('d-none');
            $('#cart-empty-footer').removeClass('d-none');
        } else {
            $('#cart-empty-message').addClass('d-none');
            $('#cart-table-container').removeClass('d-none');
            $('#venta-summary').removeClass('d-none');
            $('#venta-total-wrapper').removeClass('d-none');
            $('#cart-empty-footer').addClass('d-none');
        }

        carrito.forEach(item => {
            const subtotalItem = item.producto.precio * item.cantidad;
            subtotalVenta += subtotalItem;
            tbody.append(`<tr data-id="${item.producto.id}" class="flash-added"><td>${item.producto.nombre}</td><td><input type="number" class="form-control form-control-sm cantidad-item" value="${item.cantidad}" data-id="${item.producto.id}" min="1" max="${item.producto.stock}"></td><td>S/ ${subtotalItem.toFixed(2)}</td><td><button class="btn btn-danger btn-sm remover-item" data-id="${item.producto.id}"><i class="bi bi-trash"></i></button></td></tr>`);
        });

        const tipoDescuento = $('#tipoDescuento').val();
        let valorDescuento = parseFloat($('#descuentoVenta').val()) || 0;
        let descuentoCalculado = 0;

        if (valorDescuento < 0) {
            valorDescuento = 0;
            $('#descuentoVenta').val(0);
        }

        if (tipoDescuento === 'porcentaje') {
            if (valorDescuento > 100) {
                valorDescuento = 100;
                $('#descuentoVenta').val(100);
            }
            descuentoCalculado = (subtotalVenta * valorDescuento) / 100;
        } else {
            if (valorDescuento > subtotalVenta) {
                valorDescuento = subtotalVenta;
                $('#descuentoVenta').val(subtotalVenta.toFixed(2));
            }
            descuentoCalculado = valorDescuento;
        }

        const totalVenta = subtotalVenta - descuentoCalculado;

        $('#venta-subtotal').text(`S/ ${subtotalVenta.toFixed(2)}`);
        $('#venta-total').text(`S/ ${totalVenta.toFixed(2)}`);
        $('#btnFinalizarVenta').prop('disabled', carrito.length === 0);

        updateProductCardActions();
        updateProgressSteps();

        setTimeout(() => {
            $('#carrito-items').find('.flash-added').removeClass('flash-added');
        }, 400);
    }

    function updateProductCardActions() {
        $('.product-card').each(function() {
            const card = $(this);
            const productoId = card.data('product-id');
            const item = carrito.find(i => i.producto.id === productoId);
            const agregarBtn = card.find('.btn-agregar');
            const quantityControls = card.find('.quantity-controls');
            const qtyInput = card.find('.product-qty');

            if (item) {
                agregarBtn.addClass('d-none');
                quantityControls.removeClass('d-none');
                qtyInput.val(item.cantidad);
            } else {
                agregarBtn.removeClass('d-none');
                quantityControls.addClass('d-none');
                qtyInput.val(1);
            }
        });
    }

    function flashCarritoItem(productoId) {
        const itemRow = $(`#carrito-items tr[data-id='${productoId}']`);
        if (itemRow.length) {
            itemRow.addClass('flash-added');
            setTimeout(() => itemRow.removeClass('flash-added'), 400);
        }
    }

    function updateProgressSteps() {
        const step1 = $('.step-item[data-step="1"]');
        const step2 = $('.step-item[data-step="2"]');
        const step3 = $('.step-item[data-step="3"]');

        if (carrito.length === 0) {
            step1.addClass('step-active').removeClass('step-completed step-pending');
            step2.addClass('step-pending').removeClass('step-active step-completed');
            step3.addClass('step-pending').removeClass('step-active step-completed');
        } else {
            step1.addClass('step-completed').removeClass('step-active step-pending');
            step2.addClass('step-completed').removeClass('step-active step-pending');
            step3.addClass('step-active').removeClass('step-completed step-pending');
        }
    }

    function getDescuentoFinalEnMonto() {
        const subtotalVenta = carrito.reduce((sum, item) => sum + (item.producto.precio * item.cantidad), 0);
        const tipoDescuento = $('#tipoDescuento').val();
        let valorDescuento = parseFloat($('#descuentoVenta').val()) || 0;
        if (valorDescuento < 0) valorDescuento = 0;

        if (tipoDescuento === 'porcentaje') {
            if (valorDescuento > 100) valorDescuento = 100;
            return (subtotalVenta * valorDescuento) / 100;
        } else {
            if (valorDescuento > subtotalVenta) valorDescuento = subtotalVenta;
            return valorDescuento;
        }
    }

    async function finalizarVenta() {
        if (carrito.length === 0) return showNotification('Agregue al menos un producto.', 'error');

        const tipoComprobanteVenta = $('#tipoComprobanteVenta').val();
        let clienteId = $('#clienteId').val();
        const numeroDocumento = $('#numeroDocumento').val();

        if (tipoComprobanteVenta === 'Factura' && !numeroDocumento) {
            return showNotification('Para una Factura, debe ingresar un RUC.', 'error');
        }

        if (numeroDocumento && !clienteId) {
            const clientProcessed = await buscarOCrearCliente(true);
            if (!clientProcessed) {
                showNotification('No se pudo asignar un cliente. Venta cancelada.', 'error');
                return;
            }
            clienteId = $('#clienteId').val();
        }

        if (!clienteId) {
            if (tipoComprobanteVenta === 'Boleta') {
                const confirmConsumidorFinal = await Swal.fire({
                    title: 'Cliente no especificado',
                    text: 'No se ha ingresado un documento. ¿Desea continuar con "Consumidor Final"?',
                    icon: 'question',
                    showCancelButton: true,
                    confirmButtonText: 'Sí, continuar',
                    cancelButtonText: 'No, cancelar'
                });
                if (confirmConsumidorFinal.isConfirmed) {
                    clienteId = 1;
                } else {
                    return showNotification('Venta cancelada.', 'info');
                }
            } else {
                clienteId = 1;
            }
        }

        const ventaData = {
            tipoComprobante: $('#tipoComprobante').val(),
            cliente: { id: clienteId },
            metodoPago: $('#metodoPago').val(),
            nota: $('#notaVenta').val(),
            descuento: getDescuentoFinalEnMonto(),
            total: parseFloat($('#venta-total').text().replace('S/ ', '')),
            detalles: carrito.map(item => ({
                producto: { id: item.producto.id },
                cantidad: item.cantidad,
                precioUnitario: item.producto.precio
            }))
        };

        mostrarConfirmacionVenta(ventaData);
    }

    async function mostrarConfirmacionVenta(ventaData) {
        const detallesHtml = ventaData.detalles.map(detalle => {
            const producto = productosCargados[detalle.producto.id];
            return `<li>${detalle.cantidad} x ${producto ? producto.nombre : 'Producto'} - S/ ${(detalle.precioUnitario * detalle.cantidad).toFixed(2)}</li>`;
        }).join('');

        const html = `<div style="text-align:left;">
                <p><strong>Total:</strong> S/ ${ventaData.total.toFixed(2)}</p>
                <p><strong>Método de pago:</strong> ${ventaData.metodoPago}</p>
                <p><strong>Productos:</strong></p>
                <ul style="padding-left: 1rem; margin: 0;">${detallesHtml}</ul>
            </div>`;

        const result = await Swal.fire({
            title: 'Confirmar venta',
            html: html,
            icon: 'question',
            showCancelButton: true,
            confirmButtonText: 'Confirmar venta',
            cancelButtonText: 'Seguir editando',
            width: '600px'
        });

        if (result.isConfirmed) {
            guardarVenta(ventaData);
        }
    }

    async function guardarVenta(ventaData) {
        showLoading(true);
        try {
            const response = await fetch('/ventas/api/guardar', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(ventaData)
            });
            const result = await response.json();
            if (result.success) {
                Swal.fire('¡Venta Registrada!', result.message, 'success').then(() => {
                    // Volver al dashboard para ver métricas actualizadas inmediatamente
                    window.location.href = '/';
                });
            } else {
                showNotification(result.message || 'Error al guardar la venta.', 'error');
            }
        } catch (error) {
            showNotification('Error de conexión al guardar la venta.', 'error');
        } finally {
            showLoading(false);
        }
    }

    function showNotification(message, type = 'success') {
        const toastContainer = $('#notification-container');
        if (!toastContainer.length) return;
        const toastClass = type === 'success' ? 'text-bg-success' : (type === 'warning' ? 'text-bg-warning' : 'text-bg-danger');
        const toastHTML = `<div class="toast align-items-center ${toastClass} border-0" role="alert" aria-live="assertive" aria-atomic="true"><div class="d-flex"><div class="toast-body">${message}</div><button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button></div></div>`;
        toastContainer.append(toastHTML);
        const toast = new bootstrap.Toast(toastContainer.children().last(), { delay: 3000 });
        toast.show();
    }

    function showLoading(show) {
        let loadingOverlay = $('#loading-overlay');
        if (loadingOverlay.length === 0) {
            $('body').append('<div id="loading-overlay" class="loading-overlay"><div class="spinner"></div></div>');
            loadingOverlay = $('#loading-overlay');
        }
        loadingOverlay.toggleClass('loading-overlay-visible', show);
    }
});
