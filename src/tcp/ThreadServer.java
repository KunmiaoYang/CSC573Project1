package tcp;

import db.Database;

import java.io.*;
import java.net.Socket;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import static tcp.MainServer.*;
import static tcp.Requests.KEY_COMMAND;

public class ThreadServer extends Thread {
    static final String PREFIX = "Thread server";
    private static final String VERSION = "P2P-CI/1.0";
    private static final int STATUS_OK = 200;
    private static final int STATUS_BAD_REQUEST = 400;
    private static final int STATUS_NOT_FOUND = 404;
    private static final int STATUS_INVALID_VERSION = 505;
    private static final String PHRASE_OK = "OK";
    private static final String PHRASE_BAD_REQUEST = "Bad Request";
    private static final String PHRASE_NOT_FOUND = "Not Found";
    private static final String PHRASE_INVALID_VERSION = "P2P-CI Version Not Supported";
    private Socket socket;
    private Database db;
    private String ip;
    private int clientServicePort;
    private String whereClause;
    private String clientPrefix;

    ThreadServer(Socket socket, Database db) {
        this.socket = socket;
        this.db = db;
    }

    private void registerClient(Map<String, String> requestMap) throws SQLException {
        this.clientServicePort = Integer.parseInt(requestMap.get(MainServer.HEADER_PORT));
        this.ip = socket.getInetAddress().getHostAddress();
//        this.clientServicePort = socket.getPort();
        this.clientPrefix = String.format("%s %s:%d", Client.PREFIX, this.ip, socket.getPort());
        this.whereClause = String.format("WHERE ip = '%s' AND port = %d", ip, clientServicePort);
        Statement st = db.getStatement();
        ResultSet resultSet =
                st.executeQuery("SELECT active FROM client " + whereClause + ";");
        if (!resultSet.next())
            st.executeUpdate("INSERT INTO client (ip, port, active) values " +
                    "('" + ip + "', " + clientServicePort + ", true);");
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

    private void executeAddRFC (PrintWriter pw, Map<String, String> requestMap) throws SQLException {
        String command = requestMap.get(KEY_COMMAND);
        String[] args = command.split("\\s+");
        int number = Integer.parseInt(args[2]);
        int port = requestMap.containsKey(HEADER_PORT)?
                Integer.parseInt(requestMap.get(HEADER_PORT)): this.clientServicePort;
        String title = requestMap.get(HEADER_TITLE),
                ip = requestMap.getOrDefault(HEADER_HOST, this.ip);
        if (port > -1 && null != title && null != ip) {
            addRFC(number, title, ip, port);
            pw.format("%s %d %s\r\n", VERSION, STATUS_OK, PHRASE_OK);
            pw.format("Add RFC %d %s from %s:%d complete!\r\n", number, title, ip, port);
        }
        pw.println(MainServer.CODE_END);
        pw.flush();
    }

    private void lookup (PrintWriter pw, String whereCondition) throws SQLException {
        StringBuilder sbSql = new StringBuilder("SELECT * FROM RFC");
        if (null != whereCondition && !whereCondition.equals(""))
            sbSql.append(" ").append(whereCondition.trim());
        ResultSet resultSet = db.getStatement().executeQuery(sbSql.append(" ORDER BY number;").toString());
        StringBuilder sb = new StringBuilder();
        while (resultSet.next()) {
            sb.append(String.format ("RFC %d %s %s %d\r\n",
                    resultSet.getInt("number"),
                    resultSet.getString("title"),
                    resultSet.getString("ip"),
                    resultSet.getInt("port")
            ));
        }
        if (0 == sb.length()) {
            pw.format("%s %d %s\r\n", VERSION, STATUS_NOT_FOUND, PHRASE_NOT_FOUND);
        } else {
            pw.format("%s %d %s\r\n", VERSION, STATUS_OK, PHRASE_OK);
            pw.print(sb.toString());
        }
        resultSet.close();
        pw.println(MainServer.CODE_END);
        pw.flush();
    }

    private void executeLookup (PrintWriter pw, Map<String, String> requestMap) throws SQLException {
        String command = requestMap.get(KEY_COMMAND);
        String[] args = command.split("\\s+");
        int number = Integer.parseInt(args[2]);
        String title = requestMap.get(HEADER_TITLE);
        StringBuilder sbWhere = new StringBuilder("WHERE number = ").append(number);
        if (null != title) sbWhere.append(" AND title = '").append(title).append("'");
        lookup(pw, sbWhere.toString());
    }

    private void executeList(PrintWriter pw) throws SQLException {
        lookup(pw, "");
    }

    private void removeClient() throws SQLException {
        Statement st = db.getStatement();
        st.executeUpdate("DELETE FROM rfc " + whereClause + ";");
        st.executeUpdate("DELETE FROM client " + whereClause + ";");
    }

    private int[] getVersion(String version) throws VersionException {
        String[] sNums = version.substring(version.lastIndexOf('/') + 1).split("\\.");
        if (sNums.length < 2) throw new VersionException("Invalid version!");
        int[] iNums = new int[sNums.length];
        try {
            for (int i = 0; i < iNums.length; i++)
                iNums[i] = Integer.parseInt(sNums[i]);
        } catch (NumberFormatException e) {
            throw new VersionException("Invalid version!");
        }
        return iNums;
    }

    private boolean checkVersion(String version) throws VersionException {
        int[] serverVersion = getVersion(VERSION), clientVersion = getVersion(version);
        for (int i = 0; i < serverVersion.length && i < clientVersion.length; i++) {
            if (serverVersion[i] < clientVersion[i]) return false;
            if (serverVersion[i] > clientVersion[i]) return true;
        }
        return serverVersion.length >= clientVersion.length;
    }

    private void execute (PrintWriter pw, List<String> request) throws IOException {
        Requests.consoleOutputRequest(clientPrefix, request);
        Map<String, String> requestMap = Requests.getRequestMap(request);
        String command = requestMap.get(KEY_COMMAND);
        String[] args = command.trim().split("\\s+");
        String code = args[0];
        String version = args[args.length - 1];
        try {
            if (!checkVersion(version)) { // if version is not supported
                pw.format("%s %d %s\r\n", VERSION, STATUS_INVALID_VERSION, PHRASE_INVALID_VERSION);
                pw.println(command);
                pw.println(MainServer.CODE_END);
                pw.flush();
                return;
            } else if (code.equals(MainServer.CODE_ADD)) {
                executeAddRFC(pw, requestMap);
                return;
            } else if (code.equals(MainServer.CODE_LOOKUP)) {
                executeLookup(pw, requestMap);
                return;
            } else if (code.equals(MainServer.CODE_LIST)) {
                executeList(pw);
                return;
            }
        } catch (SQLException | VersionException e) {
            e.printStackTrace();
        }
        pw.format("%s %d %s\r\n", VERSION, STATUS_BAD_REQUEST, PHRASE_BAD_REQUEST);
        pw.println(command);
        pw.println(MainServer.CODE_END);
        pw.flush();
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

            List<String> request = Requests.readRequest(br);
            registerClient(Requests.getRequestMap(request));
            Requests.consoleOutputRequest(clientPrefix, request);

            for (request = Requests.readRequest(br);
                 !request.get(request.size() - 1).equals(CODE_EXIT);
                 request = Requests.readRequest(br)) {
                try {
                    execute(pw, request);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            Requests.consoleOutput(clientPrefix, "Disconnected!");
        } catch (IOException | SQLException e) {
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

    public static class VersionException extends Exception {
        VersionException(String message) {
            super(message);
        }
    }
}
