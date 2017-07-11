import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;

public class ServerThread extends Thread {

    private Socket socket = null;
    private BufferedReader br = null;  // 读客户端消息
    private PrintStream ps = null;  // 向客户端写消息

    private boolean clientClosedErr = true;

    /*
    接收客户端消息并回复的线程
     */
    ServerThread(Socket socket) {
        this.socket = socket;
    }

    public void run() {
        try {
            br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            ps = new PrintStream(socket.getOutputStream());
        } catch (IOException e) {
            System.out.println("服务器线程新建流异常。");
        }

        String line;
        try {
            while ((line = br.readLine()) != null) {

                clientClosedErr = false;
                /*
                处理退出命令
                 */
                if (line.equals(CrazyitProtocol.QUIT)) {

                    String nameTemp;
                    if ((nameTemp = Server.clients.getKeyByValue(ps)) != null) {
                        Server.clients.removeByValue(ps);
                        System.out.println(nameTemp + " 已登出。");
                    } else {
                        System.out.println("未登录用户已登出。");
                    }
                    for (PrintStream clientPs : Server.clients.valueSet()) {
                        clientPs.println(nameTemp + " has quit. ");
                    }
                    closeServerThread();
                    break;
                }

                /*
                处理登录命令
                 */
                else if (line.startsWith(CrazyitProtocol.USER_ROUND) && line.endsWith(CrazyitProtocol.USER_ROUND)) {
                    String userName = getRealMsg(line);
                    if (Server.clients.containsKey(userName)) {
                        System.out.println(userName + " 重复。");
                        ps.println(CrazyitProtocol.NAME_REP);
                    } else {
                        System.out.println(userName + " 登录成功！");
                        ps.println(CrazyitProtocol.LOGIN_SUCCESS);
                        Server.clients.put(userName, ps);
                        for (PrintStream clientPs : Server.clients.valueSet()) {
                            if (clientPs != ps) {
                                clientPs.println(userName + " has logined.");
                            }
                        }
                    }
                }

                /*
                处理用户查询
                 */
                else if (line.equals(CrazyitProtocol.WHO)) {
                    ps.println("Online users: ");
                    for (Object key : Server.clients.keySet()) {
                        ps.println(key);
                    }
                    ps.println("Total online user: " + Server.clients.size());
                }

                /*
                处理私聊表情
                 */
                else if (line.startsWith(CrazyitProtocol.PRI_EMOTION) && line.endsWith(CrazyitProtocol.PRI_EMOTION)) {
                    String userAndMsg = getRealMsg(line);
                    String emotion = userAndMsg.split(CrazyitProtocol.SPLIT_SIGN)[0];
                    String user = userAndMsg.split(CrazyitProtocol.SPLIT_SIGN)[1];
                    if (Server.EMOTIONS_PRI.containsKey(emotion)) {
                        if (Server.clients.get(user) != null) {
                            Server.clients.get(user).println(Server.clients.getKeyByValue(ps) + "向你" + Server.EMOTIONS_PRI.get(emotion));
                            ps.println("你向" + user + Server.EMOTIONS_PRI.get(emotion));
                        } else {
                            ps.println("您私聊表情的用户不存在，请重新输入聊天信息：");
                        }
                    }
                }

                /*
                处理公屏表情
                 */
                else if (line.startsWith(CrazyitProtocol.ALL_EMOTION) && line.endsWith(CrazyitProtocol.ALL_EMOTION)) {
                    String emotion = getRealMsg(line);
                    if (Server.EMOTIONS.containsKey(emotion)) {
                        ps.println("你向大家" + Server.EMOTIONS.get(emotion));
                        for (PrintStream clientPs : Server.clients.valueSet()) {
                            if (clientPs != ps) {
                                clientPs.println(Server.clients.getKeyByValue(ps) + "向大家" + Server.EMOTIONS.get(emotion));
                            }
                        }
                    } else {
                        ps.println("Invalid emotion.");
                    }
                }

                /*
                处理私聊
                 */
                else if (line.startsWith(CrazyitProtocol.PRIVATE_ROUND) && line.endsWith(CrazyitProtocol.PRIVATE_ROUND)) {
                    String userAndMsg = getRealMsg(line);
                    String user = userAndMsg.split(CrazyitProtocol.SPLIT_SIGN)[0];
                    String msg = userAndMsg.split(CrazyitProtocol.SPLIT_SIGN)[1];
                    if (!user.equals(Server.clients.getKeyByValue(ps))) {
                        if (Server.clients.containsKey(user)) {
                            Server.clients.get(user).println(Server.clients.getKeyByValue(ps) + "对你说：" + msg);
                            ps.println("你对" + user + "说：" + msg);
                        } else {
                            ps.println(user + " is not online.");
                        }

                    } else {
                        ps.println("Stop talking to yourself!");
                    }
                }

                /*
                处理公聊
                 */
                else {
                    String msg = getRealMsg(line);
                    for (PrintStream clientPs : Server.clients.valueSet()) {
                        if (clientPs != ps) {
                            clientPs.println(Server.clients.getKeyByValue(ps) + "说：" + msg);
                        }
                    }
                }

            }
        } catch (IOException e) {
            System.out.println("服务器线程消息读取异常。");
            Server.clients.removeByValue(ps);  // 捕捉不到这个异常
            closeServerThread();
        }
    }

    private void closeServerThread() {
        try {
            if (br != null) {
                br.close();
            }
            if (ps != null) {
                ps.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            System.out.println("服务器线程关闭异常。");
        }
    }

    private String getRealMsg(String line) {
        return line.substring(CrazyitProtocol.PROTOCOL_LEN, line.length() - CrazyitProtocol.PROTOCOL_LEN);
    }
}
