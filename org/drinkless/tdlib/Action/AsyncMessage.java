package org.drinkless.tdlib.Action;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.drinkless.tdlib.Client;
import org.drinkless.tdlib.TdApi;
import org.drinkless.tdlib.Airtable.AirtableService;
import org.drinkless.tdlib.utils.DataUtils;

public class AsyncMessage {
	private String currentChatId = "";
    private String currentSenderId = "";
    private String currentMessageId = "0";
    private String currentMessage = "";
    private String currentUserId;
    
    private Boolean syncDone = false;

    public void getUserId(Client client){
        client.send(new TdApi.GetMe(), new Client.ResultHandler() {
            @Override
            public void onResult(TdApi.Object object) {
                TdApi.User currentUser = (TdApi.User) object;
                currentUserId = currentUser.id + "";
            }
        });
    }
    
    public void asyncChatToAirtable(Client client, long chatId) throws IOException, InterruptedException {
    	//idea: (https://github.com/tdlib/td/issues/168)
        System.out.println("start async chatId: " + String.valueOf(chatId));
        getMessageFromChat(chatId, Long.parseLong(currentMessageId), client);
        getUserId(client);
        while(syncDone == false){
            updateAndGetMessage(chatId, client);
            TimeUnit.MILLISECONDS.sleep(300);   //lí do: https://github.com/tdlib/td/issues/743
        }
        System.out.println("done");
    }
    
    public void updateAndGetMessage(long chatId, Client client) throws IOException, InterruptedException {
        if(AirtableService.recordCheck(currentUserId, currentChatId, currentSenderId, currentMessageId, currentMessage) == false){ //cái nào record ở airtable rồi sẽ skip
            System.out.println("New message can sync:" + currentUserId + "  " + currentChatId + "  " + currentSenderId + "  " + currentMessageId + "  " + currentMessage);
            AirtableService.updateDataAirtable(currentUserId, currentChatId, currentSenderId, currentMessageId, currentMessage);
        }
        else {
            System.out.println("Sync done.");
            syncDone = true;
        }
        getMessageFromChat(chatId, Long.parseLong(currentMessageId), client);
    }
    
    public void getMessageFromChat(long chatId, long fromMessageId, Client client){
        client.send(new TdApi.GetChatHistory(chatId,fromMessageId,0,1,false), new Client.ResultHandler() {
            @Override
            public void onResult(TdApi.Object object) {
                String[] currentData = DataUtils.getDataFromChat(object);
                currentMessageId = currentData[0];
                currentSenderId = currentData[1];
                currentChatId = currentData[2];
                currentMessage = currentData[3];
            }
        });
    }
}
