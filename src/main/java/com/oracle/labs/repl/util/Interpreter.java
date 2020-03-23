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

        System.out.print("Unpacking language runtimes...");
        try {
            ZipUtils.unzip(Interpreter.class.getResourceAsStream("/filesystem.zip"), new File(tmpDir));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(100);
        }
        System.out.println(" Done.");

        for (String lang : Engine.create().getLanguages().keySet()) {
            if (languageImplementations.containsKey(lang))
                languages.add(lang);
        }

        if (languages.size() == 0) {
            System.err.println("No languages present!");
            System.err.println("You can add them to your GraalVM build using:");
            System.err.println("$ cd $GRAAL_SOURCE/vm");
            System.err.println("$ mx --dynamicimports /graal-js,graalpython,truffleruby build`");
            System.exit(1);
        }

        System.out.print("Creating context builder...");
        // Create builder

        Builder builder = Context.newBuilder().in(Interpreter.in).out(Interpreter.out).logHandler(Interpreter.log)
                .err(Interpreter.err).allowAllAccess(true);

        for (String lang : languages) {
            builder = languageImplementations.get(lang).addParameters(builder);
        }
        
        polyglot = builder.build();
        System.out.println(" Done.");

        // Make language bindings

        System.out.print("Adding language bindings...");
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
        System.out.println(" Done.");

        // Initialize languages

        System.out.print("Initializing languages...");
        for (String lang : languages) {
            String code = languageImplementations.get(lang).initCode();
            evalInternal(code);
            nextLanguage();
        }
        System.out.println(" Done.");
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
        String input;
        
        do {
            input = in.readLine();
        } while (!in.isEmpty() && (input.isEmpty() || input.charAt(0) == '#'));
            
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
                        sb.append(additionalInput);
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
                    e.printStackTrace();
                }
            }
            break;
        }
        System.out.println("Executed command: ");
        System.out.println(sb.toString().replace("\n","\\n"));
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