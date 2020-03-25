package com.oracle.labs.repl.streams;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class FXOutputStream extends ByteArrayOutputStream {

    public void write(String s) {
        try {
            this.write(s.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeLine(String s) {
        this.write(s + System.lineSeparator());
    }

    @Override
    public String toString() {
        return super.toString(StandardCharsets.UTF_8);
    }
}