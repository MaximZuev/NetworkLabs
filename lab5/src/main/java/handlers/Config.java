package handlers;

public class Config {
    /*SOCKS5 version*/
    public static final byte VERSION = 0x05;

    /*type of the address*/
    public static final byte IPV4 = 0x01;
    public static final byte DOMAIN_NAME = 0x03;

    /*authentication methods*/
    public static final byte NO_AUTH = 0x00;
    public static final int AUTH_NOT_FOUND = 0xFF;

    /*command code*/
    public static final byte TCP_IP_CONNECT = 0x01;

    /*RSV*/
    public static final byte RSV = 0x00;

    /*status code*/
    public static byte REQUEST_GRANTED = 0x00;
    public static byte GENERAL_FAILURE = 0x01;
    public static byte NOT_ALLOWED = 0x02;
    public static byte NETWORK_UNREACHABLE = 0x03;
    public static byte HOST_UNREACHABLE = 0x04;
    public static byte CONNECTION_REFUSED = 0x05;
    public static byte TTL_EXPIRED = 0x06;
    public static byte CMD_NOT_SUPPORTED = 0x07;
    public static byte ADDRESS_TYPE_NOT_SUPPORTED = 0x08;

    /*buffer size*/
    public static final int BUF_SIZE = 4096;
}
