package edu.buffalo.cse.cse486586.groupmessenger;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

/**
 * References used:
 * http://stackoverflow.com/questions/2266827/when-to-use-comparable-and-comparator
 * http://stackoverflow.com/questions/1814095/sorting-an-arraylist-of-contacts-based-on-name
 * http://stackoverflow.com/questions/3718383/java-class-implements-comparable
 * http://stackoverflow.com/questions/7544606/sending-an-object-via-sockets-java
 * http://stackoverflow.com/questions/6700717/how-to-iterate-through-an-array-list-arrayindexoutofboundsexception
 *
 * @author cnettem
 */

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */

public class GroupMessengerActivity extends Activity {
	static final String TAG = GroupMessengerActivity.class.getSimpleName();
	static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";
    static final int SERVER_PORT = 10000;
    public static final String AUTHORITY = "edu.buffalo.cse.cse486586.groupmessenger.provider";
    public static final Uri CONTENT_URI = Uri.parse("content://"+AUTHORITY);
    public static final String KEY_FIELD = "key";
    public static final String VALUE_FIELD = "value";
    static int key = 0;

    static ArrayList<MessageData> queue = new ArrayList<MessageData>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        try {
            /*
             * Create a server socket as well as a thread (AsyncTask) that listens on the server
             * port.
             * 
             * AsyncTask is a simplified thread construct that Android provides. Please make sure
             * you know how it works by reading
             * http://developer.android.com/reference/android/os/AsyncTask.html
             */
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            serverSocket.setReuseAddress(true);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            /*
             * Log is a good way to debug your code. LogCat prints out all the messages that
             * Log class writes.
             * 
             * Please read http://developer.android.com/tools/debugging/debugging-projects.html
             * and http://developer.android.com/tools/debugging/debugging-log.html
             * for more information on debugging.
             */
            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }


        /*
         * TextView is used to display messages. 
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        
        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));
        
        /*
         * OnClickListener being registered for the "Send" button.
         * Message goes from the input box (EditText)
         * to other AVDs in a total-causal order.
         */
        final EditText editText = (EditText) findViewById(R.id.editText1);

        findViewById(R.id.button4).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = editText.getText().toString()+"\n";
                editText.setText("");
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,msg, myPort);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }
    
    /*
    * Server code that receives messages and passes them
    * to onProgressUpdate().
    */
    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            int counter = -1;
            
            /*
             * This while loop will remain running until the thread in which it runs
             * is interrupted. This allows continuous sending of messages.
             */
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    InputStream in = clientSocket.getInputStream();
                    MessageData incoming = null;
                    ObjectInputStream oin = new ObjectInputStream(in);

                    try {
						incoming = (MessageData) oin.readObject();
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					}
                    
                    if (incoming.sequence<0) {
                        incoming.sequence = key;
                        key += 1;
                        Socket socket = null;
                    
                        for (int port = Integer.parseInt(REMOTE_PORT0); port <= Integer.parseInt
                                (REMOTE_PORT4); port += 4) {
                            socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                    port);
                        	ObjectOutputStream outputStream = new ObjectOutputStream(
                        	        socket.getOutputStream());
                        	outputStream.writeObject(incoming);
                            socket.close();
                        }
                    }
                    
                    else {
                        queue.add(incoming);
                    
                        if (incoming.sequence == counter+1) {
                            Collections.sort(queue);
                            Iterator<MessageData> it = queue.iterator();
                        
                            while(it.hasNext())
                            {
                                ContentValues inputValues = new ContentValues();
                                inputValues.put(KEY_FIELD, Integer.toString(incoming.sequence));
                                inputValues.put(VALUE_FIELD, incoming.message);
                                getContentResolver().insert(CONTENT_URI, inputValues);
                                publishProgress(incoming.message);
                                counter = incoming.sequence;
                                int temp = it.next().sequence;
                                if (temp != counter+1) break;
                            }
                        }
                    }
                } 
                catch (IOException e) {
                    Log.e(TAG, "can't accept.");
                }
            }
            return null;
        }
        protected void onProgressUpdate(String...strings) {
            /*
             * The following code displays what is received in doInBackground().
             */
            String strReceived = strings[0].trim();
            TextView remoteTextView = (TextView) findViewById(R.id.textView1);
            remoteTextView.append(strReceived + "\t\n");            
        }
    }

    /***
     * ClientTask is an AsyncTask that should send a string over the network.
     * It is created by ClientTask.executeOnExecutor() call whenever OnKeyListener.onKey() detects
     * an enter key press event.
     * 
     * @author stevko
     *
     */
    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
            try {
                Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                        Integer.parseInt(REMOTE_PORT0));
                MessageData msgToSend = new MessageData();
                /*
                 * Client code that sends out message.
                 */
                msgToSend.message = msgs[0];
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                out.writeObject(msgToSend);
                socket.close();
            } catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");
            } catch (IOException e) {
                Log.e(TAG, "ClientTask socket IOException ", e);
            }
            return null;
        }
    }
}
