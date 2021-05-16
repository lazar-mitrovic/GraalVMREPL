package com.oracle.labs.repl.streams;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Custom InputStream implementation that supports thread-safe reading and writing of String objects.
 */
public class TerminalInputStream extends InputStream {
    private byte[] buffer;
    private int pos;
    private final StringBuilder flushString;

    private boolean inputBlocked = false;

    public TerminalInputStream() {
        super();
        flushString = new StringBuilder();
        pos = -1;
        buffer = null;
    }

    @Override
    public int read() throws IOException {
        if (buffer == null || pos >= buffer.length) {

            while (flushString.length() == 0) {
                synchronized (this) {
                    inputBlocked = true;
                    try {
                        this.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            synchronized (this) {
                inputBlocked = false;
                buffer = flushString.toString().getBytes(StandardCharsets.UTF_8);
                flushString.setLength(0);
                pos = 0;
            }
        }
        return buffer[pos++];
    }

    public String readLine() {
        try {
            char c;
            StringBuilder s = new StringBuilder();
            do {
                c = (char) this.read();
                if (c == '\r' || c == '\n')
                    break;
                s.append(c);
            } while (c != ((char) -1));
            return s.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void write(String s) {
        synchronized (this) {
            flushString.append(s);
            this.notifyAll();
        }
    }

    public void writeLine(String s) {
        write(s + System.lineSeparator());
    }

    public Boolean isInputBlocked() {
        return inputBlocked;
    }

    public void flush() {
        synchronized (this) {
            buffer = null;
            flushString.setLength(0);
            pos = 0;
        }
    }

    public Boolean isEmpty() {
        return flushString.length() == 0 && (buffer == null || pos == buffer.length);
    }
}