package impiccato;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
 
public interface ClientIF extends Remote {
    public void partiteDisponibili(ArrayList<PartitaIF> p) throws RemoteException;
    public void terminaMaster() throws RemoteException;
}