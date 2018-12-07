package bbcom.utils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FileUtils {


    public static boolean runningInJar(){

        String u = Utils.class.getProtectionDomain().getCodeSource().getLocation().getPath().toString();
        //System.out.println("Source url="+u);
        if(u.contains(".jar"))
            return true;
        return false;

    }

    public static String getSourceHome(){
        return Utils.class.getProtectionDomain().getCodeSource().getLocation().getPath();

    }
    public static void createDirIfNotExists(String path){

        File directory = new File(path);
        if (! directory.exists()){
            directory.mkdir();
            // If you require it to make the entire directory path including parents,
            // use directory.mkdirs(); here instead.
        }

    }

    public static String findRersource(String path, String filename) throws IOException {
        String sourceHome = getSourceHome();
        String[] res=new String[1];
        Files.walk(Paths.get(path))
                .forEach((f)->{
                    String file = f.toString();
                    if( file.endsWith(filename)) {
                        res[0] = file;
                        return;
                    }
                });
        return res[0];
    }


}