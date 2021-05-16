package com.oracle.labs.repl.util.languages;

import java.util.function.IntSupplier;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Context.Builder;

/**
 * Interface that encapsulates polyglot language implementation.
 */
public interface LanguageAdapter {

    /**
     * Language name, as returned from: Engine.create().getLanguages().
     * @return language name
     */
    String languageName();

    /**
     * Adds context options (if they are required by language implementation).
     * @param builder
     * @return
     */
    Builder addContextOptions(Builder builder);

    /**
     * Binds clear and exit, to language function in order to make them accessible by end-user.
     * @param context
     * @param clear
     * @param exit
     */
    void putBindings(Context context, IntSupplier clear, IntSupplier exit);

    /**
     * Returns code to be executed at language initialization (usually prints version info etc).
     * @return init code in set language.
     */
    String initCode();
}