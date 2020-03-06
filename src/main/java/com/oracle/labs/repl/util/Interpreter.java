package com.oracle.labs.repl.util;

import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Context.Builder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.IntSupplier;
import javafx.application.Platform;
import javafx.concurrent.Task;

import com.oracle.labs.repl.util.TerminalComponent;
import com.oracle.labs.repl.util.ZipUtils;

import com.oracle.labs.repl.util.languages.*;

public class Interpreter {
    private static Map<?, LanguageAdapter> languageImplementations 
                = Map.of("js", new JavaScriptAdapter(), "python", new PythonAdapter(), "ruby", new RubyAdapter());

    private static Context polyglot = null;
    private static ArrayList<String> languages = new ArrayList<>();
    private static int languageIndex = 0;
    private static ByteArrayOutputStream out, log, err;
    
    private static TerminalComponent term;
    private static Boolean blocked = false;

    public Interpreter(TerminalComponent term) {

        Interpreter.term = term;
        Interpreter.out = term.out;
        Interpreter.log = term.log;
        Interpreter.err = term.err;
        
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

        Builder builder = Context.newBuilder().in(term.in).out(term.out).logHandler(term.log)
                .err(term.err).allowAllAccess(true);
        
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
            try {
                eval(code, false, true);
            } catch (IOException e) {
                e.printStackTrace();
            }
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
            out.write(prompt.getBytes(StandardCharsets.UTF_8));
        } catch (final Exception e) {
        }
    }

    public Value eval(final String code) throws IOException {
        return eval(code, false, false);
    }

    public Value eval(final String code, final Boolean longTask) throws IOException {
        return eval(code, longTask, true);
    }

    public Value eval(final String code, final Boolean longTask, final Boolean visible) throws IOException {
        Source source;
        
        if (visible)
            source = Source.newBuilder(getLanguageName(), code, "<shell>").interactive(true).build();
        else    
            source = Source.create(getLanguageName(), code);

        if (longTask) {
            final EvalTask e = new EvalTask(source, visible);
            new Thread(e).start();
            return Value.asValue(null);
        }
        return polyglot.eval(source);
    }

    public Boolean getBlocked() {
        return blocked;
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



    protected class EvalTask extends Task {
        private final Source source;
        private Value result;
        private final boolean visible;

        public EvalTask(final Source source, final boolean visible) {
            this.source = source;
            this.visible = visible;
        }

        @Override
        protected Object call() throws IOException {
            blocked = true;
            try {
                result = polyglot.eval(source);
            } catch (final PolyglotException exception) {
                err.write(exception.getMessage().getBytes(StandardCharsets.UTF_8));
            }
            if (visible)
                showPrompt();
            blocked = false;
            return null;
        }

        public Value getResult() {
            return result;
        }
    }
}