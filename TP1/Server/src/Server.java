import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Pattern;

public class Server {

    // Pattern for the IP address. 
    // Source: https://stackoverflow.com/questions/5667371/validate-ipv4-address-in-java
	private static final Pattern PATTERN = Pattern.compile(
			"^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");

	// Validation of the IP that matches the pattern of an IP.
	public static boolean validateIp(final String ip) {
		return PATTERN.matcher(ip).matches();
	}
	
	// Validation of a Port that make sures that it is between 5000 and 5500.
	public static boolean validatePort(final int port) {
		if (port >= 5000 && port <= 5500){
			return true;
		}
		else {
			return false;
		}
	}

    // Log. For future use
    private static void log(String message) {
        System.out.println(message);
    }

    
    public static void main(String[] args) throws Exception {
    	
    	int clientNumber = 1;
      
        // Enter IP address 
        Server.log("Enter IP Address of the Server:");
  	    String serverAddress = System.console().readLine();
  	    
  	    // If IP address entered is wrong.
        while (!Server.validateIp(serverAddress)){
            Server.log("Wrong IP Address. Enter another one:");
        	serverAddress = System.console().readLine();
        }
        
        // Enter Port
        Server.log("Enter Port for the server :");
        int port = Integer.parseInt(System.console().readLine());
        
        // If Port entered is wrong.
        while (!Server.validatePort(port)){
            Server.log("Wrong Port. Should be between 5000 and 5500. Enter another one:");
            port = Integer.parseInt(System.console().readLine());
        }
        
		ServerSocket listener;
		InetAddress locIP = InetAddress.getByName(serverAddress);
		listener = new ServerSocket();
		listener.setReuseAddress(true);
		listener.bind(new InetSocketAddress(locIP, port));
		
        System.out.format("The server is running on %s:%d%n", serverAddress, port);
    
        try {
            while (true) {
                new Manager(listener.accept(), clientNumber++).start();
            }
        } finally {
            listener.close();
        }
        
    }

    private static class Manager extends Thread {
        private Socket socket;
        private int clientNumber;
        private Path actualPath;
        private String IpAdressClient;
        private String portClient;
        private PrintWriter out;
        private BufferedReader in;

