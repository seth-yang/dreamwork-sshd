package org.dreamwork.network.sshd;

class Keys {
    private static final String PACKET_NAME = "org.dreamwork.network.sshd";

    public static final String
            KEY_IN                   = PACKET_NAME + "KEY_IN",
            KEY_OUT                  = PACKET_NAME + "KEY_OUT",
            KEY_ERR                  = PACKET_NAME + "KEY_ERR",
            CFG_SSHD_PORT            = "service.sshd.port",
            CFG_SSHD_CA_DIR          = "service.sshd.cert.file",
            CFG_DB_FILE              = "database.file";
}
