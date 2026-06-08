import { useEffect, useState } from "react";
import {
  Alert,
  Badge,
  Button,
  Card,
  Center,
  Divider,
  Group,
  Modal,
  Stack,
  Text,
  Title,
} from "@mantine/core";
import { QRCodeSVG } from "qrcode.react";
import {
  activateTicket,
  getTickets,
  removeTicket,
} from "../services/ticketService";

function getTicketStatusLabel(status) {
  if (status === "inactive") {
    return "Nieskasowany";
  }

  if (status === "active") {
    return "Aktywny";
  }

  if (status === "expired") {
    return "Wygasły";
  }

  return status;
}

function getTicketStatusColor(status) {
  if (status === "inactive") {
    return "gray";
  }

  if (status === "active") {
    return "green";
  }

  if (status === "expired") {
    return "red";
  }

  return "blue";
}

function formatDateTime(value) {
  if (!value) {
    return "—";
  }

  return new Date(value).toLocaleString("pl-PL", {
    dateStyle: "short",
    timeStyle: "short",
  });
}

function getRemainingTimeText(ticket) {
  if (ticket.status !== "active") {
    return "";
  }

  const diffMs = new Date(ticket.validUntil).getTime() - Date.now();

  if (diffMs <= 0) {
    return "Bilet wygasł";
  }

  const diffMinutes = Math.floor(diffMs / 1000 / 60);
  const diffSeconds = Math.floor((diffMs / 1000) % 60);

  return `${diffMinutes} min ${diffSeconds.toString().padStart(2, "0")} s`;
}

export default function MyTicketsModal({ opened, onClose, refreshKey }) {
  const [tickets, setTickets] = useState([]);
  const [isLoadingTickets, setIsLoadingTickets] = useState(false);
  const [ticketsError, setTicketsError] = useState("");

  async function refreshTickets() {
    setTicketsError("");
    setIsLoadingTickets(true);

    try {
      const loadedTickets = await getTickets();
      setTickets(loadedTickets);
    } catch {
      setTicketsError("Nie udało się pobrać biletów.");
    } finally {
      setIsLoadingTickets(false);
    }
  }

  async function handleActivateTicket(ticketId) {
    setTicketsError("");

    try {
      const updatedTickets = await activateTicket(ticketId);
      setTickets(updatedTickets);
    } catch {
      setTicketsError("Nie udało się skasować biletu.");
    }
  }

  async function handleRemoveTicket(ticketId) {
    setTicketsError("");

    try {
      const updatedTickets = await removeTicket(ticketId);
      setTickets(updatedTickets);
    } catch {
      setTicketsError("Nie udało się usunąć biletu.");
    }
  }

  useEffect(() => {
    if (!opened) {
      return;
    }

    refreshTickets();

    const intervalId = setInterval(() => {
      refreshTickets();
    }, 1000);

    return () => {
      clearInterval(intervalId);
    };
  }, [opened, refreshKey]);

  return (
    <Modal opened={opened} onClose={onClose} title="Moje bilety" size="lg" centered>
      <Stack gap="md">
        {ticketsError && (
          <Alert color="red" variant="light">
            {ticketsError}
          </Alert>
        )}

        {isLoadingTickets && tickets.length === 0 && (
          <Alert color="blue" variant="light">
            Ładowanie biletów...
          </Alert>
        )}

        {!isLoadingTickets && tickets.length === 0 && (
          <Alert color="gray" variant="light">
            Nie masz jeszcze żadnych biletów. Kliknij „Kup bilet”, żeby kupić
            pierwszy bilet.
          </Alert>
        )}

        {tickets.map((ticket) => {
          const qrValue = `LOCALMOBILITY:TICKET:${ticket.id}`;

          return (
            <Card key={ticket.id} withBorder radius="md" padding="md">
              <Stack gap="sm">
                <Group justify="space-between" align="flex-start">
                  <div>
                    <Title order={4}>{ticket.name}</Title>
                    <Text size="sm" c="dimmed">
                      Kupiono: {formatDateTime(ticket.purchasedAt)}
                    </Text>
                  </div>

                  <Badge color={getTicketStatusColor(ticket.status)}>
                    {getTicketStatusLabel(ticket.status)}
                  </Badge>
                </Group>

                <Divider />

                <Group justify="space-between">
                  <Text>Cena:</Text>
                  <Text fw={700}>
                    {ticket.price.toFixed(2)} {ticket.currency}
                  </Text>
                </Group>

                <Group justify="space-between">
                  <Text>Ważny od:</Text>
                  <Text>{formatDateTime(ticket.validFrom)}</Text>
                </Group>

                <Group justify="space-between">
                  <Text>Ważny do:</Text>
                  <Text>{formatDateTime(ticket.validUntil)}</Text>
                </Group>

                {ticket.status === "active" && (
                  <>
                    <Alert color="green" variant="light">
                      Pozostało: <b>{getRemainingTimeText(ticket)}</b>
                    </Alert>

                    <Center>
                      <Stack align="center" gap="xs">
                        <QRCodeSVG value={qrValue} size={180} />
                        <Text size="xs" c="dimmed">
                          {qrValue}
                        </Text>
                      </Stack>
                    </Center>
                  </>
                )}

                {ticket.status === "inactive" && (
                  <Alert color="yellow" variant="light">
                    Ten bilet jest kupiony, ale jeszcze nieskasowany. Po
                    skasowaniu zacznie się jego czas ważności.
                  </Alert>
                )}

                {ticket.status === "expired" && (
                  <Alert color="red" variant="light">
                    Ten bilet wygasł i nie może być użyty do przejazdu.
                  </Alert>
                )}

                <Group justify="flex-end">
                  {ticket.status === "inactive" && (
                    <Button onClick={() => handleActivateTicket(ticket.id)}>
                      Skasuj bilet
                    </Button>
                  )}

                  <Button
                    color="red"
                    variant="light"
                    onClick={() => handleRemoveTicket(ticket.id)}
                  >
                    Usuń
                  </Button>
                </Group>
              </Stack>
            </Card>
          );
        })}
      </Stack>
    </Modal>
  );
}