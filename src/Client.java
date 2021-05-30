import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


import java.io.*;

public class Client {
	// initialize socket and input output streams
	private static Socket socket = null;
	private BufferedReader input = null;
	private DataOutputStream out = null;
	private BufferedReader in = null;
	

	// constructor to put ip address and port
	public Client(String address, int port) throws IOException {

		// connect to server
		connect(address, port);

		// takes input from keyboard
		input = new BufferedReader(new InputStreamReader(System.in));

		// sends output to the server
		out = new DataOutputStream(socket.getOutputStream());

		// gets input from server
		in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
	}

	private void start () {
	
		// Initial handshake
		sendMessage("HELO");
		readMessage();

		// Send user details
		sendMessage("AUTH " + System.getProperty("user.name"));
		readMessage();

		// handshake completed
		boolean connected = true;

		// populate arrayList of server objects from:
		ArrayList<Server> servers = new ArrayList<Server>();

		// arrayList for holding job info from "GETS Capable"
		ArrayList<Job> jobs = new ArrayList<Job>();
		

		// Tells client it is ready to recieve commands
		sendMessage("REDY");

		// temp string to hold readMessage data
		// we check the contents of this string, rather than call readMessage() 
		String msg = readMessage();
		

		// SCHEDULES JOB TO LARGEST SERVER
		while (connected){
			// Job completed we tell ds-server we are ready
			if (msg.contains("JCPL")){
				sendMessage("REDY");
				msg = readMessage(); 
				// there are no more jobs left
			} else if (msg.contains("NONE")){ // there are no more jobs left
				connected = false;
				sendMessage("QUIT");
			}else {

				// Get next message
				if (msg.contains("OK")){ 
					sendMessage("REDY");
					msg = readMessage();
				}

				// we have a JOB incoming, so we create a job objet based on it
				if (msg.contains("JOBN")){
					jobs.add(addJob(msg)); // create job 

					// the job arrayList will only ever have 1 item in it at a time...
					sendMessage(getsCapable(jobs.get(0))); // GETS Capable called
					msg = readMessage();

					sendMessage("OK");

					// list of capable servers are added to arrayList of server objects
					msg = readMessage();
					servers = addServer(msg);
					sendMessage("OK");
				
					// we should receive a "." here
					msg = readMessage();

					sendMessage(lowCost(servers, jobs)); // Scheduling algorithm called here
					msg = readMessage();

					// only need one job at a time
					jobs.remove(0);
				} 
			} 
		}

		
		// close the connection
		try {

			// QUIT hand-shake, must receive confirmation from server for quit
			if (readMessage().contains("QUIT")){
				input.close();
				out.close();
				socket.close();
			}
				
		} catch (IOException i) {
			//System.out.println(i);
		}

		// Exit the program
		System.exit(1);
	}


	// receives string input and the largest server as input
	// schedules job to largest server
	//private String toLargest(String job, Server s){
	//	String[] splitStr = job.split("\\s+");
	//	return "SCHD " + splitStr[2] + " " + s.getType() + " " + (s.getLimit()-s.getLimit());
	//	
	//	// the index [2] represents the job ID from the String "job"
	//	//		This string is sent by ds-server to the client 
	//	
	//}

	
	//
	// Most basic implementation of first fit
	//
	public String firstFit(ArrayList<Server> servers, ArrayList<Job> job){

		// This method will always pick the first server
		// Without checking any job requirements
		// Or any server resources

		String ServerInfo = "";
		ServerInfo = servers.get(0).getType() + " " + servers.get(0).getID();
		// Scheduling command for first server
		return "SCHD " + job.get(0).getID() + " " + ServerInfo;
	}

	//
	// Low cost implementation
	// Reduce cost server rental cost
	// But increased wait time and turn around time
	//
	public String lowCost(ArrayList<Server> servers, ArrayList<Job> job){

		// Server information string
		String ServerInfo = "";

		for (Server server: servers) {

			// find best fit for job
			if (server.getDisk() >= job.get(0).getDiskReq() &&
				server.getCores() >= job.get(0).getCoreReq() &&
				server.getMemory() >= job.get(0).getMemoryReq() &&
				// Ensure start time is greater than runnning time
				job.get(0).getStartTime() >= job.get(0).getRunTime()) {

					// Ensure the server is already active or idle to reduce cost
					ServerInfo = server.getType() + " " + server.getID();
					return "SCHD " + job.get(0).getID() + " " + ServerInfo;
			}
			// When there is no optimal server, just use first server.
			else {
				// Send job to first server
				ServerInfo = servers.get(0).getType() + " " + servers.get(0).getID();
			}
		}
		// There is only one job in queue so schedule it
		return "SCHD " + job.get(0).getID() + " " + ServerInfo;
	}

