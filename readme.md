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
    - Install PostgreSQL if needed.
    - Create a database (e.g., `vgr_feedback`).
    - Note: JDBC URL consists of **host + dbname**, for example:
      ```
      jdbc:postgresql://localhost:5432/vgr_feedback
      ```
3. **OpenAI API**
    - Register at [OpenAI](https://platform.openai.com/) and generate an API key here:  
      [https://platform.openai.com/api-keys](https://platform.openai.com/api-keys)
    - Place your key into `config.properties`:
      ```
      openai_key=sk-xxxxxx
      ```
4. **Google Sheets**
    - Go to [Google Cloud Console](https://console.cloud.google.com/).
    - Create a new project and enable **Google Sheets API**.
    - Create a **Service Account** and download the JSON key file.
    - Rename this file to something like `google-credentials.json` and place it into `src/main/resources/`.
    - Share your Google Spreadsheet with the **client_email** from the JSON file (give “Editor” permission).
    - Copy the spreadsheet ID (from the URL: `https://docs.google.com/spreadsheets/d/<spreadsheetId>/edit`)  
     and put it into `config.properties`:
      ```
      google_spreadsheet_id=your-spreadsheet-id
      google_credentials_file=src/main/resources/google-credentials.json
      ```
    - Each Telegram chat has its own sheet (created dynamically if not present).


5. **Trello**
    - Go to [Trello API Keys](https://trello.com/app-key) to get your **API Key**.
    - On the same page, click **Token** to generate your **API Token**.
    - Get your **List ID** (open a Trello list in browser and extract the ID from the URL, or use Trello API).
    - Put these values into `config.properties`:
      ```
      trello_key=your-key
      trello_token=your-token
      trello_list_id=your-list-id
      ```

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
telegram_token=YOUR_TELEGRAM_BOT_TOKEN
telegram_username=YOUR_OPENAI_KEY

db_url=jdbc:postgresql://localhost:5432/
db_name=vgr_feedback
db_user=admin
db_password=yourpassword

google_credentials_path=src/main/resources/google.credentials.json
google_sheet_id=YOUR_SPREADSHEET_ID

openai_key=sk-xxxxxx

trello_key=YOUR_TRELLO_KEY
trello_token=YOUR_TRELLO_TOKEN
trello_listId=YOUR_TRELLO_LIST_ID
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
- Supports CSV export.

---

## Notes
- All sensitive data is excluded from Git and stored locally in `config.properties` (not versioned).
- Use `config.properties.example` as a reference for setting up credentials.
- AI integration can be disabled if not needed.  
