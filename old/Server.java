import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class Server {


    static HashMap<String, String> EMOTIONS = new HashMap<>();  // 公屏表情对照
    static HashMap<String, String> EMOTIONS_PRI = new HashMap<>();  // 私聊表情对照

    static CrazyitMap<String, PrintStream> clients = new CrazyitMap<>();  // HashMap保存用户名与用户输出流
    Socket socket = null;  // 与客户端相连的socket
    private ServerSocket ss = null;

    private void init() throws IOException {
        ss = new ServerSocket(12345);
        System.out.println("服务器开启成功！");
        while (true) {
            socket = ss.accept();
            new ServerThread(socket).start();  // 服务器端为每一个客户端建立一个线程用于通信（每个客户端自己有两个线程）
        }
    }

    private void closeServer() {
        if (this.ss != null) {
            try {
                this.ss.close();
            } catch (IOException e1) {
                System.out.println("服务器关闭异常。");
            }
        }
    }

    public static void main(String[] args) {

        // 建立表情对照表
        EMOTIONS.put("smile", "脸上泛起了无邪的笑容");
        EMOTIONS.put("hi", "打招呼，\"Hi, 大家好！我来咯~\"");

        EMOTIONS_PRI.put("smile", "脸上泛起了无邪的笑容");
        EMOTIONS_PRI.put("hi", " 打招呼，\"Hi, 你好啊~\"");
        Server server = new Server();
        try {
            server.init();
        } catch (IOException e) {
            System.out.println("服务器开启异常，检查端口号。");
            server.closeServer();
            System.exit(1);
        }
    }
}
