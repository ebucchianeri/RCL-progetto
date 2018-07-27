package impiccato;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.ReentrantLock;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.jasypt.contrib.org.apache.commons.codec_1_3.binary.Base64;
import org.jasypt.exceptions.EncryptionOperationNotPossibleException;
import org.jasypt.util.text.BasicTextEncryptor;
import org.jasypt.util.text.TextEncryptor;

public class Master extends Thread {
	String nome;
	String niface;
	ArrayList<String> giocatori;
	int numerog;
	int port;
	MulticastSocket socketm;
	String indirizzogruppo;
	InetAddress group;
	boolean finegioco; // Variabile che comanda la fine del ciclo di attesa sulla socket
	int tentativi;
	int tentativitotali;
	boolean indovinata;
	ArrayList<Punti> punteggi;
	boolean timeoutdigioco;
	boolean abbandonotuttiguesser;
	boolean abbandonomaster;
	Timer timer = null;
	BufferedReader stdIn = null;
	String chiaves;
	AttesaInput ai;
	ServerIF server;

	
	// Classe per il timer della partita.
	class Task extends TimerTask {

		public void run() {
		    // Mando un messaggio di timeout in modo che tutti, sia master che guesser si sblocchino dalla socket e inizino
			// la procedura di chiusura della partita
			Messaggio rm = new Messaggio('E',false,tentativitotali,"",false,true,true,false);
			try {
				send(socketm,group,port,rm.toJson().toString());
				finegioco = true;
				timeoutdigioco = true;
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		 }

	}
	
	
	public Master(String nomem, int ngioc, String ip, String indgroup, String pss, int porta, ArrayList<String> avversari, AttesaInput ai, ServerIF s){
		stdIn = new BufferedReader(new InputStreamReader(System.in));
		finegioco = false;
		tentativi = 0; 
		tentativitotali = 10;
		abbandonomaster = false; 
		timeoutdigioco = false;
		this.chiaves = pss;
		this.nome = nomem;
		this.niface = ip;
		this.indirizzogruppo = indgroup;
		try {
			group = InetAddress.getByName(this.indirizzogruppo);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.port = porta;
		this.giocatori = avversari;
		this.numerog = ngioc;
		this.ai = ai;
		this.server = s;
		
		abbandonotuttiguesser = false;
		//setParametriPerFaseUDP(Guesser g,BufferedReader stdIn, Master mm, Guesser gg, boolean gf, LockAttesaAck llock, InetAddress inda, int pp, String chiav){
		ai.setParametriPerFaseUDP(null,stdIn,this,null,false,null,group,port,chiaves);
		
	}
	
	private String encrypt(String s){
		BasicTextEncryptor textEncryptor = new BasicTextEncryptor();
		textEncryptor.setPassword(chiaves);
		String risultato = textEncryptor.encrypt(s);
		return risultato;
	}

	private String decrypt(String s){
		BasicTextEncryptor textEncryptor = new BasicTextEncryptor();
		textEncryptor.setPassword(chiaves);
		//System.out.println(s);
		String plainText = textEncryptor.decrypt(s);
		return plainText;
	}
	
	/* Funzione che serve per riceve dalla socket.
	 * Prende come parametro la socket di multicast e restituisce la stringa ricevuta 
	 */
	private String receive(MulticastSocket s) throws UnknownHostException, IOException {
		String str = null;
		byte[] buf = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        s.receive(packet);
        String received = new String(packet.getData(), 0, packet.getLength());
        System.out.println("Ricevuto il pacchetto in broadcast");
        try {
        	str = decrypt(received);
        } catch (EncryptionOperationNotPossibleException ex){
        	str = null;
        }
        return str;
	}
	
	/* Funzione per inviare un messaggio in multicast
	 * Prende come parametro la socket, l'indirizzo del gruppo e la stringa da inviare
	 */
	private void send(MulticastSocket s, InetAddress group, int port, String frase) throws UnknownHostException, IOException {
    	String stringa = encrypt(frase);
		byte[] buf = new byte[1024];
    	buf = stringa.getBytes();
    	DatagramPacket pkt = new DatagramPacket(buf,buf.length,group,port);
        s.send(pkt);
        System.out.println("Inviato il pacchetto in broadcast");
	}
	
	public void run(){
		try {
			
			System.out.println("Sonooo "+this.nome);
			// Fase di inizializzazione del multicast. Costruisco la socketm
			socketm = new MulticastSocket(port);
			InetAddress iface = InetAddress.getByName(niface);
			socketm.setInterface(iface);
			socketm.joinGroup(group);
			socketm.setSoTimeout(6000);
			
			// Attendo esattamente n-1 messaggi, uno da ogni guesser, prima di cominciare la partita.
			// In ogni caso ho settato il timeout della socketm a 6 secondi.
			String s = null;
			Messaggio i = null;
			System.out.println("Sfidanti: ");
			List<String> gioc = giocatori;
			for(String str : gioc){
				System.out.println(str);
			}
			for(int t = 0; t < numerog -1; t++){
				try {
					s = receive(socketm);
				} catch (SocketTimeoutException ex){
					System.out.println("timeout in attesa di giocatore");
				}
			}
			
			
			// Appena ho ricevuto tutti i messaggi o dopo un determinato periodo di tempo inizio la fase di preparazione del gioco
			punteggi = new ArrayList<Punti>();
			for(String gnome : giocatori){
				punteggi.add(new Punti(gnome,0));
			}
			char lettere[] = { 'a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z' };
			boolean letterachiesta[] = { false,false,false,false,false,false,false,false,
					false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,
					false,false };
			

			
			//ai.start();
			// Dico al threa che siamo nella fase di gioco.
			// In questa prima fase devo riceve la parola da indovinare quindi mi sospendo su una condition
			// in attesa di essere svegliata dal thread che attende sullo stdIn
			ai.fasegioco = true;
			String data = null;
			try {
				System.out.println("Scrivere la parola da indovinare: ");
				ai.lock.lock();
				ai.stop.await();
				ai.lock.unlock();
				s = ai.ricevuta;

			}catch(InterruptedException e){
				System.out.println("prooooblem");
			}
			
			// Appena mi sveglio dalla condition verifico che non si abbia avuto un abbandono del master e continuo.
			if(this.abbandonomaster == false && abbandonotuttiguesser == false){
				System.out.println("La parola scelta e' "+s);
				int t = 0;
				// Creo due array di char. Uno la parola da indovinare uno con gli spazi in corrispodenza delle lettere ancora da indovinare
				char[] arrayparola = s.toCharArray();
				char[] caratteriindovintati = s.toCharArray();
				int numerocaratteriindovinati = 0;
				
				// Stampo
				for(t = 0; t<arrayparola.length;t++){
					caratteriindovintati[t] = '_';
					System.out.print(caratteriindovintati[t]+" ");
				}
				System.out.println(" ");
				
				
				// Appena ho terminato la fase di inizializzazione della partita lo segnalo ai giocatori inviando un messaggio di iniziopartita = true
				System.out.println("La partita puo' cominciare, lo segnalo ai giocatori inviando un inizio");
				i = new Messaggio();
				i.iniziopartita = true;
				try {
					send(socketm,group,port,i.toJson().toJSONString());
				} catch (SocketException ex){
					// La socket e' stata chiusa, probabilmente dal thread  AttesaInput che ha ricevuto un exit.
					// Il gioco per il master termina qua.
					finegioco = true;
				}
				if(finegioco == false){
					// Adesso inizia il gioco. tolgo il timeout dalla socketm, adesso mi svegliero' dalla socketm solo alla ricezione di messaggi
					tentativi = 0;
					indovinata = false;
					socketm.setSoTimeout(0);
					
					timer = new Timer();
					timer.schedule(new Task(), 80*1000);
				}
				
				while(tentativi < tentativitotali && finegioco == false && indovinata == false && (s = receive(socketm))!=null){
					// !!! Se ricevo qualcosa ed esso e' decrittografabile con la pwd allora proseguo
					boolean giusto = true;
					JSONObject r1 = null;
					Messaggio mess = null;
					try {
						r1 = (JSONObject) new JSONParser().parse(s);
						mess = new Messaggio(r1);
					} catch (ParseException ex){
						giusto = false;
					} catch (NullPointerException ex){
						giusto = false;
					}
					if(giusto == true && mess.proposta == true){
						// Adesso devo controllare che la lettera sia nella array arrayparola
						boolean giachiesta = false;
						// Scorro tutto l'array con l'alfabeto e individuo l'indice della lettera, cosi' da segnarla gia' chiesta
						for(int y=0; y<26;y++){
							if(lettere[y]==mess.letteraproposta){
								if(letterachiesta[y] == true){
									giachiesta = true;
								} else {
									letterachiesta[y] = true;
								}
							}
						}
						boolean presente = false;
						Messaggio risposta = null;
						
						for(t = 0; t<arrayparola.length;t++){
							if(arrayparola[t]==mess.letteraproposta){
								caratteriindovintati[t] = mess.letteraproposta;
								if( giachiesta == false) numerocaratteriindovinati++;
								presente = true;
							}
						}
						if(presente){
							System.out.println("LETTERA PRESENTE");
							// La lettera e' stata indovintata, devo dare un punto a tale giocatore.
							// Devo cercare l'utente e incrementare di uno il punteggio
							if(giachiesta == false){
								for( Punti puntig : punteggi){
									if(puntig.nome.equals(mess.nome)){
										puntig.punti = puntig.punti+1;
										System.out.println("La lettera e' presente e non ancora proposta, quindi punto a "+mess.nome+" "+puntig.punti);
									}
								}
							}
							if(numerocaratteriindovinati == arrayparola.length){
								indovinata = true;
								System.out.println("Tentativo n"+tentativi+"  la lettera e' presente. E' gia' stata chiesta?"+giachiesta);
								//public Messaggio(char lett, boolean pres, int trim ,String parolascoperta,boolean ind,boolean fp,boolean to,boolean am){
								risposta = new Messaggio(mess.letteraproposta,true,tentativitotali-tentativi,String.valueOf(caratteriindovintati),true,true,false,false);
							} else {
								System.out.println("Tentativo n"+tentativi+"  la lettera e' presente. E' gia' stata chiesta?"+giachiesta);
								risposta = new Messaggio(mess.letteraproposta,true,tentativitotali-tentativi,String.valueOf(caratteriindovintati),false,false,false,false);
							}
							send(socketm,group,port,risposta.toJson().toString());
						} else {
							// Non e' presente
							System.out.println("LETTERA *NON* PRESENTE");
							if(giachiesta == false){
								// Non e' ancora stata chiesta
								tentativi++;
							}
							int tentativirimanenti = tentativitotali-tentativi;
							System.out.println("Tentativo n"+tentativi+"  la lettera non e' presente E' gia' stata chiesta?"+giachiesta);
							
							if(tentativirimanenti == 0)								
								risposta = new Messaggio(mess.letteraproposta,false,tentativitotali-tentativi,String.valueOf(caratteriindovintati),false,true,false,false);
							else 
								risposta = new Messaggio(mess.letteraproposta,false,tentativitotali-tentativi,String.valueOf(caratteriindovintati),false,false,false,false);
							send(socketm,group,port,risposta.toJson().toString());
						}
						
					} else if(giusto == true && mess.timeout){
						System.out.println("E' scaduto il tempo per giocare");
						finegioco = true;
					} else {
						System.out.println("Non e' il pacchetto che mi aspetto");
					}
	
					
				}
				if(timeoutdigioco == false){
					if(timer!=null) timer.cancel();
				}
				System.out.println("La partita e' terminata!!!!!!!!");
				// Se arrivo qui devo far terminare il thread in attesa su stdIO
				// e comunicare al server che la partita e'
				// finita e che l indirizzo in uso e' libero
				try {
					if(tentativi == tentativitotali) {
						System.out.println("tentativi esauriti.. Vince il master.");
					} else if(timeoutdigioco){
						System.out.println("Fine tempo per giocare. Vince il master.");
						timer.cancel();
					} else if(abbandonomaster){
						System.out.println("Ho abbandonato la partita. Ho perso.");
					} else {
						System.out.println("Ho perso.");
						System.out.println("Parola indovinata. Chi e' il vincitore?");
					}
					
					for( Punti puntig : punteggi){
						MessaggioChiusura rm = null;
						System.out.println(puntig.nome+": "+puntig.punti);
						rm = new MessaggioChiusura(puntig.nome,puntig.punti);
						send(socketm,group,port,rm.toJson().toString());
					}
				
					if(ai.isAlive()){
						ai.sblocco = true;
						System.out.println("Scrivi OK per terminare la partita");
					}
				
					this.socketm.leaveGroup(group);
					this.socketm.close();
					server.close(this.nome);
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (SocketException ex){
					ex.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				
				System.out.println("############### FINE PARTITA ##############");
				
			}	else {
				// Il master ha abbandonato prima ancora di iniziare la partita
				if(ai.isAlive()){
					ai.sblocco = true;
					System.out.println("Scrivi OK per terminare la partita");
				}
				server.close(this.nome);
				this.socketm.leaveGroup(group);
				this.socketm.close();
				System.out.println("############### FINE PARTITA ##############");
			}
		} catch (SocketException e){
			// prima di arrivare qui è probabile che sia stato invocato il metodo endGame dal server
		//System.out.println("La comunicazione e' stata interrotta, potrebbero essere usciti tutti i guesser");
			
		} catch (IOException e){
			e.printStackTrace();
		}
	}
	
	public void endGame(){
		System.out.println("############### FINE PARTITA ##############");
		System.out.println("##### CHIUSURA DAL SERVER: Non ci sono piu' avversari");
		abbandonotuttiguesser = true;
		if(timer!=null)	timer.cancel();
		if(ai.isAlive()){
			ai.sblocco = true;
			System.out.println("Scrivi OK per terminare la partita");
			
		}
		try {
			this.socketm.leaveGroup(group);
			this.socketm.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}