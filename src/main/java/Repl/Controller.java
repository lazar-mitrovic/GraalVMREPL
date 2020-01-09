package Repl;

import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.function.IntSupplier;
import java.time.Year;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.application.Platform;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Context.Builder;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;

import java.util.ResourceBundle;

public class Controller {

    @FXML
    private TextArea terminal;

    @FXML
    private ResourceBundle resources;

    private String currentCode = "";
    private String terminalText = "";
    private String oldVal = "";

    private Context polyglot = null;
    private ByteArrayOutputStream out, log, err;

    public void init() {
        if (polyglot == null) {
            out = new ByteArrayOutputStream();
            log = new ByteArrayOutputStream();
            err = new ByteArrayOutputStream();
            polyglot = Context.newBuilder("js").out(out).logHandler(log).err(err).allowAllAccess(true).build();

            Value bindings = polyglot.getBindings("js");
            bindings.putMember("quit", new IntSupplier() {
                @Override
                public int getAsInt() {
                    Platform.exit();
                    return 0;
                }
            });
            bindings.putMember("exit", new IntSupplier() {
                @Override
                public int getAsInt() {
                    Platform.exit();
                    return 0;
                }
            });
            bindings.putMember("clear", new IntSupplier() {
                @Override
                public int getAsInt() {
                    TerminalClear();
                    return 0;
                }
            });

            TerminalWrite("GraalVM REPL Prompt");
            TerminalWrite(
                    "Copyright (c) 2013-" + String.valueOf(Year.now().getValue()) + ", Oracle and/or its affiliates");
            TerminalWrite("");
            TerminalWrite("GraalJS " + eval("Graal.versionJS"));
            TerminalWrite("GraalVM " + eval("Graal.versionGraalVM"));
            TerminalWrite("\n", "js>");
            terminal.requestFocus();
        }
    }

    public String eval(String code) {
        String result = "", exception = "";

        out.reset();
        log.reset();
        err.reset();

        try {
            result = polyglot.eval("js", code).toString();
        } catch (PolyglotException e) {
            exception = e.getMessage();
        }
        ArrayList<String> response = new ArrayList<String>();

        String outputString = out.toString();
        if (!outputString.isEmpty()) {
            if (outputString.endsWith("\n")) // removes last newline since it will be added later
                outputString = outputString.substring(0, outputString.length() - 1);
            response.add(outputString);
        }
        if (!log.toString().isEmpty())
            response.add("log>" + log.toString());
        if (!err.toString().isEmpty())
            response.add("err>" + err.toString());
        if (!result.isEmpty())
            response.add(result);
        if (!exception.isEmpty())
            response.add(exception);

        return String.join("\n", response);
    }

    public void TerminalWrite(String s) {
        TerminalWrite(s, "\n");
    }

    public void TerminalWrite(String s, String endl) {
        terminalText += s + endl;
        TerminalUpdate();
    }

    public void TerminalClear() {
        terminalText = "";
        TerminalUpdate();
    }

    public void TerminalUpdate() {
        oldVal = terminalText + currentCode;
        int pos = (terminal.getCaretPosition() < terminalText.length()) ? oldVal.length() : terminal.getCaretPosition();
        terminal.clear();
        terminal.setText(oldVal);
        terminal.positionCaret(pos);
    }

    public void doEval() {
        TerminalWrite(currentCode);
        String resp = eval(currentCode);
        currentCode = "";
        TerminalWrite(resp + "\n", "js>");
    }

    public void initialize() {
        init();
        terminal.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                doEval();
                TerminalUpdate();
            }
        });
        terminal.setOnKeyTyped(event -> {
            String newVal = terminal.getText();
            if (newVal == oldVal)
                return;
            if (!newVal.startsWith(terminalText)) {
                TerminalUpdate(); // you cannot edit output!
                return;
            }
            currentCode = newVal.substring(terminalText.length());
            TerminalUpdate();

        });
    }

}
