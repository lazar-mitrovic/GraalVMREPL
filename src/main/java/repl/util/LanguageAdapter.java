package repl.util;

import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.IntSupplier;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Context.Builder;

import javafx.application.Platform;
import javafx.concurrent.Task;

import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.Source;

import repl.util.TerminalComponent;
import repl.util.ZipUtils;

public class LanguageAdapter {

    private static Context polyglot = null;
    private static String[] languages;
    private String languageName;
    private TerminalComponent term;
    private EvalTask runningTask;
    private Boolean blocked;

    private ByteArrayOutputStream out, log, err;

    public static LanguageAdapter[] generateAdapters(final TerminalComponent term) {
        if (polyglot == null) {
            String tmpDir = System.getProperty("java.io.tmpdir"); 
            try {
                ZipUtils.unzip(LanguageAdapter.class.getResourceAsStream("/filesystem.zip"), new File(tmpDir));
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(100);
            }
            polyglot = Context.newBuilder().in(term.in).out(term.out).logHandler(term.log).err(term.err).option("ruby.home", Paths.get(tmpDir, "truffleruby").toString())
                    .allowAllAccess(true).build();
            List<String> langNames = new ArrayList<>(polyglot.getEngine().getLanguages().keySet());
            langNames.remove("llvm"); // llvm is not supported, of course
            languages = (String[]) langNames.toArray(new String[0]);
        }
        final LanguageAdapter[] LanguageAdapters = new LanguageAdapter[languages.length];

        for (int i = 0; i < languages.length; i++) {
           // System.out.println("Lang:'"+languages[i]+"'");
            LanguageAdapters[i] = new LanguageAdapter(languages[i], term);
        }
        return LanguageAdapters;
    }  

    public LanguageAdapter(final String languageName, final TerminalComponent term) {

        this.languageName = languageName;
        this.term = term;
        this.blocked = false;

        this.out = term.out;
        this.log = term.log;
        this.err = term.err;

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
        } catch (final IOException e) { e.printStackTrace();}
    }

    public void clear() {
        term.clear();
    }

    public void showPrompt() {
        final String prompt = languageName + "> ";
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
            source = Source.newBuilder(languageName, code, "<shell>").interactive(true).build();
        else    
            source = Source.create(languageName, code);

        if (longTask) {
            final EvalTask e = new EvalTask(source, visible);
            new Thread(e).start();
            return Value.asValue(null);
        }
        return polyglot.eval(source);
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

    public static String[] getLanguages() {
        return languages;
    }

}