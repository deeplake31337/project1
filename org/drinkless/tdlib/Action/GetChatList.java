package org.drinkless.tdlib.Action;

import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.drinkless.tdlib.Client;
import org.drinkless.tdlib.utils.OrderedChat;
import org.drinkless.tdlib.TdApi;

public class GetChatList {
	
    public final NavigableSet<OrderedChat> mainChatList = new TreeSet<OrderedChat>();
    private boolean haveFullMainChatList = false;
    private final String newLine = System.getProperty("line.separator");
    private volatile String currentPrompt = null;
    private static final ConcurrentMap<Long, TdApi.Chat> chats = new ConcurrentHashMap<Long, TdApi.Chat>();
    
    public void getMainChatList(final int limit, Client client) {
        synchronized (mainChatList) {
            if (!haveFullMainChatList && limit > mainChatList.size()) {
                client.send(new TdApi.LoadChats(new TdApi.ChatListMain(), limit - mainChatList.size()), new Client.ResultHandler() {
                    @Override
                    public void onResult(TdApi.Object object) {
                        switch (object.getConstructor()) {
                            case TdApi.Error.CONSTRUCTOR:
                                if (((TdApi.Error) object).code == 404) {
                                    synchronized (mainChatList) {
                                        haveFullMainChatList = true;
                                    }
                                } else {
                                    System.err.println("Receive an error for LoadChats:" + newLine + object);
                                }
                                break;
                            case TdApi.Ok.CONSTRUCTOR:
                                getMainChatList(limit, client);
                                break;
                            default:
                                System.err.println("Receive wrong response from TDLib:" + newLine + object);
                        }
                    }
                });
                return;
            }

            java.util.Iterator<OrderedChat> iter = mainChatList.iterator();
            System.out.println();
            System.out.println("List " + mainChatList.size() + " chat(s):");
            for (int i = 0; i < mainChatList.size(); i++) {
                long chatId = iter.next().chatId;
                TdApi.Chat chat = chats.get(chatId);
                synchronized (chat) {
                    System.out.println(chatId + ": " + chat.title);
                }
            }
            print("");
        }
    }
    
    public void updateNewChat(TdApi.Object object) {
        TdApi.UpdateNewChat updateNewChat = (TdApi.UpdateNewChat) object;
        TdApi.Chat chat = updateNewChat.chat;
        synchronized (chat) {
            chats.put(chat.id, chat);

            TdApi.ChatPosition[] positions = chat.positions;
            chat.positions = new TdApi.ChatPosition[0];
            setChatPositions(chat, positions);
        }
    }
    
    public void updateChatPosition(TdApi.Object object) {
        TdApi.UpdateChatPosition updateChat = (TdApi.UpdateChatPosition) object;
        if (updateChat.position.list.getConstructor() != TdApi.ChatListMain.CONSTRUCTOR) {
            return;
        }

        TdApi.Chat chat = chats.get(updateChat.chatId);
        synchronized (chat) {
            int i;
            for (i = 0; i < chat.positions.length; i++) {
                if (chat.positions[i].list.getConstructor() == TdApi.ChatListMain.CONSTRUCTOR) {
                    break;
                }
            }
            TdApi.ChatPosition[] new_positions = new TdApi.ChatPosition[chat.positions.length + (updateChat.position.order == 0 ? 0 : 1) - (i < chat.positions.length ? 1 : 0)];
            int pos = 0;
            if (updateChat.position.order != 0) {
                new_positions[pos++] = updateChat.position;
            }
            for (int j = 0; j < chat.positions.length; j++) {
                if (j != i) {
                    new_positions[pos++] = chat.positions[j];
                }
            }
            assert pos == new_positions.length;

            setChatPositions(chat, new_positions);
        }
    }
    
    private void setChatPositions(TdApi.Chat chat, TdApi.ChatPosition[] positions) {
        synchronized (mainChatList) {
            synchronized (chat) {
                for (TdApi.ChatPosition position : chat.positions) {
                    if (position.list.getConstructor() == TdApi.ChatListMain.CONSTRUCTOR) {
                        boolean isRemoved = mainChatList.remove(new OrderedChat(chat.id, position));
                        assert isRemoved;
                    }
                }
                chat.positions = positions;

                for (TdApi.ChatPosition position : chat.positions) {
                    if (position.list.getConstructor() == TdApi.ChatListMain.CONSTRUCTOR) {
                        boolean isAdded = mainChatList.add(new OrderedChat(chat.id, position));
                        assert isAdded;
                    }
                }
            }
        }
    }
    
    private void print(String str) {
        if (currentPrompt != null) {
            System.out.println("");
        }
        System.out.println(str);
        if (currentPrompt != null) {
            System.out.print(currentPrompt);
        }
    }
}
