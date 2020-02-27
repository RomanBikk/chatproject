package main.java.com.client;



import main.java.com.Connection;
import main.java.com.ConsoleHelper;
import main.java.com.Message;
import main.java.com.MessageType;

import java.io.IOException;
import java.net.Socket;

public class Client extends Thread {
    protected Connection connection;
    private volatile boolean clientConnected = false;

    public static void main(String[] args) {
        Client client = new Client();
        client.run();
    }


    @Override
    public void run() {
        Thread thread = getSocketThread();
        thread.setDaemon(true);
        thread.start();
        synchronized (this) {
            try {
                wait();
                if (clientConnected = true) {
                    notify();
                    ConsoleHelper.writeMessage("Соединение установлено. Для выхода наберите команду 'exit'.");
                    while (clientConnected) {
                        String text = ConsoleHelper.readString();
                        if (text.equals("exit")) clientConnected = false;
                        else {
                            if (shouldSendTextFromConsole()) sendTextMessage(text);
                        }
                    }
                }
                else {
                    ConsoleHelper.writeMessage("Произошла ошибка во время работы клиента.");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public class SocketThread extends Thread {


        @Override
        public void run() {
            try {
                String address = getServerAddress();
                int serverPort = getServerPort();
                Socket socket = new Socket(address,serverPort);
                connection = new Connection(socket);
                clientHandshake();
                clientMainLoop();
            } catch (IOException | ClassNotFoundException e) {
                notifyConnectionStatusChanged(false);
                e.printStackTrace();
            }
        }

        protected void processIncomingMessage(String message) {
            ConsoleHelper.writeMessage(message);
        }

        protected void informAboutAddingNewUser(String userName) {
            ConsoleHelper.writeMessage(userName + " присоединился к чату.");
        }

        protected void informAboutDeletingNewUser(String userName) {
            ConsoleHelper.writeMessage(userName + " покинул чат.");
        }

        protected void notifyConnectionStatusChanged(boolean clientConnected) {
            Client.this.clientConnected = clientConnected;
             synchronized (Client.this) {
                Client.this.notify();
             }
        }

        protected void clientHandshake() throws IOException, ClassNotFoundException {
            while (true) {
               Message message =  connection.receive();
               if (message.getType() == MessageType.NAME_REQUEST) {
                   String userName = getUserName();
                   Message outputMessage = new Message(MessageType.USER_NAME,userName);
                   connection.send(outputMessage);
               }
               else if (message.getType() == MessageType.NAME_ACCEPTED) {
                   notifyConnectionStatusChanged(true);
                   break;
               }
               else throw new IOException("Unexpected main.java.com.MessageType");
            }
        }

        protected void clientMainLoop() throws IOException, ClassNotFoundException {
            while (true) {
                Message message = connection.receive();
                if (message.getType() == MessageType.TEXT) {
                    processIncomingMessage(message.getData());
                }
                else if (message.getType() == MessageType.USER_ADDED) {
                    informAboutAddingNewUser(message.getData());
                }
                else if (message.getType() == MessageType.USER_REMOVED) {
                    informAboutDeletingNewUser(message.getData());
                }
                    else {
                    throw new IOException("Unexpected main.java.com.MessageType");
                }
            }
        }

    }

    protected String getServerAddress() {
        return ConsoleHelper.readString();
    }

    protected int getServerPort() {
        return ConsoleHelper.readInt();
    }

    protected String getUserName() {
        return ConsoleHelper.readString();
    }

    protected boolean shouldSendTextFromConsole() {
        return true;
    }

    protected SocketThread getSocketThread() {
        return new SocketThread();
    }

    protected void sendTextMessage(String text) {
        try {
            Message newMessage = new Message(MessageType.TEXT, text);
            connection.send(newMessage);
        } catch (IOException e) {
            clientConnected = false;
            ConsoleHelper.writeMessage("Ошибка при отправке сообщения!");
            e.printStackTrace();

        }
    }
}
