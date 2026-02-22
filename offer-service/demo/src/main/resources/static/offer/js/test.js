let originalOrders = [];
let displayedOrders = [];
let currentPage = 1;
const pageSize = 10;

function copyOrders() {
    const src = window.myAppData?.orders ?? [];
    originalOrders = src;
    displayedOrders = [...src];
    return displayedOrders;
}

function renderOrdersTable(ordersArray) {
    const tbody = document.getElementById("ordersTbody");
    const infoSpan = document.getElementById("paginationInfo");
    if (!tbody) return;

    // 1. Clear the existing table rows
    tbody.innerHTML = "";

    // 2. Pagination Math
    const totalOrders = ordersArray.length;
    const startIndex = (currentPage - 1) * pageSize;
    const endIndex = Math.min(startIndex + pageSize, totalOrders);

    // 3. Update the label
    if (infoSpan) {
        if (totalOrders === 0) {
            infoSpan.textContent = "No orders found.";
        } else {
            infoSpan.textContent = `Showing ${startIndex + 1}-${endIndex} of ${totalOrders} orders`;
        }
    }

    // 4. Get items for current page
    const paginatedItems = ordersArray.slice(startIndex, endIndex);

    // 5. Build the rows
paginatedItems.forEach(o => {
    const row = tbody.insertRow();

    // Helper to create cells with labels
    const createCell = (text, label) => {
        const cell = row.insertCell();
        cell.textContent = text;
        cell.setAttribute("data-label", label);
        return cell;
    };

    createCell(o.id ?? "", "Order ID");
    createCell(o.storeName ?? "", "Store");
    createCell(o.orderDate ?? "", "Date");

    // Status Badge Cell
    const statusCell = row.insertCell();
    statusCell.setAttribute("data-label", "Order Status");
    const badge = document.createElement("span");
    const statusClass = (o.status ?? "default").toLowerCase().replace(/\s+/g, '-');
    badge.className = `badge status-badge status-${statusClass}`;
    badge.textContent = o.status ?? "";
    statusCell.appendChild(badge);

    createCell(o.paymentStatus ?? "", "Payment");

    const price = o.totalPrice != null ? Number(o.totalPrice).toFixed(2) : "0.00";
    createCell(`${price} PLN`, "Total Price");

    // Details Button Cell
    const detailsCell = row.insertCell();
    detailsCell.setAttribute("data-label", "Details");
    const a = document.createElement("a");
    a.className = "btn btn-sm btn-outline-primary";
    a.href = `/offer/orders/${o.id}`;
    a.textContent = "Details";
    detailsCell.appendChild(a);

    // Status Update Form Cell
    const changeCell = row.insertCell();
    changeCell.setAttribute("data-label", "Action");
    changeCell.innerHTML = `
        <form action="/offer/wholesaler/orders/status" method="post" class="status-update-form">
            <input type="hidden" name="orderId" value="${o.id}" />
            <select name="status" class="form-select form-select-sm">
                <option value="Ordered" ${o.status === 'Ordered' ? 'selected' : ''}>Ordered</option>
                <option value="In progress" ${o.status === 'In progress' ? 'selected' : ''}>In progress</option>
                <option value="Shipped" ${o.status === 'Shipped' ? 'selected' : ''}>Shipped</option>
                <option value="Delivered" ${o.status === 'Delivered' ? 'selected' : ''}>Delivered</option>
            </select>
            <button type="submit" class="btn btn-sm btn-success">Update</button>
        </form>
    `;
});

    renderPaginationControls(totalOrders);
}

