package timers;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;


public class Timers {

    public static void main(String[] args) {
        // Check and get parameters
        if (args.length != 1) {
            System.out.println("[ERROR] Bad number of parameters");
            System.out.println("    Usage: simple.Simple <counterValue>");
            System.exit(-1);
        }
        String counterName = "counter";
        int initialValue = Integer.parseInt(args[0]);

        // ------------------------------------------------------------------------
        // Write value
        try (FileOutputStream fos = new FileOutputStream(counterName)) {
            fos.write(initialValue);
            System.out.println("Initial counter value is " + initialValue);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            System.exit(-1);
        }

        // ------------------------------------------------------------------------
        // Execute increment
        TimersImpl.increment(counterName);

        // ------------------------------------------------------------------------
        // Read new value
        System.out.println("After Sending task");
        try (FileInputStream fis = new FileInputStream(counterName)) {
            System.out.println("Final counter value is " + fis.read());
        } catch (IOException ioe) {
            ioe.printStackTrace();
            System.exit(-1);
        }
    }

}
