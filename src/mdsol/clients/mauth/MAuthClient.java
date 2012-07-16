package mdsol.clients.mauth;

/**
 * This class implements a set of utilities to be used as a client for several mAuth operations.
 * Primarily it can be used to create mAuth signatures and also to validate mAuth signatures coming
 * from other applications.
 * 
 *  Created by Ricardo Chavarria on July 4th, 2012
 * 
 */

import java.io.*;
import java.net.*;
import java.security.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.Date;
import java.text.SimpleDateFormat;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.crypto.encodings.PKCS1Encoding;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.engines.RSAEngine;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;



//========================================================================
//========================================================================
public class MAuthClient
{
    //Below are the only config strings to be updated before executing this code:
    private String _appId;
    private String _publicKey;
    private String _privateKey;
    private String _mAuthUrl;
    private String _mAuthRequestUrlPath;
    private String _securityTokensUrl;
    
    // Cache for public keys
    private Map<String, PublicKey> _publicKeys = new HashMap<String, PublicKey>();
    // Cache for private keys (TODO: may only have one key, need to see if better remove it)
    private Map<String, PrivateKey> _privateKeys = new HashMap<String, PrivateKey>();

    // Default constructor
    public MAuthClient() {}
    //=======================================================================================
    /**
     * 
     * @param mAuthUrl
     * @param mAuthRequestUrlPath
     * @param securityTokensUrl
     * @param appId
     * @param privateKeyFilePath
     * @throws Exception 
     */
    //=======================================================================================
    public MAuthClient(String mAuthUrl, String mAuthRequestUrlPath, String securityTokensUrl, String appId, String publicKey, String privateKey) throws Exception
    {
        init(mAuthUrl, mAuthRequestUrlPath, securityTokensUrl, appId, publicKey, privateKey);
    }
	
    //=======================================================================================
    /**
     * 
     * @param mAuthUrl
     * @param mAuthRequestUrlPath
     * @param securityTokensUrl
     * @param appId
     * @param privateKeyFilePath
     * @return
     * @throws Exception 
     */
    //=======================================================================================
    public boolean init(String mAuthUrl, String mAuthRequestUrlPath, String securityTokensUrl, String appId, String publicKey, String privateKey) throws Exception
    {
    	if (null==mAuthUrl || mAuthUrl.equals(""))
    		throw new Exception("Cannot initialize MAuth client: mAuthUrl cannot be null");
    	if (null==mAuthRequestUrlPath || mAuthRequestUrlPath.equals(""))
    		throw new Exception("Cannot initialize MAuth client: mAuthRequestUrlPath cannot be null");
    	if (null==securityTokensUrl || securityTokensUrl.equals(""))
    		throw new Exception("Cannot initialize MAuth client: securityTokensUrl cannot be null");
    	if (null==appId || appId.equals(""))
    		throw new Exception("Cannot initialize MAuth client: appId cannot be null");
    	if (null==publicKey || publicKey.equals(""))
    		throw new Exception("Cannot initialize MAuth client: publicKey cannot be null");
    	if (null==privateKey || privateKey.equals(""))
    		throw new Exception("Cannot initialize MAuth client: privateKey cannot be null");

    	_appId = appId;
        _publicKey = publicKey;
        _privateKey = privateKey;
        _mAuthUrl = mAuthUrl;
        _mAuthRequestUrlPath = mAuthRequestUrlPath;
        _securityTokensUrl = securityTokensUrl;
        
        //Initialize cryptographic security provider: BouncyCastle
        Security.addProvider(new BouncyCastleProvider());
        return true;
    }
    
    //=======================================================================================
    /**
     * 
     * @param header
     * @return
     */
    //=======================================================================================
    public String getSignatureInHeader(String header)
    {
        int pos = header.indexOf(":");
        if (pos < 0)
            return header;
        
        return header.substring(pos+1);
    }
    
    //=======================================================================================
    /**
     * 
     * @param header
     * @return
     */
    //=======================================================================================
    public String getAppIdInHeader(String header)
    {
        int pos = header.indexOf(":");
        return header.substring(4, pos);
    }
    
