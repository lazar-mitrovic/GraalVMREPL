package repl.util;

import java.io.IOException;
import java.util.function.IntSupplier;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Context.Builder;

import javafx.application.Platform;

import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.Source;

import repl.util.TerminalComponent;

public class LanguageAdapter {

    private Context polyglot = null;
    private String languageName;

    public LanguageAdapter(String languageName, TerminalComponent term) {

        this.languageName = languageName;
        polyglot = Context.newBuilder(languageName).out(term.out).logHandler(term.log).err(term.err)
                .allowAllAccess(true).build();

        Value bindings = polyglot.getBindings(languageName);

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
                term.clear();
                return 0;
            }
        });
    }

    public Value eval(String code) throws IOException {
        return eval(code, false);
    }

    public Value eval(String code, Boolean interactive) throws IOException {
        Source source = Source.newBuilder(languageName, code, "<shell>").interactive(interactive).build();
        return polyglot.eval(source);
    }

    public String getLanguageName() {
        return languageName;
    }

    @Override
    public String toString() {
        return "LanguageAdapter [languageName=" + languageName + "]";
    }
    
}