# Praktikrapport-vurdering

Webapplikation til vurdering af praktikrapporter fra datamatikeruddannelsen på Erhvervsakademi København (EK). En underviser vælger en rapport i browseren, tilføjer eventuelle noter, og får en struktureret AI-vurdering baseret på en fastlagt rubric.

---

## Hvad projektet gør

1. Underviseren åbner appen i browseren og vælger en rapport fra `data/`-mappen
2. Appen sender rapporten til OpenAI (GPT-4o) med en systemprompt der indeholder rubricen
3. AI'en returnerer en struktureret vurdering med rubric-oversigt, karakter på 7-trinsskalaen, stærke punkter, udviklingsområder, eksamenssspørgsmål og en samlet konklusion
4. Vurderingen vises som formateret markdown i browseren og gemmes automatisk i `output/`

---

## Mappestruktur

```
praktikvurdering/
│
├── data/                        # Praktikrapporter (én .md-fil pr. studerende)
│   └── student1.md
│
├── prompts/
│   ├── system-prompt.md         # Rubric og instruktioner til AI'en (redigér her)
│   └── user-prompt.md           # Skabelon til manuel brug i Claude Code
│
├── output/                      # Genererede vurderinger gemmes her automatisk
│   └── student1-vurdering.md
│
└── app/
    ├── backend/                 # Python + FastAPI
    │   ├── main.py
    │   ├── requirements.txt
    │   └── .env.example
    └── frontend/                # React + Vite + TypeScript
        ├── src/
        │   ├── App.tsx
        │   ├── index.css
        │   ├── main.tsx
        │   └── components/
        │       ├── ReportSelector.tsx
        │       ├── NoteEditor.tsx
        │       └── EvaluationView.tsx
        ├── index.html
        ├── package.json
        └── vite.config.ts
```

---

## Rubricen

Vurderingen er bygget op om **6 dimensioner** med niveauer der mappes til 7-trinsskalaen:

| Dimension | Niveauer | Karakter |
|-----------|----------|----------|
| Formelle krav | Opfyldt / Delvist opfyldt / Ikke opfyldt | 12 / 4 / 00 |
| Læringsmål: Viden | Fremragende / Tilfredsstillende / Under forventning | 10-12 / 7 / 02-4 |
| Læringsmål: Færdigheder | Fremragende / Tilfredsstillende / Under forventning | 10-12 / 7 / 02-4 |
| Læringsmål: Kompetencer | Fremragende / Tilfredsstillende / Under forventning | 10-12 / 7 / 02-4 |
| DARE / SHARE / CARE | Stærkt tilstede / Tilstede / Fraværende | 10-12 / 7 / 02-4 |
| Refleksionsdybde | Dyb / Overfladisk / Beskrivende | 10-12 / 7 / 02-4 |

Rubricen redigeres i [`prompts/system-prompt.md`](prompts/system-prompt.md).

Grundlaget for rubricen er:
- Krav til praktikrapporten (omfang, obligatoriske elementer)
- EKs DARE / SHARE / CARE-værdier
- Læringsmål fra studieordningen for datamatikeruddannelsen

---

## Kom i gang

### Krav

- Python 3.10+
- Node.js 18+
- En OpenAI API-nøgle

### 1. Opsæt backend

```bash
cd app/backend

# Kopiér miljøfil og indsæt din API-nøgle
cp .env.example .env
# Åbn .env og sæt: OPENAI_API_KEY=sk-...

# Installér afhængigheder
pip install -r requirements.txt

# Start backend (kører på http://localhost:8000)
uvicorn main:app --reload
```

### 2. Opsæt frontend

Åbn et nyt terminalvindue:

```bash
cd app/frontend

npm install

# Start frontend (kører på http://localhost:5173)
npm run dev
```

### 3. Brug appen

1. Åbn [http://localhost:5173](http://localhost:5173) i browseren
2. Vælg en rapport i dropdown-menuen
3. Tilføj eventuelle noter til eksaminatoren (valgfrit)
4. Klik **"Kør vurdering"**
5. Vurderingen vises i browseren og gemmes i `output/{rapportnavn}-vurdering.md`

---

## Tilføj en ny rapport

1. Gem rapporten som en `.md`-fil i `data/`-mappen (fx `student2.md`)
2. Genindlæs siden — filen dukker automatisk op i dropdown-menuen

---

## Tilpas rubricen

Rediger [`prompts/system-prompt.md`](prompts/system-prompt.md) direkte. Ændringerne træder i kraft ved næste vurdering (ingen genstart nødvendig).

---

## API-oversigt (backend)

| Method | Endpoint | Beskrivelse |
|--------|----------|-------------|
| `GET` | `/reports` | Returnerer liste af rapporter i `data/` |
| `POST` | `/evaluate` | Body: `{filename, notes}` — kører vurdering og gemmer output |
| `GET` | `/output/{filename}` | Henter en gemt vurdering |

---

## Miljøvariabler

| Variabel | Beskrivelse | Standard |
|----------|-------------|---------|
| `OPENAI_API_KEY` | Din OpenAI API-nøgle | — (påkrævet) |
| `OPENAI_MODEL` | Model der bruges til vurdering | `gpt-4o` |
| `CORS_ORIGINS` | Kommasepareret liste over tilladte origins | — |

---

## Deploy til produktion

> GitHub Pages kan kun hoste statiske filer. Frontenden deployes til Pages, backenden deployes separat til Render.com (gratis tier).

### Arkitektur i produktion

```
GitHub Pages  ←→  Render.com (FastAPI)  ←→  OpenAI API
(frontend)         (backend)
```

### Trin 1 – Opret GitHub-repo og aktiver Pages

1. Opret et nyt repo på GitHub og push koden
2. Gå til **Settings → Pages → Source** og vælg **"GitHub Actions"**

### Trin 2 – Deploy backend til Render

1. Gå til [render.com](https://render.com) og opret en konto
2. Vælg **New → Web Service** og peg på dit GitHub-repo
3. Sæt **Root Directory** til `app/backend`
4. Render finder automatisk `render.yaml` og konfigurerer servicen
5. Under **Environment** i Render-dashboardet tilføjes:
   - `OPENAI_API_KEY` = din OpenAI-nøgle
   - `CORS_ORIGINS` = `https://DITBRUGERNAVN.github.io`
6. Kopiér URL'en til din Render-service (fx `https://praktikvurdering-api.onrender.com`)

### Trin 3 – Tilføj GitHub Secrets

I dit GitHub-repo: **Settings → Secrets and variables → Actions → New repository secret**

| Secret | Værdi |
|--------|-------|
| `VITE_API_URL` | URL fra Render, fx `https://praktikvurdering-api.onrender.com` |

### Trin 4 – Push til main

Workflow-filen i `.github/workflows/deploy.yml` kører automatisk ved hvert push til `main` og deployer frontenden til:

```
https://DITBRUGERNAVN.github.io/REPO-NAVN/
```

### API-nøgle – lokalt vs. produktion

| Miljø | Hvor |
|-------|------|
| Lokal udvikling | `app/backend/.env` (aldrig i git — se `.gitignore`) |
| Produktion (Render) | Environment variable i Render-dashboardet |
| CI/CD-workflow | GitHub Secret `VITE_API_URL` (backend-URL, ikke selve nøglen) |

> OpenAI-nøglen forlader aldrig backenden. Frontenden kender kun URL'en til din Render-service.
