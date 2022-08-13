package org.python.pydev.ast.interpreter_managers;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Map;

import org.python.pydev.ast.codecompletion.revisited.SystemModulesManager;
import org.python.pydev.core.log.Log;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.parser.jython.ast.factory.AdapterPrefs;
import org.python.pydev.parser.jython.ast.factory.PyAstFactory;
import org.python.pydev.shared_core.utils.ArrayUtils;

public class TypeshedLoader {

    public static void fillTypeshedFromDirInfo(Map<String, File> typeshedCache, File f, String basename) {
        java.nio.file.Path path = Paths.get(f.toURI());
        try (DirectoryStream<java.nio.file.Path> newDirectoryStream = Files.newDirectoryStream(path)) {
            Iterator<java.nio.file.Path> it = newDirectoryStream.iterator();
            while (it.hasNext()) {
                java.nio.file.Path path2 = it.next();
                File file2 = path2.toFile();
                String fName = file2.getName();
                if (file2.isDirectory()) {
                    String dirname = fName;
                    if (!dirname.contains("@")) {
                        fillTypeshedFromDirInfo(typeshedCache, file2,
                                basename.isEmpty() ? dirname + "." : basename + dirname + ".");
                    }
                } else {
                    if (fName.endsWith(".pyi")) {
                        String modName = fName.substring(0, fName.length() - (".pyi".length()));
                        typeshedCache.put(basename + modName, file2);
                    }
                }
            }
        } catch (IOException e) {
            Log.log(e);
        }
    }

    public static SimpleNode fixBuiltinsAST(SimpleNode ast, SystemModulesManager systemModulesManager,
            InterpreterInfo interpreterInfo) {
        // None/False/True must be added as they're not there by default.
        if (ast instanceof Module) {
            Module module = (Module) ast;
            PyAstFactory astFactory = new PyAstFactory(
                    new AdapterPrefs("\n", systemModulesManager.getNature()));
            module.body = ArrayUtils.concatArrays(module.body, new stmtType[] {
                    astFactory.createAssign(astFactory.createStoreName("None"),
                            astFactory.createNone()),
                    astFactory.createAssign(astFactory.createStoreName("False"),
                            astFactory.createFalse()),
                    astFactory.createAssign(astFactory.createStoreName("True"),
                            astFactory.createTrue()),
                    astFactory.createAssign(astFactory.createStoreName("__builtins__"),
                            astFactory.createName("Any"))
            });

        }
        return ast;
    }

}
