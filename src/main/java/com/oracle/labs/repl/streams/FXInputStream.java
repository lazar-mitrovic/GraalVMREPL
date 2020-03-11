package com.oracle.labs.repl.streams;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class FXInputStream extends InputStream {
    private byte[] buffer;
    private int pos;
    private StringBuilder flushString;

    private boolean inputBlocked = false;

    public FXInputStream() {
        super();
        flushString = new StringBuilder();
        pos = -1;
        buffer = null;
    }

    @Override
    public int read() throws IOException {
        if (buffer == null || pos == buffer.length) {
            pos = 0;

            while (flushString.length() == 0) {
                inputBlocked = true;
                try {
                    System.out.println("usao u blok");
                    this.wait();
                    System.out.println("izasao iz bloka");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            synchronized (this) {
                inputBlocked = false;
                buffer = flushString.toString().getBytes(StandardCharsets.UTF_8);
                flushString.setLength(0);
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
                if (c == '\n')
                    break;
                s.append(c);
            } while (c != -1);
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
};