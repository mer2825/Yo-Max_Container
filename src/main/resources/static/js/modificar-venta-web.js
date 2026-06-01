$(document).ready(function() {
    let carrito = [];
    let productosCargados = {};
    let ventaId = null;

    // --- Inicialización ---
    cargarProductos();
    setupEventListeners();
    aplicarValidacionDocumento();

    const pathParts = window.location.pathname.split('/');
    ventaId = pathParts[pathParts.length - 1];

    if (ventaId && !isNaN(ventaId)) {
        cargarDatosVenta(ventaId);
    } else {
        showNotification('ID de venta web no válido en la URL.', 'error');
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
        $('#carrito-items').on('change', '.cantidad-item', actualizarCantidad);
        $('#carrito-items').on('click', '.remover-item', removerDelCarrito);
        $('#btnFinalizarVenta').on('click', modificarVenta);
        $('#filtroNombre, #filtroPrecioMin, #filtroPrecioMax').on('keyup input change', filtrarProductos);
        $('.category-header').on('click', function() {
            const target = $(this).data('bs-target');
            $(target).collapse('toggle');
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
    }

    async function cargarDatosVenta(id) {
        try {
            const response = await fetch(`/ventas_web/api/detalle/${id}`);
            if (!response.ok) throw new Error(`Error al cargar la venta: ${response.status}`);
            const result = await response.json();

            if (result.success) {
                const venta = result.data;

                if (venta.cliente && venta.cliente.id) {
                     $('#clienteId').val(venta.cliente.id);
                     const tipoDoc = venta.cliente.tipoDocumento ? venta.cliente.tipoDocumento.toLowerCase() : 'dni';
                     $('#tipoDocumento').val(tipoDoc);
                     $('#numeroDocumento').val(venta.cliente.numeroDocumento);
                     updateClienteDisplay(venta.cliente, tipoDoc);
                } else if (venta.numeroDocumentoCliente) {
                    $('#numeroDocumento').val(venta.numeroDocumentoCliente);
                    $('#nombreCliente').text(venta.nombreCliente || 'Consumidor Final');
                } else {
                    updateClienteDisplay({ nombre: 'Consumidor Final', direccion: '' }, 'dni');
                }

                const tipoComprobanteOriginal = venta.tipoComprobante ? venta.tipoComprobante.toLowerCase() : 'nota_venta';
                if (tipoComprobanteOriginal.includes('factura')) {
                    $('#tipoComprobanteVenta').val('factura');
                } else if (tipoComprobanteOriginal.includes('boleta')) {
                    $('#tipoComprobanteVenta').val('boleta');
                } else {
                    $('#tipoComprobanteVenta').val('nota_venta');
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

                $('#metodoPago').val(venta.metodoPago || 'Efectivo');
                $('#notaVenta').val(venta.nota);

            } else {
                showNotification(result.message || 'Error al obtener detalles de la venta.', 'error');
            }
        } catch (error) {
            showNotification('Error de conexión al cargar la venta.', 'error');
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

    function updateFormularioUI() {
        const tipoComprobante = $('#tipoComprobanteVenta').val();
        const seccionCliente = $('#seccionCliente');
        const tipoDocumentoSelect = $('#tipoDocumento');
        const inputComprobanteFinal = $('#tipoComprobante');

        seccionCliente.hide();
        tipoDocumentoSelect.prop('disabled', false);

        if (tipoComprobante === 'factura') {
            inputComprobanteFinal.val('Factura');
            tipoDocumentoSelect.val('ruc').prop('disabled', true);
            seccionCliente.show();
        } else if (tipoComprobante === 'boleta') {
            inputComprobanteFinal.val('Boleta');
            tipoDocumentoSelect.val('dni').prop('disabled', false);
            seccionCliente.show();
        } else {
            inputComprobanteFinal.val('Nota de Venta');
            limpiarCliente(false);
        }

        aplicarValidacionDocumento();
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
        const tipoDocumento = $('#tipoDocumento').val();
        const numeroDocumento = $('#numeroDocumento').val().trim();

        const longitudCorrecta = (tipoDocumento === 'dni' && numeroDocumento.length === 8) || (tipoDocumento === 'ruc' && numeroDocumento.length === 11);
        if (!longitudCorrecta) {
            if (!calledFromFinalizarVenta) {
                showNotification(`El ${tipoDocumento.toUpperCase()} debe tener ${tipoDocumento === 'dni' ? 8 : 11} dígitos.`, 'error');
            }
            return false;
        }

        try {
            const response = await fetch(`/clientes/api/buscar-o-crear?tipo=${tipoDocumento}&numero=${numeroDocumento}`);
            const result = await response.json();

            if (result.success) {
                const cliente = result.cliente;
                if (result.isNewClient) {
                    const { value: confirmed } = await Swal.fire({
                        title: 'Cliente no registrado',
                        html: `
                            <p>El cliente con ${tipoDocumento.toUpperCase()} <strong>${numeroDocumento}</strong> no está en tu base de datos.</p>
                            <p><strong>Nombre:</strong> ${cliente.nombre}</p>
                            ${cliente.direccion ? `<p><strong>Dirección:</strong> ${cliente.direccion}</p>` : ''}
                            <p>¿Deseas registrarlo y asignarlo a esta venta?</p>`,
                        icon: 'question',
                        showCancelButton: true,
                        confirmButtonText: 'Sí, registrar y asignar',
                        cancelButtonText: 'Cancelar'
                    });

                    if (confirmed) {
                        const createResponse = await fetch(`/clientes/api/crear-cliente-desde-venta`, {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify(cliente)
                        });
                        const createResult = await createResponse.json();
                        if (createResult.success) {
                            $('#clienteId').val(createResult.cliente.id);
                            updateClienteDisplay(createResult.cliente, tipoDocumento);
                            showNotification('Nuevo cliente creado y asignado.', 'success');
                            return true;
                        } else {
                            showNotification(createResult.message || 'Error al crear el cliente.', 'error');
                            return false;
                        }
                    } else {
                        return false;
                    }
                } else {
                    $('#clienteId').val(cliente.id);
                    updateClienteDisplay(cliente, tipoDocumento);
                    showNotification('Cliente encontrado y asignado.', 'success');
                    return true;
                }
            } else {
                 showNotification(result.message || 'No se encontró el cliente.', 'error');
                 return false;
            }
        } catch (error) {
            showNotification('Error de conexión al buscar cliente.', 'error');
            return false;
        }
    }

    function filtrarProductos() {
        const nombre = $('#filtroNombre').val().toLowerCase();
        const min = parseFloat($('#filtroPrecioMin').val()) || 0;
        const max = parseFloat($('#filtroPrecioMax').val()) || Infinity;

        $('.product-list-item').each(function() {
            const item = $(this);
            const productName = item.data('product-nombre').toLowerCase();
            const productPrice = item.data('product-precio');

            const matchName = productName.includes(nombre);
            const matchPrice = productPrice >= min && productPrice <= max;

            if (matchName && matchPrice) {
                item.show();
            } else {
                item.hide();
            }
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

    function actualizarCantidad(e) {
        const productoId = $(e.currentTarget).data('id');
        const nuevaCantidad = parseInt($(e.currentTarget).val());
        const item = carrito.find(item => item.producto.id === productoId);
        if (!item) return;

        const producto = productosCargados[productoId];
        if (nuevaCantidad > 0) {
            if (nuevaCantidad > producto.stock) {
                $(e.currentTarget).val(item.cantidad);
                return showNotification(`Cantidad excede el stock disponible (${producto.stock}).`, 'warning');
            }
            item.cantidad = nuevaCantidad;
        } else {
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
        carrito.forEach(item => {
            const subtotalItem = item.producto.precio * item.cantidad;
            subtotalVenta += subtotalItem;
            const stock = productosCargados[item.producto.id]?.stock || item.cantidad;
            tbody.append(`<tr><td>${item.producto.nombre}</td><td><input type="number" class="form-control form-control-sm cantidad-item" value="${item.cantidad}" data-id="${item.producto.id}" min="1" max="${stock}"></td><td>S/ ${subtotalItem.toFixed(2)}</td><td><button class="btn btn-danger btn-sm remover-item" data-id="${item.producto.id}"><i class="bi bi-x-circle"></i></button></td></tr>`);
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

        if (!clienteId) {
            clienteId = 1;
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

        try {
            const response = await fetch(`/ventas_web/api/actualizar/${ventaId}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(ventaData)
            });
            const result = await response.json();
            if (result.success) {
                Swal.fire('¡Pedido Modificado!', result.message, 'success').then(() => {
                    window.parent.postMessage('ventaActualizada', '*');
                });
            } else {
                showNotification(result.message || 'Error al actualizar el pedido.', 'error');
            }
        } catch (error) {
            showNotification('Error de conexión al actualizar el pedido.', 'error');
        }
    }

    function showNotification(message, type = 'success') {
        const container = $('#notification-container');
        const toast = $(`
            <div class="toast align-items-center text-bg-${type === 'success' ? 'success' : 'danger'} border-0" role="alert" aria-live="assertive" aria-atomic="true">
                <div class="d-flex">
                    <div class="toast-body">${message}</div>
                    <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
                </div>
            </div>
        `);
        container.append(toast);
        const bsToast = new bootstrap.Toast(toast[0]);
        bsToast.show();
        toast.on('hidden.bs.toast', function () { $(this).remove(); });
    }
});