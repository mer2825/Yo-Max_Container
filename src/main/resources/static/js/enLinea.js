document.addEventListener("DOMContentLoaded", () => {
    // =================================================================
    // NÚMERO DE WHATSAPP ACTUALIZADO
    // =================================================================
    const NUMERO_WHATSAPP = '51948466088';

    const usuarioLogeado = localStorage.getItem("usuarioLogeado") === "true";
    if(!usuarioLogeado){
        alert("⚠️ Debes iniciar sesión para acceder a Pedidos.");
        window.location.href = "index.html";
    }

    // Lista de postres con precios
    const postres = [
        {nombre: "Carlota de Chocolate", precio: 15},
        {nombre: "Cuchareable de Fresa", precio: 12},
        {nombre: "Cuchareable de Chocolate", precio: 13},
        {nombre: "Mousse de Fresa", precio: 14},
        {nombre: "Mousse de Maracuyá", precio: 16},
        {nombre: "Trufas", precio: 10},
        {nombre: "Waffles", precio: 18}
    ];

    const menuPostres = document.getElementById("menuPostres");
    const pedidoLista = document.getElementById("pedidoLista");
    const totalPrecio = document.getElementById("totalPrecio");
    const direccionEntrega = document.getElementById("direccionEntrega");
    const finalizarPedido = document.getElementById("finalizarPedido");

    let pedido = [];

    // Cargar menú dinámico
    postres.forEach((p, i) => {
        const div = document.createElement("div");
        div.className = "col-md-4";

        div.innerHTML = `
            <div class="card h-100">
                <div class="card-body text-center">
                    <h5 class="card-title">${p.nombre}</h5>
                    <p>Precio: S/ ${p.precio.toFixed(2)}</p>
                    <div class="d-flex justify-content-center align-items-center gap-2">
                        <button class="btn btn-sm btn-secondary" data-i="${i}" data-action="restar">-</button>
                        <span id="cantidad-${i}">0</span>
                        <button class="btn btn-sm btn-secondary" data-i="${i}" data-action="sumar">+</button>
                    </div>
                    <button class="btn btn-primary mt-2" data-i="${i}">Agregar al pedido</button>
                </div>
            </div>
        `;
        menuPostres.appendChild(div);
    });

    // Cantidades temporales
    const cantidades = new Array(postres.length).fill(0);

    menuPostres.addEventListener("click", (e) => {
        const index = e.target.dataset.i;
        const action = e.target.dataset.action;

        if(action === "sumar"){
            cantidades[index]++;
            document.getElementById(`cantidad-${index}`).textContent = cantidades[index];
        }

        if(action === "restar"){
            if(cantidades[index] > 0){
                cantidades[index]--;
                document.getElementById(`cantidad-${index}`).textContent = cantidades[index];
            }
        }

        if(e.target.tagName === "BUTTON" && e.target.textContent.includes("Agregar")){
            if(cantidades[index] > 0){
                const itemExistente = pedido.find(item => item.nombre === postres[index].nombre);
                if(itemExistente){
                    itemExistente.cantidad += cantidades[index];
                } else {
                    pedido.push({nombre: postres[index].nombre, precio: postres[index].precio, cantidad: cantidades[index]});
                }
                cantidades[index] = 0;
                document.getElementById(`cantidad-${index}`).textContent = 0;
                actualizarPedido();
            }
        }
    });

    function actualizarPedido(){
        pedidoLista.innerHTML = "";
        let total = 0;
        pedido.forEach(item => {
            const subtotal = item.precio * item.cantidad;
            total += subtotal;
            const li = document.createElement("li");
            li.className = "list-group-item d-flex justify-content-between align-items-center";
            li.textContent = `${item.nombre} x${item.cantidad}`;
            const span = document.createElement("span");
            span.textContent = `S/ ${subtotal.toFixed(2)}`;
            li.appendChild(span);
            pedidoLista.appendChild(li);
        });
        totalPrecio.textContent = total.toFixed(2);
    }

    // LÓGICA MODIFICADA PARA ENVIAR A WHATSAPP
    finalizarPedido.addEventListener("click", () => {
        if(pedido.length === 0){
            alert("Agrega al menos un postre a tu pedido.");
            return;
        }
        if(direccionEntrega.value.trim() === ""){
            alert("Ingresa una dirección de entrega.");
            return;
        }

        // 1. Construir el mensaje de pedido
        let mensajeWhatsApp = `*¡NUEVO PEDIDO MIXTURAS DULCES!* 🍰\n\n`;
        mensajeWhatsApp += `--- Detalle del Pedido ---\n`;

        // 1.1. Listar los ítems
        pedido.forEach(item => {
            const subtotal = item.precio * item.cantidad;
            mensajeWhatsApp += `${item.cantidad} x ${item.nombre} = S/ ${subtotal.toFixed(2)}\n`;
        });

        // 1.2. Agregar Total y Dirección
        mensajeWhatsApp += `\n*TOTAL A PAGAR: S/ ${totalPrecio.textContent}*\n`;
        mensajeWhatsApp += `*Dirección de Entrega:* ${direccionEntrega.value.trim()}\n\n`;
        mensajeWhatsApp += `¡Por favor, confirmar mi pedido! Gracias. 😊`;

        // 2. Codificar el mensaje para la URL
        const mensajeCodificado = encodeURIComponent(mensajeWhatsApp);

        // 3. Construir el link de WhatsApp
        const whatsappURL = `https://wa.me/${NUMERO_WHATSAPP}?text=${mensajeCodificado}`;

        // 4. Redirigir al usuario
        window.open(whatsappURL, '_blank');

        // Opcional: Limpiar el pedido después de enviarlo
        pedido = [];
        actualizarPedido();
        direccionEntrega.value = "";
    });
});