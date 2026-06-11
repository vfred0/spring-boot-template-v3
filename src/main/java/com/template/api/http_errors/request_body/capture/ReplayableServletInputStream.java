package com.template.api.http_errors.request_body.capture;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;

import java.io.IOException;
import java.io.InputStream;

public class ReplayableServletInputStream extends ServletInputStream {

    private final InputStream source;

    public ReplayableServletInputStream(InputStream source) {
        this.source = source;
    }

    @Override public int read() throws IOException { return source.read(); }
    @Override public int read(byte[] b, int off, int len) throws IOException { return source.read(b, off, len); }
    @Override public boolean isFinished() { try { return source.available() == 0; } catch (IOException e) { return true; } }
    @Override public boolean isReady() { return true; }
    @Override public void setReadListener(ReadListener listener) {}
    @Override public void close() throws IOException { source.close(); }
}
