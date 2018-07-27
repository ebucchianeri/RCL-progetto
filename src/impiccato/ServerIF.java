package impiccato;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ServerIF extends Remote {
	public int register(String n, String p) throws RemoteException;
    public int login(String n, String p, Object callback) throws RemoteException;
    public int abbandonaPartita(String n) throws RemoteException;
    public int logout(String n) throws RemoteException;
    public int leave(String m, String n) throws RemoteException;
    public int close(String m) throws RemoteException;
    public int remove(String n, String p) throws RemoteException;
}
