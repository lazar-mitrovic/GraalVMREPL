package com.oracle.labs.repl;

import com.gluonhq.attach.display.DisplayService;
import com.gluonhq.attach.keyboard.KeyboardService;
import com.gluonhq.attach.lifecycle.LifecycleEvent;
import com.gluonhq.attach.lifecycle.LifecycleService;
import com.gluonhq.attach.util.Platform;
import com.oracle.labs.repl.util.Interpreter;
import com.oracle.labs.repl.util.TerminalComponent;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Path;
import javafx.scene.shape.Rectangle;

import java.io.IOException;
import java.time.Year;

public class Controller {

    private static final PseudoClass LANDSCAPE = PseudoClass.getPseudoClass("landscape");

    @FXML private BorderPane mainBox;

    @FXML private HBox buttonsBox;
    @FXML private Button interpreterButton;
    @FXML private Button codeButton;

    @FXML private VBox centerBox;
    @FXML private StackPane codePane;
    @FXML private TextArea codeBox;
    @FXML private Button runCodeButton;
    @FXML private StackPane interpreterPane;
    @FXML private TextArea interpreterBox;
    @FXML private Rectangle caret;
    @FXML private Button switchLanguageButton;

    private Interpreter interpreter;
    private TerminalComponent term;

    enum GUI_STATE {
        INTERPRETER, CODE_EDITOR,
    }

    private GUI_STATE state;

    public void init() {
        term = new TerminalComponent(interpreterBox);

        term.writeLine("GraalVM REPL Prompt");
        term.writeLine(
                "Copyright (c) 2013-" + String.valueOf(Year.now().getValue()) + ", Oracle and/or its affiliates");

        term.writeLine("");

        interpreter = new Interpreter(term);

        switchLanguageButton.setOnAction(e -> {
            interpreter.nextLanguage();
            term.writeLine("");
            interpreter.showPrompt();
        });

        term.writeLine("");
        interpreter.showPrompt();
        interpreterBox.requestFocus();
        System.out.println("GUI init done.");
    }

    public void doInterpreterEval() throws IOException {
        term.updateStreams();
        if (!interpreter.isBlocked() || interpreter.isInputBlocked()) {
            term.in.flush();
            term.commitCurrent();
            if (!interpreter.isBlocked())
                interpreter.eval();
        }
        term.updateStreams();
    }

    public void doExecutionEval() throws IOException {
        String code = codeBox.getText();
        term.writeLine("");
        interpreter.evalCode(code);
        term.updateStreams();
    }

    public void initialize() {
        init();

        interpreterBox.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                try {
                    doInterpreterEval();
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            } else if (event.getCode() == KeyCode.ESCAPE) {
                event.consume();
                interpreterBox.getParent().requestFocus();
            }
            else if (event.getCode() == KeyCode.UP) {
                event.consume();
                term.historyChange(+1);
            } else if (event.getCode() == KeyCode.DOWN) {
                event.consume();
                term.historyChange(-1);
            } else
                term.checkInvalidState();
        });


        codeBox.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) // Keyboard turned off.
                codeBox.getParent().requestFocus();
        });

        interpreterButton.setOnAction(event -> {
            state = GUI_STATE.INTERPRETER;
            setState(state);
        });

        codeButton.setOnAction(event -> {
            state = GUI_STATE.CODE_EDITOR;
            setState(state);
        });

        runCodeButton.setOnAction(event -> {
            try {
                doExecutionEval();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        initKeyboardSupport();

        initIosNotch();
        codeBox.setOnMouseClicked(e -> {
            if (interpreterBox.isFocused()) {
                buttonsBox.requestFocus();
            }
        });

        interpreterButton.fire();
    }

    private void setState(GUI_STATE state) {
        codePane.setVisible(state == GUI_STATE.CODE_EDITOR);
        codePane.setManaged(state == GUI_STATE.CODE_EDITOR);
    }

    private void initIosNotch() {

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
            ChangeListener<DisplayService.Notch> notchListener = (obs, oldNotch, newNotch) -> applyNotch(oldNotch,
                    newNotch);

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

    private void initKeyboardSupport() {
        if (Platform.isDesktop()) {
            return;
        }
        caret.setManaged(false);
        caret.setLayoutX(0);

        interpreterBox.skinProperty().addListener(new InvalidationListener() {
            @Override
            public void invalidated(Observable observable) {
                if (interpreterBox.getSkin() != null) {
                    Region content = (Region) ((ScrollPane) interpreterBox.getChildrenUnmodifiable().get(0)).getContent();
                    Path caretPath = content.getChildrenUnmodifiable().stream()
                            .filter(Path.class::isInstance)
                            .map(Path.class::cast)
                            .findFirst()
                            .orElse(new Path());
                    caretPath.boundsInParentProperty().addListener((obs, ov, nv) ->
                            caret.setLayoutY(nv.getMinY()));
                    codeBox.focusedProperty().addListener((obs, ov, nv) -> {
                        if (nv) {
                            caret.setLayoutY(0d);
                        }
                    });
                    interpreterBox.focusedProperty().addListener((obs, ov, nv) ->
                            caret.setLayoutY(!nv ? 0d : caretPath.getBoundsInParent().getMinY()));
                    interpreterButton.requestFocus();
                    KeyboardService.create().ifPresent(k -> k.keepVisibilityForNode(caret, centerBox));
                    interpreterBox.skinProperty().removeListener(this);
                }
            }
        });
    }
}