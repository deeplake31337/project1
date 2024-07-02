package org.drinkless.tdlib.Airtable;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class AirtableService {
    private static String AIRTABLE_TOKEN = "patadtKN0lkO5zei3.db92a413ba1a309d90ad2478e5dc57a03bef70b3682dc2c94164a248fa1651cd";
    private static String AIRTABLE_BASE_NAME = "appiu3qkaGFqnoaGd";
    private static String AIRTABLE_TABLE_NAME = "tblWjEaZwPRf3mScr";
    public static boolean recordCheck(String fromAccount, String chatId, String senderId, String messageId, String message) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.airtable.com/v0/" + AIRTABLE_BASE_NAME + "/MyTable?maxRecords=1&filterByFormula=AND(FromAccount%3D%22" + fromAccount + "%22%2CChatId%3D%22" + chatId + "%22%2CSenderId%3D%22" + senderId + "%22%2CMessageId%3D%22" + messageId + "%22%2CMessage%3D%22" + message + "%22)"))
                .GET()
                .setHeader("Authorization", "Bearer " + AIRTABLE_TOKEN)
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if(response.body().length() == 14){
            //System.out.println("true");
            return false; //record chưa có
        }
        else {
            //System.out.println("false");
            return true; //record có rồi
        }
    }

    public static void updateDataAirtable(String fromAccount, String chatId, String senderId, String messageId, String message) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.airtable.com/v0/" + AIRTABLE_BASE_NAME + "/" + AIRTABLE_TABLE_NAME))
                .POST(HttpRequest.BodyPublishers.ofString("{\"fields\": {\"FromAccount\": \"" + fromAccount + "\",\"ChatId\": \"" + chatId + "\",\"SenderId\": \"" + senderId + "\",\"MessageId\": \"" + messageId + "\",\"Message\": \"" + message + "\"}}"))
                .setHeader("Authorization", "Bearer patadtKN0lkO5zei3.db92a413ba1a309d90ad2478e5dc57a03bef70b3682dc2c94164a248fa1651cd")
                .setHeader("Content-Type", "application/json")
                .build();

        client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
