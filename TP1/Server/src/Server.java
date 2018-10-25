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

	private static final Pattern PATTERN = Pattern.compile(
			"^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");
	
	public static boolean validateIp(final String ip) {
		return PATTERN.matcher(ip).matches();
	}
	
	public static boolean validatePort(final int port) {
		if (port >= 5000 && port <= 5500){
			return true;
		}
		else {
			return false;
		}
	}
	
//    private static void log(String message) {
//        System.out.println(message);
//    }

    public static void main(String[] args) throws Exception {
    	
    	int clientNumber = 1;
      
//        // Enter IP address 
//        Server.log("Enter IP Address of the Server:");
//  	    String serverAddress = System.console().readLine();
//  	    
//  	    // If IP address entered is wrong.
//        while (!Server.validateIp(serverAddress)){
//            Server.log("Wrong IP Address. Enter another one:");
//        	serverAddress = System.console().readLine();
//        }
//        
//        // Enter Port
//        Server.log("Enter Port for the server :");
//        int port = Integer.parseInt(System.console().readLine());
//        
//        // If Port entered is wrong.
//        while (!Server.validatePort(port)){
//            Server.log("Wrong Port. Should be between 5000 and 5500. Enter another one:");
//            port = Integer.parseInt(System.console().readLine());
//        }
    	String serverAddress = "127.0.0.1";
    	int port = 5000;
        
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

    /**
     * A private thread to handle capitalization requests on a particular
     * socket.  The client terminates the dialogue by sending a single line
     * containing only a period.
     */
    private static class Manager extends Thread {
        private Socket socket;
        private int clientNumber;
        private Path actualPath;
        private String IpAdressClient;
        private String portClient;

        public Manager(Socket socket, int clientNumber) {
            this.socket = socket;
            this.clientNumber = clientNumber;
            log("New connection with client# " + clientNumber + " at " + socket);
            actualPath = new File(".").toPath().toAbsolutePath();
        }
        
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
        
        private String secondWordFromCommand(String command){
        	String secondWord = "";
            if (command.contains(" ")){
            	secondWord = command.substring(command.indexOf(" ") + 1, command.length());
            }
            return secondWord;
        }
        
        private void processCommand(String command, PrintWriter out) {
        	
        	
        	switch(firstWordFromCommand(command))
        	{
        	case "ls" :
        		processLs(out);
        		break; 
        		
        	case "mkdir":
        		processMkdir(secondWordFromCommand(command), out);
			    break; 
			    
        	case "cd":
        		processCd(secondWordFromCommand(command), out);
			    break;
			    
        	case "upload":
    			try {
    				saveFile(out, socket, (secondWordFromCommand(command)));
    			} catch (IOException e) {
    				e.printStackTrace();
    			}
			    break;
        	case "download":
    			try {
    				sendFile(out, socket, (secondWordFromCommand(command)));
    			} catch (IOException e) {
    				e.printStackTrace();
    			}
			    break;
        	   
    	    default:
    	    	out.println("default");
    	    	break;
        	}
        }
        
        private void processCd(String secondArgument, PrintWriter out){
        	
        	Path desiredPath = actualPath.subpath(0, actualPath.getNameCount()-1);
        	
        	Path path = Paths.get(secondArgument);
        	for (int i = 0; i < path.getNameCount(); i++){
        		
        		String subpath = path.subpath(i, i + 1).toString();
        		if (subpath.equals("..")){
        			desiredPath = desiredPath.subpath(0, desiredPath.getNameCount()-1);
        			// MAC
        			// desiredPath = Paths.get("/", desiredPath.subpath(0, desiredPath.getNameCount()-1).toString());
        		} else {
        			desiredPath = desiredPath.resolve(subpath);
        			// MAC
        			// desiredPath = Paths.get("/", desiredPath.toString(), "/" , subpath);
        		}
        	}
        	desiredPath = actualPath.getRoot().resolve(desiredPath);
        	desiredPath = desiredPath.resolve(".");

        	// MAC
        	// desiredPath = Paths.get(desiredPath.toString(), "/.");
        	
        	if (desiredPath.toFile().isDirectory()){        		
        		actualPath = desiredPath;
        		out.println("Vous etes dans le dossier " + actualPath.subpath(actualPath.getNameCount()-2, actualPath.getNameCount()-1).toString());
        	}
        	else {
        		out.println("Le dossier " + desiredPath.subpath(desiredPath.getNameCount()-2, desiredPath.getNameCount()-1).toString() + " n'existe pas");
        	}

        }
        
        private void processLs(PrintWriter out) {
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
        
        private void processMkdir(String folder, PrintWriter out) {
        	
		    if (new File(actualPath.resolve(folder).toString()).mkdirs()) {
		  	  	out.println("Le dossier " + folder + " a bien ete cree");
		    } else {
		    	out.println("Le dossier " + folder + " n'a pas ete cree");
		    }
        }
        
    	private void saveFile(PrintWriter out, Socket sock, String fileName) throws IOException {
    		DataInputStream dis = new DataInputStream(sock.getInputStream());
    		FileOutputStream fos = new FileOutputStream(fileName);
    		byte[] buffer = new byte[4096];
    		
    		int read = 0;
    		while((read = dis.read(buffer)) > 0) {
    			System.out.println("read " + read + " bytes.");
    			fos.write(buffer, 0, read);
    			buffer = new byte[4096];
    			log("writeDone");
    		}
    		
    		fos.close();
    		out.println("Le fichier a ete ajoute");
    	}
    	
    	private boolean sendFile(PrintWriter out, Socket sock, String fileName) throws IOException {
    		
        	File file = new File(actualPath.resolve(fileName).toString());
        	if (!(file.isFile())){
        		log("Ce fichier n'existe pas.");
        		return false;
        	}
        	
        	DataOutputStream dos = new DataOutputStream(sock.getOutputStream());
    		FileInputStream fis = new FileInputStream(file);
    		byte[] buffer = new byte[4096];
    		int read;
    		
    		while ((read=fis.read(buffer)) > 0) {
    			dos.write(buffer, 0, read);
    		}
    		out.println("Le fichier a ete envoye");
    		fis.close();
        	return true;
    	}


        public void run() {
            try {

                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                // Send a welcome message to the client.
                out.println("Hello, you are client #" + clientNumber + ".");
                String input = in.readLine();
                IpAdressClient = firstWordFromCommand(input);
                portClient = secondWordFromCommand(input);

                // Get messages from the client, line by line; return them
                // capitalized
                while (true) {
                    input = in.readLine();
                    if (input == null || input.equals("exit")) {
                        break;
                    }
                    logServer(input);
                    processCommand(input, out);
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