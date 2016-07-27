import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class Main {

    private static final String HEADER_HANDSHAKE = "HANDSHAKE";
    private static final String COMMAND_HEADER = "COMMAND";


    private static String server = "";
    private static int port = 1111;

    public static void main(String[] args) {
        if (args.length == 0) return;
        String command;

        try {
            server = args[0];
            port = Integer.parseInt(args[1]);
            command = args[2];
        } catch (Exception e) {
            System.out.println("Usage: java -jar SafeAdmin.jar some.host.com port command");
            return;
        }

        Socket socket = null;
        DataOutputStream out = null;
        try {
            try {
                socket = new Socket();
                socket.connect(new InetSocketAddress(server, port), 10000);
                out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                // create handshake
                StringBuilder sbHandshake = new StringBuilder();
                sbHandshake.append("ADMIN");
                String handshake = sbHandshake.toString();
                // send handshake
                byte[] bHandshakeHeader = new byte[24];
                System.arraycopy(HEADER_HANDSHAKE.getBytes(), 0, bHandshakeHeader, 0, HEADER_HANDSHAKE.getBytes().length);
                // notify the server we are going to send a handshake
                //out.write(bHandshakeHeader, 0, bHandshakeHeader.length);
                out.writeInt(Protocol.HANDSHAKE);
                // notify of size
                out.writeInt(handshake.getBytes().length);
                // send handshake data
                out.write(handshake.getBytes(), 0, handshake.getBytes().length);
                out.flush();

                // sleep a while so server can relax
                Thread.sleep(100);

                // send command
                sendCommand(command, out);

            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                if (out != null) {
                    out.flush();
                    out.close();
                }
                if (socket != null) {
                    socket.close();
                }
            }
        } catch (SocketTimeoutException e) {
            System.out.println("Connection timed out, could not reach server");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sendCommand(String command, DataOutputStream out) {
        synchronized (out) { // wait for any other uploads or messages
            if (out != null) {
                try {
                    // notify server we are sending a command
                    out.writeInt(Protocol.COMMAND);
                    // notify server of the size of the command
                    out.writeInt(command.getBytes().length);
                    out.flush();

                    // convert command to bytes and send
                    byte[] commandBytes = command.getBytes();
                    out.write(commandBytes, 0, commandBytes.length);
                    out.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
