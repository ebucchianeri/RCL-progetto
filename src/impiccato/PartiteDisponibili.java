package impiccato;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class PartiteDisponibili {
	private static final String AB = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
	private ArrayList<Partita> partite;
	private HashMap<InetAddress,Boolean> indirizzi;
	private long indirizzoi;
	private long indirizzof;
	private UtentiOnline utentionline;
	
	public int numeropartite;
	public int numeropartitemassime;
	
	public PartiteDisponibili(ParametriServer p, UtentiOnline u){
		partite = new ArrayList<Partita>();
		numeropartite = 0;
		utentionline = u;
		indirizzi = inizializzaIndirizzi(p);
		if(indirizzi.size() == 0 || indirizzi == null){
			System.out.println("Errore nell'assegnazione degli indirizzi multicast");
			System.exit(1);
		}
		numeropartitemassime = indirizzi.size();
		System.out.println(numeropartitemassime); 
		for (InetAddress i: indirizzi.keySet()){
            String key = i.getHostName();
            Boolean value = indirizzi.get(i);  
            System.out.println(key + " " + value);  
		}	
		
	}
	
	
	public synchronized int eliminaMaster(Giocatore g){
		Partita pa = null;
		
		for(Partita p : partite){
			if(p.master.name.equals(g.name)){
				pa = p;
			}
		}
		if(pa!=null){
			pa.eliminaPartitaAbbandonoMaster();
			System.out.println("Ho iniziato la chiusura della partita");
			partite.remove(pa);
			this.numeropartite = this.numeropartite -1;
			this.indirizzi.put(pa.indirizzo, false);
			for (InetAddress i: indirizzi.keySet()){
	            String key = i.getHostName();
	            Boolean value = indirizzi.get(i);  
	            System.out.println(key + " " + value);  
			}
			return 0;
			
		}
		else return 1;
	}
	
	/* Funzione di PartiteDisponibili per eliminare un guesser, prima viene cercata la partita e se esiste tale guesser,
	 * dopo ne viene chiesta la rimozione. Return:
	 * 0: Rimozione corretta
	 * 1: errore;
	 */
	public synchronized int eliminaGuesser(Giocatore g){
		// Devo cercare un guesser
		ArrayList<Giocatore> guesser = null;
		Partita pa = null;
		Giocatore daelim = null;
		
		for(Partita p : partite){
			guesser = p.guesser;
			for(Giocatore gg : guesser){
				if(gg.name.equals(g.name)){
					daelim = gg;
					pa = p;
				}
			}
		}
		if(pa!=null && daelim!=null){
			pa.eliminaGuesser(daelim);
			return 0;
		}
		else return 1;
		
	}
	
	/* Funzione per abbandonare la partita in fase di gioco.
	 * 0:
	 * 1:
	 * 2:
	 * 3: La partita non esiste
	 */
	public synchronized int abbandonoGuesser(String m, String n){
		Partita pa = null;
		
		for(Partita p : partite){
			if(p.master.name.equals(m)){
				pa = p;
			}
		}
		if(pa!=null){
			return pa.abbandonoGuesser(n);
		}
		else return 3;
	}
	
	/* Funzione per creare una nuova partita, restituisce
	 * - true se e' possibile creare una nuova partita
	 * - false se non e' possibile
	 */
	public synchronized boolean creaPartita(Giocatore m, ConnessioneTCP c, int k){
		if(this.numeropartite < this.numeropartitemassime){
			String i = this.prossimoIndirizzoLibero();
			System.out.println(i);
			m.setConnessioneTCP(c); // salvo il thread che gestisce il master nella partita
			m.setStato(1); // il giocatore diventa master
			
			// Genero la password per il gioco
			String password = randomString(5);
			Partita p = new Partita(m,k,password,this);
			try {
				p.indirizzo = InetAddress.getByName(i); // salvo l'indirizzo dedicato
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
			partite.add(p);		
			numeropartite++;
			return true;
		} else {
			return false;
		}
	}
	
	/* Funzione che permette ad un giocatore di unirsi ad una partita.
	 * Restituisce:
	 * 0: Se la partita deve essere cominciata subito, il numero di giocatori e' stato raggiunto
	 * 1: il giocatore si e' unito ma ancora mancano giocatori
	 * 2: la partita e' al completo
	 * 3: errore, la partita non e' stata trovata
	 */
	public synchronized int joinPartita(Giocatore g, String m, ConnessioneTCP c){
		Partita pa = null;
		for(Partita p : partite){
			if(p.master.getName().equals(m)){
				pa = p;
			}
		}
		if(pa == null) return 3;
		else {
			g.setConnessioneTCP(c);
			g.setStato(2);
			int r2 = pa.join(g);
			if(r2 == 0){
				// Si deve cominciare la partita.
				return 0;
			} else if(r2 == 1){
				// La partita non e' ancora al completo
				return 1;
			} else if(r2 == 2){
				// La partita non e' ancora al completo
				g.setConnessioneTCP(null);
				g.setStato(0);
				return 2;
			}
			return 0;
		}
	}
	
	public synchronized int timerscaduto(Giocatore m){
		Partita darimuovere = null;
		for(Partita p : partite){
			if(p.master.name.equals(m.name)){
				System.out.println("E' terminata devo rimuoverla");
				darimuovere = p;
			}
		}
		this.numeropartite = this.numeropartite - 1;
		partite.remove(darimuovere);
		inviaPartite();
		return 0;
	}
	
	/* Funzione per chiudere una partita che e' invocata dall'oggetto partita stessa. 
	 * Quando un guesser abbandona una partita e viene raggiunto il numero 0 di guesser la partita viene chiusa,
	 * avvisando in RMI callback il master. 
	 */
	public synchronized void chiudiPartita(Partita pa){
		// Libero l'indirizzo che era in uso.
		InetAddress i = pa.indirizzo;
		indirizzi.put(i, false);
		partite.remove(pa);
		numeropartite--;
		inviaPartite();
	}
	
	/* Funzione per chiudere una partita invocata dal server, su richiesta RMI del master, che vuole abbandonarla
	 * in fase di gioco. Non e' necessario che lo segnali ai guesser in quanto cio' e' fatto dal master stesso
	 * in fase di chiusura.
	 * 0: chiusura corretta
	 * 1: la partita non esiste
	 */
	public synchronized int chiudiPartita(String m){
		Partita pa = null;
		for(Partita p : partite){
			if(p.master.name.equals(m)){
				pa = p;
			}
		}
		if (pa!= null){
			// Libero l'indirizzo che era in uso.
			InetAddress i = pa.indirizzo;
			indirizzi.put(i, false);
			pa.chiudiPartita(m);
			partite.remove(pa);
			numeropartite--;
			this.inviaPartite();
			utentionline.stampaUtenti();
			return 0;
		}
		else return 1;
	}
	
	/* Metodo invocato dalla partita stessa che, dopo aver ricevuto un timeout (a gioco iniziato) chiude la partita
	 * I giocatori potrebbero tutti esser terminati in modo anomalo, e ne viene effettuato il logout.
	 * 0: Rimozione corretta
	 * 1: Partita non esistente
	 */
	public synchronized int eliminaPartita(Giocatore m){
		Partita pa = null;
		for(Partita p : partite){
			if(p.master.name.equals(m.name)){
				pa = p;
			}
		}
		if(pa!=null){
			partite.remove(pa);
			numeropartite--;
			InetAddress i = pa.indirizzo;
			indirizzi.put(i, false);
			utentionline.remove(m.name);
			for(Giocatore g : pa.guesser){
				utentionline.remove(g.name);
			}
			return 0;
		}
		else return 1;
	}
	
	public void inviaPartite(){
		try {
			utentionline.inviaPartiteDisponibili(this.copia());
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public ArrayList<PartitaIF> copia(){
		//Copio solo l'array, no gli elementi all interno
		return (ArrayList<PartitaIF>)partite.clone();
	}
	
	/* Funzione di inizializzazione dell hash per la gestione degli indirizzi a disposizione.
	 * Ogni volta che viene richiesto il prossimo indirizzo libero esso viene segnato come occupato
	 */
	private HashMap<InetAddress,Boolean> inizializzaIndirizzi(ParametriServer p) {
		HashMap<InetAddress,Boolean> ind = new HashMap<InetAddress,Boolean>();

			String inizio = p.getFirstI();
			String fine = p.getLastI();
			InetAddress ii = null;
			InetAddress ff = null;
			long ipi = 0;
		    long ipf = 0;
			try {
				ii = InetAddress.getByName(inizio);
				ff = InetAddress.getByName(fine);
				ipi = ipToLong(InetAddress.getByName(inizio)); indirizzoi = ipi;
			    ipf = ipToLong(InetAddress.getByName(fine)); indirizzof = ipf;

		
				if(!inizio.equals(fine) && ii.isMulticastAddress() && ff.isMulticastAddress()){
					long inte = ipf - ipi;
					System.out.println(ipi +"  "+ ipf +" "+ inte);
					
					// Itero da 0 a inte, perche' io indico nel fine i due estremi compresi!
					for(int i = 0; i<=inte; i++){
						System.out.println(ipi+i);
						ind.put(InetAddress.getByName(longToIp(ipi+i)),false);
					}
				}
			} catch (UnknownHostException ex){}
			return ind;
			
	}
		
	private static long ipToLong(InetAddress ip) {
        byte[] octets = ip.getAddress();
        long result = 0;
        for (byte octet : octets) {
            result <<= 8;
            result |= octet & 0xff;
        }
        return result;
	}

	private static String randomString( int len ){
		Random rnd = new Random();
		StringBuilder sb = new StringBuilder( len );
		for( int i = 0; i < len; i++ ) 
	      sb.append( AB.charAt( rnd.nextInt(AB.length()) ) );
		return sb.toString();
	}
	
	private static String longToIp(long ip) {
	    StringBuilder sb = new StringBuilder(15);

	    for (int i = 0; i < 4; i++) {
	        sb.insert(0, Long.toString(ip & 0xff));

	        if (i < 3) {
	            sb.insert(0, '.');
	        }

	        ip >>= 8;
	    }

	    return sb.toString();
	  }
	
	private synchronized String prossimoIndirizzoLibero(){
		if(numeropartite < this.numeropartitemassime){
			String ind = null;
			long inte = indirizzof - indirizzoi;
			
			// Itero da 0 a inte, perche' io indico nel fine i due estremi compresi!
			boolean trovato = false;
			int i = 0;
			while( i<=inte && trovato == false){
				System.out.println(indirizzoi+i);
				InetAddress ii = null;
				try {
					ii = InetAddress.getByName(longToIp(indirizzoi+i));
					//System.out.println(ii.getHostAddress());
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if( indirizzi.get(ii) == false){
					// Ne trovo uno dove usato e' false?
					//System.out.println("L'indirizzo scelto e' "+ii);
					indirizzi.put(ii,true);
					trovato = true;
					ind = longToIp(indirizzoi+i);
					//System.out.println(ind);
				}
				i++;
			}
			return ind;
		} else {
			return null;
		}
	}
}

