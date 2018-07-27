package impiccato;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.jasypt.contrib.org.apache.commons.codec_1_3.binary.Base64;
import org.jasypt.util.text.BasicTextEncryptor;

public class AttesaInput extends Thread{
	boolean sblocco; // Variabile per far terminare il ciclo sullo standard input
	
	//Fase TCP:
	ServerIF s;
	Parametri param;
	ClientInizio client;
	
	LockAttesaAck laa;
	ReentrantLock lock;
	Condition stop; 
	
	BufferedReader stdi;
	boolean fasegioco; // Variabile che indica se siamo nella fase di gioco multicast
	boolean iniziofaseUDP;
	String ricevuta = null;
	int stato;
	Master m;
	boolean guesser;
	Guesser g;
	//Client c;
	
	String chiave;
	int port;
	InetAddress ia;
	int prova;
	boolean partitaincorso;
	int numerotimeoutconsecutivi;
		
	public void setParametriPerFaseUDP(Guesser g, BufferedReader stdIn, Master mm, Guesser gg, boolean gf, LockAttesaAck llock, InetAddress inda, int pp, String chiav){
		lock = new ReentrantLock();
		stop = lock.newCondition();  
		this.stdi = stdIn;
		this.g = g;
		sblocco = false; fasegioco = false; iniziofaseUDP = true;
		m = mm;
		g = gg;
		guesser = gf;
		laa = llock;
		ia = inda;
		port = pp;
		numerotimeoutconsecutivi = 0;
		chiave = chiav;
	}
	
	public AttesaInput(ClientInizio c,Parametri p,BufferedReader stdIn, int s, ServerIF server){
		param = p;
		client = c;
		this.stdi = stdIn;
		stato = s;
		fasegioco = false;
		iniziofaseUDP = false;
		this.s = server;
	}
	
