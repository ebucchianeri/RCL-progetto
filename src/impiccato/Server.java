package impiccato;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Server extends UnicastRemoteObject implements ServerIF {
	private static final long serialVersionUID = 1L;
	private UtentiRegistrati registrati;
	private UtentiOnline online;
	private PartiteDisponibili partite;
	private serverTCPAccept serverTCP;
	public ParametriServer parametri;
	
	public Server(ParametriServer p) throws RemoteException {
		parametri = p;
		registrati = new UtentiRegistrati();
		online = new UtentiOnline();
		partite = new PartiteDisponibili(p,online);
		//partite.creaPartita(new Giocatore("c",null), 5);
		
	 
		
		serverTCP = new serverTCPAccept(this,p.getPortTCP());
		serverTCP.start();
		
	}
	

	
	public static void main(String[] args) {
		// Prendo la configurazione da un file di config. Costruisco un oggetto parametri che contiene tutto quello che mi serve
		ParametriServer param = null;
		String l=null;
		JSONObject pj = null;
		
		try {
	        BufferedReader inputStream = new BufferedReader(new FileReader(args[0]));
				
	        while((l = inputStream.readLine() ) != null){
	         	pj = (JSONObject) new JSONParser().parse(l);
	         	param = new ParametriServer(pj);
			}
	        
	        
	        inputStream.close();
		} catch (IOException xe) {
			System.out.println("Errore nell'inizializzazione.");
		} catch (ParseException e) {
			System.out.println("Errore nella lettura del file di configurazione.");
		}
  

		try {
			System.setProperty("java.rmi.server.hostname", param.getIp());
	        Server objServer = new Server(param);
	        Registry reg = LocateRegistry.createRegistry(1111);
	        reg.rebind("MyServer", objServer);
	        System.out.println("Server Ready");
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
        
	}
	
	public int register(String n, String p) throws RemoteException {
		if(registrati.esiste(n)==false){
			// Se l'utente non esiste ancora lo inserisco
			registrati.aggiungi(new Utente(n,p));
			System.out.println("Client registrato");
			return 0;
		}
		else {
			// Se l'utente esiste gia'
			System.out.println("Client gia' registrato");
			return 1;
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see impiccato.ServerIF#login(java.lang.String, java.lang.String, java.lang.Object)
	 * 
	 * Funzione per loggarsi al server, prende come parametro nome, password e puntatore
	 * restituisce un intero
	 * 0: Login corretto ed effettuato
	 * 1: Utente non registrato
	 * 2: Utente gia' registrato.
	 * 3: Password errata
	 * 
	 */
	public int login(String n, String p, Object callback) throws RemoteException{
		if(registrati.esiste(n)==true){
			if(registrati.esiste(n,p)==true){
				// L'utente che richiede il login e' gia' registrato, controllo che non sia gia' loggato
				if(online.esiste(n)){
					System.out.println(n+" ha *GIA'* effettuato il login");
					return 2;
				} else {
					System.out.println(n+" ha effettuato il login");
					online.aggiungi(new Giocatore(n,callback));
					((ClientIF)callback).partiteDisponibili(partite.copia());
					return 0;
				}
			} else {
				System.out.println("Password errata.");
				return 3;
			}
		} else {
			System.out.println("Utente inesistente.");
			return 1;
		}
	}
	
	
	
	/*
	 * 
	 * 0: Abbandono corretto.
	 * 1: errore nell'abbadono
	 * 2: il giocatore e' gia' uscito
	 * 
	 */
	public int abbandonaPartita(String n) throws RemoteException{
		System.out.println("Il giocatore "+n+" chiede di abbandonare la partita");
		int ris = 0;
		// Devo assicurarmi che il giocatore sia master o guesser di qualche partita.
		Giocatore daeliminare = null;
		if(online.esiste(n)){
			daeliminare = online.get(n);
			
			if(daeliminare.stato == 1){
				System.out.println("Il giocatore era un master");
				// Quindi devo cancellare la partita. Puo essere iniziata gia'??
				// Se e' gia' iniziata chiudo le mie cose e lascio gestire il resto alla fase UDP.
				// devo riinviare l'insieme delle partite
				ris = partite.eliminaMaster(daeliminare);
				
			} else if( daeliminare.stato == 2){
				System.out.println("Il giocatore era un guesser");
				// Devo semplicemente decrementare il contatore dei giocatori e risvegliare il thread che
				// gestiva tale utente
				// devo riinviare l'insieme delle partite

				ris = partite.eliminaGuesser(daeliminare);
				
				
			}
			
			if(ris == 0){
				daeliminare.setConnessioneTCP(null);
				daeliminare.setStato(0);
				System.out.println(n+" ha effettuato l'abbandono della partita");
				online.inviaPartiteDisponibili(partite.copia());
				return 0;
			} else {
				return 1;
			}
		} else {
			System.out.println(n+" ha *GIA'* effettuato il logout");
			return 2;
		}
		
	}
	
	/*
	 * Funzione per creare una nuova partita, param: nome master, num giocatori, puntatore al thread
	 * connessioneTCP che gestisce tale userAgent
	 * restituisce un intero
	 * 0: Partita creata
	 * 1: Partita non creata - indirizzi terminati.
	 * 2: Utente non online.
	 * 
	 */
	public int creaPartita(String n, int m, ConnessioneTCP conn) throws RemoteException{
		if(online.esiste(n)){
			Giocatore master = online.get(n);
			if(partite.creaPartita(master, conn, m) == true) {
				online.inviaPartiteDisponibili(partite.copia());
				return 0;
			}
			else return 1;
		} else return 2;
	}
	
	/*
	 * Funzione per unirsi ad una partita esistente, param: nome utente, nome master, puntatore al thread
	 * connessioneTCP che gestisce tale userAgent
	 * restituisce un intero
	 * 0: Join corretto alla partita - sono l'ultimo e la partita va cominciata
	 * 1: Join corretto alla partita
	 * 2: Impossibile unirsi alla partita: patita completa
	 * 3: Impossibile unirsi alla partita: partita non esiste
	 * 4: Utente non online.
	 * 
	 */
	public int aggiungiAPartita(String n, String m, ConnessioneTCP conn) throws RemoteException{
		if(online.esiste(n)){
			Giocatore guesser = online.get(n);
			int rr = partite.joinPartita(guesser, m, conn);
			if( rr == 0) {
				// Sono entrato nella partita con sucesso e SONO sono l'ultimo giocatore. La partita deve cominciare.
				online.inviaPartiteDisponibili(partite.copia());
				return 0;
			}
			else if(rr == 1){
				// Sono entrato nella partita con sucesso, ma non sono l'ultimo giocatore
				online.inviaPartiteDisponibili(partite.copia());
				return 1;
			} else if( rr == 2){
				return 2;
			} else {
				return 3;
			}
		} else return 4;
	}
	
	
	/*
	 * Funzione per fare il logout dal server, prende come parametro nome, password
	 * restituisce un intero
	 * 0: Login corretto ed effettuato
	 * 1: Utente non in stato da logout
	 * 2: Utente gia' uscito.
	 * 
	 */
	public int logout(String n) throws RemoteException{
		System.out.println("Il giocatore "+n+" chiede il logout");
		// Quando faccio il logout devo assicurarmi che il giocatore non sia master o guesser di qualche partita.
		Giocatore daeliminare = null;
		if(online.esiste(n)){
			daeliminare = online.get(n);
			if(daeliminare.stato==0){
				System.out.println("Il giocatore ha effettuato il logout");
				online.remove(n);
				return 0;
			} else {
				return 1;
			}
		} else {
			System.out.println("Il giocatore ha *GIA'* effettuato il logout");
			return 2;
		}
	}

	/* Funzione invocata dai guesser che vogliono abbandonare la fase UDP. La partita al
	 * server risulta iniziata ed e' completamente gestita dal master. Si decrementa il
	 * contatore dei giocatori
	 * 
	 */
	public int leave(String m, String n) throws RemoteException{
		System.out.println(n + " il guesser vuole abbandonare");
		return partite.abbandonoGuesser(m, n);
	}
	
	/* Funzione invocata dal master che vuole abbandonare la fase UDP. La partita al
	 * server risulta iniziata ed e' completamente gestita dal master. Non si devono avvirtire i guesser
	 * che gia' sanno dell'abbandono del master 
	 * La funzione puo' essere chiamata dal master anche in caso di terminazione della partita
	 */
    public int close(String m) throws RemoteException{
    	System.out.println("Il MASTER " +m+ " vuole terminare la partita");
    	return partite.chiudiPartita(m);
    }
    
    public int remove(String n, String p) throws RemoteException{
    	System.out.println("L' utente " +n+ " ha richiesto la cancellazione");
    	if(this.registrati.esiste(n, p)){
    		if(this.online.esiste(n)){
    			this.logout(n);
    			System.out.println("Prima, l'utente " +n+ " e' uscito");
    		}
    		this.registrati.remove(n,p);
    		return 0;
    	}
    	else return 1;
    }


}
