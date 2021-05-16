package com.oracle.labs.repl.util;

import com.oracle.labs.repl.util.languages.LanguageAdapter;
import javafx.application.Platform;
import javafx.concurrent.Task;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Context.Builder;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.function.IntSupplier;

/**
 * Singleton class that provides interpreter access trough TerminalComponent.
 */
public class Interpreter {
    private static final String[] ALL_LANGUAGES = {"js", "python", "ruby", "R"};

    private Map<String, LanguageAdapter> languageImplementations;
    private List<String> availableLanguages;

    private Context polyglot;
    private int languageIndex = 0;

    private static TerminalComponent term;

    private boolean blocked; // is interpreter waiting for previous command execution?

    public Interpreter(final TerminalComponent term) {
        Interpreter.term = term;
        blocked = false;

        // Unpack language files
        System.out.print("Unpacking language runtimes...");

        final String tmpDir = System.getProperty("java.io.tmpdir");

        try {
            ZipUtils.unzip(this.getClass().getResourceAsStream("/filesystem.zip"), new File(tmpDir));
        } catch (final IOException e) {
            e.printStackTrace();
            System.exit(100);
        }
        System.out.println("Done.");

        final var engineLangList = Engine.create().getLanguages().keySet();

        languageImplementations = new HashMap<>();

        ServiceLoader<LanguageAdapter> loader = ServiceLoader.load(LanguageAdapter.class);
        loader.iterator().forEachRemaining(languageAdapter -> {
            if (engineLangList.contains(languageAdapter.languageName())) {
                languageImplementations.put(languageAdapter.languageName(), languageAdapter);
            }
        });

        availableLanguages = new ArrayList<>();
        Arrays.stream(ALL_LANGUAGES).filter(languageImplementations.keySet()::contains)
                .forEach(availableLanguages::add);

        if (availableLanguages.size() == 0) {
            System.err.println("No languages present!");
            System.err.println("You can add them to your GraalVM build using:");
            System.err.println("$ gu install <lang-name>");
            System.exit(1);
        }

        // Create builder
        System.out.print("Creating context builder...");

        Builder builder = Context.newBuilder().in(term.in).out(term.out).logHandler(term.log)
                .err(term.err).allowAllAccess(true);

        for (LanguageAdapter language : languageImplementations.values()) {
            language.addContextOptions(builder);
        }

        polyglot = builder.build();
        System.out.println("Done.");

        // Now, add language bindings
        System.out.print("Adding language bindings...");

        final IntSupplier exit = () -> {
            Platform.exit();
            System.exit(0);
            return 0;
        };

        final IntSupplier clear = () -> {
            term.clear();
            return 0;
        };

        for (LanguageAdapter language : languageImplementations.values()) {
            language.putBindings(polyglot, clear, exit);
        }
        System.out.println("Done.");

        // Initialize languages
        System.out.print("Initializing languages...");

        for (LanguageAdapter language : languageImplementations.values()) {
            evalInternal(language.initCode());
            nextLanguage();
        }
        System.out.println("Done.");
    }

    public void nextLanguage() {
        languageIndex = (languageIndex + 1) % languageImplementations.size();
    }

    public void showPrompt() {
        final String prompt = getLanguageName() + "> ";
        term.out.write(prompt);
    }

    public void readEvalPrint() {
        String input;
        do {
            input = term.in.readLine();
        } while (!term.in.isEmpty() && (input.isEmpty() || input.charAt(0) == '#'));

        if (input.isEmpty()) {
            return;
        }

        final StringBuilder sb = new StringBuilder(input).append('\n');
        while (true) { // processing subsequent lines while input is incomplete
            try {
                polyglot.eval(Source.newBuilder(getLanguageName(), sb.toString(), "<shell>").interactive(true)
                        .buildLiteral());
            } catch (final PolyglotException e) {
                if (e.isIncompleteSource()) {
                    // read more input until we get an empty line
                    term.out.write("... ");
                    String additionalInput = term.in.readLine();
                    while (additionalInput != null && !additionalInput.isEmpty()) {
                        sb.append(additionalInput).append("\n");
                        term.out.write("... ");
                        additionalInput = term.in.readLine();
                    }
                    if (additionalInput == null) {
                        term.err.write("EOF reached.");
                        return;
                    }
                    // The only continuation in the while loop
                    continue;
                } else {
                    term.err.write(getPolyglotException(e));
                }
            }
            break;
        }
        term.in.flush();
    }

    public void evalInternal(final String code) {
        polyglot.eval(Source.newBuilder(getLanguageName(), code, "<internal>").internal(true).buildLiteral());
    }

    public void eval() {
        new Thread(new EvalTask()).start();
    }

    public void evalCode(final String code) {
        new Thread(new EvalTask(code)).start();
    }

    public Boolean isBlocked() {
        return blocked;
    }

    public Boolean isInputBlocked() {
        return term.in.isInputBlocked();
    }

    protected class EvalTask extends Task<Object> {

        boolean interpreter;
        String code;

        EvalTask() {
            this.interpreter = true;
        }

        EvalTask(final String code) {
            this.interpreter = false;
            this.code = code;
        }

        @Override
        protected Object call() throws IOException {
            blocked = true;
            try {
                if (interpreter) {
                    readEvalPrint();
                } else {
                    final Source source = Source.newBuilder(getLanguageName(), code, "<shell>").build();
                    polyglot.eval(source);
                }

            } catch (final PolyglotException e) {
                term.err.write(getPolyglotException(e));
            }
            blocked = false;
            showPrompt();
            return null;
        }
    }

    public String getPolyglotException(PolyglotException e) {
        return e.getLocalizedMessage() + "\n\tat \"" + e.getSourceLocation().getCharacters() +
                "\" (" + e.getSourceLocation().getStartLine() + ":" + e.getSourceLocation().getStartColumn() + ")";
    }

    @Override
    public String toString() {
        return getLanguageName();
    }

    public String getLanguageName() {
        return availableLanguages.get(languageIndex);
    }
}