package com.mdsol.mauth.test.utils;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.Charset;

public class FixturesLoader {

  // fixtures of Cross platform testing for mAuth signatures (match with PrivateKey2 & PublicKey2)
  public final static String APP_UUID_V2 ="5ff4257e-9c16-11e0-b048-0026bbfffe5e";
  public final static String EPOCH_TIME_V2 = "1309891855";

  public final static String REQUEST_METHOD_V2 = "PUT";
  public final static String REQUEST_PATH_V2 = "/v1/pictures";
  public final static String REQUEST_QUERY_PARAMETERS_V2 = "key=-_.~!@#$%^*()+{}|:\"'`<>?&∞=v&キ=v&0=v&a=v&a=b&a=c&a=a&k=&k=v";
  public final static String SIGNATURE_V1_BINARY = "hDKYDRnzPFL2gzsru4zn7c7E7KpEvexeF4F5IR+puDxYXrMmuT2/fETZty5NkGGTZQ1nI6BTYGQGsU/73TkEAm7SvbJZcB2duLSCn8H5D0S1cafory1gnL1TpMPBlY8J/lq/Mht2E17eYw+P87FcpvDShINzy8GxWHqfquBqO8ml4XtirVEtAlI0xlkAsKkVq4nj7rKZUMS85mzogjUAJn3WgpGCNXVU+EK+qElW5QXk3I9uozByZhwBcYt5Cnlg15o99+53wKzMMmdvFmVjA1DeUaSO7LMIuw4ZNLVdDcHJx7ZSpAKZ/EA34u1fYNECFcw5CSKOjdlU7JFr4o8Phw==";
  public final static String SIGNATURE_V2_BINARY = "kNmQchPnfSZOo29GHHDcp+res452+IIiWK/h7HmPdFsTU510X+eWPLaYONmfd2fMAuVLncDAiOPxyOS4WXap69szL37k9537ujnEU15I+j+vINTspCnAIbtZ9ia35c+gQyPgNQo7F1RxNl1P3hfXJ4qNXIrMSc/DlKpieNzmXQFPFs9zZxK5VPvdS0QBsuQFSMN71o2Rupf+NRStxvH55pVej/mjJj4PbeCgAX2N6Vi0dqU2GLgcx+0U5j5FphLUIdqF6/6FKRqPRSCLX5hEyFf2c4stRnNWSpP/y/gGFtdIVxFzKEe42cL3FmYSM4YFTKn3wGgViw0W+CzkbDXJqQ==";
  public final static String SIGNATURE_V2_EMPTY = "rUB9ZnFqqjuNUv5new2vAplAjOh5eTEMYJjMm2G32jkPYqIhUmffEErkWbzOIrHzVfsW9zwgyEO+QRmGSvDDkYSa0ecDQ0/HDUokQoQ0yuZGInztXDJVPDKVy6MDxLcNwnwFo1qHtANL3dYrGAai11AamPU2Hvjf2BcNYLnryIbP3/8ChLRbQTu9rw/v7bmC+owG1BchLIBuBhsr33yiuGD/AfbxqOD9jVb3rZsFvM1/O6aLzT3enNgKxWlpZl+vgnnIHBtdiYl/HGLoX5BMuNsaxpDT7OS4cBIsgkkszQe17vNotnMUUV2WklOZ27x7Uv77LbrY0LgEdzofkv6v3Q==";

  public final static String REQUEST_NORMALIZE_PATH = "/%cf%80";
  public final static String SIGNATURE_NORMALIZE_PATH_V2 = "QtD4t3uO5xYRhT/QDxJm3Dd9zH79hLsUjL9IRvUhana8cS9/JE7dxpsBC17Owh+6cSvTENa1FjKqcPoRgZKACR/pRqmx43+Ha2twdWN2p+Zt+plBZ56ylTv1yKpOCEO6FM/QPEnJtY1DezKmu/EILhkfaLdVh5JTq665SeD1LRv46MVyhAN1BoqQPJI/BLddQRpmmTvmPtKBK5VFhcJs6ZfD2YgFxsKajhGE+posgRjkQ18D1CV54v1kQBK9iiImN1h8G1k/bDKfDjpgTiC7A+n5kJa5PWU+pP9rseGnagEV2bIiA67cObLqRH7kjnKI8PK63cDKJoO7Zfv4zFnrbw==";

  public static String getPublicKey() {
    try {
      return IOUtils.toString(FixturesLoader.class.getResourceAsStream("/keys/fake_publickey.pem"), Charset.defaultCharset());
    } catch (IOException ex) {
      throw new IllegalStateException("Unable to load the public key.", ex);
    }
  }

  public static String getPublicKey2() {
    try {
      return IOUtils.toString(FixturesLoader.class.getResourceAsStream("/keys/fake_publickey2.pem"), Charset.defaultCharset());
    } catch (IOException ex) {
      throw new IllegalStateException("Unable to load the public key2.", ex);
    }
  }

  public static String getPrivateKey() {
    try {
      return IOUtils.toString(FixturesLoader.class.getResourceAsStream("/keys/fake_privatekey.pem"), Charset.defaultCharset());
    } catch (IOException ex) {
      throw new IllegalStateException("Unable to load the private key.", ex);
    }
  }

  public static String getPrivateKey2() {
    try {
      return IOUtils.toString(FixturesLoader.class.getResourceAsStream("/keys/fake_privatekey2.pem"), Charset.defaultCharset());
    } catch (IOException ex) {
      throw new IllegalStateException("Unable to load the private key2.", ex);
    }
  }

  public static byte[] getBinaryFileBody() {
    try {
       return IOUtils.toByteArray(FixturesLoader.class.getResourceAsStream("/blank.jpeg"));
    } catch (IOException ex) {
      throw new IllegalStateException("Unable to load blank.jpeg.", ex);
    }
  }
}
