package repl.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import java.lang.Thread;
import java.nio.charset.StandardCharsets;

import javafx.application.Platform;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;

public class TerminalComponent {

    private TextArea terminal;

    private String currentCode = "";
    private String flushCode = null;

    private String terminalText = "";
    private String oldVal = "";

    private ArrayList<String> history;
    private int historyPosition;

    private Boolean changed;
    private Boolean inputBlocked;

    public ByteArrayOutputStream out, log, err;
    public InputStream in;

    public TerminalComponent(TextArea terminal) {
        this.terminal = terminal;
        this.changed = false;
        history = new ArrayList<>();
        historyPosition = 0;
        inputBlocked = false;

        in = createInputStream();

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

        terminal.anchorProperty().addListener(event -> {
            this.fixCaretPosition();
        });

        out = new ByteArrayOutputStream();
        log = new ByteArrayOutputStream();
        err = new ByteArrayOutputStream();

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
            write(System.lineSeparator() + "err> " + err.toString(StandardCharsets.UTF_8), false);
            changed = true;
            err.reset();
        }

        if (!log.toString().isEmpty()) {
            write(System.lineSeparator() + "log> " + log.toString(StandardCharsets.UTF_8), false);
            changed = true;
            log.reset();
        }

        if (!out.toString().isEmpty()) {
            write(System.lineSeparator() + out.toString(StandardCharsets.UTF_8), false);
            changed = true;
            out.reset();
        }
    }

    public synchronized void write(String s) {
        write(s, true);
    }

    public synchronized void writeLine(String s) {
        writeLine(s, true);
    }

    public synchronized void writeLine(String s, boolean update) {
        write(s + System.lineSeparator(), true);
    }

    public synchronized void write(String s, boolean update) {
        terminalText += s;
        terminalText = terminalText.substring(Math.max(terminalText.length() - 1000, 0));
        changed = true;
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
        terminal.selectRange(pos, pos);
        return pos;
    }

    public synchronized void commitCurrent() {
        history.add(currentCode);
        historyPosition = 0;
        write(currentCode, false);
        if (inputBlocked) {
            flushCode = currentCode;
            synchronized (in) {
                in.notifyAll();
            }
        }
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

    private InputStream createInputStream() {
        return new InputStream() {
            byte[] buffer = null;
            int pos = -1;

            @Override
            public int read() throws IOException {
                if (buffer == null) {
                    pos = 0;
                    synchronized (this) {
                        while (flushCode == null) {
                            inputBlocked = true;
                            try {
                                this.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        inputBlocked = false;
                    }
                    buffer = flushCode.getBytes(StandardCharsets.UTF_8);
                    flushCode = null;
                }
                if (pos == buffer.length) {
                    buffer = null;
                    return '\n';
                } else {
                    return buffer[pos++];
                }
            }
        };
    }

    public Boolean isInputBlocked() {
        return inputBlocked;
    }
}
