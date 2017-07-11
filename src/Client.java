import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Vector;

public class Client {

    private Socket socket = null;  // Client与Server通信的socket
    private PrintStream ps = null;  // Client向Server写消息
    private BufferedReader brServer = null;  // 读Server的消息
    private BufferedReader brKeyIn = null;  // 读键盘输入
    private ClientThread clientThread = null;

    private Vector<String> history = new Vector<>();
    private int history_cnt = 0;  // 历史消息的编号

    private void init() {

        /*
        Client初始化
         */
        try {
            socket = new Socket("127.0.0.1", 12345);
            System.out.println("Please login:");

            ps = new PrintStream(socket.getOutputStream());  // Client向Server写消息

            brServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));  // 读Server的消息
            brKeyIn = new BufferedReader(new InputStreamReader(System.in));  // 读键盘输入
        } catch (IOException e) {
            System.out.println("网络通信异常。");
            closeClient();
            System.exit(1);
        }

        /*
        Client登录
         */
        try {
            login();
        } catch (NullPointerException e) {
            System.out.println("客户端非法关闭。");
        } catch (IOException e) {
            System.out.println("由于服务器异常关闭，您已失去连接。");
        }

    }

    private void login() throws IOException {
        while (true) {

            String line = brKeyIn.readLine();  // 此处可能抛出异常
            if (line.startsWith("/login ")) {
                String userName = line.split("\\s+", 2)[1];
                if (userName.matches("")) {
                    System.out.println("用户名输入要求非空，请重新输入。");
                    continue;
                }
                ps.println(CrazyitProtocol.USER_ROUND + userName + CrazyitProtocol.USER_ROUND);
                String result = brServer.readLine();  // 此处可能抛出异常
                if (result.equals(CrazyitProtocol.NAME_REP)) {
                    System.out.println("Name exist, please choose another name:");
                } else if (result.equals(CrazyitProtocol.LOGIN_SUCCESS)) {
                    System.out.println("You have logined.");

                    /*
                    将于读取服务器消息的br传入客户端线程，并启动线程
                     */
                    clientThread = new ClientThread(brServer);
                    clientThread.start();
                    break;
                }
            } else if (line.equals("/quit")) {
                ps.println(CrazyitProtocol.QUIT);
                System.out.println("您已成功退出。");
                closeClient();
                System.exit(0);
            } else {
                System.out.println("Invalid command.");
            }

        }
    }

    private void readAndSend() {
        String line;
        try {
            while ((line = brKeyIn.readLine()) != null) {

                line = line.trim();  // 去除输入命令行两端的空格

                /*
                申请退出
                 */
                if (line.equals("/quit")) {
                    ps.println(CrazyitProtocol.QUIT);
                    closeClient();
                    break;
                }

                /*
                查询用户
                 */
                else if (line.equals("/who")) {
                    ps.println(CrazyitProtocol.WHO);
                }

                /*
                查询记录
                 */
                else if (line.startsWith("/history")) {
                    if (line.equals("/history")) {
                        // 默认情况，从最后一条记录开始，输出50条
                        show_history(history_cnt - 1, 50);
                    }

                    // 显示从start_index这一条开始，倒数max_count条记录。
                    else {
                        int start_index, max_count;
                        try {
                            start_index = Integer.parseInt(line.split("\\s+")[1]);
                            max_count = Integer.parseInt(line.split("\\s+")[2]);
                            if (start_index > history_cnt) {
                                System.out.println("目前最多只有" + history_cnt + "条记录，请重新输入查询历史指令：");
                                continue;
                            }
                            show_history(start_index, max_count);
                        } catch (NumberFormatException e) {
                            System.out.println("历史消息查询指令有误，若需要查询历史消息请输入此格式：\"/history 15 5\"");
                        }
                    }

                }

                /*
                私聊表情
                 */
                else if (line.startsWith("//") && line.indexOf(" ") > 0) {
                    line = line.substring(2);
                    ps.println(CrazyitProtocol.PRI_EMOTION + line.split("\\s+")[0] + CrazyitProtocol.SPLIT_SIGN + line.split("\\s+")[1] + CrazyitProtocol.PRI_EMOTION);
                }

                /*
                公屏表情
                 */
                else if (line.startsWith("//")) {
                    line = line.substring(2);
                    ps.println(CrazyitProtocol.ALL_EMOTION + line.split("\\s+")[0] + CrazyitProtocol.ALL_EMOTION);
                }

                /*
                指定私聊
                 */
                else if (line.startsWith("/to ")) {
                    ps.println(CrazyitProtocol.PRIVATE_ROUND + line.split("\\s+")[1] + CrazyitProtocol.SPLIT_SIGN + line.split("\\s+")[2] + CrazyitProtocol.PRIVATE_ROUND);
                }

                /*
                公屏发送
                 */
                else if (!line.isEmpty() && !line.startsWith("/")) {
                    ps.println(CrazyitProtocol.MSG_ROUND + line + CrazyitProtocol.MSG_ROUND);
                    System.out.println("你说：" + line);
                }

                /*
                line只包含空格 或 以'/'开头
                 */
                else {
                    System.out.println("Invalid input.");
                }

            }
        } catch (IOException e) {
            System.out.println("客户端线程读取键盘输入并发送异常。");
            closeClient();
            System.exit(1);
        }

    }

    private void show_history(int start_index, int max_count) {
        System.out.println("您的消息历史记录为：");
        while (max_count > 0 && start_index >= 0) {
            System.out.println(history.get(start_index));
            start_index--;
            max_count--;
        }
    }

    private void closeClient() {
        try {
            if (socket != null) {
                socket.close();
            }
            if (ps != null) {
                ps.close();
            }
            if (brServer != null) {
                brServer.close();
            }
            if (brKeyIn != null) {
                brKeyIn.close();
            }
            if (clientThread != null && clientThread.br != null) {
                clientThread.br.close();
            }
        } catch (IOException e) {
            System.out.println("客户端关闭异常。");
        }
    }

    /*
    读取Server消息并显示的线程
     */
    public class ClientThread extends Thread {
        BufferedReader br = null;

        ClientThread(BufferedReader br) {
            this.br = br;
        }

        /*
        读取Server消息并显示的函数
         */
        public void run() {
            String line;
            try {
                while ((line = br.readLine()) != null) {
                    System.out.println(line);
                    history.add(String.valueOf(history_cnt) + " " + line);  // 存入聊天记录
                    history_cnt++;
                }
            } catch (IOException e) {
//                    System.out.println("由于服务器异常关闭，您已退出聊天室。");  // 正常/quit也会捕捉到此异常
            } finally {
                this.closeClientThread();
            }
        }

        private void closeClientThread() {
            if (this.br != null) {
                try {
                    this.br.close();
                } catch (IOException e1) {
                    System.out.println("客户端线程 br 关闭异常。");
                }
            }
        }
    }

    public static void main(String[] args) {

        Client client = new Client();
        client.init();
        client.readAndSend();

        System.out.println("Quit success.");
    }

}
