# DermaCare — dermatology clinic booking (portfolio project)

A small full-stack booking site for a fictional dermatology clinic: patients browse
services and book appointments online; the clinic admin manages the service catalogue,
the weekly opening schedule, and incoming appointments.

This is a **portfolio project** — a first iteration meant to show a complete, working
slice end to end rather than a production system. There is no real clinic, no real
patient data, and no payments: every appointment here is free to book.

It is a sibling of the `fanvote` project in this workspace and reuses the same stack and
conventions (Spring Boot + MongoDB backend, Angular frontend, Keycloak for auth), but is
otherwise fully independent — its own database, its own Docker Compose file, no shared
code. The only thing actually shared is a Keycloak instance (see below).

## Stack

- **Backend**: Java 21, Spring Boot 3.5, MongoDB (Spring Data), Spring Security OAuth2
  resource server (validates Keycloak JWTs).
- **Frontend**: Angular 22 (standalone components), `keycloak-angular` / `keycloak-js`.
- **Database**: MongoDB, one collection each for services, the weekly schedule, and
  appointments.
- **Auth**: Keycloak, realm `clinic`, client `frontend`. No Keycloak instance ships with
  this repo — see "Auth" below.

## Repository layout

```
portfolio-clinic/
  backend/            Spring Boot app (Maven)
  frontend/           Angular app
  scripts/populate/   Demo service data (one folder per service, about.txt)
  scripts/add-populated-services.sh   Idempotent seeder for the demo services
  docker-compose.yml  Local dev stack: mongo + backend + frontend
  .env.example        Every environment variable this stack reads
```

## What's implemented (first iteration)

- **Public**: homepage, service catalogue (`/services`).
- **Booking** (`/book`, signed in): pick a service and a date, see live available slots
  computed from the clinic's weekly schedule minus already-booked appointments, confirm.
- **Account** (`/account`, signed in): your own appointment history, cancel an upcoming
  one.
- **Admin** (`/admin`, realm role `clinic-admin`): three tabs — service CRUD (with an
  active/inactive toggle so a service can be hidden without deleting its history),
  weekly schedule editor (7 rows: day, open/closed, start/end time, slot length),
  appointments list with status changes (confirm / cancel / complete).

Backend REST surface:

| Method | Path | Access |
|---|---|---|
| GET | `/api/services` | public — active services only |
| GET/POST/PUT/DELETE | `/api/admin/services[/{id}]` | `clinic-admin` |
| GET/PUT | `/api/admin/schedule` | `clinic-admin` |
| GET | `/api/appointments/available-slots?date=YYYY-MM-DD&serviceId=...` | public |
| POST | `/api/appointments` | authenticated |
| GET | `/api/account/appointments` | authenticated (own) |
| PUT | `/api/account/appointments/{id}/cancel` | authenticated (own) |
| GET | `/api/admin/appointments` | `clinic-admin` |
| PUT | `/api/admin/appointments/{id}/status` | `clinic-admin` |

The available-slots endpoint deliberately reveals only which times are free, never who
holds the busy ones — no patient names or ids appear in that response, so it can safely
stay public: a visitor can check whether the clinic has room before creating an account.

**Race-condition guard**: booking re-validates server-side that the requested slot is
still free, and a partial unique index on `(date, startTime)` (excluding cancelled
appointments) means two near-simultaneous booking requests can't both win — the loser
gets a 409 rather than a silently double-booked slot. See
`AppointmentServiceImpl.book()` and `AppointmentIndexInitializer`.

**Default schedule**: on first startup, if the weekly schedule collection is empty, it is
seeded with Mon–Fri 09:00–17:00 in 30 minute slots, weekend closed
(`ScheduleSeedInitializer`). This never overwrites an admin's edits — it only fills in
what's missing.

## Prerequisites

- Docker (for the full local stack), **or** Java 21 + Maven + Node for running the
  pieces directly.
- A reachable Keycloak instance with a `clinic` realm and a public `frontend` client
  (PKCE, redirect URIs covering `http://localhost:4200/*` or wherever you serve the
  frontend from) — see "Auth" below.

## Running locally with Docker Compose

```
cp .env.example .env
docker compose up --build
```

- Frontend: http://localhost:4201
- Backend: http://localhost:8081/api
- Mongo: mongodb://localhost:27018/clinic (a dedicated `clinic-mongo-data` volume)

Ports are deliberately offset from the fanvote stack's (27017 / 8090 / 4200) so both can
run on the same machine at once.

## Running the pieces directly (no Docker)

Backend:

```
cd backend
mvn spring-boot:run
```

