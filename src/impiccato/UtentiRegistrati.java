package impiccato;

import java.util.ArrayList;

public class UtentiRegistrati {
	private ArrayList<Utente> utenti;
	public int numeroutentiregistrati;
	
	public UtentiRegistrati(){
		utenti = new ArrayList<Utente>();
		numeroutentiregistrati = 0;
	}
	
	// E' giusto creare un metodo di sola lettura sincronizzato??
	public synchronized boolean esiste(String n){
		for(Utente t : utenti){
			if(t.name.equals(n)){
				return true;
			}
		}
	return false;
	}
	
	public synchronized boolean esiste(String n, String p){
		for(Utente t : utenti){
			if(t.name.equals(n) && t.pwd.equals(p)){
				return true;
			}
		}
	return false;
	}
	
	public synchronized void aggiungi(Utente u){
		utenti.add(u);
		this.numeroutentiregistrati++;
	}
	
	public synchronized void remove(String n, String p){
		Utente daelim = null;
		for(Utente ut : utenti){
			if(ut.name.equals(n) && ut.pwd.equals(p)){
				daelim = ut;
			}
		}
		utenti.remove(daelim);
		this.numeroutentiregistrati--;
		System.out.println("L' utente "+ n+" e' stato cancellato. ");
	}
}
