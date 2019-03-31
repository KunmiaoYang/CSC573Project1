package tcp;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;

public class Client {
    public static void main (String[] args) throws Exception {
        try {
            System.out.println("args: " + Arrays.toString(args));
            String serverName = args.length > 0? args[0]: "localhost";
            Socket socket = new Socket(serverName, MainServer.PORT);

            // Init IO
            OutputStream os = socket.getOutputStream();
            PrintWriter pw = new PrintWriter(os);
            InputStream is = socket.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));

            pw.println("This is client!");
            pw.println(MainServer.CODE_EXIT);
            pw.flush();

            String info = null;
            while (!(info = br.readLine()).equals(MainServer.CODE_EXIT)) {
                System.out.println("<Server>: " + info);
            }

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
