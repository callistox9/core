import java.io.File;

import javax.tools.*;
import java.util.*;

import java.net.URL;
import java.net.URLClassLoader;

import java.lang.reflect.Method;
import java.lang.Object;

import java.nio.file.Path;
import java.nio.file.Paths;

class Handle {
  HashMap<String, Class<?>> handleMap = new HashMap<String, Class<?>>();

  public void addClass(String name, Class<?> classObj) {
    handleMap.put(name, classObj);
  }

  public Class<?> getClassFromName(String name) {
    return handleMap.get(name);
  }

  public String[] getStringArray() {
    String[] strArr = new String[handleMap.size()];
    int i = 0;
    for (Map.Entry<String, Class<?>> handle : handleMap.entrySet()) {
      strArr[i++] = handle.getKey();
    }

    return strArr;
  }
}

public class bootstrap {

  // TODO: The "Global Handle Class" is wrong, we cannot have a global handle
  // array.
  // The relationship is like this:

  // for each call to load_from_{file, memory, package}, we return one handle,
  // internally it must hold the classes separately to each other handle
  // the handle can hold the classes with an array, hashmap or whatever

  // for each class instantiation or method call, we can hold a pointer to the
  // handle that owns this class, so we can instantiate it properly (we can do
  // this in C++)

  // for each clear call, we receive a handle, so we iterate the classes of the
  // handle and clear them, including each resource attached to it

  // This approach is more interesting because in this way we have isolated
  // classes,
  // avoiding name collisions (for example, we can load two different java classes
  // with the same
  // name in different load_from_{file, memory, package}, also we can use things
  // for sandboxing
  // like this:
  // https://docs.oracle.com/javase/7/docs/api/java/security/ProtectionDomain.html

  // The handle array will be implicitly managed by metacall core if we do this
  // correctly,
  // we can store the pointer to the handle in the struct
  // loader_impl_java_handle_type directly
  // and return it in the load_from_{file, memory, package} in the C++ side

  private static Handle handleArray = new Handle(); // Global Handle Class to store classes and names

  private static String executionPath;

  public static void callFunction(String classname, String functionName) {
    Class<?> c = handleArray.getClassFromName(classname);

    try {
      Method m = c.getDeclaredMethod(functionName, new Class[] { String[].class });
      m.invoke(null, new Object[] { null });

    } catch (Exception e) {
      System.err.println("CallFunction" + e);
    }
  }

  public static int java_bootstrap_execution_path(String path) {
    System.out.println("bootstraping Execution path = " + path);
    executionPath = path;
    return 0;
  }

  public static String[] loadFromFile(String[] paths) {
    // load all scripts and store them into a Handle class, then return it
    for (int i = 0; i < paths.length; i++) {
      System.out.println("Path provided " + paths[i]);

      try {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> ds = new DiagnosticCollector<>();
        StandardJavaFileManager mgr = compiler.getStandardFileManager(ds, null, null);
        // TODO: This hardcoded path should be avoided, if it can be in memory it would
        // be cool, otherwise
        // we can do this in a temporary path
        Iterable<String> classOutputPath = Arrays.asList(new String[] { "-d", executionPath });

        File pathFile = new File(paths[i]);
        Iterable<? extends JavaFileObject> sources = mgr.getJavaFileObjectsFromFiles(Arrays.asList(pathFile));
        JavaCompiler.CompilationTask task = compiler.getTask(null, mgr, ds, classOutputPath, null, sources);
        Boolean call = task.call(); // main method to compile the file into class

        if (call) {
          System.out.println("Compilation Successful");

          Path path = Paths.get(pathFile.getCanonicalPath());
          String classname = path.getFileName().toString().split(".java")[0];

          File execPathFile = new File(executionPath);
          URLClassLoader clsLoader = new URLClassLoader(new URL[] { execPathFile.toURI().toURL() });

          handleArray.addClass(classname, clsLoader.loadClass(classname));
          clsLoader.close();

          // handleArray.addClass(classname, Class.forName(classname));
          System.out.println("Class Loading Successful");

        } else {
          System.out.println("Compilation Failed");
        }

        for (Diagnostic<? extends JavaFileObject> d : ds.getDiagnostics()) { // diagnostic error printing
          System.out.format("DIAGNOSTIC Line: %d, %s in %s", d.getLineNumber(), d.getMessage(null),
              d.getSource().getName());
        }

        mgr.close();
        System.out.print("\n");

      } catch (Exception e) {
        System.err.println("Load Function" + e);
      }
    }

    return handleArray.getStringArray();
  }

  public static void DiscoverData(String classname) {
    // for each loaded .java file in the Handle list, get the DiscoverData, which is
    // another class with the list of classes and methods etc
    Class<?> hClass = handleArray.getClassFromName(classname);

    System.out.println("ClassName: " + hClass.getName());

    Method[] methods = hClass.getDeclaredMethods();

    for (Method method : methods) {
      System.out.println("Name of the method: " + method.getName());

      Class<?>[] parameters = method.getParameterTypes();
      if (parameters.length == 0)
        System.out.println("\tparameter: none");
      for (Class<?> parameter : parameters) {
        System.out.println("\tparameter: " + parameter.getSimpleName());
      }
      System.out.println("\tReturn Type: " + method.getReturnType() + "\n");

    }
  }
}