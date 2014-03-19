package edu.buffalo.cse.cse486586.groupmessenger;

import java.io.Serializable;

/*
 * Message datastructure. Contains a field for storing the sequence number and one field for
 * storing the message. Sequence number is initialized to -100. It has to implement Serializable
 * so that GroupMessengerActivity can stream Objects. It has to implement Comparable to be used
 * as an ArrayList.
 * 
 * I really only needed a dumb datastructure to store the message with its sequence. I didn't bother
 * making those fields private and providing getter and setter methods because that will be equivalent
 * to having public fields.
 */

class MessageData implements Serializable, Comparable<MessageData>{
    private static final long serialVersionUID = 1L;
    int sequence;
    String message;
    public MessageData() {
        super();
        this.sequence = -100;
    }

    @Override
    public int compareTo(MessageData compareM) {
        return sequence - compareM.sequence;
    }
}