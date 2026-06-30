import { authFetch } from "./authService";

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? "").replace(/\/$/, "");

function buildUrl(path) {
  return `${API_BASE_URL}${path}`;
}

export async function getBusLocations() {
  let response;

  try {
    response = await authFetch(buildUrl("/api/bus-locations"));
  } catch {
    throw new Error("Nie udało się połączyć z serwerem lokalizacji autobusów.");
  }

  if (!response.ok) {
    throw new Error("Nie udało się pobrać lokalizacji autobusów.");
  }

  let data;

  try {
    data = await response.json();
  } catch {
    throw new Error("Nie udało się odczytać lokalizacji autobusów.");
  }

  const buses = Array.isArray(data?.buses) ? data.buses : [];

  return buses
    .map((bus, index) => {
      const lat = Number(bus.latitude);
      const lng = Number(bus.longitude);

      return {
        id: `${bus.line ?? "bus"}-${lat}-${lng}-${index}`,
        line: bus.line ?? "?",
        lat,
        lng,
      };
    })
    .filter((bus) => Number.isFinite(bus.lat) && Number.isFinite(bus.lng));
}