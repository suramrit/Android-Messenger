package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */


public class GroupMessengerActivity extends Activity {

    static final String[] REMOTE_PORTS = {"11108","11112","11116","11120","11124"};
    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";
    static String order_delivery = "0";
    static final int SERVER_PORT = 10000;
    static int deliverable_count =0;
    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    // one list for sent messages.
    static HashMap<String, ArrayList<String>> sent_msgs = new HashMap<String,ArrayList<String>>();
    //one list for sent proposals
    static HashMap<String,String> sent_proposals = new HashMap<String, String>();
    //one list for deliverable
    static TreeMap<Float, String> deliverable = new TreeMap<Float, String>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);
        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        Log.e(TAG,"My PORT is: " + myPort);
        final int my_id_int = (Integer.parseInt(myPort) - 11108) / 4;
        final String my_id = Integer.toString(my_id_int);
        try {

            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,serverSocket);
        }
        catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }
        findViewById(R.id.button4).setOnClickListener(new View.OnClickListener() {
            TextView tv = (TextView) findViewById(R.id.textView1);
            EditText et = (EditText) findViewById(R.id.editText1);

            @Override
            public void onClick(View v) {
                //NOTE: parts copied as-is from PA1
                String msg = et.getText().toString(); // + "\n";
                //tv.setText(et.getText().toString());
                tv.append("\t" + msg + "\n");
                et.setText("");
                //getting correct proposals according to delivered proposals + deliverable messages...
                String my_proposal = get_proposal(my_id);
                Log.e(TAG, "my original proposal:" + my_proposal + "..");
                sent_list_insert(msg, my_proposal);
                Log.e(TAG, "msg to send is:" + msg + "..");
                Log.e(TAG, "my id to send is:" + my_id + "..");

                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort,
                        "11108", my_id, my_proposal);
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort,
                        "11112", my_id, my_proposal);
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort,
                        "11116", my_id, my_proposal);
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort,
                        "11120", my_id, my_proposal);
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort,
                        "11124", my_id, my_proposal);
            }
        });

        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */

        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));
        
        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */
    }
    public String[] get_my_id(){
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        Log.e(TAG, "My PORT is: " + myPort);
        final int my_id_int = (Integer.parseInt(myPort) - 11108) / 4;
        final String my_id = Integer.toString(my_id_int);
        String[] port_info = {myPort,my_id};
        return port_info;
    }
    public void setDeliverable(String msg, String prop){

        Log.e(TAG,"Inserting into deliverable:"+msg+"..."+prop+
            "deliverable count:"+deliverable_count+"..");

        deliverable_count++;
        deliverable.put(Float.parseFloat(prop), msg);

    }
    public String[] get_deliverable(){
        // Sort: treeMap<String--Msg, String--Prop> deliverable
        // return highest priority msg... and remove from deliverable list
        // return only decimal part and the msg...
        float min = 999999;
        String msg = new String();
        float prev_key;
        int peek =0; //look at the next 10 msgs for the min..
        for (Float key : deliverable.keySet()) {
                if (key < min ) {
                min = key;
                msg = deliverable.get(key);
                //break; //2nd implementation
                }
            peek++;
            if(peek == 10)
                break;
        }

        int order = Integer.parseInt(order_delivery);
        String str_order = Integer.toString(order);
        Log.e(TAG,"delivering msg:"+msg+"..."+"delivering order:"+str_order+"...");
        String[] deliver = {str_order,msg};
        order++;
        order_delivery = Integer.toString(order);

        deliverable.remove(min);
        return deliver;
    }
    public void  sent_list_insert(String msg, String proposal){
        ArrayList<String> vals = new ArrayList<String>();
        vals.add(0, proposal);
        vals.add(1, "5");
        //add updated values on getting back a proposal... when all proposals received pick highest and move to deliverable..
        sent_msgs.put(msg, vals);
        sent_proposals.put(proposal, proposal);
        System.out.println(sent_msgs.size());
        return ;
    }
    public void recast_final(String msg){
        String[] my_info = get_my_id();
        String final_prop = Float.toString(get_high_proposal(msg));
        Log.e(TAG,"Final prop to recast is:"+final_prop+"...");
        new ClientTask2().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, my_info[0],
                "11108", my_info[1], final_prop);
        new ClientTask2().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, my_info[0],
                "11112",my_info[1], final_prop);
        new ClientTask2().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, my_info[0],
                "11116",my_info[1], final_prop);
        new ClientTask2().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, my_info[0],
                "11120",my_info[1], final_prop);
        new ClientTask2().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, my_info[0],
                "11124",my_info[1], final_prop);
    }
    public  float get_high_proposal(String msg){
        ArrayList<String> vals = sent_msgs.get(msg);
        float prop = Float.parseFloat(vals.get(0));
        return prop;
    }
    public int sent_list_update(String msg, String new_prop){
        ArrayList<String> vals = sent_msgs.get(msg);
        int count = Integer.parseInt(vals.get(1));
        System.out.println("Count vlaue updated from :"+count);
        count--;
        System.out.println("..to"+count+"...");
        float prop = Float.parseFloat(vals.get(0));
        if(Float.parseFloat(new_prop) >= prop){

            vals.add(0, new_prop);
        }
            vals.add(1,Integer.toString(count));
        return count;

    }
    //needs to be synchronised
    public String get_proposal(String my_id){
        //get highest proposal from deliverable list...
        float deliverable_proposal;
        float sent_proposal;
        if(deliverable.isEmpty())
        {
           deliverable_proposal =  Float.parseFloat("0"+"."+my_id);
        }
        else{
            float max = 0;

            for(Float key : deliverable.keySet())
            {
                Log.e(TAG,"value from deliverable list:"+(deliverable.get(key)+
                        "..msg:"+key));
                //if (Float.parseFloat(deliverable.get(key)) > max )
                    if (key > max )
                    //max = Float.parseFloat(deliverable.get(key));
                      max = key;
            }
            deliverable_proposal = max;
        }
        //get highest proposal from proposal list...
        if(sent_proposals.isEmpty())
        {
            sent_proposal =  Float.parseFloat("0"+"."+my_id);
        }
        else{
            float max = 0;
            for(String value : sent_proposals.keySet())
            {
                if (Float.parseFloat(value) > max )
                    max = Float.parseFloat(value);
            }
            sent_proposal = max;
        }

        float prop = ((deliverable_proposal > sent_proposal) ? deliverable_proposal : sent_proposal);
        //val: whole_part+1.id
        Log.e(TAG,"max prop sent till now is:"+prop);
        if(!(sent_proposals.isEmpty())|!(deliverable.isEmpty())) {
            int val = (int)prop;
            val = val + 1;
            Log.e(TAG,"My proposed val:" + val + "..");
            sent_proposals.put(Integer.toString(val) + "." + my_id, Integer.toString(val) + "." + my_id);
            return Integer.toString(val) + "." + my_id;
        }
        else {
            Log.e(TAG,"My proposed val:" + prop + "..");
            sent_proposals.put(Float.toString(prop),Float.toString(prop));
            return Float.toString(prop);}
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            int key_val =0;
            String key;
            try {
                ServerSocket serverSocket = sockets[0];
                String[] my_port = get_my_id();
                System.out.println("MY ID@(SERVER):"+my_port[1]+"..");
                while(true) {

                        Socket clientSocket = serverSocket.accept();

                        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true); //trying 2-way
                        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                        String userInput = new String();
                        String their_id = new String();
                        String is_final = new String();
                        String their_prop = new String();

                        userInput = in.readLine();
                        their_id = in.readLine();
                        is_final = in.readLine();
                        their_prop = in.readLine();
                        Log.e(TAG, "Others Id Recieved@Server is:" + their_id + "..");
                        Log.e(TAG, "Prop Recieved@Server is:" + their_prop + "..");
                        Log.e(TAG, "Bool Value@Server is:" + is_final + "..");
                        //if not final....get highest proposal... send proposal back...
                        //depend on sender to determine the highest proposal for themselves
                        int f_prop = Integer.parseInt(is_final);
                        if (f_prop == 0) {
                            String my_new_prop;
                            if (my_port[1].equals(their_id)) {
                                my_new_prop = their_prop;
                            } else {
                                my_new_prop = get_proposal(my_port[1]);
                            }

                            Log.e(TAG, "Sending Proposal:" + my_new_prop + "..");
                            out.println(my_new_prop);
                        }
                        //if final.. move msg to deliverable...
                        else {
                            String final_prop = new String();
                            final_prop = their_prop;
                            Log.e(TAG, "Final prop@Server:" + final_prop + "..");
                            //after gettin back the final propoasl move the message to deliverable..
                            setDeliverable(userInput, final_prop);
                            //values getting inserted correctly across AVDs till here...
                            //move from deliverable to Provider for Delivery...
                            Uri.Builder uriBuilder = new Uri.Builder();
                            uriBuilder.authority("edu.buffalo.cse.cse486586.groupmessenger2.provider");
                            uriBuilder.scheme("content");
                            Uri mUri = uriBuilder.build();
                            ContentValues mContentValues = new ContentValues();
                            ContentResolver mContentResolver = getContentResolver();
                            if (deliverable.size() >= 5) {
                                String[] deliverable_pair = get_deliverable();
                                mContentValues.put("key", deliverable_pair[0]);
                                mContentValues.put("value", deliverable_pair[1]);
                                mContentResolver.insert(mUri, mContentValues);
                            }
                            if (deliverable_count >= 25) {

                                Log.e(TAG, "sending last...deliverable size:" + deliverable.size());
                                for (int i = 0; i <= (deliverable.size() + 2); i++) {
                                    String[] deliverable_pair = get_deliverable();
                                    mContentValues.put("key", deliverable_pair[0]);
                                    mContentValues.put("value", deliverable_pair[1]);
                                    mContentResolver.insert(mUri, mContentValues);
                                }
                            }

                            publishProgress(userInput, final_prop);
                        }

                        //update to send deliverable msgs... using PublishProgress


                        // after getting a proposal... look getproposal and send proposal back..
                        // wait for confimration
                        // after getting confirmation... move to deliverable adn write to thcontent provider..


                }

            }
            catch (IOException e) {

                Log.e(TAG, "try again");
                return null;
            }


            //return null;
        }

        protected void onProgressUpdate(String...strings) {
            // while(true) {

            //Uri.Builder uriBuilder = new Uri.Builder();
           // uriBuilder.authority("edu.buffalo.cse.cse486586.groupmessenger2.provider");
           // uriBuilder.scheme("content");
           // Uri mUri = uriBuilder.build();
           // ContentValues mContentValues=new ContentValues();
           // ContentResolver mContentResolver = getContentResolver();
            String msg = strings[0].trim();
            String proposal = strings[1].trim();
            //Log.e(TAG,"Server Recieved1: " + strReceived);
            //Log.e(TAG,"Server recvd Key:"+strings[1]);
            TextView remoteTextView = (TextView) findViewById(R.id.textView1);
            remoteTextView.append(msg + "\t\n");
            TextView localTextView = (TextView) findViewById(R.id.editText1);
            //localTextView.append("\n");

            String filename = "SimpleMessengerOutput";
            String string = msg + "\n";//.trim();//strReceived;//+ "\n";
            //Log.e(TAG,"Server Recieved2: " + string+"..");
            //key-- order value-- msg
           // String[] deliverable_pair = get_deliverable();
           // mContentValues.put("key", deliverable_pair[0]);
           // mContentValues.put("value", deliverable_pair[1]);
           // mContentResolver.insert(mUri, mContentValues);

            // }

        }
    }

    //implementing client task..
    private class ClientTask extends AsyncTask<String, Void, Void>{

        @Override
        protected Void doInBackground(String... msgs){
            try {
                String remotePort = msgs[2];
                Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                        Integer.parseInt(remotePort));

                String msgToSend = msgs[0];
                String my_id = msgs[3];
                String my_proposal = msgs[4];
                String is_final = "0";
                int proposal_count;
                //NOTE: check whether '/n' appended is correct implementation
                PrintWriter out =
                        new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out.println(msgToSend);
                out.println(my_id);
                //send final bool
                out.println(is_final);
                out.println(my_proposal);

                //wait for their proposal.. proposal not updated until their proposal recieved...
              //  System.out.println("stuck here?");
                String read = in.readLine();
                System.out.println("Read back from @Server:" + read + "...");
                //update the sent messages list..
                //taking the highest returned proposal..
                proposal_count = sent_list_update(msgToSend,read);
                //if all confirmations rcvd..
                socket.close();
                if(proposal_count == 0){
                    //multicast message again...
                    //This shd be in a multicast option..
                    recast_final(msgToSend);
                }

                //move to deliverable list...
                //once a move to deliverable is made... reorder and send one at front..
                //socket.close();
            }
            catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");
            } catch (IOException e) {
                Log.e(TAG, "ClientTask socket IOException");
            }



            return null;

        }


    }

    //implementing client task for recasting the final proposal....
    private class ClientTask2 extends AsyncTask<String, Void, Void>{

        @Override
        protected Void doInBackground(String... msgs){
            try {
                String remotePort = msgs[2];
                Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                        Integer.parseInt(remotePort));

                String msgToSend = msgs[0];
                String my_id = msgs[3];
                String is_final = "1";
                String final_proposal = msgs[4];
                int proposal_count;
                //NOTE: check whether '/n' appended is correct implementation
                PrintWriter out =
                        new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out.println(msgToSend);
                out.println(my_id);
                out.println(is_final);
                out.println(final_proposal);
                socket.close();
            }
            catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask2 UnknownHostException");
            } catch (IOException e) {
                Log.e(TAG, "ClientTask2 socket IOException");
            }



            return null;

        }


    }
}
