const TICKETS_STORAGE_KEY = "localmobility_tickets";

export const TICKET_TYPES = [
  {
    id: "ticket-20-normal",
    name: "Bilet 20-minutowy normalny",
    durationMinutes: 20,
    price: 4.0,
  },
  {
    id: "ticket-20-reduced",
    name: "Bilet 20-minutowy ulgowy",
    durationMinutes: 20,
    price: 2.0,
  },
  {
    id: "ticket-60-normal",
    name: "Bilet 60-minutowy normalny",
    durationMinutes: 60,
    price: 6.0,
  },
  {
    id: "ticket-60-reduced",
    name: "Bilet 60-minutowy ulgowy",
    durationMinutes: 60,
    price: 3.0,
  },
  {
    id: "ticket-24h-normal",
    name: "Bilet 24-godzinny normalny",
    durationMinutes: 24 * 60,
    price: 17.0,
  },
];

function readTicketsFromStorage() {
  const rawTickets = localStorage.getItem(TICKETS_STORAGE_KEY);

  if (!rawTickets) {
    return [];
  }

  try {
    return JSON.parse(rawTickets);
  } catch {
    return [];
  }
}

function saveTicketsToStorage(tickets) {
  localStorage.setItem(TICKETS_STORAGE_KEY, JSON.stringify(tickets));
}

function normalizeTicketStatus(ticket) {
  if (ticket.status !== "active") {
    return ticket;
  }

  const validUntil = new Date(ticket.validUntil).getTime();
  const now = Date.now();

  if (validUntil < now) {
    return {
      ...ticket,
      status: "expired",
    };
  }

  return ticket;
}

export async function getTicketTypes() {
  return TICKET_TYPES;
}

export async function getTickets() {
  const tickets = readTicketsFromStorage().map(normalizeTicketStatus);
  saveTicketsToStorage(tickets);

  return tickets;
}

export async function createTicket(ticketType, paymentResult) {
  const ticket = {
    id: crypto.randomUUID(),
    typeId: ticketType.id,
    name: ticketType.name,
    durationMinutes: ticketType.durationMinutes,
    price: ticketType.price,
    currency: "PLN",
    status: "inactive",
    purchasedAt: new Date().toISOString(),
    validFrom: null,
    validUntil: null,
    transactionId: paymentResult.transactionId,
  };

  const tickets = await getTickets();
  const updatedTickets = [ticket, ...tickets];

  saveTicketsToStorage(updatedTickets);

  return ticket;
}

export async function activateTicket(ticketId) {
  const now = new Date();

  const tickets = await getTickets();

  const updatedTickets = tickets.map((ticket) => {
    if (ticket.id !== ticketId) {
      return ticket;
    }

    if (ticket.status !== "inactive") {
      return ticket;
    }

    return {
      ...ticket,
      status: "active",
      validFrom: now.toISOString(),
      validUntil: new Date(
        now.getTime() + ticket.durationMinutes * 60 * 1000
      ).toISOString(),
    };
  });

  saveTicketsToStorage(updatedTickets);

  return updatedTickets;
}

export async function removeTicket(ticketId) {
  const tickets = await getTickets();
  const updatedTickets = tickets.filter((ticket) => ticket.id !== ticketId);

  saveTicketsToStorage(updatedTickets);

  return updatedTickets;
}