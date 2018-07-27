package impiccato;

import org.json.simple.JSONObject;

public class Messaggio {
	String nomeguesser;
	String nome;
	Boolean iniziopartita;
	String parola;
	Boolean proposta;
	char lettera;
	char letteraproposta;
	Boolean presente;
	Number tentativirimanenti;
	Boolean indovinata;
	Boolean finepartita;
	Boolean timeout;
	Boolean abbandonomaster;
	
	public Messaggio(){
		iniziopartita = false;
		nome = null;
		parola = null;
		proposta = false;
		lettera = '-';
		letteraproposta = '-';
		presente = false;
		tentativirimanenti = 0;
		indovinata =  false;
		finepartita =  false;
		timeout =  false;
		abbandonomaster = false;
	}
	
	public Messaggio(char lett, boolean pres, int trim ,String parolascoperta,boolean ind,boolean fp,boolean to,boolean am){
		nome = null; iniziopartita = false;
		parola = parolascoperta;
		proposta = false;
		lettera = lett; 
		letteraproposta = '-';
		presente = pres;
		tentativirimanenti = trim;
		indovinata = ind;
		finepartita = fp;
		timeout = to;
		abbandonomaster = am;
		
	}
	
	public Messaggio (JSONObject j) {
		String s = null;
		iniziopartita = (Boolean) j.get("iniziopartita");
		nome = (String) j.get("nome");

		parola = (String) j.get("parola");
		proposta = (Boolean) j.get("proposta");
		s = (String) j.get("lettera");
		lettera = s.charAt(0);
		s = (String) j.get("letteraproposta");
		letteraproposta = s.charAt(0);
		presente = (Boolean) j.get("presente");
		tentativirimanenti = (Number) j.get("tentativirimanenti");
		indovinata = (Boolean) j.get("indovinata");
		finepartita = (Boolean) j.get("finepartita");
		timeout = (Boolean) j.get("timeout");
		abbandonomaster = (Boolean) j.get("abbandonomaster");
	}
	
	/* Costruisce il JSON dato l'oggetto */
	@SuppressWarnings("unchecked")
	public JSONObject toJson() {
		JSONObject scatola = new JSONObject();
		scatola.put("iniziopartita", iniziopartita);
		scatola.put("nome", nome);
		scatola.put("parola", parola);
		scatola.put("proposta", proposta);
		scatola.put("lettera", String.valueOf(lettera));
		scatola.put("letteraproposta", String.valueOf(letteraproposta));
		scatola.put("presente", presente);
		scatola.put("tentativirimanenti", tentativirimanenti);
		scatola.put("indovinata", indovinata);
		scatola.put("finepartita", finepartita);
		scatola.put("timeout", timeout);
		scatola.put("abbandonomaster", abbandonomaster);
		return scatola;
	}	
	
}