	//
	// Create new server object
	//
	public ArrayList<Server> addServer(String s){

		// remove trailing spaces
		s = s.trim();

		// temp arrayList to be passed back
		ArrayList<Server> newList = new ArrayList<Server>();

		// split strings by newline
		String[] lines = s.split("\\r?\\n");
 		
		for (String line : lines) {

			// split each line by white space
			String[] splitStr = line.split("\\s+");

			// 
			//	Generate new server with these values:
			//
			//	String	serverType
			//	int		serverID
			//	String	state
			//	int		startTime
			//	int		cores
			//	int		memory
			//	int		disk
			//	int		watTime
			//	int		runTime

			//						server type		server ID					state		curStart Time					core count						memory						disk							wJobs							rJobs
			Server server = new Server(splitStr[0], 
								Integer.parseInt(splitStr[1]),
								splitStr[2], 
								Integer.parseInt(splitStr[3]), 
								Integer.parseInt(splitStr[4]), 
								Integer.parseInt(splitStr[5]), 
								Integer.parseInt(splitStr[6]), 
								Integer.parseInt(splitStr[7]), 
								Integer.parseInt(splitStr[8]) 
								);
			newList.add(server);
        }

		return newList;
	}

	
	//
	//	create a new job object
	//
	public Job addJob(String job){

		// remove trailing spaces
		job = job.trim();

		// split string up by white space; "\\s+" is a regex expression
		String[] splitStr = job.split("\\s+");

		//
		//	Create new job
		//
		//		int submit Time
		//		int jobID	 
		//		int run Time 	
		//		int core req
		//		int	memory req
		// 	 	int	disk req
		//
		Job j = new Job(Integer.parseInt(splitStr[1]), 
						Integer.parseInt(splitStr[2]), 
						Integer.parseInt(splitStr[3]),  
						Integer.parseInt(splitStr[4]),
						Integer.parseInt(splitStr[5]), 
						Integer.parseInt(splitStr[6]));

		// returns job object to fill arrayList
		return j;
	}

	// 
	//	We have chosen to comment out the console printing portions of
	//	sendMessage() and readMessage() as it significantly reduces the
	//	speed of the test files/scripts. 
	//
	private void sendMessage (String outStr) {
		// send message to server
		byte[] byteMsg = outStr.getBytes();
		try {
			out.write(byteMsg);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Display outgoing message from client
		//System.out.println("OUT: " + outStr);		
	}

	private String readMessage () {
		// read string sent from server
		String inStr = "";
		char[] cbuf = new char[Short.MAX_VALUE];
		try {
			in.read(cbuf);
		} catch (IOException e) {
			e.printStackTrace();
		}
		inStr = new String(cbuf, 0, cbuf.length);

		// Display incoming message from server
		// System.out.println("INC: " + inStr);
		return inStr;
	}

	//
	//	GET THE AVAILABLE SERVERS with "GETS Capable"
	//
	public String getsCapable(Job j){
		
		// grab info from job inputted job object
		return("GETS Capable " + j.getCoreReq() + " " + j.getMemoryReq() + " " + j.getDiskReq());
	}

	private static void connect(String address, int port) {
		// try establish a connection
		double secondsToWait = 1;
		int tryNum = 1;
		while (true) {
			try {
				//System.out.println("Connecting to server at: " + address + ":" + port);
				socket = new Socket(address, port);
				//System.out.println("Connected");
				break; // we connected, so exit the loop
			} catch (IOException e) {
				// reconnect failed, wait.
				secondsToWait = Math.min(30, Math.pow(2, tryNum));
				tryNum++;
				//System.out.println("Connection timed out, retrying in  " + (int) secondsToWait + " seconds ...");
				try {
					TimeUnit.SECONDS.sleep((long) secondsToWait);
				} catch (InterruptedException ie) {
					// interrupted
				}
			}
		}
	}

	//
	// Read XML provider by server
	//
	public static ArrayList<Server> readXML(String fileName){
        ArrayList<Server> serverList = new ArrayList<Server>();
		
		try {
			// XML file to read
			File systemXML = new File(fileName);

			// Setup XML document parser
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(systemXML);

			// String converting to normalized form
			doc.getDocumentElement().normalize();
			NodeList servers = doc.getElementsByTagName("server");
			for (int i = 0; i < servers.getLength(); i++) {
				Element server = (Element) servers.item(i);

				// Parse all XML attributes to new Server object
				String type = server.getAttribute("type");
				int limit = Integer.parseInt(server.getAttribute("limit"));
				int bootupTime = Integer.parseInt(server.getAttribute("bootupTime"));
				float hourlyRate = Float.parseFloat(server.getAttribute("hourlyRate"));
				int coreCount = Integer.parseInt(server.getAttribute("coreCount"));
				int memory = Integer.parseInt(server.getAttribute("memory"));
				int disk = Integer.parseInt(server.getAttribute("disk"));
				
				Server s = new Server(type,limit,bootupTime,hourlyRate,coreCount,memory,disk);
				serverList.add(s);
			}

			
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return serverList;
    }


	public static void main(String args[]) throws IOException {
		// Specify Server IP address and Port
		Client client = new Client("127.0.0.1", 50000);
		client.start();
	}
	
}