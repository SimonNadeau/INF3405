import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.regex.Pattern;
import java.lang.String;

public class Client {

    private BufferedReader in;
    private PrintWriter out;
    private String messageArea = new String();

    public Client() {

    }

	private final Pattern PATTERN = Pattern.compile(
			"^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$"); // Pattern pour l'adresse IP
	
	public boolean validateIp(final String ip) {
		return PATTERN.matcher(ip).matches();
	}
	
	public boolean validatePort(final int port) { // Pattern pour le port
		if (port >= 5000 && port <= 5500){
			return true;
		}
		else {
			return false;
		}
	}
	
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
    
    private boolean isCommandValid(String command) {
    	
    	return (command.equals("ls") || command.equals("exit") ||
    			firstWordFromCommand(command).equals("cd") ||
    			firstWordFromCommand(command).equals("mkdir") ||
    			firstWordFromCommand(command).equals("upload") ||
    			firstWordFromCommand(command).equals("download"));
    }
    
    private String firstWordFromCommand(String command){
    	String firstWord = command;
        if (firstWord.contains(" ")){
        	firstWord= firstWord.substring(0, firstWord.indexOf(" "));
        }
        return firstWord;
    }
	
    private void log(String message) {
        System.out.println(message);
    }
    
    @SuppressWarnings("resource")
	public void connectToServer() throws IOException {
        
    	String serverAddress = initializeIp();
        int port = initializePort();
        Socket socket = new Socket(serverAddress, port);
		
        System.out.format("The capitalization server is running on %s:%d%n", serverAddress, port);
        
        in = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        // Consume the initial welcoming messages from the server
        for (int i = 0; i < 3; i++) {
            messageArea += in.readLine() + "\n";
        }
        log(messageArea);
        
        String command = "";
        while (!isCommandValid(command)){
        	log("Enter Command");
        	command = System.console().readLine();
        }
        out.println(command);
        
        
        
        String response;
        try {
            response = in.readLine();
            if (response == null || response.equals("")) {
                  System.exit(0);
              }
        } catch (IOException ex) {
               response = "Error: " + ex;
        }
        log(response);
        
    }

    public static void main(String[] args) throws Exception {
        Client client = new Client();
        client.connectToServer();
    }
}