import { Alert, Button, Loader, Modal, Stack, Text } from "@mantine/core";
import { useEffect, useState } from "react";
import { getAdminEmails } from "../services/adminService";

export default function AdminEmailsButton() {
  const [isAdmin, setIsAdmin] = useState(false);
  const [opened, setOpened] = useState(false);
  const [emails, setEmails] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [message, setMessage] = useState("");

  useEffect(() => {
    let isMounted = true;

    async function checkAdmin() {
      try {
        const result = await getAdminEmails();

        if (!isMounted) {
          return;
        }

        if (result === null) {
          setIsAdmin(false);
          setEmails([]);
          return;
        }

        setIsAdmin(true);
        setEmails(result);
      } catch {
        if (isMounted) {
          setIsAdmin(false);
          setEmails([]);
        }
      }
    }

    checkAdmin();

    return () => {
      isMounted = false;
    };
  }, []);

  async function loadEmails() {
    setIsLoading(true);
    setMessage("");

    try {
      const result = await getAdminEmails();

      if (result === null) {
        setOpened(false);
        setIsAdmin(false);
        setEmails([]);
        return;
      }

      setEmails(result);
    } catch {
      setMessage("Nie udało się pobrać listy maili. Spróbuj ponownie później.");
    } finally {
      setIsLoading(false);
    }
  }

  function handleOpen() {
    setOpened(true);
    loadEmails();
  }

  if (!isAdmin) {
    return null;
  }

  return (
    <>
      <Button variant="outline" onClick={handleOpen}>
        Lista maili użytkowników
      </Button>

      <Modal
        opened={opened}
        onClose={() => setOpened(false)}
        title="Utworzone konta"
        centered
      >
        <Stack gap="sm">
          {isLoading ? <Loader size="sm" /> : null}

          {message ? (
            <Alert color="red" variant="light">
              {message}
            </Alert>
          ) : null}

          {!isLoading && !message && emails.length === 0 ? (
            <Text c="dimmed">Nie ma jeszcze utworzonych kont.</Text>
          ) : null}

          {!isLoading && !message && emails.length > 0 ? (
            <ul style={{ margin: 0, paddingLeft: 20 }}>
              {emails.map((email) => (
                <li key={email}>{email}</li>
              ))}
            </ul>
          ) : null}
        </Stack>
      </Modal>
    </>
  );
}