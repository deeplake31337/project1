package org.drinkless.tdlib.Action;

import org.drinkless.tdlib.Client;
import org.drinkless.tdlib.TdApi;

public class TelegramAction {
	
	private final Client.ResultHandler defaultHandler = new DefaultHandler();
	
    @SuppressWarnings("unused")
	public void sendMessage(long chatId, String message, Client client) {
        TdApi.InlineKeyboardButton[] row = {new TdApi.InlineKeyboardButton("https://telegram.org?1", new TdApi.InlineKeyboardButtonTypeUrl()), new TdApi.InlineKeyboardButton("https://telegram.org?2", new TdApi.InlineKeyboardButtonTypeUrl()), new TdApi.InlineKeyboardButton("https://telegram.org?3", new TdApi.InlineKeyboardButtonTypeUrl())};
        TdApi.ReplyMarkup replyMarkup = new TdApi.ReplyMarkupInlineKeyboard(new TdApi.InlineKeyboardButton[][]{row, row, row});

        TdApi.InputMessageContent content = new TdApi.InputMessageText(new TdApi.FormattedText(message, null), null, true);
        client.send(new TdApi.SendMessage(chatId, 0, null, null, replyMarkup, content), defaultHandler);
    }
    
    @SuppressWarnings("unused")
	public void createGroupChat(String name, Client client){
        client.send(new TdApi.CreateNewBasicGroupChat(null, name, 0), defaultHandler);
    }

    public void addMemberToGroup(long groupId, long memberId, Client client){
        client.send(new TdApi.AddChatMember(groupId, memberId, 1), defaultHandler);
        //@note check fail cacthu nữa (do fail khi không có permission, ..) https://core.telegram.org/tdlib/docs/classtd_1_1td__api_1_1add_chat_member.html
    }
    
    private class DefaultHandler implements Client.ResultHandler {
        @Override
        public void onResult(TdApi.Object object) {
            System.out.println(object.toString());
        }
    }
}
