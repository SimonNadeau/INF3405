import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.Path;
import java.util.regex.Pattern;
import java.lang.String;

public class Client {

    private BufferedReader in;
    private PrintWriter out;

    public Client() {

    }
    
    // Log. For future use
    private void log(String message) {
        System.out.println(message);
    }

    // Pattern for the IP address. 
    // Source: https://stackoverflow.com/questions/5667371/validate-ipv4-address-in-java
	private final Pattern PATTERN = Pattern.compile(
			"^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");
	
	// Validation of the IP that matches the pattern of an IP.
	public boolean validateIp(final String ip) {
		return PATTERN.matcher(ip).matches(); 
	}
	
	// Validation of a Port that make sures that it is between 5000 and 5500.
	public boolean validatePort(final int port) {
		if (port >= 5000 && port <= 5500){
			return true;
		}
		else {
			return false;
		}
	}
	
	// Initialize Ip for connection in connectToServer()
    public String initializeIp(){
        log("Enter IP Address of the Client:");
  	    String serverAddress = System.console().readLine();
  	    
  	    // If IP address entered is wrong.
        while (!validateIp(serverAddress)){
            log("Wrong IP Address. Enter another one:");
        	serverAddress = System.console().readLine();
        }
        return serverAddress;
    }
    
	// Initialize Port for connection in connectToServer()
    public int initializePort(){
        log("Enter Port for the Client :");
        int port = Integer.parseInt(System.console().readLine());
        
        // If Port entered is wrong.
        while (!validatePort(port)){
            log("Wrong Port. Should be between 5000 and 5500. Enter another one:");
            port = Integer.parseInt(System.console().readLine());
        }
        return port;
    }
    
    // Return true if it is one of the command. If not, then return the helps.
    private boolean isCommandValid(String command) {
    	
    	return (command.equals("ls") || command.equals("exit") ||
    			firstWordFromCommand(command).equals("cd") ||
    			firstWordFromCommand(command).equals("mkdir") ||
    			firstWordFromCommand(command).equals("upload") ||
    			firstWordFromCommand(command).equals("download"));
    }
    
    // If wrong command is entered
    private void logHelp() {
    	log("*** Help ***");
        log("     ls");
        log("     cd <Repertoire>");
        log("     mkdir <Nom du Dossier>");
        log("     upload <Nom du Fichier>");
        log("     download <Nom du Fichier>");
        log("     exit");
    }
    
    // Takes the first part of the command that is entered. Mostly used for the command line
    private String firstWordFromCommand(String command){
    	String firstWord = "";
        if (command.contains(" ")){
        	firstWord= command.substring(0, command.indexOf(" "));
        }
        else {
        	firstWord = command;
        }
        return firstWord;
    }
    
    // Takes the first part of the command that is entered. Mostly used for the command line
    private String secondWordFromCommand(String command){
    	String secondWord = "";
        if (command.contains(" ")){
        	secondWord = command.substring(command.indexOf(" ") + 1, command.length());
        }
        return secondWord;
    }
    
    private boolean uploadFile(Socket sock, String fileName) throws IOException {
    	File file = new File(fileName);
    	// log("UPLOAD " + file.getAbsolutePath());
    	if (!(file.isFile())){
    		log("Ce fichier n'existe pas.");
    		return false;
    	}
    	else {    		
    		DataOutputStream dos = new DataOutputStream(sock.getOutputStream());
    		FileInputStream fis = new FileInputStream(file.toString());
    		byte[] buffer = new byte[4096];
    		int read;
    		dos.writeLong(file.length());
    		while ((read=fis.read(buffer)) > 0) {
    			dos.write(buffer, 0, read);
    		}
    		fis.close();
    		return true;
    	}
    	
    }
    
    private void downloadFile(Socket sock, String fileName) throws IOException {
    	
		DataInputStream dis = new DataInputStream(sock.getInputStream());
		FileOutputStream fos = new FileOutputStream(fileName);
		byte[] buffer = new byte[4096];
		long fileSize = dis.readLong();
		int read = 0;
		while(fileSize > 0 && (read = dis.read(buffer)) > 0) {
			fos.write(buffer, 0, read);
			fileSize -= read;
		}
		fos.close();
    }
    
//    @SuppressWarnings("resource")
	public void connectToServer() throws IOException {
        
//    	String serverAddress = initializeIp();
//        int port = initializePort();
    	String serverAddress = "127.0.0.1";
    	int port = 5000;
        Socket socket = new Socket(serverAddress, port);
		
        System.out.format("The server is running on %s:%d%n", serverAddress, port);
        
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        log(in.readLine() + "\n");
        Path actualPath = new File(".").toPath().toAbsolutePath();
        out.println(actualPath);
        out.println(serverAddress + " " + port);
        String command = "";
        while (!command.equals("exit")) {

	        log("\nEnter Command");
	        command = System.console().readLine();
	        while (!isCommandValid(command)){
	            log("Invalid Command");
	            logHelp();
	        	command = System.console().readLine();
	        }
	        out.println(command);
	        
	        // Envoie d'un fichier
	        if (firstWordFromCommand(command).equals("upload")){
	        	uploadFile(socket, secondWordFromCommand(command));
	        }
	        if (firstWordFromCommand(command).equals("download")){
	        	downloadFile(socket, secondWordFromCommand(command));
	        }
	        
	        String response = "";
	        try {
	        	do {
	        		log(response);
	        		response = in.readLine();
	        	} while (!response.equals("done"));

	        } catch (IOException ex) {
	        	response = "Error: " + ex;
	        }
    	}
        if (command.equals("exit")){
        	log("Vous avez ete deconnecte avec succes.");
        }
    }

    public static void main(String[] args) throws Exception {
        Client client = new Client();
        client.connectToServer();
    }
}