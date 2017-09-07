// SUBMISSION BY:
// Name: Thomas Lim Jun Wei
// Matric No.: U1521407L
// Lab Group: SSP4

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
   private boolean no_nak = true;

   // The following method checks the circular condition of the frame numbers:
   public static boolean between (int x,int y,int z){
       return ((x <= y) && (y < z)) || ((z < x) && (x <= y)) || ((y < z) && (z < x));
   }
 
   
   // java method to send frame
   private void send_frame(int frame_type, int frame_no, int frame_expected, Packet buffer[]){
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
	   s.ack = frame_expected + MAX_SEQ;
	   s.ack = s.ack % (MAX_SEQ + 1);
	   
	   if (frame_type == PFrame.NAK)
		   no_nak = false; //one NAK per frame
		   
	   // send frame from physical layer
	   to_physical_layer(s);
	   if(frame_type == PFrame.DATA)
		   start_timer(frame_no);
	  
	   //  not required for seperate ACK frame
	   stop_ack_timer();
   }
   
   public void protocol6() {
        init();
        
        // lower edge of sender window
        int ack_expected;
        // upper edge of sender window
        int next_frame_to_send;
        //lower edge of receiver window
        int frame_expected;
        // upper edge of receiver window
        int too_far;
        int index;
        
        // track frames arrival
        boolean received[] = new boolean[NR_BUFS];
        
        PFrame temp_frame = new PFrame();
        
        // init network layer
        enable_network_layer(NR_BUFS);
        
        // setup here
        ack_expected = 0;
        next_frame_to_send = 0;
        frame_expected =0;
        too_far = NR_BUFS;
        index = 0;
        for (int i=0; i <NR_BUFS;i++)
        	received[i] = false;
        
	while(true) {	
         wait_for_event(event);
	   switch(event.type) {
	      case (PEvent.NETWORK_LAYER_READY):
	    	// sending a frame if network layer is ready
	    	from_network_layer(out_buf[next_frame_to_send % NR_BUFS]);
	      	send_frame(PFrame.DATA, next_frame_to_send, frame_expected, out_buf);
	      	// increase pointer of upper edge of window
	      	next_frame_to_send = inc(next_frame_to_send);
	      	break; 
	      case (PEvent.FRAME_ARRIVAL ):
	    	// receive a frame 
	    	from_physical_layer(temp_frame);
	      	
	      	if (temp_frame.kind == PFrame.DATA){
	      		// correct frame arrived
	      		
	      		if ((temp_frame.seq != frame_expected) && no_nak)
	      			send_frame(PFrame.NAK, 0, frame_expected, out_buf);
	      		else
	      			start_ack_timer();
	      		
	      		// check if incoming frame is between the sliding window
	      		// if it has not been prev recieved, allow frame to accept in any order of arrival
	      		if (between(frame_expected, temp_frame.seq, too_far) && received[temp_frame.seq % NR_BUFS] == false){
	      			
	      			// indicate that buffer is full
	      			received[temp_frame.seq % NR_BUFS] = true;
	      			// insert data into buffer
	      			in_buf[temp_frame.seq % NR_BUFS] = temp_frame.info;
	      			
	      			while(received[frame_expected % NR_BUFS]){
	      				// pass frame from Physical > network, then advance window edge
	      				to_network_layer(in_buf[frame_expected % NR_BUFS]);
	      				no_nak = true;
	      				// mark undamaged frame as received
	      				received[frame_expected % NR_BUFS] = false;
	      				frame_expected = inc(frame_expected);
	      				too_far = inc(too_far);
	      				// start ack timer
	      				start_ack_timer();
	      			}
	      		}
	      		
	      		// if NAK frame arrived, check frame is between expected frames of SW 
	      		// resent data of the frame which NAK feedback
	      		if (temp_frame.kind == PFrame.NAK && between(ack_expected, ((temp_frame.ack + 1) % (MAX_SEQ + 1)),next_frame_to_send)) {
                    send_frame(PFrame.DATA, ((temp_frame.ack + 1) % (MAX_SEQ + 1)), frame_expected, out_buf);
	      		}
	      		
	      		while(between(ack_expected, temp_frame.ack, next_frame_to_send)){
	      			// if frame is undamaged and complete frame is delivered, prep next step
	      			stop_timer(ack_expected % NR_BUFS);
	      			ack_expected = inc(ack_expected);
	      			// free buffer slot
	      			enable_network_layer(1);
	      		}
	      	}
	      	
		   break;	   
	      case (PEvent.CKSUM_ERR):
	    	  if (no_nak){
	    		  // damaged frame arrived
	    		  send_frame(PFrame.NAK, 0, frame_expected, out_buf);
	    	  }
      	      break;  
	      case (PEvent.TIMEOUT): 
	    	  // timer expired for frame, request resend
	    	  send_frame(PFrame.DATA, oldest_frame, frame_expected, out_buf);
	    	  break; 
	      case (PEvent.ACK_TIMEOUT): 
	    	  // if ack timer expired, resend the ack
	    	  send_frame(PFrame.ACK, 0, frame_expected, out_buf);
	    	  break; 
            default: 
		   System.out.println("SWP: undefined event type = " 
                                       + event.type); 
		   System.out.flush();
	   }
      }      
   }

 /* Note: when start_timer() and stop_timer() are called, 
    the "seq" parameter must be the sequence number, rather 
    than the index of the timer array, 
    of the frame associated with this timer, 
   */
   
   Timer frame_timer[] = new Timer[NR_BUFS];
   Timer ack_timer;
   
   public static int inc(int numToInc){
	   return ((numToInc +1) % (MAX_SEQ +1));
   }
 
   private void start_timer(int seq) {
	   // stop exisiting timer - aka hard reset
	   stop_timer(seq);
	   frame_timer[seq % NR_BUFS] = new Timer();
       frame_timer[seq % NR_BUFS].schedule(new ReTask(swe, seq), 200);
   }

   private void stop_timer(int seq) {
	   if (frame_timer[seq % NR_BUFS] != null)
		   frame_timer[seq % NR_BUFS].cancel();
   }

   private void start_ack_timer( ) {
	   stop_ack_timer();
       ack_timer = new Timer();
       ack_timer.schedule(new AckTask(swe), 100);
   }

   private void stop_ack_timer() {
	  if (ack_timer != null) 
		  ack_timer.cancel();
   }
   
   // implement retransmission here
   class ReTask extends TimerTask{
	   private SWE swe = null;
	   public int seqNo;
	   
	   public ReTask(SWE swe, int seqNo){
		   this.swe = swe;
		   this.seqNo = seqNo;
	   }
	   
	   public void run(){
		   stop_timer(seqNo);
		   swe.generate_timeout_event(seqNo);
	   }
   }
   
   // implent ACK timer here
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

}//End of class

/* Note: In class SWE, the following two public methods are available:
   . generate_acktimeout_event() and
   . generate_timeout_event(seqnr).

   To call these two methods (for implementing timers),
   the "swe" object should be referred as follows:
     swe.generate_acktimeout_event(), or
     swe.generate_timeout_event(seqnr).
*/


