import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class Client {

	public static void main(String[] args) throws UnknownHostException, IOException, ClassNotFoundException {

		Socket clientSocket = null;
		try {
			// Création d'un socket client vers le serveur. Ici 127.0.0.1 est indicateur que
			// le serveur s'exécute sur la machine locale. Il faut changer 127.0.0.1 pour
			// l'adresse IP du serveur si celui-ci ne s'exécute pas sur la même machine. Le port est 5000.
			clientSocket = new Socket("127.0.0.1", 5000);
			ObjectOutputStream objectOutput = new ObjectOutputStream(clientSocket.getOutputStream());
			// Ici, on suppose que le fichier que vous voulez inverser se nomme text.txt
			List<String> linesToSend = readFile("text.txt");
			// Écriture de l'objet à envoyer dans le output stream. Attention, la fonction
			// writeObject n'envoie pas l'objet vers le serveur! Elle ne fait qu'écrire dans
			// le output stream.
			objectOutput.writeObject(linesToSend);
			// Envoi des lignes du fichier texte vers le serveur sous forme d'une liste.
			objectOutput.flush();
			// Création du input stream, pour recevoir les données traitées du serveur.
			ObjectInputStream obj = new ObjectInputStream(clientSocket.getInputStream());
			@SuppressWarnings("unchecked")
			// Noté bien que la fonction readObject est bloquante! Ainsi, l'exécution du
			// client s'arrête jusqu'à la réception du résultat provenant du serveur!
			Stack<String> receivedStack = (Stack<String>) obj.readObject();
			// Écriture du résultat dans un fichier nommée FichierInversee.txt
			writeToFile(receivedStack, "FichierInversee.txt");
		} finally {
			// Fermeture du socket.
			clientSocket.close();
		}
	}

	// Fonction permettant de lire un fichier et de stocker son contenu dans une liste.
	private static List<String> readFile(String nomFichier) throws IOException {
		List<String> listOfLines = new ArrayList<String>();
		String line = null;
		FileReader fileReader = null;
		BufferedReader bufferedReader = null;
		try {
			fileReader = new FileReader(nomFichier);

			bufferedReader = new BufferedReader(fileReader);

			while ((line = bufferedReader.readLine()) != null) {
				listOfLines.add(line);
			}
		} finally {
			fileReader.close();
			bufferedReader.close();
		}
		return listOfLines;
	}

	// Fonction permettant d'écrire dans un fichier les données contenues dans la
	// stack reçu du serveur.
	private static void writeToFile(Stack<String> myStack, String nomFichier) throws IOException {
		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new FileWriter(nomFichier));
			while (!myStack.isEmpty()) {
				out.write(myStack.pop() + "\n");
			}
		} finally {
			out.close();
		}
	}
}