package impiccato;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.regex.Pattern;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class ClientInizio extends UnicastRemoteObject implements ClientIF {
	private static final long serialVersionUID = 1L;
	ArrayList<PartitaIF> partitericevute;
	BufferedReader stdIn;
	String nomeutente;
	String password;
	int stato;
	ServerIF server;
	Parametri param;
	
	
	int richiestanuovostato;
	int numerogiocatori;
	String masterrichiesto;
	
	Socket Socket = null;
	PrintWriter out = null;
	BufferedReader in = null;
	
	AttesaInput ai = null;
	boolean abbandono;
	boolean fine;
	boolean iniziogioco;
	
	String pwd;
	String indirizzo;
	int porta;
	ArrayList<String> sfidanti;
	
	Guesser threadguesser;
	Master threadmaster;
	
	public ClientInizio(ServerIF server,Parametri param) throws RemoteException {
		int rispostas = 0;
		this.server = server;
		this.param = param;
		abbandono = false;
		stato = 0;
		stdIn = new BufferedReader(new InputStreamReader(System.in));
		
		
		String s = null;
		int numerodati = 2;
		int datiletti = 0;
		try {
			System.out.println("Inserisci il tuo nome utente:");
			while (datiletti<2 && (s = stdIn.readLine())!=null){
				if( datiletti == 0 && !s.equals("") ){
					nomeutente = s;
					datiletti++;
					System.out.println("Inserisci la tua password:");
				} else if(datiletti == 1 && !s.equals("")){
					password = s;
					datiletti++;
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		param.nome = nomeutente;
		param.pwd = password;
		System.out.println("Sono "+param.getNome());
		// Provo a fare il login, se non sono registrato mi registro.
		rispostas = server.login(param.getNome(), param.getPwd(), this);
		if(rispostas == 1){
			// Non sono registrato.
			System.out.println("Non sono registrato, mi registro.");
			rispostas = server.register(param.getNome(), param.getPwd());
			if(rispostas == 1) {
				System.out.println("Errore nella registrazione. Il client viene chiuso.");
				System.exit(1);
			} else {
				System.out.println("Registrazione corretta. Adesso faccio il login.");
				rispostas = server.login(param.getNome(), param.getPwd(), this);
				if(rispostas == 1){
					System.out.println("Errore nel login.  Il client viene chiuso.");
					System.exit(1);
				} else if(rispostas == 2){
					System.out.println("Utente gia' loggato. Il client viene chiuso.");
					System.exit(1);
				}
			}
		} else if(rispostas == 2){
			System.out.println("Utente gia' loggato. Il client viene chiuso.");
			System.exit(1);
		}
		else if(rispostas == 3){
			System.out.println("Utente registrato. Password errata. Il client viene chiuso.");
			System.exit(1);
		}
		System.out.println("Utente gia' iscritto. Mi sono autenticato.");
		inizio();
	}
	
	public static void main(String[] args) {
		// Prendo la configurazione da un file di config. Costruisco un oggetto parametri che contiene tutto quello che mi serve
		Parametri param = null;
		String l=null;
		JSONObject pj = null;
		ServerIF server = null;
		
		try {
	        BufferedReader inputStream = new BufferedReader(new FileReader(args[0]));
				
	        while((l = inputStream.readLine() ) != null){
	         	pj = (JSONObject) new JSONParser().parse(l);
	         	param = new Parametri(pj);
			}
	        
	        
	        inputStream.close();
		} catch (IOException xe) {
			System.out.println("Errore nell'inizializzazione.");
		} catch (ParseException e) {
			System.out.println("Errore nella lettura del file di configurazione.");
		}
        System.setProperty("java.rmi.server.hostname", param.getIpc());
        
        try {
			server = (ServerIF) Naming.lookup("rmi://"+param.getIps()+":1111/MyServer");
			ClientInizio c = new ClientInizio(server,param);
		} catch (MalformedURLException | RemoteException | NotBoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
       
        
	}
	
	public void inizio(){
		fine = false;
		String userInput = null;
		
		try {
			while (fine == false){
				System.out.println("Inserire  master <numero intero> per creare una nuova partita");
    			System.out.println("Inserire  guesser <nome master della partita> per partecipare ad una partita");
    			//System.out.println("Il mio stato e' "+stato);
				if((userInput = stdIn.readLine()) != null) {
			    	System.out.println(userInput);
		
			    	
			    	// ## LOGOUT
			    	if(userInput.equals("logout")){
			    		if(server.logout(param.getNome())==0){
			    			System.out.println("Logout effettuato. Bye.");
			    			System.exit(1);
			    		} else {
			    			System.out.println("Errore nel logout. Il client viene chiuso");
			    			System.exit(1);
			    		}
			    	}
			    	// ## Rimozione utente
			    	else if(userInput.equals("cancella")){
			    		if(server.remove(param.getNome(),param.getPwd())==0){
			    			System.out.println("Logout e cancellazione effettuati. Bye.");
			    			System.exit(1);
			    		} else {
			    			System.exit(1);
			    		}
			    	}
			    	// ## CAMBIO STATO
			    	else if (stato == 0){
			    		boolean errore = false;
			    		if (userInput.contains(" ")) {
			    			String[] parts = userInput.split(Pattern.quote(" "));
			    			if(parts[0].equals("master")){
			    				System.out.println("Richiedo di essere master");
			    				int ng = 0;
			    				try{
			    					ng = Integer.parseInt(parts[1]);
			    				} catch (NumberFormatException ex){
			    					errore = true;
			    					System.err.println("Il comando master deve essere seguito da un intero.");
			    				}
			    				if(errore == false) {
			    					richiestanuovostato = 1;
			    					numerogiocatori = ng;
			    					master();
			    					if(this.iniziogioco){
			    						System.out.println("GIOCO");
			    						threadmaster = new Master(this.param.getNome(),this.numerogiocatori,this.param.getIpc(),this.indirizzo,this.pwd,this.porta,this.sfidanti,ai,this.server);
			    						threadmaster.start();
			    						try {
											threadmaster.join();
										} catch (InterruptedException e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										}
			    						// Riporto lo stato 0 e chiudo la partita.
			    						System.out.println("Ho finito di giocare");
			    						stato = 0;
			    					}
			    				}
			    				
			    			} else if(parts[0].equals("guesser")){
			    				if(parts.length == 2){
			    					richiestanuovostato = 2;
			    					System.out.println("Richiedo di essere guesser");
			    					masterrichiesto = parts[1];
			    					guesser();
			    					if(this.iniziogioco) {
			    						System.out.println("GIOCO");
			    						threadguesser = new Guesser(this.masterrichiesto,this.param.getNome(),this.numerogiocatori,this.param.getIpc(),this.indirizzo,this.pwd,this.porta,ai,this.server);
			    						threadguesser.start();
			    						try {
											threadguesser.join();
										} catch (InterruptedException e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										}
			    						// Riporto lo stato 0 e chiudo la partita.
			    						System.out.println("Ho finito di giocare");
			    						stato = 0;
			    					}
			    				}	
			    			} else {}
			    		
			    		}
			    	}
				}
			}
			System.out.println("Chiudo il client");
			System.exit(0);
		} catch( RemoteException ex){
			
		} catch( IOException ex){
			
		}
	}
	
	
	public void master(){
		abbandono = false;
		try {
			Socket = new Socket(param.getIps(), param.getPortServerTCP());
			out = new PrintWriter(Socket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(Socket.getInputStream()));
			System.out.println("Ho aperto una connessione TCP col server");
		} catch (UnknownHostException e) {
			System.err.println("Don't know about host.");
			System.exit(1);
		} catch (IOException e) {
			System.err.println("Couldn't get I/O for the connection.");
			System.exit(1);
		}
		
		// Devo aprire il thread che e' in attesa del logout.
		ai = new AttesaInput(this,this.param,stdIn,1,server);
		ai.start();
	
		// Preparo i dati da inviare al server per la mia richiesta di diventare master.
		// public MessaggioTCP(String n, String p, int s, int l){
		MessaggioTCP msg = new MessaggioTCP(param.getNome(),1,numerogiocatori);
		System.out.println(msg);
		out.println(msg.toJson());
		
		// Mi devo mettere in attesa di un messaggio dal server:
		String input = null;
		
		boolean corretto = true;
		boolean fineletturadasocket = false;
		iniziogioco = false;
		
		try {
			Socket.setSoTimeout(0);
			System.out.println(abbandono);
				while (iniziogioco == false && abbandono == false && fineletturadasocket == false && (input = in.readLine()) != null ) {
					//System.out.println(input);
					try {
						JSONObject dp = (JSONObject) new JSONParser().parse(input);
						msg = new MessaggioTCP(dp);
					} catch (ParseException e) {
						e.printStackTrace();
						corretto = false;
					}
					if(corretto == true && msg.valore.intValue() == 2){
						// Ho ricevuto un messaggio di risposta
						if(msg.partitacreata == false){
							System.out.println("Impossibile creare partita: " + msg.errore  );
							fineletturadasocket = true;
						} else if(msg.partitacreata){
							System.out.println("Aggiunto alla partita come master");
							this.stato = 1;
						}
					} else if(corretto == true && msg.valore.intValue() == 4){
						if(msg.timeout){
							System.out.println("TIMEOUT");
							fineletturadasocket = true;
						}
					} else if(corretto == true && msg.valore.intValue() == 3){
						System.out.println("La partita sta per cominciare!!!!!!!!!");
						iniziogioco = true;
					}
				}
			System.out.println(abbandono);
			} catch (SocketException e) {
				// La socket e' stata chiusa in modo brutale, 
				if(abbandono == true){
					this.stato = 0;
					System.out.println("------------- FINE PARTITA ------------");
				} else {
					e.printStackTrace();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		if(fineletturadasocket){
			// Se ho dovuto terminare in modo corretto o meno.
			System.out.println("------------- FINE PARTITA ------------");
			stato = 0;
			endTCP();
			if(ai.isAlive()){
				ai.sblocco = true;
				System.out.println("Scrivi qualcosa per terminare la partita");
			}
		} else if(iniziogioco) {
			System.out.println("------------- *INIZIO* PARTITA ------------");
			endTCP();
			this.pwd = msg.pwd;
			this.indirizzo = msg.indirizzo;
			this.porta = msg.porta.intValue();
			this.sfidanti = new ArrayList<String>();
			for(String gg : msg.giocatori){
				sfidanti.add(gg);
			}
		}
	}	
	
	
	public void guesser(){
		abbandono = false;
		try {
			Socket = new Socket(param.getIps(), param.getPortServerTCP());
			out = new PrintWriter(Socket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(Socket.getInputStream()));
			System.out.println("Ho aperto una connessione TCP col server");
		} catch (UnknownHostException e) {
			System.err.println("Don't know about host.");
			System.exit(1);
		} catch (IOException e) {
			System.err.println("Couldn't get I/O for the connection.");
			System.exit(1);
		}
	
		
		// Devo aprire il thread che e' in attesa del logout.
		ai = new AttesaInput(this,this.param,stdIn,2,server);
		ai.start();
		
		// Preparo i dati da inviare al server per la mia richiesta di diventare master.
		// public MessaggioTCP(String n, String p, int s, int l){
		MessaggioTCP msg = new MessaggioTCP(param.getNome(),2,masterrichiesto);
		System.out.println(msg);
		out.println(msg.toJson());
		
		// Mi devo mettere in attesa di un messaggio dal server:
		String input = null;
		boolean corretto = true;
		boolean fineletturadasocket = false;
		iniziogioco = false;
		
		try {
			Socket.setSoTimeout(0);
			System.out.println(abbandono);
				while (iniziogioco == false && abbandono == false && fineletturadasocket == false && (input = in.readLine()) != null ) {
					//System.out.println(input);
					try {
						JSONObject dp = (JSONObject) new JSONParser().parse(input);
						msg = new MessaggioTCP(dp);
					} catch (ParseException e) {
						e.printStackTrace();
						corretto = false;
					}
					if(corretto == true && msg.valore.intValue() == 2){
						// Ho ricevuto un messaggio di risposta
						if(msg.partitacreata == false){
							System.out.println("Impossibile unirsi alla partita: " + msg.errore  );
							fineletturadasocket = true;
						} else if(msg.partitacreata){
							System.out.println("Aggiunto alla partita come guesser");
							this.stato = 2;
						}
					} else if(corretto == true && msg.valore.intValue() == 4){
						if(msg.abbandonomaster){
							System.out.println("Abbandono del master");
							fineletturadasocket = true;
							
						} else if(msg.timeout){
							System.out.println("TIMEOUT");
							fineletturadasocket = true;
			
						} 
					} else if(corretto == true && msg.valore.intValue() == 3){
						System.out.println("La partita sta per cominciare!!!!!!!!!");
						iniziogioco = true;
					}
				}
			System.out.println(abbandono);
			} catch (SocketException e) {
				// La socket e' stata chiusa in modo brutale, 
				if(abbandono == true){
					this.stato = 0;
					System.out.println("------------- FINE PARTITA ------------");
				} else {
					e.printStackTrace();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		if(fineletturadasocket){
			// Se ho dovuto terminare in modo corretto o meno.
			System.out.println("------------- FINE PARTITA ------------");
			endTCP();
			stato = 0;
			if(ai.isAlive()){
				ai.sblocco = true;
				System.out.println("Scrivi qualcosa per terminare la partita");
			}
		} else if(iniziogioco) {
			System.out.println("------------- *INIZIO* PARTITA ------------");
			endTCP();
			this.pwd = msg.pwd;
			this.indirizzo = msg.indirizzo;
			this.numerogiocatori = msg.k.intValue();
			this.porta = msg.porta.intValue();
		}
	}
	
	
	public void endTCP(){
		System.out.println("Chiudo la connessione TCP col server");
		
		try {
			Socket.close();
			in.close();
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public void terminaMaster() throws RemoteException {
		this.threadmaster.endGame();
	}
	
    public void partiteDisponibili(ArrayList<PartitaIF> p) throws RemoteException {
    	if(p!=null){
    		partitericevute = p;
	    	System.out.println("Le partite disponibili sono:");
	    	if(p.size() == 0) System.out.print("\t Nessuna partita disponibile\n");
	    	else {
	    		for(PartitaIF pa : p){
					System.out.print("\t Master:"+pa.getMaster()+" Numero gioc:"+pa.getNumeroGiocatori()+"su"+pa.getNumeroMaxGiocatori()+"\n");
				}
	    	}
    	}
    }

}
