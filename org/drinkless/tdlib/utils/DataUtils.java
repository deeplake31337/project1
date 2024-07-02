package org.drinkless.tdlib.utils;

import org.drinkless.tdlib.TdApi;

public class DataUtils {
    @SuppressWarnings("unused")
	public static int toInt(String arg) {
        int result = 0;
        try {
            result = Integer.parseInt(arg);
        } catch (NumberFormatException ignored) {
        }
        return result;
    }
    public static long getChatId(String arg) {
        long chatId = 0;
        try {
            chatId = Long.parseLong(arg);
        } catch (NumberFormatException ignored) {
        }
        return chatId;
    }
    public static String[] getDataFromChat(TdApi.Object object){
        TdApi.Messages messages = (TdApi.Messages) object;
        TdApi.Message[] message = messages.messages;
        Long messageId = message[0].id;
        TdApi.MessageSenderUser sender = (TdApi.MessageSenderUser) message[0].senderId;
        Long senderId = sender.userId;
        Long chatId = message[0].chatId;
        TdApi.MessageText content = (TdApi.MessageText) message[0].content;
        TdApi.FormattedText formattedText = (TdApi.FormattedText) content.text;
        String text = formattedText.text;

        return new String[]{messageId.toString(), senderId.toString(), chatId.toString(), text};
    }
}
