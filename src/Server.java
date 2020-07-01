import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;

public class Server extends Application {

    ArrayList<PrintWriter> clientOutputStreams = new ArrayList<>();
    ArrayList<String> users = new ArrayList<>();

    @FXML
    private TextArea textArea1;
    @FXML
    private Button buttonStart;
    @FXML
    private Button buttonClose;
    @FXML
    private Button buttonUsers;

    public static void main(String[] args) {
        launch(args);
    }

    @FXML
    void onButtonClose(ActionEvent event) {
        try {
            publicMsg("[Server] Closing Server. All users will be disconnected");
            textArea1.appendText("Closing the Server in 5 sec...\n");
            Thread.sleep(1000);
            System.exit(0);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    @FXML
    void onButtonStart(ActionEvent event) {
        Thread starter = new Thread(new ServerStart());
        starter.start();
        textArea1.appendText(" Server has been started\n");
        textArea1.appendText(" Waiting for connection...\n");

        buttonStart.setDisable(true);
        buttonClose.setDisable(false);
        buttonUsers.setDisable(false);
    }

    @FXML
    void onButtonUsers(ActionEvent event) {
        textArea1.appendText(" Printing Online Users List:\n");
        if (!users.isEmpty()) {
            for (String u : users) {
                textArea1.appendText(" " + u + ", ID = " + users.indexOf(u) + "\n");
            }
        } else {
            textArea1.appendText(" 0 User Online...\n");
        }
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("server.fxml"));
        primaryStage.setTitle("Scaillpe - Server Dashboard");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();

    }

    public void addUser(String user) {
        String message, s = ": :Connect";
        users.add(user);
        for (String u : users) {
            message = (u + ": :Connect");
            publicMsg(message);
        }
        publicMsg("Server: :Done");
    }

    public void deleteUser(String user) {
        @SuppressWarnings("unused")
        String message;
        users.remove(user);
        for (String u : users) {
            message = (u + ": :Disconnect");
            publicMsg(message);
        }
    }

    public void privateMsg(String msg, int targetId, String targetName) {
        if (targetId == -1) {
            msg = "[Server]: User Not Found :private";
            targetId = users.indexOf(targetName);
            try {
                PrintWriter writer = clientOutputStreams.get(targetId);
                writer.println(msg);
                writer.flush();
                textArea1.appendText("Sending [" + targetName + "]: User Not Found.\n");
            } catch (Exception ex) {
                textArea1.appendText(" Error transferring the msg.\n");
            }
        } else {
            if (clientOutputStreams.get(targetId) != null) {
                try {
                    PrintWriter writer = clientOutputStreams.get(targetId);
                    writer.println(msg);
                    writer.flush();
                    textArea1.appendText("Sending [" + targetName + "]:" + msg + "\n");
                } catch (Exception ex) {
                    textArea1.appendText(" Error transferring the msg.\n");
                }
            } else {
                textArea1.appendText(" Error transferring the msg.\n");
            }
        }
    }

    public void publicMsg(String message) {
        Iterator it = clientOutputStreams.iterator();

        while (it.hasNext()) {
            try {
                PrintWriter writer = (PrintWriter) it.next();
                writer.println(message);
                writer.flush();
            } catch (Exception ex) {
                textArea1.appendText(" Error transferring the msg.\n");
            }
        }
    }

    public class Handler implements Runnable {
        BufferedReader reader;
        Socket socket;
        PrintWriter client;

        public Handler(Socket clientSocket, PrintWriter user) {
            client = user;
            try {
                socket = clientSocket;
                InputStreamReader isReader = new InputStreamReader(socket.getInputStream());
                reader = new BufferedReader(isReader);
            } catch (Exception ex) {
                textArea1.appendText(" Unexpected Error. Check console for more info...\n");
                System.out.println(ex.toString());
            }

        }

        @Override
        public void run() {
            String message;
            String[] data;

            try {
                while ((message = reader.readLine()) != null) {

                    textArea1.appendText(" Received: " + message + "\n");
                    data = message.split(":");
                    int targetId;

                    switch (data[2]) {
                        case "Connect":
                            publicMsg((data[0] + ":" + data[1] + ":Chat"));
                            addUser(data[0]);
                            break;
                        case "Disconnect":
                            publicMsg((data[0] + "  has :Disconnected" + ":Chat"));
                            clientOutputStreams.remove(users.indexOf(data[0]));
                            deleteUser(data[0]);
                            break;
                        case "Chat":
                            publicMsg(message);
                            break;
                        case "private":
                            targetId = users.indexOf(data[3]);
                            if (targetId != -1) {
                                privateMsg(message, targetId, data[3]);
                            } else {
                                privateMsg(message, targetId, data[0]);
                            }
                            break;
                        case "request":
                            StringBuilder stringBuilder = new StringBuilder();
                            for (String u : users) {
                                targetId = users.indexOf(u);
                                stringBuilder.append(u).append(", ID = ").append(targetId);
                                stringBuilder.append(".   ");
                            }
                            targetId = users.indexOf(data[0]);
                            String finalString = stringBuilder.toString();
                            finalString = data[0] + ":" + finalString + ":" + "request";
                            privateMsg(finalString, targetId, data[0]);
                            break;
                        default:
                            textArea1.appendText(" Can't identify request type");
                            break;
                    }
                }
            } catch (Exception ex) {
                textArea1.appendText("Connection Lost.\n");
            }
        }
    }

    public class ServerStart implements Runnable {

        @Override
        public void run() {
            try {
                @SuppressWarnings("resource")
                ServerSocket serverSock = new ServerSocket(6000);

                textArea1.appendText(" IP: " + serverSock.getLocalSocketAddress().toString() + "\n");
                textArea1.appendText(" Port : " + serverSock.getLocalPort() + "\n");

                while (true) {
                    Socket clientSock = serverSock.accept();
                    PrintWriter writer = new PrintWriter(clientSock.getOutputStream());

                    clientOutputStreams.add(writer);
                    Thread listener = new Thread(new Handler(clientSock, writer));
                    listener.start();
                    textArea1.appendText(" New User Connected\n");
                }

            } catch (Exception ex) {
                textArea1.appendText(" Error making a connection. Check console for more info...\n");
                System.out.println(ex.toString());
            }
        }
    }
}
