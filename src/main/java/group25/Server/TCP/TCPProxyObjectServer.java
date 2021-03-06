package group25.Server.TCP;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.io.*;
import java.net.*;
import java.lang.reflect.Method;

import group25.Server.Common.Trace;
import group25.Server.Common.Customer;

public class TCPProxyObjectServer {
    private String hostname;
    private int port;

    // map names to the proxy and real object pairs for all object to serve
    private HashMap<String, PairRealProxy> nameToObjectPair;
    private ServerSocket listenerSocket;
    private long boundCustomerCount;

    public TCPProxyObjectServer(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;

        nameToObjectPair = new HashMap<String, PairRealProxy>();
        try {
            listenerSocket = new ServerSocket(port);
            boundCustomerCount = 0;
        } catch (Exception err) {
            Trace.info("TCPProxyObjectServer new ServerSocket -> failed");
            System.exit(1);
        }
    }

    public synchronized boolean bind(String objectName, IProxiable object) {
        System.out.println("binding " + objectName);
        AbstractProxyObject proxyObject = null;
        try {
            proxyObject = object.makeProxyObject(hostname, port, objectName);
        } catch (RemoteException e) {
            Trace.info("TCPProxyObject::bind(IProxiable) -> failed. object not proxiable");
        }
        nameToObjectPair.put(objectName, new PairRealProxy(object, proxyObject));
        return true;
    }

    public synchronized AbstractProxyObject getProxy(String objectName) {
        return nameToObjectPair.get(objectName).proxyObject;
    }

    public synchronized IProxiable getReal(String objectName) {
        return nameToObjectPair.get(objectName).realObject;
    }

    public void runServer() {
        new Thread(() -> {
            while (true) {
                try {
                    Socket connectionSocket = listenerSocket.accept();
                    new Thread(() -> {
                        ObjectInputStream objectInputStream = null;
                        ObjectOutputStream objectOutputStream = null;
                        try {
                            objectInputStream =
                                    new ObjectInputStream(connectionSocket.getInputStream());
                            objectOutputStream =
                                    new ObjectOutputStream(connectionSocket.getOutputStream());
                        } catch (Exception err) {
                            err.printStackTrace();
                            Trace.info("TCPProxyObjectServer object stream initialization -> failed");
                            return;
                        }

                        Message inputMessage = null;
                        try {
                            inputMessage = (Message) objectInputStream.readObject();
                        } catch (Exception e) {
                            Trace.info("ProxyServer::(Message)readObject() -> invalid Message, failed to read object");
                            return;
                        }

                        Message outputMessage = null;
                        if (!(inputMessage instanceof ProxyMethodCallMessage)) {
                            System.out.println("serving " + inputMessage.proxyObjectBoundName);
                            // if it's not a ProxyMethodCallMessage, then it's just a method to request a proxy object
                            outputMessage = new Message();
                            try {
                                outputMessage.requestedValue = getProxy(inputMessage.proxyObjectBoundName);
                                outputMessage.requestSuccessful = true;
                            } catch (Exception e) {
                                Trace.info("ProxyServer::getProxy(" + inputMessage.proxyObjectBoundName + ") -> proxy object not found");
                                outputMessage.requestSuccessful = false;
                            }
                        } else if (inputMessage instanceof ProxyMethodCallMessage) {
                            ProxyMethodCallMessage PMCinputMessage = (ProxyMethodCallMessage) inputMessage;
                            ProxyMethodCallMessage PMCoutputMessage = new ProxyMethodCallMessage();

                            IProxiable realObject = null;
                            try {
                                realObject = getReal(PMCinputMessage.proxyObjectBoundName);
                            } catch (Exception e) {
                                Trace.info("ProxyServer::getReal(" + PMCinputMessage.proxyObjectBoundName + ") -> real object not found");
                                PMCoutputMessage.requestSuccessful = false;
                                try {
                                    objectOutputStream.writeObject(PMCoutputMessage);
                                } catch (Exception err) {
                                    Trace.info("ProxyServer could not send on output stream");
                                }
                                return;
                            }

                            Class cls = realObject.getClass();
                            Object outputObject = null;
                            try {
                                System.out.println(cls);
                                Method m = cls.getMethod(PMCinputMessage.methodName, PMCinputMessage.methodArgTypes);
                                outputObject = m.invoke(realObject, PMCinputMessage.methodArgs);
                            } catch (Exception e) {
                                e.printStackTrace();
                                Trace.info("ProxyServer could not invoke method " + PMCinputMessage.methodName + " -> failed");
                                PMCoutputMessage.requestedValue = null;
                                PMCoutputMessage.requestSuccessful = false;
                                try {
                                    objectOutputStream.writeObject(PMCoutputMessage);
                                } catch (Exception err) {
                                    Trace.info("ProxyServer could not send on output stream");
                                }
                                return;
                            }

                            PMCoutputMessage.requestedValue = outputObject;
                            PMCoutputMessage.requestSuccessful = true;

                            if (outputObject instanceof Customer) {
                                Customer customer = (Customer) outputObject;
                                AbstractProxyObject proxyCustomer;
                                try {
                                    proxyCustomer = getProxy(customer.getKey());
                                } catch (Exception err) {
                                    bind(customer.getKey(), customer);
                                    proxyCustomer = getProxy(customer.getKey());
                                }

                                PMCoutputMessage.requestedValue = proxyCustomer;
                                PMCoutputMessage.requestedValueIsCustomer = true;
                            }

                            outputMessage = PMCoutputMessage;
                        }

                        try {
                            objectOutputStream.writeObject(outputMessage);
                        } catch (Exception e) {
                            Trace.info("ProxyServer could not send on output stream");
                        }
                    }).start();
                } catch (Exception e) {
                    Trace.info("Couldn't initialize listener socket");
                    System.exit(1);
                }
            }
        }).start();
    }
}

class PairRealProxy {
    AbstractProxyObject proxyObject;
    IProxiable realObject;

    public PairRealProxy(IProxiable realObject, AbstractProxyObject proxyObject) {
        this.proxyObject = proxyObject;
        this.realObject = realObject;
    }
}
