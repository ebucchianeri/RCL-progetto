package impiccato;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;


class ParametriServer {
	private String ip;
	private String firsti;
	private String lasti;
	private Number portTCP;
	private Number portUDP;

	
	/* Costruttore: data una stringa csv costruisce l'oggetto */
	public ParametriServer (String ipp,String f,String l, int p, int pu) {
		this.ip = ipp;
		this.firsti = f;
		this.lasti = l;
		this.portTCP = p;
		this.portUDP = pu;

	}
	/* Costruttore: dato il JSON costruisce l'oggetto */
	public ParametriServer (JSONObject j) {
		ip = (String)  j.get("ip");
		firsti = (String)  j.get("firsti");
		lasti = (String)  j.get("lasti");
		portTCP = (Number)  j.get("portTCP");
		portUDP = (Number)  j.get("portUDP");
	}
	
	/* Costruisce il JSON dato l'oggetto */
	@SuppressWarnings("unchecked")
	public JSONObject toJson() {
		JSONObject scatola = new JSONObject();
		scatola.put("ip", ip);
		scatola.put("firsti", firsti);
		scatola.put("lasti", lasti);
		scatola.put("portTCP", portTCP);
		scatola.put("portUDP", portUDP);
		System.out.println(scatola);
		return scatola;
	}
	


	
	@Override
	public String toString() {
		return "ParametriServer [ip=" + ip + ", firsti=" + firsti + ", lasti="
				+ lasti + ", portTCP=" + portTCP + ", portUDP=" + portUDP + "]";
	}
	public String getIp(){
		return ip;
	}
	
	public String getLastI(){
		return this.lasti;
	}
	
	public String getFirstI(){
		return this.firsti;
	}
	
	public int getPortTCP(){
		return this.portTCP.intValue();
	}
	
	public int getPortUDP(){
		return this.portUDP.intValue();
	}
	
	public static void main(String[] args) {
		PrintWriter outputStream = null;
		BufferedReader inputStream = null;
		
		try {
			outputStream = new PrintWriter(new FileWriter("configServer.txt"));
			
			ParametriServer p = new ParametriServer("127.0.0.1","230.0.0.0","230.0.0.9",50555,56666);


			System.out.println(p.toJson());
            
            outputStream.println(p.toJson());
            outputStream.close();
            
            String l=null;
           p = null;
            inputStream = new BufferedReader(new FileReader("configServer.txt"));
			JSONObject dmj = null;
            while((l = inputStream.readLine() ) != null){
            	dmj = (JSONObject) new JSONParser().parse(l);
            	p = new ParametriServer(dmj);
            	 System.out.println(dmj.get("ip"));
			}
            
            System.out.println("estraggo\n"+p.toString());
             
        } catch (Exception ex){
        	ex.printStackTrace();
		}finally {
        	 if (inputStream != null) {
        		 try{
                 inputStream.close();
        		 } catch (Exception ex){
        	        	ex.printStackTrace();
        			}
             }
        	 
        }
	}
}



class Parametri {
	private String ips;
	private String ipc;
	public String nome;
	public String pwd;
	private Number serverPortTCP;

	
	/* Costruttore: data una stringa csv costruisce l'oggetto */
	public Parametri (String ips,int port,String ipc) {
		this.ips = ips;
		this.ipc = ipc;
		//this.nome = n;
		//this.pwd = p;
		this.serverPortTCP = port;
	}
	/* Costruttore: dato il JSON costruisce l'oggetto */
	public Parametri (JSONObject j) {
		ips = (String)  j.get("ips");
		ipc = (String)  j.get("ipc");
		//nome = (String)  j.get("nome");
		//pwd = (String) j.get("pwd");
		serverPortTCP = (Number) j.get("serverPortTCP");
	}
	
	/* Costruisce il JSON dato l'oggetto */
	@SuppressWarnings("unchecked")
	public JSONObject toJson() {
		JSONObject scatola = new JSONObject();
		scatola.put("ips", ips);
		scatola.put("ipc", ipc);
		//scatola.put("nome", nome);
		//scatola.put("pwd", pwd);
		scatola.put("serverPortTCP", serverPortTCP);
		System.out.println(scatola);
		return scatola;
	}
	

	public String getIps(){
		return ips;
	}
	
	public String getIpc(){
		return ipc;
	}
	
	public String getNome(){
		return nome;
	}
	
	public String getPwd(){
		return pwd;
	}
	
	
	@Override
	public String toString() {
		return "Parametri [ips=" + ips + ", ipc=" + ipc + ", serverPortTCP=" + serverPortTCP + "]";
	}
	public int getPortServerTCP(){
		return this.serverPortTCP.intValue();
	}
	
	public static void main(String[] args) {
		PrintWriter outputStream = null;
		BufferedReader inputStream = null;
		String nomefile = "config3.txt";
		try {
			outputStream = new PrintWriter(new FileWriter(nomefile));
			
			Parametri p = new Parametri("127.0.0.1",50555,"127.0.0.1");


			System.out.println(p.toJson());
            
            outputStream.println(p.toJson());
            outputStream.close();
            
            String l=null;
           p = null;
            inputStream = new BufferedReader(new FileReader(nomefile));
			JSONObject dmj = null;
            while((l = inputStream.readLine() ) != null){
            	dmj = (JSONObject) new JSONParser().parse(l);
            	p = new Parametri(dmj);
            	 System.out.println(dmj.get("ips"));
			}
            
            System.out.println("estraggo\n"+p.toString());
             
        } catch (Exception ex){
        	ex.printStackTrace();
		}finally {
        	 if (inputStream != null) {
        		 try{
                 inputStream.close();
        		 } catch (Exception ex){
        	        	ex.printStackTrace();
        			}
             }
        	 
        }
	}

}
