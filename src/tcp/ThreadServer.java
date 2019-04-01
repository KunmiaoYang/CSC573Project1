package tcp;

import db.Database;

import java.io.*;
import java.net.Socket;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static tcp.MainServer.CODE_END;
import static tcp.MainServer.CODE_EXIT;
import static tcp.MainServer.printMessage;

public class ThreadServer extends Thread {
    static final String PREFIX = "Thread server";
    private Socket socket = null;
    private Database db = null;
    private String ip;
    private int port;
    private String whereClause;

    ThreadServer(Socket socket, Database db) throws SQLException {
        this.socket = socket;
        this.db = db;
        registerClient();
    }

    private void registerClient() throws SQLException {
        this.ip = socket.getInetAddress().getHostAddress();
        this.port = socket.getPort();
        this.whereClause = "WHERE ip = '" + ip + "' AND port = " + port;
        Statement st = db.getStatement();
        ResultSet resultSet =
                st.executeQuery("SELECT active FROM client " + whereClause + ";");
        if (!resultSet.next())
            st.executeUpdate("INSERT INTO client (ip, port, active) values " +
                    "('" + ip + "', " + port + ", true);");
        else
            st.executeUpdate("UPDATE client SET active = true " + whereClause + ";");
    }

    private void addRFC (int number, String title, String ip, int port) throws SQLException {
        Statement st = db.getStatement();
        String whereClauseRFC = whereClause + " AND number = " + number;
        ResultSet resultSet =
                st.executeQuery("SELECT title FROM rfc " + whereClauseRFC + ";");
        if (resultSet.next())
            st.executeUpdate("UPDATE rfc SET title = " + title + " " + whereClauseRFC + ";");
        else
            st.executeUpdate("INSERT INTO rfc (number, title, ip, port) VALUES " +
                    "(" + number + ", '" + title + "', '" + ip + "', " + port + ");");
    }

    private void removeClient() throws SQLException {
        Statement st = db.getStatement();
        st.executeUpdate("DELETE FROM rfc " + whereClause + ";");
        st.executeUpdate("DELETE FROM client " + whereClause + ";");
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
                if (null != socket) socket.close();
                if (null != pw) pw.close();
                if (null != os) os.close();
                if (null != br) br.close();
                if (null != is) is.close();
                removeClient();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
