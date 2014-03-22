package eureka;

//************************** Eureka_A4S *********************************** 
// ƒ um fork dos seguintes projeto:
// Baseado nas ideias do projeto A4S
//    -> https://github.com/thatpixguy/scratch4arduino - by Claudio Becchetti
//      |-> https://github.com/cbecc/scratch4arduino - by thatpixguy
//         |-> https://github.com/villemedeiros/eureka_a4s - by Ville Medeiros
//
// O Eureka_A4S Ž um Helper app que executa um servidor HTTP que permite o Scrath a se comunicar
// com as placas Arduino rodando o Firmata firmware.
//
// Agradeco ao Claudio Becchetti e o Thatpixguy.
//
//

import java.io.*;
import java.net.*;
import java.util.*;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;

import org.firmata.Firmata;

public class Eureka_A4S {
	private static boolean DEBUG_ACTIVE = true;
	private static int num_of_poll_reponse = 0;

	private static int refresh_rate_30msec = 60;// after 1800 millisec arduino.
												// init
	//private static int REPORT_DIGITAL = 0xD0; // Habilita digital input de dados
												// pela porta

	private static final int PORT = 12345; // Configure para a porta que sua
											// extension ira receber conexoes
	//private static int volume = 8; // Troque para os dados de sua extension

	private static InputStream sockIn;
	private static OutputStream sockOut;

	private static SerialPort serialPort;
	private static Firmata arduino;

	private static SerialReader reader;

	public static class SerialReader implements SerialPortEventListener {
		public void serialEvent(SerialPortEvent e) {
			try {
				while (serialPort.getInputStream().available() > 0)
					arduino.processInput(serialPort.getInputStream().read());
			} catch (IOException err) {
				System.err.println(err.getStackTrace()[0].getLineNumber() + ":" + err);
			}
		}
	}

	public static class MyWriter implements Firmata.Writer {
		public void write(int val) {
			try {
				serialPort.getOutputStream().write(val);
			} catch (IOException err) {
				System.err.println(err.getStackTrace()[0].getLineNumber() + ":" + err);
			}
		}
	}

	public static MyWriter writer;

	public static void main(String[] args) throws IOException {

		System.out.println("\n\r\n\r|*************** EUREKA_A4S *****************|");
		System.out.println("| App para controlar o Arduino pelo Scratch. |");
		System.out.println("|   - By Ville Medeiros do Patrulha Eureka.  |");
		System.out.println("|____________________________________________|");

		CommPortIdentifier portIdentifier;
		CommPort commPort;
		int i = 0;

		// 1) Verificacao dos parametros
		try {
			if (args.length < 1) {
				System.err.println("Informe a PORTA SERIAL na linha de comando.");
				System.err.println("java -jar Eureka_A4S <porta serial>");

				return;
			}

		} catch (Exception e) {
			System.err.println("Porta Serial n‹o encontrada como parametro");
			System.err.println(e.getStackTrace()[0].getLineNumber() + ":" + e);
			return;
		}

		// 2) Localizando o identificador da porta serial
		while (true) {

			try {
				i++;
				Thread.sleep(1000);
				portIdentifier = CommPortIdentifier.getPortIdentifier(args[0]);

				break;
			} catch (Exception e) {

				System.err.println("Problemas na localizacao da Porta Serial " + args[0]);
				System.err.println(i + " " + e);
				
			}
		}

		// 3) Abrindo a Porta Serial (bind)
		try {

			commPort = portIdentifier.open("Eureka_A4S", 2000);

		} catch (Exception e) {
			System.err.println("Problemas na ebertura da porta " + args[0]);
			System.err.println(e);
			return;
		}

		try {
			System.out.println("Abrindo conexao com Arduino....");
			if (commPort instanceof SerialPort) {
				serialPort = (SerialPort) commPort;
				serialPort.setSerialPortParams(57600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
						SerialPort.PARITY_NONE);

				writer = new MyWriter();

				arduino = new Firmata(writer);
				reader = new SerialReader();

				serialPort.addEventListener(reader);
				serialPort.notifyOnDataAvailable(true);
			} else {
				System.out.println("Error: Apenas portas seriais sao utilizadas.");
				return;
			}

		} catch (Exception e) {
			System.err.println("Error: n‹o foi possivel abrir a porta.: " + args[0]);
			System.err.println(e);
			return;
		}

		System.out.println("Arduino UP na porta.: " + args[0]);

		System.out.println("\nAbrindo conexao com Scratch....");
		InetAddress addr = InetAddress.getLocalHost();

		ServerSocket serverSock = new ServerSocket(PORT);
		System.out.println("Scratch UP no host.: " + addr.toString());

		while (true) {
			Socket sock = serverSock.accept();
			sockIn = sock.getInputStream();
			sockOut = sock.getOutputStream();
			try {
				handleRequest();
			} catch (Exception e) {
				e.printStackTrace(System.err);
				sendResponse("Error: servidor n‹o esta respondendo.");
			}
			sock.close();
		}

	}