    public HttpServletResponse getSignedResponse(HttpServletResponse response) throws Exception
    {
    	String statusCodeString = ""; //String.valueOf(response.getStatus());
    	String body = ""; //response.getBody();
        String epochTime = getEpochTime();
    	Map<String, String> headers = getSignedResponseHeaders(statusCodeString, body, _appId, epochTime);
    	
    	response.addHeader("x-mws-authentication", headers.get("x-mws-authentication"));
    	response.addHeader("x-mws-time", headers.get("x-mws-time"));
    	
    	return response;
    }
    
    public Map<String, String> getSignedResponseHeaders(String statusCodeString, String body, String appId, String epochTime) throws Exception
    {
    	String stringToSign = statusCodeString + "\n" +
    	body + "\n" +
        appId + "\n" +
        epochTime;
    	
    	String xMwsAuthentication = signMessageString(stringToSign);
    	Map<String, String> headers = new HashMap<String, String>();
    	headers.put("x-mws-authentication", xMwsAuthentication);
    	headers.put("x-mws-time", epochTime);
    	
    	return headers;    	
    }
    
    //=======================================================================================
    /**
     * 
     * @param signatureInHeader
     * @param epochTime
     * @param verb
     * @param resourceUrl
     * @param body
     * @param appId
     * @return
     * @throws Exception
     */
    //=======================================================================================
    public Boolean validateRequest(String signatureInHeader, String epochTime, String verb, String resourceUrl, String body, String appId) throws Exception
    {
        PublicKey key = getPublicKey(appId);
        
        byte[] encryptedSignature = Base64.decodeBase64(signatureInHeader);

        // Decrypt the signature
        PKCS1Encoding decryptEngine = new PKCS1Encoding(new RSAEngine());
        decryptEngine.init(false, (CipherParameters) PublicKeyFactory.createKey(key.getEncoded()));
        byte[] decryptedHexMsg_bytes = decryptEngine.processBlock(encryptedSignature, 0, encryptedSignature.length);
        
        // Recreate the plaintext signature and hash it   
        String stringToSign = getStringToSign(verb, resourceUrl, body, appId, epochTime);
        byte[] stringToSign_bytes = stringToSign.getBytes("US-ASCII");
        byte[] messageDigest_bytes = getHex(getMessageDigest(stringToSign_bytes).digest()).getBytes();
        
        // Compare the decrypted signature and the recreated signature hash
        // If both match, the request is valid
        boolean result = Arrays.equals(messageDigest_bytes, decryptedHexMsg_bytes);

        return result;
    }
    
    //=======================================================================================
    /**
     * 
     * @param appId
     * @return
     * @throws Exception
     */
    //=======================================================================================
    private PublicKey getPublicKey(String appId) throws Exception
    {
        if (_publicKeys.containsKey(appId))
        {
            return _publicKeys.get(appId);
        }
        
        // Generate url to call mAuth api
        String completeMAuthResourceUrlPath = _mAuthRequestUrlPath + String.format(_securityTokensUrl, appId); //this appId is the appId of the app we want the public key from
        // Generate mAuth headers (sign request)
        Map<String, String> headers = generateHeaders("GET", completeMAuthResourceUrlPath, "", _appId); // notice the appId here is _appId, which is our appId not the appId of the app we want the public key from
        
        // Call mAuth api
        String res = callmAuth(_mAuthUrl + completeMAuthResourceUrlPath, headers, "", "GET");
        
        // Get key from json response
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonResponse = null;
        String publicKeyString = "";
        try
        {
            jsonResponse = mapper.readTree(res);
            publicKeyString = jsonResponse.findValue("public_key_str").getTextValue();
        }
        catch (Exception ex)
        {
            
        }
        
        // Get a PublicKey Object from the text version of the public key
        PEMReader reader = null;
        PublicKey key = null;
        try {
            reader = new PEMReader(new StringReader(publicKeyString));
            key = (PublicKey) reader.readObject();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            // Add public key to cache
            _publicKeys.put(appId, key);
            
            // Cleanup
            try
            {
                if (reader != null)
                {
                    reader.close();
                }
            }
            catch (Exception ignore) { }
        }
        
        return key;     
    }
    
