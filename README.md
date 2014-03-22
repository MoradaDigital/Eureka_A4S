# Eureka_A4S (Arduinando com o Scratch)

Eureka_A4S eh um servidor que disponibiliza um link de comunicacao entre o [Scratch 2](http://scratch.mit.edu) e o [Arduino](http://www.arduino.cc) executando o [Firmata](http://firmata.org/).

## Funcionamento
Em sua versao 2.0 o [Scratch](http://scratch.mit.edu) possui o recurso [Scratch Extension](http://wiki.scratch.mit.edu/wiki/Scratch_Extension) que permite a conexao do Scratch com outros sistemas, sejam eles Hardware ou Web.
O Eureka_A4S eh um servidor Java que conecta com o Scratch utilizando o protocolo HTTP e se comunica com o Arduino pela porta (USB) Serial.

## Instrucoes

### Video para configuracao, instalacao e testes
1. Acesse o link [Arduinando com Scratch](http://www.patrulhaeureka.org/#!arduinando-com-scratch/c185f) e acompanhe passo a passo de como utilizar o Eureka_A4S.

### Linha de Comando

1. Instale o [Scratch 2 offline editor](http://scratch.mit.edu/scratch2download/).
2. Instale o [Arduino software](http://arduino.cc/en/Main/Software). Instrucoes: [Windows](http://arduino.cc/en/Guide/Windows), [Mac OS X](http://arduino.cc/en/Guide/MacOSX).
3. Instale o StandardFirmata no seu Arduino. (Ele vem junto com a instalacao do "Arduino Software" e eh encontrado no menu: Examples > Firmata).
4. Download do [Eureka_A4S](https://github.com/villemedeiros/Eureka_A4S/archive/master.zip) do GitHub e descompacte-o.
5. Execute o servidor usando o script "Eureka_A4S_run.sh" na linha de comando, passando como parametro o nome da porta serial que o seu Arduino esta.

		Exemplo: $ Eureka_A4S_run.sh /dev/tty.usbmodemfd131
		
   Devera ver uma mensagem como esta:
   
   		|*************** EUREKA_A4S *****************|
		| App para controlar o Arduino pelo Scratch. |
		|   - By Ville Medeiros do Patrulha Eureka.  |
		|____________________________________________|
		Abrindo conexao com Arduino....
		Arduino UP na porta.: /dev/tty.usbmodemfd131
		
		Abrindo conexao com Scratch....
		Scratch UP no host.: VillaoMAC.local/10.1.1.2
	
6. Execute o Scratch 2 offline editor.
7. Com a tecla "shift" pressionada click no menu "Arquivo", assim sera disponibilizado a opcao "Import Experimental Extension" no final do menu. Click nele.
8. Navegue nos diretorios do Eureka_A4S e selecione o arquivo Eureka_A4S.s2e.
9. Voce devera ver a extensao Eureka_A4S e seu blocos em "Mais Blocos" na aba Roteiros do Scratch Editor.
   Se o servidor Eureka_A4S estiver executando corretamente, vai aparecer uma bolinha verde proximo do titulo Eureka_A4S. 

Agora eh so programar.
No diretorio exemplos existem alguns projetos para testa o Eureka_A4S.   

## Creditos
O Eureka_A4S esta sendo desenvolvido por [Ville Medeiros](mailto:ville.medeiros@gmail.com) inspirado nos projetos: [A4S](https://github.com/damellis/A4S/) by David Mellis e [scratch4arduino](https://github.com/thatpixguy/scratch4arduino) by Thatpixguy, todos baseados na documentacao e codigos do [Time do Scratch](http://wiki.scratch.mit.edu/wiki/Scratch_Extension_Protocol_(2.0)).

Para atualizacoes acesse o link do projeto Eureka_A4S: <https://github.com/villemedeiros/Eureka_A4S>.
   