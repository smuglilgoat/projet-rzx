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
            chatArea.appendText("Disconnected.\n");
        } catch (Exception ex) {
            chatArea.appendText("Failed to disconnect.\n");
        }
        ((Stage) chatArea.getScene().getWindow()).close();
        incomingReader.interrupt();
        reader.close();
        writer.close();
        socket.close();
    }

    public void initConnection() {
        try {
            socket = new Socket(loginCreds[1], Integer.parseInt(loginCreds[2]));
            InputStreamReader inputStreamReader = new InputStreamReader(socket.getInputStream());
            reader = new BufferedReader(inputStreamReader);
            writer = new PrintWriter(socket.getOutputStream());
            writer.println(loginCreds[0] + ": has connected :Connect");
            writer.flush();
        } catch (Exception ex) {
            chatArea.appendText("Could not Connect ! Try Again\n");
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
                chatArea.appendText("Message was not sent.\n" + "Could not find user");
            }
        } else {
            try {
                writer.println(loginCreds[0] + ":" + msgField.getText() + ":" + "Chat");
                writer.flush();
            } catch (Exception ex) {
                chatArea.appendText("Message was not sent.\n");
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
            String[] data;
            String stream;

            try {
                while ((stream = reader.readLine()) != null) {
                    data = stream.split(":");

                    switch (data[2]) {
                        case "Connect":
                            chatArea.appendText("[" + data[0] + "] Joined the Chatroom\n");
                            users.add(data[0]);
                            playSound("E:\\Documents\\Code\\project-rzx\\src\\soundFiles\\Online_tone.wav");
                            break;
                        case "Disconnect":
                            users.remove(data[0]);
                            break;
                        case "Chat":
                            chatArea.appendText(data[0] + ": " + data[1] + "\n");
                            break;
                        case "Private":
                            chatArea.appendText("[Private] {" + data[0] + "}: " + data[1] + "\n");
                            playSound("E:\\Documents\\Code\\project-rzx\\src\\soundFiles\\PrivMsg_tone.wav");
                            break;
                        case "Request":
                            chatArea.appendText("[Server]: " + "\n" + data[1] + "\n");
                            break;
                        default:
                            chatArea.appendText("Can't identify request type\n");
                            System.out.println(Arrays.toString(data));
                            break;
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

}