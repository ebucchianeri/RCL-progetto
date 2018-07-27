package impiccato;

import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONObject;

public class MessaggioTCP {
	Number valore; // 1 messaggio di richiesta, 2 msg di risposta 4 di segnalazione 3 di inizio
	String nome;
	Number stato;
	Number k;
	String master;
	Boolean partitacreata;
	String errore;
	Boolean iniziata;
	String pwd;
	String indirizzo;
	Number porta; // La porta dove fare multicast del gruppo di giocatori
	Boolean abbandonomaster;
	Boolean timeout;
	List<String> giocatori;
	
	// Richiesta di diventare guessed
	public MessaggioTCP(String n, int s, String m){
		valore = 1; nome = n; stato = s; master = m;
		k = 0; partitacreata = false; errore = ""; iniziata = false; pwd = ""; indirizzo = ""; porta = 0;
		abbandonomaster = false; timeout = false; giocatori = null;
	}
	
	// Richiesta di diventare master
	public MessaggioTCP(String n, int s, int l){
		valore = 1; nome = n; stato = s; k = l;
		master = ""; partitacreata = false; errore = ""; iniziata = false; pwd = ""; indirizzo = ""; porta = 0;
		abbandonomaster = false; timeout = false; giocatori = null;
	}
	
	// Messaggi di risposta alle richieste, possono essere di conferma o di errore.
	public MessaggioTCP(boolean b, String s){
		valore = 2; partitacreata = b; errore = s;
		nome = ""; stato = 0; k = 0; master = ""; iniziata = false; pwd = ""; indirizzo = ""; porta = 0;
		abbandonomaster = false; timeout = false; giocatori = null;
	}
	
	// Messaggio di segnalazione errore 
	public MessaggioTCP(boolean a, boolean t){
		valore = 4; abbandonomaster = a; timeout = t; errore = "";
		partitacreata = false;
		nome = ""; stato = 0; k = 0; master = ""; iniziata = false; pwd = ""; indirizzo = ""; porta = 0; giocatori = null;
	}
	
	// Messaggio di inizio partita per il guesser
	public MessaggioTCP(boolean b, String pas, String ind, int p, int gio){
		valore = 3; iniziata = b; pwd = pas; indirizzo = ind; porta = p; k = gio;
		partitacreata = false; errore = ""; nome = ""; stato = 0;  master = "";
		abbandonomaster = false; timeout = false; giocatori = null;
	}
	
	// Messaggio di inizio partita per il master
	public MessaggioTCP(boolean b, String pas, String ind, int p, ArrayList<Giocatore> guesser){
		valore = 3; iniziata = b; pwd = pas; indirizzo = ind; porta = p;
		partitacreata = false; errore = ""; nome = ""; stato = 0; k = 0; master = "";
		abbandonomaster = false; timeout = false; 
		giocatori = new ArrayList<String>();
		for( Giocatore g : guesser){
			giocatori.add(g.name);
		}
	}
	
	/* Costruttore: dato il JSON costruisce l'oggetto */
	public MessaggioTCP (JSONObject j) {
		valore = (Number) j.get("valore");
		nome = (String) j.get("nome");
		stato = (Number)j.get("stato");
		k = (Number) j.get("numerogiocatori");
		master = (String) j.get("master");
		partitacreata = (Boolean) j.get("partitacreata");
		errore = (String) j.get("errore");
		iniziata = (Boolean) j.get("iniziata");
		pwd = (String) j.get("pwd");
		indirizzo = (String) j.get("indirizzo");
		porta = (Number)j.get("porta");
		abbandonomaster = (Boolean) j.get("abbandonomaster");
		timeout = (Boolean) j.get("timeout");
		giocatori = (List<String>) j.get("giocatori");
	}
	
	/* Costruisce il JSON dato l'oggetto */
	@SuppressWarnings("unchecked")
	public JSONObject toJson() {
		JSONObject scatola = new JSONObject();
		scatola.put("valore", valore);
		scatola.put("nome", nome);
		scatola.put("stato", stato);
		scatola.put("numerogiocatori", k);
		scatola.put("master", master);
		scatola.put("partitacreata", partitacreata);
		scatola.put("errore", errore);
		scatola.put("iniziata", iniziata);
		scatola.put("pwd", pwd);
		scatola.put("indirizzo", indirizzo);
		scatola.put("porta", porta);
		scatola.put("abbandonomaster", abbandonomaster);
		scatola.put("timeout", timeout);
		scatola.put("giocatori", giocatori);
		return scatola;
	}

	@Override
	public String toString() {
		return "MessaggioTCP [valore=" + valore + ", nome=" + nome + ", stato="
				+ stato + ", k=" + k + ", master=" + master
				+ ", partitacreata=" + partitacreata + ", errore=" + errore
				+ ", iniziata=" + iniziata + ", pwd=" + pwd + ", indirizzo="
				+ indirizzo + ", porta=" + porta + ", abbandonomaster="
				+ abbandonomaster + ", timeout=" + timeout + "]";
	}
}
