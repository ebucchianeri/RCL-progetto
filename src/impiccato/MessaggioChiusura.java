package impiccato;

import org.json.simple.JSONObject;

public class MessaggioChiusura {
		String nome;
		Number punti;
		
		public MessaggioChiusura(String n, int p){
			nome = n;
			punti = p;
		}
		
		
		public MessaggioChiusura (JSONObject j) {
			nome = (String) j.get("nome");
			punti = (Number) j.get("punti");
		}
		
		/* Costruisce il JSON dato l'oggetto */
		@SuppressWarnings("unchecked")
		public JSONObject toJson() {
			JSONObject scatola = new JSONObject();
			scatola.put("nome", nome);
			scatola.put("punti", punti);
			return scatola;
		}


		@Override
		public String toString() {
			return "MessaggioChiusura [nome=" + nome + ", punti=" + punti + "]";
		}

		
		
		
}
