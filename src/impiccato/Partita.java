package impiccato;

import java.io.Serializable;
import java.net.InetAddress;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

class Partita implements Serializable, PartitaIF {
	// Alcuni campi sono dichiarati transient in quanto non e' necessario che vengano inviati
	// ai giocatori nell'elenco delle partite aperte.
	private static final long serialVersionUID = 1L;
	transient PartiteDisponibili PA;
	Giocatore master;
	transient ArrayList<Giocatore> guesser;
	int n;
	int max;
	boolean iniziata;
	transient boolean terminata;
	String pwd;
	transient InetAddress indirizzo;
	transient Timer timer;
	transient Timer timerfine;
	
	
	public String getMaster(){
		return master.name;
	}
	
	public int getNumeroGiocatori(){
		return n;
	}
	
	public int getNumeroMaxGiocatori(){
		return max;
	}
	
	
	// Classe per il timer della partita in fase di apertura.
	class Task extends TimerTask {
		public void run() {
			System.out.println("La partita deve essere chiusa");
			// Sveglio il master
			master.t.timeout = true;
			synchronized (master.t) {
				master.t.notify();
			}
			master.setStato(0);
			master.setConnessioneTCP(null);
			// Sveglio i giocatori
			for(Giocatore gg : guesser){
				gg.t.timeout = true;
				synchronized (gg.t) {
					gg.t.notify();
				}
				gg.setStato(0);
				gg.setConnessioneTCP(null);
			}
			timer.cancel();
			PA.timerscaduto(master);
		 }
	}
		
	// Classe per il timer della partita in fase di apertura.
	class TaskChiudiPartita extends TimerTask {
		public void run() {
			System.out.println("TIMEOUT!!");
			if(terminata == false){
				System.out.println("La partita deve essere chiusa. Si suppone non ci siano piu' giocatori");
				System.out.println("Il master "+master.name+" e' online?");
				master.setStato(0);
				// Sveglio i giocatori
				for(Giocatore gg : guesser){
					gg.setStato(0);
				}
				timerfine.cancel();
				PA.eliminaPartita(master);
			}
			else timerfine.cancel();
		 }
	}


	/*  
	 */
	public Partita(Giocatore m, int k, String ps, PartiteDisponibili p){
		PA = p;
		guesser = new ArrayList<Giocatore>();
		master = m;
		n = 1;
		max = k;
		iniziata = false;
		pwd = ps;
		
		// Avvio il timer per l'inizio della partita
		timer = new Timer();
		timer.schedule(new Task(), 40*1000);
	}
	
	/*
	 * 0: Mi sono aggiunto con successo e solo l'ultimo
	 * 1: Mi sono aggiunto con successo ma non sono l'ultimo
	 * 2: La partita e' gia' al completo
	 */
	public int join(Giocatore g){
		if(iniziata == false) {
			if(n<max){
				n++;
				if(n == max) { 
					// fermo il timer!!!!
					terminata = false;
					timer.cancel();
					
					System.out.println("La partita deve cominciare");
					iniziata = true;
					// Sveglio il master
					master.t.iniziata = true;
					master.t.partita = this;
					synchronized (master.t) {
						master.t.notify();
					}
					// Sveglio i giocatori
					for(Giocatore gg : guesser){
						gg.t.iniziata = true;
						gg.t.partita = this;
						synchronized (gg.t) {
							gg.t.notify();
						}
						
					}
					guesser.add(g);
					g.t.iniziata = true;
					g.t.partita = this;
					
					// Inizializzo il timer massimo:
					timerfine = new Timer();
					timerfine.schedule(new TaskChiudiPartita(), 120*1000);
					
					return 0;
				}
				else {
					guesser.add(g);
					return 1;
				}
			} else return 2;
		} else return 2;
	}
	
	
	public void eliminaPartitaAbbandonoMaster(){
		if(iniziata == false){
			// FErmo il timer
			timer.cancel();
			// Se la partita non e' ancora cominciata devo segnalare ai guesser l'abbandono del master.
			// E svegliare tutti i thread dei client.
			System.out.println("La partita deve essere chiusa");
			// Sveglio il master
			master.t.abbandonomaster = true;
			master.t.partita = this;
			master.setStato(0);
			
			synchronized (master.t) {
				master.t.notify();
			}
			master.setConnessioneTCP(null);
			// Sveglio i giocatori
			for(Giocatore gg : guesser){
				gg.t.abbandonomaster = true;
				gg.t.partita = this;
				synchronized (gg.t) {
					gg.t.notify();
				}
				gg.setStato(0);
				gg.setConnessioneTCP(null);
			}
			
			
		} else {
			// Se e' gia' iniziata ho poco da fare, devo 
			// SOLO CAMBIaRE GLI Stati
		}
	}
	
 	public void eliminaGuesser(Giocatore g){
		n--;
		guesser.remove(g);
		g.t.abbandonoguesser = true;
		g.t.partita = this;
		synchronized (g.t) {
			g.t.notify();
		}
	}
	
 	/* La funzione per far abbandonare la partita in fase UDP. Il contatore dei giocatori
 	 * viene decrementato e si controlla di non aver guesser = 0. In tal caso la partita va chiusa.
 	 * 0: Rimozione corretta
 	 * 1: Rimozione corretta, chiudo anche la partita
 	 * 2: La partita non e' ancora cominciata
 	 */
 	public int abbandonoGuesser(String n){
 		Giocatore g = null;
 		if(iniziata){
 			// Se la partita e' iniziata
 			for(Giocatore gg : guesser){
 				if(gg.name.equals(n)){
 					g = gg;
 				}
 			}
 			this.n = this.n -1;
 			g.stato = 0;
 			guesser.remove(g);
 			
 			if(guesser.size() == 0){
 				// In partita ho solo il master quindi chiudo la partita
 				System.out.println("Devo chiudere anche " + master.name + " che e' solo in partita");
 				chiudiPartita();
 				return 1;
 			} else {
 				return 0;
 			}
 			
 		} return 2;
 	}
	
 	/* Metodo che permette di chiudere una partita perche' l'ultimo guesser ha abbandonato la partita.
 	 * 
 	 */
 	public void chiudiPartita(){
 		this.terminata = true;
 		for(Giocatore g: guesser){
 			g.stato = 0;
 		}
 		guesser = null;
 		master.stato = 0;
 		ClientIF c = (ClientIF)master.callback;
 		try {
			c.terminaMaster();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
 		// dovrei anche fermare un futuro timer??
 		if(timerfine!=null) timerfine.cancel();
 		PA.chiudiPartita(this);
 	}
 	
 	public void chiudiPartita(String m){
 		this.terminata = true;
 		for(Giocatore g: guesser){
 			g.stato = 0;
 		}
 		guesser = null;
 		master.stato = 0;
 		// dovrei anche fermare un futuro timer??
 	 	if(timerfine!=null) timerfine.cancel();
 		
 	}
 	
 	@Override
	public String toString() {
		return "Partita [master=" + master + ", n=" + n + ", max=" + max
				+ ", iniziata=" + iniziata + ", pwd=" + pwd + "]";
	}


}
