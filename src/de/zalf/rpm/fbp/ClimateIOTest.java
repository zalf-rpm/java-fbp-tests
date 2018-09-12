package de.zalf.rpm.fbp;

import com.sun.jna.Native;
import com.sun.jna.Pointer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;


public class ClimateIOTest {

    static {
        Native.register("libclimateio");
    }


    //public interface CLibrary extends Library {
    //    CLibrary INSTANCE = (CLibrary) Native.loadLibrary("libclimateio", CLibrary.class);
    //    String helloWorld(String name);
    //}


    public static native Pointer Climate_readClimateDataFromCSVStringViaHeaders(String csvString, String options);
    public static native void Climate_freeCString(Pointer str);

    public static void main(String[] args){
        //File csvFile = new File("C:\\Users\\berg.ZALF-AD\\GitHub\\monica\\installerHohenfinow2\\climate.csv");
        //String csvStr = new FileReader("C:\\Users\\berg.ZALF-AD\\GitHub\\monica\\installerHohenfinow2\\climate.csv").;
        try {
            String csvStr = new String(Files.readAllBytes(Paths.get("C:\\Users\\berg.ZALF-AD\\GitHub\\monica\\installer\\Hohenfinow2\\climate.csv")));

            String options = "{\n" +
                    "\t\t\"__given the start and end date, monica will run just this time range, else the full time range given by supplied climate data\": \"\",\n" +
                    "\t\t\"start-date\": \"1991-01-01\",\n" +
                    "\t\t\"end-date\": \"1997-12-31\",\n" +
                    "\t\n" +
                    "\t\t\"no-of-climate-file-header-lines\": 2,\n" +
                    "\t\t\"csv-separator\": \";\",\n" +
                    "\t\t\"header-to-acd-names\": {\n" +
                    "\t\t\t\"DE-date\": \"de-date\",\n" +
                    "\t\t\t\"globrad\": [\"globrad\", \"/\", 100]\n" +
                    "\t\t}\n" +
                    "\t}";

            Pointer jsonPtr = Climate_readClimateDataFromCSVStringViaHeaders(csvStr, options);
            String json = jsonPtr.getString(0);
            Climate_freeCString(jsonPtr);
            System.out.println(json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