	private String encrypt(String s){
		BasicTextEncryptor textEncryptor = new BasicTextEncryptor();
		textEncryptor.setPassword(chiave);
		String risultato = textEncryptor.encrypt(s);
		return risultato;
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
	
	private void indovinaParola(String userInput) throws IOException{
		System.out.println(userInput);
		if(userInput.equals("exit")) {
			sblocco = true; // Voglio terminare questo thread
			partitaincorso = false;
			g.abbandonoguesser = true;
			// Quando arrivo qui, potrei essere in attesa sulla socket, quindi per terminare il guesser la chiudo.
			// Avro' una socketException che gestisco grazie alla variabile MASTERNONRISPONDE
			s.leave(g.nomem, g.nome);
			g.socketm.leaveGroup(g.group);
			g.socketm.close();
			System.err.println("Ricevuto abbandono");
		} else {
			char[] caratteri = userInput.toCharArray();
			if(caratteri.length == 1){
				System.out.println("Si propone il carattere "+caratteri[0]);
				Messaggio mess = new Messaggio();
				mess.proposta = true; mess.nome = g.nome; mess.letteraproposta = caratteri[0];
				boolean ricevuto = false;
				prova = 0;
				while(ricevuto == false && partitaincorso && prova < 2){
					System.out.println("Invio la mia proposta");
					// Il set della proposta devo farlo prima dell'invio del messaggo!!!
					g.setProposta(caratteri[0]);
					send(g.socketm,ia,port,mess.toJson().toString());
					
					// adesso devo mettermi in attesa dell'ack
					try {
					laa.lock.lock();
					laa.w();
					laa.lock.unlock();
					} catch (InterruptedException ex){
						ex.printStackTrace();
					}
					
					// Quando mi risveglio controllo se mi sono svegliata per il timer o altro.
					if( laa.ricevutoack == true ){
						// posso proseguire
						ricevuto = true;
						numerotimeoutconsecutivi = 0;
						g.resetProposta();
					} else if( laa.timer == true ){
						// ho ricevuto un timout - devo riinviare
						prova++;
						numerotimeoutconsecutivi++;
						System.out.println("Ricevuto timeout devo riinviare  "+prova+" "+numerotimeoutconsecutivi);
					} else if( laa.chiusurapartita == true){
						//System.out.println("No ack, no timer!");
						// Sono stato svegliato dal semaforo ed entrambe sono false. 
						// Allora era la signal che faccio alla fine dei tentativi o in altri casi gestiti dal thread main
						ricevuto = true;
						sblocco = true;
					}

					laa.resetLockAttesaAck();
					if(numerotimeoutconsecutivi > 4){
						// ------------------------ DEVO FARE ABBANDONO DELLA PARTITA!!!!!!!!!!!!!!!!!!!!
						System.out.println("Timeout consecutivi > 4");
						sblocco = true; // Voglio terminare questo thread
						partitaincorso = false;
						s.leave(g.nomem, g.nome);
						g.masternonrisponde = true;
						// Quando arrivo qui, potrei essere in attesa sulla socket, quindi per terminare il guesser la chiudo.
						// Avro' una socketException che gestisco grazie alla variabile MASTERNONRISPONDE
						g.socketm.leaveGroup(g.group);
						g.socketm.close();
					}
				}
				if(partitaincorso) {
					if(prova == 0)	System.out.println("Inserisci una lettera, per abbandonare scrivere 'exit' ");
					else System.out.println("Richiesta persa, inserisci nuovamente una lettera o scrivi 'exit' per lasciare il gioco");
				} else System.out.println("Il gioco sta per terminare");
			}
		}
	}
	
	public void run(){
		String userInput = null;
		try {
			while(sblocco == false){	
				
				System.out.println("Mi metto in attesa sullo stdin");
				if((userInput = stdi.readLine())!=null){
					
					if(stato == 2){
						// Se sono un GUESSER e sono in fase di gioco devo mettermi in attesa sullo STDIn e attendere lettere o exit
						if(fasegioco == false){
							if(iniziofaseUDP==false){
								// Sono nella fase TCP
								if(userInput.equals("exit")) {
									System.out.println("FaseTCP: logout richiesto!");
									sblocco = true; // Voglio terminare questo thread
									s.abbandonaPartita(param.getNome());
									client.abbandono = true;
									//client.fine = true;
									System.out.println("Chiudo la connessione TCP col server");
									try {
										client.Socket.close();
										client.in.close();
										client.out.close();
									} catch (IOException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
								}
								
							} else {
								// Sono all'inizio della fase UDP
								if(userInput.equals("exit")) {
									
									sblocco = true; // Voglio terminare questo thread
									partitaincorso = false;
									g.abbandonoguesser = true;
									// Quando arrivo qui, potrei essere in attesa sulla socket, quindi per terminare il guesser la chiudo.
									// Avro' una socketException che gestisco grazie alla variabile ABBANDONOGUESSER
									s.leave(g.nomem, g.nome);
									g.socketm.leaveGroup(g.group);
									g.socketm.close();
									System.err.println("Ricevuto abbandono");
								}
							}
						} else {
							indovinaParola(userInput);
						}
					} else if (stato == 1){
						// Se sono un MASTER invece devo semplicemente mettermi in attesa di un exit da partita
						if(fasegioco == false){
							if(iniziofaseUDP==false){
								// Sono nella fase TCP
								if(userInput.equals("exit")) {
									System.out.println("FaseTCP: logout richiesto!");
									sblocco = true; // Voglio terminare questo thread
									s.abbandonaPartita(param.getNome());
									client.abbandono = true;
									//client.fine = true;
									System.out.println("Chiudo la connessione TCP col server");
									try {
										client.Socket.close();
										client.in.close();
										client.out.close();
									} catch (IOException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
								}
								
							} else {
								// Sono all'inizio della fase UDP, ma il master non ha una fase iniziale UDP dove puo' scrivere,
								// per lui il gioco parte subito. Comunque gestisco un possibile exit
								if(userInput.equals("exit")) {
									ricevuta = userInput;
									m.finegioco = true; // Cosi esco dal while.
									m.abbandonomaster = true;
									System.err.println("Ricevuto abbandono");
									// INVIO IO UN MESSAGGIO DI CHIUSURA X ABBANDONO MaSTER
									//public Messaggio(char lett, boolean pres, int trim ,String parolascoperta,boolean ind,boolean fp,boolean to,boolean am){
									
									System.err.println("Ricevuto abbandono");
									sblocco = true;
									s.close(m.nome);
								}
								
							}
						} else {
							System.out.println(userInput);
							if(userInput.equals("exit")) {
								ricevuta = userInput;
								m.finegioco = true; // Cosi esco dal while.
								m.abbandonomaster = true;
								System.err.println("Ricevuto abbandono");
								// INVIO IO UN MESSAGGIO DI CHIUSURA X ABBANDONO MaSTER
								//public Messaggio(char lett, boolean pres, int trim ,String parolascoperta,boolean ind,boolean fp,boolean to,boolean am){
								
								Messaggio msg = new Messaggio('-',false,0,"",false,true,false,true);
								send(m.socketm,ia,port,msg.toJson().toString());
								System.err.println("Ricevuto abbandono");
								sblocco = true;
								s.close(m.nome);
								ricevuta = userInput;
								lock.lock();
								stop.signalAll();
								lock.unlock();
							}
							else {
								ricevuta = userInput;
								lock.lock();
								stop.signalAll();
								lock.unlock();
							}
						}
					}
				}
			}
		} catch (IOException ex){
			System.err.println("eccezioneeee");
		} 
		System.out.println("Il THREAD AttesaInput termina");
	}
}
