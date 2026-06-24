import { useEffect, useMemo, useState } from "react";
import {
  Alert,
  Badge,
  Button,
  Card,
  Group,
  Loader,
  Paper,
  Select,
  SimpleGrid,
  Stack,
  Text,
  TextInput,
  Title,
} from "@mantine/core";
import {
  getBusLines,
  getLineStops,
  getStopDepartures,
} from "../services/scheduleService";

function getTodayInputValue() {
  const now = new Date();
  const timezoneOffset = now.getTimezoneOffset() * 60 * 1000;
  return new Date(now.getTime() - timezoneOffset).toISOString().slice(0, 10);
}

function formatGtfsTime(time) {
  if (!time) return "—";
  return time.slice(0, 5);
}

export default function ScheduleView({ initialLineNumber = "" }) {
  const [date, setDate] = useState(getTodayInputValue());
  const [lineNumber, setLineNumber] = useState(initialLineNumber);
  const [direction, setDirection] = useState("default");

  const [busLines, setBusLines] = useState([]);
  const [stopsResponse, setStopsResponse] = useState(null);
  const [selectedStopId, setSelectedStopId] = useState(null);
  const [departuresResponse, setDeparturesResponse] = useState(null);

  const [isLoadingLines, setIsLoadingLines] = useState(false);
  const [isLoadingStops, setIsLoadingStops] = useState(false);
  const [isLoadingDepartures, setIsLoadingDepartures] = useState(false);
  const [error, setError] = useState("");

  const lineOptions = useMemo(
    () =>
      busLines.map((line) => ({
        value: line.lineNumber,
        label: line.name
          ? `${line.lineNumber} — ${line.name}`
          : line.lineNumber,
      })),
    [busLines]
  );

  useEffect(() => {
    if (initialLineNumber) {
      setLineNumber(initialLineNumber);
    }
  }, [initialLineNumber]);

  useEffect(() => {
    async function loadLines() {
      try {
        setIsLoadingLines(true);
        setError("");
        const lines = await getBusLines();
        setBusLines(lines);
      } catch (err) {
        setError(err.message || "Nie udało się pobrać listy linii.");
      } finally {
        setIsLoadingLines(false);
      }
    }

    loadLines();
  }, []);

  useEffect(() => {
    async function loadStops() {
      if (!lineNumber) {
        setStopsResponse(null);
        setSelectedStopId(null);
        setDeparturesResponse(null);
        return;
      }

      try {
        setIsLoadingStops(true);
        setError("");
        setSelectedStopId(null);
        setDeparturesResponse(null);

        const response = await getLineStops({
          lineNumber,
          direction,
          date,
        });

        setStopsResponse(response);
      } catch (err) {
        setStopsResponse(null);
        setError(err.message || "Nie udało się pobrać przystanków.");
      } finally {
        setIsLoadingStops(false);
      }
    }

    loadStops();
  }, [lineNumber, direction, date]);

  async function handleStopClick(stopId) {
    try {
      setSelectedStopId(stopId);
      setIsLoadingDepartures(true);
      setError("");

      const response = await getStopDepartures({
        lineNumber,
        stopId,
        direction,
        date,
      });

      setDeparturesResponse(response);
    } catch (err) {
      setDeparturesResponse(null);
      setError(err.message || "Nie udało się pobrać odjazdów.");
    } finally {
      setIsLoadingDepartures(false);
    }
  }

  function handleReverseDirection() {
    setDirection((currentDirection) =>
      currentDirection === "default" ? "opposite" : "default"
    );
  }

  return (
    <Stack gap="md">
      <Group justify="space-between" align="flex-end">
        <div>
          <Title order={2}>Rozkład jazdy</Title>
          <Text c="dimmed">
            Wybierz dzień i linię, a potem kliknij przystanek, żeby zobaczyć
            odjazdy.
          </Text>
        </div>

        <Button variant="light" onClick={handleReverseDirection}>
          Trasa odwrotna
        </Button>
      </Group>

      {error && (
        <Alert color="red" title="Błąd">
          {error}
        </Alert>
      )}

      <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="md">
        <TextInput
          label="Dzień"
          type="date"
          value={date}
          onChange={(event) => setDate(event.currentTarget.value)}
        />

        <Select
          label="Linia"
          placeholder={isLoadingLines ? "Ładowanie..." : "Wybierz linię"}
          searchable
          clearable
          data={lineOptions}
          value={lineNumber || null}
          onChange={(value) => setLineNumber(value ?? "")}
          rightSection={isLoadingLines ? <Loader size="xs" /> : null}
        />
      </SimpleGrid>

      {stopsResponse && (
        <Paper withBorder radius="md" p="md">
          <Group justify="space-between">
            <Stack gap={2}>
              <Text fw={700}>Linia {stopsResponse.lineNumber}</Text>
              <Text size="sm" c="dimmed">
                Kierunek: {stopsResponse.headsign || "brak nazwy kierunku"}
              </Text>
            </Stack>

            <Badge variant="light">
              kierunek {stopsResponse.directionId}
            </Badge>
          </Group>
        </Paper>
      )}

      <SimpleGrid cols={{ base: 1, md: 2 }} spacing="md">
        <Card withBorder radius="md">
          <Stack gap="sm">
            <Group justify="space-between">
              <Title order={3}>Przystanki</Title>
              {isLoadingStops && <Loader size="sm" />}
            </Group>

            {!lineNumber && (
              <Text c="dimmed">Najpierw wybierz linię autobusową.</Text>
            )}

            {lineNumber && !isLoadingStops && !stopsResponse?.stops?.length && (
              <Text c="dimmed">Brak przystanków dla wybranej linii.</Text>
            )}

            {stopsResponse?.stops?.map((stop) => (
              <Button
                key={stop.id}
                variant={selectedStopId === stop.id ? "filled" : "light"}
                onClick={() => handleStopClick(stop.id)}
                fullWidth
                style={{
                  height: "auto",
                  justifyContent: "flex-start",
                  padding: "10px 12px",
                }}
              >
                <Stack gap={2} align="flex-start">
                  <Text fw={700}>
                    {stop.stopSequence}. {stop.name}
                  </Text>
                  <Text size="xs">
                    {stop.stopCode ? `Kod: ${stop.stopCode}` : "Brak kodu"}
                    {stop.platformCode
                      ? ` · Peron: ${stop.platformCode}`
                      : ""}
                  </Text>
                </Stack>
              </Button>
            ))}
          </Stack>
        </Card>

        <Card withBorder radius="md">
          <Stack gap="sm">
            <Group justify="space-between">
              <Title order={3}>Odjazdy</Title>
              {isLoadingDepartures && <Loader size="sm" />}
            </Group>

            {!selectedStopId && (
              <Text c="dimmed">Kliknij przystanek, żeby zobaczyć rozkład.</Text>
            )}

            {departuresResponse && (
              <Paper withBorder radius="md" p="sm">
                <Text fw={700}>{departuresResponse.stopName}</Text>
                <Text size="sm" c="dimmed">
                  Data: {departuresResponse.date}
                  {departuresResponse.platformCode
                    ? ` · Peron: ${departuresResponse.platformCode}`
                    : ""}
                </Text>
              </Paper>
            )}

            {departuresResponse?.departures?.map((departure) => (
              <Paper
                key={`${departure.tripId}-${departure.departureSeconds}`}
                withBorder
                radius="md"
                p="sm"
              >
                <Group justify="space-between">
                  <Text fw={800} size="lg">
                    {formatGtfsTime(departure.departureTime)}
                  </Text>

                  <Stack gap={0} align="flex-end">
                    <Text size="sm">
                      kierunek: {departure.headsign || "—"}
                    </Text>
                    <Text size="xs" c="dimmed">
                      kurs: {departure.tripExternalId || departure.tripId}
                    </Text>
                  </Stack>
                </Group>
              </Paper>
            ))}

            {selectedStopId &&
              !isLoadingDepartures &&
              departuresResponse &&
              departuresResponse.departures.length === 0 && (
                <Text c="dimmed">Brak odjazdów dla tego przystanku.</Text>
              )}
          </Stack>
        </Card>
      </SimpleGrid>
    </Stack>
  );
}