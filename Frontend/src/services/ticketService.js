import { authFetch } from "./authService";

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? "").replace(/\/$/, "");

function buildUrl(path) {
  return `${API_BASE_URL}${path}`;
}

function normalizePrice(price) {
  const numericPrice = Number(price);

  if (!Number.isFinite(numericPrice)) {
    return null;
  }

  return numericPrice;
}

function normalizeDuration(durationMinutes) {
  const numericDuration = Number(durationMinutes);

  if (!Number.isFinite(numericDuration)) {
    return null;
  }

  return numericDuration;
}

function normalizeStatus(status) {
  if (!status) {
    return "unknown";
  }

  return String(status).toLowerCase();
}

function normalizeTicketType(ticketType) {
  return {
    key: `${ticketType.name}__${ticketType.ticketCategory}`,
    name: ticketType.name,
    price: normalizePrice(ticketType.price),
    durationMinutes: normalizeDuration(ticketType.durationMinutes),
    ticketCategory: ticketType.ticketCategory,
  };
}

function normalizeTicket(ticket) {
  const normalizedStatus = normalizeStatus(ticket.status);

  return {
    id: ticket.uuid,
    uuid: ticket.uuid,
    name: ticket.name,
    price: normalizePrice(ticket.price),
    durationMinutes: normalizeDuration(ticket.durationMinutes),
    ticketCategory: ticket.ticketCategory,
    status: normalizedStatus,
    purchasedAt: ticket.purchasedAt,
    validFrom: ticket.validFrom,
    validTo: ticket.validTo,
    qrCode: ticket.qrCode,
  };
}

async function readJsonResponse(response, fallbackMessage) {
  let data = null;

  try {
    data = await response.json();
  } catch {
    data = null;
  }

  if (!response.ok) {
    if (response.status === 400) {
      throw new Error("Sprawdź dane i spróbuj ponownie.");
    }

    if (response.status === 401 || response.status === 403) {
      throw new Error("Sesja wygasła. Zaloguj się ponownie.");
    }

    if (response.status === 404) {
      throw new Error("Nie znaleziono wybranego biletu.");
    }

    if (response.status === 410) {
      throw new Error("Tego biletu nie można już aktywować.");
    }

    throw new Error(fallbackMessage);
  }

  return data;
}

export async function getTicketTypes() {
  let response;

  try {
    response = await authFetch(buildUrl("/api/ticket-types"));
  } catch {
    throw new Error("Nie udało się pobrać dostępnych biletów.");
  }

  const data = await readJsonResponse(
    response,
    "Nie udało się pobrać dostępnych biletów.",
  );

  const ticketTypes = Array.isArray(data?.ticketTypes) ? data.ticketTypes : [];

  return ticketTypes.map(normalizeTicketType);
}

export async function getTickets() {
  let response;

  try {
    response = await authFetch(buildUrl("/api/tickets"));
  } catch {
    throw new Error("Nie udało się pobrać Twoich biletów.");
  }

  const data = await readJsonResponse(
    response,
    "Nie udało się pobrać Twoich biletów.",
  );

  const tickets = Array.isArray(data?.tickets) ? data.tickets : [];

  return tickets.map(normalizeTicket);
}

export async function purchaseTicket(ticketType, paymentData) {
  let response;

  try {
    response = await authFetch(buildUrl("/api/tickets/purchase"), {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        ticketName: ticketType.name,
        ticketCategory: ticketType.ticketCategory,
        cardNumber: paymentData.cardNumber,
        expiryDate: paymentData.expiryDate,
        cvv: paymentData.cvv,
        cardHolder: paymentData.cardHolder,
      }),
    });
  } catch {
    throw new Error("Nie udało się kupić biletu. Spróbuj ponownie.");
  }

  const data = await readJsonResponse(
    response,
    "Nie udało się kupić biletu. Spróbuj ponownie.",
  );

  return normalizeTicket(data);
}

export async function activateTicket(ticketUuid) {
  let response;

  try {
    response = await authFetch(buildUrl(`/api/tickets/${ticketUuid}/activate`), {
      method: "POST",
    });
  } catch {
    throw new Error("Nie udało się skasować biletu. Spróbuj ponownie.");
  }

  const data = await readJsonResponse(
    response,
    "Nie udało się skasować biletu. Spróbuj ponownie.",
  );

  return normalizeTicket(data);
}