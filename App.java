package com.cn2.communication;

import java.io.*;
import java.net.*;

import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.*;
import java.awt.event.*;
import java.awt.Color;
import java.lang.Thread;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.DataLine;


public class App extends Frame implements WindowListener, ActionListener {

	/*
	 * Definition of the app's fields
	 */
	static TextField inputTextField;		
	static JTextArea textArea;				 
	static JFrame frame;					
	static JButton sendButton;				
	static JTextField meesageTextField;		  
	public static Color gray;				
	final static String newline="\n";		
	static JButton callButton;	
	static JButton hangupButton;
	
	
	/**
	 * Construct the app's frame and initialize important parameters
	 */
	
	static String peerIP = "192.168.43.235"; 
	static int peerPort = 6789;
	static int myPort = 6666;
	static int audio = 4444;
	static int audioPeer = 5555;
	
	static TargetDataLine microphone;
	static SourceDataLine speakers;
	
	
    static DatagramSocket socket;
    static DatagramSocket socketaudio;
    
    static {
        try {
            socket = new DatagramSocket(myPort);
        } catch (SocketException e) {
            e.printStackTrace(); 
        }
    }
    
    
    
	public App(String title) {
		
		/*
		 * 1. Defining the components of the GUI
		 */
		
		// Setting up the characteristics of the frame
		super(title);									
		gray = new Color(254, 254, 254);		
		setBackground(gray);
		setLayout(new FlowLayout());			
		addWindowListener(this);	
		
		// Setting up the TextField and the TextArea
		inputTextField = new TextField();
		inputTextField.setColumns(20);
		
		// Setting up the TextArea.
		textArea = new JTextArea(10,40);			
		textArea.setLineWrap(true);				
		textArea.setEditable(false);			
		JScrollPane scrollPane = new JScrollPane(textArea);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		
		//Setting up the buttons
		sendButton = new JButton("Send");			
		callButton = new JButton("Call");	
		hangupButton = new JButton("Hang Up");
						
		/*
		 * 2. Adding the components to the GUI
		 */
		add(scrollPane);								
		add(inputTextField);
		add(sendButton);
		add(callButton);
		add(hangupButton);
		
		/*
		 * 3. Linking the buttons to the ActionListener
		 */
		sendButton.addActionListener(this);			
		callButton.addActionListener(this);	
		hangupButton.addActionListener(this);

		
	}
	
	/**
	 * The main method of the application. It continuously listens for
	 * new messages.
	 */
	public static void main(String[] args){
	
		/*
		 * 1. Create the app's window
		 */
		App app = new App("Tenekedaki me sxoini");  																		  
		app.setSize(500,250);				  
		app.setVisible(true);				  

		/*
		 * 2. 
		 */
		new Thread (()-> {    
				try {
					
					byte[] buffer = new byte[1024];
		do{		
			
			    DatagramPacket receivePack= new DatagramPacket(buffer,buffer.length);
			    socket.receive(receivePack);
			    
			    String message= new String(receivePack.getData(),0,receivePack.getLength());
			    
			    textArea.append("Peer: " + message + "\n"); 
			
		}while(true);
					
					
				}catch (Exception e) {
			        e.printStackTrace();
	            }
			}).start();
		
		}
	
	/**
	 * The method that corresponds to the Action Listener. Whenever an action is performed
	 * (i.e., one of the buttons is clicked) this method is executed. 
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		

		if (e.getSource() == sendButton){
           String message = inputTextField.getText();
			
			if (!message.isEmpty()) {
			 new Thread(()-> {
				try {
				
				byte[] datatosend =message.getBytes();
				
				DatagramPacket sendPack = new DatagramPacket(
						datatosend,
						datatosend.length,
						InetAddress.getByName(peerIP),
						peerPort);
				
				socket.send(sendPack);
				
				textArea.append("Me:" + message + "\n"); // printing the message on the text area ,as long it there is no issue above
				inputTextField.setText("");
				
				}catch(Exception ex) {
	                ex.printStackTrace();
	            }
			
				
			 }).start();
			
		
			}
		}else if(e.getSource() == callButton){
			
			
		        try {
		            socketaudio = new DatagramSocket(audio);
		        } catch (SocketException ex) {
		             
		        }
		    
			
			textArea.append("Starting VOIP call..." + "\n");
		
			new Thread(() -> {
			    try {
			      
			        AudioFormat format = new AudioFormat(8000.0f, 8, 1, true, true); // Mono, 8-bit, 8 kHz
			        microphone = AudioSystem.getTargetDataLine(format);
			        microphone.open(format);
			        microphone.start();

			        byte[] buffer = new byte[512];

			        
			        while (!socketaudio.isClosed()&&(microphone.isOpen())) {
			            int bytesRead = microphone.read(buffer, 0, buffer.length); 
			            DatagramPacket packet = new DatagramPacket(buffer, bytesRead, InetAddress.getByName(peerIP), audioPeer);
			            socketaudio.send(packet); 
			        }
			      
			    } catch (Exception ex) {
			        
			    }
			}).start();
		
			new Thread(() -> {
			    try {
			       
			        AudioFormat format = new AudioFormat(8000.0f, 8, 1, true, true); // Mono, 8-bit, 8 kHz
			        speakers = AudioSystem.getSourceDataLine(format);
			        speakers.open(format);
			        speakers.start();

			        byte[] buffer = new byte[512];
			

			
			        while (!socketaudio.isClosed()&&(speakers.isOpen())) {
			        	
			            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
			            socketaudio.receive(packet); 
			            speakers.write(packet.getData(), 0, packet.getLength()); 
			        }
			       
			    } catch (Exception ex) {
			        
			    }
			}).start();
			
			
		}else if(e.getSource() == hangupButton){
			textArea.append("Call ended" +"\n");
		    microphone.close();
			speakers.close();
			try {
			    socketaudio.close();
			}catch(Exception p) {
			
			}
		}
			
		

		
			

	}

	/**
	 * These methods have to do with the GUI. You can use them if you wish to define
	 * what the program should do in specific scenarios (e.g., when closing the 
	 * window).
	 */
	@Override
	public void windowActivated(WindowEvent e) {
		// TODO Auto-generated method stub	
	}

	@Override
	public void windowClosed(WindowEvent e) {
		// TODO Auto-generated method stub	
	}

	@Override
	public void windowClosing(WindowEvent e) {
		// TODO Auto-generated method stub
		dispose();
        	System.exit(0);
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
		// TODO Auto-generated method stub	
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
		// TODO Auto-generated method stub	
	}

	@Override
	public void windowIconified(WindowEvent e) {
		// TODO Auto-generated method stub	
	}

	@Override
	public void windowOpened(WindowEvent e) {
		// TODO Auto-generated method stub	
	}
}
