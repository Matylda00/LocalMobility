import { useEffect, useRef, useState } from "react";

import maplibregl from "maplibre-gl";

import "maplibre-gl/dist/maplibre-gl.css";
import { getBikeStations } from "../services/bikeStationService";
import { getBusLocations } from "../services/busLocationService";
import { getParkings } from "../services/parkingService";

function createMarkerElement({ label, background }) {
  const markerElement = document.createElement("div");

  markerElement.style.minWidth = "38px";
  markerElement.style.height = "38px";
  markerElement.style.padding = "0 8px";
  markerElement.style.borderRadius = "999px";
  markerElement.style.background = background;
  markerElement.style.color = "white";
  markerElement.style.display = "flex";
  markerElement.style.alignItems = "center";
  markerElement.style.justifyContent = "center";
  markerElement.style.fontWeight = "700";
  markerElement.style.fontSize = "13px";
  markerElement.style.border = "2px solid white";
  markerElement.style.boxShadow = "0 8px 18px rgba(0, 0, 0, 0.35)";
  markerElement.style.cursor = "pointer";
  markerElement.innerText = label;

  return markerElement;
}

function fitMapToPoints(map, points) {
  if (!map || points.length === 0) {
    return;
  }

  const bounds = new maplibregl.LngLatBounds();

  points.forEach((point) => {
    bounds.extend([point.lng, point.lat]);
  });

  map.fitBounds(bounds, {
    padding: 60,
    maxZoom: 13,
    duration: 800,
  });
}

function createPopupContent() {
  const container = document.createElement("div");
  container.style.fontFamily = "sans-serif";

  return container;
}

function createPopupTitle(text) {
  const title = document.createElement("strong");
  title.textContent = text;

  return title;
}

function createPopupLine(text) {
  const line = document.createElement("div");
  line.textContent = text;

  return line;
}

function createParkingPopup(parking) {
  const container = createPopupContent();

  const availableSpacesText =
    parking.availableSpaces === null
      ? "Brak danych"
      : `${parking.availableSpaces}`;

  container.appendChild(createPopupTitle(parking.name));
  container.appendChild(createPopupLine(`Wolne miejsca: ${availableSpacesText}`));

  return new maplibregl.Popup({
    offset: 28,
  }).setDOMContent(container);
}

function createBusPopup(bus) {
  const container = createPopupContent();

  container.appendChild(createPopupTitle(`Linia ${bus.line}`));

  return new maplibregl.Popup({
    offset: 28,
  }).setDOMContent(container);
}

function createBikePopup(station) {
  const container = createPopupContent();

  const availableBikesText =
    station.availableBikes === null
      ? "Brak danych"
      : `${station.availableBikes}`;

  container.appendChild(createPopupTitle(station.name));
  container.appendChild(createPopupLine(`Dostępne rowery: ${availableBikesText}`));

  return new maplibregl.Popup({
    offset: 28,
  }).setDOMContent(container);
}

