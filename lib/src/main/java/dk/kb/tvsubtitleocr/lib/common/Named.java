package dk.kb.tvsubtitleocr.lib.common;

public class Named implements AutoCloseable{

    private final String oldName;

    public Named(String name) {
        oldName = Thread.currentThread().getName();
        Thread.currentThread().setName(name);
    }

    @Override
    public void close() {
        Thread.currentThread().setName(oldName);
    }
}
