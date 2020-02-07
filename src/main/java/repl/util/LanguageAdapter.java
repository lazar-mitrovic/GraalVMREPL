package repl.util;

import java.nio.charset.StandardCharsets;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
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

    private static Context polyglot = null;
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

        if (polyglot == null)
            polyglot = Context.newBuilder().in(term.in).out(term.out).logHandler(term.log).err(term.err)
                    .allowAllAccess(true).build();

        Value bindings = polyglot.getBindings(languageName);

        if (Arrays.asList("ruby").contains(languageName)) { // Languages that don't support function binding must have
                                                            // custom code inserted
            bindings = polyglot.getPolyglotBindings();
        }

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

        try {
            if (languageName.equals("js"))
                eval("console.log(`GraalVM ${Graal.language} ${Graal.versionJS}`)", false, true);
            if (languageName.equals("ruby"))
                eval("def quit()  Polyglot.import('quit').call  end;" + "def exit()  Polyglot.import('exit').call  end;"
                        + "def clear() Polyglot.import('clear').call end; RUBY_DESCRIPTION", false, true);
            if (languageName.equals("python")) {
                eval("import sys,polyglot", false, true);
                eval("print('GraalPython {}'.format(sys.version.split()[0]))");
            }
        } catch (IOException e) { e.printStackTrace();}
    }

    public void clear() {
        term.clear();
    }

    public void showPrompt() {
        String prompt = languageName + "> ";
        try {
            out.write(prompt.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
        }
    }

    public Value eval(String code) throws IOException {
        return eval(code, false, false);
    }

    public Value eval(String code, Boolean longTask) throws IOException {
        return eval(code, longTask, true);
    }

    public Value eval(String code, Boolean longTask, Boolean visible) throws IOException {
        Source source;
        
        if (visible)
            source = Source.newBuilder(languageName, code, "<shell>").interactive(true).build();
        else    
            source = Source.create(languageName, code);

        if (longTask) {
            EvalTask e = new EvalTask(source, visible);
            new Thread(e).start();
            return Value.asValue(null);
        }
        return polyglot.eval(source);
    }

    protected class EvalTask extends Task {
        private Source source;
        private Value result;
        private boolean visible;

        public EvalTask(Source source, boolean visible) {
            this.source = source;
            this.visible = visible;
        }

        @Override
        protected Object call() throws IOException {
            blocked = true;
            try {
                result = polyglot.eval(source);
            } catch (PolyglotException exception) {
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