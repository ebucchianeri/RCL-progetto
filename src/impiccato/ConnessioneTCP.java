package impiccato;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class ConnessioneTCP extends Thread{
	protected ServerSocket socket = null;
	protected BufferedReader in = null;
	protected PrintWriter out;
	Server s;
	Socket clientUA;
	Partita partita;
	boolean timeout;
	boolean abbandonomaster;
	boolean abbandonoguesser;
	boolean iniziata;
	InetAddress indirizzo;
	//String pwd;
	
	
	public ConnessioneTCP(Socket c, Server s){
		super();
		this.s = s;
		clientUA = c;
		timeout = false;
		abbandonomaster = false;
		abbandonoguesser = false;
		iniziata = false;
		System.out.println("Creato nuovo thread con un giocatore");
		try {
			in = new BufferedReader(new InputStreamReader(c.getInputStream()));
			out = new PrintWriter(c.getOutputStream(),true);
		} catch(IOException e){
			e.printStackTrace();
		}
	}
	
	public void run() {
		String inputLine = null;
	    boolean errore = false;
	    boolean fine = false;
	    int eccezione = 0;
	    int stato = 0;
		try {
			clientUA.setSoTimeout(0);
			// Il thread attende di ricevere qualcosa dal client, se il client muore prima di aver 
			// inviato nome e cognome lui rimarra' bloccato nel while quindi devo anche usare un timer
			
			MessaggioTCP msg = null;
			MessaggioTCP risposta = null;
			boolean corretto = true;
			
			
			System.out.println("Mi metto in attesa di ricevere");
			while (errore == false && fine == false && (inputLine = in.readLine()) != null ) {
				System.out.println(inputLine);
				try {
					JSONObject dp = (JSONObject) new JSONParser().parse(inputLine);
					msg = new MessaggioTCP(dp);
				} catch (ParseException e) {
					e.printStackTrace();
					corretto = false;
				}
				if(corretto == true && msg.valore.intValue()==1 && msg.stato.intValue() == 1){
					System.out.println("Voglio diventare master");
					// Ho una richiesta di diventare master. Controllo col server se e' possibile.
					// In caso affermativo creo una nuova partita e rispondo.
					int riscontro = s.creaPartita(msg.nome, msg.k.intValue(), this);
					if(riscontro == 0)	{
						risposta = new MessaggioTCP(true, "OK");
						System.out.println("Creo la partita correttamente");
						out.println(risposta.toJson());
						// La partita e' stata creata, sospendo il thread in attesa dell'inizio
						// o di un risveglio per timero o abbandono.
						try {
							while(iniziata == false && timeout == false && abbandonomaster == false){
								synchronized (this) {
									this.wait();
								}
							}
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						// Quando mi risveglio controllo le 2 variabili abbandonomaster e timeout
						System.out.println("Mi sono svegliato");
						if(abbandonomaster){
							// Visto che io sono il thread che gestisce il master, devo semplicemente chiudere
							System.out.println("Il master ha abbandonato il gioco. Chiudo il thread");
							fine = true;
						} else if(timeout){
							System.out.println("Fine tempo per iniziare la partita. TIMEOUT. Lo comunico e chiudo il thread");
							risposta = new MessaggioTCP(false, true);
							out.println(risposta.toJson());
							fine = true;
						} else if(iniziata) {
							System.out.println("Si comincia la partita");
							// Invio al master
							risposta = new MessaggioTCP(true,this.partita.pwd, this.partita.indirizzo.getHostName(),this.s.parametri.getPortUDP(),this.partita.guesser);
							out.println(risposta.toJson());
						}
						
					}
					else if(riscontro == 1) {
						// Non si puo' creare una nuova partita
						risposta = new MessaggioTCP(false, "Impossibile creare la partita. Limite massimo raggiunto.");
						System.out.println("Impossibile creare alla partita");
						out.println(risposta.toJson());
						// Devo chiudere la connessione.
					}
					else if(riscontro == 2) {
						// Utente inesistente
						risposta = new MessaggioTCP(false, "Utente inesistente.");
						System.out.println("Utente inesistente");
						out.println(risposta.toJson());
						// Devo chiudere la connessione
					}
					
					
					// dopo aver inviato la risposta
				}
				else if(corretto == true && msg.valore.intValue()==1 && msg.stato.intValue() == 2){
					System.out.println("Voglio diventare guesser");
					// Ho una richiesta di diventare guesser di una parttia. Controllo col server se e' possibile.
					int riscontro = s.aggiungiAPartita(msg.nome, msg.master, this);
					if(riscontro == 0)	{
						// Sono l'ultimo, la partita va cominciata direttamente, non mi sospendo sulla wait, invio l'inizio
						// public MessaggioTCP(boolean b, String pas, String ind, int p){
						risposta = new MessaggioTCP(true,this.partita.pwd, this.partita.indirizzo.getHostName(),this.s.parametri.getPortUDP(),this.partita.max);
						out.println(risposta.toJson());
						System.out.println("Per me si comincia subito!!!");
						
					}
					else if(riscontro == 1) {
						// Mi sono unito alla partita correttamente
						risposta = new MessaggioTCP(true, "OK");
						System.out.println("mi sono unito correttamente");
						out.println(risposta.toJson());
						try {
							while(iniziata == false && timeout == false && abbandonomaster == false && abbandonoguesser == false){
								synchronized (this) {
									this.wait();
								}
							}
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						System.out.println("Guesser: Mi sono svegliato!!!!!");
						// Quando mi risveglio controllo le 3 variabili abbandonomaster e timeout e abbandonoguesser
						if(abbandonoguesser){
							System.out.println("Il guesser ha abbandonato il gioco. Chiudo il thread");
							fine = true;
						} else if(abbandonomaster){
							System.out.println("Il master ha abbandonato il gioco. Invio un messaggio di chiusura");
							//public MessaggioTCP(boolean a, boolean t){
							risposta = new MessaggioTCP(true, false);
							out.println(risposta.toJson());
							fine = true;
						} else if(timeout){
							System.out.println("Fine tempo per iniziare la partita. TIMEOUT. Lo comunico e chiudo il thread");
							risposta = new MessaggioTCP(false, true);
							out.println(risposta.toJson());
							fine = true;
						} else if(iniziata) {
							System.out.println("Si comincia la partita");
							// Invio al master
							risposta = new MessaggioTCP(true,this.partita.pwd,this.partita.indirizzo.getHostName(),this.s.parametri.getPortUDP(),this.partita.max);
							out.println(risposta.toJson());
						}
						
					} else if(riscontro == 2) {
						// Utente inesistente
						risposta = new MessaggioTCP(false, "Impossibile unirsi alla partita. La partita potrebbe non esistere.");
						System.out.println("Impossibile unirsi alla partita");
						out.println(risposta.toJson());
						// Devo chiudere la connessione
					} else if(riscontro == 3) {
						// Utente inesistente
						risposta = new MessaggioTCP(false, "Impossibile unirsi alla partita.");
						System.out.println("Impossibile unirsi alla partita");
						out.println(risposta.toJson());
						// Devo chiudere la connessione
					} else if(riscontro == 4) {
						// Utente inesistente
						risposta = new MessaggioTCP(false, "Utente inesistente.");
						System.out.println("Utente inesistente");
						out.println(risposta.toJson());
						// Devo chiudere la connessione
					}
					
					
					
					// dopo aver inviato la risposta
				} else {
					System.out.println("Messaggio non utile "+corretto);
				}
			}
			
		} catch(SocketTimeoutException e){
			// Se catturo un timout dalla socket significa che e' scaduto il tempo massimo
			System.out.println("Tempo scaduto per iniziare una partita");
			// Se sono un master - e il timeout scadra' prima a lui - devo chiudere la partita come se avessi un abbandono
			// del master.
			//Informazioni info = new Informazioni(false,"Timeout");
			//out.println(info.toJson());
			
		} catch(SocketException e){
			e.printStackTrace();			
		} catch(IOException e){
			e.printStackTrace();			
		}
		System.out.println("----------- THREAD CHIUSO ------------");
		
	}

}
