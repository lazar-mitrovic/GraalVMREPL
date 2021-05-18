/*
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oracle.labs.repl;

import com.gluonhq.attach.display.DisplayService;
import com.gluonhq.attach.keyboard.KeyboardService;
import com.gluonhq.attach.lifecycle.LifecycleEvent;
import com.gluonhq.attach.lifecycle.LifecycleService;
import com.gluonhq.attach.util.Platform;
import com.oracle.labs.repl.util.Interpreter;
import com.oracle.labs.repl.util.TerminalComponent;
import javafx.beans.value.ChangeListener;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
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
    private VBox centerBox;
    @FXML
    private StackPane codePane;
    @FXML
    private TextArea codeBox;
    @FXML
    private Button runCodeButton;
    @FXML
    private StackPane interpreterPane;
    @FXML
    private TextArea interpreterBox;
    @FXML
    private Button switchLanguageButton;

    @FXML
    private Pane keyboardPane;

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
                "Copyright (c) 2019-" + Year.now().getValue() + ", Oracle and/or its affiliates");

        term.writeLine();

        interpreter = new Interpreter(term);

        switchLanguageButton.setOnAction(e -> {
            interpreter.nextLanguage();
            term.writeLine();
            interpreter.showPrompt();
        });

        term.writeLine();
        interpreter.showPrompt();
        interpreterBox.requestFocus();
        System.out.println("GUI init done.");
    }

    public void doInterpreterEval() {
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
        term.clear();
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
            } else if (event.getCode() == KeyCode.UP) {
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

        if (Platform.isIOS() && !mainBox.getStyleClass().contains("ios")) {
            mainBox.getStyleClass().add("ios");
        }
        if (DisplayService.create().map(DisplayService::hasNotch).orElse(false)) {
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
            mainBox.pseudoClassStateChanged(LANDSCAPE, isLandscape(oldNotch));
        } else {
            mainBox.pseudoClassStateChanged(LANDSCAPE, isLandscape(newNotch));
        }
    }

    private boolean isLandscape(DisplayService.Notch notch) {
        return notch == DisplayService.Notch.LEFT || notch == DisplayService.Notch.RIGHT;
    }

    private void initKeyboardSupport() {
        KeyboardService.create().ifPresent(k ->
                k.visibleHeightProperty().addListener((obs, ov, nv) -> {
                    double height = nv.doubleValue() == 0d ? 0d : nv.doubleValue() - mainBox.getPadding().getBottom() + 2;
                    keyboardPane.setMinHeight(height);
                    keyboardPane.setPrefHeight(height);
                    keyboardPane.setMaxHeight(height);
                }));
    }
}