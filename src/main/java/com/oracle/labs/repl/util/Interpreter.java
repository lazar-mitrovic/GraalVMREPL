package com.oracle.labs.repl.util;

import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Context.Builder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.IntSupplier;
import javafx.application.Platform;
import javafx.concurrent.Task;

import com.oracle.labs.repl.streams.FXInputStream;
import com.oracle.labs.repl.streams.FXOutputStream;
import com.oracle.labs.repl.util.languages.*;

public class Interpreter {
    private static Map<?, LanguageAdapter> languageImplementations = Map.of("js", new JavaScriptAdapter(), "python",
            new PythonAdapter(), "ruby", new RubyAdapter());

    private static Context polyglot = null;
    private static ArrayList<String> languages = new ArrayList<>();
    private static int languageIndex = 0;

    private static FXInputStream in;
    private static FXOutputStream out, log, err;

    private static TerminalComponent term;

    private boolean blocked;

    public Interpreter(TerminalComponent term) {

        Interpreter.term = term;
        Interpreter.in = term.in;
        Interpreter.out = term.out;
        Interpreter.log = term.log;
        Interpreter.err = term.err;

        blocked = false;

        // Unpack language files

        String tmpDir = System.getProperty("java.io.tmpdir");

        try {
            ZipUtils.unzip(Interpreter.class.getResourceAsStream("/filesystem.zip"), new File(tmpDir));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(100);
        }

        for (String lang : Engine.create().getLanguages().keySet()) {
            if (languageImplementations.containsKey(lang))
                languages.add(lang);
        }

        // Create builder

        Builder builder = Context.newBuilder().in(Interpreter.in).out(Interpreter.out).logHandler(Interpreter.log)
                .err(Interpreter.err).allowAllAccess(true);

        for (String lang : languages) {
            builder = languageImplementations.get(lang).addParameters(builder);
        }

        polyglot = builder.build();

        // Make language bindings

        IntSupplier exit = new IntSupplier() {
            @Override
            public int getAsInt() {
                Platform.exit();
                System.exit(0);
                return 0;
            }
        };

        IntSupplier clear = new IntSupplier() {
            @Override
            public int getAsInt() {
                clear();
                return 0;
            }
        };

        for (String lang : languages) {
            languageImplementations.get(lang).putBindings(polyglot, clear, exit);
        }

        // Initialize languages

        for (String lang : languages) {
            String code = languageImplementations.get(lang).initCode();
            evalInternal(code);
            nextLanguage();
        }
    }

    public void nextLanguage() {
        languageIndex = (languageIndex + 1) % languages.size();
    }

    public void clear() {
        term.clear();
    }

    public void showPrompt() {
        final String prompt = getLanguageName() + "> ";
        try {
            out.write(prompt);
        } catch (final Exception e) {
        }
    }

    public void readEvalPrint() {
        String input = in.readLine();
        
        if (input.isEmpty() || input.charAt(0) == '#') {
            // nothing to parse
            return;
        }

        StringBuilder sb = new StringBuilder(input).append('\n');
        while (!in.isEmpty()) { // processing subsequent lines while input is incomplete
            try {
                polyglot.eval(Source.newBuilder(getLanguageName(), sb.toString(), "<shell>").interactive(true)
                        .buildLiteral()).toString();
            } catch (PolyglotException e) {
                if (e.isIncompleteSource()) {
                    // read more input until we get an empty line
                    String additionalInput = in.readLine();
                    while (additionalInput != null && !additionalInput.isEmpty()) {
                        sb.append(additionalInput).append('\n');
                        additionalInput = in.readLine();
                    }
                    if (additionalInput == null) {
                        err.write("EOF reached.");
                        return;
                    }
                    // The only continuation in the while loop
                    continue;
                }
                else {
                    err.write(e.getMessage());
                }
            }
            break;
        }
        in.flush();
    }

    public void evalInternal(String code) {
        polyglot.eval(Source.newBuilder(getLanguageName(), code, "<internal>").internal(true).buildLiteral());
    }

    public void eval() {
        new Thread(new EvalTask()).start();
    }

    public Boolean isBlocked() {
        return blocked;
    }

    public Boolean isInputBlocked() {
        return in.isInputBlocked();
    }

    protected class EvalTask extends Task<Object> {
        @Override
        protected Object call() throws IOException {
            blocked = true;
            try {
                readEvalPrint();
            } catch (final PolyglotException exception) {
                err.write(exception.getMessage());
            }
            blocked = false;
            showPrompt();
            return null;
        }
    }

    @Override
    public String toString() {
        return getLanguageName();
    }

    public static String[] getLanguages() {
        return (String[]) languages.toArray();
    }

    public String getLanguageName() {
        return languages.get(languageIndex);
    }
}