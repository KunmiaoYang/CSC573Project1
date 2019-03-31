package tcp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class MainServer {
    static final int PORT = 7734;
    static final String CODE_EXIT = "exit";
    public static void main (String[] args) throws IOException {
        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("Main server started!");
            Socket socket = null;

            int count = 0;
            while (true) {
                socket = serverSocket.accept();
                ThreadServer threadServer = new ThreadServer(socket);
                threadServer.start();
                count++;
                InetAddress address = socket.getInetAddress();
                System.out.println("New connection IP: "+ address.getHostAddress() + ":" + socket.getPort());
                System.out.println("Client count:" + count);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
