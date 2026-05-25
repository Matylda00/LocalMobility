import { useEffect, useRef, useState } from "react";
import maplibregl from "maplibre-gl";
import "maplibre-gl/dist/maplibre-gl.css";

const BUSES = [
  {
    id: "bus-1",
    line: "152",
    lat: 50.0647,
    lng: 19.945,
  },
  {
    id: "bus-2",
    line: "179",
    lat: 50.0675,
    lng: 19.923,
  },
  {
    id: "bus-3",
    line: "501",
    lat: 50.061,
    lng: 19.958,
  },
];

const PARKINGS = [
  {
    id: "parking-1",
    name: "Parking Galeria Krakowska",
    lat: 50.0679,
    lng: 19.9468,
  },
  {
    id: "parking-2",
    name: "Parking Wawel",
    lat: 50.054,
    lng: 19.935,
  },
  {
    id: "parking-3",
    name: "Parking Kazimierz",
    lat: 50.0495,
    lng: 19.944,
  },
];

const BIKES = [
  {
    id: "bike-1",
    name: "Stacja Rynek Główny",
    lat: 50.0617,
    lng: 19.9373,
  },
  {
    id: "bike-2",
    name: "Stacja Dworzec Główny",
    lat: 50.0675,
    lng: 19.947,
  },
  {
    id: "bike-3",
    name: "Stacja Kazimierz",
    lat: 50.051,
    lng: 19.9445,
  },
];

function getUpdatedBuses(previousBuses) {
  return previousBuses.map((bus) => ({
    ...bus,
    lat: bus.lat + (Math.random() - 0.5) * 0.002,
    lng: bus.lng + (Math.random() - 0.5) * 0.002,
  }));
}

function getUpdatedBikes(previousBikes) {
  return previousBikes
    .map((bike) => (bike))
}

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

export default function MapView({ mapMode }) {
  const mapContainerRef = useRef(null);
  const mapRef = useRef(null);

  const busMarkersRef = useRef({});
  const parkingMarkersRef = useRef({});
  const bikeMarkersRef = useRef({});

  const [buses, setBuses] = useState(BUSES);
  const [parkings, setParkings] = useState([]);
  const [bikes, setBikes] = useState(BIKES);

  useEffect(() => {
    if (mapRef.current) return;

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
      center: [19.94498, 50.06465],
      zoom: 12,
    });

    mapRef.current.addControl(new maplibregl.NavigationControl(), "top-right");
  }, []);

  useEffect(() => {
    if (mapMode !== "parking") return;

    setParkings(PARKINGS);
  }, [mapMode]);

  useEffect(() => {
    if (mapMode !== "buses") return;

    const intervalId = setInterval(() => {
      setBuses((currentBuses) => getUpdatedBuses(currentBuses));
    }, 5000);

    return () => {
      clearInterval(intervalId);
    };
  }, [mapMode]);

  useEffect(() => {
    if (mapMode !== "bikes") return;

    const intervalId = setInterval(() => {
      setBikes((currentBikes) => getUpdatedBikes(currentBikes));
    }, 5000);

    return () => {
      clearInterval(intervalId);
    };
  }, [mapMode]);

  useEffect(() => {
    if (!mapRef.current) return;

    Object.values(busMarkersRef.current).forEach((marker) => marker.remove());
    Object.values(parkingMarkersRef.current).forEach((marker) =>
      marker.remove()
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
          .addTo(mapRef.current);

        busMarkersRef.current[bus.id] = marker;
      });
    }

    if (mapMode === "parking") {
      parkings.forEach((parking) => {
        const markerElement = createMarkerElement({
          label: `P `,
          background: "#2f9e44",
        });

   
        const marker = new maplibregl.Marker({
          element: markerElement,
        })
          .setLngLat([parking.lng, parking.lat])
          .addTo(mapRef.current);

        parkingMarkersRef.current[parking.id] = marker;
      });
    }

    if (mapMode === "bikes") {
      bikes.forEach((bike) => {
        const markerElement = createMarkerElement({
          label: `🚲`,
          background: "#ae3ec9",
        });


        const marker = new maplibregl.Marker({
          element: markerElement,
        })
          .setLngLat([bike.lng, bike.lat])
          .addTo(mapRef.current);

        bikeMarkersRef.current[bike.id] = marker;
      });
    }
  }, [mapMode, buses, parkings, bikes]);

  return (
    <div
      ref={mapContainerRef}
      style={{ width: "100%", height: "100vh" }}
    />
  );
}