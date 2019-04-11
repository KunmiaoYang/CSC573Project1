package tcp;

import file.LocalStorage;
import file.RFC;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static tcp.Client.BUF_SIZE;
import static tcp.Client.TRANS_DELAY;
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
                // Set socket buffer
                socket.setSendBufferSize(32*1024);

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

                // get file
                File file = rfc.path.toFile();

                // transmit data
                try (InputStream fis = new FileInputStream(file)) {
                    // Response after open the file to deal with the case when the same
                    // file overwrite itself. The client wouldn't open the file until this
                    // header is arrived. So when opening the same file, it would be blocked until
                    // this file has already been closed here.
                    pw.format("%s %d %s\r\n", Client.VERSION, STATUS_OK, PHRASE_OK);
                    pw.format("%s %s\r\n", HEADER_DATE, new Date().toString());
                    pw.format("%s %s\r\n", HEADER_OS, OS);
                    pw.format("%s %s\r\n", HEADER_TITLE, rfc.title);
                    pw.format("%s %s\r\n", HEADER_LAST_MODIFIED, new Date(file.lastModified()).toString());
                    pw.format("%s %s\r\n", HEADER_CONTENT_LENGTH, file.length());
                    pw.format("%s %s\r\n", HEADER_CONTENT_TYPE, RFC.CONTENT_TYPE);
                    pw.println(CODE_END);
                    pw.flush();

                    byte[] buf = new byte[BUF_SIZE];
                    int nr = 0;
                    for (int num = fis.read(buf); num != -1; num = fis.read(buf)) {
                        TimeUnit.MILLISECONDS.sleep(TRANS_DELAY);
                        os.write(buf, 0, num);
                        os.flush();
                        nr += num;
                    }
                    System.out.println("Data sent: " + nr);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } catch (NoRFCFoundException e) {
                System.out.format("<%s>: %s\r\n", PREFIX, e.toString());
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
