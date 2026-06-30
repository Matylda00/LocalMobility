import { authFetch } from "./authService";

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? "").replace(/\/$/, "");

function buildUrl(path) {
  return `${API_BASE_URL}${path}`;
}

export async function getParkings() {
  let response;

  try {
    response = await authFetch(buildUrl("/api/parkings"));
  } catch {
    throw new Error("Nie udało się połączyć z serwerem parkingów.");
  }

  if (!response.ok) {
    throw new Error("Nie udało się pobrać parkingów.");
  }

  let data;

  try {
    data = await response.json();
  } catch {
    throw new Error("Nie udało się odczytać parkingów.");
  }

  const parkings = Array.isArray(data?.parkings) ? data.parkings : [];

  return parkings
    .map((parking, index) => {
      const lat = Number(parking.latitude);
      const lng = Number(parking.longitude);
      const availableSpaces = Number(parking.availableSpaces);

      return {
        id: `${parking.name ?? "parking"}-${lat}-${lng}-${index}`,
        name: parking.name ?? "Parking",
        lat,
        lng,
        availableSpaces: Number.isFinite(availableSpaces)
          ? availableSpaces
          : null,
      };
    })
    .filter((parking) => Number.isFinite(parking.lat) && Number.isFinite(parking.lng));
}