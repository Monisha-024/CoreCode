/* ==========================================================================
   Renders the shared sidebar and wires up the logout button.
   Call renderSidebar("dashboard") etc. with the current page's nav key.
   ========================================================================== */

function renderSidebar(activeKey) {
  const isAdmin = Auth.isAdmin();

  const navItems = [
    { key: "dashboard", href: "dashboard.html", label: "Dashboard", icon: "\u25A3" },
    { key: "assistant", href: "assistant.html", label: "Ask AI", icon: "\u25C9" },
    { key: "policies", href: "policies.html", label: "Policies", icon: "\u2637" },
    { key: "repositories", href: "repositories.html", label: "Repositories", icon: "\u2318" },
    { key: "code-explorer", href: "code-explorer.html", label: "Code Explorer", icon: "\u2699" }
  ];

  const navHtml = navItems.map(item => `
    <a href="${item.href}" class="${item.key === activeKey ? "active" : ""}">
      <span>${item.icon}</span> ${item.label}
    </a>
  `).join("");

  const el = document.getElementById("sidebar-root");
  if (!el) return;

  el.innerHTML = `
    <aside class="sidebar">
      <div class="brand">
        <h2>CoreCode</h2>
        <small>AI Developer Knowledge &amp; Policy Assistant</small>
      </div>
      <nav>${navHtml}</nav>
      <div class="sidebar-footer">
        <div class="user-chip">
          ${Auth.getName() || ""}
          <span class="badge ${isAdmin ? "badge-admin" : "badge-developer"}" style="margin-left:6px;">
            ${Auth.getRole() || ""}
          </span>
        </div>
        <button class="logout-btn" onclick="Auth.logout()">Log out</button>
      </div>
    </aside>
  `;
}

function escapeHtml(str) {
  if (str === null || str === undefined) return "";
  return String(str)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

function formatDateTime(value) {
  if (!value) return "-";
  try {
    return new Date(value).toLocaleString();
  } catch (e) {
    return value;
  }
}

function showAlert(containerId, message, type = "error") {
  const el = document.getElementById(containerId);
  if (!el) return;
  el.className = "alert alert-" + type;
  el.textContent = message;
  el.classList.remove("hidden");
}

function hideAlert(containerId) {
  const el = document.getElementById(containerId);
  if (el) el.classList.add("hidden");
}

