package Repl;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import java.util.ResourceBundle;

public class Controller {

    @FXML
    private TextArea terminal;

    @FXML
    private TextField currentCode;

    @FXML
    private Button evalButton;

    @FXML
    private ResourceBundle resources;

    private Context polyglot = null;

    public String eval(String code){
        if (polyglot == null)
            polyglot = Context.create("js");

        return polyglot.eval("js", code).toString();
    }

    public void TerminalWrite(String s){
        terminal.setText(terminal.getText() + "\n" + s);
    }

    public void initialize() {
        evalButton.setOnAction(e -> {
            String code = currentCode.getText();
            currentCode.setText("");
            TerminalWrite("> " + code);
            String resp = eval(code);
	    TerminalWrite(resp);
        });
    }

}