function renderPaginationControls(totalItems) {
    const paginationUl = document.getElementById("paginationControls");
    if (!paginationUl) return;

    paginationUl.innerHTML = "";
    const totalPages = Math.ceil(totalItems / pageSize);
    if (totalPages <= 1) return;

    // --- PREVIOUS BUTTON ---
    createPageItem(paginationUl, "«", currentPage > 1, () => {
        currentPage--;
        renderOrdersTable(displayedOrders);
    });

    // --- SMART PAGE NUMBERS ---
    const windowSize = 2; // How many pages to show before/after current

    for (let i = 1; i <= totalPages; i++) {
        // Always show first page, last page, and pages near the current page
        if (i === 1 || i === totalPages || (i >= currentPage - windowSize && i <= currentPage + windowSize)) {
            createPageItem(paginationUl, i, true, () => {
                currentPage = i;
                renderOrdersTable(displayedOrders);
            }, i === currentPage);
        }
        // Add ellipsis (...) if there's a gap
        else if (i === currentPage - windowSize - 1 || i === currentPage + windowSize + 1) {
            const li = document.createElement("li");
            li.className = "page-item disabled";
            li.innerHTML = '<span class="page-link">...</span>';
            paginationUl.appendChild(li);
        }
    }

    // --- NEXT BUTTON ---
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
    a.addEventListener("click", (e) => {
        e.preventDefault();
        if (enabled) onClick();
    });
    li.appendChild(a);
    container.appendChild(li);
}

function applyAllFilters() {
    const storeVal = document.getElementById("filterStore").value.toLowerCase();
    const idVal = document.getElementById("filterOrderId").value.toLowerCase();
    const dateFrom = document.getElementById("filterDateFrom").value;
    const dateTo = document.getElementById("filterDateTo").value;
    const statusVal = document.getElementById("filterStatus").value;

    displayedOrders = originalOrders.filter(o => {
        const matchesStore = (o.storeName ?? "").toLowerCase().includes(storeVal);
        const matchesId = String(o.id).toLowerCase().includes(idVal);
        const matchesStatus = statusVal === "" || o.status === statusVal;

        let matchesDate = true;
        if (o.orderDate) {
            const orderDateStr = o.orderDate.substring(0, 10);
            if (dateFrom && orderDateStr < dateFrom) matchesDate = false;
            if (dateTo && orderDateStr > dateTo) matchesDate = false;
        }
        return matchesStore && matchesId && matchesStatus && matchesDate;
    });

    currentPage = 1;
    renderOrdersTable(displayedOrders);
}

// --- INITIAL LOAD & EVENT LISTENERS ---
document.addEventListener("DOMContentLoaded", () => {
    copyOrders();
    renderOrdersTable(displayedOrders);

    // 1. Setup Filters
    const filterIds = ["filterStore", "filterOrderId", "filterDateFrom", "filterDateTo", "filterStatus"];
    filterIds.forEach(id => {
        const el = document.getElementById(id);
        if (el) el.addEventListener("input", applyAllFilters);
    });

    // 2. Setup Event Delegation for the Status Update Forms
// 2. Setup Event Delegation for the Status Update Forms
    const tbody = document.getElementById("ordersTbody");
    if (tbody) {
        tbody.addEventListener("submit", async (event) => {
            if (event.target.tagName === 'FORM') {
                event.preventDefault(); // Stop page reload

                const form = event.target;
                const formData = new FormData(form);
                const button = form.querySelector('button');

                // Disable button to prevent double-clicks
                button.disabled = true;

                try {
                    const response = await fetch(form.action, {
                        method: 'POST',
                        body: formData // Sends orderId and status
                    });

                    const result = await response.json(); // Assumes your controller returns JSON

                    if (response.ok) {
                        // Success! (200 OK)
                        alert("Success: " + result.message);

                        // Update the local data so the UI reflects the change
                        const orderId = formData.get('orderId');
                        const newStatus = formData.get('status');
                        const order = originalOrders.find(o => o.id == orderId);
                        if (order) order.status = newStatus;

                        renderOrdersTable(displayedOrders);
                    } else {
                        // Error! (400 Bad Request from your Service)
                        alert("Cannot update status: " + result.message);

                        // Reset the table to show the original status in the dropdown
                        renderOrdersTable(displayedOrders);
                    }
                } catch (error) {
                    console.error("Network error:", error);
                    alert("System error. Please try again later.");
                } finally {
                    button.disabled = false;
                }
            }
        });
    }
});