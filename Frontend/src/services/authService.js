const AUTH_STORAGE_KEY = "localmobility_auth";
const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? "").replace(/\/$/, "");

function buildUrl(path) {
  return `${API_BASE_URL}${path}`;
}

function encodeBasicAuth(email, password) {
  const value = `${email}:${password}`;
  const bytes = new TextEncoder().encode(value);

  let binary = "";
  bytes.forEach((byte) => {
    binary += String.fromCharCode(byte);
  });

  return `Basic ${btoa(binary)}`;
}

export function getStoredAuth() {
  try {
    const raw = localStorage.getItem(AUTH_STORAGE_KEY);

    if (!raw) {
      return null;
    }

    const auth = JSON.parse(raw);

    if (!auth?.email || !auth?.password) {
      clearAuth();
      return null;
    }

    return auth;
  } catch {
    clearAuth();
    return null;
  }
}

export function saveAuth(auth) {
  localStorage.setItem(
    AUTH_STORAGE_KEY,
    JSON.stringify({
      email: auth.email,
      password: auth.password,
    }),
  );
}

export function clearAuth() {
  localStorage.removeItem(AUTH_STORAGE_KEY);
}

export function getAuthorizationHeader(auth = getStoredAuth()) {
  if (!auth?.email || !auth?.password) {
    return {};
  }

  return {
    Authorization: encodeBasicAuth(auth.email, auth.password),
  };
}

export async function registerUser({ email, password }) {
  const normalizedEmail = email.trim().toLowerCase();

  let response;

  try {
    response = await fetch(buildUrl("/auth/register"), {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        email: normalizedEmail,
        password,
      }),
    });
  } catch {
    throw new Error("Nie udało się połączyć z serwerem.");
  }

  if (response.status === 201) {
    return;
  }

  if (response.status === 409) {
    throw new Error("Konto z takim adresem e-mail już istnieje.");
  }

  throw new Error("Nie udało się założyć konta.");
}

export async function loginUser({ email, password }) {
  const normalizedEmail = email.trim().toLowerCase();

  const auth = {
    email: normalizedEmail,
    password,
  };

  let response;

  try {
    response = await fetch(buildUrl("/api/ticket-types"), {
      headers: getAuthorizationHeader(auth),
    });
  } catch {
    throw new Error("Nie udało się połączyć z serwerem.");
  }

  if (response.status === 401 || response.status === 403) {
    throw new Error("Nieprawidłowy e-mail albo hasło.");
  }

  if (!response.ok) {
    throw new Error("Nie udało się sprawdzić danych logowania.");
  }

  saveAuth(auth);
  return auth;
}

export async function authFetch(input, init = {}) {
  const headers = new Headers(init.headers ?? {});
  const auth = getStoredAuth();

  if (auth && !headers.has("Authorization")) {
    headers.set("Authorization", encodeBasicAuth(auth.email, auth.password));
  }

  return fetch(input, {
    ...init,
    headers,
  });
}