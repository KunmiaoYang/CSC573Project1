package tcp;

import db.Database;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class MainServer {
    static final int PORT = 7734;
    static final String CODE_END = "end";
    static final String CODE_EXIT = "exit";
    static final Database db =  new Database();

    public static void printMessage(String prefix, BufferedReader br) throws IOException {
        String info;
        while (!(info = br.readLine()).equals(CODE_END)) {
            System.out.println("<" + prefix + ">: " + info);
        }
    }
    public static void main (String[] args) throws IOException {
        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("Main server started!");
            Socket socket = null;

            while (true) {
                socket = serverSocket.accept();
                ThreadServer threadServer = new ThreadServer(socket);
                threadServer.start();
                InetAddress address = socket.getInetAddress();
                System.out.println("New connection: "+ address.getHostAddress() + ":" + socket.getPort());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
