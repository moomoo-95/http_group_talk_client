package protocol.hgtp.message.base.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.module.ByteUtil;

import java.nio.charset.StandardCharsets;

public class HgtpAuthorizedContext extends HgtpContext {

    private final int realmLength;          // 4 bytes
    private final String realm;             // realmLength bytes

    public HgtpAuthorizedContext(byte[] data) {
        super(data);
        int index = super.getBodyLength();

        if (data.length >= index + ByteUtil.NUM_BYTES_IN_INT) {

            byte[] realmLengthByteData = new byte[ByteUtil.NUM_BYTES_IN_INT];
            System.arraycopy(data, index, realmLengthByteData, 0, realmLengthByteData.length);
            realmLength = ByteUtil.bytesToInt(realmLengthByteData, true);
            index += realmLengthByteData.length;

            byte[] realmByteData = new byte[realmLength];
            System.arraycopy(data, index, realmByteData, 0, realmByteData.length);
            realm = new String(realmByteData, StandardCharsets.UTF_8);

        } else {
            this.realmLength = 0;
            this.realm = null;
        }
    }

    public HgtpAuthorizedContext(Short requestType, String userId, String realm) {
        super(requestType, userId);

        this.realmLength = realm.getBytes(StandardCharsets.UTF_8).length;
        this.realm = realm;
    }

    @Override
    public byte[] getByteData() {
        byte[] data = new byte[getTotalBodyLength()];
        int index = 0;

        byte[] commonContextData = super.getByteData();
        Logger log = LoggerFactory.getLogger(HgtpAuthorizedContext.class);
        System.arraycopy(commonContextData, 0, data, index, commonContextData.length);
        index += commonContextData.length;

        byte[] realmLengthByteData = ByteUtil.intToBytes(realmLength, true);
        System.arraycopy(realmLengthByteData, 0, data, index, realmLengthByteData.length);
        index += realmLengthByteData.length;

        byte[] realmByteData = realm.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(realmByteData, 0, data, index, realmByteData.length);

        return data;
    }

    @Override
    public short getRequestType() {
        return super.getRequestType();
    }

    @Override
    public int getUserIdLength() {
        return super.getUserIdLength();
    }

    @Override
    public String getUserId() {
        return super.getUserId();
    }

    public int getTotalBodyLength() {
        return super.getBodyLength() + ByteUtil.NUM_BYTES_IN_INT + realmLength;
    }

    public String getRealm() {
        return realm;
    }
}
