let originalOrders = [];
let displayedOrders = [];
let currentPage = 1;
const pageSize = 10;

function copyOrders() {
    const src = window.myAppData?.orders ?? [];
    originalOrders = src;
    displayedOrders = [...src];
}

async function handleCheckout(orderId) {
    try {
        const response = await fetch('/payment/getLinkForCheckout', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(orderId)
        });

        if (response.ok) {
            const checkoutUrl = await response.text();
            window.location.href = checkoutUrl; // Redirecting is usually better than an alert!
        } else {
            // Read the actual error message from the body
            const errorMessage = await response.text();
            alert("Server error: " + errorMessage);
        }
    } catch (error) {
        console.error("Network error:", error);
        alert("An error occurred. Check your connection.");
    }
}

function renderOrdersTable(ordersArray) {
    const tbody = document.getElementById("ordersTbody");
    const infoSpan = document.getElementById("paginationInfo");
    if (!tbody) return;

    tbody.innerHTML = "";
    const totalOrders = ordersArray.length;
    const startIndex = (currentPage - 1) * pageSize;
    const endIndex = Math.min(startIndex + pageSize, totalOrders);

    if (infoSpan) {
        infoSpan.textContent = totalOrders === 0 ? "No orders found." : `Showing ${startIndex + 1}-${endIndex} of ${totalOrders} orders`;
    }

    const paginatedItems = ordersArray.slice(startIndex, endIndex);

    paginatedItems.forEach(o => {
        const row = tbody.insertRow();

        // Helper to add data-label for mobile CSS
        const addCell = (text, label) => {
            const cell = row.insertCell();
            cell.textContent = text;
            cell.setAttribute("data-label", label);
            return cell;
        };

        // 1. Order ID
        addCell(o.id ?? "", "Order ID");

        // 2. Wholesaler Name
        addCell(o.wholesalerName ?? "", "Wholesaler");

        // 3. Date
        addCell(o.orderDate ?? "", "Date");

        // 4. Status Badge
        const statusCell = row.insertCell();
        statusCell.setAttribute("data-label", "Order Status");
        const badge = document.createElement("span");
        const statusClass = (o.status ?? "default").toLowerCase().replace(/\s+/g, '-');
        badge.className = `badge status-badge status-${statusClass}`;
        badge.textContent = o.status ?? "";
        statusCell.appendChild(badge);

        // 5. Payment Status
        const paymentCell = row.insertCell();
        paymentCell.setAttribute("data-label", "Payment Status");
        const currentPaymentStatus = (o.paymentStatus ?? "").trim();
        const isPending = currentPaymentStatus.toLowerCase() === "pending";

        const paymentBtn = document.createElement("button");
        if (isPending) {
            paymentBtn.className = "btn btn-link p-0 text-decoration-none fw-bold";
            paymentBtn.textContent = currentPaymentStatus + " (Pay)";
            paymentBtn.onclick = () => handleCheckout(o.id);
        } else {
            paymentBtn.className = "btn p-0 text-muted text-decoration-none";
            paymentBtn.style.cursor = "default";
            paymentBtn.disabled = true;
            paymentBtn.textContent = currentPaymentStatus || "Paid";
        }
        paymentCell.appendChild(paymentBtn);

        // 6. Price
        const price = o.totalPrice != null ? Number(o.totalPrice).toFixed(2) : "0.00";
        addCell(`${price} PLN`, "Total price");

        // 7. Details Button
        const detailsCell = row.insertCell();
        detailsCell.setAttribute("data-label", "Details");
        detailsCell.className = "text-center";
        const detailsBtn = document.createElement("a");
        detailsBtn.className = "btn btn-sm btn-outline-primary w-100";
        detailsBtn.href = `/offer/orders/${o.id}`;
        detailsBtn.textContent = "View Details";
        detailsCell.appendChild(detailsBtn);

        // 8. Cancel Button
        const cancelCell = row.insertCell();
        cancelCell.setAttribute("data-label", "Action");
        cancelCell.className = "text-center";
        const cancelForm = document.createElement("form");
        cancelForm.method = "POST";
        cancelForm.action = "/offer/orders/cancel";
        cancelForm.className = "w-100";

        const hiddenInput = document.createElement("input");
        hiddenInput.type = "hidden";
        hiddenInput.name = "orderId";
        hiddenInput.value = o.id;

        const cancelBtn = document.createElement("button");
        cancelBtn.type = "submit";
        cancelBtn.textContent = "Cancel";

        const currentStatus = (o.status ?? "").trim();
        if (currentStatus === "Ordered") {
            cancelBtn.className = "btn btn-sm btn-danger w-100";
            cancelForm.onsubmit = (e) => { if (!confirm(`Cancel order #${o.id}?`)) e.preventDefault(); };
        } else {
            cancelBtn.className = "btn btn-sm btn-secondary opacity-50 w-100";
            cancelBtn.disabled = true;
        }

        cancelForm.appendChild(hiddenInput);
        cancelForm.appendChild(cancelBtn);
        cancelCell.appendChild(cancelForm);
    });

    renderPaginationControls(totalOrders);
}

