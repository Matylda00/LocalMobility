import { useEffect, useMemo, useState } from "react";
import {
  Alert,
  Badge,
  Button,
  Card,
  Divider,
  Group,
  Loader,
  Modal,
  SimpleGrid,
  Stack,
  Text,
  TextInput,
  Title,
} from "@mantine/core";
import { getTicketTypes, purchaseTicket } from "../services/ticketService";

const INITIAL_PAYMENT_DATA = {
  cardHolder: "",
  cardNumber: "",
  expiryDate: "",
  cvv: "",
};

function getTicketTypeKey(ticketType) {
  return `${ticketType.name}__${ticketType.ticketCategory}`;
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

function normalizeCardNumber(cardNumber) {
  return cardNumber.replaceAll(" ", "").replaceAll("-", "");
}

function validatePaymentData(selectedTicketType, paymentData) {
  if (!selectedTicketType) {
    return "Wybierz typ biletu.";
  }

  if (!paymentData.cardHolder.trim()) {
    return "Podaj imię i nazwisko właściciela karty.";
  }

  const normalizedCardNumber = normalizeCardNumber(paymentData.cardNumber);

  if (!/^\d{12,19}$/.test(normalizedCardNumber)) {
    return "Numer karty powinien mieć od 12 do 19 cyfr.";
  }

  if (!/^(0[1-9]|1[0-2])\/(\d{2}|\d{4})$/.test(paymentData.expiryDate.trim())) {
    return "Data ważności powinna mieć format MM/YY albo MM/YYYY.";
  }

  if (!/^\d{3,4}$/.test(paymentData.cvv.trim())) {
    return "CVV powinno mieć 3 albo 4 cyfry.";
  }

  return "";
}

export default function TicketPurchaseModal({ opened, onClose, onTicketBought }) {
  const [ticketTypes, setTicketTypes] = useState([]);
  const [selectedTicketKey, setSelectedTicketKey] = useState("");
  const [paymentData, setPaymentData] = useState(INITIAL_PAYMENT_DATA);
  const [isLoadingTicketTypes, setIsLoadingTicketTypes] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [successMessage, setSuccessMessage] = useState("");

  const selectedTicketType = useMemo(
    () =>
      ticketTypes.find(
        (ticketType) => getTicketTypeKey(ticketType) === selectedTicketKey,
      ) ?? null,
    [ticketTypes, selectedTicketKey],
  );

  useEffect(() => {
    if (!opened) {
      return;
    }

    let isActive = true;

    async function loadTicketTypes() {
      setIsLoadingTicketTypes(true);
      setError("");
      setSuccessMessage("");

      try {
        const loadedTicketTypes = await getTicketTypes();

        if (!isActive) {
          return;
        }

        setTicketTypes(loadedTicketTypes);

        setSelectedTicketKey((currentSelectedKey) => {
          const currentStillExists = loadedTicketTypes.some(
            (ticketType) => getTicketTypeKey(ticketType) === currentSelectedKey,
          );

          if (currentStillExists) {
            return currentSelectedKey;
          }

          if (loadedTicketTypes.length === 0) {
            return "";
          }

          return getTicketTypeKey(loadedTicketTypes[0]);
        });
      } catch (loadError) {
        if (isActive) {
          setError(loadError.message);
        }
      } finally {
        if (isActive) {
          setIsLoadingTicketTypes(false);
        }
      }
    }

    loadTicketTypes();

    return () => {
      isActive = false;
    };
  }, [opened]);

  function updatePaymentField(fieldName, value) {
    setPaymentData((currentPaymentData) => ({
      ...currentPaymentData,
      [fieldName]: value,
    }));
  }

  function handleClose() {
    if (isSubmitting) {
      return;
    }

    setError("");
    setSuccessMessage("");
    onClose();
  }

  async function handleSubmit(event) {
    event.preventDefault();

    const validationError = validatePaymentData(selectedTicketType, paymentData);

    if (validationError) {
      setError(validationError);
      setSuccessMessage("");
      return;
    }

    setIsSubmitting(true);
    setError("");
    setSuccessMessage("");

    try {
      const createdTicket = await purchaseTicket(selectedTicketType, {
        ...paymentData,
        cardNumber: normalizeCardNumber(paymentData.cardNumber),
        expiryDate: paymentData.expiryDate.trim(),
        cvv: paymentData.cvv.trim(),
        cardHolder: paymentData.cardHolder.trim(),
      });

      setPaymentData(INITIAL_PAYMENT_DATA);
      setSuccessMessage("Bilet został kupiony. Znajdziesz go w zakładce „Moje bilety”.");

      if (onTicketBought) {
        onTicketBought(createdTicket);
      }
    } catch (purchaseError) {
      setError(purchaseError.message);
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <Modal
      opened={opened}
      onClose={handleClose}
      title="Kup bilet"
      size="xl"
      centered
    >
      <form onSubmit={handleSubmit}>
        <Stack gap="md">
          {error && (
            <Alert color="red" variant="light">
              {error}
            </Alert>
          )}

          {successMessage && (
            <Alert color="green" variant="light">
              {successMessage}
            </Alert>
          )}

          <Stack gap={4}>
            <Title order={4}>Wybierz bilet</Title>
            <Text size="sm" c="dimmed">
              Wybierz bilet, który chcesz kupić.
            </Text>
          </Stack>

          {isLoadingTicketTypes ? (
            <Group justify="center" py="xl">
              <Loader />
            </Group>
          ) : ticketTypes.length === 0 ? (
            <Alert color="yellow" variant="light">
              Nie ma teraz dostępnych biletów.
            </Alert>
          ) : (
            <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="sm">
              {ticketTypes.map((ticketType) => {
                const ticketKey = getTicketTypeKey(ticketType);
                const isSelected = ticketKey === selectedTicketKey;

                return (
                  <Card
                    key={ticketKey}
                    withBorder
                    radius="md"
                    shadow={isSelected ? "md" : "xs"}
                    onClick={() => setSelectedTicketKey(ticketKey)}
                    style={{
                      cursor: "pointer",
                      borderColor: isSelected
                        ? "var(--mantine-color-blue-6)"
                        : undefined,
                    }}
                  >
                    <Stack gap="xs">
                      <Group justify="space-between" align="flex-start">
                        <Title order={5}>{ticketType.name}</Title>

                        <Badge variant={isSelected ? "filled" : "light"}>
                          {getCategoryLabel(ticketType.ticketCategory)}
                        </Badge>
                      </Group>

                      <Text size="sm" c="dimmed">
                        Ważność: {formatDuration(ticketType.durationMinutes)}
                      </Text>

                      <Text fw={700}>{formatPrice(ticketType.price)}</Text>
                    </Stack>
                  </Card>
                );
              })}
            </SimpleGrid>
          )}

          <Divider />

          <Stack gap={4}>
            <Title order={4}>Dane karty</Title>
            <Text size="sm" c="dimmed">
              Uzupełnij dane karty, żeby kupić bilet.
            </Text>
          </Stack>

          <TextInput
            label="Imię i nazwisko na karcie"
            placeholder="Jan Kowalski"
            value={paymentData.cardHolder}
            onChange={(event) =>
              updatePaymentField("cardHolder", event.currentTarget.value)
            }
            autoComplete="cc-name"
            required
          />

          <TextInput
            label="Numer karty"
            placeholder="4111 1111 1111 1111"
            value={paymentData.cardNumber}
            onChange={(event) =>
              updatePaymentField("cardNumber", event.currentTarget.value)
            }
            inputMode="numeric"
            autoComplete="cc-number"
            required
          />

          <Group grow>
            <TextInput
              label="Data ważności"
              placeholder="12/30"
              value={paymentData.expiryDate}
              onChange={(event) =>
                updatePaymentField("expiryDate", event.currentTarget.value)
              }
              autoComplete="cc-exp"
              required
            />

            <TextInput
              label="CVV"
              placeholder="123"
              value={paymentData.cvv}
              onChange={(event) =>
                updatePaymentField("cvv", event.currentTarget.value)
              }
              inputMode="numeric"
              autoComplete="cc-csc"
              required
            />
          </Group>

          <Group justify="space-between" mt="sm">
            <Button variant="default" onClick={handleClose} disabled={isSubmitting}>
              Zamknij
            </Button>

            <Button
              type="submit"
              loading={isSubmitting}
              disabled={isLoadingTicketTypes || ticketTypes.length === 0}
            >
              Kup bilet
            </Button>
          </Group>
        </Stack>
      </form>
    </Modal>
  );
}