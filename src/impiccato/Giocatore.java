package impiccato;

import java.io.Serializable;

class Giocatore implements Serializable, GiocatoreIF{
	private static final long serialVersionUID = 1L;
	String name;
	Object callback;
	int stato;
	transient ConnessioneTCP t;
	
	Giocatore(String n,Object c){
		name = n;		stato = 0; 		callback = c;
	}
	
	public synchronized void setConnessioneTCP(ConnessioneTCP c){
		t = c;
	}
	
	public synchronized void setStato(int i){
		stato = i;
	}
	

	public String toString() {
		return "Giocatore [name=" + name + ", stato=" + stato + "]";
	}

	public String getName() {
		return name;
	}
	
}