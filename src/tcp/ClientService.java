package tcp;

import file.LocalStorage;
import file.RFC;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static tcp.Client.BUF_SIZE;
import static tcp.MainServer.*;
import static tcp.Requests.KEY_COMMAND;

public class ClientService extends Thread {
    static final String PREFIX = "Client Service";
    private LocalStorage localStorage;
    private String ip;
    private int port;
    private String OS;
    private ServerSocket serverSocket;

    ClientService(LocalStorage localStorage, String ip) throws IOException {
        this.localStorage = localStorage;
        this.ip = ip;
        this.OS = System.getProperty("os.name");
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
        System.out.format("<%s>: Server has started!\r\n", PREFIX);
        while (!serverSocket.isClosed()) {
            try (Socket socket = serverSocket.accept();
                 InputStream is = socket.getInputStream();
                 BufferedReader br = new BufferedReader(new InputStreamReader(is));
                 OutputStream os = socket.getOutputStream();
                 PrintWriter pw = new PrintWriter(os)){
                // parse request
                List<String> request = Requests.readRequest(br);
                Requests.consoleOutputRequest(PREFIX, request);
                Map<String, String> requestMap = Requests.getRequestMap(request);
                int number = Integer.parseInt(requestMap.get(KEY_COMMAND).split("\\s+")[2]);
                RFC rfc = localStorage.getRFC(number);
                if (null == rfc) {
                    pw.format("%s %d %s\r\n", Client.VERSION, STATUS_NOT_FOUND, PHRASE_NOT_FOUND);
                    pw.println(requestMap.get(KEY_COMMAND));
                    pw.println(CODE_END);
                    pw.flush();
                    throw new NoRFCFoundException("RFC "+ number);
                }

                // response header
                pw.format("%s %d %s\r\n", Client.VERSION, STATUS_OK, PHRASE_OK);
                pw.format("%s %s\r\n", HEADER_DATE, new Date().toString());
                pw.format("%s %s\r\n", HEADER_OS, OS);
                pw.format("%s %s\r\n", HEADER_TITLE, rfc.title);
                pw.println(CODE_END);
                pw.flush();

                // transmit data
                try (InputStream fis = new FileInputStream(rfc.path.toFile())) {
                    byte[] buf = new byte[BUF_SIZE];
                    for (int num = fis.read(buf); num != -1; num = fis.read(buf)) {
                        os.write(buf, 0, num);
                        os.flush();
                    }
                }
            } catch (NoRFCFoundException e) {
                System.out.format("<%s>: %s\r\n", PREFIX, e.getMessage());
            } catch (IOException e) {
                System.out.format("<%s>: Server has stopped!\r\n", PREFIX);
            }
        }
    }

    private static class NoRFCFoundException extends Exception {
        public NoRFCFoundException(String message) {
            super(message);
        }
    }
}
