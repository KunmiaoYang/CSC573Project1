package tcp;

import file.LocalStorage;
import file.RFC;

import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import static tcp.MainServer.*;
import static tcp.Requests.KEY_COMMAND;

public class Client {
    static final String PREFIX = "Client";
    static final String VERSION = "P2P-CI/1.0";
    static final int BUF_SIZE = 1024;
    private static String serverName = null;
    private static String host;
    private static int clientServicePort;
    private static int port;
    private static String OS;
    private static Path localRoot = null;
    private static LocalStorage localStorage;

    private static void updateRFC (PrintWriter pw, BufferedReader br, LocalStorage localStorage)
            throws IOException {
        for (RFC rfc: localStorage.getAllRFC()) {
            addRFC(pw, br, rfc);
        }
    }

    private static void addRFC (PrintWriter pw, BufferedReader br, RFC rfc) throws IOException {
        pw.format("%s RFC %d %s\r\n", CODE_ADD, rfc.id, VERSION);
        pw.format("%s %s\r\n", HEADER_HOST, host);
        pw.format("%s %s\r\n", HEADER_PORT, clientServicePort);
        pw.format("%s %s\r\n", HEADER_TITLE, rfc.title);
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
        pw.format("%s %s\r\n", HEADER_HOST, host);
        pw.format("%s %s\r\n", HEADER_PORT, port);
        if (args.length > 2) {
            String title = command.substring(CODE_LOOKUP.length()).trim()
                    .substring(args[1].length()).trim();
            pw.format("%s %s\r\n", HEADER_TITLE, title);
        }
        pw.println(CODE_END);
        pw.flush();
        printMessage(ThreadServer.PREFIX, br);
        return true;
    }

    private static void listAll (PrintWriter pw, BufferedReader br) throws IOException {
        pw.format("%s ALL %s\r\n", CODE_LIST, VERSION);
        pw.format("%s %s\r\n", HEADER_HOST, host);
        pw.format("%s %s\r\n", HEADER_PORT, port);
        pw.println(CODE_END);
        pw.flush();
        printMessage(ThreadServer.PREFIX, br);
    }

    private static boolean getRFC (String command) {
        String[] args = command.split("\\s+");
        if (args.length < 4) return false;
        int number = Integer.parseInt(args[1]),
                port = Integer.parseInt(args[3]);
        String host = args[2],
                peerPrefix = String.format("Client service %s:%d", host, port);
        try(Socket peerSocket = new Socket(host, port);
            OutputStream os = peerSocket.getOutputStream();
            PrintWriter pw = new PrintWriter(os);
            InputStream is = peerSocket.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is))
        ) {
            pw.format("%s RFC %d %s\r\n", CODE_GET, number, VERSION);
            pw.format("%s %s\r\n", HEADER_HOST, host);
            pw.format("%s %s\r\n", HEADER_PORT, port);
            pw.format("%s %s\r\n", HEADER_OS, OS);
            pw.println(CODE_END);
            pw.flush();
            List<String> response = Requests.readRequest(br);
            Requests.consoleOutputRequest(peerPrefix, response);
            Map<String, String> responseMap = Requests.getRequestMap(response);

            if (STATUS_OK != Integer.parseInt(responseMap.get(KEY_COMMAND).split("\\s+")[1]))
                return false;

            String title = responseMap.get(HEADER_TITLE),
                    filename = String.format("%04d %s.txt", number, title);
            Path file = localStorage.createFile(filename);
            try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file.toFile()))) {
                byte[] buf = new byte[BUF_SIZE];
                for (int num = is.read(buf); num != -1; num = is.read(buf)) {
                    bos.write(buf, 0, num);
                    bos.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (ConnectException e) {
            System.err.println("Invalid peer address!");
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private static void execute (PrintWriter pw, BufferedReader br, String command) throws Exception {
        String code = command.trim().split("\\s+")[0];
        if (code.equalsIgnoreCase(MainServer.CODE_LOOKUP)) {
            if (lookup(pw, br, command.trim())) return;
        } else if (code.equalsIgnoreCase(MainServer.CODE_LIST)) {
            listAll(pw, br);
            return;
        } else if (code.equalsIgnoreCase(MainServer.CODE_GET)) {
            if (getRFC(command)) return;
        } else {
            pw.format("%s %s\r\n", command, VERSION);
            pw.println(CODE_END);
            pw.flush();
            printMessage(ThreadServer.PREFIX, br);
        }
    }

    public static void main (String[] args) throws Exception {
        ClientService clientService = null;
        try {
            System.out.println("args: " + Arrays.toString(args));
            for (int i = 0; i < args.length; i++) {
                if (args[i].equalsIgnoreCase("-l")) {
                    try {
                        localRoot = Paths.get(args[++i]).toAbsolutePath();
                        if (!Files.exists(localRoot))
                            localRoot = Files.createDirectory(localRoot);
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

            OS = System.getProperty("os.name");

            // init local storage
            localStorage = new LocalStorage(localRoot);

            // connect
            Socket socket = new Socket(serverName, PORT);
            host = socket.getLocalAddress().getHostAddress();
            port = socket.getLocalPort();

            // start client service
            clientService = new ClientService(localStorage, host);
            clientServicePort = clientService.getPort();
            clientService.start();

            // init IO
            OutputStream os = socket.getOutputStream();
            PrintWriter pw = new PrintWriter(os);
            InputStream is = socket.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            Scanner scanner = new Scanner(System.in);

            // send basic info
            pw.format("%s %s %s\r\n", CODE_CONNECT, "INIT", VERSION);
            pw.format("%s %s\r\n", HEADER_HOST, host);
            pw.format("%s %s\r\n", HEADER_PORT, clientServicePort);
            pw.println(CODE_END);
            pw.flush();
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
        } finally {
            if (null != clientService) clientService.kill();
        }
    }
}
