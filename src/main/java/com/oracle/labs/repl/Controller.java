package com.oracle.labs.repl;

import com.gluonhq.attach.display.DisplayService;
import com.gluonhq.attach.lifecycle.LifecycleEvent;
import com.gluonhq.attach.lifecycle.LifecycleService;
import com.gluonhq.attach.util.Platform;
import com.oracle.labs.repl.util.Interpreter;
import com.oracle.labs.repl.util.TerminalComponent;
import javafx.beans.value.ChangeListener;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.time.Year;


public class Controller {

    private static final PseudoClass LANDSCAPE = PseudoClass.getPseudoClass("landscape");

    @FXML
    private VBox mainBox;

    @FXML
    private HBox buttonsBox;

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

        if (Platform.isIOS() && !buttonsBox.getStyleClass().contains("ios")) {
            buttonsBox.getStyleClass().add("ios");
        }
        if (Platform.isIOS() && !mainBox.getStyleClass().contains("ios")) {
            mainBox.getStyleClass().add("ios");
        }
        if (DisplayService.create().map(DisplayService::hasNotch).orElse(false)) {
            if (!buttonsBox.getStyleClass().contains("notch")) {
                buttonsBox.getStyleClass().add("notch");
            }
            if (!mainBox.getStyleClass().contains("notch")) {
                mainBox.getStyleClass().add("notch");
            }
            ChangeListener<DisplayService.Notch> notchListener = (obs, oldNotch, newNotch) ->
                    applyNotch(oldNotch, newNotch);

            DisplayService.create().ifPresent(display -> {
                LifecycleService.create().ifPresent((l) -> {
                    l.addListener(LifecycleEvent.RESUME, () -> display.notchProperty().addListener(notchListener));
                    l.addListener(LifecycleEvent.PAUSE, () -> display.notchProperty().removeListener(notchListener));
                });
                display.notchProperty().addListener(notchListener);
            });
            applyNotch(null, DisplayService.create().map(display -> display.notchProperty().get())
                    .orElse(DisplayService.Notch.UNKNOWN));
        }
    }

    private void applyNotch(DisplayService.Notch oldNotch, DisplayService.Notch newNotch) {
        if (newNotch == DisplayService.Notch.BOTTOM && oldNotch != null) {
            boolean landscape = isLandscape(oldNotch);
            mainBox.pseudoClassStateChanged(LANDSCAPE, landscape);
            buttonsBox.pseudoClassStateChanged(LANDSCAPE, landscape);
        } else {
            boolean landscape = isLandscape(newNotch);
            mainBox.pseudoClassStateChanged(LANDSCAPE, landscape);
            buttonsBox.pseudoClassStateChanged(LANDSCAPE, landscape);
        }
    }

    private boolean isLandscape(DisplayService.Notch notch) {
        return notch == DisplayService.Notch.LEFT || notch == DisplayService.Notch.RIGHT;
    }
}