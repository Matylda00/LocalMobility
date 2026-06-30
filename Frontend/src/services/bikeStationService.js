import { authFetch } from "./authService";

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? "").replace(/\/$/, "");

function buildUrl(path) {
  return `${API_BASE_URL}${path}`;
}

export async function getBikeStations() {
  let response;

  try {
    response = await authFetch(buildUrl("/api/bike-stations"));
  } catch {
    throw new Error("Nie udało się połączyć z serwerem stacji rowerowych.");
  }

  if (!response.ok) {
    throw new Error("Nie udało się pobrać stacji rowerowych.");
  }

  let data;

  try {
    data = await response.json();
  } catch {
    throw new Error("Nie udało się odczytać stacji rowerowych.");
  }

  const stations = Array.isArray(data?.stations) ? data.stations : [];

  return stations
    .map((station, index) => {
      const lat = Number(station.latitude);
      const lng = Number(station.longitude);
      const availableBikes = Number(station.availableBikes);

      return {
        id:
          station.stationId ??
          `${station.name ?? "bike-station"}-${lat}-${lng}-${index}`,
        name: station.name ?? "Stacja rowerowa",
        lat,
        lng,
        availableBikes: Number.isFinite(availableBikes)
          ? availableBikes
          : null,
      };
    })
    .filter((station) => Number.isFinite(station.lat) && Number.isFinite(station.lng));
}