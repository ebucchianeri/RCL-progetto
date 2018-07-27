package impiccato;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import org.jasypt.exceptions.EncryptionOperationNotPossibleException;
import org.jasypt.util.text.BasicTextEncryptor;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Guesser extends Thread {
	String nome;
	String nomem;
	String niface;
	int ngiocatori;
	int port;
	MulticastSocket socketm;
	String indirizzogruppo;
	InetAddress group;
	boolean finepartita;
	int tentativirimanenti;
	boolean timeoutdigioco;
	boolean abbandonomaster;
	boolean abbandonoguesser;
	boolean masternonrisponde;
	boolean proposta;
	char atteso;
	AttesaInput ai;
	String chiaves; // Da inizializzare con cio' che mi invia il server;
	BufferedReader stdIn;
	LockAttesaAck  laa;
	ServerIF server;
	
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
	
	public Guesser(String nomem, String ng, int numg, String ip, String indgroup, String pss, int porta,AttesaInput ai, ServerIF s){
		finepartita = false;
		timeoutdigioco = false;
		abbandonomaster = false;
		abbandonoguesser = false;
		masternonrisponde = false;
		proposta = false;
		this.chiaves = pss;
		this.nomem = nomem;
		this.nome = ng;
		this.ngiocatori = numg;
		this.niface = ip;
		this.indirizzogruppo = indgroup;
		try {
			group = InetAddress.getByName(indirizzogruppo);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.port = porta;
		this.ai = ai;
		this.server = s;
		stdIn = new BufferedReader(new InputStreamReader(System.in));
		laa = new LockAttesaAck();
		
		ai.setParametriPerFaseUDP(this,stdIn,null,this,true,laa,group,port,chiaves);
	}
	
	// Setto la lettera di cui devo attendere l'ack
	public void setProposta(char c){
		proposta = true;
		atteso = c;
	}
	
	public void resetProposta(){
		proposta = false;
	}
	
	public void run(){
		try {
			System.out.println("Sono "+this.nome);
			socketm = new MulticastSocket(port);
			InetAddress iface = InetAddress.getByName(niface);
			socketm.setInterface(iface);
			socketm.joinGroup(group);
			socketm.setSoTimeout(5000);
			
			
			Messaggio i = new Messaggio();
			i.nome = this.nome;
			send(socketm,group,port,i.toJson().toString());
			String s = null;
			
			
			// a questo punto il thread in attesa su stdIn - AttesaLogout - posso farlo terminare, 
			// da qui in avanti dovro' io thread main mettermi in attesa su stdIn per le lettere ipotizzate
			socketm.setSoTimeout(15000);
			boolean sincro = false;
			try {
			while(sincro == false && (s = receive(socketm))!=null){
				boolean giusto = true;
				//System.out.println(s);
				JSONObject r1 = null;
				try {
					r1 = (JSONObject) new JSONParser().parse(s);
				} catch (ParseException ex){
					ex.printStackTrace();
					giusto = false;
				}
				if(giusto == true){
					Messaggio d = new Messaggio(r1);
					if(d.iniziopartita == true){
						sincro = true;
						System.out.println("Si inizia!");
					} else {
						if(d.abbandonomaster){
							System.out.println("Il master ha abbandonato la partita");
							this.abbandonomaster = true;
							sincro = true;
						}
					}
				} else {
					//System.out.println("non e' il messaggio che mi aspetto");
				}
			}
			} catch (SocketTimeoutException ex){
				// Il master non mi ha risposto in tempo, chiudo.
				/// DA CHIUDEREEEE !!!!!!!!!11
				System.out.println("Si chiude, il master non mi ha risposto in tempo!!!!!!!");
				System.out.println("Il master ha abbandonato la partita");
				this.abbandonomaster = true;
			} catch (SocketException ex){
				// Il master non mi ha risposto in tempo, chiudo.
				/// DA CHIUDEREEEE !!!!!!!!!11
				System.out.println("SocketException.");				
			}


			if(abbandonomaster == false && abbandonoguesser == false){
				ai.fasegioco = true;
				ai.partitaincorso = true;
				String data = null;
				System.out.println("la partita può durare al massimo 80 secondi.");
				System.out.println("Scrivere la lettera: ");
	
				
				Messaggio mess = null;
				String str = null;
				try {
					socketm.setSoTimeout(0);
					boolean indovinata = false;
					//ntentativi = 1;
	
						while(finepartita == false && (str = receive(socketm))!=null){
							boolean giusto = true;
							//System.out.println(str);
							JSONObject r1 = null;
							try {
								r1 = (JSONObject) new JSONParser().parse(str);
								mess = new Messaggio(r1);
							} catch (ParseException ex){
								giusto = false;
								System.out.println("parseexception");
							} catch (NullPointerException ex){
								giusto = false;
								
							}
							if(giusto == true){
								if(mess.finepartita == true) {
									// C'e' bisogno di finire la partita, per quale motivo?
									if(mess.indovinata.booleanValue() == true){
										System.out.println("La parola e' stata indovinata.");
										finepartita = true; // Lo setto prima di svegliare il guesser in attesa
										indovinata = true;
										laa.finePartita();
	
									} else if( mess.timeout == true ){
										System.out.println("Fine tempo per giocare. Il master ha vinto.");
										finepartita = true;
										timeoutdigioco = true;
										laa.finePartita();
									} else if( mess.abbandonomaster == true){
										System.out.println("Il master ha abbandonato il gioco.");
										finepartita = true;
										abbandonomaster = true;
										laa.finePartita();
	
									} else {
										if(mess.tentativirimanenti.longValue() == 0){
											System.out.println("I tentativi sono terminati.");
											tentativirimanenti = 0;
											finepartita = true;  // Lo setto prima di svegliare il guesser in attesa
											laa.finePartita();
										}
									}
								} else {
									//System.out.println("#RICEVUTO:" + mess.lettera + mess.presente + proposta);
									tentativirimanenti = (Integer)mess.tentativirimanenti.intValue();
									if(proposta == true){
										if(mess.lettera == atteso){
											System.out.println("La lettera "+mess.lettera+" e' presente? " +mess.presente +". I tentativi rimanenti:"+tentativirimanenti);
											laa.ricevutoAck();
										} 
									}
								}
							} else {
								//System.out.println("Non e' il pacchetto che mi aspetto");
							}
						}
						// Termino correttamente attendendo i punteggi.
						endGuesser();
						
					} catch (SocketTimeoutException ex){
						System.out.println("Devo ri-inviare di nuovo");
						// Il timeout di questa socket non potro' mai averlo perche' ho settato setSoTimeout(0) cioe' infinito
					} catch (SocketException ex){
						if(abbandonoguesser){
							System.out.println("Abbandono della partita");
							this.endGuesserErrato();
						} else if(masternonrisponde){
							System.out.println("Impossibile comunicare col master");
							this.endGuesserErrato();
						} else{
							System.out.println("SocketException");
							this.endGuesserErrato();
						}
					} catch (IOException ex){
						System.out.println("eccezione");
					} catch (InterruptedException ex){
						System.out.println("eccezione sul semaforo?");
					}
	
			} else if (abbandonomaster){
				System.out.println("Il master ha abbandonato prima dell'inizio.");
				// Il master ha abbandonato prima ancora di iniziare, quindi faccio una chiusura semplice
				server.leave(nomem, nome);
				socketm.leaveGroup(group);
				socketm.close();
				this.endGuesserErrato();
			} else if (abbandonoguesser){
				// Il master ha abbandonato prima ancora di iniziare, quindi faccio una chiusura semplice
				System.out.println("Ho abbandonato io prima dell inizio della partita.");
				this.endGuesserErrato();
			}
		} catch (IOException e){
			e.printStackTrace();
		}
	}
	
	
	private void endGuesserErrato(){
		// La socket e' gia' stata chiusa dal thread di AttesaInput al fine di uscire in ogni caso dal while di ricezione
		// dalla socket. Devo Segnalare la fine partita e comunicare al server il mio abbandono.
		// Devo inoltre, dopo, cambiare nuovamente il mio stato
			System.out.println("------------- FINE PARTITA ------------");
			if(ai.isAlive()){
				ai.sblocco = true;
				System.out.println("Scrivi OK per terminare la partita");
			}
			//server.leave(param.getNome(), masterrichiesto);
	}
	
	
	private void endGuesser(){
		String str = null;
		int con = 0;
		try {
			socketm.setSoTimeout(2000);
			MessaggioChiusura msg = null;
			while(con < ngiocatori - 1 ){
				System.out.println("Attendo i risultati");
				try {
					if((str = receive(socketm))!=null){
						boolean giusto = true;
						// Attendo il messaggio coi punteggi
						JSONObject r1 = null;
						try {
							r1 = (JSONObject) new JSONParser().parse(str);
							msg = new MessaggioChiusura(r1);
						} catch (ParseException ex){
							giusto = false;
							System.out.println("parseexception");
						} catch (NullPointerException ex){
							giusto = false;
							
						}
						if(giusto == true){
							System.out.println(msg.toString());
						}
					}
					con++;
					
				} catch (SocketTimeoutException ex){}
			}
			
			System.out.println("------------- FINE PARTITA ------------");
			if(ai.isAlive()){
				ai.sblocco = true;
				System.out.println("Scrivi OK per terminare la partita");
			}
			
			
			//server.leave(param.getNome(), masterrichiesto);
			this.socketm.leaveGroup(group);
			this.socketm.close();
		}  catch (SocketException ex){
			System.out.println("eccezione");
		} catch (IOException ex){
			System.out.println("eccezione");
		}
	}
}
