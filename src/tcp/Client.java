package tcp;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.Scanner;

import static tcp.MainServer.*;

public class Client {
    static final String PREFIX = "Client";
    public static void main (String[] args) throws Exception {
        try {
            System.out.println("args: " + Arrays.toString(args));
            String serverName = args.length > 0? args[0]: "localhost";
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
