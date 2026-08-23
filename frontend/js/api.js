/* ==========================================================================
   API client - all requests to the Spring Boot backend go through here.
   ========================================================================== */

const API_BASE_URL = "http://localhost:8080/api";

const Auth = {
  saveSession(authResponse) {
    localStorage.setItem("dka_token", authResponse.token);
    localStorage.setItem("dka_name", authResponse.name);
    localStorage.setItem("dka_email", authResponse.email);
    localStorage.setItem("dka_role", authResponse.role);
  },
  getToken() { return localStorage.getItem("dka_token"); },
  getName() { return localStorage.getItem("dka_name"); },
  getEmail() { return localStorage.getItem("dka_email"); },
  getRole() { return localStorage.getItem("dka_role"); },
  isAdmin() { return this.getRole() === "ADMIN"; },
  isLoggedIn() { return !!this.getToken(); },
  logout() {
    localStorage.removeItem("dka_token");
    localStorage.removeItem("dka_name");
    localStorage.removeItem("dka_email");
    localStorage.removeItem("dka_role");
    window.location.href = "login.html";
  },
  /** Redirect to login if not authenticated; call at the top of every protected page. */
  requireAuth() {
    if (!this.isLoggedIn()) {
      window.location.href = "login.html";
    }
  },
  /** Redirect non-admins away from admin-only pages/actions. */
  requireAdmin() {
    this.requireAuth();
    if (!this.isAdmin()) {
      alert("This page is restricted to administrators.");
      window.location.href = "dashboard.html";
    }
  }
};

class ApiError extends Error {
  constructor(message, status) {
    super(message);
    this.status = status;
  }
}

const Api = {
  async request(path, { method = "GET", body = null, isFormData = false } = {}) {
    const headers = {};
    const token = Auth.getToken();
    if (token) headers["Authorization"] = "Bearer " + token;
    if (!isFormData && body) headers["Content-Type"] = "application/json";

    let response;
    try {
      response = await fetch(API_BASE_URL + path, {
        method,
        headers,
        body: isFormData ? body : (body ? JSON.stringify(body) : null)
      });
    } catch (networkErr) {
      throw new ApiError(
        "Could not reach the backend at " + API_BASE_URL + ". Is the Spring Boot server running?", 0
      );
    }

    if (response.status === 401) {
      Auth.logout();
      throw new ApiError("Session expired. Please log in again.", 401);
    }

    if (response.status === 204) return null;

    let data = null;
    const text = await response.text();
    if (text) {
      try { data = JSON.parse(text); } catch (e) { data = text; }
    }

    if (!response.ok) {
      const message = (data && data.message) ? data.message : "Request failed (" + response.status + ")";
      throw new ApiError(message, response.status);
    }
    return data;
  },

  get(path) { return this.request(path); },
  post(path, body) { return this.request(path, { method: "POST", body }); },
  put(path, body) { return this.request(path, { method: "PUT", body }); },
  del(path) { return this.request(path, { method: "DELETE" }); },
  postForm(path, formData) { return this.request(path, { method: "POST", body: formData, isFormData: true }); },

  // ---- Auth ----
  login(email, password) { return this.post("/auth/login", { email, password }); },
  register(name, email, password, role) { return this.post("/auth/register", { name, email, password, role }); },

  // ---- Policies ----
  listPolicies() { return this.get("/policies"); },
  getPolicy(id) { return this.get("/policies/" + id); },
  createPolicy(name, description) { return this.post("/policies", { name, description }); },
  deletePolicy(id) { return this.del("/policies/" + id); },
  listVersions(policyId) { return this.get("/policies/" + policyId + "/versions"); },
  uploadVersion(policyId, file, effectiveDate) {
    const form = new FormData();
    form.append("file", file);
    if (effectiveDate) form.append("effectiveDate", effectiveDate);
    return this.postForm("/policies/" + policyId + "/versions", form);
  },
  activateVersion(policyId, versionNumber) {
    return this.put("/policies/" + policyId + "/versions/" + versionNumber + "/activate", null);
  },
  archiveVersion(policyId, versionNumber) {
    return this.put("/policies/" + policyId + "/versions/" + versionNumber + "/archive", null);
  },
  compareVersions(policyId, from, to) {
    return this.get("/policies/" + policyId + "/compare?from=" + from + "&to=" + to);
  },

  // ---- Repositories ----
  listRepositories() { return this.get("/repositories"); },
  connectRepository(owner, name, branch) { return this.post("/repositories", { owner, name, branch }); },
  listFiles(repoId) { return this.get("/repositories/" + repoId + "/files"); },
  getFileContent(repoId, path) { return this.get("/repositories/" + repoId + "/files/content?path=" + encodeURIComponent(path)); },
  listCommits(repoId, path) {
    return this.get("/repositories/" + repoId + "/commits" + (path ? "?path=" + encodeURIComponent(path) : ""));
  },

  // ---- Assistant ----
  ask(question, repositoryId, filePath) {
    return this.post("/assistant/ask", { question, repositoryId, filePath });
  }
};

