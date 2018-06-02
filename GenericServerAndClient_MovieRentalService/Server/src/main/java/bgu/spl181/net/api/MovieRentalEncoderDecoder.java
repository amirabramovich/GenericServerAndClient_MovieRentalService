package bgu.spl181.net.api;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
/**
 * This class represens the MovieRentalEncoderDecoder.The framing we work with is with "/n" delimiter to recognize full massage
 * We decode each byte from the socket and keep the massage until we get "/n" and pop the string we get.
 * We keep int length as position of our buffer points to the first empty place in buffer,is initialized to 0 after each massage.
 * We initialize buffer to 1k and double its size each time it is full if needed.
 * @author chen
 *
 */
public class MovieRentalEncoderDecoder implements MessageEncoderDecoder<String>{

    private byte[] bytes = new byte[1 << 10]; //start with 1k
    private int len = 0;

    @Override
    public String decodeNextByte(byte nextByte) {
        //notice that the top 128 ascii characters have the same representation as their utf-8 counterparts
        //this allow us to do the following comparison
        if (nextByte == '\n') {
            return popString();
        }

        pushByte(nextByte);
        return null; //not a line yet
    }

    @Override
    public byte[] encode(String message) {
        return (message + "\n").getBytes(); //uses utf8 by default
    }
    /**
     * This method push byte to buffer and increments the length.
     * In case buffer is full we double it.
     * @param nextByte
     */
    private void pushByte(byte nextByte) {
        if (len >= bytes.length) {
            bytes = Arrays.copyOf(bytes, len * 2);
        }

        bytes[len++] = nextByte;
    }
    /**
     * This method retrieve a message and initialize len to zero,
     * to indicate buffer is empty now.
     * @return
     */
    private String popString() {
        //notice that we explicitly requesting that the string will be decoded from UTF-8
        //this is not actually required as it is the default encoding in java.
        String result = new String(bytes, 0, len, StandardCharsets.UTF_8);
        len = 0;
        return result;
    }

}
