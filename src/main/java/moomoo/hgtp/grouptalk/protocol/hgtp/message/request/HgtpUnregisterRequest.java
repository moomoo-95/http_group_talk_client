package moomoo.hgtp.grouptalk.protocol.hgtp.message.request;

import moomoo.hgtp.grouptalk.protocol.hgtp.exception.HgtpException;
import moomoo.hgtp.grouptalk.protocol.hgtp.message.base.HgtpHeader;
import moomoo.hgtp.grouptalk.protocol.hgtp.message.base.HgtpMessage;
import moomoo.hgtp.grouptalk.protocol.hgtp.message.base.HgtpMessageType;

public class HgtpUnregisterRequest extends HgtpMessage {

    private final HgtpHeader hgtpHeader;

    public HgtpUnregisterRequest(byte[] data) throws HgtpException {
        if (data.length >= HgtpHeader.HGTP_HEADER_SIZE) {
            int index = 0;

            byte[] headerByteData = new byte[HgtpHeader.HGTP_HEADER_SIZE];
            System.arraycopy(data, index, headerByteData, 0, headerByteData.length);
            this.hgtpHeader = new HgtpHeader(headerByteData);

        } else {
            this.hgtpHeader = null;
        }
    }

    public HgtpUnregisterRequest(short magicCookie, String userId, int seqNumber, long timeStamp) {
        this.hgtpHeader = new HgtpHeader(magicCookie, HgtpMessageType.UNREGISTER, HgtpMessageType.UNREGISTER, userId, seqNumber, timeStamp, 0);
    }

    @Override
    public byte[] getByteData() {
        byte[] data = new byte[HgtpHeader.HGTP_HEADER_SIZE + this.hgtpHeader.getBodyLength()];
        int index = 0;

        byte[] headerByteData = this.hgtpHeader.getByteData();
        System.arraycopy(headerByteData, 0, data, index, headerByteData.length);
        return data;
    }

    public HgtpHeader getHgtpHeader() {return hgtpHeader;}
}
