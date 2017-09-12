//import javax.management.timer.Timer;
import java.util.Timer;
import java.util.TimerTask;

/*===============================================================*
 *  File: SWP.java                                               *
 *                                                               *
 *  This class implements the sliding window protocol            *
 *  Used by VMach class					         *
 *  Uses the following classes: SWE, Packet, PFrame, PEvent,     *
 *                                                               *
 *  Author: Professor SUN Chengzheng                             *
 *          School of Computer Engineering                       *
 *          Nanyang Technological University                     *
 *          Singapore 639798                                     *
 *===============================================================*/

public class SWP {

/*========================================================================*
 the following are provided, do not change them!!
 *========================================================================*/
   //the following are protocol constants.
   public static final int MAX_SEQ = 7; 
   public static final int NR_BUFS = (MAX_SEQ + 1)/2;

   // the following are protocol variables
   private int oldest_frame = 0;
   private PEvent event = new PEvent();  
   private Packet out_buf[] = new Packet[NR_BUFS];
   
   // added new protocol variable for incoming buffer
   private Packet in_buf[] = new Packet[NR_BUFS];

   //the following are used for simulation purpose only
   private SWE swe = null;
   private String sid = null;  

   //Constructor
   public SWP(SWE sw, String s){
      swe = sw;
      sid = s;
   }

   //the following methods are all protocol related
   private void init(){
      for (int i = 0; i < NR_BUFS; i++){
	   out_buf[i] = new Packet();
      }
   }

   private void wait_for_event(PEvent e){
      swe.wait_for_event(e); //may be blocked
      oldest_frame = e.seq;  //set timeout frame seq
   }

   private void enable_network_layer(int nr_of_bufs) {
   //network layer is permitted to send if credit is available
	swe.grant_credit(nr_of_bufs);
   }

   private void from_network_layer(Packet p) {
      swe.from_network_layer(p);
   }

   private void to_network_layer(Packet packet) {
	swe.to_network_layer(packet);
   }

   private void to_physical_layer(PFrame fm)  {
      System.out.println("SWP: Sending frame: seq = " + fm.seq + 
			    " ack = " + fm.ack + " kind = " + 
			    PFrame.KIND[fm.kind] + " info = " + fm.info.data );
      System.out.flush();
      swe.to_physical_layer(fm);
   }

   private void from_physical_layer(PFrame fm) {
      PFrame fm1 = swe.from_physical_layer(); 
	fm.kind = fm1.kind;
	fm.seq = fm1.seq; 
	fm.ack = fm1.ack;
	fm.info = fm1.info;
   }


/*===========================================================================*
 	implement your Protocol Variables and Methods below: 
 *==========================================================================*/
   /*
    * Author: Thomas Lim Jun Wei
    * Matric No.: U1521407L
    * Lab Group: SSP5
    */
   
   // implement retransmission here
   class Retransmission extends TimerTask{
	   private SWE swe = null;
	   public int seqNo;
	   
	   public Retransmission(SWE swe, int seqNo){
		   this.swe = swe;
		   this.seqNo = seqNo;
	   }
	   
	   public void run(){
		   stop_timer(seqNo);
		   swe.generate_timeout_event(seqNo);
	   }
   }
   
   // implement ACK timer here
   class AckTask extends TimerTask{
	   private SWE swe = null;
	   
	   public AckTask(SWE swe){
		   this.swe = swe;
	   }
	   
	   public void run(){
		   stop_ack_timer();
		   swe.generate_acktimeout_event();
	   }
   }
   
   // additional variable declaration
   private boolean no_nak = true;
   Timer frame_timer[] = new Timer[NR_BUFS];
   Timer ack_timer;
   
   // Checks the circular condition of the frame numbers:
   public static boolean between (int x,int y,int z){
       return ((x <= y) && (y < z)) || ((z < x) && (x <= y)) || ((y < z) && (z < x));
   }
 
   
   // Method to transmit frame
   private void send_frame(int frame_type, int frame_no, int receiver_lowerEdge, Packet buffer[]){
	   //temp frame
	   PFrame s = new PFrame();
	   
	   // frame_type is defined to be 3 different kind
	   // 1) DATA
	   // 2) ACK
	   // 3) NAK
	   s.kind = frame_type;
	   if (frame_type == PFrame.DATA)
		   s.info = buffer[frame_no % NR_BUFS];
	   
	   // for meaningful data frames
	   s.seq = frame_no;
	   s.ack = (receiver_lowerEdge + MAX_SEQ) % (MAX_SEQ + 1);
	   //s.ack = s.ack % (MAX_SEQ + 1);
	   
	   //one NAK per frame
	   if (frame_type == PFrame.NAK)
		   no_nak = false; 
		   
	   // send frame from physical layer
	   to_physical_layer(s);
	   if(frame_type == PFrame.DATA)
		   start_timer(frame_no);
	  
	   //  not required for seperate ACK frame
	   stop_ack_timer();
   }
   
