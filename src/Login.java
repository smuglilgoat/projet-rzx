import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;

public class Login extends Application {
    private double xOffset = 0;
    private double yOffset = 0;
    @FXML
    private ImageView miniButton;
    @FXML
    private ImageView closeButton;

    public void closeWindow() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }

    public  void minimizeWindow(){
        Stage stage = (Stage) miniButton.getScene().getWindow();
        stage.setIconified(true);
    }

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
}