package ru.project;

import java.io.IOException;
import java.net.BindException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;

public class App {
    static ServerSocketChannel servChannel;
    static SocketChannel clientChannel;
    static Selector selector;
    static Scanner scan = new Scanner(System.in);

    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            usage("Слишком мало аргументов \n");
        } else {
            if (args[0].equals("client")) {
                configClient(args[1], args[2]);
                while (true) {
                    String request = "";
                    while (request.isEmpty()) {
                        System.out.println("Введите число которое будет отправлено на сервер, или STOP для завершения работы");
                        request = scan.nextLine().trim();
                        try {
                            if(!request.equals("STOP")) {
                                int intRequest = Integer.parseInt(request);
                                if(intRequest < 0 ){
                                    System.out.println("Введенное значение не может быть меньше нуля");
                                    request = "";
                                }
                            }
                        } catch (NumberFormatException e) {
                            System.out.println("Введенное значение не является числом");
                            request = "";
                        }
                    }
                    System.out.print(sendMessageWithResponse(request));
                    if (request.trim().equals("STOP")) {
                        exit();
                    }
                }
            } else if (args[0].equals("server")) {
                configServer(args[1], args[2]);
                ByteBuffer buffer = ByteBuffer.allocate(1024);
                while (true) {
                    selector.select();
                    Set<SelectionKey> selectedKeys = selector.selectedKeys();
                    Iterator<SelectionKey> iter = selectedKeys.iterator();
                    while (iter.hasNext()) {
                        SelectionKey key = iter.next();
                        if (key.isAcceptable()) {
                            register(selector, servChannel);
                        }
                        if (key.isReadable()) {
                            answerFib(buffer, key);
                        }
                        iter.remove();
                    }
                }
            } else {
                usage("Неизвестный первый аргумент\n");
            }
        }
    }

    private static void exit() throws IOException {
        if (servChannel == null) {
        } else {
            servChannel.close();
        }
        if (clientChannel == null) {
        } else {
            clientChannel.close();
        }
        if (selector == null) {
        } else {
            selector.close();
        }
        System.exit(0);
    }

    private static void usage(String message) {
        System.out.print(message);
        System.out.println("usage: client/server [host] [port]");
        System.exit(0);
    }

    private static Integer readPortIfNeeded() throws IOException {
        int result = -1;
        System.out.println("Введите новое значение порта. Если не хотите, введите пустую строку");
        while (result == -1) {
            String str = scan.nextLine();
            if (str.trim().isEmpty()) {
                exit();
            } else {
                try {
                    result = Integer.parseInt(str);
                } catch (NumberFormatException e) {
                    System.out.println("Введенное значение не является числом");
                    result = -1;
                }
            }
        }
        return result;
    }

    private static String readPHostIfNeeded() throws IOException {
        String host;
        System.out.println("Введите новое значение хоста. Если не хотите, введите пустую строку");
        host = scan.nextLine();
        if (host.isEmpty()) {
            exit();
        }
        return host;
    }

    private static void configServer(String host, String port) throws IOException {
        selector = Selector.open();
        servChannel = ServerSocketChannel.open();
        while (true) {
            try {
                int intPort = Integer.parseInt(port);
                servChannel.bind(new InetSocketAddress(host, intPort));
                break;
            } catch (NumberFormatException e) {
                System.out.println("Порт не является числом");
                port = String.valueOf(readPortIfNeeded());
            } catch (IllegalArgumentException e) {
                System.out.println("Значение порта недоступно системно");
                port = String.valueOf(readPortIfNeeded());
            } catch (BindException e) {
                System.out.println("Такой хост недоступен");
                host = readPHostIfNeeded();
            }
        }
        servChannel.configureBlocking(false);
        servChannel.register(selector, SelectionKey.OP_ACCEPT);
    }

    private static void configClient(String host, String port) throws IOException {
        while (true) {
            try {
                int intPort = Integer.parseInt(port);
                clientChannel = SocketChannel.open(new InetSocketAddress(host, intPort));
                break;
            } catch (NumberFormatException e) {
                System.out.println("Порт не является числом");
                port = String.valueOf(readPortIfNeeded());
            } catch (IllegalArgumentException e) {
                System.out.println("Значение порта недоступно системно");
                port = String.valueOf(readPortIfNeeded());
            } catch (SecurityException e) {
                System.out.println("Такой хост недоступен");
                host = readPHostIfNeeded();
            } catch (ConnectException e) {
                System.out.println("Такая комбинация хоста и порта не доступна");
                host = readPHostIfNeeded();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        clientChannel.configureBlocking(false);
    }

    private static void register(Selector selector, ServerSocketChannel serverSocket)
            throws IOException {

        SocketChannel client = serverSocket.accept();
        client.configureBlocking(false);
        client.register(selector, SelectionKey.OP_READ);
    }

    private static void answerFib(ByteBuffer buffer, SelectionKey key)
            throws IOException {

        SocketChannel client = (SocketChannel) key.channel();
        int byteCounter;
        byteCounter = client.read(buffer);
        if (byteCounter == -1) {
            client.close();
            System.out.println("Клиент отсоединился");
            return;
        }
        String fromClient = new String(Arrays.copyOfRange(buffer.array(), 0, byteCounter)).trim();
        System.out.println("От клиента пришло " + fromClient);
        if (fromClient.equals("STOP")) {
            client.close();
            System.out.println("Клиент отсоединился");
        } else {
            int intFromClient;
            String answer;
            try {
                intFromClient = Integer.parseInt(fromClient);
                answer = fib(intFromClient) + "\n";
            } catch (NumberFormatException e) {
                answer = "Сервер принимает только целые числа больше 0";
            }
            System.out.println("Отправляю клиенту " + answer);
            buffer.flip();
            client.write(ByteBuffer.wrap(answer.getBytes()));
            buffer.clear();
        }
    }

    private static String sendMessageWithResponse(String message) throws IOException {
        ByteBuffer bufferRequest = ByteBuffer.wrap(message.getBytes());
        ByteBuffer bufferResponse = ByteBuffer.allocate(1024);
        String response = null;
        try {
            clientChannel.write(bufferRequest);
            bufferRequest.clear();
            while (clientChannel.read(bufferResponse) == 0) {
            }
            clientChannel.read(bufferResponse);
            response = new String(bufferResponse.array());
            bufferResponse.clear();
        } catch (IOException e) {
            System.out.println("Сервер здох");
            exit();
        }
        return response;
    }

    static int fib(int n) {
        if (n < 0) {
            throw new NumberFormatException();
        }
        if (n <= 1)
            return n;
        return fib(n - 1) + fib(n - 2);
    }
}
