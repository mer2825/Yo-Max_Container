$(document).ready(function() {
    let carrito = [];
    let productosCargados = {};
    let ventaId = null;

    cargarProductos();
    setupEventListeners();
    aplicarValidacionDocumento();

    const pathParts = window.location.pathname.split('/');
    ventaId = pathParts[pathParts.length - 1];

    if (ventaId && !isNaN(ventaId)) {
        cargarDatosVenta(ventaId);
    } else {
        showNotification('ID de venta no válido en la URL.', 'error');
        setTimeout(() => { window.parent.postMessage('cerrarModal', '*'); }, 2000);
    }

    function cargarProductos() {
        $('.product-list-item').each(function() {
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
        $('#product-selection-area').on('click', '.product-list-item', agregarAlCarrito);
        $('#filtroNombre, #filtroPrecioMin, #filtroPrecioMax').on('keyup input change', filtrarProductos);
        $('.category-header').on('click', function() {
            const target = $(this).data('bs-target');
            $(target).collapse('toggle');
        });
        $('#btnVolver').on('click', function () {
            window.parent.postMessage('cerrarModal', '*');
        });
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
        $('#btnFinalizarVenta').on('click', modificarVenta);

        $(document).on('keydown', '.cantidad-item', function(e) {
            const teclasBloqueadas = ['-', '.', ',', 'e', '+'];
            if (teclasBloqueadas.includes(e.key)) {
                e.preventDefault();
                return false;
            }
            if (e.key === 'ArrowDown' && parseInt($(this).val()) <= 1) {
                e.preventDefault();
                return false;
            }
        });

        $(document).on('input', '.cantidad-item', function() {
            this.value = this.value.replace(/[^0-9]/g, '');
            if (this.value === '' || this.value === '0') return;
            actualizarCantidadDesdeInput(this);
        });

        $(document).on('blur', '.cantidad-item', function() {
            if (!this.value || parseInt(this.value, 10) < 1) {
                this.value = '1';
                actualizarCantidadDesdeInput(this);
            }
        });

        $(document).on('click', '.remover-item', removerDelCarrito);
    }

    function actualizarCantidadDesdeInput(input) {
        const productoId = $(input).data('id');
        let nuevaCantidad = parseInt(input.value, 10);

        if (isNaN(nuevaCantidad) || nuevaCantidad < 1) return;

        const item = carrito.find(item => item.producto.id === productoId);
        if (!item) return;

        const producto = productosCargados[productoId];
        if (nuevaCantidad > producto.stock) {
            input.value = item.cantidad;
            return showNotification(`Cantidad excede el stock disponible (${producto.stock}).`, 'warning');
        }

        item.cantidad = nuevaCantidad;
        const subtotalItem = item.producto.precio * nuevaCantidad;
        $(input).closest('tr').find('.subtotal-item').text(`S/ ${subtotalItem.toFixed(2)}`);
        recalcularTotales();
    }

    async function cargarDatosVenta(id) {
        showLoading(true);
        try {
            const response = await fetch(`/ventas/api/detalle/${id}`);
            if (!response.ok) throw new Error(`Error al cargar la venta: ${response.status}`);
            const result = await response.json();

            if (result.success) {
                const venta = result.data;
                const tipoComprobanteOriginal = venta.tipoComprobante.toLowerCase();
                if (tipoComprobanteOriginal.includes('factura')) {
                    $('#tipoComprobanteVenta').val('factura');
                } else if (tipoComprobanteOriginal.includes('boleta')) {
                    $('#tipoComprobanteVenta').val('boleta');
                } else {
                    $('#tipoComprobanteVenta').val('nota_venta');
                }

                if (venta.cliente && venta.cliente.id !== 1) {
                    $('#clienteId').val(venta.cliente.id);
                    const tipoDoc = venta.cliente.tipoDocumento ? venta.cliente.tipoDocumento.toLowerCase() : 'dni';
                    $('#tipoDocumento').val(tipoDoc);
                    updateClienteDisplay(venta.cliente, tipoDoc);
                } else {
                    updateClienteDisplay({ nombre: 'Consumidor Final', direccion: '' }, 'dni');
                }

                updateFormularioUI();

                carrito = venta.detalles.map(detalle => {
                    const productoCompleto = productosCargados[detalle.producto.id];
                    return {
                        producto: { ...productoCompleto, precio: detalle.precioUnitario },
                        cantidad: detalle.cantidad
                    };
                });

                if (venta.descuento && venta.descuento > 0) {
                    $('#tipoDescuento').val('monto');
                    $('#descuentoVenta').val(venta.descuento.toFixed(2));
                }

                renderizarCarrito();
                $('#metodoPago').val(venta.metodoPago);
                $('#notaVenta').val(venta.nota);

            } else {
                showNotification(result.message || 'Error al obtener detalles de la venta.', 'error');
            }
        } catch (error) {
            showNotification('Error de conexión al cargar la venta.', 'error');
        } finally {
            showLoading(false);
        }
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
    }

    function mapTipoComprobante(value) {
        if (value === 'factura') return 'Factura';
        if (value === 'boleta') return 'Boleta';
        return 'Nota de Venta';
    }

    function updateFormularioUI() {
        const tipoComprobante = $('#tipoComprobanteVenta').val();
        const seccionCliente = $('#seccionCliente');
        const tipoDocumentoSelect = $('#tipoDocumento');
        const inputComprobanteFinal = $('#tipoComprobante');

        seccionCliente.hide();
        tipoDocumentoSelect.prop('disabled', false);

        const comprobanteFinal = mapTipoComprobante(tipoComprobante);
        inputComprobanteFinal.val(comprobanteFinal);

        if (tipoComprobante === 'factura') {
            tipoDocumentoSelect.val('ruc').prop('disabled', true);
            seccionCliente.show();
        } else if (tipoComprobante === 'boleta') {
            tipoDocumentoSelect.val('dni').prop('disabled', false); // Allow changing for boleta
            seccionCliente.show();
        } else {
            limpiarCliente(false);
        }

        if (!$('#loading-overlay').hasClass('loading-overlay-visible')) {
            aplicarValidacionDocumento();
        }
    }

    function limpiarCliente(resetearSeleccion = false) {
        $('#numeroDocumento').val('');
        $('#clienteId').val('');
        updateClienteDisplay({ nombre: 'Consumidor Final', direccion: '' }, $('#tipoDocumento').val());
        if (resetearSeleccion) {
            $('#tipoComprobanteVenta').val('nota_venta');
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
            const response = await fetch(url, { credentials: 'include' });
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
                        const createResponse = await fetch(forceCreateUrl, { credentials: 'include' });
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
        const filtroNombre = $('#filtroNombre').val().toLowerCase();
        const filtroPrecioMin = Math.max(0, parseFloat($('#filtroPrecioMin').val()) || 0);
        const filtroPrecioMax = Math.max(0, parseFloat($('#filtroPrecioMax').val()) || Infinity);

        $('.product-list-item').each(function() {
            const item = $(this);
            const nombre = item.data('product-nombre').toLowerCase();
            const precio = parseFloat(item.data('product-precio'));
            const cumpleNombre = nombre.includes(filtroNombre);
            const cumplePrecioMin = precio >= filtroPrecioMin;
            const cumplePrecioMax = precio <= filtroPrecioMax;
            item.toggle(cumpleNombre && cumplePrecioMin && cumplePrecioMax);
        });

        $('.category-group').each(function() {
            const group = $(this);
            group.toggle(group.find('.product-list-item:visible').length > 0);
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
    }

    function removerDelCarrito(e) {
        const productoId = $(e.currentTarget).data('id');
        carrito = carrito.filter(item => item.producto.id !== productoId);
        renderizarCarrito();
    }

    function renderizarCarrito() {
        const tbody = $('#carrito-items');
        tbody.empty();

        carrito.forEach(item => {
            const cantidad = Math.max(1, parseInt(item.cantidad, 10) || 1);
            item.cantidad = cantidad;
            const subtotalItem = item.producto.precio * cantidad;
            const stock = productosCargados[item.producto.id]?.stock || cantidad;

            tbody.append(`
                <tr>
                    <td>${item.producto.nombre}</td>
                    <td>
                        <input
                            type="text"
                            inputmode="numeric"
                            class="form-control form-control-sm cantidad-item"
                            value="${cantidad}"
                            data-id="${item.producto.id}"
                            data-stock="${stock}"
                            autocomplete="off"
                        >
                    </td>
                    <td class="subtotal-item">S/ ${subtotalItem.toFixed(2)}</td>
                    <td>
                        <button class="btn btn-danger btn-sm remover-item" data-id="${item.producto.id}">
                            <i class="bi bi-x-circle"></i>
                        </button>
                    </td>
                </tr>
            `);
        });

        recalcularTotales();
    }

    function recalcularTotales() {
        let subtotalVenta = carrito.reduce((sum, item) => {
            const cantidad = Math.max(1, parseInt(item.cantidad, 10) || 1);
            return sum + (item.producto.precio * cantidad);
        }, 0);

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
        $('#venta-descuento').text(`S/ ${descuentoCalculado.toFixed(2)}`);
        $('#venta-total').text(`S/ ${totalVenta.toFixed(2)}`);
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

    async function modificarVenta() {
        if (carrito.length === 0) return showNotification('Agregue al menos un producto.', 'error');

        const tipoComprobanteVenta = $('#tipoComprobanteVenta').val();
        let clienteId = $('#clienteId').val();
        const numeroDocumento = $('#numeroDocumento').val();

        if (tipoComprobanteVenta === 'factura' && !numeroDocumento) {
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

        if (!clienteId) clienteId = 1;

        const ventaData = {
            tipoComprobante: mapTipoComprobante($('#tipoComprobanteVenta').val()),
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

        showLoading(true);
        try {
            const response = await fetch(`/ventas/api/actualizar/${ventaId}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(ventaData)
            });
            const result = await response.json();
            if (result.success) {
                Swal.fire('¡Venta Modificada!', result.message, 'success').then(() => {
                    window.parent.postMessage('ventaActualizada', '*');
                });
            } else {
                showNotification(result.message || 'Error al actualizar la venta.', 'error');
            }
        } catch (error) {
            showNotification('Error de conexión al actualizar la venta.', 'error');
        } finally {
            showLoading(false);
        }
    }

    function showNotification(message, type = 'success') {
        const toastContainer = $('#notification-container');
        if (!toastContainer.length) {
            // If the container is not in the current DOM (because it's an iframe),
            // try to find it in the parent.
            const parentToastContainer = $(window.parent.document).find('#notification-container');
            if (parentToastContainer.length) {
                // Use parent's notification system
                window.parent.showNotification(message, type);
                return;
            }
        }
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