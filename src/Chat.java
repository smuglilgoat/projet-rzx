import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXListView;
import com.jfoenix.controls.JFXTextArea;
import com.jfoenix.controls.JFXTextField;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Chat extends Application {

    public String[] loginCreds;
    ArrayList<String> users = new ArrayList<String>();
    ArrayList<String> files = new ArrayList<String>();
    private double xOffset = 0;
    private double yOffset = 0;

    Socket socket;
    BufferedReader reader;
    PrintWriter writer;
    Thread thread;

    @FXML
    private ImageView miniButton;
    @FXML
    private ImageView closeButton;
    @FXML
    private Label usernameLabel;
    @FXML
    private JFXListView<String> userList;
    @FXML
    private JFXTextField msgField;
    @FXML
    private JFXButton sendButton;
    @FXML
    private JFXButton disconnectButton;
    @FXML
    private JFXListView<String> fileList;
    @FXML
    private JFXButton downloadButton;
    @FXML
    private JFXButton uploadButton;
    @FXML
    private JFXTextArea chatArea;

    public void closeWindow() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }

    public void minimizeWindow() {
        Stage stage = (Stage) miniButton.getScene().getWindow();
        stage.setIconified(true);
    }

    public static void main(String[] args) {
        launch(args);
    }

    @FXML
    void onDisconnectButton(MouseEvent event) throws IOException {
        Disconnect();
        closeWindow();
    }

    @FXML
    void onDownloadButton(MouseEvent event) throws Exception {
        File directory = new File("E:\\Documents\\Code\\project-rzx\\out\\production\\project-rzx\\Clientfiles");
        if (!directory.exists()) {
            directory.mkdir();
        }

        File file = new File(directory, fileList.getSelectionModel().getSelectedItem());
        downloadFile(file);
        downloadButton.setDisable(true);
    }

    @FXML
    void onFileList(MouseEvent event) {
        downloadButton.setDisable(false);
    }

    @FXML
    void onKeyEnter(KeyEvent event) {
        if (event.getCode().toString().equals("ENTER")) {
            transmitMsg();
        }
    }

    private static void sendBytes(BufferedInputStream in, OutputStream out) throws Exception {
        int size = 9022386;
        byte[] data = new byte[size];
        int current = in.read(data, 0, data.length);
        out.write(data, 0, current);
        out.flush();
    }

    @FXML
    void onSendButton(MouseEvent event) {
        transmitMsg();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("Chat.fxml"));
        primaryStage.setTitle("Chat");
        primaryStage.setScene(new Scene(root));
        primaryStage.initStyle(StageStyle.UNDECORATED);

        root.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                xOffset = event.getSceneX();
                yOffset = event.getSceneY();
            }
        });

        root.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                primaryStage.setX(event.getScreenX() - xOffset);
                primaryStage.setY(event.getScreenY() - yOffset);
            }
        });

        primaryStage.show();
    }

    @FXML
    void onUploadButton(MouseEvent event) throws Exception {
        File file = new FileChooser().showOpenDialog(uploadButton.getScene().getWindow());
        chatArea.appendText("[Client] Uploading file: " + file.getPath() + "\n");
        uploadFile(file);
    }

    @FXML
    void onUserPrivate(MouseEvent event) {
        msgField.setText("@" + userList.getSelectionModel().getSelectedItem() + "@");
    }

    public void setLoginCredentials(String[] args) {
        this.loginCreds = args;
        usernameLabel.setText(args[0]);
        initConnection();
    }

    public void ListenThread() {
        thread = new Thread(new isReader());
        thread.start();
    }

    public void Disconnect() throws IOException {
        try {
            writer.println(loginCreds[0] + ": :Disconnect");
            writer.flush();
        } catch (Exception ex) {
            chatArea.appendText("[Client] Failed to disconnect.\n");
        }
        ((Stage) chatArea.getScene().getWindow()).close();
        reader.close();
        writer.close();
        socket.close();
    }

    public void initConnection() {
        try {
            socket = new Socket(loginCreds[1], Integer.parseInt(loginCreds[2]));
            InputStreamReader inputStreamReader = new InputStreamReader(socket.getInputStream());
            reader = new BufferedReader(inputStreamReader);
            writer = new PrintWriter(socket.getOutputStream(), true);
            writer.println(loginCreds[0] + ": :Connect");
            writer.flush();
        } catch (Exception ex) {
            chatArea.appendText("[Client] Could not Connect, Check info and Try Again\n");
            System.out.println(ex.toString());
        }

        ListenThread();
    }

    private void getUsers() {
        try {
            writer.println(loginCreds[0] + ":" + " " + ":" + "Request");
            writer.flush();
        } catch (Exception ex) {
            chatArea.appendText("Message was not sent.\n");
        }
    }

    public void playSound(String path) throws LineUnavailableException, UnsupportedAudioFileException {
        try {
            @SuppressWarnings("unused")
            SoundManager sound = new SoundManager(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void transmitMsg() {
        String msgFieldText = msgField.getText();
        Matcher matcher = Pattern.compile("(@).*\\1").matcher(msgFieldText);

        if (!msgFieldText.equals("")) {
            if (matcher.find()) {
                String[] data = msgFieldText.split("@");
                try {
                    writer.println(loginCreds[0] + ":" + data[2] + ":" + "Private" + ":" + data[1]);
                    writer.flush();
                    chatArea.appendText("[Private] : {" + data[2] + "} to " + data[1] + "\n");
                } catch (Exception ex) {
                    chatArea.appendText("[Client] Message was not sent: UserNotFound\n");
                }
            } else {
                try {
                    writer.println(loginCreds[0] + ":" + msgField.getText() + ":" + "Chat");
                    writer.flush();
                } catch (Exception ex) {
                    chatArea.appendText("[Client] Unknown Error: MsgNotSent.\n");
                }
            }
        }

        msgField.setText("");
        msgField.requestFocus();
    }

    public void uploadFile(File file) throws Exception {
        writer.println(loginCreds[0] + ":" + file.getName() + ":File:Upload");
        writer.flush();

        //Specify the file
        FileInputStream fis = new FileInputStream(file);
        BufferedInputStream bis = new BufferedInputStream(fis);

        //Get socket's output stream
        OutputStream os = socket.getOutputStream();

        //Read File Contents into contents array
        byte[] contents;
        long fileLength = file.length();
        long current = 0;

        long start = System.nanoTime();
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
            chatArea.appendText("[Client] Sending file ... " + (current * 100) / fileLength + "% complete!\n");
        }

        os.flush();
        bis.close();
        fis.close();
    }

    public void downloadFile(File file) throws Exception {
        writer.println(loginCreds[0] + ":" + file.getName() + ":File:Download");
        writer.flush();

        chatArea.appendText("[Client] Downloading file : " + file.getName() + "\n");

        File directory = new File("E:\\Documents\\Code\\project-rzx\\out\\production\\project-rzx\\Clientfiles\\");
        if (!directory.exists()) {
            directory.mkdir();
        }

        byte[] contents = new byte[10000];

        //Initialize the FileOutputStream to the output file's full path.
        FileOutputStream fos = new FileOutputStream("E:\\Documents\\Code\\project-rzx\\out\\production\\project-rzx\\Clientfiles\\" + file.getName());
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        InputStream is = socket.getInputStream();

        //No of bytes read in one read() call
        int bytesRead = 0;

        do {
            bytesRead = is.read(contents);
            chatArea.appendText("[Server] Read " + bytesRead + " bytes\n");
            bos.write(contents, 0, bytesRead);
        } while (bytesRead == 10000);

        bos.flush();
        bos.close();
        fos.close();
        chatArea.appendText("[Client] Downloading Complete\n");
    }

    public class isReader implements Runnable {

        @Override
        public void run() {
            String[] packet;
            String stream;
            StringBuilder stringBuilder;
            String finalString;

            try {
                while ((stream = reader.readLine()) != null) {
                    System.out.println(stream);
                    packet = stream.split(":");

                    switch (packet[2]) {
                        case "Connect":
                            chatArea.appendText("[" + packet[0] + "] Joined the Chatroom\n");
                            getUsers();
                            users.add(packet[0]);
                            playSound("E:\\Documents\\Code\\project-rzx\\src\\soundFiles\\Online_tone.wav");
                            break;
                        case "Disconnect":
                            getUsers();
                            chatArea.appendText("[" + packet[0] + "] Left the Chatroom\n");
                            users.remove(packet[0]);
                            break;
                        case "Chat":
                            chatArea.appendText(packet[0] + ": " + packet[1] + "\n");
                            break;
                        case "Private":
                            chatArea.appendText("[Private] {" + packet[0] + "}: " + packet[1] + "\n");
                            playSound("E:\\Documents\\Code\\project-rzx\\src\\soundFiles\\PrivMsg_tone.wav");
                            break;
                        case "Request":
                            String[] users = packet[1].split("/");
                            List<String> list1 = new ArrayList<String>();
                            Collections.addAll(list1, users);
                            ObservableList<String> usersList = FXCollections.observableList(list1);
                            userList.setItems(usersList);
                            userList.refresh();
                            break;
                        case "File":
                            switch (packet[3]) {
                                case "List":
                                    String[] files = packet[1].split("/");
                                    List<String> list2 = new ArrayList<String>();
                                    Collections.addAll(list2, files);
                                    ObservableList<String> filesList = FXCollections.observableList(list2);
                                    fileList.setItems(filesList);
                                    fileList.refresh();
                                    break;
                                default:
                                    chatArea.appendText("[Client] Can't read FileRequest Type.\n");
                                    break;
                            }
                            break;
                        default:
                            chatArea.appendText("[Client] Decoding Error: UnknownRequestType\n");
                            System.out.println(Arrays.toString(packet));
                            break;
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }


    }


}