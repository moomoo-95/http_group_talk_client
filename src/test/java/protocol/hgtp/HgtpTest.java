package protocol.hgtp;

import org.apache.commons.net.ntp.TimeStamp;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import protocol.hgtp.exception.HgtpException;
import protocol.hgtp.message.request.HgtpRegisterRequest;
import protocol.hgtp.message.base.HgtpHeader;
import protocol.hgtp.message.base.HgtpMessageType;
import protocol.hgtp.message.request.HgtpUnregisterRequest;
import protocol.hgtp.message.response.HgtpCommonResponse;
import protocol.hgtp.message.response.HgtpUnauthorizedResponse;
import util.CnameGenerator;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;

public class HgtpTest {

    private static final Logger log = LoggerFactory.getLogger(HgtpTest.class);
    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("YYYY-MM-DD HH:mm:ss.SSS");

    // hgtpRegisterTest
    // 200 OK 응답                    : CLIENT_TEST_REALM == SERVER_TEST_REALM && AVAILABLE_REGISTER > CURRENT_REGISTER
    // 403 Forbidden 응답             : CLIENT_TEST_REALM != SERVER_TEST_REALM
    // 503 Service Unavailable 응답   : CLIENT_TEST_REALM == SERVER_TEST_REALM && AVAILABLE_REGISTER <= CURRENT_REGISTER
    private static final String CLIENT_TEST_REALM = "HGTP_SERVICE";
    private static final String SERVER_TEST_REALM = "HGTP_SERVICE";
    private static final String TEST_HASH_KEY = "950817";
    private static final int AVAILABLE_REGISTER = 3;
    private static final int CURRENT_REGISTER = 1;

    // hgtpUnregisterTest
    // 200 OK 응답                    : isServerError == false
    // 400 Bad Request 응답           : isServerError == false && unknown messageType
    // 503 Service Unavailable 응답   : isServerError == true
    private static final boolean isServerError = false;

    @Test
    public void hgtpRegisterTest() {
        try {
            String registerUserId = CnameGenerator.generateCname();
            // send first Register
            HgtpRegisterRequest sendFirstHgtpRegisterRequest = new HgtpRegisterRequest(
                    HgtpHeader.MAGIC_COOKIE, HgtpMessageType.REGISTER, registerUserId, 4, TimeStamp.getCurrentTime().getSeconds(),
                    3600L, (short) 5060);
            log.debug("RG1 SEND DATA : {}", sendFirstHgtpRegisterRequest);

            // recv first Register
            byte[] recvFirstRegister = sendFirstHgtpRegisterRequest.getByteData();

            log.debug("recvFirstRegister : {}", recvFirstRegister);
            HgtpRegisterRequest recvFirstHgtpRegisterRequest = new HgtpRegisterRequest(recvFirstRegister);
            log.debug("RG1 RECV DATA  : {}", recvFirstHgtpRegisterRequest);

            // send unauthorized
            HgtpHeader recvReg1Header = recvFirstHgtpRegisterRequest.getHgtpHeader();
            HgtpUnauthorizedResponse sendHgtpUnauthorizedResponse = new HgtpUnauthorizedResponse(
                    recvReg1Header.getMagicCookie(), HgtpMessageType.UNAUTHORIZED,
                    recvReg1Header.getRequestType(), recvReg1Header.getUserId(),
                    recvFirstHgtpRegisterRequest.getHgtpHeader().getSeqNumber() + 1, TimeStamp.getCurrentTime().getSeconds(),
                    CLIENT_TEST_REALM);
            log.debug("URE SEND DATA : {}", sendHgtpUnauthorizedResponse);

            // recv unauthorized
            byte[] recvUnauthorized = sendHgtpUnauthorizedResponse.getByteData();
            HgtpUnauthorizedResponse recvHgtpUnauthorizedResponse = new HgtpUnauthorizedResponse(recvUnauthorized);
            log.debug("URE RECV DATA : {}", recvHgtpUnauthorizedResponse);

            // Encoding realm -> nonce
            MessageDigest messageDigestRealm = MessageDigest.getInstance("MD5");
            messageDigestRealm.update(recvHgtpUnauthorizedResponse.getHgtpUnauthorizedContext().getRealm().getBytes(StandardCharsets.UTF_8));
            messageDigestRealm.update(TEST_HASH_KEY.getBytes(StandardCharsets.UTF_8));
            byte[] digestRealm = messageDigestRealm.digest();
            messageDigestRealm.reset();
            messageDigestRealm.update(digestRealm);
            String nonce = new String(messageDigestRealm.digest());

            // send second Register
            HgtpHeader recvUnauthHeader = recvHgtpUnauthorizedResponse.getHgtpHeader();
            HgtpRegisterRequest sendSecondHgtpRegisterRequest = new HgtpRegisterRequest(
                    HgtpHeader.MAGIC_COOKIE, HgtpMessageType.REGISTER, recvUnauthHeader.getUserId(),
                    recvUnauthHeader.getSeqNumber() + 1, TimeStamp.getCurrentTime().getSeconds(),
                    3600L, (short) 5060);
            sendSecondHgtpRegisterRequest.getHgtpRegisterContext().setNonce(sendSecondHgtpRegisterRequest.getHgtpHeader(), nonce);
            log.debug("RG2 SEND DATA : {}", sendSecondHgtpRegisterRequest);

            // recv second Register
            byte[] recvSecondRegister = sendSecondHgtpRegisterRequest.getByteData();
            HgtpRegisterRequest recvSecondHgtpRegisterRequest = new HgtpRegisterRequest(recvSecondRegister);
            log.debug("RG2 RECV DATA  : {}", recvSecondHgtpRegisterRequest);

            // Decoding nonce -> realm
            MessageDigest messageDigestNonce = MessageDigest.getInstance("MD5");
            messageDigestNonce.update(SERVER_TEST_REALM.getBytes(StandardCharsets.UTF_8));
            messageDigestNonce.update(TEST_HASH_KEY.getBytes(StandardCharsets.UTF_8));
            byte[] digestNonce = messageDigestNonce.digest();
            messageDigestNonce.reset();
            messageDigestNonce.update(digestNonce);

            String curNonce = new String(messageDigestNonce.digest());

            short messageType;
            if (curNonce.equals(recvSecondHgtpRegisterRequest.getHgtpRegisterContext().getNonce())) {
                if (AVAILABLE_REGISTER > CURRENT_REGISTER) {
                    messageType = HgtpMessageType.OK;
                } else {
                    messageType = HgtpMessageType.SERVER_UNAVAILABLE;
                }
            } else {
                messageType = HgtpMessageType.FORBIDDEN;
            }

            // send response
            HgtpHeader recvReg2Header = recvSecondHgtpRegisterRequest.getHgtpHeader();
            HgtpCommonResponse sendHgtpResponse = new HgtpCommonResponse(
                    recvSecondHgtpRegisterRequest.getHgtpHeader().getMagicCookie(), messageType,
                    recvReg2Header.getMessageType(), recvReg2Header.getUserId(),
                    recvReg2Header.getSeqNumber() + 1, TimeStamp.getCurrentTime().getSeconds()
                    );
            log.debug("SEND DATA : {}", sendHgtpResponse);
            // recv response
            byte[] recvResponse = sendHgtpResponse.getByteData();
            HgtpCommonResponse recvHgtpResponse = new HgtpCommonResponse(recvResponse);
            log.debug("RECV DATA  : {}", recvHgtpResponse);

        } catch (HgtpException | NoSuchAlgorithmException e) {
            log.error("HgtpTest.hgtpRegisterSuccessTest ", e);
        }
    }

