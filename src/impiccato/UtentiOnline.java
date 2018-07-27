package impiccato;

import java.rmi.RemoteException;
import java.util.ArrayList;

public class UtentiOnline {
	private ArrayList<Giocatore> utenti;
	public int numeroutenti;
	
	public UtentiOnline(){
		utenti = new ArrayList<Giocatore>();
		numeroutenti = 0;
	}
	
	
	public synchronized boolean esiste(String n){
		for(Giocatore t : utenti){
			if(t.name.equals(n)){
				return true;
			}
		}
	return false;
	}
	
	public synchronized Giocatore get(String n){
		for(Giocatore t : utenti){
			if(t.name.equals(n)){
				return t;
			}
		}
	return null;
	}
	
	public synchronized void aggiungi(Giocatore u){
		utenti.add(u);
		this.numeroutenti++;
		this.stampaUtenti();
	}
	
	public synchronized void remove(String n){
		Giocatore daelim = null;
		for(Giocatore t : utenti){
			if(t.name.equals(n)){
				daelim = t;
			}
		}
		utenti.remove(daelim);
		this.numeroutenti--;
		this.stampaUtenti();
	}
	
	public void stampaUtenti(){
		System.out.println("UTENTI ON LINE:");
		for(Giocatore g : utenti){
			System.out.println(g.toString());
		}
	}
		
	public void inviaPartiteDisponibili(ArrayList<PartitaIF> o) throws RemoteException{
		for( Giocatore g : utenti){
			if(g.stato==0){
				((ClientIF)g.callback).partiteDisponibili(o);
			}
		}
	}
	
}
