import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

public class Server extends Application {

    ArrayList<PrintWriter> clientOutputStreams = new ArrayList<>();
    ArrayList<String> users = new ArrayList<>();
    ArrayList<String> files = new ArrayList<String>();

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
        publicMsg("[Server]: Closing Server. All users will be disconnected:Chat");
        textArea1.appendText("[Server] Closing the Server in 5 sec...\n");
        System.exit(0);
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
        publicMsg(user + ": :Disconnect");
        textArea1.appendText("[Server] User {" + user + "} left the chatroom.\n");
        clientOutputStreams.get(users.indexOf(user)).close();
        clientOutputStreams.remove(users.indexOf(user));
        users.remove(user);
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
            StringBuilder stringBuilder;
            String finalString;
            byte[] contents = new byte[10000];

            try {
                while ((msg = reader.readLine()) != null) {
                    packet = msg.split(":");
                    textArea1.appendText("[" + packet[0] + "] " + "{" + packet[2] + "} " + packet[1] + "\n");

                    switch (packet[2]) {
                        case "Connect":
                            addUser(packet[0]);
                            break;
                        case "Disconnect":
                            deleteUser(packet[0]);
                            socket.close();
                            break;
                        case "Chat":
                            publicMsg(msg);
                            break;
                        case "Private":
                            privateMsg(msg, packet[3], packet[0]);
                            break;
                        case "Request":
                            stringBuilder = new StringBuilder();
                            for (String u : users) {
                                stringBuilder.append(u).append("/");
                            }
                            finalString = packet[0] + ":" + stringBuilder.toString() + ":" + "Request";
                            privateMsg(finalString, packet[0], packet[0]);
                            break;
                        case "File":
                            switch (packet[3]) {
                                case "Upload":
                                    textArea1.appendText("[Server] Downloading file : " + packet[1] + " from {" + packet[0] + "}\n");

                                    File directory = new File("E:\\Documents\\Code\\project-rzx\\out\\production\\project-rzx\\files\\");
                                    if (!directory.exists()) {
                                        directory.mkdir();
                                    }

                                    contents = new byte[10000];

                                    //Initialize the FileOutputStream to the output file's full path.
                                    FileOutputStream fos = new FileOutputStream("E:\\Documents\\Code\\project-rzx\\out\\production\\project-rzx\\files\\" + packet[1]);
                                    BufferedOutputStream bos = new BufferedOutputStream(fos);
                                    InputStream is = socket.getInputStream();

                                    //No of bytes read in one read() call
                                    int bytesRead = 0;


                                    do {
                                        bytesRead = is.read(contents);
                                        textArea1.appendText("[Server] Read " + bytesRead + " bytes\n");
                                        bos.write(contents, 0, bytesRead);
                                    } while (bytesRead == 10000);

                                    bos.flush();
                                    bos.close();
                                    fos.close();
                                    files.add(packet[1]);

                                    publicMsg("[Server]: New File Uploaded:Chat");
                                    stringBuilder = new StringBuilder();
                                    for (String u : files) {
                                        stringBuilder.append(u).append("/");
                                    }
                                    finalString = packet[0] + ":" + stringBuilder.toString() + ":" + "File:List";
                                    privateMsg(finalString, packet[0], packet[0]);
                                    break;
                                case "List":
                                    stringBuilder = new StringBuilder();
                                    for (String u : files) {
                                        stringBuilder.append(u).append("/");
                                    }
                                    finalString = packet[0] + ":" + stringBuilder.toString() + ":" + "File:List";
                                    privateMsg(finalString, packet[0], packet[0]);
                                    break;
                                case "Download":
                                    textArea1.appendText("[Server] Uploading File: " + packet[1] + ", to " + packet[0] + "\n");

                                    //Specify the file
                                    File file = new File("E:\\Documents\\Code\\project-rzx\\out\\production\\project-rzx\\files\\" + packet[1]);
                                    FileInputStream fis = new FileInputStream("E:\\Documents\\Code\\project-rzx\\out\\production\\project-rzx\\files\\" + packet[1]);
                                    BufferedInputStream bis = new BufferedInputStream(fis);

                                    //Get socket's output stream
                                    OutputStream os = socket.getOutputStream();

                                    //Read File Contents into contents array
                                    long fileLength = file.length();
                                    long current = 0;

                                    System.out.println(String.valueOf(fileLength));
                                    PrintWriter writer = clientOutputStreams.get(users.indexOf(packet[0]));
                                    writer.println(String.valueOf(fileLength));
                                    writer.println("\n");
                                    writer.flush();

                                    while (current != fileLength) {
                                        int size = 10000;
                                        if (fileLength - current >= size)
                                            current += size;
                                        else {
                                            size = (int) (fileLength - current);
                                            current = fileLength;
                                        }
                                        contents = new byte[size];
                                        bis.read(contents, 0, size);
                                        os.write(contents);
                                        textArea1.appendText("[Server] Sending file ... " + (current * 100) / fileLength + "% complete!\n");
                                    }

                                    writer.println("Go");

                                    bis.close();
                                    fis.close();
                                    textArea1.appendText("[Server] Uploading Complete\n");

                                    break;
                                default:
                                    textArea1.appendText("[Server] Can't read FileRequest Type.\n");
                                    break;
                            }
                            break;
                        default:
                            textArea1.appendText("[Server] Decoding Error: UnknownRequestType");
                            System.out.println(Arrays.toString(packet));
                            break;
                    }
                }
            } catch (Exception ex) {
                textArea1.appendText("[Server] Connection Error: HostClientLost\n");
                ex.printStackTrace();
            }
        }

        private void sendBytes(BufferedInputStream in, OutputStream out) throws Exception {
            int size = 9022386;
            byte[] data = new byte[size];
            int c = in.read(data, 0, data.length);
            out.write(data, 0, c);
            out.flush();
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
