package repl.util;

import java.io.ByteArrayOutputStream;
import java.util.Timer;
import java.util.TimerTask;

import javafx.scene.control.TextArea;

public class TerminalComponent {

    private TextArea terminal;

    private String currentCode = "";
    private String terminalText = "";
    private String oldVal = "";

    public ByteArrayOutputStream out, log, err;

    public TerminalComponent(TextArea terminal) {
        this.terminal = terminal;

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

        Timer timer = new Timer(true);
        TimerTask streamListener = new TimerTask() {
            @Override
            public void run() {
                if (!out.toString().isEmpty())
                    write(out.toString());
                out.reset();

                if (!log.toString().isEmpty())
                    write("log>" + log.toString());
                log.reset();

                if (!err.toString().isEmpty())
                    write("err>" + err.toString());
                err.reset();
            }
        };
        timer.scheduleAtFixedRate(streamListener, 100, 100);
    }

    public void write(String s) {
        write(s, "\n");
    }

    public void write(String s, String endl) {
        terminalText += s + endl;
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