     //=======================================================================================
    /**
     * 
     * @param verb
     * @param resourceUrl
     * @param body
     * @param appId
     * @return
     * @throws Exception
     */
    //=======================================================================================
    public Map<String, String> generateHeaders(String verb, String resourceUrl, String body, String appId) throws Exception
    {
        String epochTime = getEpochTime();
        
        String encryptedHexMsg_base64 = signMessageString(verb, resourceUrl, body, appId, epochTime);
        
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("x-mws-authentication", "MWS " + appId + ":" + encryptedHexMsg_base64);
        headers.put("x-mws-time", epochTime);
        
        return headers;
        
    }
    
    //=======================================================================================
    /**
     * 
     * @param stringToSign_bytes
     * @return
     * @throws NoSuchAlgorithmException
     */
    //=======================================================================================
    private MessageDigest getMessageDigest(byte[] stringToSign_bytes) throws NoSuchAlgorithmException
    {
        // Get an SHA-512 message digest object and compute the digest for string to sign.
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-512");
        messageDigest.update(stringToSign_bytes);
        
        return messageDigest;       
    }
    
    //=======================================================================================
    /**
     * 
     * @param verb
     * @param resourceUrl
     * @param body
     * @param appId
     * @param epochTime
     * @return
     */
    //=======================================================================================
    private String getStringToSign(String verb, String resourceUrl, String body, String appId, String epochTime)
    {
        String stringToSign =
            verb + "\n"
          + resourceUrl + "\n"
          + body + "\n"
          + appId + "\n"
          + epochTime;
        
        return stringToSign;
    }
    
    //=======================================================================================
    /**
     * 
     * @param verb
     * @param resourceUrl
     * @param body
     * @param appId
     * @param epochTime
     * @return
     * @throws Exception
     */
    //=======================================================================================
    public String signMessageString(String verb, String resourceUrl, String body, String appId, String epochTime) throws Exception
    {
        String stringToSign = getStringToSign(verb, resourceUrl, body, appId, epochTime);
        
        return signMessageString(stringToSign);
    }
    
    //=======================================================================================
    /**
     * 
     * @param stringToSign
     * @return
     * @throws Exception
     */
    //=======================================================================================
    public String signMessageString(String stringToSign) throws Exception
    {
        //get US-ASCII encoded string to sign bytes
        byte[] stringToSign_bytes = stringToSign.getBytes("US-ASCII");
        
        MessageDigest messageDigest = getMessageDigest(stringToSign_bytes);
        
        //Get the hex equivalent of message digest.
        byte[] md_hex_bytes = getHex(messageDigest.digest()).getBytes();
        
        PrivateKey key = getPrivateKey(_appId);
        
        if (null == key)
        {
            throw new Exception("Uanble to get private key:");
        }

        PKCS1Encoding encryptEngine = new PKCS1Encoding(new RSAEngine());
        encryptEngine.init(true, (CipherParameters) PrivateKeyFactory.createKey(key.getEncoded()));
        byte[] encryptedHexMsg_bytes = encryptEngine.processBlock(md_hex_bytes, 0, md_hex_bytes.length);

        String encryptedHexMsg_base64 = new String(Base64.encodeBase64(encryptedHexMsg_bytes), "UTF-8");
        
        return encryptedHexMsg_base64;
    }
    
    //=======================================================================================
    /**
     * 
     * @param privateKeyFilePath
     * @return
     */
    //=======================================================================================
    private PrivateKey getPrivateKey(String appId)
    {
        if (_privateKeys.containsKey(appId))
        {
            return _privateKeys.get(appId);
        }       
        
        //Get private key and encrypt hex message digest with it.
        PEMReader reader = null;
        PrivateKey key = null;
        try
        {
            reader = new PEMReader(new StringReader(_privateKey));
            KeyPair r = (KeyPair) reader.readObject();
            key = r.getPrivate();

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            _privateKeys.put(appId, key);
            try
            {
                if (reader != null)
                {
                    reader.close();
                }
            }
            catch (Exception ignore) { }
        }
        
        return key;
    }

