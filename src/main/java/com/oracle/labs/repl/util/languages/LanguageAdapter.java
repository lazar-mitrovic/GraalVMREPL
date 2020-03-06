package com.oracle.labs.repl.util.languages;

import java.util.HashMap;
import java.util.function.IntSupplier;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Context.Builder;

public interface LanguageAdapter {

    public Builder addParameters(Builder builder);
    public void putBindings(Context context, IntSupplier clear, IntSupplier exit);
    public String initCode();
    
}