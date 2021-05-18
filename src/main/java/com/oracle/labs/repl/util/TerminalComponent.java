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
package com.oracle.labs.repl.util;

import com.oracle.labs.repl.streams.TerminalInputStream;
import com.oracle.labs.repl.streams.TerminalOutputStream;
import javafx.application.Platform;
import javafx.scene.control.TextArea;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Class that encapsulates TextArea and adds terminal-like behaviour.
 */
public class TerminalComponent {

    private TextArea terminal;

    private String currentCode = "";
    private String terminalText = "";

    private final ArrayList<String> history;
    private int historyPosition;

    private Boolean changed;

    public TerminalOutputStream out, log, err;
    public TerminalInputStream in;

    public TerminalComponent(TextArea terminal) {
        this.terminal = terminal;
        this.changed = false;
        history = new ArrayList<>();
        historyPosition = 0;

        in = new TerminalInputStream();

        terminal.textProperty().addListener(event -> checkInvalidState());

        out = new TerminalOutputStream();
        log = new TerminalOutputStream();
        err = new TerminalOutputStream();

        Timer timer = new Timer(true);
        TimerTask streamListener = new TimerTask() {
            @Override
            public void run() {
                Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
                updateStreams();
                safeUpdate();
            }
        };
        timer.scheduleAtFixedRate(streamListener, 100, 100);
    }

    public void historyChange(int move) {
        historyPosition = Math.max(Math.min(historyPosition + move, history.size()), 0);
        if (historyPosition > 0)
            currentCode = history.get(history.size() - historyPosition);
        changed = true;
        safeUpdate();
        terminal.positionCaret(terminal.getText().length());
    }

    public synchronized void checkInvalidState() {
        String newVal = terminal.getText();

        changed = true;

        if (newVal.length() < terminalText.length() || !newVal.startsWith(terminalText)) {
            safeUpdate(); // you cannot edit output!
            return;
        }

        if (newVal.endsWith(System.lineSeparator()) && !terminalText.equals(newVal)) {
            safeUpdate(); // you cannot edit output!
            return;
        }

        currentCode = newVal.substring(terminalText.length());
        safeUpdate();
    }

    public synchronized void updateStreams() {
        if (!err.toString().isEmpty()) {
            guiWrite("err> " + err + System.lineSeparator());
            changed = true;
            err.reset();
        }

        if (!log.toString().isEmpty()) {
            guiWrite("log> " + log + System.lineSeparator());
            changed = true;
            log.reset();
        }

        if (!out.toString().isEmpty()) {
            guiWrite(out.toString());
            changed = true;
            out.reset();
        }
    }

    private synchronized void guiWrite(String s) {
        terminalText += s;
        terminalText = terminalText.substring(Math.max(terminalText.length() - 1000, 0));
        changed = true;
    }

    public synchronized void writeLine() {
        write(System.lineSeparator());
    }

    public synchronized void writeLine(String s) {
        write(s + System.lineSeparator());
    }

    public synchronized void write(String s) {
        out.write(s);
    }

    public synchronized void clear() {
        terminalText = "";
        update();
    }

    private synchronized void update() {
        if (!changed) {
            return;
        }
        String oldVal = terminalText + currentCode;
        int pos = this.fixCaretPosition();
        terminal.setText(oldVal);
        terminal.positionCaret(pos);
        terminal.setScrollTop(Double.MAX_VALUE);
        changed = false;
    }

    public void safeUpdate() {
        Platform.runLater(this::update);
    }

    public synchronized int fixCaretPosition() {
        int pos = Math.max(terminal.getCaretPosition(), terminalText.length());
        terminal.positionCaret(pos);
        return pos;
    }

    public synchronized void commitCurrent() {
        if (!currentCode.equals("")) {
            history.add(currentCode.trim());
        }
        historyPosition = 0;
        currentCode = currentCode.replace(System.lineSeparator(), "");
        writeLine(currentCode);
        in.writeLine(currentCode);
        flushCurrent();
    }

    public synchronized void flushCurrent() {
        currentCode = "";
        update();
    }

    public String getCurrentCode() {
        return currentCode;
    }

    public void setTerminal(TextArea terminal) {
        this.terminal = terminal;
    }
}
