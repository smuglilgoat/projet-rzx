package sample;

public class Controller {
    public javafx.scene.control.TextField textField1;
    public javafx.scene.control.TextField textField2;

    public void buttonClicked() {
        System.out.println(textField1.getText());
        System.out.println(textField2.getText());
    }
}
