const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? "").replace(/\/$/, "");

class ScheduleApiError extends Error {
  constructor(message) {
    super(message);
    this.name = "ScheduleApiError";
  }
}

function buildUrl(path, params = {}) {
  const url = new URL(`${API_BASE_URL}${path}`, window.location.origin);

  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") {
      url.searchParams.set(key, value);
    }
  });

  return url.toString();
}

async function requestJson(path, params, messages = {}) {
  let response;

  try {
    response = await fetch(buildUrl(path, params));
  } catch {
    throw new ScheduleApiError(
      messages.connection ||
        "Nie udało się połączyć z serwerem. Sprawdź, czy backend jest uruchomiony."
    );
  }

  if (!response.ok) {
    if (response.status === 404) {
      throw new ScheduleApiError(
        messages.notFound ||
          "Nie znaleziono rozkładu dla wybranych danych."
      );
    }

    if (response.status >= 500) {
      throw new ScheduleApiError(
        messages.server ||
          "Rozkład jest teraz niedostępny. Spróbuj ponownie za chwilę."
      );
    }

    throw new ScheduleApiError(
      messages.default ||
        "Nie udało się pobrać rozkładu. Zmień dane i spróbuj ponownie."
    );
  }

  return response.json();
}

export async function getBusLines() {
  return requestJson("/api/bus-lines", {}, {
    connection:
      "Nie udało się pobrać listy linii. Sprawdź, czy backend jest uruchomiony.",
    server:
      "Lista linii jest teraz niedostępna. Spróbuj ponownie za chwilę.",
    default:
      "Nie udało się pobrać listy linii.",
  });
}

export async function getLineStops({ lineNumber, direction, date }) {
  return requestJson(
    `/api/bus-lines/${encodeURIComponent(lineNumber)}/stops`,
    {
      direction,
      date,
    },
    {
      notFound:
        "Ta linia nie kursuje w wybrany dzień albo nie ma trasy w tym kierunku.",
      connection:
        "Nie udało się pobrać trasy. Sprawdź, czy backend jest uruchomiony.",
      server:
        "Trasa tej linii jest teraz niedostępna. Spróbuj ponownie za chwilę.",
      default:
        "Nie udało się pobrać przystanków dla tej linii.",
    }
  );
}

export async function getStopDepartures({
  lineNumber,
  stopId,
  direction,
  date,
}) {
  return requestJson(
    `/api/bus-lines/${encodeURIComponent(lineNumber)}/stops/${encodeURIComponent(
      stopId
    )}/departures`,
    {
      direction,
      date,
    },
    {
      notFound:
        "Z tego przystanku nie ma odjazdów tej linii w wybrany dzień.",
      connection:
        "Nie udało się pobrać odjazdów. Sprawdź, czy backend jest uruchomiony.",
      server:
        "Odjazdy z tego przystanku są teraz niedostępne. Spróbuj ponownie za chwilę.",
      default:
        "Nie udało się pobrać odjazdów z tego przystanku.",
    }
  );
}