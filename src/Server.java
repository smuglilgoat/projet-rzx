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
import java.util.Arrays;
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
            publicMsg("[Server]:[Server] Closing Server. All users will be disconnected:Chat");
            textArea1.appendText("[Server] Closing the Server in 5 sec...\n");
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
        textArea1.appendText("[Server] Server has been started\n");
        textArea1.appendText("[Server] Waiting for connection...\n");

        buttonStart.setDisable(true);
        buttonClose.setDisable(false);
        buttonUsers.setDisable(false);
    }

    @FXML
    void onButtonUsers(ActionEvent event) {
        textArea1.appendText("[Server] Printing Online Users List:\n");
        if (!users.isEmpty()) {
            for (String u : users) {
                textArea1.appendText("[Server] " + u + ", ID = " + users.indexOf(u) + "\n");
            }
        } else {
            textArea1.appendText("[Server] 0 User Online...\n");
        }
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("server.fxml"));
        primaryStage.setTitle("XenoTalk - Server Dashboard");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();

    }

    public void addUser(String user) {
        users.add(user);
        publicMsg(user + ": :Connect");
        textArea1.appendText("[Server] User {" + user + "} joined the chatroom.\n");
    }

    public void deleteUser(String user) {
        clientOutputStreams.get(users.indexOf(user)).close();
        clientOutputStreams.remove(users.indexOf(user));
        users.remove(user);
        publicMsg(user + ": :Disconnect");
        textArea1.appendText("[Server] User {" + user + "} left the chatroom.\n");
    }

    public void privateMsg(String msg, String targetName, String senderName) {
        if (!users.contains(targetName)) {
            msg = "[Server]: User {" + targetName + "} Not Found :Private:" + senderName;
            int senderId = users.indexOf(senderName);
            try {
                PrintWriter writer = clientOutputStreams.get(senderId);
                writer.println(msg);
                writer.flush();
                textArea1.appendText("[Server] Sending {" + senderName + "}: User Not Found.\n");
            } catch (Exception ex) {
                textArea1.appendText("[Server] Error transferring the msg: UserNotFound\n");
            }
        } else if (clientOutputStreams.get(users.indexOf(targetName)) != null) {
            try {
                PrintWriter writer = clientOutputStreams.get(users.indexOf(targetName));
                writer.println(msg);
                writer.flush();
                textArea1.appendText("[Server] Sending {" + targetName + "}: " + msg + "\n");
            } catch (Exception ex) {
                textArea1.appendText("[Server] Error transferring the msg: CouldNotTransmit\n");
            }
        } else {
            textArea1.appendText("[Server] Error transferring the msg: StreamNotFound\n");
        }
    }


    public void publicMsg(String msg) {
        Iterator it = clientOutputStreams.iterator();

        while (it.hasNext()) {
            try {
                PrintWriter writer = (PrintWriter) it.next();
                writer.println(msg);
                writer.flush();
            } catch (Exception ex) {
                textArea1.appendText("[Server] Error transferring the msg: PublicTransitionError\n");
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
                textArea1.appendText("[Server] Unexpected Error: InputStreamNotInitialised\n");
                textArea1.appendText("[Server] Check console for more info...\n");
                System.out.println(ex.toString());
            }

        }

        @Override
        public void run() {
            String msg;
            String[] packet;

            try {
                while ((msg = reader.readLine()) != null) {
                    packet = msg.split(":");
                    textArea1.appendText("[" + packet[0] + "] " + "{" + packet[2] + "} " + packet[1] + "\n");
                    int targetId;

                    switch (packet[2]) {
                        case "Connect":
                            addUser(packet[0]);
                            break;
                        case "Disconnect":
                            deleteUser(packet[0]);
                            break;
                        case "Chat":
                            publicMsg(msg);
                            break;
                        case "Private":
                            privateMsg(msg, packet[3], packet[0]);
                            break;
                        case "Request":
                            StringBuilder stringBuilder = new StringBuilder();
                            for (String u : users) {
                                targetId = users.indexOf(u);
                                stringBuilder.append(u).append(", ID = ").append(targetId);
                                stringBuilder.append(".   ");
                            }
                            String finalString = packet[0] + ":" + stringBuilder.toString() + ":" + "Request";
                            privateMsg(finalString, packet[0], packet[0]);
                            break;
                        default:
                            textArea1.appendText("[Server] Decoding Error: UnknownRequestType");
                            System.out.println(Arrays.toString(packet));
                            break;
                    }
                }
            } catch (Exception ex) {
                textArea1.appendText("[Server] Connection Error: HostClientLost\n");
            }
        }
    }

    public class ServerStart implements Runnable {

        @Override
        public void run() {
            try {
                @SuppressWarnings("resource")
                ServerSocket serverSock = new ServerSocket(6000);

                textArea1.appendText("[Server] IP: " + serverSock.getLocalSocketAddress().toString() + "\n");
                textArea1.appendText("[Server] Port : " + serverSock.getLocalPort() + "\n");

                while (true) {
                    Socket clientSock = serverSock.accept();
                    PrintWriter writer = new PrintWriter(clientSock.getOutputStream());

                    clientOutputStreams.add(writer);
                    Thread listener = new Thread(new Handler(clientSock, writer));
                    listener.start();
                    textArea1.appendText("[Server] New Stream Detected\n");
                }

            } catch (Exception ex) {
                textArea1.appendText("[Server] Connection Error: CouldNotInitialise\n");
                textArea1.appendText("[Server] Check console for more info...\n");
                System.out.println(ex.toString());
            }
        }
    }
}
