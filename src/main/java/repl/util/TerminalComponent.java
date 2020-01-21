package repl.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import java.lang.Thread;
import java.nio.charset.StandardCharsets;

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

    public ByteArrayOutputStream out, log, err;
    public InputStream in;

    public TerminalComponent(TextArea terminal) {
        this.terminal = terminal;
        this.changed = false;
        history = new ArrayList<>();
        historyPosition = 0;

        in = createInputStream();

        terminal.setOnKeyTyped(event -> {
            String newVal = terminal.getText();
            if (newVal.equals(oldVal))
                return;
            if (!newVal.startsWith(terminalText)) {
                update(); // you cannot edit output!
                return;
            }
            currentCode = newVal.substring(terminalText.length());
            update();
        });

        terminal.setOnKeyReleased(event -> {
            if (event.getCode() == KeyCode.UP) {
                historyPosition = Math.min(historyPosition + 1, history.size());
                if (historyPosition > 0)
                    currentCode = history.get(history.size() - historyPosition);
                update();
            }
            if (event.getCode() == KeyCode.DOWN) {
                historyPosition = Math.max(historyPosition - 1, 0);
                if (historyPosition > 0)
                    currentCode = history.get(history.size() - historyPosition);
                else
                    currentCode = "";
                update();
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
            }
        };
        timer.scheduleAtFixedRate(streamListener, 100, 100);
    }

    public synchronized void updateStreams() {
        if (!err.toString().isEmpty()) {
            write("err> " + err.toString());
            changed = true;
            err.reset();
        }

        if (!log.toString().isEmpty()) {
            write("log> " + log.toString());
            changed = true;
            log.reset();
        }

        if (!out.toString().isEmpty()) {
            write(out.toString(), "");
            changed = true;
            out.reset();
        }
    }

    public void write(String s) {
        write(s, "\n");
    }

    public void write(String s, String endl) {
        synchronized (this) {
            terminalText += s + endl;
            terminalText = terminalText.substring(Math.max(terminalText.length() - 1000, 0));
            changed = true;
            update();
        }
    }

    public void clear() {
        terminalText = "";
        update();
    }

    private void update() {
        synchronized (this) {
            oldVal = terminalText + currentCode;
            int pos = this.fixCaretPosition();
            terminal.clear();
            terminal.setText(oldVal);
            terminal.positionCaret(pos);
            if (changed) {
                terminal.setScrollTop(Double.MAX_VALUE);
                changed = false;
            }
        }
    }

    public int fixCaretPosition() {
        int pos = (terminal.getCaretPosition() < terminalText.length()) ? terminalText.length()
                : terminal.getCaretPosition();
        terminal.selectRange(pos, pos);
        return pos;
    }

    public void commitCurrent(boolean input) {
        history.add(currentCode);
        write(currentCode);
        if (input) {
            flushCode = currentCode;
            synchronized (in) {
                in.notifyAll();
            }
        }
        flushCurrent();
    }

    public void flushCurrent() {
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
                            try {
                                this.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
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
}