function renderPaginationControls(totalItems) {
    const paginationUl = document.getElementById("paginationControls");
    if (!paginationUl) return;
    paginationUl.innerHTML = "";

    const totalPages = Math.ceil(totalItems / pageSize);
    if (totalPages <= 1) return;

    const windowSize = 1; // Shows current, 1 before, 1 after

    // Prev
    createPageItem(paginationUl, "«", currentPage > 1, () => {
        currentPage--;
        renderOrdersTable(displayedOrders);
    });

    for (let i = 1; i <= totalPages; i++) {
        // Only show first, last, and pages near current
        if (i === 1 || i === totalPages || (i >= currentPage - windowSize && i <= currentPage + windowSize)) {
            createPageItem(paginationUl, i, true, () => {
                currentPage = i;
                renderOrdersTable(displayedOrders);
            }, i === currentPage);
        }
        // Add dots
        else if (i === currentPage - windowSize - 1 || i === currentPage + windowSize + 1) {
            const li = document.createElement("li");
            li.className = "page-item disabled";
            li.innerHTML = '<span class="page-link">...</span>';
            paginationUl.appendChild(li);
        }
    }

    // Next
    createPageItem(paginationUl, "»", currentPage < totalPages, () => {
        currentPage++;
        renderOrdersTable(displayedOrders);
    });
}

// Funkcja filtrowania dostosowana do hurtownika
function applyAllFilters() {
    const wholesalerVal = document.getElementById("filterWholesaler").value.toLowerCase();
    const idVal = document.getElementById("filterOrderId").value.toLowerCase();
    const dateFrom = document.getElementById("filterDateFrom").value;
    const dateTo = document.getElementById("filterDateTo").value;
    const statusVal = document.getElementById("filterStatus").value;

    displayedOrders = originalOrders.filter(o => {
        const matchesWholesaler = (o.wholesalerName ?? "").toLowerCase().includes(wholesalerVal);
        const matchesId = String(o.id).toLowerCase().includes(idVal);
        const matchesStatus = statusVal === "" || o.status === statusVal;

        let matchesDate = true;
        if (o.orderDate) {
            const orderDateStr = o.orderDate.toString().substring(0, 10);
            if (dateFrom && orderDateStr < dateFrom) matchesDate = false;
            if (dateTo && orderDateStr > dateTo) matchesDate = false;
        }
        return matchesWholesaler && matchesId && matchesStatus && matchesDate;
    });

    currentPage = 1;
    renderOrdersTable(displayedOrders);
}

// Reużywalne funkcje paginacji (takie same jak u Ciebie)
function renderPaginationControls(totalItems) {
    const paginationUl = document.getElementById("paginationControls");
    if (!paginationUl) return;
    paginationUl.innerHTML = "";

    const totalPages = Math.ceil(totalItems / pageSize);
    if (totalPages <= 1) return;

    const windowSize = 1; // Shows current, 1 before, 1 after

    // Prev
    createPageItem(paginationUl, "«", currentPage > 1, () => {
        currentPage--;
        renderOrdersTable(displayedOrders);
    });

    for (let i = 1; i <= totalPages; i++) {
        // Only show first, last, and pages near current
        if (i === 1 || i === totalPages || (i >= currentPage - windowSize && i <= currentPage + windowSize)) {
            createPageItem(paginationUl, i, true, () => {
                currentPage = i;
                renderOrdersTable(displayedOrders);
            }, i === currentPage);
        }
        // Add dots
        else if (i === currentPage - windowSize - 1 || i === currentPage + windowSize + 1) {
            const li = document.createElement("li");
            li.className = "page-item disabled";
            li.innerHTML = '<span class="page-link">...</span>';
            paginationUl.appendChild(li);
        }
    }

    // Next
    createPageItem(paginationUl, "»", currentPage < totalPages, () => {
        currentPage++;
        renderOrdersTable(displayedOrders);
    });
}

function createPageItem(container, text, enabled, onClick, isActive = false) {
    const li = document.createElement("li");
    li.className = `page-item ${enabled ? '' : 'disabled'} ${isActive ? 'active' : ''}`;
    const a = document.createElement("a");
    a.className = "page-link";
    a.href = "#";
    a.textContent = text;
    a.addEventListener("click", (e) => { e.preventDefault(); if (enabled) onClick(); });
    li.appendChild(a);
    container.appendChild(li);
}

document.addEventListener("DOMContentLoaded", () => {
    copyOrders();
    renderOrdersTable(displayedOrders);

    const filterIds = ["filterWholesaler", "filterOrderId", "filterDateFrom", "filterDateTo", "filterStatus"];
    filterIds.forEach(id => {
        const el = document.getElementById(id);
        if (el) el.addEventListener("input", applyAllFilters);
    });
});