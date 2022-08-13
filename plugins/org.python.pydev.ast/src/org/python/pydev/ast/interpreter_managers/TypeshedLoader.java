package org.python.pydev.ast.interpreter_managers;

import java.io.File;
import java.io.IOException;
import java.lang.Runtime.Version;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import org.python.pydev.core.ISystemModulesManager;
import org.python.pydev.core.log.Log;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.Compare;
import org.python.pydev.parser.jython.ast.If;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.Num;
import org.python.pydev.parser.jython.ast.Tuple;
import org.python.pydev.parser.jython.ast.cmpopType;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.parser.jython.ast.factory.AdapterPrefs;
import org.python.pydev.parser.jython.ast.factory.PyAstFactory;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.shared_core.string.FastStringBuffer;

public class TypeshedLoader {

    public static void fillTypeshedFromDirInfo(final Map<String, File> typeshedCache, final File f,
            final String basename) {
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

    public static void fixBuiltinsAST(final SimpleNode ast, final ISystemModulesManager systemModulesManager,
            final InterpreterInfo interpreterInfo) {
        if (!(ast instanceof Module)) {
            Log.logInfo("Expected Module. Found: " + ast.getClass());
            return;
        }
        fixAST(ast, systemModulesManager, interpreterInfo);

        // None/False/True must be added as they're not there by default.
        PyAstFactory astFactory = new PyAstFactory(
                new AdapterPrefs("\n", systemModulesManager.getNature()));

        stmtType[] body = NodeUtils.getBody(ast);
        LinkedList<stmtType> bodyAsList = new LinkedList<>(Arrays.asList(body));
        // Iterator<stmtType> it = bodyAsList.iterator();
        // while (it.hasNext()) {
        //     stmtType next = it.next();
        //     if (next != null) {
        //         try {
        //         } catch (Exception e) {
        //             Log.log(e);
        //         }
        //     }
        // }

        bodyAsList.add(astFactory.createAssign(astFactory.createStoreName("None"),
                astFactory.createNone()));
        bodyAsList.add(astFactory.createAssign(astFactory.createStoreName("False"),
                astFactory.createFalse()));
        bodyAsList.add(astFactory.createAssign(astFactory.createStoreName("True"),
                astFactory.createTrue()));
        bodyAsList.add(astFactory.createAssign(astFactory.createStoreName("__builtins__"),
                astFactory.createName("Any")));

        NodeUtils.setBody(ast, bodyAsList.toArray(new stmtType[0]));
        return;
    }

    //    private void printAst(SimpleNode ast) {
    //        PrettyPrinterPrefsV2 prefs = new PrettyPrinterPrefsV2("\n", "    ", new IGrammarVersionProvider() {
    //
    //            @Override
    //            public int getGrammarVersion() throws MisconfigurationException {
    //                // TODO Auto-generated method stub
    //                return IGrammarVersionProvider.LATEST_GRAMMAR_PY3_VERSION;
    //            }
    //
    //            @Override
    //            public AdditionalGrammarVersionsToCheck getAdditionalGrammarVersions() throws MisconfigurationException {
    //                return null;
    //            }
    //        });
    //        PrettyPrinterV2 printer = new PrettyPrinterV2(prefs);
    //        try {
    //            String result = printer.print(ast);
    //            System.out.println(result);
    //        } catch (IOException e) {
    //            Log.log(e);
    //        }
    //    }

    /**
     * Updates the ast in-place to remove statements which wouldn't match due to the interpreter version.
     */
    public static void fixAST(final SimpleNode ast, final ISystemModulesManager systemModulesManager,
            final InterpreterInfo interpreterInfo) {
        final stmtType[] body = NodeUtils.getBody(ast);
        final FastStringBuffer buf = new FastStringBuffer();
        String version = interpreterInfo.getVersion();
        version += ".99";
        Version interpreterVersion = Version.parse(version);

        if (body == null || body.length == 0) {
            return;
        }

        fixBody(ast, body, interpreterVersion, buf);
    }

    private static void fixBody(final SimpleNode ast, final stmtType[] body, final Version interpreterVersion,
            final FastStringBuffer buf) {
        LinkedList<stmtType> bodyAsList = new LinkedList<>(Arrays.asList(body));
        Iterator<stmtType> it = bodyAsList.iterator();
        boolean changed = false;
        while (it.hasNext()) {
            stmtType next = it.next();
            if (next != null) {
                try {
                    if (next instanceof ClassDef) {
                        ClassDef classDef = (ClassDef) next;
                        NameTok name = (NameTok) classDef.name;
                        if (name != null) {
                            if ("_SpecialForm".equals(name.id)) {
                                classDef.body = new stmtType[0];
                                break;
                            }
                        }
                    }

                    stmtType[] body2 = NodeUtils.getBody(next);
                    if (body2 != null && body2.length > 0) {
                        fixBody(next, body2, interpreterVersion, buf);
                    }
                    if (next instanceof If) {
                        If if1 = (If) next;
                        if (if1.test instanceof Compare) {
                            Compare compare = (Compare) if1.test;
                            if (compare.left instanceof Attribute) {
                                if ("sys.version_info"
                                        .equals(NodeUtils.getFullRepresentationString(compare.left))) {
                                    if (compare.ops != null && compare.ops.length == 1) {
                                        if (compare.comparators != null && compare.comparators.length == 1) {
                                            exprType exprType = compare.comparators[0];
                                            if (exprType instanceof Tuple) {
                                                Tuple tuple = (Tuple) exprType;
                                                if (tuple.elts != null) {
                                                    buf.clear();
                                                    for (exprType e : tuple.elts) {
                                                        if (e instanceof Num) {
                                                            Num num = (Num) e;
                                                            if (!buf.isEmpty()) {
                                                                buf.append('.');
                                                            }
                                                            buf.append(num.num);
                                                        }
                                                    }
                                                    Version checkedVersion = Version.parse(buf.toString());
                                                    int op = compare.ops[0];
                                                    switch (op) {
                                                        case cmpopType.Eq:
                                                            changed = true;
                                                            it.remove();
                                                            break;
                                                        case cmpopType.NotEq:
                                                            // keep because it's comparing something as:
                                                            // (3, 8, 1, 'final', 0) with (3, 8)
                                                            break;
                                                        case cmpopType.GtE:
                                                        case cmpopType.Gt:
                                                            // same code because it's comparing something as:
                                                            // (3, 8, 1, 'final', 0) with (3, 8) we consider >= the same as >
                                                            if (interpreterVersion.compareTo(checkedVersion) < 0) {
                                                                changed = true;
                                                                it.remove();
                                                            }
                                                            break;
                                                        case cmpopType.LtE:
                                                        case cmpopType.Lt:
                                                            // same code because it's comparing something as:
                                                            // (3, 8, 1, 'final', 0) with (3, 8) we consider <= the same as <
                                                            if (interpreterVersion
                                                                    .compareTo(checkedVersion) >= 0) {
                                                                changed = true;
                                                                it.remove();
                                                            }
                                                            break;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.log(e);
                }
            }
        }

        if (changed) {
            NodeUtils.setBody(ast, bodyAsList.toArray(new stmtType[0]));
        }
    }

}
