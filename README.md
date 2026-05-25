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

**Link do repozytorium (np. GitHub):** [Zostanie przesłane później.](https://github.com/Matylda00/LocalMobility.git)

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

Hasła będą hashowane za pomocą prostego algorytmu (BCrypt) . Nawigacja za pomocą myszki, obsługa w przeglądarce tylko na komputerze.

---

## 5. Stos Technologiczny

- Frontend: [np. React, Vue, Next.js]  
  React, javascript

- Backend: [np. Node.js, Python/Django, Java/Spring]  
  java, spring

- Baza danych: [np. PostgreSQL, MongoDB]  
  PostgreSQL

---

## 6. Projekt Bazy Danych

Baza danych będzie zawierała tabele potrzebne do obsługi funkcjonalności z kategorii MUST HAVE oraz SHOULD HAVE, czyli kont użytkowników, logowania, autobusów, rozkładów jazdy, zakupu biletów autobusowych, różnych rodzajów biletów, parkingów, opłat parkingowych oraz rowerów miejskich.

---

### Tabele w bazie danych

#### users

| Nazwa pola | Typ danych |
|----------|------------|
| id | BIGINT PRIMARY KEY |
| first_name | VARCHAR(100) |
| last_name | VARCHAR(100) |
| email | VARCHAR(255) UNIQUE NOT NULL |
| password_hash | VARCHAR(255) NOT NULL |
| role | VARCHAR(50) NOT NULL |
| created_at | TIMESTAMP DEFAULT CURRENT_TIMESTAMP |

---

#### bus_lines

| Nazwa pola | Typ danych |
|----------|------------|
| id | BIGINT PRIMARY KEY |
| line_number | VARCHAR(20) UNIQUE NOT NULL |
| name | VARCHAR(100) |
| start_stop | VARCHAR(100) |
| end_stop | VARCHAR(100) |

---

#### buses

| Nazwa pola | Typ danych |
|----------|------------|
| id | BIGINT PRIMARY KEY |
| bus_number | VARCHAR(50) NOT NULL |
| line_id | BIGINT NOT NULL |
| current_latitude | DECIMAL(10, 7) |
| current_longitude | DECIMAL(10, 7) |
| last_updated | TIMESTAMP |

---

#### bus_stops

| Nazwa pola | Typ danych |
|----------|------------|
| id | BIGINT PRIMARY KEY |
| name | VARCHAR(100) NOT NULL |
| latitude | DECIMAL(10, 7) NOT NULL |
| longitude | DECIMAL(10, 7) NOT NULL |

---

#### schedules

| Nazwa pola | Typ danych |
|----------|------------|
| id | BIGINT PRIMARY KEY |
| line_id | BIGINT NOT NULL |
| stop_id | BIGINT NOT NULL |
| departure_time | TIME NOT NULL |
| day_type | VARCHAR(50) NOT NULL |

---

#### ticket_types

| Nazwa pola | Typ danych |
|----------|------------|
| id | BIGINT PRIMARY KEY |
| name | VARCHAR(100) NOT NULL |
| price | DECIMAL(10, 2) NOT NULL |
| duration_minutes | INTEGER |
| ticket_category | VARCHAR(50) |

---

#### tickets

| Nazwa pola | Typ danych |
|----------|------------|
| id | BIGINT PRIMARY KEY |
| user_id | BIGINT NOT NULL |
| ticket_type_id | BIGINT NOT NULL |
| purchase_date | TIMESTAMP DEFAULT CURRENT_TIMESTAMP |
| valid_from | TIMESTAMP |
| valid_to | TIMESTAMP |
| status | VARCHAR(50) NOT NULL |

---

#### parking_lots

| Nazwa pola | Typ danych |
|----------|------------|
| id | BIGINT PRIMARY KEY |
| name | VARCHAR(100) NOT NULL |
| address | VARCHAR(255) |
| latitude | DECIMAL(10, 7) NOT NULL |
| longitude | DECIMAL(10, 7) NOT NULL |
| total_spaces | INTEGER |
| available_spaces | INTEGER |

---

#### parking_fees

| Nazwa pola | Typ danych |
|----------|------------|
| id | BIGINT PRIMARY KEY |
| parking_lot_id | BIGINT NOT NULL |
| fee_name | VARCHAR(100) NOT NULL |
| price | DECIMAL(10, 2) NOT NULL |
| duration_minutes | INTEGER |

---

#### bike_stations

| Nazwa pola | Typ danych |
|----------|------------|
| id | BIGINT PRIMARY KEY |
| name | VARCHAR(100) NOT NULL |
| latitude | DECIMAL(10, 7) NOT NULL |
| longitude | DECIMAL(10, 7) NOT NULL |
| available_bikes | INTEGER |
| available_slots | INTEGER |

---

#### bikes

| Nazwa pola | Typ danych |
|----------|------------|
| id | BIGINT PRIMARY KEY |
| bike_number | VARCHAR(50) UNIQUE NOT NULL |
| station_id | BIGINT |
| status | VARCHAR(50) NOT NULL |

---

#### bike_rentals

| Nazwa pola | Typ danych |
|----------|------------|
| id | BIGINT PRIMARY KEY |
| user_id | BIGINT NOT NULL |
| bike_id | BIGINT NOT NULL |
| start_time | TIMESTAMP DEFAULT CURRENT_TIMESTAMP |
| end_time | TIMESTAMP |
| price | DECIMAL(10, 2) |
| status | VARCHAR(50) NOT NULL |



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
