import { useEffect, useState } from "react";
import {
  Alert,
  Badge,
  Button,
  Card,
  Center,
  Divider,
  Group,
  Loader,
  Modal,
  Stack,
  Text,
  Title,
} from "@mantine/core";
import { QRCodeSVG } from "qrcode.react";
import { activateTicket, getTickets } from "../services/ticketService";

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

  return "Nieznany";
}

function getTicketStatusColor(status) {
  if (status === "inactive") {
    return "yellow";
  }

  if (status === "active") {
    return "green";
  }

  if (status === "expired") {
    return "gray";
  }

  return "blue";
}

function getCategoryLabel(category) {
  if (!category) {
    return "Bilet";
  }

  const normalizedCategory = category.toLowerCase();

  if (normalizedCategory === "normal") {
    return "Normalny";
  }

  if (normalizedCategory === "reduced") {
    return "Ulgowy";
  }

  return category;
}

function formatPrice(price) {
  const numericPrice = Number(price);

  if (!Number.isFinite(numericPrice)) {
    return "Brak ceny";
  }

  return `${numericPrice.toFixed(2)} zł`;
}

function formatDuration(durationMinutes) {
  const numericDuration = Number(durationMinutes);

  if (!Number.isFinite(numericDuration)) {
    return "Brak czasu ważności";
  }

  if (numericDuration < 60) {
    return `${numericDuration} min`;
  }

  if (numericDuration % 1440 === 0) {
    const days = numericDuration / 1440;
    return days === 1 ? "24 godz." : `${days} dni`;
  }

  if (numericDuration % 60 === 0) {
    return `${numericDuration / 60} godz.`;
  }

  return `${numericDuration} min`;
}

function formatDate(dateValue) {
  if (!dateValue) {
    return "—";
  }

  const date = new Date(dateValue);

  if (Number.isNaN(date.getTime())) {
    return "—";
  }

  return date.toLocaleString("pl-PL", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function getRemainingTime(validTo) {
  if (!validTo) {
    return "";
  }

  const validToDate = new Date(validTo);
  const remainingMilliseconds = validToDate.getTime() - Date.now();

  if (!Number.isFinite(remainingMilliseconds) || remainingMilliseconds <= 0) {
    return "Bilet wygasł.";
  }

  const remainingMinutes = Math.ceil(remainingMilliseconds / 60000);

  if (remainingMinutes < 60) {
    return `Pozostało: ${remainingMinutes} min`;
  }

  const hours = Math.floor(remainingMinutes / 60);
  const minutes = remainingMinutes % 60;

  if (minutes === 0) {
    return `Pozostało: ${hours} godz.`;
  }

  return `Pozostało: ${hours} godz. ${minutes} min`;
}

export default function MyTicketsModal({ opened, onClose, refreshKey }) {
  const [tickets, setTickets] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [activatingTicketUuid, setActivatingTicketUuid] = useState("");
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  useEffect(() => {
    if (!opened) {
      return;
    }

    let isActive = true;

    async function loadTickets(showLoader) {
      if (showLoader) {
        setIsLoading(true);
      }

      setError("");

      try {
        const loadedTickets = await getTickets();

        if (isActive) {
          setTickets(loadedTickets);
        }
      } catch (loadError) {
        if (isActive) {
          setError(loadError.message);
        }
      } finally {
        if (isActive && showLoader) {
          setIsLoading(false);
        }
      }
    }

    setMessage("");
    loadTickets(true);

    const intervalId = setInterval(() => {
      loadTickets(false);
    }, 10000);

    return () => {
      isActive = false;
      clearInterval(intervalId);
    };
  }, [opened, refreshKey]);

  async function handleActivateTicket(ticketUuid) {
    setActivatingTicketUuid(ticketUuid);
    setError("");
    setMessage("");

    try {
      const activatedTicket = await activateTicket(ticketUuid);

      setTickets((currentTickets) =>
        currentTickets.map((ticket) =>
          ticket.uuid === activatedTicket.uuid ? activatedTicket : ticket,
        ),
      );

      setMessage("Bilet został skasowany i jest aktywny.");
    } catch (activationError) {
      setError(activationError.message);
    } finally {
      setActivatingTicketUuid("");
    }
  }

  return (
    <Modal opened={opened} onClose={onClose} title="Moje bilety" size="xl" centered>
      <Stack gap="md">
        {error && (
          <Alert color="red" variant="light">
            {error}
          </Alert>
        )}

        {message && (
          <Alert color="green" variant="light">
            {message}
          </Alert>
        )}

        {isLoading ? (
          <Group justify="center" py="xl">
            <Loader />
          </Group>
        ) : tickets.length === 0 ? (
          <Alert color="blue" variant="light">
            Nie masz jeszcze żadnych biletów.
          </Alert>
        ) : (
          tickets.map((ticket) => (
            <Card key={ticket.uuid} withBorder radius="md" shadow="xs">
              <Stack gap="sm">
                <Group justify="space-between" align="flex-start">
                  <Stack gap={2}>
                    <Title order={4}>{ticket.name}</Title>
                    <Text size="sm" c="dimmed">
                      {getCategoryLabel(ticket.ticketCategory)} •{" "}
                      {formatDuration(ticket.durationMinutes)} •{" "}
                      {formatPrice(ticket.price)}
                    </Text>
                  </Stack>

                  <Badge color={getTicketStatusColor(ticket.status)} variant="light">
                    {getTicketStatusLabel(ticket.status)}
                  </Badge>
                </Group>

                <Divider />

                <Stack gap={4}>
                  <Text size="sm">
                    <strong>Kupiony:</strong> {formatDate(ticket.purchasedAt)}
                  </Text>

                  <Text size="sm">
                    <strong>Ważny od:</strong> {formatDate(ticket.validFrom)}
                  </Text>

                  <Text size="sm">
                    <strong>Ważny do:</strong> {formatDate(ticket.validTo)}
                  </Text>

                  <Text size="sm" c="dimmed">
                    ID biletu: {ticket.uuid}
                  </Text>
                </Stack>

                {ticket.status === "active" && (
                  <Alert color="green" variant="light">
                    {getRemainingTime(ticket.validTo)}
                  </Alert>
                )}

                {ticket.status === "inactive" && (
                  <Alert color="yellow" variant="light">
                    Bilet nie jest jeszcze skasowany. Aktywuj go przed wejściem do
                    autobusu.
                  </Alert>
                )}

                {ticket.status === "expired" && (
                  <Alert color="gray" variant="light">
                    Ten bilet wygasł.
                  </Alert>
                )}

                {ticket.status === "active" && ticket.qrCode && (
                  <Center py="md">
                    <Stack align="center" gap="xs">
                      <QRCodeSVG value={ticket.qrCode} size={180} />
                      <Text size="xs" c="dimmed" ta="center">
                        Kod QR biletu
                      </Text>
                    </Stack>
                  </Center>
                )}

                {ticket.status === "inactive" && (
                  <Group justify="flex-end">
                    <Button
                      onClick={() => handleActivateTicket(ticket.uuid)}
                      loading={activatingTicketUuid === ticket.uuid}
                    >
                      Skasuj bilet
                    </Button>
                  </Group>
                )}
              </Stack>
            </Card>
          ))
        )}
      </Stack>
    </Modal>
  );
}