# Specyfikacja Wymagań i Projektu

**Przedmiot:** Języki Programowania Aplikacji Internetowych

---

## Informacje o zespole

| Nr | Imię i Nazwisko | Rola w projekcie (np. Frontend, Backend, DevOps) |
|---:|-----------------|--------------------------------------------------|
| 1 | Łucja Ciborowska | Backend |
| 2 | Matylda Lis | Frontend |
| 3 |  |  |
| 4 |  |  |
| 5 |  |  |

---

**Nazwa projektu:** LocalMobility: Transport i Parkingi

**Link do repozytorium (np. GitHub):** Zostanie przesłane później.

---

## 1. Wizja i Cel Projektu

Celem projektu jest zarządzanie zasobami gminy w zakresie transportu i parkingów, w celu ułatwienia korzystania mieszkańcom, turystom i zarządowi.

Projekt jest realizowany poprzez dostarczenie aplikacji elektronicznej z przyjaznym interfejsem użytkownika. Dzięki aplikacji każdy będzie mógł kupić bilet, sprawdzić gdzie znajduje się autobus i jaki ma rozkład, znaleźć punkt parkingowy,  a także wynająć rower miejski i za niego zapłacić.

---

## 2. Użytkownicy (Aktorzy)

Wymień i opisz role w systemie:

- Gość: Może sprawdzić rozkląd jazdy, lokalizacje parkingów.
- Użytkownik zalogowany (Mieszkaniec): Oprócz powyższego może zakupić bilet na autobus, a także znaleźć dostępne rowery.
- Administrator/Urzędnik: Wyświetlanie listy użytkowników, listy kupowanych biletów, wprowadanie danych.

---

## 3. Wymagania Funkcjonalne (Metoda MoSCoW)

Określ priorytety funkcji aplikacji:

| Kategoria | Lista funkcjonalności (User Stories) |
|----------|---------------------------------------|
| MUST HAVE (Krytyczne) | Użytkownik może założyć konto i się zalogować, może sprawdzić mapę autobusów w czasie rzeczywistym, może sprawdzić rozkład jazdy, zakupić bilet na autobus. |
| SHOULD HAVE (Ważne) | Zakup wielu rodzajów biletów - czasowych, miesięcznych, sprawdzanie lokalizacji parkingów, informacje o opłatach, mozliwość najmu roweru miejskiego. |
| COULD HAVE (Opcjonalne) | Możliwość liczenia ludzi w poszczególnych autobusach, stan obłożenia parkingu. |
| WON'T HAVE (Poza zakresem) | Obliczanie najlepszej możliwej trasy, estymowanego czasu opóźnienia autobusów. |

---

## 4. Wymagania Niefunkcjonalne

Hasła będą hashowane za pomocą prostego algorytmu. Nawigacja za pomocą myszki, obsługa w przeglądarce tylko na komputerze.

---

## 5. Stos Technologiczny

- Frontend: [np. React, Vue, Next.js]  
  React, javascript

- Backend: [np. Node.js, Python/Django, Java/Spring]  
  java, spring

- Baza danych: [np. PostgreSQL, MongoDB]  
  PostgreSQL

- Inne: [np. Docker, Mapbox API, Stripe]

---

## 6. Projekt Bazy Danych

Tworzenie bazy danych jest przewidywane na kolejne tygodnie pracy.

---

## 7. Harmonogram 7-tygodniowy

| Tydzień | Zadania do zrealizowania |
|--------|---------------------------|
| Tydzień 1 | Analiza wymagań i specyfikacji. |
| Tydzień 2 | Tworzenie interktywnej mapy, UI do niej |
| Tydzień 3 | Tworzenie bazy danych - implementacja serwera |
| Tydzień 4 | Tworzenie systemu płatności dla klienta |
| Tydzień 5 | Itegracja frontendu z backendem |
| Tydzień 6 | Testy aplikacji |
| Tydzień 7 | Tworzenie dokumentacji |
