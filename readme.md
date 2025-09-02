# Feedback Bot Project

## Overview
This project is a **Telegram feedback bot** with integration into:
- **PostgreSQL** (persistent storage of feedback and user profiles),
- **Google Sheets** (optional storage for analytics/visibility),
- **OpenAI API** (automatic analysis: sentiment, criticality, recommendation),
- **Trello** (automatic card creation for critical feedbacks, levels 4–5).

Additionally, it includes a minimal **admin panel (Javalin-based web interface)** to view feedbacks with filtering by branch, role, and criticality.

> ℹ️ AI response handling and multi-channel posting (Telegram + DB + Google Sheets + Trello) was added by me as an enhancement.  
> If desired, this functionality can be disabled or simplified.

---

## Requirements
Before running the project, you need to register and obtain credentials for several external services:

1. **Telegram Bot**
    - Register a bot via [@BotFather](https://t.me/BotFather).
    - Obtain the bot token.

2. **PostgreSQL**
    - Install PostgreSQL.
    - Create a database (e.g., `vgr_feedback`).
    - Note: JDBC URL consists of **host + dbname**, for example:
      ```
      jdbc:postgresql://localhost:5432/vgr_feedback
      ```

3. **OpenAI API**
    - Register at [OpenAI](https://platform.openai.com/).
    - Generate an API key.

4. **Google Sheets**
    - Create a spreadsheet in Google Drive.
    - Enable **Google Sheets API** in [Google Cloud Console](https://console.cloud.google.com/).
    - Download the service account JSON credentials.
    - Each Telegram chat has its own sheet (created dynamically if not present).

5. **Trello**
    - Register at [Trello](https://trello.com/).
    - Obtain API key and token.
    - Identify your **list ID** where cards will be created.

---

## Configuration
In the repository you will find a template:

```
src/main/resources/config.properties.example
```

Copy it to:

```
src/main/resources/config.properties
```

and fill it with your own credentials:

```properties
telegram.token=YOUR_TELEGRAM_BOT_TOKEN
openai.key=YOUR_OPENAI_KEY

db.url=jdbc:postgresql://localhost:5432/vgr_feedback
db.user=admin
db.password=yourpassword

gcp.credentials=src/main/resources/vgr-feedbacks-XXXX.json
google.spreadsheet.id=YOUR_SPREADSHEET_ID

trello.key=YOUR_TRELLO_KEY
trello.token=YOUR_TRELLO_TOKEN
trello.listId=YOUR_TRELLO_LIST_ID
```

---

## Running
Build and run the project with Maven:

```bash
mvn clean package
java -jar target/feedback-bot-1.0-SNAPSHOT.jar
```

The bot will start listening for Telegram updates, process feedbacks, analyze them with AI, and:
- save them into PostgreSQL,
- append rows into Google Sheets,
- create Trello cards for critical feedback,
- send confirmation to the user.

Additionally, the **admin panel** will be available at:

```
http://localhost:7070
```

---

## Admin Panel
- `/` – welcome page with navigation.
- `/feedbacks` – feedback list with filtering (branch, role, criticality).
- Supports CSV/Excel export (WIP).

---

## Notes
- All sensitive data is excluded from Git and stored locally in `config.properties` (not versioned).
- Use `config.properties.example` as a reference for setting up credentials.
- AI integration can be disabled if not needed.  
