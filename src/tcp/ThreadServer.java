package tcp;

import db.Database;

import java.io.*;
import java.net.Socket;
import java.sql.SQLException;

import static tcp.MainServer.CODE_END;
import static tcp.MainServer.CODE_EXIT;
import static tcp.MainServer.printMessage;

public class ThreadServer extends Thread {
    static final String PREFIX = "Thread server";
    private Socket socket = null;
    private Database db = null;
    private String ip;
    private int port;

    ThreadServer(Socket socket, Database db) throws SQLException {
        this.socket = socket;
        this.db = db;
        registerClient();
    }

    private void registerClient() throws SQLException {
        this.ip = socket.getInetAddress().getHostAddress();
        this.port = socket.getPort();
        db.getStatement().executeUpdate("INSERT INTO client (ip, port, active) values " +
                "('" + ip + "', " + port + ", true);");
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

            String command;
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
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