Reads `SPRING_DATA_MONGODB_URI` / `SPRING_DATA_MONGODB_DATABASE` /
`KEYCLOAK_ISSUER_URI` / `CORS_ALLOWED_ORIGIN` from the environment, or falls back to
`application.properties`' defaults (`mongodb://localhost:27017/clinic`,
`http://localhost:8080/realms/clinic`, `http://localhost:4200`). Point MongoDB at a
local instance any way you like — `docker run -p 27017:27017 mongo:8.3` is enough.

Frontend:

```
cd frontend
npm install
npm start
```

Serves on http://localhost:4200 by default, talking to the backend at
`http://localhost:8081/api` (see `src/environment/environment.ts`).

> If your local Node is older than the Angular CLI here requires (this project was
> built with Angular 22, which wants Node ^20.19 / ^22.12 / >=24), install a matching
> Node version rather than editing `engines` down — the CLI's own minimum is enforced
> at runtime regardless of what `package.json` claims.

## Auth (Keycloak)

This project does **not** run its own Keycloak — it is designed to share the instance
the sibling `fanvote` project already runs (`docker compose up` in `../fanvote`), the
same way both projects might share infrastructure in a portfolio. What's needed:

1. In that Keycloak's admin console, create a realm named `clinic`.
2. In it, create a public client `frontend` with:
   - Standard flow enabled, PKCE method S256
   - Valid redirect URIs: `http://localhost:4200/*` (or `4201` if served via Docker
     Compose) and the silent-check-sso asset URL under `/assets/*`
   - Web origins: `http://localhost:4200` (or `4201`)
3. Create a realm role `clinic-admin` and assign it to whichever test user should reach
   `/admin`.
4. Create at least one ordinary user (no `clinic-admin` role) to test booking as a
   patient.

Point the backend's `KEYCLOAK_ISSUER_URI` and the frontend's
`src/environment/environment.ts` `keycloak.url`/`realm`/`clientId` at wherever that
realm is reachable. Until this is configured, the public pages (`/`, `/services`) work
fine; `/book`, `/account` and `/admin` will fail to validate tokens.

## Seeding demo services

With the stack up and a `.env` present:

```
scripts/add-populated-services.sh
```

Reads every folder under `scripts/populate/` (one demo dermatology service each —
`consult-initial`, `dermatoscopie`, `tratament-acnee`, `peeling-chimic`, `crioterapie`,
`consult-pediatric`) and writes it into the `services` collection. Idempotent: services
are matched by title, so rerunning it after editing an `about.txt` replaces rather than
duplicates. No real photos are seeded — `imageUrl` is left blank and the frontend shows a
placeholder icon; point a service at a real image later from the admin panel.

## Known limitations / follow-ups (left as TODO for later iterations)

- No email/SMS confirmation or reminder for appointments — booking, confirming and
  cancelling are all silent besides the in-app state change.
- No pagination on the admin appointments list; fine for a demo dataset, would need one
  under real volume.
- The available-slots computation re-fetches and re-scans a day's appointments on every
  call rather than caching — fine at this scale, would want an index-backed query if the
  appointment volume ever grew large per day.
- No automated end-to-end/UI tests; the backend has a unit test suite for the slot
  computation and booking race-condition logic (`AppointmentServiceImplTest`), but there
  is no Angular test coverage yet.
- No favicon/brand imagery shipped — the header uses a plain "✚" glyph as a placeholder
  mark rather than a designed logo.
- Patient name/contact on an appointment is a free-text snapshot taken at booking time,
  not validated against or synced with the Keycloak account profile.
- `MedicalService.imageUrl` is a plain string with no upload flow; there's no picture
  storage in this iteration (unlike fanvote's GridFS-backed pictures) since no real
  photos were meant to be sourced for a demo dermatology catalogue.

## Assumptions made while building this

- Slot generation steps through the day at the schedule's configured granularity
  (`slotDurationMinutes`, e.g. every 30 minutes) but each candidate slot occupies the
  *service's own* duration for overlap purposes — a 60 minute service starting at 09:00
  blocks 09:00–10:00 even on a clinic that otherwise offers a new slot every half hour.
  This matches "different services can have different durations" more literally than a
  same-length-as-granularity reading would.
- `available-slots` is public (no auth) since it reveals only free/busy times, never
  identities — anyone can check the clinic's availability before deciding to sign up and
  book, similar to why fanvote's public profile GETs are unauthenticated.
- Cancelling one's own appointment is a status change (`CANCELLED`), not a delete — so
  the slot becomes bookable again (the partial unique index excludes cancelled
  appointments) while the record stays in the admin's list for visibility.
- Ports in `docker-compose.yml` (27018/8081/4201) are offset from fanvote's
  (27017/8090/4200) on the assumption both stacks might run side by side on the same
  laptop, per this workspace's existing "works across two laptops" setup.
