package com.mdsol.mauth;

import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.crypto.encodings.PKCS1Encoding;
import org.bouncycastle.crypto.engines.RSAEngine;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMReader;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import javax.servlet.http.HttpServletResponse;


/**
 * This class implements a set of utilities to be used as a client for several mAuth operations.
 * Primarily it can be used to create mAuth signatures and also to validate mAuth signatures coming
 * from other applications.
 * 
 * @author Ricardo Chavarria
 * @deprecated Use {@link MAuthRequestSigner}
 * 
 */
@Deprecated
public class MAuthClient
{
    private String _appId;
    private String _publicKey;
    private String _privateKey;
    private String _mAuthUrl;
    private String _mAuthRequestUrlPath;
    private String _securityTokensUrl;
    
    // Cache for public keys
    private static Map<String, PublicKey> _publicKeys = new HashMap<String, PublicKey>();
   
    // Cache for private keys (TODO: may only have one key, need to see if better remove it)
    private final Map<String, PrivateKey> _privateKeys = new HashMap<String, PrivateKey>();

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
    	if (null==mAuthUrl || mAuthUrl.equals("")) {
        throw new Exception("Cannot initialize MAuth client: mAuthUrl cannot be null");
      }
    	if (null==mAuthRequestUrlPath || mAuthRequestUrlPath.equals("")) {
        throw new Exception("Cannot initialize MAuth client: mAuthRequestUrlPath cannot be null");
      }
    	if (null==securityTokensUrl || securityTokensUrl.equals("")) {
        throw new Exception("Cannot initialize MAuth client: securityTokensUrl cannot be null");
      }
    	if (null==appId || appId.equals("")) {
        throw new Exception("Cannot initialize MAuth client: appId cannot be null");
      }
    	if (null==publicKey || publicKey.equals("")) {
        throw new Exception("Cannot initialize MAuth client: publicKey cannot be null");
      }
    	if (null==privateKey || privateKey.equals("")) {
        throw new Exception("Cannot initialize MAuth client: privateKey cannot be null");
      }

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
     * Get signature portion of http header based on mAuth specification
     * 
     * @param header the mAuth header x-mws-authentication
     * @return signature portion in header
     */
    //=======================================================================================
    public String getSignatureInHeader(String header)
    {
        int pos = header.indexOf(":");
        if (pos < 0) {
          return "";
        }
        
        return header.substring(pos+1);
    }
    
    //=======================================================================================
    /**
     * Get appId portion of http header based on mAuth specification
     * 
     * @param header the mAuth header x-mws-authentication
     * @return appId portion in header
     */
    //=======================================================================================
    public String getAppIdInHeader(String header)
    {
        int pos = header.indexOf(":");
        if (pos < 0) {
          return "";
        }
        
        return header.substring(4, pos);
    }
    
