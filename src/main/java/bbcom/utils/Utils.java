package bbcom.utils;


import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

public class Utils {
    /**
     * Checks the modifiers and super classes of a given reflected class, returns true if we found an instantiable subclass of of {@param toImplement}
     * @param optimizerClass the {@link java.lang.Class} we want to examine
     * @return
     */
    public static boolean isImplementedClass(Class optimizerClass,Class toImplement){
        if(Modifier.isAbstract(optimizerClass.getModifiers()))
            return false;
        Class C = optimizerClass;
        while (C != null) {
            if(C.equals(toImplement))
                return true;
            C = C.getSuperclass();
        }
        return false;
    }



    public static <T> Map<Class<? extends T>, String> findAllMatchingTypes(Class<T> toFind, String modulePath) throws IOException, ClassNotFoundException {
        Map<Class<? extends T>, String> foundClasses = new HashMap<Class<? extends T>, String>();
        String[] classnametoprocess = new String[]{"none"};
        try(final Stream<Path> pathsStream = Files.walk(Paths.get(modulePath))) {
            pathsStream.forEach(filePath -> {
                if (Files.isRegularFile(filePath)&&filePath.toString().contains(".jar")) {


                    URL[] urls = new URL[0];
                    JarFile jarFile = null;
                    try {
                        System.out.println(filePath.toString());


                        //this is ok
                        jarFile = new JarFile(filePath.toString());
                        String mainClassName = (String) jarFile.getManifest().getMainAttributes().getValue(Attributes.Name.MAIN_CLASS);;
                        Enumeration<JarEntry> e = jarFile.entries();

                        urls = new URL[0];
                        urls = new URL[]{new URL("jar:file:" + filePath + "!/")};
                        URLClassLoader cl = URLClassLoader.newInstance(urls);
                        try {
                            while (e.hasMoreElements()) {
                                JarEntry je = e.nextElement();
                                classnametoprocess[0]= je.getName();

                                if (je.isDirectory() || !je.getName().endsWith(".class") /*|| !je.getName().replace(".class","").equals(mainClassName)*/) {
                                    continue;

                                }
                                // -6 because of .class
                                String className = je.getName().substring(0, je.getName().length() - 6);
                                className = className.replace('/', '.');
                                System.out.println("Found: "+className);

                                Class c = cl.loadClass(className);
                                if(isImplementedClass( c,toFind)) {
                                    foundClasses.put(c,null);
                                }

                            }

                        } catch (ClassNotFoundException e1) {
                            System.out.println("Error: " + classnametoprocess[0]);

                            e1.printStackTrace();
                        }
                    } catch (IOException e) {
                        System.out.println("Error: "+ filePath.toString());
                        e.printStackTrace();
                    }



                }
            });
        }

        return foundClasses;
    }
    /**
     * Finds the available  subclasses of parameter of type {{@link java.lang.Class}}, tghen collect them in a {@link java.util.Map},
     * key:  {@link java.lang.Class } that  extends {@code toFind},
     * value: setup files for the given class with the same name - to be removed.
     * @param toFind {@link java.lang.Class }, whose subclasses we want to find .
     * @param optimizerClassLocation the location where we want to search for the subclasses.
     * @param <T> {@link java.lang.Class } the super class ehose descendant we are curious of.
     * @return
     * @throws IOException
     */
//    public static <T> Map<Class<? extends T>, String> findAllMatchingTypes(Class<T> toFind, String optimizerClassLocation) throws IOException {
//        Map<Class<? extends T>,String> foundClasses = new HashMap<Class<? extends T>, String>() ;
//
//        try(final Stream<Path> pathsStream = Files.walk(Paths.get(optimizerClassLocation))) {
//            pathsStream.forEach(filePath -> {
//                if (Files.isRegularFile(filePath)) {
//                    if (filePath.toString().contains(".jar")){
//                        File f = filePath.toFile();
//                        try {
//                            URL url = f.toURI().toURL();
//                            URL[] urls = new URL[]{new URL("jar:file:" +url+"!/")};
//                            ClassLoader cl = new URLClassLoader(urls);
//                            String classname = /*"lib." +*/ f.getName().split("\\.")[0];
//                            Class optimizerClass = cl.loadClass(classname);
//                            if(isImplementedClass( optimizerClass,toFind))
//                                foundClasses.put(optimizerClass,null);
//                        } catch (ClassNotFoundException | MalformedURLException e) {
//                            e.printStackTrace();
//                        }
//                    }
//
//                }
//            });
//        }
//        //findConfigFiles(foundClasses,optimizerClassLocation);
//        return foundClasses;
//    }

    //public static List<Class> getRunnableClasses(Path path, Class c)
}
