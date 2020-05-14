package com.oracle.labs.repl.util.languages;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Context.Builder;
import org.graalvm.polyglot.Value;

import java.util.function.IntSupplier;
import java.nio.file.Paths;

public class PythonAdapter implements LanguageAdapter {

    public Builder addParameters(Builder builder) {
        String tmpDir = System.getProperty("java.io.tmpdir");
        return builder.option("python.CoreHome",   Paths.get(tmpDir, "python", "lib-graalpython").toString())
                      .option("python.SysPrefix",  Paths.get(tmpDir, "python").toString())
                      .option("python.CAPI",       Paths.get(tmpDir, "python", "lib-graalpython").toString())
                      .option("python.StdLibHome", Paths.get(tmpDir, "python", "lib-python", "3").toString());
    }

    public void putBindings(Context context, IntSupplier clear, IntSupplier exit) {
        Value bindings = context.getBindings("python");
        bindings.putMember("clear", clear);
        bindings.putMember("quit", exit);
        bindings.putMember("exit", exit);
    }

    public String initCode() {
        return "import sys,polyglot\n" + "print('GraalPython {}'.format(sys.version.split()[0]))";
    }
}