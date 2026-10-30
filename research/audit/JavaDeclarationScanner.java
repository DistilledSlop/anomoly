import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/** Parse-only declaration inventory. Input is a newline-delimited list of Java paths. */
public final class JavaDeclarationScanner {
    public static void main(String[] args) throws IOException {
        if (args.length != 1) throw new IllegalArgumentException("usage: JavaDeclarationScanner file-list.txt");
        List<String> paths = Files.readAllLines(Path.of(args[0])).stream().filter(s -> !s.isBlank()).toList();
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager manager = compiler.getStandardFileManager(diagnostics, null, null)) {
            JavacTask task = (JavacTask) compiler.getTask(null, manager, diagnostics,
                List.of("-proc:none", "-source", "25"), null,
                manager.getJavaFileObjectsFromStrings(paths));
            SourcePositions positions = Trees.instance(task).getSourcePositions();
            for (CompilationUnitTree unit : task.parse()) {
                String file = Path.of(unit.getSourceFile().toUri()).toString();
                System.out.println("FILE\t" + file);
                new TreeScanner<Void, Void>() {
                    final Deque<String> classes = new ArrayDeque<>();
                    @Override public Void visitClass(ClassTree node, Void unused) {
                        String name = node.getSimpleName().toString();
                        classes.addLast(name.isEmpty() ? "<anonymous>" : name);
                        long pos = positions.getStartPosition(unit, node);
                        System.out.println("CLASS\t" + file + "\t" + String.join(".", classes)
                            + "\t" + unit.getLineMap().getLineNumber(pos));
                        super.visitClass(node, unused);
                        classes.removeLast();
                        return null;
                    }
                    @Override public Void visitMethod(MethodTree node, Void unused) {
                        long pos = positions.getStartPosition(unit, node);
                        System.out.println("METHOD\t" + file + "\t" + String.join(".", classes)
                            + "\t" + node.getName() + "\t" + node.getParameters().size()
                            + "\t" + unit.getLineMap().getLineNumber(pos));
                        return super.visitMethod(node, unused);
                    }
                }.scan(unit, null);
            }
        }
        for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
            if (d.getKind() == Diagnostic.Kind.ERROR) {
                System.err.println("ERROR\t" + d.getSource() + "\t" + d.getLineNumber() + "\t" + d.getMessage(null));
            }
        }
    }
}
