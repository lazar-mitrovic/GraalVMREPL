package com.oracle.labs.repl.util.languages;

import java.nio.file.Paths;
import java.util.function.IntSupplier;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Context.Builder;

import org.graalvm.polyglot.Value;

public class RubyAdapter implements LanguageAdapter {

    public String languageName() {
        return "ruby";
    }

    public Builder addContextOptions(Builder builder) {
        String tmpDir = System.getProperty("java.io.tmpdir");
        String rubyHome = Paths.get(tmpDir, "ruby").toString();
        System.setProperty("ruby.home", rubyHome);
        System.setProperty("org.graalvm.language.ruby.home", rubyHome);
        return builder; //.option("ruby.home",  rubyHome).option("log.level",  "CONFIG");
    }

    public void putBindings(Context context, IntSupplier clear, IntSupplier exit) {
        Value bindings = context.getPolyglotBindings();
        bindings.putMember("clear", clear);
        bindings.putMember("quit", exit);
        bindings.putMember("exit", exit);
    }

    public String initCode() {
        return "def quit() Polyglot.import('quit').call end;" + "def exit() Polyglot.import('exit').call end;"
        + "def clear() Polyglot.import('clear').call end; print(RUBY_ENGINE + ' (like ruby ' + RUBY_VERSION + ')\n');";
    }
}