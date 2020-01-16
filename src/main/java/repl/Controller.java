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
    private Button interpreterButton;

    @FXML
    private Button codeButton;

    @FXML
    private Button runCodeButton;

    @FXML
    private SplitPane mainSplit;

    @FXML
    private TextArea interpreterBox;

    @FXML
    private TextArea executionBox;

    private LanguageAdapter[] interpreterAdapters;

    private LanguageAdapter[] executionAdapters;

    private TerminalComponent interpreterComponent, executionComponent;

    final String[] languages = new String[] { "js", "python" };

    private int currentLangIndex = 0;

    enum GUI_STATE {
        INTERPRETER, CODE_EDITOR,
    }

    private GUI_STATE state = GUI_STATE.INTERPRETER;

    public void init() {
        interpreterComponent = new TerminalComponent(interpreterBox);
        executionComponent = new TerminalComponent(executionBox);

        interpreterAdapters = new LanguageAdapter[languages.length];
        executionAdapters = new LanguageAdapter[languages.length];

        for (int i = 0; i < languages.length; i++) {
            interpreterAdapters[i] = new LanguageAdapter(languages[i], interpreterComponent);
            executionAdapters[i] = new LanguageAdapter(languages[i], executionComponent);
        }

        interpreterComponent.write("GraalVM REPL Prompt");
        interpreterComponent.write(
                "Copyright (c) 2013-" + String.valueOf(Year.now().getValue()) + ", Oracle and/or its affiliates");
        interpreterComponent.write("");
        interpreterAdapters[currentLangIndex].showPrompt();
        interpreterBox.requestFocus();
    }

    public void doInterpreterEval() throws IOException {
        String code = interpreterComponent.getCurrentCode();
        interpreterComponent.commitCurrent(interpreterAdapters[currentLangIndex].getBlocked());
        if (!interpreterAdapters[currentLangIndex].getBlocked())
            interpreterAdapters[currentLangIndex].eval(code, true);
        interpreterComponent.updateStreams();
    }

    public void initialize() {
        init();

        mainSplit.setDividerPositions(0);

        interpreterBox.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER)
                try {
                    doInterpreterEval();
                } catch (Exception e) {
                    System.err.println(e);
                }
        });

        mainSplit.getDividers().get(0).positionProperty().addListener(e -> {
            if (state == GUI_STATE.INTERPRETER)
                mainSplit.setDividerPositions(0);
        });

        interpreterButton.setOnAction(event -> {
            state = GUI_STATE.INTERPRETER;
            mainSplit.setDividerPositions(0);
            interpreterBox.setVisible(true);
            executionBox.setVisible(false);
        });

        codeButton.setOnAction(event -> {
            state = GUI_STATE.CODE_EDITOR;
            mainSplit.setDividerPositions(0.5);
            interpreterBox.setVisible(false);
            executionBox.setVisible(true);
        });
    }

}
