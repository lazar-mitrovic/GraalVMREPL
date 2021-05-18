/*
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
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