import { useState } from "react";
import {
  Alert,
  Anchor,
  Button,
  Card,
  Container,
  Group,
  PasswordInput,
  Stack,
  Text,
  TextInput,
  Title,
} from "@mantine/core";
import { loginUser, registerUser } from "../services/authService";

export default function AuthView({ onAuthenticated }) {
  const [mode, setMode] = useState("login");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [repeatedPassword, setRepeatedPassword] = useState("");
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  const isRegisterMode = mode === "register";

  function switchMode(nextMode) {
    setMode(nextMode);
    setPassword("");
    setRepeatedPassword("");
    setError("");
  }

  function validateForm() {
    const trimmedEmail = email.trim();

    if (!trimmedEmail) {
      return "Podaj adres e-mail.";
    }

    if (!trimmedEmail.includes("@")) {
      return "Podaj poprawny adres e-mail.";
    }

    if (!password) {
      return "Podaj hasło.";
    }

    if (isRegisterMode && password.length < 4) {
      return "Hasło musi mieć co najmniej 4 znaki.";
    }

    if (isRegisterMode && password !== repeatedPassword) {
      return "Hasła nie są takie same.";
    }

    return "";
  }

  async function handleSubmit(event) {
    event.preventDefault();

    const validationError = validateForm();

    if (validationError) {
      setError(validationError);
      return;
    }

    setError("");
    setIsSubmitting(true);

    try {
      if (isRegisterMode) {
        await registerUser({ email, password });
      }

      const auth = await loginUser({ email, password });
      onAuthenticated(auth);
    } catch (err) {
      setError(err.message || "Nie udało się zalogować.");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <Container size={460} py="xl">
      <Card withBorder shadow="sm" radius="md" p="xl">
        <form onSubmit={handleSubmit}>
          <Stack gap="md">
            <Stack gap={4}>
              <Title order={2}>
                {isRegisterMode ? "Załóż konto" : "Zaloguj się"}
              </Title>

              <Text size="sm" c="dimmed">
                {isRegisterMode
                  ? "Utwórz konto, żeby korzystać z LocalMobility."
                  : "Zaloguj się, żeby przejść do aplikacji."}
              </Text>
            </Stack>

            {error && (
              <Alert color="red" variant="light">
                {error}
              </Alert>
            )}

            <TextInput
              label="E-mail"
              placeholder="adres@email.pl"
              value={email}
              onChange={(event) => setEmail(event.currentTarget.value)}
              autoComplete="email"
              required
            />

            <PasswordInput
              label="Hasło"
              placeholder="Hasło"
              value={password}
              onChange={(event) => setPassword(event.currentTarget.value)}
              autoComplete={isRegisterMode ? "new-password" : "current-password"}
              required
            />

            {isRegisterMode && (
              <PasswordInput
                label="Powtórz hasło"
                placeholder="Powtórz hasło"
                value={repeatedPassword}
                onChange={(event) => setRepeatedPassword(event.currentTarget.value)}
                autoComplete="new-password"
                required
              />
            )}

            <Button type="submit" loading={isSubmitting}>
              {isRegisterMode ? "Załóż konto" : "Zaloguj się"}
            </Button>

            <Group justify="center" gap={6}>
              <Text size="sm" c="dimmed">
                {isRegisterMode ? "Masz już konto?" : "Nie masz konta?"}
              </Text>

              <Anchor
                component="button"
                type="button"
                size="sm"
                onClick={() => switchMode(isRegisterMode ? "login" : "register")}
              >
                {isRegisterMode ? "Zaloguj się" : "Załóż konto"}
              </Anchor>
            </Group>
          </Stack>
        </form>
      </Card>
    </Container>
  );
}