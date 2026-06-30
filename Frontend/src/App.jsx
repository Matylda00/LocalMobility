import { useState } from "react";
import {
  AppShell,
  Button,
  Divider,
  Group,
  SegmentedControl,
  Stack,
  Text,
  TextInput,
  Title,
} from "@mantine/core";
import MapView from "./components/MapView";
import MyTicketsModal from "./components/MyTicketsModal";
import ScheduleView from "./components/ScheduleView";
import TicketPurchaseModal from "./components/TicketPurchaseModal";
import AuthView from "./components/AuthView";
import { clearAuth, getStoredAuth } from "./services/authService";
import AdminEmailsButton from "./components/AdminEmailsButton";
export default function App() {
  const [auth, setAuth] = useState(() => getStoredAuth());

  const [lineNumber, setLineNumber] = useState("");
  const [mainView, setMainView] = useState("map");
  const [mapMode, setMapMode] = useState("buses");
  const [scheduleInitialLine, setScheduleInitialLine] = useState("");
  const [isTicketPurchaseOpen, setIsTicketPurchaseOpen] = useState(false);
  const [isMyTicketsOpen, setIsMyTicketsOpen] = useState(false);
  const [ticketsRefreshKey, setTicketsRefreshKey] = useState(0);

  function handleSearch() {
    const cleanedLineNumber = lineNumber.trim();

    if (!cleanedLineNumber) {
      return;
    }

    setScheduleInitialLine(cleanedLineNumber);
    setMainView("schedule");
  }

  function handleTicketBought() {
    setTicketsRefreshKey((currentKey) => currentKey + 1);
  }

  function handleLogout() {
    clearAuth();
    setAuth(null);
    setIsTicketPurchaseOpen(false);
    setIsMyTicketsOpen(false);
  }

  if (!auth) {
    return <AuthView onAuthenticated={setAuth} />;
  }

  return (
    <>
      <AppShell
        padding="md"
        header={{ height: 72 }}
        navbar={{
          width: 320,
          breakpoint: "sm",
        }}
      >
        <AppShell.Header>
          <Group h="100%" px="md" justify="space-between">
            <div>
              <Title order={3}>LocalMobility</Title>
              <Text size="sm" c="dimmed">
                Transport miejski w czasie rzeczywistym
              </Text>
            </div>

            <Group gap="sm">
              <Text size="sm" c="dimmed">
                Zalogowano jako <strong>{auth.email}</strong>
              </Text>

              <Button variant="subtle" color="red" onClick={handleLogout}>
                Wyloguj się
              </Button>
            </Group>
          </Group>
        </AppShell.Header>

        <AppShell.Navbar p="md">
          <Stack gap="md">
            <Stack gap={4}>
              <Title order={4}>Widok</Title>
              <Text size="sm" c="dimmed">
                Przełącz mapę albo rozkład jazdy.
              </Text>
            </Stack>

            <SegmentedControl
              fullWidth
              value={mainView}
              onChange={setMainView}
              data={[
                { value: "map", label: "Mapa" },
                { value: "schedule", label: "Rozkład" },
              ]}
            />

            <Divider />

            <Stack gap={4}>
              <Title order={4}>Bilety</Title>
              <Text size="sm" c="dimmed">
                Kup bilet albo sprawdź swoje aktywne bilety.
              </Text>
            </Stack>

            <Button onClick={() => setIsTicketPurchaseOpen(true)}>
              Kup bilet
            </Button>

            <Button
              variant="light"
              onClick={() => setIsMyTicketsOpen(true)}
            >
              Moje bilety
            </Button>
            <AdminEmailsButton />
            <Divider />

            <Stack gap={4}>
              <Title order={4}>Wyszukaj linię</Title>
              <Text size="sm" c="dimmed">
                Wpisz numer linii autobusowej i otwórz jej rozkład.
              </Text>
            </Stack>

            <Group align="end" gap="xs">
              <TextInput
                label="Numer linii"
                placeholder="np. 175"
                value={lineNumber}
                onChange={(event) => setLineNumber(event.currentTarget.value)}
                size="md"
                radius="md"
                style={{ flex: 1 }}
              />

              <Button onClick={handleSearch}>Szukaj</Button>
            </Group>

            {mainView === "map" && (
              <>
                <Divider />

                <Stack gap={4}>
                  <Title order={4}>Tryb mapy</Title>
                  <Text size="sm" c="dimmed">
                    Wybierz, co chcesz zobaczyć na mapie.
                  </Text>
                </Stack>

                <SegmentedControl
                  fullWidth
                  value={mapMode}
                  onChange={setMapMode}
                  data={[
                    { value: "buses", label: "Autobusy" },
                    { value: "parking", label: "Parkingi" },
                    { value: "bikes", label: "Rowery" },
                  ]}
                />

                <Text size="sm" c="dimmed">
                  Wybrano:{" "}
                  {mapMode === "buses"
                    ? "autobusy"
                    : mapMode === "parking"
                      ? "parkingi"
                      : "rowery"}
                </Text>
              </>
            )}
          </Stack>
        </AppShell.Navbar>

        <AppShell.Main>
          {mainView === "map" ? (
            <MapView mapMode={mapMode} />
          ) : (
            <ScheduleView initialLineNumber={scheduleInitialLine} />
          )}
        </AppShell.Main>
      </AppShell>

      <TicketPurchaseModal
        opened={isTicketPurchaseOpen}
        onClose={() => setIsTicketPurchaseOpen(false)}
        onTicketBought={handleTicketBought}
      />

      <MyTicketsModal
        opened={isMyTicketsOpen}
        onClose={() => setIsMyTicketsOpen(false)}
        refreshKey={ticketsRefreshKey}
      />
    </>
  );
}