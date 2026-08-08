# ProcureAI — Cloud Deployment Guide

Complete step-by-step guide for deploying **ProcureAI** across cloud platforms:
- **Frontend**: Firebase Hosting (`https://procureai-89869.web.app`)
- **Backend & AI Microservice**: Render Docker Blueprint (`procureai-backend` + `procureai-ai`)
- **Database**: Aiven MySQL Cloud Database

---

## 1. Frontend Deployment — Firebase Hosting (🟢 Live)

The React single-page frontend is deployed on **Firebase Hosting**.

### Production Live URLs:
- **Main App**: [https://procureai-89869.web.app](https://procureai-89869.web.app)
- **Secondary Domain**: [https://procureai-89869.firebaseapp.com](https://procureai-89869.firebaseapp.com)

### Deployment Steps (Automated / Manual CLI):
```bash
# 1. Build optimized production bundle
cd FRONTEND
npm run build

# 2. Deploy to Firebase Hosting from root directory
cd ..
npx firebase deploy --only hosting
```

---

## 2. Backend & AI Microservice — Render Deployment

Both the **Java Spring Boot Backend** and **Python FastAPI AI Microservice** are configured for 1-click container deployment on Render using the [`render.yaml`](./render.yaml) blueprint specification.

### Render Blueprint Configuration (`render.yaml`)

```yaml
services:
  - type: web
    name: procureai-ai
    env: docker
    dockerfilePath: ./Dockerfile
    dockerContext: ./AI-SERVICE
    plan: free
    region: singapore
    envVars:
      - key: GEMINI_API_KEY
        sync: false
      - key: GEMINI_MODEL
        value: gemini-1.5-flash-latest

  - type: web
    name: procureai-backend
    env: docker
    dockerfilePath: ./Dockerfile
    dockerContext: .
    plan: free
    region: singapore
    envVars:
      - key: SPRING_PROFILES_ACTIVE
        value: prod
      - key: DB_URL
        sync: false
      - key: DB_USERNAME
        sync: false
      - key: DB_PASSWORD
        sync: false
      - key: DB_DRIVER
        value: com.mysql.cj.jdbc.Driver
      - key: DB_DIALECT
        value: org.hibernate.dialect.MySQL8Dialect
      - key: DDL_AUTO
        value: update
      - key: GEMINI_API_KEY
        sync: false
      - key: BREVO_API_KEY
        sync: false
      - key: JWT_SECRET
        generateValue: true
      - key: CORS_ORIGINS
        value: https://procureai-89869.web.app,https://procureai-89869.firebaseapp.com,http://localhost:5173
      - key: PYTHON_AI_ENABLED
        value: "true"
      - key: PYTHON_AI_URL
        sync: false
```

### Steps to Launch on Render:
1. Go to [Render Dashboard](https://dashboard.render.com/) and click **New +** -> **Blueprint**.
2. Connect your GitHub repository: `https://github.com/VishalRajExe/ProcureAI`.
3. Render auto-detects `render.yaml` and provisions both services:
   - `procureai-ai` (FastAPI microservice on port 8000)
   - `procureai-backend` (Spring Boot backend on port 8080)
4. Fill in environment variables:
   - `GEMINI_API_KEY`: Your Google Gemini API key
   - `BREVO_API_KEY`: Your Brevo API key
   - `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`: Your Aiven MySQL credentials
   - `PYTHON_AI_URL`: `https://procureai-ai.onrender.com` (or internal Render address)

---

## 3. Database Setup — Aiven MySQL Cloud Database

Aiven provides managed MySQL instances for cloud production storage.

### Aiven MySQL Connection Parameters:

| Parameter | Value |
|---|---|
| **DB_URL** | `jdbc:mysql://<AIVEN_HOST>:<AIVEN_PORT>/procureai?createDatabaseIfNotExist=true&useSSL=true&allowPublicKeyRetrieval=true` |
| **DB_USERNAME** | `avnadmin` (or your Aiven user) |
| **DB_PASSWORD** | `<YOUR_AIVEN_PASSWORD>` |
| **DB_DRIVER** | `com.mysql.cj.jdbc.Driver` |
| **DB_DIALECT** | `org.hibernate.dialect.MySQL8Dialect` |

### Setting up Aiven MySQL:
1. Log in to [Aiven Console](https://console.aiven.io/).
2. Create a **MySQL** service instance (Free / Starter plan).
3. Under **Service Overview**, copy the **Host**, **Port**, **User**, and **Password**.
4. Pass these values to Render as environment variables (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`).
5. Spring Boot Hibernate auto-creates database tables on startup via `DDL_AUTO=update`.

---

## Summary Architecture

```
React SPA Frontend (Firebase Hosting)
   └── https://procureai-89869.web.app
         │
         ▼ HTTP / REST (JWT Auth)
Spring Boot Backend (Render Docker)
   ├── https://procureai-backend.onrender.com
   ├── Managed Aiven MySQL Database (jdbc:mysql://aiven...)
   └── FastAPI AI Microservice (Render Docker)
         └── https://procureai-ai.onrender.com → Google Gemini API
```
