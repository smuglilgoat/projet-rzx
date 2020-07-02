import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.stage.StageStyle;


import java.io.IOException;

public class Login extends Application {
    private double xOffset = 0;
    private double yOffset = 0;

    public void closeWindow(){
        System.exit(0);
    @FXML
    private TextField userNameField;

    @FXML
    private TextField ipAdressField;

    @FXML
    private TextField portField;

    @FXML
    private Button connectButton;

    @FXML
    void onButtonConnect() throws IOException {
        Stage currentStage = (Stage) connectButton.getScene().getWindow();
        currentStage.close();

        FXMLLoader chatSceneLoader = new FXMLLoader(getClass().getResource("Chat.fxml"));
        Stage chatStage = new Stage();
        chatStage.setScene(new Scene(chatSceneLoader.load()));
        chatStage.setTitle("XenoTalk");

        Chat chatSceneController = chatSceneLoader.<Chat>getController();
        chatSceneController.setLoginCredentials(new String[]{userNameField.getText(), ipAdressField.getText(), portField.getText()});
        chatStage.setOnHiding(event -> {
            try {
                chatSceneController.Disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        chatStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("Login.fxml"));
        primaryStage.setTitle("Login");
        primaryStage.setScene(new Scene(root));
        primaryStage.initStyle(StageStyle.UNDECORATED);

        //grab your root here
        root.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                xOffset = event.getSceneX();
                yOffset = event.getSceneY();
            }
        });

        //move around here
        root.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                primaryStage.setX(event.getScreenX() - xOffset);
                primaryStage.setY(event.getScreenY() - yOffset);
            }
        });

        primaryStage.show();
    }
}