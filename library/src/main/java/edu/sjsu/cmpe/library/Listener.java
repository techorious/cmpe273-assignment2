package edu.sjsu.cmpe.library;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.StringTokenizer;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.fusesource.stomp.jms.StompJmsConnectionFactory;
import org.fusesource.stomp.jms.StompJmsDestination;
import org.fusesource.stomp.jms.message.StompJmsMessage;

import edu.sjsu.cmpe.library.config.LibraryServiceConfiguration;
import edu.sjsu.cmpe.library.domain.Book;
import edu.sjsu.cmpe.library.domain.Book.Status;
import edu.sjsu.cmpe.library.repository.BookRepositoryInterface;

public class Listener {

	private final LibraryServiceConfiguration configuration;
    private BookRepositoryInterface bookRepository;
    private String portNumber;
	private String apolloUser;
    private String apolloPassword;
    private String apolloHost;
    private int apolloPort;
    private String stompTopicName;
    private long isbn[]= new long[3];  
    private String title[] = new String[3];
    private String category[] = new String[3];
    private String coverImage[] = new String[3];
    Book bookInRepository = null;
    long bookIsbn;
    private int i = 0;
    

    public Listener(LibraryServiceConfiguration config, BookRepositoryInterface bookRepository) {
   	 this.configuration = config;
   	 this.bookRepository = bookRepository;
   	 this.apolloUser = configuration.getApolloUser();
   	 this.apolloPassword = configuration.getApolloPassword();
   	 this.apolloHost = configuration.getApolloHost();
   	 this.apolloPort = configuration.getApolloPort();
   	 this.stompTopicName = configuration.getStompTopicName();
   	 this.portNumber = ""+apolloPort;
    }

    public void listenerThread()
    {
   	 while(true)
   	 {
   		 try{
   			 String user = env("APOLLO_USER", apolloUser);
   			 String password = env("APOLLO_PASSWORD", apolloPassword);
   			 String host = env("APOLLO_HOST", apolloHost);
   			 int apolloPortNumber = Integer.parseInt(env("APOLLO_PORT", portNumber));
   			 //String target = stompTopicName;
   			 StompJmsConnectionFactory factoryConnection = new StompJmsConnectionFactory();
   			factoryConnection.setBrokerURI("tcp://" + host + ":" + apolloPortNumber);

   			 Connection connection = factoryConnection.createConnection(user, password);
   			 connection.start();
   			 Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
   			 Destination destination = new StompJmsDestination(stompTopicName);

   			 MessageConsumer consumer = session.createConsumer(destination);
   			 System.currentTimeMillis();
   			 while(true) {
   				 Message message = consumer.receive();		
   				 if( message instanceof TextMessage ) {		
   					 String messageBody = ((TextMessage) message).getText();	
   					 if( "SHUTDOWN".equals(messageBody) ) {
   						 break;
   					 }
   					 
   					 StringTokenizer delimitor = new StringTokenizer(messageBody,":,\"");
   					 while(delimitor.hasMoreTokens())
   					 {
   						 isbn[i]= Long.parseLong(delimitor.nextToken());  
   						 title[i]=delimitor.nextToken();
   						 category[i] = delimitor.nextToken();
   						 coverImage[i] = delimitor.nextToken()+":"+delimitor.nextToken();
   						 i++;
   					 }

   					 System.out.println("Text Message");
   					 if(i == 3)
   						 break;
   				 } else if (message instanceof StompJmsMessage) {		
   					 StompJmsMessage stomopMessage = ((StompJmsMessage) message);	
   					 String body = stomopMessage.getFrame().contentAsString();
   					 if ("SHUTDOWN".equals(body)) {
   						 break;
   					 }
   					 
   				 } else {
   					 System.out.println("Illegal Message:"+message.getClass());	
   				 }

   			 }
   			 connection.close();
   		 }
   		 catch(JMSException e)
   		 {
   			 e.printStackTrace();
   		 }
   		 
   		 //Looking up for book in the bookRepository
   		 
   		 

   		 
   		 for(int i=0;i<3;i++)
   		 {
   			bookIsbn = isbn[i];			
   			 if(bookIsbn!=0)
   			 {
   				bookInRepository = bookRepository.getBookByISBN(bookIsbn);
   				 if(bookInRepository!=null)
   				 {
   					 if(bookInRepository.getStatus()==Status.lost);
   					 {
   						 //Printing the book details
   						 System.out.println("Isbn:" + bookInRepository.getIsbn() + "\nTitle:" + bookInRepository.getTitle() + "\nStatus:" + bookInRepository.getStatus());
   						 //Update the book status
   						 bookRepository.updateLostToAvailable(bookIsbn,Status.available);
   					 }
   				 }
   				 //Adding the book in the repository if the book does not exist
   				 else
   				 {
   					 Book newBook = new Book();
   					 newBook.setIsbn(isbn[i]);
   					 newBook.setTitle(title[i]);
   					 newBook.setCategory(category[i]);
   					 newBook.setStatus(Status.available);
   					 try {
   						 newBook.setCoverimage(new URL(coverImage[i]));
   					 } catch (MalformedURLException e) {
   						 e.printStackTrace();
   					 }

   					 bookRepository.insertNewBooks(bookIsbn, newBook);
   				 }
   			 }
   		 }
   		 try {
   			 Thread.sleep(10000);
   		 } catch (InterruptedException e) {
   			 e.printStackTrace();
   		 }
   	 }
    }

    private static String env(String key, String defaultValue)
    {
   	 String rc = System.getenv(key);
   	 if( rc== null )
   	 {
   		 return defaultValue;
   	 }
   	 return rc;
    }
}
