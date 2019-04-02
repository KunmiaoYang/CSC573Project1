package tcp;

import file.LocalStorage;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class ClientService extends Thread {
    private LocalStorage localStorage;
    private String ip;
    private int port;
    private ServerSocket serverSocket;
    private Socket socket;

    ClientService(LocalStorage localStorage, String ip) throws IOException {
        this.localStorage = localStorage;
        this.ip = ip;
        for (this.port = 5001; this.port < 65536; this.port++) {
            try {
                serverSocket = new ServerSocket(this.port);
            } catch (IOException e) {
                continue;
            }
            break;
        }
    }

    void kill() {
        try {
            this.serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getPort() {
        return port;
    }

    @Override
    public void run() {
//        super.run();
        System.out.println("<Client Service>: Server started!");
        InputStream is = null;
        BufferedReader br = null;
        OutputStream os = null;
        PrintWriter pw = null;
        while (!serverSocket.isClosed()) {
            try {
                // init IO
                socket = serverSocket.accept();
                is = socket.getInputStream();
                br = new BufferedReader(new InputStreamReader(is));
                os = socket.getOutputStream();
                pw = new PrintWriter(os);

                // TODO: transmit data

            } catch (SocketException e) {
                System.out.println("<Client Service>: Server stopped!");
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (null != socket) socket.close();
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
}