        public Manager(Socket socket, int clientNumber) {
            this.socket = socket;
            this.clientNumber = clientNumber;
            log("New connection with client# " + clientNumber);
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
        
        // Takes the second part of the command that is entered. Mostly used for the command line
        private String secondWordFromCommand(String command){
        	String secondWord = "";
            if (command.contains(" ")){
            	secondWord = command.substring(command.indexOf(" ") + 1, command.length());
            }
            return secondWord;
        }
        
        // Menu
        private void processCommand(String command) {
        	
        	switch(firstWordFromCommand(command))
        	{
        	case "ls" :
        		processLs();
        		break; 
        		
        	case "mkdir":
        		processMkdir(secondWordFromCommand(command));
			    break; 
			    
        	case "cd":
        		processCd(secondWordFromCommand(command));
			    break;
			    
        	case "upload":
    			try {
    				saveFile(secondWordFromCommand(command));
    			} catch (IOException e) {
    				e.printStackTrace();
    			}
			    break;
        	case "download":
    			try {
    				if (isFileExist(secondWordFromCommand(command))){    					
    					sendFile(secondWordFromCommand(command));
    				}
    			} catch (IOException e) {
    				e.printStackTrace();
    			}
			    break;
        	case "exit":
        		break;
        	   
    	    default:
    	    	break;
        	}
        }
        
        // Change directory
        private void processCd(String secondArgument){
        	
        	// Desired Path
        	Path desiredPath = actualPath.subpath(0, actualPath.getNameCount()-1);
        	
        	// If there is more than just one "/" in the second argument. 
        	Path path = Paths.get(secondArgument);
        	for (int i = 0; i < path.getNameCount(); i++){
        		
        		String subpath = path.subpath(i, i + 1).toString();
        		// Process the .. -> back
        		if (subpath.equals("..")){
        			desiredPath = desiredPath.subpath(0, desiredPath.getNameCount()-1);
        		// Process the rest of the commands
        		} else {
        			desiredPath = desiredPath.resolve(subpath);
        		}
        	}
        	// Append the beginning of a root
        	desiredPath = actualPath.getRoot().resolve(desiredPath);
        	desiredPath = desiredPath.resolve(".");

        	if (desiredPath.toFile().isDirectory()){        		
        		actualPath = desiredPath;
        		out.println("Vous etes dans le dossier " + actualPath.subpath(actualPath.getNameCount()-2, actualPath.getNameCount()-1).toString());
        	}
        	else {
        		out.println("Le dossier " + desiredPath.subpath(desiredPath.getNameCount()-2, desiredPath.getNameCount()-1).toString() + " n'existe pas");
        	}

        }
        
        // List all elements in a directory
        private void processLs() {
        	File[] files = new File(actualPath.toString()).listFiles();
        	for(File file : files){
        		if (file.isFile()){
        			out.println("[File] " + file.getName());
        		}
        		else if (file.isDirectory()){
        			out.println("[Folder] " + file.getName());
        		} 
        		else {
        			out.println("Neither a file or a folder");
        		}
        	}
        	// Si le dossier est vide.
        	if (files.length == 0) {
        		out.println("Aucun fichier dans le repertoire");
        	}
        }
        
        // Create a folder
        private void processMkdir(String folder) {
        	
		    if (new File(actualPath.resolve(folder).toString()).mkdirs()) {
		  	  	out.println("Le dossier " + folder + " a bien ete cree");
		    } else {
		    	out.println("Le dossier " + folder + " n'a pas ete cree");
		    }
        }
        
    	private void saveFile(String fileName) throws IOException {
    		DataInputStream dis = new DataInputStream(socket.getInputStream());
    		FileOutputStream fos = new FileOutputStream(fileName);
    		byte[] buffer = new byte[4096];
    		long fileSize = dis.readLong();
    		int read = 0;
    		while(fileSize > 0 && (read = dis.read(buffer)) > 0) {
    			fos.write(buffer, 0, read);
    			fileSize -= read;
    		}
    		fos.close();
    		out.println("Le fichier " + fileName + " a bien ete televerse");
    	}
    	
        private boolean isFileExist(String fileName){
        	
        	File file = actualPath.resolve(fileName).toFile();
        	if (!(file.isFile())){
        		out.println("Ce fichier n'existe pas.");
        		return false;
        	} else {
        		out.println("Downloading...");
        		return true;
        	}
        }
    	
    	private void sendFile(String fileName) throws IOException {
    		
    		File file = actualPath.resolve(fileName).toFile();
    		DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
    		FileInputStream fis = new FileInputStream(file.toString());
    		byte[] buffer = new byte[4096];
    		int read;
    		dos.writeLong(file.length());
    		while ((read=fis.read(buffer)) > 0) {
    			dos.write(buffer, 0, read);
    		}
    		fis.close();
    		out.println("Le fichier " + fileName + " a bien ete telecharge");
    	}


        public void run() {
            try {

                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // Send a welcome message to the client.
                out.println("Hello, you are client #" + clientNumber + ".");
                
                // Get where is the client jar files run
                String input = in.readLine();
                actualPath = Paths.get(input);
                
                // Get ip and port from the client
                input = in.readLine();
                IpAdressClient = firstWordFromCommand(input);
                portClient = secondWordFromCommand(input);

                // While exit is not entered
                while (true) {
                    input = in.readLine();
                    if (input == null) {
                        break;
                    }
                    logServer(input);
                    processCommand(input);
                    // This command is done.
                    out.println("done");
                    
                }
            } catch (IOException e) {
                log("Error handling client# " + clientNumber + ": " + e);
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    log("Couldn't close a socket, what's going on?");
                }
                log("Connection with client# " + clientNumber + " closed");
            }
        }

        private void logServer(String message) {
        	DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd@HH:mm:ss");
        	Date date = new Date();
        	System.out.println("[" + IpAdressClient + ":" + portClient + " - " + dateFormat.format(date) + "]" + " : " + message);
        }
        
        private void log(String message) {
            System.out.println(message);
        }
    }
}