import { useState } from "react";
import {
  AppShell,
  ActionIcon,
  Button,
  Group,
  SegmentedControl,
  Stack,
  Text,
  TextInput,
  Title,
} from "@mantine/core";
import MapView from "./components/MapView";
export default function App() {
  const [lineNumber, setLineNumber] = useState("");
  const [mapMode, setMapMode] = useState("buses");

  function handleSearch() {
    console.log("Szukana linia:", lineNumber);
  }

  function handleBuyTicket() {
    console.log("Kup bilet");
  }

  function handleLogout() {
    console.log("Wyloguj się");
  }

  return (
    <AppShell navbar={{ width: 360, breakpoint: "sm" }} padding={0}>
      <AppShell.Navbar p="md">
        <Stack h="100%" justify="space-between">
          <Stack gap="lg">
            <div>
              <Title order={2}>Bus App</Title>
              <Text size="sm" c="dimmed">
                Transport miejski w czasie rzeczywistym
              </Text>
            </div>

            <Group gap="xs" wrap="nowrap">
              <TextInput
                placeholder="Numer linii"
                value={lineNumber}
                onChange={(event) => setLineNumber(event.currentTarget.value)}
                size="md"
                radius="md"
                style={{ flex: 1 }}
                styles={{
                  input: {
                    backgroundColor: "white",
                    color: "black",
                  },
                }}
              />

              <ActionIcon
                size={42}
                radius="md"
                variant="filled"
                aria-label="Szukaj"
                onClick={handleSearch}
              >
                🔍
              </ActionIcon>
            </Group>

            <Button size="md" fullWidth onClick={handleBuyTicket}>
              Kup bilet
            </Button>

            <Stack gap="xs">
              <Text size="sm" fw={600}>
                Tryb mapy
              </Text>

              <SegmentedControl
                value={mapMode}
                onChange={setMapMode}
                fullWidth
                data={[
                  { label: "Autobusy", value: "buses" },
                  { label: "Parkingi", value: "parking" },
                  { label: "Rowery", value: "bikes" },
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
            </Stack>
          </Stack>

          <Button variant="subtle" color="red" fullWidth onClick={handleLogout}>
            Wyloguj się
          </Button>
        </Stack>
      </AppShell.Navbar>

      <AppShell.Main>
        <MapView mapMode={mapMode} />
      </AppShell.Main>
    </AppShell>
  );
}