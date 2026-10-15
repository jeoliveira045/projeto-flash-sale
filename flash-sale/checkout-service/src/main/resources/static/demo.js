const result = document.querySelector("#order-result");

if (result) {
    const orderId = result.dataset.orderId;
    const status = document.querySelector("#order-status");
    const reason = document.querySelector("#order-reason");
    const correlation = document.querySelector("#order-correlation-id");
    const correlationId = correlation.textContent.trim();
    let attempts = 0;

    const refresh = async () => {
        attempts += 1;
        try {
            const response = await fetch(`/v1/orders/${encodeURIComponent(orderId)}`, {
                headers: {
                    Accept: "application/json",
                    "X-Correlation-Id": correlationId
                }
            });
            if (response.ok) {
                const order = await response.json();
                status.textContent = order.status;
                correlation.textContent = order.correlation_id;
                if (order.motivo_falha) {
                    if (!reason) {
                        const detail = document.createElement("span");
                        detail.id = "order-reason";
                        result.insertBefore(detail, correlation.parentElement);
                        detail.textContent = order.motivo_falha;
                    } else {
                        reason.textContent = order.motivo_falha;
                    }
                }
                result.classList.toggle("order-confirmed", order.status === "CONFIRMED");
                result.classList.toggle("order-cancelled", order.status === "CANCELLED");
                if (order.status !== "PROCESSING" || attempts >= 30) {
                    clearInterval(timer);
                }
            }
        } catch {
            if (attempts >= 30) clearInterval(timer);
        }
    };

    const timer = setInterval(refresh, 500);
    refresh();
}