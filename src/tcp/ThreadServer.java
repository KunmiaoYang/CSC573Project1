package tcp;

import java.io.*;
import java.net.Socket;

public class ThreadServer extends Thread {
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
            pw.flush();

            String info = null;
            while (!(info = br.readLine()).equals(MainServer.CODE_EXIT)) {
                System.out.println("<Client>: " + info);
            }
            pw.println(MainServer.CODE_EXIT);
            pw.flush();
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
