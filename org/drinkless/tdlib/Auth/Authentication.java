package org.drinkless.tdlib.Auth;

import java.io.BufferedReader;

import java.io.IOError;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.drinkless.tdlib.Action.AsyncMessage;
import org.drinkless.tdlib.Action.GetChatList;
import org.drinkless.tdlib.Action.TelegramAction;
import org.drinkless.tdlib.Client;
import org.drinkless.tdlib.TdApi;
import org.drinkless.tdlib.utils.DataUtils;

public class Authentication {
	
	private Client client = null;
	private TdApi.AuthorizationState authorizationState = null;
    private volatile boolean needQuit = false;
    private volatile boolean canQuit = false;
    private volatile String currentPrompt = null;
    private final Lock authorizationLock = new ReentrantLock();
    private final String newLine = System.getProperty("line.separator");
    private final Condition gotAuthorization = authorizationLock.newCondition();
    private volatile boolean haveAuthorization = false;
    private static final String commandsLine = "";
    private static final Client.ResultHandler defaultHandler = new DefaultHandler();
    
    private GetChatList chatList = new GetChatList();
    private TelegramAction getTelegramAction = new TelegramAction();
    private AsyncMessage asyncTelegramMessage = new AsyncMessage();
    
    public void displayHelp() {
    	System.out.println("This is program to work with telegram and airtable.");
    	System.out.println("Command list:");
    	System.out.println("help                  : Show command list.");
    	System.out.println("gcs <number>          : Get id of <number> first group chats.");
    	System.out.println("gc <chatId>           : Get info of group chat.");
    	System.out.println("create <name>         : Create group chat.");
    	System.out.println("add <chatId> <userId> : Add user to group chat.");
    	System.out.println("async <chatId>        : Async all message from specific chatId to AirTable.");
    	System.out.println("lo                    : Logout telegram.");
    	System.out.println("me                    : Get infomation of your account.");
    	System.out.println("q                     : Close program.");
    	System.out.println("Have fun!");
    	}
	
	public void loginTelegram() throws InterruptedException, IOException {
        Client.setLogMessageHandler(0, new LogMessageHandler());

        // disable TDLib log and redirect fatal errors and plain log messages to a file
        try {
            Client.execute(new TdApi.SetLogVerbosityLevel(0));
            Client.execute(new TdApi.SetLogStream(new TdApi.LogStreamFile("tdlib.log", 1 << 27, false)));
        } catch (Client.ExecutionException error) {
            throw new IOError(new IOException("Write access to the current directory is required"));
        }

        // create client
        client = Client.create(new UpdateHandler(), null, null);
        
        while (!needQuit) {
            // await authorization
            authorizationLock.lock();
            try {
                while (!haveAuthorization) {
                    gotAuthorization.await();
                }
            } finally {
                authorizationLock.unlock();
            }
            while (haveAuthorization) {
                getCommand();
            }
        }
        while (!canQuit) {
            Thread.sleep(1);
        }
	}
    
