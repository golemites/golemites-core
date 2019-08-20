package org.golemites.baseline.plugin.synth;

import java.io.File;
import java.io.IOException;

public interface Mirror {
    void mirror(File f) throws IOException;
}
