package repl;

import repl.util.TerminalComponent;
import repl.util.LanguageAdapter;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.control.Button;
import javafx.scene.control.SplitPane;

import java.io.IOException;
import java.time.Year;

public class Controller {

    @FXML
    private TextArea terminal;

    @FXML
    private Button interpreterButton;

    @FXML
    private Button codeButton;

    @FXML
    private Button runCodeButton;

    @FXML
    private SplitPane mainsplit;

    private TerminalComponent term;

    private LanguageAdapter[] languages;

    private int currentLangIndex = 0;

    enum GUI_STATE {
        INTERPRETER,
        CODE_EDITOR,
    }

    private GUI_STATE state = GUI_STATE.INTERPRETER; 

    public void init() {
        term = new TerminalComponent(terminal);

        languages = new LanguageAdapter[]{ 
            new LanguageAdapter("js", term), 
            new LanguageAdapter("python", term)
        };

        term.write("GraalVM REPL Prompt");
        term.write("Copyright (c) 2013-" + String.valueOf(Year.now().getValue()) + ", Oracle and/or its affiliates");
        term.write("");
        languages[currentLangIndex].showPrompt();
        terminal.requestFocus();
    }


    public void doEval() throws IOException {
        String code = term.getCurrentCode();
        term.commitCurrent();
        languages[currentLangIndex].eval(code, true);
        term.updateStreams();
    }

    public void initialize() {
        init();

        terminal.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER)
                try{doEval();} catch(Exception e) {System.err.println(e);}
        });
        mainsplit.setDividerPositions(0);

        mainsplit.getDividers().get(0).positionProperty().addListener( e -> {
            if (state == GUI_STATE.INTERPRETER)
                mainsplit.setDividerPositions(0);
        });

        interpreterButton.setOnAction(event ->{
            state = GUI_STATE.INTERPRETER;
            mainsplit.setDividerPositions(0);
        });

        codeButton.setOnAction(event ->{
            state = GUI_STATE.CODE_EDITOR;
            mainsplit.setDividerPositions(0.5);
        });
    }

}