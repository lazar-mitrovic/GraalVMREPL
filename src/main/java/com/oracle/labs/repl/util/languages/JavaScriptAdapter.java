package com.oracle.labs.repl.util.languages;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Context.Builder;
import org.graalvm.polyglot.Value;

import java.util.function.IntSupplier;

public class JavaScriptAdapter implements LanguageAdapter {

    public String languageName() {
        return "js";
    }

    public Builder addContextOptions(Builder builder) {
        return builder.option("engine.WarnInterpreterOnly", "false");
    }

    public void putBindings(Context context, IntSupplier clear, IntSupplier exit) {
        Value bindings = context.getBindings("js");
        bindings.putMember("clear", clear);
        bindings.putMember("quit", exit);
        bindings.putMember("exit", exit);
    }

    public String initCode() {
        return "console.log(`GraalVM ${Graal.language} ${Graal.versionGraalVM}`)";
    }
}