package repl.util;

import java.nio.charset.StandardCharsets;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.function.IntSupplier;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Context.Builder;

import javafx.application.Platform;
import javafx.concurrent.Task;

import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.Source;

import repl.util.TerminalComponent;

public class LanguageAdapter {

    private Context polyglot = null;
    private String languageName;
    private TerminalComponent term;
    private EvalTask runningTask;
    private Boolean blocked;

    private ByteArrayOutputStream out, log, err;

    public LanguageAdapter(String languageName, TerminalComponent term) {

        this.languageName = languageName;
        this.term = term;
        this.blocked = false;

        this.out = term.out;
        this.log = term.log;
        this.err = term.err;

        polyglot = Context.newBuilder(languageName).in(term.in).out(term.out).logHandler(term.log).err(term.err)
                .allowAllAccess(true).build();

        Value bindings = polyglot.getBindings(languageName);

        bindings.putMember("quit", new IntSupplier() {
            @Override
            public int getAsInt() {
                Platform.exit();
                System.exit(0);
                return 0;
            }
        });

        bindings.putMember("exit", new IntSupplier() {
            @Override
            public int getAsInt() {
                Platform.exit();
                System.exit(0);
                return 0;
            }
        });

        bindings.putMember("clear", new IntSupplier() {
            @Override
            public int getAsInt() {
                clear();
                return 0;
            }
        });
    }

    public void clear() {
        term.clear();
    }

    public void showPrompt() {
        String prompt = languageName + ">";
        try {
            out.write(prompt.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
        }
    }

    public Value eval(String code) throws IOException {
        return eval(code, false);
    }

    public Value eval(String code, Boolean interactive) throws IOException {
        Source source = Source.newBuilder(languageName, code, "<shell>").interactive(interactive).build();

        if (interactive) {
            EvalTask e = new EvalTask(source);
            new Thread(e).start();
            return Value.asValue(null);
        }
        return polyglot.eval(source);
    }

    protected class EvalTask extends Task {
        private Source source;
        private Value result;

        public EvalTask(Source source) {
            this.source = source;
        }

        @Override
        protected Object call() throws IOException {
            blocked = true;
            try {
                result = polyglot.eval(source);
            } catch (PolyglotException exception) {
                err.write(exception.getMessage().getBytes(StandardCharsets.UTF_8));
            }
            showPrompt();
            blocked = false;
            return null;
        }

        public Value getResult() {
            return result;
        }
    }

    public String getLanguageName() {
        return languageName;
    }

    public Boolean getBlocked() {
        return blocked;
    }

    @Override
    public String toString() {
        return getLanguageName();
    }

}