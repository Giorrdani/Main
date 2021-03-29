package clientside.one;

import serverside.service.MyServer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.Socket;

public class EchoClient extends JFrame {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                new EchoClient();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        });
    }

    private final static String IP_ADDRESS = "localhost"; //127.0.0.1 ip address
    private final static int SERVER_PORT = 8081;

    FileOutputStream cash = new FileOutputStream("src\\liveCash.txt");

    private JTextField msgInputField;
    private JTextArea chatArea;

    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;
    private static boolean timer = true;
    private MyServer myServer;

    private boolean isAuthorized;

    public EchoClient() throws FileNotFoundException {
        try {
            connection();
        } catch (IOException ignored) {
            ignored.printStackTrace();
        }
        prepareGUI();
    }

    public void setAuthorized(boolean authorized) {
        isAuthorized = authorized;
    }

    private void connection() throws IOException {
        socket = new Socket(IP_ADDRESS, SERVER_PORT);
        dis = new DataInputStream(socket.getInputStream());
        dos = new DataOutputStream(socket.getOutputStream());
        setAuthorized(false);
        Thread thread = new Thread(() -> {
            try {
                new Thread(() -> {
                    long timeMillis = System.currentTimeMillis();
                    while (timer) {
                        if (System.currentTimeMillis() - timeMillis >= 120000 && isAuthorized == false) {
                            System.out.println("Time authorized is end");
                            closeConnection();
                            timer = false;
                            break;
                        }
                    }
                }).start();
                while (true) {
                    try {
                        String serverMessage = dis.readUTF();
                        if (serverMessage.startsWith("/auth ok - ")) {
                            setAuthorized(true);
                            chatArea.append(serverMessage + "\n");

                            break;
                        }

                        chatArea.append(serverMessage + "\n");
                    } catch (IOException ignored) {
                    }
                }
                while (true) {
                    String serverMessage = dis.readUTF();
                    if (serverMessage.equals("/q")) {
                        break;
                    }
                    chatArea.append(serverMessage + "\n");
                }

            } catch (IOException ignored) {
                ignored.printStackTrace();
            }
            closeConnection();
        });
        thread.start();
    }

    private void sendMessageToServer() {
        if (!msgInputField.getText().trim().isEmpty()) {
            try {
                String messageToServer = msgInputField.getText();
                dos.writeUTF(messageToServer);
                cashChat(chatArea);
                msgInputField.setText("");
            } catch (IOException ignored) {
            }
        }
    }

    private void cashChat (JTextArea chatArea) throws IOException {
        try (FileWriter writer = new FileWriter("src\\liveCash.txt", false)) {
            writer.write(chatArea.getText());
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

    public void prepareGUI() {
        setBounds(600, 300, 500, 500);
        setTitle("Клиент");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);

        chatArea = new JTextArea();
        chatArea.setPreferredSize( new Dimension(390, 500));
        chatArea.setEnabled(false);
        chatArea.setBackground(new Color(15, 200, 150));
        chatArea.setFont(new Font("Century Gothic", Font.PLAIN, 20));
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setDisabledTextColor(Color.WHITE);



        add(new JScrollPane(chatArea), BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        JButton btnSendMsg = new JButton("Отправить");
        bottomPanel.add(btnSendMsg, BorderLayout.EAST);
        msgInputField = new JTextField();
        add(bottomPanel, BorderLayout.SOUTH);
        bottomPanel.add(msgInputField, BorderLayout.CENTER);

        btnSendMsg.addActionListener(e -> {
            sendMessageToServer();
        });

        msgInputField.addActionListener(e -> {
            sendMessageToServer();
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                try {
                    dos.writeUTF("/q");
                } catch (IOException ignored) {
                }
            }
        });

        setVisible(true);
    }

    private void closeConnection() {
        try {
            dos.flush();
        } catch (IOException ignored) {
        }

        try {
            dis.close();
        } catch (IOException ignored) {
        }

        try {
            dos.close();
        } catch (IOException ignored) {
        }

        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }
}
