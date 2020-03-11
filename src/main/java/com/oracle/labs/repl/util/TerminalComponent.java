package com.oracle.labs.repl.util;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import com.oracle.labs.repl.streams.FXInputStream;
import com.oracle.labs.repl.streams.FXOutputStream;

import java.lang.Thread;

import javafx.application.Platform;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;

public class TerminalComponent {

    private TextArea terminal;

    private String currentCode = "";
    private String terminalText = "";
    private String oldVal = "";

    private ArrayList<String> history;
    private int historyPosition;

    private Boolean changed;

    public FXOutputStream out, log, err;
    public FXInputStream in;

    public TerminalComponent(TextArea terminal) {
        this.terminal = terminal;
        this.changed = false;
        history = new ArrayList<>();
        historyPosition = 0;

        in = new FXInputStream();

        terminal.setOnKeyTyped(event -> {
            checkInvalidState();
        });

        terminal.setOnKeyReleased(event -> {
            if (event.getCode() == KeyCode.UP) {
                historyPosition = Math.min(historyPosition + 1, history.size());
                if (historyPosition > 0)
                    currentCode = history.get(history.size() - historyPosition);
                changed = true;
                safeUpdate();
            } else if (event.getCode() == KeyCode.DOWN) {
                historyPosition = Math.max(historyPosition - 1, 0);
                if (historyPosition > 0)
                    currentCode = history.get(history.size() - historyPosition);
                else
                    currentCode = "";
                changed = true;
                safeUpdate();
            }
        });

        /*terminal.anchorProperty().addListener(event -> {
            this.fixCaretPosition();
        });*/

        out = new FXOutputStream();
        log = new FXOutputStream();
        err = new FXOutputStream();

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

    public synchronized void checkInvalidState() {
        String newVal = terminal.getText();

        changed = true;

        if (newVal.length() == terminalText.length() && newVal.equals(oldVal)) {
            safeUpdate(); // you cannot edit output!
            return;
        }

        if (newVal.length() < terminalText.length() || !newVal.startsWith(terminalText)) {
            safeUpdate(); // you cannot edit output!
            return;
        }

        if (newVal.endsWith(System.lineSeparator()) && !oldVal.endsWith(System.lineSeparator())) {
            safeUpdate(); // you cannot edit output!
            return;
        }

        currentCode = newVal.substring(terminalText.length());
        safeUpdate();
    }

    public synchronized void updateStreams() {
        if (!err.toString().isEmpty()) {
            _guiWrite(System.lineSeparator() + "err> " + err);
            changed = true;
            err.reset();
        }

        if (!log.toString().isEmpty()) {
            _guiWrite(System.lineSeparator() + "log> " + log);
            changed = true;
            log.reset();
        }

        if (!out.toString().isEmpty()) {
            _guiWrite(System.lineSeparator() + out);
            changed = true;
            out.reset();
        }
    }

    private synchronized void _guiWrite(String s) {
        terminalText += s;
        terminalText = terminalText.substring(Math.max(terminalText.length() - 1000, 0));
        changed = true;
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
        if (!changed)
            return;
        oldVal = terminalText + currentCode;
        int pos = this.fixCaretPosition();
        terminal.setText(oldVal);
        terminal.positionCaret(pos);
        terminal.setScrollTop(Double.MAX_VALUE);
        changed = false;
    }

    public void safeUpdate() {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                update();
            }
        });
    }

    public synchronized int fixCaretPosition() {
        int pos = Math.max(terminal.getCaretPosition(), terminalText.length());
        terminal.positionCaret(pos);
        return pos;
    }

    public synchronized void commitCurrent() {
        history.add(currentCode);
        historyPosition = 0;
        writeLine(currentCode);
        in.write(currentCode);
        flushCurrent();
    }

    public synchronized void flushCurrent() {
        currentCode = "";
        update();
    }

    public String getCurrentCode() {
        return currentCode;
    }

    public TextArea getTerminal() {
        return terminal;
    }

    public void setTerminal(TextArea terminal) {
        this.terminal = terminal;
    }
}