    public HttpServletResponse getSignedResponse(HttpServletResponse response) throws Exception
    {
    	String statusCodeString = "";
    	String body = "";
        String epochTime = String.valueOf(getEpochTime());
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
     * Validates an incoming http request which contains mAuth headers.
     * The validation process consists on recreating the mAuth hashed signature
     * and comparing it with the decrypted hash signature from the mAuth header.
     * This approach is faster because it computates the hashes locally and the
     * mAuth service is not contacted to validate, but just to get the requester's
     * public key (if not in cache already). There is an additional way to
     * perform an mAuth validation but is is not implemented in this client
     * 
     * @param signatureInHeader the encrypted and hashed mAuth signature in the header x-mws-authentication
     * @param epochTime the epoch time from the mAuth header x-mws-time
     * @param verb the http verb of the http request
     * @param resourceUrl the resource url of the http request
     * @param body the body of the http request (if available or empty string if none)
     * @param appId the appId of the application under which the validation will be performed
     * @return true or false indicating if the request if valid or not with respect to mAuth
     * @throws Exception
     */
    //=======================================================================================
    public Boolean validateRequest(String signatureInHeader, String epochTime, String verb, String resourceUrl, String body, String appId) throws Exception
    {
    	// Perform routine validations of parameters
    	if (null==signatureInHeader || signatureInHeader.equals("")) {
        throw new Exception("validateRequest error: parameter signatureInHeader cannot be null");
      }
    	if (null==epochTime || epochTime.equals("")) {
        throw new Exception("validateRequest error: parameter epochTime cannot be null");
      }
    	if (null==verb || verb.equals("")) {
        throw new Exception("validateRequest error: parameter verb cannot be null");
      }
    	if (null==resourceUrl || resourceUrl.equals("")) {
        throw new Exception("validateRequest error: parameter resourceUrl cannot be null");
      }
    	if ( (null==body || body.equals("")) && (! "GET".equalsIgnoreCase(verb))) {
        throw new Exception("validateRequest error: parameter body cannot be null: " + verb);
      }
    	if (null==appId || appId.equals("")) {
        throw new Exception("validateRequest error: parameter appId cannot be null");
      }
    	
    	// Check epoc time is not older than 5 minutes
    	long currentEpoc = getEpochTime();
    	long lEpocTime = Long.valueOf(epochTime);
    	if ((currentEpoc - lEpocTime) > 300) {
        throw new Exception("validateRequest error: epoc time is older than 5 minutes");
      }
    	
    	
    	// We need the public key of the appId from which the request comes from (this appId is part of the mAuth header x-mws-authentication)
        PublicKey key = getPublicKey(appId);
        
        // Decode the signature from its base 64 form
        byte[] encryptedSignature = Base64.decodeBase64(signatureInHeader);

        // Decrypt the signature with public key from requesting application
        PKCS1Encoding decryptEngine = new PKCS1Encoding(new RSAEngine());
        decryptEngine.init(false, PublicKeyFactory.createKey(key.getEncoded()));
        byte[] decryptedHexMsg_bytes = decryptEngine.processBlock(encryptedSignature, 0, encryptedSignature.length);
        
        // Recreate the plaintext signature, based on the incoming request parameters, and hash it
        String stringToSign = getStringToSign(verb, resourceUrl, body, appId, epochTime);
        byte[] stringToSign_bytes = stringToSign.getBytes("US-ASCII");
        byte[] messageDigest_bytes = getHex(getMessageDigest(stringToSign_bytes).digest()).getBytes();
        
        // Compare the decrypted signature and the recreated signature hashes
        // If both match, the request was signed by the requesting application and hence the request is valid
        boolean result = Arrays.equals(messageDigest_bytes, decryptedHexMsg_bytes);
 
        return result;
    }
    
    //=======================================================================================
    /**
     * Gets a public key, of a requesting appId, from the mAuth service, and caches it
     * 
     * @param appId appId of an application registered with mAuth
     * @return public key object of the appId
     * @throws Exception
     */
    //=======================================================================================
    private PublicKey getPublicKey(String appId) throws Exception
    {
    	// If key is in cache just return it
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
            throw new Exception("Unable to parse jsonResponse from mAuth service while requesting a public key for: " + appId, ex);
        }
        
        // Get a PublicKey Object from the text version of the public key
        PEMReader reader = null;
        PublicKey key = null;
        try
        {
            reader = new PEMReader(new StringReader(publicKeyString));
            key = (PublicKey) reader.readObject();
         
            // Add public key to cache
            _publicKeys.put(appId, key);
        }
        catch (Exception ex)
        {
        	throw new Exception("Unable to create public key for: " + appId, ex);
        }
        finally
        {
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
     * Generate a map with the mAuth http headers after signing an http request's
     * parameters
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
    	// Get epoch time for now
        String epochTime = String.valueOf(getEpochTime());
        
        // Use the http request's parameters to generate a base64encoded/encrypted/signed request
        String encryptedHexMsg_base64 = signMessageString(verb, resourceUrl, body, appId, epochTime);
        
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("x-mws-authentication", "MWS " + appId + ":" + encryptedHexMsg_base64);
        headers.put("x-mws-time", epochTime);
        
        return headers;
        
    }
    
    //=======================================================================================
    /**
     * Get a SHA-512 message digest object from a byte array
     * 
     * @param rawMessage the raw byte array
     * @return message digest object
     * @throws NoSuchAlgorithmException
     */
    //=======================================================================================
    private MessageDigest getMessageDigest(byte[] rawMessage) throws NoSuchAlgorithmException
    {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-512");
        messageDigest.update(rawMessage);
        
        return messageDigest;
    }
    
    //=======================================================================================
    /**
     * Construct a string to be signed, according to mAuth specifications
     * 
     * @param verb
     * @param resourceUrl
     * @param body
     * @param appId
     * @param epochTime
     * @return string ready to be signed by mAuth
     */
    //=======================================================================================
    private String getStringToSign(String verb, String resourceUrl, String body, String appId, String epochTime)
    {
        String stringToSign =
            verb + "\n"
          + resourceUrl + "\n"
          + body + "\n" //TODO: confirm if body should be hashed with SHA-512
          + appId + "\n"
          + epochTime;
        
        return stringToSign;
    }
    
    //=======================================================================================
    /**
     * Sign an mAuth request based on its parameters
     * 
     * @param verb
     * @param resourceUrl
     * @param body
     * @param appId
     * @param epochTime
     * @return a base64encoded(encrypted(signed prepared)) string
     * @throws Exception
     */
    //=======================================================================================
    public String signMessageString(String verb, String resourceUrl, String body, String appId, String epochTime) throws Exception
    {
    	// Get the string to sign based on parameters
        String stringToSign = getStringToSign(verb, resourceUrl, body, appId, epochTime);
        
        return signMessageString(stringToSign);
    }
    
    //=======================================================================================
    /**
     * Sign an mAuth request based on a string prepared from its parameters
     * 
     * @param stringToSign
     * @return a base64encoded(encrypted(signed prepared)) string
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
            throw new Exception("Unable to get private key:");
        }

        PKCS1Encoding encryptEngine = new PKCS1Encoding(new RSAEngine());
        encryptEngine.init(true, PrivateKeyFactory.createKey(key.getEncoded()));
        byte[] encryptedHexMsg_bytes = encryptEngine.processBlock(md_hex_bytes, 0, md_hex_bytes.length);

        String encryptedHexMsg_base64 = new String(Base64.encodeBase64(encryptedHexMsg_bytes), "UTF-8");
        
        return encryptedHexMsg_base64;
    }
    
    //=======================================================================================
    /**
     * Gets a private key object from the text version of the private key, and caches it.
     * Typically the private key will belong to a signer appId which in
     * most cases will be the appId of the application calling this client class
     * 
     * @param appId the appId for which we want the privat key object
     * @return a private key object of the appId
     * @throws Exception
     */
    //=======================================================================================
    private PrivateKey getPrivateKey(String appId) throws Exception
    {
    	// If private key in cache just return it
        if (_privateKeys.containsKey(appId))
        {
            return _privateKeys.get(appId);
        }
        
        //Get a private key object from the text version in the private member _privateKey
        PEMReader reader = null;
        PrivateKey key = null;
        try
        {
            reader = new PEMReader(new StringReader(_privateKey));
            KeyPair r = (KeyPair) reader.readObject();
            key = r.getPrivate();

        }
        catch (Exception ex)
        {
        	throw new Exception("Unable to create private key for: " + appId, ex);
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

    //=======================================================================================
    /**
     * This method makes call to mAuth invokeApi and process returned result.
     *
     * @param url -Resource URL.
     * @param params_header -collection(Hashtable<Key, value> ) of header key-value pair.
     * @param content -A String object representing body of http request.
     * @param method -request method, either GET or POST.
     * @return result form the mAuth call.
     * @throws Exception.
     */
    //=======================================================================================
    public synchronized String callmAuth(String url, Map<String, String> params_header, String content, String method) throws Exception
    {
        StringBuffer res = new StringBuffer("");
        String sURL = url;
        
        HttpURLConnection conn = invokeApi(sURL, params_header, content, method);
        int lastResponseCode = conn.getResponseCode();
        String lastResponseMessage = conn.getResponseMessage();
        System.out.println("lastResponseCode=" + lastResponseCode + " lastResponseMessage=" + lastResponseMessage);
        if (lastResponseCode >= 200 && lastResponseCode < 300)
        {
            InputStream strm = conn.getInputStream();
            if (strm != null)
            {
                res.append(readToEnd(strm));
                //System.out.println(readToEnd(strm));
            }
            strm.close();

        }
        conn.disconnect();
        
        return res.toString();
    }
    
    
    //=======================================================================================
    /**
     * This method makes the actual http request.
     *
     * Parameters:
     *
     * @param sURL -Resource URL.
     * @param params_header -collection(Hashtable<Key, value> ) of header key-value pair.
     * @param content -A String object representing body of http request.
     * @param method -request method, either GET or POST.
     * @return HttpURLConnection object.
     * @throws MalformedURLException, URISyntaxException, IOException.
     */
    //=======================================================================================
    private HttpURLConnection invokeApi(String sURL, Map<String, String> params_header, String content, String method)
            throws MalformedURLException, URISyntaxException, IOException
    {
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
        if (params_header != null)
        {
            for (String key : params_header.keySet())
            {
                String v = params_header.get(key);
                contentlen = contentlen + v.length();
                conn.addRequestProperty(key.toString(), v.toString());
                
                System.out.println("setting header..key=" + key.toString() + " value=" + v.toString());
            }
        }
        //conn.setRequestProperty("Content-Type", "application/json");
       
        conn.setDoInput(true);
        if (content != null && content.length() > 0)
        {
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
     * @return: epoch time in seconds.
     */
     //=================================================================================
    private long getEpochTime()
    {
        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy hh:mm:ss a");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String utcTime = sdf.format(new Date());
        long epochSec = 0;
        try
        {
            Date curdate = sdf.parse(utcTime);
            epochSec = curdate.getTime() / 1000; //getTime returns time in millisec since 1/1/1970
        }
        catch (Exception ignore) { }
        
        return epochSec;
    }
    
    
    //========================================================================================
    /**
     * This method reads InputSteam and returns content as String.
     *
     * @param InputStream object.
     * @return String containing stream contents.
     * @throws IOException.
     */
     //==========================================================================================
    public String readToEnd(InputStream stream) throws IOException
    {
        int c = 0;
        StringBuffer b = new StringBuffer();
        while ((c = stream.read()) > 0)
        {
        	b.append((char) c);
        }

        return b.toString();
    }
    
    //================================================================================================
    /**
     * This method returns hex string equivalent of byte array.
     *
     * @param raw - byte[] object.
     * @return HEX String.
     */
     //=================================================================================================
    public String getHex(byte[] raw)
    {
        if (raw == null)
        {
            return null;
        }
        final String HEXES = "0123456789abcdef";

        final StringBuilder hex = new StringBuilder(2 * raw.length);
        for (final byte b : raw)
        {
            hex.append(HEXES.charAt((b & 0xF0) >> 4)).append(HEXES.charAt((b & 0x0F)));
        }
        return hex.toString();
    }
    
    //================================================================================================
    /**
     * This method returns the number of elements in the public key cache.
     *
     * @return number of elements in public key cache
     */
     //=================================================================================================
    public int getPublicKeyCacheSize()
    {
    	return _publicKeys.size();
    }
}
