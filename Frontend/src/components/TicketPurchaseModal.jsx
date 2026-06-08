import { useEffect, useState } from "react";
import {
  Alert,
  Badge,
  Button,
  Card,
  Divider,
  Group,
  Modal,
  SimpleGrid,
  Stack,
  Text,
  TextInput,
  Title,
} from "@mantine/core";
import { processPayment } from "../services/paymentService";
import { createTicket, getTicketTypes } from "../services/ticketService";

export default function TicketPurchaseModal({ opened, onClose, onTicketBought }) {
  const [ticketTypes, setTicketTypes] = useState([]);
  const [selectedTicket, setSelectedTicket] = useState(null);

  const [cardNumber, setCardNumber] = useState("4242 4242 4242 4242");
  const [cardOwner, setCardOwner] = useState("Jan Kowalski");
  const [expiryDate, setExpiryDate] = useState("12/30");
  const [cvv, setCvv] = useState("123");

  const [isLoadingTicketTypes, setIsLoadingTicketTypes] = useState(false);
  const [isProcessing, setIsProcessing] = useState(false);

  const [paymentError, setPaymentError] = useState("");
  const [successMessage, setSuccessMessage] = useState("");

  function resetPaymentState() {
    setPaymentError("");
    setSuccessMessage("");
    setIsProcessing(false);
  }

  function handleClose() {
    resetPaymentState();
    onClose();
  }

  async function loadTicketTypes() {
    setIsLoadingTicketTypes(true);

    try {
      const loadedTicketTypes = await getTicketTypes();

      setTicketTypes(loadedTicketTypes);

      if (loadedTicketTypes.length > 0) {
        setSelectedTicket(loadedTicketTypes[0]);
      }
    } catch {
      setPaymentError("Nie udało się pobrać typów biletów.");
    } finally {
      setIsLoadingTicketTypes(false);
    }
  }

  async function handlePaymentSubmit(event) {
    event.preventDefault();

    if (!selectedTicket) {
      setPaymentError("Wybierz bilet przed płatnością.");
      return;
    }

    resetPaymentState();
    setIsProcessing(true);

    try {
      const paymentResult = await processPayment({
        cardNumber,
        cardOwner,
        expiryDate,
        cvv,
        amount: selectedTicket.price,
        currency: "PLN",
        ticketTypeId: selectedTicket.id,
      });

      if (!paymentResult.success) {
        setPaymentError(paymentResult.message);
        return;
      }

      const ticket = await createTicket(selectedTicket, paymentResult);

      setSuccessMessage(
        `Kupiono bilet: ${ticket.name}. Znajdziesz go w sekcji "Moje bilety".`
      );

      onTicketBought();
    } catch {
      setPaymentError("Wystąpił błąd podczas płatności.");
    } finally {
      setIsProcessing(false);
    }
  }

  useEffect(() => {
    if (opened) {
      resetPaymentState();
      loadTicketTypes();
    }
  }, [opened]);

  return (
    <Modal
      opened={opened}
      onClose={handleClose}
      title="Kup bilet"
      size="lg"
      centered
    >
      <Stack gap="md">
        <div>
          <Title order={4}>Wybierz bilet</Title>
          <Text size="sm" c="dimmed">
            Bilet po zakupie będzie nieskasowany. Ważność zacznie się dopiero po
            kliknięciu przycisku „Skasuj bilet”.
          </Text>
        </div>

        {isLoadingTicketTypes && (
          <Alert color="blue" variant="light">
            Ładowanie typów biletów...
          </Alert>
        )}

        {!isLoadingTicketTypes && ticketTypes.length === 0 && (
          <Alert color="yellow" variant="light">
            Brak dostępnych typów biletów.
          </Alert>
        )}

        <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="sm">
          {ticketTypes.map((ticketType) => (
            <Card
              key={ticketType.id}
              withBorder
              radius="md"
              padding="md"
              style={{
                cursor: "pointer",
                borderColor:
                  selectedTicket?.id === ticketType.id ? "#228be6" : undefined,
              }}
              onClick={() => setSelectedTicket(ticketType)}
            >
              <Group justify="space-between" align="flex-start">
                <div>
                  <Text fw={700}>{ticketType.name}</Text>
                  <Text size="sm" c="dimmed">
                    Ważny przez {ticketType.durationMinutes} min
                  </Text>
                </div>

                <Badge variant="light">
                  {ticketType.price.toFixed(2)} PLN
                </Badge>
              </Group>
            </Card>
          ))}
        </SimpleGrid>

        <Divider />

        <form onSubmit={handlePaymentSubmit}>
          <Stack gap="sm">
            <Title order={4}>Płatność</Title>

            <Alert color="blue" variant="light">
              Test: karta <b>4242 4242 4242 4242</b> daje sukces, a{" "}
              <b>4000 0000 0000 0002</b> daje odrzucenie.
            </Alert>

            <TextInput
              label="Numer karty"
              value={cardNumber}
              onChange={(event) => setCardNumber(event.currentTarget.value)}
              placeholder="4242 4242 4242 4242"
              required
            />

            <TextInput
              label="Imię i nazwisko"
              value={cardOwner}
              onChange={(event) => setCardOwner(event.currentTarget.value)}
              required
            />

            <Group grow>
              <TextInput
                label="Data ważności"
                value={expiryDate}
                onChange={(event) => setExpiryDate(event.currentTarget.value)}
                placeholder="12/30"
                required
              />

              <TextInput
                label="CVV"
                value={cvv}
                onChange={(event) => setCvv(event.currentTarget.value)}
                placeholder="123"
                required
              />
            </Group>

            {paymentError && (
              <Alert color="red" variant="light">
                {paymentError}
              </Alert>
            )}

            {successMessage && (
              <Alert color="green" variant="light">
                {successMessage}
              </Alert>
            )}

            <Group justify="space-between" mt="sm">
              <Text fw={700}>
                Do zapłaty:{" "}
                {selectedTicket ? selectedTicket.price.toFixed(2) : "0.00"} PLN
              </Text>

              <Button
                type="submit"
                loading={isProcessing}
                disabled={!selectedTicket || isLoadingTicketTypes}
              >
                Zapłać
              </Button>
            </Group>
          </Stack>
        </form>
      </Stack>
    </Modal>
  );
}