//// Source :
//// Ray Toal
//// Department of Electrical Engineering and Computer Science
//// Loyola Marymount University
//// http://cs.lmu.edu/~ray/notes/javanetexamples/

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
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
		
        System.out.format("The capitalization server is running on %s:%d%n", serverAddress, port);
    
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
    				saveFile(socket);
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
        	
        	Path desiredPath = actualPath.subpath(0, actualPath.getNameCount()-1);;
//        	if (desiredPath.endsWith(".")){
//        		desiredPath = .subpath(0, actualPath.getNameCount()-1);
//        	}
        	
        	Path path = Paths.get(secondArgument);
        	for (int i = 0; i < path.getNameCount(); i++){
        		
        		String subpath = path.subpath(i, i + 1).toString();
        		if (subpath.equals("..")){
        			desiredPath = Paths.get("/", desiredPath.subpath(0, desiredPath.getNameCount()-1).toString());
        		} else {
        			desiredPath = Paths.get("/", desiredPath.toString(), "/" , subpath);
        		}
        	}
        	desiredPath = Paths.get(desiredPath.toString(), "/.");
        	log(desiredPath.toString());
        	
        	if (desiredPath.toFile().isDirectory()){        		
        		actualPath = desiredPath;
        		out.println("Vous êtes dans le dossier " + actualPath.subpath(actualPath.getNameCount()-2, actualPath.getNameCount()-1).toString());
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
        }
        
        private void processMkdir(String folder, PrintWriter out) {
        	
		    if (new File(folder).mkdirs()) {
		  	  	out.println("Le dossier " + folder + " a bien ete cree");
		    } else {
		    	out.println("Le dossier " + folder + " n'a pas ete cree");
		    }
        }
        
    	private void saveFile(Socket sock) throws IOException {
    		DataInputStream dis = new DataInputStream(sock.getInputStream());
    		FileOutputStream fos = new FileOutputStream("testfile.txt");
    		byte[] buffer = new byte[4096];
    		
    		int read = 0;
    		while((read = dis.read(buffer)) > 0) {
    			System.out.println("read " + read + " bytes.");
    			fos.write(buffer, 0, read);
    			buffer = new byte[4096];
    			log("writeDone");
    		}
    		
    		fos.close();
    		dis.close();
    	}

        /**
         * Services this thread's client by first sending the
         * client a welcome message then repeatedly reading strings
         * and sending back the capitalized version of the string.
         */
        public void run() {
            try {

                // Decorate the streams so we can send characters
                // and not just bytes.  Ensure output is flushed
                // after every newline.
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                // Send a welcome message to the client.
                out.println("Hello, you are client #" + clientNumber + ".");

                // Get messages from the client, line by line; return them
                // capitalized
                while (true) {
                    String input = in.readLine();
                    if (input == null || input.equals("exit")) {
                        break;
                    }
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

        /**
         * Logs a simple message.  In this case we just write the
         * message to the server applications standard output.
         */
        private void log(String message) {
            System.out.println(message);
        }
    }
}