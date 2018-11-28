package group25.Server.Common;

import group25.Server.Interface.*;
import group25.Utils.XMLPersistor;

import java.util.*;
import java.rmi.RemoteException;
import java.io.*;

public abstract class AbstractRMHashMapManager {
    protected RMHashMap globalState = new RMHashMap();
    protected HashMap<Integer, RMHashMap> transactionStates = new HashMap<>();
    protected XMLPersistor xmlPersistor = new XMLPersistor(RMHashMap.class);

    public RMHashMap getTransactionState(int xid) {
        synchronized(transactionStates) {
            if (transactionStates.containsKey(xid)) {
                return transactionStates.get(xid);
            }
            else {
                synchronized(globalState) {
                    transactionStates.put(xid, globalState.clone());
                    return transactionStates.get(xid);
                }
            }
        }
    }

    public boolean transactionExists(int xid) {
        return transactionStates.containsKey(xid);
    }

    public void removeTransactionState(int xid) {
        transactionStates.remove(xid);
    }

    public void commitGlobalState(int xid, String filename) {
        synchronized(globalState) {
            // update global state to contain all changes made to transaction-specific state
            RMHashMap m_data = transactionStates.get(xid);
            for (String i : m_data.keySet()) {
                globalState.put(i, m_data.get(i));
            }

            // remove the transaction-specific state
            transactionStates.remove(xid);

            // write to file
            xmlPersistor.writeObject(globalState, filename);
        }
    }

    public RMItem readData(int xid, String key) {
        RMHashMap m_data = getTransactionState(xid);
        synchronized (m_data) {
            RMItem item = m_data.get(key);
            if (item != null) {
                return (RMItem) item.clone();
            }
            return null;
        }
    }

    // Writes a data item
    public void writeData(int xid, String key, RMItem value) {
        RMHashMap m_data = getTransactionState(xid);
        synchronized (m_data) {
            m_data.put(key, value);
        }
    }

    // Remove the item out of storage
    public void removeData(int xid, String key) {
        RMHashMap m_data = getTransactionState(xid);
        synchronized (m_data) {
            m_data.remove(key);
        }
    }

    public boolean deleteItem(int xid, String key) {
        Trace.info("RM::deleteItem(" + xid + ", " + key + ") called");
        ReservableItem curObj = (ReservableItem) readData(xid, key);
        // Check if there is such an item in the storage
        if (curObj == null) {
            Trace.warn("RM::deleteItem(" + xid + ", " + key + ") failed--item doesn't exist");
            return false;
        } else {
            if (curObj.getReserved() == 0) {
                removeData(xid, curObj.getKey());
                Trace.info("RM::deleteItem(" + xid + ", " + key + ") item deleted");
                return true;
            } else {
                Trace.info("RM::deleteItem(" + xid + ", " + key + ") item can't be deleted because some customers have reserved it");
                return false;
            }
        }
    }

    // Query the number of available seats/rooms/cars
    public int queryNum(int xid, String key) {
        Trace.info("RM::queryNum(" + xid + ", " + key + ") called");
        ReservableItem curObj = (ReservableItem) readData(xid, key);
        int value = 0;
        if (curObj != null) {
            value = curObj.getCount();
        }
        Trace.info("RM::queryNum(" + xid + ", " + key + ") returns count=" + value);
        return value;
    }

    // Query the price of an item
    public int queryPrice(int xid, String key) {
        Trace.info("RM::queryPrice(" + xid + ", " + key + ") called");
        ReservableItem curObj = (ReservableItem) readData(xid, key);
        int value = 0;
        if (curObj != null) {
            value = curObj.getPrice();
        }
        Trace.info("RM::queryPrice(" + xid + ", " + key + ") returns cost=$" + value);
        return value;
    }

    public void shutdown() {
        System.exit(0);
    }
}
