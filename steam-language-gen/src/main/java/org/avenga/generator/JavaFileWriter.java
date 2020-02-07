package org.avenga.generator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class JavaFileWriter extends FileWriter {

    private static final String INDENTATION = "    ";

    private String indent = "";

    public JavaFileWriter(File file) throws IOException {
        super(file);
    }

    public void indent() {
        indent += INDENTATION;
    }

    public void unindent() {
        indent = indent.substring(INDENTATION.length());
    }

    public void writeln(String line) throws IOException {
        write(indent);
        write(line);
        writeln();
    }

    void writeln() throws IOException {
        write('\n');
    }
}
