package tcp;

import db.Database;

import java.io.*;
import java.net.Socket;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static tcp.MainServer.*;

public class ThreadServer extends Thread {
    static final String PREFIX = "Thread server";
    private Socket socket;
    private Database db;
    private String ip;
    private int port;
    private String whereClause;
    private String clientPrefix;

    private static void consoleOutput (String prefix, String msg) {
        System.out.format("<%s>: %s\r\n", prefix, msg);
    }

    ThreadServer(Socket socket, Database db) throws SQLException {
        this.socket = socket;
        this.db = db;
        registerClient();
    }

    private void registerClient() throws SQLException {
        this.ip = socket.getInetAddress().getHostAddress();
        this.port = socket.getPort();
        this.clientPrefix = String.format("%s %s:%d", Client.PREFIX, this.ip, this.port);
        this.whereClause = String.format("WHERE ip = '%s' AND port = %d", ip, port);
        Statement st = db.getStatement();
        ResultSet resultSet =
                st.executeQuery("SELECT active FROM client " + whereClause + ";");
        if (!resultSet.next())
            st.executeUpdate("INSERT INTO client (ip, port, active) values " +
                    "('" + ip + "', " + port + ", true);");
        else
            st.executeUpdate(String.format("UPDATE client SET active = true %s;", whereClause));
        resultSet.close();
    }

    private void addRFC (int number, String title, String ip, int port) throws SQLException {
        Statement st = db.getStatement();
        String whereClauseRFC = String.format("%s AND number = %d", whereClause, number);
        ResultSet resultSet =
                st.executeQuery("SELECT title FROM rfc " + whereClauseRFC + ";");
        if (resultSet.next())
            st.executeUpdate(String.format("UPDATE rfc SET title = %s %s;", title, whereClauseRFC));
        else
            st.executeUpdate("INSERT INTO rfc (number, title, ip, port) VALUES " +
                    "(" + number + ", '" + title + "', '" + ip + "', " + port + ");");
        resultSet.close();
    }

    private void executeAddRFC (PrintWriter pw, BufferedReader br, String command) throws IOException, SQLException {
        String[] args = command.split("\\s+");
        int number = Integer.parseInt(args[2]);
        int port = this.port;
        String title = null, ip = this.ip;
        consoleOutput(clientPrefix, String.format("Adding RFC %d ...", number));
        String info;
        while (!(info = br.readLine()).equals(CODE_END)) {
            if (info.startsWith(HEADER_HOST))
                ip = info.substring(HEADER_HOST.length()).trim();
            else if (info.startsWith(HEADER_PORT))
                port = Integer.parseInt(info.substring(HEADER_PORT.length()).trim());
            else if (info.startsWith(HEADER_TITLE))
                title = info.substring(HEADER_TITLE.length()).trim();
        }
        if (port > -1 && null != title && null != ip) {
            addRFC(number, title, ip, port);
            String msg = String.format("Add RFC %d %s from %s:%d complete!", number, title, ip, port);
            pw.println(msg);
        }
        pw.println(MainServer.CODE_END);
        pw.flush();
    }

    private void lookup (PrintWriter pw, BufferedReader br, String whereCondition) throws SQLException {
        StringBuilder sbSql = new StringBuilder("SELECT * FROM RFC");
        if (null != whereCondition && !whereCondition.equals(""))
            sbSql.append(" ").append(whereCondition.trim());
        ResultSet resultSet = db.getStatement().executeQuery(sbSql.append(" ORDER BY number;").toString());
        int count = 0;
        while (resultSet.next()) {
            count++;
            pw.format ("RFC %d %s on %s:%d\r\n",
                    resultSet.getInt("number"),
                    resultSet.getString("title"),
                    resultSet.getString("ip"),
                    resultSet.getInt("port")
            );
        }
        if (0 == count) pw.println("No RFC found!");
        resultSet.close();
        pw.println(MainServer.CODE_END);
        pw.flush();
    }

    private void executeLookup (PrintWriter pw, BufferedReader br, String command) throws IOException, SQLException {
        String[] args = command.split("\\s+");
        int number = Integer.parseInt(args[2]);
        int port = this.port;
        String title = null, ip = this.ip;
        consoleOutput(clientPrefix, String.format("Looking up RFC %d ...", number));
        String info;
        while (!(info = br.readLine()).equals(CODE_END)) {
            if (info.startsWith(HEADER_TITLE))
                title = info.substring(HEADER_TITLE.length()).trim();
        }
        StringBuilder sbWhere = new StringBuilder("WHERE number = ").append(number);
        if (null != title) sbWhere.append(" AND title = '").append(title).append("'");
        lookup(pw, br, sbWhere.toString());
    }

    private void executeList (PrintWriter pw, BufferedReader br, String command) throws IOException, SQLException {
        consoleOutput(clientPrefix, "List all RFCs");
        lookup(pw, br, "");
    }

    private void removeClient() throws SQLException {
        Statement st = db.getStatement();
        st.executeUpdate("DELETE FROM rfc " + whereClause + ";");
        st.executeUpdate("DELETE FROM client " + whereClause + ";");
    }

    private void execute (PrintWriter pw, BufferedReader br, String command) throws Exception {
        String code = command.trim().split("\\s+")[0];
        if (code.equals(MainServer.CODE_ADD)) {
            executeAddRFC(pw, br, command.trim());
        } else if (code.equals(MainServer.CODE_LOOKUP)) {
            executeLookup(pw, br, command.trim());
        } else if (code.equals(MainServer.CODE_LIST)) {
            executeList(pw, br, command.trim());
        }
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

            pw.println(this.ip + ":" + this.port);
            pw.println("Welcome!");
            pw.println(CODE_END);
            pw.flush();
            printMessage(clientPrefix, br);

            String command;
            while (!(command = br.readLine()).equals(CODE_EXIT)) {
                try {
                    execute(pw, br, command);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            consoleOutput(clientPrefix, "Disconnected!");
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
