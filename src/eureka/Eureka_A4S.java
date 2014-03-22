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
import java.util.Enumeration;

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
	// private static int REPORT_DIGITAL = 0xD0; // Habilita digital input de
	// dados
	// pela porta

	private static final int PORT = 12345; // Configure para a porta que sua
											// extension ira receber conexoes
	// private static int volume = 8; // Troque para os dados de sua extension

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
		System.out.println("\n\r\n\r");
		System.out.println("+-------------------------------------------------+");
		System.out.println("|***** ARDUINANDO COM SCRATCH COM EUREKA_A4S *****|");
		System.out.println("|-------------------------------------------------|");
		System.out.println("| Servidor para controlar o Arduino pelo Scratch. |");
		System.out.println("|        - By Ville Medeiros do Patrulha Eureka.  |");
		System.out.println("+-------------------------------------------------+");

		CommPortIdentifier portIdentifier;
		CommPort commPort;

		int i = 0;

		// 1) Verificacao dos parametros
		try {
			if (args.length < 1) {
				System.err.println("Error: Porta (USB) serial nao informada.\n");
				System.err.println("Portas Seriais encontradas:");

				Enumeration portIdentifiers = CommPortIdentifier.getPortIdentifiers();
				while (portIdentifiers.hasMoreElements()) {
					CommPortIdentifier pid = (CommPortIdentifier) portIdentifiers.nextElement();
					System.out.println(" -> " + pid.getName());
				}
				System.err.println("\n\rInforme a PORTA SERIAL na linha de comando.");
				System.err.println("Exemplo: $ java -jar Eureka_A4S.jar <porta serial>");

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

				System.err.println("Problemas na localizacao da Porta Serial:" + args[0]);
				System.err.println(i + " " + e);

				System.exit(0);
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
				System.err.println("Error: Apenas portas seriais devem ser utilizadas.");
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
		int i;
		byte[] buf = new byte[5000];
		;

		int bytes_read = sockIn.read(buf, 0, buf.length);
		StringBuilder httpBuffer = new StringBuilder(new String(Arrays.copyOf(buf, bytes_read)));

		i = httpBuffer.indexOf("\n");

		String header = httpBuffer.substring(0, i);
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
		StringBuilder httpResponse = new StringBuilder("HTTP/1.1 200 OK" + crlf);
		httpResponse.append("Content-Type: text/html; charset=ISO-8859-1" + crlf);
		httpResponse.append("Access-Control-Allow-Origin: *" + crlf);
		httpResponse.append(crlf);
		httpResponse.append(s + crlf);
		try {
			byte[] outBuf = httpResponse.toString().getBytes();
			sockOut.write(outBuf, 0, outBuf.length);
		} catch (Exception ignored) {
		}
	}

	private static void doCommand(String cmdAndArgs) {
		// Essential: manuseia os comandos entendidos pelo servidor
		StringBuilder response = new StringBuilder("okay");
		String[] parts = cmdAndArgs.split("/");
		String cmd = parts[0];

		if (DEBUG_ACTIVE)
			if (cmd.equals("poll") == false)
				System.out.print("  > "+cmdAndArgs + "\n");

		if (cmd.equals("pinMode")) {
			if ("input".equals(parts[2])) // added pwm
			{
				arduino.pinMode(Integer.parseInt(parts[1]), Firmata.INPUT);

				// set report active without this digital input is not updated
				if (DEBUG_ACTIVE)
					System.out.println("sent digital report");
				arduino.init();
			} else if ("output".equals(parts[2]))
				arduino.pinMode(Integer.parseInt(parts[1]), Firmata.OUTPUT);
			else if ("pwm".equals(parts[2])) // added pwm
			{
				arduino.pinMode(Integer.parseInt(parts[1]), Firmata.PWM);
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
			// monta o conjunto dodos enviado para o Scratch no formato "pino valor"
			//			digitalRead/2 false
			//			digitalRead/3 false
			//			digitalRead/4 false
			//			digitalRead/5 false
			//			digitalRead/6 false
			//			digitalRead/7 false
			//			digitalRead/8 false
			//			digitalRead/9 false
			//			digitalRead/10 false
			//			digitalRead/11 false
			//			digitalRead/12 false
			//			digitalRead/13 false
			//			analogRead/0 0
			//			analogRead/1 0
			//			analogRead/2 0
			//			analogRead/3 0
			//			analogRead/4 0
			//			analogRead/5 0
			
			response = new StringBuilder("");
			for (int i = 2; i <= 13; i++) {
				response.append("digitalRead/").append(i).append(" ")
						.append(arduino.digitalRead(i) == Firmata.HIGH ? "true" : "false").append("\n");
			}
			for (int i = 0; i <= 5; i++) {
				response.append("analogRead/").append(i).append(" ").append(arduino.analogRead(i)).append("\n");

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
			response.append("comando desconhecido: ").append(cmd);
		}

		sendResponse(response.toString());

	}

	private static void doHelp() {
		// Optional: return a list of commands understood by this server
		String help = "Servidor HTTP para controlar o Arduino pelo Scratch.<br><br>";
		sendResponse(help);
	}

}