    @Test
    public void hgtpUnregisterTest(){
        try {
            String unregisterUserId = CnameGenerator.generateCname();
            // send Unregister
            HgtpUnregisterRequest sendHgtpUnregisterRequest = new HgtpUnregisterRequest(
                    HgtpHeader.MAGIC_COOKIE, HgtpMessageType.UNREGISTER, unregisterUserId, 7, TimeStamp.getCurrentTime().getSeconds());
            log.debug("SEND DATA : {}", sendHgtpUnregisterRequest);
            // recv Unregister
            byte[] recvRequestUnregister = sendHgtpUnregisterRequest.getByteData();
            HgtpUnregisterRequest recvHgtpUnregisterRequest = new HgtpUnregisterRequest(recvRequestUnregister);
            log.debug("RECV DATA  : {}", recvHgtpUnregisterRequest);

            short messageType;
            if (isServerError) {
                messageType = HgtpMessageType.SERVER_UNAVAILABLE;
            } else {
                if (recvHgtpUnregisterRequest.getHgtpHeader().getMessageType() != HgtpMessageType.UNREGISTER){
                    messageType = HgtpMessageType.BAD_REQUEST;
                } else {
                    messageType = HgtpMessageType.OK;
                }
            }
            // send response
            HgtpHeader recvUnregHeader = recvHgtpUnregisterRequest.getHgtpHeader();
            HgtpCommonResponse sendHgtpResponse = new HgtpCommonResponse(
                    recvUnregHeader.getMagicCookie(), messageType, recvUnregHeader.getMessageType(), recvUnregHeader.getUserId(),
                    recvUnregHeader.getSeqNumber() + 1, TimeStamp.getCurrentTime().getSeconds());
            log.debug("SEND DATA : {}", sendHgtpResponse);
            // recv response
            byte[] recvResponse = sendHgtpResponse.getByteData();
            HgtpCommonResponse recvHgtpResponse = new HgtpCommonResponse(recvResponse);
            log.debug("RECV DATA  : {}", recvHgtpResponse);

        } catch (HgtpException e) {
            log.error("HgtpTest.hgtpRegisterSuccessTest ", e);
        }

    }
}