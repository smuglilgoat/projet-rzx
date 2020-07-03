import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Chat extends Application {

    public String[] loginCreds;
    ArrayList<String> users = new ArrayList<String>();

    Socket socket;
    BufferedReader reader;
    PrintWriter writer;
    Thread incomingReader;

    @FXML
    private TextArea chatArea;
    @FXML
    private TextField msgField;

    public static void main(String[] args) {
        launch(args);
    }

    @FXML
    void onKeyEnter(KeyEvent event) {
        if (event.getCode().toString().equals("ENTER")) {
            transmitMsg();
        }
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("Chat.fxml"));
        primaryStage.setTitle("Chat");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }

    public void setLoginCredentials(String[] args) {
        this.loginCreds = args;
        initConnection();
    }

    public void ListenThread() {
        incomingReader = new Thread(new isReader());
        incomingReader.start();
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
        incomingReader.interrupt();
    }

    public void initConnection() {
        try {
            socket = new Socket(loginCreds[1], Integer.parseInt(loginCreds[2]));
            InputStreamReader inputStreamReader = new InputStreamReader(socket.getInputStream());
            reader = new BufferedReader(inputStreamReader);
            writer = new PrintWriter(socket.getOutputStream());
            writer.println(loginCreds[0] + ": :Connect");
            writer.flush();
        } catch (Exception ex) {
            chatArea.appendText("[Client] Could not Connect, Check info and Try Again\n");
            System.out.println(ex.toString());
        }

        ListenThread();
    }

    private void btnonlineusersActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnonlineusersActionPerformed
        chatArea.appendText("Online users:\n");
        try {
            writer.println(loginCreds[0] + ":" + " " + ":" + "Request");
            writer.flush(); // flushes the buffer
        } catch (Exception ex) {
            chatArea.appendText("Message was not sent.\n");
        }
    }

    public void transmitMsg() {
        String msgFieldText = msgField.getText();
        Matcher matcher = Pattern.compile("(@).*\\1").matcher(msgFieldText);

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

        msgField.setText("");
        msgField.requestFocus();
    }

    public void playSound(String path) throws LineUnavailableException, UnsupportedAudioFileException {
        try {
            @SuppressWarnings("unused")
            SoundManager sound = new SoundManager(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public class isReader implements Runnable {

        @Override
        public void run() {
            String[] packet;
            String stream;

            try {
                while ((stream = reader.readLine()) != null) {
                    packet = stream.split(":");

                    switch (packet[2]) {
                        case "Connect":
                            chatArea.appendText("[" + packet[0] + "] Joined the Chatroom\n");
                            users.add(packet[0]);
                            playSound("E:\\Documents\\Code\\project-rzx\\src\\soundFiles\\Online_tone.wav");
                            break;
                        case "Disconnect":
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
                            chatArea.appendText("[Server]: " + "\n" + packet[1] + "\n");
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