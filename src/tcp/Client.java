package tcp;

import file.LocalStorage;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Scanner;

import static tcp.MainServer.*;

public class Client {
    static final String PREFIX = "Client";
    private static String serverName = null;
    private static Path localRoot = null;
    public static void main (String[] args) throws Exception {
        try {
            System.out.println("args: " + Arrays.toString(args));
            for (int i = 0; i < args.length; i++) {
                if (args[i].equalsIgnoreCase("-l")) {
                    try {
                        localRoot = Paths.get(args[++i]).toAbsolutePath();
                        if (!Files.exists(localRoot))
                            localRoot = Files.createDirectories(localRoot);
                    } catch (IllegalArgumentException | IOException | IOError e) {
                        System.err.println("Error: \"" + args[i] + "\" is not a valid path!");
                        return;
                    }
                } else if (args[i].equalsIgnoreCase("-s")) {
                    serverName = args[++i];
                }
            }

            if (null == serverName) {
                System.err.println("Error: No server specified!");
                return;
            }
            if (null == localRoot) {
                System.err.println("Error: No path specified for local storage!");
                return;
            }

            LocalStorage localStorage = new LocalStorage(localRoot);

            Socket socket = new Socket(serverName, PORT);

            // init IO
            OutputStream os = socket.getOutputStream();
            PrintWriter pw = new PrintWriter(os);
            InputStream is = socket.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            Scanner scanner = new Scanner(System.in);

            // send basic info
            pw.println("This is client!");
            pw.println(CODE_END);
            pw.flush();
            printMessage(ThreadServer.PREFIX, br);

            // user console
            String command = null;
            while (!(command = scanner.nextLine()).equals(CODE_EXIT)) {
                pw.println(command);
                pw.flush();
                printMessage(ThreadServer.PREFIX, br);
            }
            pw.println(CODE_EXIT);
            pw.flush();

            br.close();
            is.close();
            pw.close();
            os.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
