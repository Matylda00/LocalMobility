import { authFetch } from "./authService";

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? "").replace(/\/$/, "");

function buildUrl(path) {
  return `${API_BASE_URL}${path}`;
}

export async function getAdminEmails() {
  let response;

  try {
    response = await authFetch(buildUrl("/admin/emails"));
  } catch {
    throw new Error("Nie udało się pobrać listy maili.");
  }

  if (response.status === 401 || response.status === 403) {
    return null;
  }

  if (!response.ok) {
    throw new Error("Nie udało się pobrać listy maili.");
  }

  const data = await response.json();

  if (!Array.isArray(data)) {
    return [];
  }

  return data;
}