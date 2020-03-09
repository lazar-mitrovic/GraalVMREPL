package com.oracle.labs.repl;

import com.oracle.labs.repl.util.TerminalComponent;
import com.oracle.labs.repl.util.Interpreter;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.control.Button;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ComboBox;

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
    private TextArea codeBox;

    @FXML
    private TextArea interpreterBox;

    @FXML
    private Button switchLanguageButton;

    private Interpreter interpreter;

    private TerminalComponent term;

    private int currentLangIndex = 0;

    enum GUI_STATE {
        INTERPRETER, CODE_EDITOR,
    }

    private GUI_STATE state = GUI_STATE.INTERPRETER;

    public void init() {
        term = new TerminalComponent(interpreterBox);

        term.writeLine("GraalVM REPL Prompt");
        term.writeLine(
                "Copyright (c) 2013-" + String.valueOf(Year.now().getValue()) + ", Oracle and/or its affiliates");

        interpreter = new Interpreter(term);

        switchLanguageButton.setOnAction(e -> {
            interpreter.nextLanguage();
            interpreter.showPrompt();
        });

        interpreter.showPrompt();
        interpreterBox.requestFocus();
    }

    public void doInterpreterEval() throws IOException {
        if (interpreter.getBlocked() && !term.isInputBlocked()) {
            term.flushCurrent();
            return;
        }
        final String code = term.getCurrentCode();
        term.commitCurrent();
        if (!interpreter.getBlocked())
            interpreter.eval(code, true);
        term.updateStreams();
    }

    public void doExecutionEval() throws IOException {
        final String code = codeBox.getText();
        interpreter.eval(code, true);
        term.updateStreams();
    }

    public void initialize() {
        init();

        mainSplit.setDividerPositions(0);

        interpreterBox.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                try {
                    doInterpreterEval();
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            } else if (event.getCode() == KeyCode.ESCAPE) // Keyboard turned off.
                interpreterBox.getParent().requestFocus();
            else
                term.checkInvalidState();
        });

        codeBox.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) // Keyboard turned off.
                codeBox.getParent().requestFocus();
        });

        mainSplit.getDividers().get(0).positionProperty().addListener(e -> {
            if (state == GUI_STATE.INTERPRETER)
                mainSplit.setDividerPositions(0);
        });

        interpreterButton.setOnAction(event -> {
            state = GUI_STATE.INTERPRETER;
            mainSplit.setDividerPositions(0);
        });

        codeButton.setOnAction(event -> {
            state = GUI_STATE.CODE_EDITOR;
            mainSplit.setDividerPositions(0.5);
        });

        runCodeButton.setOnAction(event -> {
            try {
                doExecutionEval();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}