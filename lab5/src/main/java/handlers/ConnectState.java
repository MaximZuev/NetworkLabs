package handlers;

public enum ConnectState {
    WAIT_AUTH,
    SEND_AUTH,
    WAIT_REQ,
    SEND_RESP,
    FORWARDING,
    CONNECTION,
    SEND_ERR
}