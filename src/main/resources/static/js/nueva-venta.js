$(document).ready(function() {
    let carrito = [];
    let productosCargados = {};
    let uiInicializada = false;
    let listenersInstalados = false;

    function initVentaUI() {
        cargarProductos();

        // Evitar duplicar listeners al re-inicializar desde el modal de apertura
        if (!listenersInstalados) {
            setupEventListeners();
            listenersInstalados = true;
        }

        // Re-hidratar estado de UI (esto sí debe ejecutarse siempre)
        updateFormularioUI();
        renderizarCarrito();
        updateProgressSteps();
        aplicarValidacionDocumento();

        // Asegurar que el botón finalize tenga estado correcto según carrito
        $('#btnFinalizarVenta').prop('disabled', carrito.length === 0);
    }

    // Primero verificar que haya sesión de caja abierta
    verificarSesionCaja().then(function() {        initVentaUI();
    });

    function verificarSesionCaja() {
        return fetch('/ventas/api/verificar-caja')
            .then(function(response) { return response.json(); })
            .then(function(data) {
                if (!data.haySesionActiva) {
                    // Mantener el modal "anterior" (SweetAlert bloqueante),
                    // pero SIEMPRE evitar competencia con el modal/banner de caja rápida.
                    var showSweetAlertModal = true;

                    // Si el banner/modal de caja rápida está en la página, NO mostramos SweetAlert.
                    if ($('#banner-caja-cerrada').length || $('#modal-caja-rapida').length) {
                        showSweetAlertModal = false;

                        // Si está el modal, lo abrimos.
                        try {
                            var saldoSugerido = data.saldoSugerido || 0;
                            var $fondo = $('#fondo-rapido');
                            if ($fondo.length) $fondo.val(saldoSugerido);

                            var $modal = $('#modal-caja-rapida');
                            if ($modal.length) $modal.css('display', 'flex');
                        } catch (e) {
                            console.warn('No se pudo abrir modal de caja rápida:', e);
                        }

                        // Blindaje: si ya estamos en el flujo de caja rápida,
                        // salir para NO continuar con SweetAlert.
                        return Promise.resolve();
                    }

                    if (!showSweetAlertModal) return;

                    // SweetAlert "anterior": Apertura de Caja Requerida
                    var saldoSugeridoFallback = data.saldoSugerido || 0;
                    var html = '<div style="text-align:left;">' +
                        '<div class="alert alert-warning">' +
                        '<i class="bi bi-exclamation-triangle"></i> ' +
                        '<strong>No hay sesión de caja abierta.</strong><br>' +
                        'Debe abrir la caja antes de poder realizar ventas.' +
                        '</div>' +
                        '<div class="mb-3">' +
                        '<label class="form-label fw-bold">Monto inicial (saldo sugerido: S/ ' + saldoSugeridoFallback.toFixed(2) + ')</label>' +
                        '<input type="number" id="montoAperturaRapida" class="form-control" value="' + saldoSugeridoFallback.toFixed(2) + '" min="0" step="0.10">' +
                        '</div>' +
                        '</div>';

                    return Swal.fire({
                        title: 'Apertura de Caja Requerida',
                        html: html,
                        icon: 'warning',
                        showCancelButton: true,
                        confirmButtonText: 'Abrir Caja',
                        cancelButtonText: 'Cancelar',
                        allowOutsideClick: false,
                        allowEscapeKey: false,
                        width: '500px',
                        preConfirm: function() {
                            var monto = parseFloat(document.getElementById('montoAperturaRapida').value);
                            if (isNaN(monto) || monto < 0) {
                                Swal.showValidationMessage('Ingrese un monto válido');
                                return false;
                            }
                            return fetch('/ventas/api/abrir-caja-rapida?montoInicial=' + monto, { method: 'POST' })
                                .then(function(r) { return r.json(); })
                                .then(function(result) {
                                    if (!result.success) throw new Error(result.message);
                                    return result;
                                });
                        }
                    }).then(function(result) {
                        if (result && result.isConfirmed) {
                            Swal.fire('Caja Abierta', result.value.message, 'success');
                            initVentaUI();
                        } else {
                            window.location.href = '/caja';
                        }
                    });
                }
            })
            .catch(function(error) {
                console.error('Error verificando caja:', error);
            });
    }

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
        $('#tipoComprobanteSecundario').on('change', updateFormularioUI);
        $('.comprobante-card').on('click', function() {
            seleccionarTipoComprobante($(this).data('tipo'));
        });
        $('#btnBuscarCliente').on('click', async () => {
            const found = await buscarDocumentoExternamente();
            if (!found) await buscarOCrearCliente(false);
        });
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
        $('#product-grid').on('input', '.product-qty', function() {
            const productoId = $(this).closest('.product-card').data('product-id');
            validarCantidadManual(productoId, $(this));
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
        const labelNumeroDocumento = $('#labelNumeroDocumento');
        const documentoAyuda = $('#documentoAyuda');
        numeroDocumentoInput.val('');
        if (tipo === 'dni') {
            numeroDocumentoInput.attr('maxlength', '8');
            numeroDocumentoInput.attr('placeholder', '8 dígitos (DNI)');
            labelNumeroDocumento.text('DNI del cliente');
            documentoAyuda.text('DNI opcional para ventas menores a S/ 700 (consumidor final).');
        } else {
            numeroDocumentoInput.attr('maxlength', '11');
            numeroDocumentoInput.attr('placeholder', '11 dígitos (RUC)');
            labelNumeroDocumento.text('RUC del cliente');
            documentoAyuda.text('RUC obligatorio para Factura.');
        }
        updateClienteDisplay({ nombre: 'Consumidor Final', direccion: '' }, tipo);
    }

    function updateFormularioUI() {
        let tipoComprobante = $('#tipoComprobanteSecundario').val() || $('#tipoComprobanteVenta').val();
        const seccionCliente = $('#seccionCliente');
        const tipoDocumentoInput = $('#tipoDocumento');
        const inputComprobanteFinal = $('#tipoComprobante');

        if (tipoComprobante === 'nota_venta') {
            inputComprobanteFinal.val('Nota de Venta');
            tipoDocumentoInput.val('dni');
            seccionCliente.hide();
            setActiveComprobanteCard('');
        } else {
            seccionCliente.show();
            if (tipoComprobante === 'factura') {
                inputComprobanteFinal.val('Factura');
                tipoDocumentoInput.val('ruc');
                setActiveComprobanteCard('factura');
            } else {
                inputComprobanteFinal.val('Boleta de Venta');
                tipoDocumentoInput.val('dni');
                setActiveComprobanteCard('boleta');
            }
            tipoDocumentoInput.prop('disabled', true);
        }

        aplicarValidacionDocumento();
        updateFinalizarButton();
        updateProgressSteps();
    }

    function limpiarCliente(resetearSeleccion = false) {
        $('#numeroDocumento').val('');
        $('#clienteId').val('');
        $('#clienteNombreInput').val('');
        $('#clienteDireccionInput').val('');
        updateClienteDisplay({ nombre: 'Consumidor Final', direccion: '' }, $('#tipoDocumento').val());
        if (resetearSeleccion) {
            $('#tipoComprobanteVenta').val('nota_venta');
            $('#tipoComprobanteSecundario').val('nota_venta');
            updateFormularioUI();
        }
    }

    function updateClienteDisplay(cliente, tipoDocumento) {
        const nombre = cliente.nombre || 'Consumidor Final';
        $('#nombreCliente').text(nombre);
        $('#clienteNombreInput').val(cliente.nombre || '');
        $('#clienteDireccionInput').val(cliente.direccion || '');
        if (tipoDocumento === 'dni') {
            $('#documentoAyuda').text('DNI opcional para ventas menores a S/ 700 (consumidor final).');
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

    function seleccionarTipoComprobante(tipo) {
        $('#tipoComprobanteVenta').val(tipo);
        $('#tipoComprobanteSecundario').val('');
        updateFormularioUI();
    }

    function setActiveComprobanteCard(tipo) {
        $('.comprobante-card').removeClass('border-primary bg-primary text-white').addClass('btn-outline-secondary text-dark');
        if (tipo) {
            $(`.comprobante-card[data-tipo='${tipo}']`).removeClass('btn-outline-secondary text-dark').addClass('border-primary bg-primary text-white');
        }
    }

    async function buscarDocumentoExternamente() {
        const tipo = $('#tipoDocumento').val();
        const numero = $('#numeroDocumento').val();
        if (!numero) {
            showNotification('Ingrese un número de documento para buscar.', 'error');
            return false;
        }
        const maxLength = (tipo === 'dni') ? 8 : 11;
        if (numero.length !== maxLength) {
            showNotification(`El ${tipo.toUpperCase()} debe tener ${maxLength} dígitos.`, 'error');
            return false;
        }

        try {
            const response = await fetch(`/clientes/api/consultar-dni/${numero}`, { credentials: 'include' });
            const result = await response.json();

            if (response.ok && result.success && result.data) {
                const cliente = result.data;
                $('#clienteNombreInput').val(cliente.nombre || '');
                $('#clienteDireccionInput').val(cliente.direccion || '');
                $('#clienteId').val(cliente.id || '');
                showNotification('Datos del cliente cargados desde MiAPI.', 'success');
                return true;
            }

            showNotification(result.message || 'No se encontraron datos para ese documento.', 'warning');
            return false;
        } catch (error) {
            showNotification('No se pudo traer los datos externos. Puede completar manualmente.', 'warning');
            return false;
        }
    }

    function updateFinalizarButton() {
        const tipoComprobante = $('#tipoComprobanteSecundario').val() || $('#tipoComprobanteVenta').val();
        const boton = $('#btnFinalizarVenta');
        boton.removeClass('btn-primary btn-warning btn-success btn-secondary');
        if (tipoComprobante === 'factura') {
            boton.addClass('btn-success');
            boton.text('Emitir Factura Electrónica');
        } else if (tipoComprobante === 'boleta') {
            boton.addClass('btn-warning');
            boton.text('Emitir Boleta Electrónica');
        } else {
            boton.addClass('btn-secondary');
            boton.text('Registrar Nota de Venta');
        }
    }

    function filtrarProductos() {
        const nombreFiltro = $('#filtroNombre').val().toLowerCase();
        const precioMin = Math.max(0, parseFloat($('#filtroPrecioMin').val()) || 0);
        const precioMax = Math.max(0, parseFloat($('#filtroPrecioMax').val()) || Infinity);
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

    function validarCantidadManual(productoId, $input) {
        const producto = productosCargados[productoId];
        const stockDisponible = parseInt($input.data('stock-disponible'), 10) || producto.stock;
        let valor = parseInt($input.val(), 10);

        if (isNaN(valor) || valor < 1) {
            valor = 1;
        } else if (valor > stockDisponible) {
            valor = stockDisponible;
            showNotification(`Cantidad máxima: ${stockDisponible} unidades disponibles.`, 'warning');
        }

        $input.val(valor);

        // Actualizar el carrito con la nueva cantidad
        const item = carrito.find(item => item.producto.id === productoId);
        if (item) {
            item.cantidad = valor;
            renderizarCarrito();
        }
    }

    function actualizarCantidad(e) {
        const productoId = $(e.currentTarget).data('id');
        let nuevaCantidad = parseInt($(e.currentTarget).val().replace(/[^0-9]/g, ''));
        if (isNaN(nuevaCantidad) || nuevaCantidad < 1) {
            nuevaCantidad = 1;
        }
        
        const item = carrito.find(item => item.producto.id === productoId);
        const producto = productosCargados[productoId];
        if (item) {
            if (nuevaCantidad > producto.stock) {
                $(e.currentTarget).val(item.cantidad);
                return showNotification(`Cantidad excede el stock disponible (${producto.stock}).`, 'warning');
            }
            item.cantidad = nuevaCantidad;
        }
        renderizarCarrito();
    }

    function calcularTotales(subtotalVenta, descuentoCalculado) {
        // El precio ya incluye IGV. Calculamos el IGV "hacia atrás":
        // subtotalSinIGV = subtotal / 1.18, igv = subtotalSinIGV * 0.18
        const subtotalSinIGV = subtotalVenta / 1.18;
        const igv = subtotalSinIGV * 0.18;
        // Total a pagar = subtotal - descuento (el IGV ya está incluido en el precio)
        const totalVenta = Math.max(0, subtotalVenta - descuentoCalculado);
        return { subtotalSinIGV, igv, totalVenta };
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
            tbody.append(`<tr data-id="${item.producto.id}" class="flash-added"><td>${item.producto.nombre}</td><td><input type="number" class="form-control form-control-sm cantidad-item" value="${item.cantidad}" data-id="${item.producto.id}" min="1" max="${item.producto.stock}" onkeydown="return event.keyCode !== 69 && event.keyCode !== 189 && event.keyCode !== 190" oninput="this.value = Math.abs(this.value.replace(/[^0-9]/g, '')) || 1"></td><td>S/ ${subtotalItem.toFixed(2)}</td><td><button class="btn btn-danger btn-sm remover-item" data-id="${item.producto.id}"><i class="bi bi-trash"></i></button></td></tr>`);
        });

        const tipoDescuento = $('#tipoDescuento').val();
        let valorDescuento = Math.abs(parseFloat($('#descuentoVenta').val()) || 0);
        let descuentoCalculado = 0;

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

        const { subtotalSinIGV, igv, totalVenta } = calcularTotales(subtotalVenta, descuentoCalculado);

        $('#venta-subtotal').text(`S/ ${subtotalSinIGV.toFixed(2)}`);
        $('#venta-igv').text(`S/ ${igv.toFixed(2)}`);
        $('#venta-total-resumen').text(`S/ ${totalVenta.toFixed(2)}`);
        $('#venta-total').text(`S/ ${totalVenta.toFixed(2)}`);
        $('#btnFinalizarVenta').prop('disabled', carrito.length === 0);
        updateFinalizarButton();

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
            const producto = productosCargados[productoId];
            const agregarBtn = card.find('.btn-agregar');
            const quantityControls = card.find('.quantity-controls');
            const qtyInput = card.find('.product-qty');
            const increaseBtn = card.find('.btn-increase');
            const stockDisponibleText = card.find('.stock-disponible-text');

            if (item) {
                agregarBtn.addClass('d-none');
                quantityControls.removeClass('d-none');
                stockDisponibleText.removeClass('d-none');
                qtyInput.val(item.cantidad);
                
                // Deshabilitar botón + si la cantidad llega al stock disponible
                if (item.cantidad >= producto.stock) {
                    increaseBtn.prop('disabled', true);
                } else {
                    increaseBtn.prop('disabled', false);
                }
            } else {
                agregarBtn.removeClass('d-none');
                quantityControls.addClass('d-none');
                stockDisponibleText.addClass('d-none');
                qtyInput.val(1);
                increaseBtn.prop('disabled', false);
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
        let valorDescuento = Math.abs(parseFloat($('#descuentoVenta').val()) || 0);

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

        const tipoComprobanteVenta = $('#tipoComprobanteSecundario').val() || $('#tipoComprobanteVenta').val();
        let clienteId = $('#clienteId').val();
        const numeroDocumento = $('#numeroDocumento').val();
        const totalVenta = parseFloat($('#venta-total').text().replace('S/ ', '')) || 0;

        if (tipoComprobanteVenta === 'factura') {
            if (!numeroDocumento) {
                return showNotification('Para una Factura, debe ingresar un RUC.', 'error');
            }
            if (numeroDocumento.length !== 11 || !/^(10|20)/.test(numeroDocumento)) {
                return showNotification('El RUC debe tener 11 dígitos y comenzar con 10 o 20.', 'error');
            }
        }

        if (tipoComprobanteVenta === 'boleta') {
            if (numeroDocumento && numeroDocumento.length !== 8) {
                return showNotification('El DNI debe tener 8 dígitos.', 'error');
            }
            if (totalVenta >= 700 && (!numeroDocumento || numeroDocumento.length !== 8)) {
                return showNotification('Para Boleta con S/ 700 o más, ingrese el DNI del cliente.', 'error');
            }
        }

        if (numeroDocumento && !clienteId) {
            const clientProcessed = await buscarOCrearCliente(true);
            if (!clientProcessed) {
                showNotification('No se pudo asignar un cliente. Venta cancelada.', 'error');
                return;
            }
            clienteId = $('#clienteId').val();
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

        const tipoComprobanteRaw = $('#tipoComprobante').val();
        const tipoComprobante = tipoComprobanteRaw && tipoComprobanteRaw.toLowerCase().includes('factura')
            ? 'Factura'
            : tipoComprobanteRaw && tipoComprobanteRaw.toLowerCase().includes('boleta')
                ? 'Boleta'
                : 'Nota de Venta';

        const ventaData = {
            tipoComprobante: tipoComprobante,
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
                if (result.estadoSunat && result.estadoSunat.toLowerCase() === 'aceptado' && result.pdfUrl) {
                    mostrarConfirmacionPostVenta(result);
                } else {
                    Swal.fire({
                        title: 'Venta registrada',
                        html: '<div style="text-align:left;">' +
                            '<p>El comprobante está <strong>pendiente de confirmación</strong> por SUNAT.</p>' +
                            '<p style="font-size:0.9em;color:#666;">Los comprobantes electrónicos pueden tardar entre <strong>5 y 15 minutos</strong> en ser procesados.</p>' +
                            '<p style="font-size:0.9em;color:#666;">Puede cerrar esta ventana y continuar con otras ventas. El comprobante se actualizará automáticamente.</p>' +
                            '</div>',
                        icon: 'info',
                        confirmButtonText: 'Entendido',
                        width: '500px'
                    }).then(() => {
                        window.location.href = '/ventas/nueva';
                    });
                }
            } else {
                showNotification(result.message || 'Error al guardar la venta.', 'error');
            }
        } catch (error) {
            showNotification('Error de conexión al guardar la venta.', 'error');
        } finally {
            showLoading(false);
        }
    }

    function mostrarConfirmacionPostVenta(result) {
        const estadoSunatLabel = result.estadoSunat && result.estadoSunat.toLowerCase() === 'aceptado'
            ? '<span class="text-success">Aceptado por SUNAT</span>'
            : '<span class="text-warning">' + (result.estadoSunat || 'Pendiente') + '</span>';

        Swal.fire({
            title: 'Venta finalizada',
            html: `<div style="text-align:left;">` +
                `<p><strong>Comprobante:</strong> ${result.numeroVenta || 'N/A'}</p>` +
                `<p><strong>Estado SUNAT:</strong> ${estadoSunatLabel}</p>` +
                `<p><strong>PDF:</strong> <a href="${result.pdfUrl}" target="_blank">Abrir comprobante</a></p>` +
                `</div>`,
            icon: 'success',
            showDenyButton: true,
            showCancelButton: true,
            confirmButtonText: 'Descargar PDF',
            denyButtonText: 'Nueva Venta',
            cancelButtonText: 'Cerrar',
            width: '600px'
        }).then((swalResult) => {
            if (swalResult.isConfirmed) {
                window.open(result.pdfUrl, '_blank');
            }
            if (swalResult.isDenied) {
                window.location.href = '/ventas/nueva';
            }
            if (!swalResult.isConfirmed && !swalResult.isDenied) {
                // Mantener en la misma vista para crear otra venta manualmente
            }
        });
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