	private static void handleRequest() throws IOException {
		String httpBuf = "";
		int i;

		// read data until the first HTTP header line is complete (i.e. a '\n'
		// is seen)
		while ((i = httpBuf.indexOf('\n')) < 0) {
			byte[] buf = new byte[5000];
			int bytes_read = sockIn.read(buf, 0, buf.length);
			if (bytes_read < 0) {
				System.out.println("Socket closed; no HTTP header.");
				return;
			}
			httpBuf += new String(Arrays.copyOf(buf, bytes_read));
		}

		String header = httpBuf.substring(0, i);
		if (header.indexOf("GET ") != 0) {
			System.out.println("This server only handles HTTP GET requests.");
			return;
		}
		i = header.indexOf("HTTP/1");
		if (i < 0) {
			System.out.println("Bad HTTP GET header.");
			return;
		}
		header = header.substring(5, i - 1);
		if (header.equals("favicon.ico"))
			return; // igore browser favicon.ico requests
		else if (header.equals("crossdomain.xml"))
			sendPolicyFile();
		else if (header.length() == 0)
			doHelp();
		else
			doCommand(header);
	}

	private static void sendPolicyFile() {
		// Send a Flash null-teriminated cross-domain policy file.
		String policyFile = "<cross-domain-policy>\n" + "  <allow-access-from domain=\"*\" to-ports=\"" + PORT
				+ "\"/>\n" + "</cross-domain-policy>\n\0";
		sendResponse(policyFile);
	}

	private static void sendResponse(String s) {
		String crlf = "\r\n";
		String httpResponse = "HTTP/1.1 200 OK" + crlf;
		httpResponse += "Content-Type: text/html; charset=ISO-8859-1" + crlf;
		httpResponse += "Access-Control-Allow-Origin: *" + crlf;
		httpResponse += crlf;
		httpResponse += s + crlf;
		try {
			byte[] outBuf = httpResponse.getBytes();
			sockOut.write(outBuf, 0, outBuf.length);
		} catch (Exception ignored) {
		}
	}

	private static void doCommand(String cmdAndArgs) {
		// Essential: handle commands understood by this server
		String response = "okay";
		String[] parts = cmdAndArgs.split("/");
		String cmd = parts[0];

		if (DEBUG_ACTIVE)
			if (cmd.equals("poll") == false)
				System.out.print(cmdAndArgs+"\n");

		// try {
		/*
		 * old commands to be removed if (cmd.equals("pinOutput")) {
		 * arduino.pinMode(Integer.parseInt(parts[1]), Firmata.OUTPUT); } else
		 * if (cmd.equals("pinInput")) {
		 * arduino.pinMode(Integer.parseInt(parts[1]), Firmata.INPUT);
		 * 
		 * } else if (cmd.equals("pinPwm")) {// added pwm
		 * arduino.pinMode(Integer.parseInt(parts[1]), Firmata.PWM);
		 * 
		 * } else if (cmd.equals("pinHigh")) {
		 * arduino.digitalWrite(Integer.parseInt(parts[1]), Firmata.HIGH); }
		 * else if (cmd.equals("pinLow")) {
		 * arduino.digitalWrite(Integer.parseInt(parts[1]), Firmata.LOW); } else
		 */
		if (cmd.equals("pinMode")) {
			if ("input".equals(parts[2])) // added pwm
			{
				arduino.pinMode(Integer.parseInt(parts[1]), Firmata.INPUT);

				// set report active without this digital input is not updated
				if (DEBUG_ACTIVE)
					System.out.println("sent digital report");
				/*
				 * for (int i = 0; i < 16; i++) {
				 * serialPort.getOutputStream().write(REPORT_DIGITAL | i);
				 * serialPort.getOutputStream().write(1); }
				 */
				arduino.init();
			} else if ("output".equals(parts[2]))
				arduino.pinMode(Integer.parseInt(parts[1]), Firmata.OUTPUT);
			else if ("pwm".equals(parts[2])) // added pwm
			{
				arduino.pinMode(Integer.parseInt(parts[1]), Firmata.PWM);
				// System.out.println("pwm requested \n");
			}
			if ("servo".equals(parts[2])) // added servo
			{
				arduino.pinMode(Integer.parseInt(parts[1]), Firmata.SERVO);
			}
		} else if (cmd.equals("digitalWrite")) {
			arduino.digitalWrite(Integer.parseInt(parts[1]), "high".equals(parts[2]) ? Firmata.HIGH : Firmata.LOW);
		} else if (cmd.equals("analogWrite")) {// added pwm
			arduino.analogWrite(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
		} else if (cmd.equals("servoWrite")) {// added servo
			arduino.servoWrite(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));

		} else if (cmd.equals("poll")) {
			// set response to a collection of sensor, value pairs, one pair per
			// line
			// in this example there is only one sensor, "volume"
			// response = "volume " + volume + "\n";
			response = "";
			for (int i = 2; i <= 13; i++) {
				response += "digitalRead/" + i + " " + (arduino.digitalRead(i) == Firmata.HIGH ? "true" : "false")
						+ "\n";
			}
			for (int i = 0; i <= 5; i++) {
				response += "analogRead/" + i + " " + (arduino.analogRead(i)) + "\n";

			}
			refresh_rate_30msec--;
			if (refresh_rate_30msec == 0) {
				refresh_rate_30msec = 200;
			}

			if (DEBUG_ACTIVE) {
				num_of_poll_reponse++;
				if (num_of_poll_reponse == 120) {
					num_of_poll_reponse = 0;
					 //System.out.println(" " + response);
				}
			}
		} else {
			response = "unknown command: " + cmd;
		}

		sendResponse(response);
		// } catch (IOException e) {
		// System.err.println(e); }
	}

	private static void doHelp() {
		// Optional: return a list of commands understood by this server
		String help = "HTTP Extension Example Server<br><br>";
		sendResponse(help);
	}

}
