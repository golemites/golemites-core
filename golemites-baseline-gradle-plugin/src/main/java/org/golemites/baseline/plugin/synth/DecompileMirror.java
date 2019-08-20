package org.golemites.baseline.plugin.synth;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.lang.model.element.Modifier;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import static org.objectweb.asm.ClassReader.*;
import static org.objectweb.asm.ClassReader.SKIP_CODE;
import static org.objectweb.asm.ClassReader.SKIP_DEBUG;
import static org.objectweb.asm.ClassReader.SKIP_FRAMES;

public class DecompileMirror implements Mirror {

    private final File target;
    private final File binaryOut;
    private static final Logger LOG = LoggerFactory.getLogger(DecompileMirror.class);

    public DecompileMirror(File sourceOut, File binaryOut) {
        this.target = sourceOut;
        this.binaryOut = binaryOut;
    }

    public void mirror(File f) throws IOException {
        try (JarInputStream jip = new JarInputStream(new FileInputStream(f))) {
            JarEntry entry = null;
            while ((entry = jip.getNextJarEntry()) != null) {
                if (entry.getName().endsWith(".class") && !entry.getName().endsWith("package-info.class")) {
                    LOG.warn(" + " + entry);
                    ClassReader cr = new ClassReader( jip );
                    ComponentClassVisitor visitor = new ComponentClassVisitor( );
                    cr.accept( visitor, SKIP_CODE | SKIP_DEBUG | SKIP_FRAMES );
                    visitor.write(target);
                }
            }
        }
    }

    private class ComponentClassVisitor extends ClassNode {

        private TypeSpec.Builder typeBuilder;
        private String packageName;
        private String shortName;

        ComponentClassVisitor() {
            super(Opcodes.ASM7);
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces);
            LOG.warn( " - " + name);
            packageName = name.substring(0,name.lastIndexOf("/")).replaceAll("\\/",".");
            shortName = name.substring(name.lastIndexOf("/")+1);

            typeBuilder = TypeSpec.classBuilder(shortName)
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            LOG.warn( " --- " + name);
            return super.visitMethod(access, name, descriptor, signature, exceptions);
        }

        public void write(File target) throws IOException {
            JavaFile javaFile = JavaFile.builder(packageName, typeBuilder.build()).build();
            javaFile.writeTo(target);
        }
    }
}