   public void protocol6() {
    init();
        
    /* protocol setup here*/
    PFrame frame_temporary = new PFrame();

    // lower edge of sender window
    int sender_lowerEdge = 0;
    // upper edge of sender window
    int sender_upperEdge = 0;
    //lower edge of receiver window
    int receiver_lowerEdge = 0;
    // upper edge of receiver window
    int receiver_upperEdge = NR_BUFS;
    // index position of buffer
    int index = 0;
    
    // track frames arrival
    boolean received[] = new boolean[NR_BUFS];
    for (int i = 0; i <NR_BUFS;i++)
    	received[i] = false;
           
    // init network layer
    enable_network_layer(NR_BUFS);
   
	while(true) {
		// wait for an incoming/outgoing frame
		wait_for_event(event);
		
		switch(event.type) {
	    	case (PEvent.NETWORK_LAYER_READY):
		    	// sending a frame if network layer is ready
		    	from_network_layer(out_buf[sender_upperEdge % NR_BUFS]);
	    	
	    		// transmit frame here
		      	send_frame(PFrame.DATA, sender_upperEdge, receiver_lowerEdge, out_buf);
		      	
		      	// increase pointer of upper edge of window
		      	sender_upperEdge = inc(sender_upperEdge);
		      	break; 
	    	case (PEvent.FRAME_ARRIVAL ):
		    	// receive a frame 
		    	from_physical_layer(frame_temporary);
		      	
		      	if (frame_temporary.kind == PFrame.DATA){
		      		// correct frame arrived
		      		
		      		// if there is no acknowledgement and frame sequence number is not the expected frame at receiver side
		      		// send nak frame with the expected frame to arrive for retransmission
		      		if ((frame_temporary.seq != receiver_lowerEdge) && no_nak)
		      			send_frame(PFrame.NAK, 0, receiver_lowerEdge, out_buf);
		      		else
		      			start_ack_timer();
		      		
		      		// check if incoming frame is between the sliding window
		      		// if it has not been prev recieved, allow frame to accept in any order of arrival
		      		if (between(receiver_lowerEdge, frame_temporary.seq, receiver_upperEdge) && received[frame_temporary.seq % NR_BUFS] == false){
		      			
		      			// indicate that buffer is full
		      			received[frame_temporary.seq % NR_BUFS] = true;
		      			
		      			// insert data into buffer
		      			in_buf[frame_temporary.seq % NR_BUFS] = frame_temporary.info;
		      			
		      			while(received[receiver_lowerEdge % NR_BUFS]){
		      				// pass frame from Physical > network, then advance window edge
		      				to_network_layer(in_buf[receiver_lowerEdge % NR_BUFS]);
		      				no_nak = true;
		      				
		      				// mark undamaged frame as received
		      				received[receiver_lowerEdge % NR_BUFS] = false;
		      				receiver_lowerEdge = inc(receiver_lowerEdge);
		      				receiver_upperEdge = inc(receiver_upperEdge);
		      				
		      				// start ack timer
		      				start_ack_timer();
		      			}
		      		}	
		      	}
		      	
		    	// if NAK frame arrived, check frame is between expected frames of SW 
	      		if (frame_temporary.kind == PFrame.NAK && between(sender_lowerEdge, ((frame_temporary.ack + 1) % (MAX_SEQ + 1)),sender_upperEdge))
		      		// resent data of the frame which NAK received by sender
                    send_frame(PFrame.DATA, ((frame_temporary.ack + 1) % (MAX_SEQ + 1)), receiver_lowerEdge, out_buf);
	      		
		      	while(between(sender_lowerEdge, frame_temporary.ack, sender_upperEdge)){
	      			// if frame is undamaged and complete frame is delivered, prep next step
	      			stop_timer(sender_lowerEdge % NR_BUFS);
	      			
	      			// move lower edge of sender window
	      			sender_lowerEdge = inc(sender_lowerEdge);
	      			
	      			// free buffer slot
	      			enable_network_layer(1);
	      		}
		      	
		      	break;	   
	    	case (PEvent.CKSUM_ERR):
		    	  if (no_nak){
		    		  // damaged frame arrived
		    		  send_frame(PFrame.NAK, 0, receiver_lowerEdge, out_buf);
		    	  }
	      	      break;  
	    	case (PEvent.TIMEOUT): 
		    	  // timer expired for frame, request resend
		    	  send_frame(PFrame.DATA, oldest_frame, receiver_lowerEdge, out_buf);
		    	  break; 
	    	case (PEvent.ACK_TIMEOUT): 
		    	  // if ack timer expired, resend the ack
		    	  send_frame(PFrame.ACK, 0, receiver_lowerEdge, out_buf);
		    	  break; 
            default: 
            	System.out.println("SWP: undefined event type = " 
                                       + event.type); 
            	System.out.flush();
            	break;
	   }
      }      
   }

 /* Note: when start_timer() and stop_timer() are called, 
    the "seq" parameter must be the sequence number, rather 
    than the index of the timer array, 
    of the frame associated with this timer, 
   */
     
   // method to increase a index
   public static int inc(int numToInc){
	   return ((numToInc + 1) % (MAX_SEQ +1));
   }
 
   private void start_timer(int seq) {
	   // stop exisiting timer - aka hard reset
	   stop_timer(seq);
	   frame_timer[seq % NR_BUFS] = new Timer();
	   
	   // schedule the task for exe after 200ms
       frame_timer[seq % NR_BUFS].schedule(new Retransmission(swe, seq), 200);
   }

   private void stop_timer(int seq) {
	   if (frame_timer[seq % NR_BUFS] != null)
		   frame_timer[seq % NR_BUFS].cancel();
   }

   private void start_ack_timer( ) {
	   stop_ack_timer();
	   
	   // start new timer for sending acknowledgement frame
       ack_timer = new Timer();
       ack_timer.schedule(new AckTask(swe), 100);
   }

   private void stop_ack_timer() {
	  if (ack_timer != null) 
		  ack_timer.cancel();
   }

}
//End of class

/* Note: In class SWE, the following two public methods are available:
   . generate_acktimeout_event() and
   . generate_timeout_event(seqnr).
   To call these two methods (for implementing timers),
   the "swe" object should be referred as follows:
     swe.generate_acktimeout_event(), or
     swe.generate_timeout_event(seqnr).
*/