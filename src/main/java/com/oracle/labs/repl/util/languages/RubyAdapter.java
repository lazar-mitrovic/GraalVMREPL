package com.oracle.labs.repl.util.languages;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Context.Builder;
import org.graalvm.polyglot.Value;

import java.util.function.IntSupplier;
import java.nio.file.Paths;

public class RubyAdapter implements LanguageAdapter {

    public Builder addParameters(Builder builder) {
        String tmpDir = System.getProperty("java.io.tmpdir");
        return builder.option("ruby.home", Paths.get(tmpDir, "ruby").toString());
    }

    public void putBindings(Context context, IntSupplier clear, IntSupplier exit) {
        Value bindings = context.getPolyglotBindings();
        bindings.putMember("clear", clear);
        bindings.putMember("quit", exit);
        bindings.putMember("exit", exit);
    }

    public String initCode() {
        return "def quit()  Polyglot.import('quit').call  end;" + "def exit()  Polyglot.import('exit').call  end;"
        + "def clear() Polyglot.import('clear').call end; RUBY_DESCRIPTION";
    } 
}