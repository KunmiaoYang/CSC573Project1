package tcp;

import java.io.*;
import java.net.Socket;

import static tcp.MainServer.CODE_END;
import static tcp.MainServer.CODE_EXIT;
import static tcp.MainServer.printMessage;

public class ThreadServer extends Thread {
    static final String PREFIX = "Thread server";
    Socket socket = null;

    public ThreadServer(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
//        super.run();
        InputStream is = null;
        BufferedReader br = null;
        OutputStream os = null;
        PrintWriter pw = null;
        try {
            // Init IO
            is = socket.getInputStream();
            br = new BufferedReader(new InputStreamReader(is));
            os = socket.getOutputStream();
            pw = new PrintWriter(os);

            pw.println("Welcome!");
            pw.println(CODE_END);
            pw.flush();
            printMessage(Client.PREFIX, br);

            String command = null;
            while (!(command = br.readLine()).equals(CODE_EXIT)) {
                pw.println(command + " received.");
                pw.println(CODE_END);
                pw.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (null != pw) pw.close();
                if (null != os) os.close();
                if (null != br) br.close();
                if (null != is) is.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
