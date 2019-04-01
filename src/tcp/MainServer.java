package tcp;

import db.Database;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.sql.Statement;

public class MainServer {
    static final int PORT = 7734;
    static final String CODE_ADD = "ADD";
    static final String CODE_LOOKUP = "LOOKUP";
    static final String CODE_LIST = "LIST";
    static final String CODE_END = "end";
    static final String CODE_EXIT = "exit";
    static final String HEADER_HOST = "Host:";
    static final String HEADER_PORT = "Port:";
    static final String HEADER_TITLE = "Title:";
    static final String HEADER_DATE = "Date:";
    static final String HEADER_OS = "OS:";
    static final String HEADER_LAST_MODIFIED = "Last-Modified:";
    static final String HEADER_CONTENT_LENGTH = "Content-Length:";
    static final String HEADER_CONTENT_TYPE = "Content-Type:";
    private static final Database db =  new Database();

    static void printMessage(String prefix, BufferedReader br) throws IOException {
        String info;
        while (!(info = br.readLine()).equals(CODE_END)) {
            System.out.println("<" + prefix + ">: " + info);
        }
    }

    private static boolean initDB() {
        if (null == db.getConnection()) return false;
        try {
            Statement statement = db.getStatement();
            statement.executeUpdate("DELETE FROM rfc;");
            statement.executeUpdate("DELETE FROM client;");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }

    public static void main (String[] args) {
        try {
            // init database
            if (!initDB()) return;

            // init socket
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("Main server started!");

            while (true) {
                Socket socket = null;
                try {
                    socket = serverSocket.accept();
                    ThreadServer threadServer = new ThreadServer(socket, db);
                    threadServer.start();
                    InetAddress address = socket.getInetAddress();
                    System.out.println("New connection: "+ address.getHostAddress() + ":" + socket.getPort());
                } catch (SQLException e) {
                    e.printStackTrace();
                    try {
                        if (null != socket) socket.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
