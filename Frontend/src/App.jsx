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
import TicketPurchaseModal from "./components/TicketPurchaseModal";
import MyTicketsModal from "./components/MyTicketsModal";

export default function App() {
  const [lineNumber, setLineNumber] = useState("");
  const [mapMode, setMapMode] = useState("buses");
  const [isTicketPurchaseOpen, setIsTicketPurchaseOpen] = useState(false);
  const [isMyTicketsOpen, setIsMyTicketsOpen] = useState(false);
  const [ticketsRefreshKey, setTicketsRefreshKey] = useState(0);

  function handleSearch() {
    console.log("Szukana linia:", lineNumber);
  }

  function handleTicketBought() {
    setTicketsRefreshKey((currentKey) => currentKey + 1);
  }

  function handleLogout() {
    console.log("Wyloguj się");
  }

  return (
    <>
      <AppShell
        header={{ height: 72 }}
        navbar={{ width: 300, breakpoint: "sm" }}
        padding="md"
      >
        <AppShell.Header>
          <Group h="100%" px="md" justify="space-between">
            <div>
              <Title order={2}>Bus App</Title>
              <Text size="sm" c="dimmed">
                Transport miejski w czasie rzeczywistym
              </Text>
            </div>
          </Group>
        </AppShell.Header>

        <AppShell.Navbar p="md">
          <Stack gap="md" h="100%">
            <div>
              <Title order={4}>Bilety</Title>
              <Text size="sm" c="dimmed">
                Kup bilet albo sprawdź swoje aktywne bilety.
              </Text>
            </div>

            <Button fullWidth onClick={() => setIsTicketPurchaseOpen(true)}>
              Kup bilet
            </Button>

            <Button
              fullWidth
              variant="light"
              onClick={() => setIsMyTicketsOpen(true)}
            >
              Moje bilety
            </Button>

            <Divider />

            <div>
              <Title order={4}>Wyszukaj linię</Title>
              <Text size="sm" c="dimmed">
                Wpisz numer linii autobusowej.
              </Text>
            </div>

            <Group gap="sm" align="flex-end">
              <TextInput
                label="Numer linii"
                placeholder="np. 152"
                value={lineNumber}
                onChange={(event) => setLineNumber(event.currentTarget.value)}
                size="md"
                radius="md"
                style={{ flex: 1 }}
              />

              <Button onClick={handleSearch}>Szukaj</Button>
            </Group>

            <Divider />

            <div>
              <Title order={4}>Tryb mapy</Title>
              <Text size="sm" c="dimmed">
                Wybierz, co chcesz zobaczyć na mapie.
              </Text>
            </div>

            <SegmentedControl
              value={mapMode}
              onChange={setMapMode}
              data={[
                { label: "Autobusy", value: "buses" },
                { label: "Parkingi", value: "parking" },
                { label: "Rowery", value: "bikes" },
              ]}
              orientation="vertical"
              fullWidth
            />

            <Text size="sm">
              Wybrano:{" "}
              <b>
                {mapMode === "buses"
                  ? "autobusy"
                  : mapMode === "parking"
                  ? "parkingi"
                  : "rowery"}
              </b>
            </Text>

            <Button
              color="red"
              variant="light"
              mt="auto"
              onClick={handleLogout}
            >
              Wyloguj się
            </Button>
          </Stack>
        </AppShell.Navbar>

        <AppShell.Main>
          <MapView mapMode={mapMode} />
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