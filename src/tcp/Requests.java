package tcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Requests {
    static final String KEY_COMMAND = "command";

    static void consoleOutput(String prefix, String msg) {
        System.out.format("<%s>: %s\r\n", prefix, msg);
    }

    static List<String> readRequest(BufferedReader br) throws IOException {
        List<String> list = new ArrayList<>();
        String line;
        do {
            list.add(line = br.readLine());
        } while (!line.equals(MainServer.CODE_END) && !line.equals(MainServer.CODE_EXIT));
        return list;
    }

    static Map<String, String> getRequestMap(List<String> request) {
        Map<String, String> map = new HashMap<>();
        map.put(KEY_COMMAND, request.get(0).trim());
        int split;
        for (String line: request) {
            if (0 == (split = line.indexOf(':') + 1)) continue;
            map.put(line.substring(0, split).trim(), line.substring(split).trim());
        }
        return map;
    }

    static void consoleOutputRequest(String prefix, List<String> request) {
        for (String line: request)
            consoleOutput(prefix, line);
    }
}
