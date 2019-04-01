package tcp;

import file.LocalStorage;
import file.RFC;

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
    private static final String VERSION = "P2P-CI/1.0";
    private static String serverName = null;
    private static String host;
    private static int port;
    private static String os;
    private static Path localRoot = null;

    private static void initHost (BufferedReader br) throws IOException {
        String[] info = br.readLine().split(":");
        host = info[0];
        port = Integer.parseInt(info[1]);
    }

    private static void updateRFC (PrintWriter pw, BufferedReader br, LocalStorage localStorage)
            throws IOException {
        for (RFC rfc: localStorage.getAllRFC()) {
            addRFC(pw, br, rfc);
        }
    }

    private static void addRFC (PrintWriter pw, BufferedReader br, RFC rfc) throws IOException {
        pw.format("%s RFC %d %s\r\n", CODE_ADD, rfc.id, VERSION);
        pw.format("%s: %s\r\n", "Host", host);
        pw.format("%s: %s\r\n", "Port", port);
        pw.format("%s: %s\r\n", "Title", rfc.title);
        pw.println(CODE_END);
        pw.flush();
        printMessage(ThreadServer.PREFIX, br);
    }

    private static boolean lookup (PrintWriter pw, BufferedReader br, String command) throws IOException {
        String[] args = command.split("\\s+");
        if (args.length < 2) return false;
        int number = 0;
        try {
            number = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            return false;
        }
        pw.format("%s RFC %d %s\r\n", CODE_LOOKUP, number, VERSION);
        pw.format("%s: %s\r\n", "Host", host);
        pw.format("%s: %s\r\n", "Port", port);
        if (args.length > 2) {
            String title = command.substring(CODE_LOOKUP.length()).trim()
                    .substring(args[1].length()).trim();
            pw.format("%s: %s\r\n",
                    "Title", title);
        }
        pw.println(CODE_END);
        pw.flush();
        printMessage(ThreadServer.PREFIX, br);
        return true;
    }

    private static void listAll (PrintWriter pw, BufferedReader br) throws IOException {
        pw.format("%s ALL %s\r\n", CODE_LIST, VERSION);
        pw.format("%s: %s\r\n", "Host", host);
        pw.format("%s: %s\r\n", "Port", port);
        pw.println(CODE_END);
        pw.flush();
        printMessage(ThreadServer.PREFIX, br);
    }

    private static void execute (PrintWriter pw, BufferedReader br, String command) throws Exception {
        String code = command.trim().split("\\s+")[0];
        if (code.equalsIgnoreCase(MainServer.CODE_LOOKUP)) {
            if (lookup(pw, br, command.trim())) return;
        } else if (code.equalsIgnoreCase(MainServer.CODE_LIST)) {
            listAll(pw, br);
            return;
        } else {
            pw.format("%s %s\r\n", command, VERSION);
            pw.println(CODE_END);
            pw.flush();
            printMessage(ThreadServer.PREFIX, br);
        }
    }

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

            os = System.getProperty("os.name");

            // init local storage
            LocalStorage localStorage = new LocalStorage(localRoot);

            // connect
            Socket socket = new Socket(serverName, PORT);

            // init IO
            OutputStream os = socket.getOutputStream();
            PrintWriter pw = new PrintWriter(os);
            InputStream is = socket.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            Scanner scanner = new Scanner(System.in);

            // send basic info
            pw.println("Connection established!");
            pw.println(CODE_END);
            pw.flush();
            initHost(br);
            printMessage(ThreadServer.PREFIX, br);
            updateRFC(pw, br, localStorage);

            // user console
            System.out.println("Please input command:");
            String command = null;
            while (!(command = scanner.nextLine()).equals(CODE_EXIT)) {
                execute(pw, br, command);
                System.out.println("Please input command:");
            }
            pw.println(CODE_EXIT);
            pw.flush();

            socket.close();
            br.close();
            is.close();
            pw.close();
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