export default function MapView({ mapMode }) {
  const mapContainerRef = useRef(null);
  const mapRef = useRef(null);
  const busMarkersRef = useRef({});
  const parkingMarkersRef = useRef({});
  const bikeMarkersRef = useRef({});
  const hasCenteredOnBusesRef = useRef(false);
  const hasCenteredOnParkingsRef = useRef(false);
  const hasCenteredOnBikesRef = useRef(false);

  const [buses, setBuses] = useState([]);
  const [parkings, setParkings] = useState([]);
  const [bikeStations, setBikeStations] = useState([]);

  useEffect(() => {
    if (mapRef.current) {
      return;
    }

    mapRef.current = new maplibregl.Map({
      container: mapContainerRef.current,
      style: {
        version: 8,
        sources: {
          osm: {
            type: "raster",
            tiles: ["https://tile.openstreetmap.org/{z}/{x}/{y}.png"],
            tileSize: 256,
            attribution:
              '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
          },
        },
        layers: [
          {
            id: "osm",
            type: "raster",
            source: "osm",
          },
        ],
      },
      center: [21.0122, 52.2297],
      zoom: 11,
    });

    mapRef.current.addControl(new maplibregl.NavigationControl(), "top-right");
  }, []);

  useEffect(() => {
    if (mapMode !== "buses") {
      return;
    }

    let isActive = true;

    async function loadBuses() {
      try {
        const loadedBuses = await getBusLocations();

        if (!isActive) {
          return;
        }

        setBuses(loadedBuses);

        if (!hasCenteredOnBusesRef.current && loadedBuses.length > 0) {
          fitMapToPoints(mapRef.current, loadedBuses);
          hasCenteredOnBusesRef.current = true;
        }
      } catch (error) {
        console.warn(error.message);
      }
    }

    loadBuses();

    const intervalId = setInterval(loadBuses, 10000);

    return () => {
      isActive = false;
      clearInterval(intervalId);
    };
  }, [mapMode]);

  useEffect(() => {
    if (mapMode !== "parking") {
      return;
    }

    let isActive = true;

    async function loadParkings() {
      try {
        const loadedParkings = await getParkings();

        if (!isActive) {
          return;
        }

        setParkings(loadedParkings);

        if (!hasCenteredOnParkingsRef.current && loadedParkings.length > 0) {
          fitMapToPoints(mapRef.current, loadedParkings);
          hasCenteredOnParkingsRef.current = true;
        }
      } catch (error) {
        console.warn(error.message);
      }
    }

    loadParkings();

    const intervalId = setInterval(loadParkings, 10000);

    return () => {
      isActive = false;
      clearInterval(intervalId);
    };
  }, [mapMode]);

  useEffect(() => {
    if (mapMode !== "bikes") {
      return;
    }

    let isActive = true;

    async function loadBikeStations() {
      try {
        const loadedBikeStations = await getBikeStations();

        if (!isActive) {
          return;
        }

        setBikeStations(loadedBikeStations);

        if (!hasCenteredOnBikesRef.current && loadedBikeStations.length > 0) {
          fitMapToPoints(mapRef.current, loadedBikeStations);
          hasCenteredOnBikesRef.current = true;
        }
      } catch (error) {
        console.warn(error.message);
      }
    }

    loadBikeStations();

    const intervalId = setInterval(loadBikeStations, 10000);

    return () => {
      isActive = false;
      clearInterval(intervalId);
    };
  }, [mapMode]);

  useEffect(() => {
    if (!mapRef.current) {
      return;
    }

    Object.values(busMarkersRef.current).forEach((marker) => marker.remove());
    Object.values(parkingMarkersRef.current).forEach((marker) =>
      marker.remove(),
    );
    Object.values(bikeMarkersRef.current).forEach((marker) => marker.remove());

    busMarkersRef.current = {};
    parkingMarkersRef.current = {};
    bikeMarkersRef.current = {};

    if (mapMode === "buses") {
      buses.forEach((bus) => {
        const markerElement = createMarkerElement({
          label: bus.line,
          background: "#228be6",
        });

        const marker = new maplibregl.Marker({
          element: markerElement,
        })
          .setLngLat([bus.lng, bus.lat])
          .setPopup(createBusPopup(bus))
          .addTo(mapRef.current);

        busMarkersRef.current[bus.id] = marker;
      });
    }

    if (mapMode === "parking") {
      parkings.forEach((parking) => {
        const markerLabel =
          parking.availableSpaces === null
            ? "P"
            : `P ${parking.availableSpaces}`;

        const markerElement = createMarkerElement({
          label: markerLabel,
          background: "#2f9e44",
        });

        const marker = new maplibregl.Marker({
          element: markerElement,
        })
          .setLngLat([parking.lng, parking.lat])
          .setPopup(createParkingPopup(parking))
          .addTo(mapRef.current);

        parkingMarkersRef.current[parking.id] = marker;
      });
    }

    if (mapMode === "bikes") {
      bikeStations.forEach((station) => {
        const markerLabel =
          station.availableBikes === null
            ? "R"
            : `R ${station.availableBikes}`;

        const markerElement = createMarkerElement({
          label: markerLabel,
          background: "#ae3ec9",
        });

        const marker = new maplibregl.Marker({
          element: markerElement,
        })
          .setLngLat([station.lng, station.lat])
          .setPopup(createBikePopup(station))
          .addTo(mapRef.current);

        bikeMarkersRef.current[station.id] = marker;
      });
    }
  }, [mapMode, buses, parkings, bikeStations]);

  return (
    <div
      ref={mapContainerRef}
      style={{
        width: "100%",
        height: "100vh",
      }}
    />
  );
}