    //==========================================================================
    /**
     * This method makes call to invokeApi and process returned result.
     *
     * Parameters:
     *
     * url           -Resource URL. 
     * params_header -collection(Hashtable<Key, value> ) of header key-value pair.
     * content       -A String object representing body of http request.  
     * method        -request method, either GET or POST.
     *
     * Returns: void.
     */
     //==============================================================================
    public synchronized String callmAuth(String url, Map<String, String> params_header, String content, String method) throws Exception
    {
        StringBuffer res = new StringBuffer("");
        String sURL = /*BaseIMedidataUri +*/ url;
        
        HttpURLConnection conn = invokeApi(sURL, params_header, content, method);
        int lastResponseCode = conn.getResponseCode();
        String lastResponseMessage = conn.getResponseMessage();
        System.out.println("lastResponseCode=" + lastResponseCode + " lastResponseMessage=" + lastResponseMessage);
        if (lastResponseCode >= 200 && lastResponseCode < 300) {
            InputStream strm = conn.getInputStream();
            if (strm != null) {
                res.append(readToEnd(strm));
                //System.out.println(readToEnd(strm));
            }
            strm.close();

        }
        conn.disconnect();
        
        return res.toString();
    }
    
    
    //============================================================================================
    /**
     * This method makes actual http request.
     *
     * Parameters:
     *
     * sURL      -Resource URL. 
     * params_header -collection(Hashtable<Key, value> ) of header key-value pair. 
     * content  -A String object representing body of http request.  
     * method   -request method, either GET or POST.
     *
     * Returns: HttpURLConnection object.
     */
     //============================================================================================
    private HttpURLConnection invokeApi(String sURL, Map<String, String> params_header, String content, String method)
            throws MalformedURLException, URISyntaxException, IOException {
        URL url = new URL(sURL);
        /*
         * List<Proxy> proxies = ProxySelector.getDefault().select( new
         * URI(sURL)); Proxy p = (proxies.size() == 0) ? Proxy.NO_PROXY : proxies.get(0);
         */
        System.out.println("Calling : "+sURL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();//openConnection(p);
        conn.setRequestMethod(method);
        
        //Set http headers
        int contentlen = 0;
        if (params_header != null) {
            for (String key : params_header.keySet()) {
                String v = params_header.get(key);
                contentlen = contentlen + v.length();
                conn.addRequestProperty(key.toString(), v.toString());
                
                System.out.println("setting header..key=" + key.toString() + " value=" + v.toString());
            }
        }
        //conn.setRequestProperty("Content-Type", "application/json");
       
        conn.setDoInput(true);
        if (content != null && content.length() > 0) {
            conn.setDoOutput(true);
            OutputStream outputStream = conn.getOutputStream();
            outputStream.write(content.getBytes());
            outputStream.flush();
        }

        conn.connect();
        return conn;
    }
    
    //================================================================================
    /**
     * This method gets difference in seconds between 1970/1/1 and today(UTC time).
     *
     * Parameters:
     *
     * Returns: String epoch seconds.
     */
     //=================================================================================
    private String getEpochTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy hh:mm:ss a");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String utcTime = sdf.format(new Date());
        long epochSec = 0;
        try {
            Date curdate = sdf.parse(utcTime);
            epochSec = curdate.getTime() / 1000; //getTime returns time in millisec since 1/1/1970
        } catch (Exception ignore) { }
        return String.valueOf(epochSec);
    }
    
    
    //========================================================================================
    /**
     * This method reads InputSteam and returns content as String
     *
     * Parameters:
     * stream - InputStream object.
     * Returns: String containing stream contents.
     */
     //==========================================================================================
    public String readToEnd(InputStream stream) {
        int c = 0;
        StringBuffer b = new StringBuffer();
        try {
            while ((c = stream.read()) > 0) {
                b.append((char) c);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return b.toString();
    }
    
    //================================================================================================
    /**
     * This method returns hex string equivalent of byte array.
     *
     * Parameters:
     * raw -  byte[] object.
     * Returns: HEX String.
     */
     //=================================================================================================
    public String getHex(byte[] raw) 
    {
        if (raw == null) {
            return null;
        }
        final String HEXES = "0123456789abcdef";

        final StringBuilder hex = new StringBuilder(2 * raw.length);
        for (final byte b : raw) {
            hex.append(HEXES.charAt((b & 0xF0) >> 4)).append(HEXES.charAt((b & 0x0F)));
        }
        return hex.toString();
    }
}
