package main.java.com;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private static Map<String, Connection> connectionMap = new ConcurrentHashMap<String, Connection>();

    public static void main(String[] args) {

        int serverPort = ConsoleHelper.readInt();
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(serverPort);
            System.out.println("Сервер запущен.");
            while (true) {
                Socket socket = serverSocket.accept();
                Handler handler = new Handler(socket);
                handler.start();
            }
        } catch (IOException e) {
            assert serverSocket != null;
            try {
                serverSocket.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void sendBroadcastMessage(Message message) {
        connectionMap.values().stream().forEach(connection -> {
            try {
                connection.send(message);
            } catch (IOException e) {
                System.out.println("Ошибка при отправке сообщения.");
                e.printStackTrace();
            }
        });
    }

    private static class Handler extends Thread {
        private Socket socket;

        public Handler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            System.out.println("Установлено новое соединение с удаленным адресом " + socket.getRemoteSocketAddress() );
            String userName = "";
            try {
                Connection connection = new Connection(socket);
                userName = serverHandshake(connection);
                sendBroadcastMessage(new Message(MessageType.USER_ADDED,userName));
                notifyUsers(connection,userName);
                serverMainLoop(connection,userName);
            } catch (IOException | ClassNotFoundException e) {
                ConsoleHelper.writeMessage("Произошла ошибка при обмене данными с удаленным адресом");
            }
            if (userName != null) {
                connectionMap.remove(userName);
                sendBroadcastMessage(new Message(MessageType.USER_REMOVED, userName));
            }
            ConsoleHelper.writeMessage("Соединение закрыто");
        }

        private String serverHandshake(Connection connection) throws IOException, ClassNotFoundException {
            while (true) {
                connection.send(new Message(MessageType.NAME_REQUEST));
                Message answer = connection.receive();
                if (answer.getType() != MessageType.USER_NAME) continue;
                String userName = answer.getData();
                if (userName == null || userName.isEmpty()) continue;
                if (connectionMap.containsKey(userName)) continue;
                connectionMap.put(userName, connection);
                connection.send(new Message(MessageType.NAME_ACCEPTED));
                return userName;
            }
        }

        private void notifyUsers(Connection connection, String userName) throws IOException {
            connectionMap.forEach((s, connection1) -> {
                try {
                    if (!s.equals(userName)) connection.send(new Message(MessageType.USER_ADDED, s));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }

        private void serverMainLoop(Connection connection, String userName) throws IOException, ClassNotFoundException {
            while (true) {
                Message message = connection.receive();
                if (message.getType() == MessageType.TEXT) {
                    String data = userName + ": " + message.getData();
                    sendBroadcastMessage(new Message(MessageType.TEXT, data));
                } else {
                    ConsoleHelper.writeMessage("Ошибка!");
                }
            }
        }
    }
}
