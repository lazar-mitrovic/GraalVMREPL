package repl.util;

import java.io.ByteArrayOutputStream;
import java.util.Timer;
import java.util.TimerTask;

import java.lang.Thread;

import javafx.scene.control.TextArea;

public class TerminalComponent {

    private TextArea terminal;

    private String currentCode = "";
    private String terminalText = "";
    private String oldVal = "";

    private Boolean changed;

    public ByteArrayOutputStream out, log, err;

    public TerminalComponent(TextArea terminal) {
        this.terminal = terminal;
        this.changed = false;

        terminal.setOnKeyTyped(event -> {
            String newVal = terminal.getText();
            if (newVal == oldVal)
                return;
            if (!newVal.startsWith(terminalText)) {
                update(); // you cannot edit output!
                return;
            }
            currentCode = newVal.substring(terminalText.length());
            update();

        });

        out = new ByteArrayOutputStream();
        log = new ByteArrayOutputStream();
        err = new ByteArrayOutputStream();

        Timer timer = new Timer(false);
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
            write("err>" + err.toString());
            changed = true;
            err.reset();
        }

        if (!log.toString().isEmpty()) {
            write("log>" + log.toString());
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
        terminalText += s + endl;
        terminalText = terminalText.substring(Math.max(terminalText.length() - 1000, 0));
        changed = true;
        update();
    }

    public void clear() {
        terminalText = "";
        update();
    }

    private void update() {
        oldVal = terminalText + currentCode;
        int pos = (terminal.getCaretPosition() < terminalText.length()) ? oldVal.length() : terminal.getCaretPosition();
        terminal.clear();
        terminal.setText(oldVal);
        terminal.positionCaret(pos);
        if (changed) {
            terminal.setScrollTop(Double.MAX_VALUE);
            changed = false;
        }
    }

    public void commitCurrent() {
        write(currentCode);
        flushCurrent();
    }

    public void flushCurrent() {
        currentCode = "";
        update();
    }

    public String getCurrentCode() {
        return currentCode;
    }

}
