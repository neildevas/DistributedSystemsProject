package Server.TCP;

import java.net.Socket;
import java.io.*;

import Server.Common.*;
import Server.Interface.*;

public class TCPFlightResourceManager extends FlightResourceManager implements IProxyResourceManagerGetter {
    private static String s_serverName = "FlightServer";
    private static String s_tcpPrefix = "group25_";

    public static void main(String[] args) {
        TCPProxyObjectServer server = new TCPProxyObjectServer("localhost", 2000);
        TCPFlightResourceManager flightRM = new TCPFlightResourceManager(s_serverName);
        flightRM.customerRM = (ICustomerResourceManager) flightRM.getProxyResourceManager("localhost", 2003, "CustomerServer");

        server.bind(s_serverName + s_tcpPrefix, flightRM);
        server.runServer();
        System.out.println("'" + s_serverName + "' resource manager server ready and bound to '" + s_tcpPrefix + s_serverName + "'");
    }

    public AbstractProxyObject getProxyResourceManager(String hostname, int port, String boundName) {
        Message messageToSend = new Message();
        messageToSend.proxyObjectBoundName = s_tcpPrefix + boundName;
        while (true) {
            try {
                Socket socket = new Socket(hostname, port);

                ObjectOutputStream osOut = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream  osIn  = new ObjectInputStream(socket.getInputStream());

                osOut.writeObject(messageToSend);

                try {
                    Message messageReceived = (Message) osIn.readObject();
                    return (AbstractProxyObject) messageReceived.requestedValue;
                } catch (Exception e) {
                    Trace.info(s_serverName + ": expected customerRM to be AbstractProxyObject. Cast failed.");
                    e.printStackTrace();
                    System.exit(1);
                }
            } catch (Exception e) {
                Trace.info(s_serverName + " waiting for customer server");
                try {
                    Thread.sleep(500);
                } catch (Exception err) {
                    Trace.info("TCPFlightResourceManager::getProxyResourceManager -> Thread sleep failed");
                }
            }
        }
    }

    public TCPFlightResourceManager(String name) {
        super(name);
    }
}