    private void getCommand() throws IOException, InterruptedException {
        String command = promptString(commandsLine);
        String[] commands = command.split(" ", 3);
        try {
            switch (commands[0]) {
                case "help":
                    displayHelp();
                    break;
                case "lo":
                    haveAuthorization = false;
                    client.send(new TdApi.LogOut(), defaultHandler);
                    break;
                case "q":
                    needQuit = true;
                    haveAuthorization = false;
                    client.send(new TdApi.Close(), defaultHandler);
                    break;
                case "me":
                    client.send(new TdApi.GetMe(), defaultHandler);
                    break;
                case "gcs": {
                    int limit = 20;
                    if (commands.length > 1) {
                        limit = DataUtils.toInt(commands[1]);
                    }
                    chatList.getMainChatList(limit, client);
                    break;
                }
                case "gc":
                    client.send(new TdApi.GetChat(DataUtils.getChatId(commands[1])), defaultHandler);
                    break;
                case "sm": {
                    getTelegramAction.sendMessage(DataUtils.getChatId(commands[1]), commands[2], client);
                    break;
                }
                case "create": {
                	getTelegramAction.createGroupChat(commands[1], client);
                    break;
                }
                case "add": {
                    getTelegramAction.addMemberToGroup(DataUtils.getChatId(commands[1]), Long.valueOf(commands[2]), client);
                    break;
                }
                case "async": {
                	asyncTelegramMessage.asyncChatToAirtable(client, Long.valueOf(commands[1]));
                    break;
                }
                default:
                    System.err.println("Unsupported command: " + command);
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            print("Not enough arguments");
        }
    }
    
    private void onAuthorizationStateUpdated(TdApi.AuthorizationState authorizationState) {
        if (authorizationState != null) {
            this.authorizationState = authorizationState;
        }
        switch (this.authorizationState.getConstructor()) {
            case TdApi.AuthorizationStateWaitTdlibParameters.CONSTRUCTOR:
                TdApi.SetTdlibParameters request = new TdApi.SetTdlibParameters();
                request.databaseDirectory = "tdlib";
                request.useMessageDatabase = true;
                request.useSecretChats = true;
                request.apiId = 94575;
                request.apiHash = "a3406de8d171bb422bb6ddf3bbd800e2";
                request.systemLanguageCode = "en";
                request.deviceModel = "Desktop";
                request.applicationVersion = "1.0";

                client.send(request, new AuthorizationRequestHandler());
                break;
            case TdApi.AuthorizationStateWaitPhoneNumber.CONSTRUCTOR: {
                String phoneNumber = promptString("Please enter phone number: ");
                client.send(new TdApi.SetAuthenticationPhoneNumber(phoneNumber, null), new AuthorizationRequestHandler());
                break;
            }
            case TdApi.AuthorizationStateWaitCode.CONSTRUCTOR: {
                String code = promptString("Please enter authentication code: ");
                client.send(new TdApi.CheckAuthenticationCode(code), new AuthorizationRequestHandler());
                break;
            }
            case TdApi.AuthorizationStateReady.CONSTRUCTOR:
                haveAuthorization = true;
                authorizationLock.lock();
                try {
                    gotAuthorization.signal();
                } finally {
                    authorizationLock.unlock();
                }
                break;
            case TdApi.AuthorizationStateLoggingOut.CONSTRUCTOR:
                haveAuthorization = false;
                System.out.println("Logging out");
                break;
            case TdApi.AuthorizationStateClosing.CONSTRUCTOR:
                haveAuthorization = false;
                System.out.println("Closing");
                break;
            case TdApi.AuthorizationStateClosed.CONSTRUCTOR:
            	System.out.println("Closed");
                if (!needQuit) {
                    client = Client.create(new UpdateHandler(), null, null); // recreate client after previous has closed
                } else {
                    canQuit = true;
                }
                break;
            default:
                System.err.println("Unsupported authorization state:" + newLine + authorizationState);
        }
    }
    public class UpdateHandler implements Client.ResultHandler {
        @Override
        public void onResult(TdApi.Object object) {
            switch (object.getConstructor()) {
                case TdApi.UpdateAuthorizationState.CONSTRUCTOR:
                    onAuthorizationStateUpdated(((TdApi.UpdateAuthorizationState) object).authorizationState);
                    break;
                case TdApi.UpdateNewChat.CONSTRUCTOR:
                    chatList.updateNewChat(object);
                case TdApi.UpdateChatPosition.CONSTRUCTOR:
                    chatList.updateChatPosition(object);
            }
        }
    }
    private String promptString(String prompt) {
        System.out.print(prompt);
        currentPrompt = prompt;
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String str = "";
        try {
            str = reader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        currentPrompt = null;
        return str;
    }
    private class AuthorizationRequestHandler implements Client.ResultHandler {
        @Override
        public void onResult(TdApi.Object object) {
            switch (object.getConstructor()) {
                case TdApi.Error.CONSTRUCTOR:
                    System.err.println("Receive an error:" + newLine + object);
                    onAuthorizationStateUpdated(null); // repeat last action
                    break;
                case TdApi.Ok.CONSTRUCTOR:
                    break;
                default:
                    System.err.println("Receive wrong response from TDLib:" + newLine + object);
            }
        }
    }
    private class LogMessageHandler implements Client.LogMessageHandler {
        @Override
        public void onLogMessage(int verbosityLevel, String message) {
            if (verbosityLevel == 0) {
                onFatalError(message);
                return;
            }
            System.err.println(message);
        }
    }
    private static class DefaultHandler implements Client.ResultHandler {
        @Override
        public void onResult(TdApi.Object object) {
            System.out.println(object.toString());
        }
    }
    private void onFatalError(String errorMessage) {
        final class ThrowError implements Runnable {
            private final String errorMessage;
            private final AtomicLong errorThrowTime;

            private ThrowError(String errorMessage, AtomicLong errorThrowTime) {
                this.errorMessage = errorMessage;
                this.errorThrowTime = errorThrowTime;
            }

            @Override
            public void run() {
                if (isDatabaseBrokenError(errorMessage) || isDiskFullError(errorMessage) || isDiskError(errorMessage)) {
                    processExternalError();
                    return;
                }

                errorThrowTime.set(System.currentTimeMillis());
                throw new ClientError("TDLib fatal error: " + errorMessage);
            }

            private void processExternalError() {
                errorThrowTime.set(System.currentTimeMillis());
                throw new ExternalClientError("Fatal error: " + errorMessage);
            }

            @SuppressWarnings("serial")
            final class ClientError extends Error {
                private ClientError(String message) {
                    super(message);
                }
            }

            @SuppressWarnings("serial")
            final class ExternalClientError extends Error {
                public ExternalClientError(String message) {
                    super(message);
                }
            }

            private boolean isDatabaseBrokenError(String message) {
                return message.contains("Wrong key or database is corrupted") ||
                        message.contains("SQL logic error or missing database") ||
                        message.contains("database disk image is malformed") ||
                        message.contains("file is encrypted or is not a database") ||
                        message.contains("unsupported file format") ||
                        message.contains("Database was corrupted and deleted during execution and can't be recreated");
            }

            private boolean isDiskFullError(String message) {
                return message.contains("PosixError : No space left on device") ||
                        message.contains("database or disk is full");
            }

            private boolean isDiskError(String message) {
                return message.contains("I/O error") || message.contains("Structure needs cleaning");
            }
        }

        final AtomicLong errorThrowTime = new AtomicLong(Long.MAX_VALUE);
        new Thread(new ThrowError(errorMessage, errorThrowTime), "TDLib fatal error thread").start();

        // wait at least 10 seconds after the error is thrown
        while (errorThrowTime.get() >= System.currentTimeMillis() - 10000) {
            try {
                Thread.sleep(1000 /* milliseconds */);
            } catch (InterruptedException ignore) {
                Thread.currentThread().interrupt();
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
