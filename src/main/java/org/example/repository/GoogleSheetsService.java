package org.example.repository;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.*;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class GoogleSheetsService {
    private final Sheets sheets;
    private final String spreadsheetId;

    public GoogleSheetsService(InputStream credentialStream, String spreadsheetId) throws Exception {

        GoogleCredentials credentials = ServiceAccountCredentials
                .fromStream(credentialStream)
                .createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS));

        sheets = new Sheets.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName("FeedbackBot")
                .build();


        this.spreadsheetId = spreadsheetId;
    }


    public GoogleSheetsService(String serviceAccountPath, String spreadsheetId) throws Exception {
        this(new FileInputStream(serviceAccountPath), spreadsheetId);

    }

    public static GoogleSheetsService create(Properties cfg) throws Exception {
        String path = cfg.getProperty("google_credentials_path");
        String sheetId = cfg.getProperty("google_sheet_id");
        return new GoogleSheetsService(path, sheetId);
    }

    public void appendFeedbackRow(List<Object> rowData) throws Exception {
        ValueRange body = new ValueRange().setValues(List.of(rowData));
        sheets.spreadsheets().values()
                .append(spreadsheetId, "A1", body)
                .setValueInputOption("RAW")
                .execute();
    }

    // додає рядок у потрібний аркуш (створює аркуш, якщо його нема)
    public void appendFeedbackRow(String sheetName, List<Object> row) throws IOException {
        ensureSheetExists(sheetName);

        ValueRange body = new ValueRange().setValues(Collections.singletonList(row));
        sheets.spreadsheets().values()
                .append(spreadsheetId, sheetName + "!A1", body)
                .setValueInputOption("RAW")
                .execute();
    }

    // перевіряє, чи існує аркуш, і створює його при потребі
    private void ensureSheetExists(String sheetName) throws IOException {
        Spreadsheet spreadsheet = sheets.spreadsheets().get(spreadsheetId).execute();

        boolean exists = spreadsheet.getSheets().stream()
                .anyMatch(s -> s.getProperties().getTitle().equals(sheetName));

        if (!exists) {
            AddSheetRequest addSheetRequest = new AddSheetRequest()
                    .setProperties(new SheetProperties().setTitle(sheetName));

            BatchUpdateSpreadsheetRequest batchUpdateRequest =
                    new BatchUpdateSpreadsheetRequest().setRequests(
                            List.of(new Request().setAddSheet(addSheetRequest))
                    );

            sheets.spreadsheets().batchUpdate(spreadsheetId, batchUpdateRequest).execute();
        }
    }
}
