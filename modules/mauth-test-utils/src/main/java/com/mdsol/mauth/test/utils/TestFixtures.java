package com.mdsol.mauth.test.utils;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;

public class TestFixtures { // fixtures of Cross platform testing for mAuth signatures (match with PrivateKey2 & PublicKey2)
    public static final String TIME_HEADER_V1 = "x-mws-time";
    public static final String AUTH_HEADER_V1 = "x-mws-authentication";
    public static final String TIME_HEADER_V2 = "mcc-time";
    public static final String AUTH_HEADER_V2 = "mcc-authentication";

    public static final String APP_UUID_1 = "2a6790ab-f6c6-45be-86fc-9e9be76ec12a";
    public static final String APP_UUID_V2 = "5ff4257e-9c16-11e0-b048-0026bbfffe5e";
    public static final String EXPECTED_TIME_HEADER_1 = "1509041057";
    public static final String EPOCH_TIME = "1309891855";

    public static final String REQUEST_METHOD_V2 = "PUT";
    public static final String REQUEST_PATH_V2 = "/v1/pictures";
    public static final String REQUEST_QUERY_PARAMETERS_V2 = "key=-_.~!@#$%^*()+{}|:\"'`<>?&∞=v&キ=v&0=v&a=v&a=b&a=c&a=a&k=&k=v";
    public static final String SIGNATURE_V1_BINARY =
            "hDKYDRnzPFL2gzsru4zn7c7E7KpEvexeF4F5IR+puDxYXrMmuT2/fETZty5NkGGTZQ1nI6BTYGQGsU/73TkEAm7SvbJZcB2duLSCn8H5D0S1cafory1gnL1TpMPBlY8J/lq/Mht2E17eYw+P87FcpvDShINzy8GxWHqfquBqO8ml4XtirVEtAlI0xlkAsKkVq4nj7rKZUMS85mzogjUAJn3WgpGCNXVU+EK+qElW5QXk3I9uozByZhwBcYt5Cnlg15o99+53wKzMMmdvFmVjA1DeUaSO7LMIuw4ZNLVdDcHJx7ZSpAKZ/EA34u1fYNECFcw5CSKOjdlU7JFr4o8Phw==";
    public static final String SIGNATURE_V2_BINARY =
            "kNmQchPnfSZOo29GHHDcp+res452+IIiWK/h7HmPdFsTU510X+eWPLaYONmfd2fMAuVLncDAiOPxyOS4WXap69szL37k9537ujnEU15I+j+vINTspCnAIbtZ9ia35c+gQyPgNQo7F1RxNl1P3hfXJ4qNXIrMSc/DlKpieNzmXQFPFs9zZxK5VPvdS0QBsuQFSMN71o2Rupf+NRStxvH55pVej/mjJj4PbeCgAX2N6Vi0dqU2GLgcx+0U5j5FphLUIdqF6/6FKRqPRSCLX5hEyFf2c4stRnNWSpP/y/gGFtdIVxFzKEe42cL3FmYSM4YFTKn3wGgViw0W+CzkbDXJqQ==";
    public static final String SIGNATURE_V2_EMPTY =
            "rUB9ZnFqqjuNUv5new2vAplAjOh5eTEMYJjMm2G32jkPYqIhUmffEErkWbzOIrHzVfsW9zwgyEO+QRmGSvDDkYSa0ecDQ0/HDUokQoQ0yuZGInztXDJVPDKVy6MDxLcNwnwFo1qHtANL3dYrGAai11AamPU2Hvjf2BcNYLnryIbP3/8ChLRbQTu9rw/v7bmC+owG1BchLIBuBhsr33yiuGD/AfbxqOD9jVb3rZsFvM1/O6aLzT3enNgKxWlpZl+vgnnIHBtdiYl/HGLoX5BMuNsaxpDT7OS4cBIsgkkszQe17vNotnMUUV2WklOZ27x7Uv77LbrY0LgEdzofkv6v3Q==";
    public static final String REQUEST_NORMALIZE_PATH = "/%cf%80";
    public static final String SIGNATURE_NORMALIZE_PATH_V1 =
            "MKQr1ogwwzGqNOpv0mSSaA1iz1nbeve1ttmqCPtweEB0Wj+R93+i7eo67YO3CjXZ2nvQeMRzGc5TUyrSQtB3okG/QkYIriFgwhLAmwOXjiHaS2ZJVY8ngcaCGheC5hZchhftVXM+1j2akAfYWer5bAFddINI7UDOfavX83pDp81I3yGpPn2X1gncnn2UVzGiv0RvYT3vngb7KY1qavjC812YGv2/0w+7LSNmR3ZJAqqQ3mI8bEK45kSf1urKcuHr+/O8o7B3btE5F8j7qerVx3jbEEG9yK+syC0vKmf9bpiA9zkUiG50ZNewazotbsSSyb+0hWCDL+lr9Xl9FH5niw==";
    public static final String SIGNATURE_NORMALIZE_PATH_V2 =
            "QtD4t3uO5xYRhT/QDxJm3Dd9zH79hLsUjL9IRvUhana8cS9/JE7dxpsBC17Owh+6cSvTENa1FjKqcPoRgZKACR/pRqmx43+Ha2twdWN2p+Zt+plBZ56ylTv1yKpOCEO6FM/QPEnJtY1DezKmu/EILhkfaLdVh5JTq665SeD1LRv46MVyhAN1BoqQPJI/BLddQRpmmTvmPtKBK5VFhcJs6ZfD2YgFxsKajhGE+posgRjkQ18D1CV54v1kQBK9iiImN1h8G1k/bDKfDjpgTiC7A+n5kJa5PWU+pP9rseGnagEV2bIiA67cObLqRH7kjnKI8PK63cDKJoO7Zfv4zFnrbw==";

    public static final URI URI_EMPTY_PATH = URI.create("http://host.com/");
    public static final URI URI_EMPTY_PATH_WITH_PARAM = URI.create("http://host.com/?key2=data2&key1=data1");
    public static final String SIMPLE_REQUEST_BODY = "Request Body";

    public static final String EXPECTED_AUTH_NO_BODY_V1 =
            "MWS " + APP_UUID_1 + ":ih3xq6OvQ2/D5ktPDaZ4F6tanzdn2XGzZ+KOaFXC+YKVjNcSCfUiKB6T/552K3AmKm/yZF4rdEOps" +
                    "MZ0QkuFqEZdwQ8R3iWUwdrNPsmNXSVvF50pRAlcI77UP5gUKV01xjZxfZ/M/vhzVn513bAgJ6CM8X4dtG20ki5xLsO35e2" +
                    "eZs5i9IwA/hEaKSm/PH2pEHwxz5c9MMGtHiFgzgXGacziVn0fr2c6X5jb3cDjHnfNVX8o57kFjL5E0YOoeEKDwHy" +
                    "flGhbfFNZys29jo83JCK2MQj9s+fZq5NsgmwuACRE6BnqKSPqwDWN4OK3N/iPcTwCsMKz/c5/3CEbMTlH8A==";

    public static final String EXPECTED_AUTH_NO_BODY_V2 =
            "MWSV2 " + APP_UUID_1 + ":h0MJYf5/zlX9VqqchANLr7XUln0RydMV4msZSXzLq2sbr3X+TGeJ60K9ZSlSuRrzyHbzzwuZABA" +
                    "3P2j3l9t+gyBC1c/JSa8mldMrIXXYzp0lYLxLkghH09hm3k0pEW2la94K/Num3xgNymn6D/B9dJ1onRIgl+T+e/m4k6" +
                    "T3apKHcV/6cJ9asm+jDjzB8OuCVWVsLZQKQbtiydUYNisYerKVxWPLs9SHNZ6GmAqq4ZCCpyEQZuMNF6cMmXgQ0Pxe9" +
                    "X/yNA1Xc3Fakuga47lUQ6Bn7xvhkH6P+ZP0k4U7kidziXpxpkDts8fEXTpkvFX0PR7vaxjbMZzWsU413jyNsw==;";

    public static final String EXPECTED_AUTH_SIMPLE_BODY_V1 = "MWS " +
            APP_UUID_1 + ":OoxiQ/Z6EjTUAoAGNKD5FS6ka+9IcWW5rtuzbXRDLRGj4pzSdeI0FPIlT0E/ZR96xR0a5EJlJ3E8usr" +
            "5qas/uzNEDajAqpjqOaO4m3j+4juXt0QrdBvj3sgStD6ozOJrfhyeSWvFp3d9SBx8tPkPrqv6z5ewQliaSOaI20yir4+RStwj6P7j/5/ZlDRMBEFBiFuA" +
            "yAWMAbKnefRwK+0yUqO9pEBQx43YqBzs+Xb9sTM0hKd5IohAW8O8xj1coBYP/NGRvhM5Z+VMXnbRXwkqUlEXIDvZ3fKjPNGEQxo+m9oFH1dLI8oGI9xoC9P3liwUqY5h+g+hSQ4KLIfDm0qvLQ==";

    public static final String EXPECTED_AUTH_SIMPLE_BODY_V2 =
            "MWSV2 " + APP_UUID_1 + ":qqdIEdENI5oxHpiSNCpKcG8s7JElNXf14IfMJ04L82t" +
                    "UvFa27o32Hg1Qz46qoFrFY9dvNxbz8W338/v0/Ce+865bZtxpK/RjhDtwd15ndUoARgILb8Q0" +
                    "y/j/JsCwWPSNBQGmWyUqsS0MnGP7TV80DcykyRjGml/jo853nzY/RrY786PmrwIvsLeLQ5lKNRPF" +
                    "LHO0fFAdLWclbZO2BXdI+qdtlVXUSRf5wLNRohcitscahrLalRW3uTtsa9i+IBwra9sVv8rZXC1HCjJ" +
                    "7T3FvUiw89nu5KQWWAftSww5vJIUggBmiIIzL/vCQ/d02LfZE/qrg3H/QyGzjxOtoNiVreA==;";

    public static final String EXPECTED_AUTH_BODY_AND_PARAM_V1 =
            "MWS " + APP_UUID_1 + ":OoxiQ/Z6EjTUAoAGNKD5FS6ka+9IcWW5rtuzbXRDLRGj4pzSdeI0FPIlT0E/ZR96xR0a5EJlJ3E" +
                    "8usr5qas/uzNEDajAqpjqOaO4m3j+4juXt0QrdBvj3sgStD6ozOJrfhyeSWvFp3d9SBx8tPkPrqv6z5ewQliaSOaI" +
                    "20yir4+RStwj6P7j/5/ZlDRMBEFBiFuAyAWMAbKnefRwK+0yUqO9pEBQx43YqBzs+Xb9sTM0hKd5IohAW8O8xj1coB" +
                    "YP/NGRvhM5Z+VMXnbRXwkqUlEXIDvZ3fKjPNGEQxo+m9oFH1dLI8oGI9xoC9P3liwUqY5h+g+hSQ4KLIfDm0qvLQ==";

    public static final String EXPECTED_AUTH_BODY_AND_PARAM_V2 =
            "MWSV2 " + APP_UUID_1 + ":n5+io+SgpPMgatLarleDkX18r1ZVBtp7YWgu3yeP0k/P8otp4ThEtBJ6Du3b2Pet+7xlkfK90" +
                    "RXrcwiKA0SS8vpPX8nCtLa92hE3G1e0A41Cn00MuasVwV7JlkQeffJH8qQjvapwRsQ9dbFTPOktS4u0fm/7L9hI6k" +
                    "m99lqCP72i0tP7vGCst4Gc1OewGMR+60FUNR7eN66z8wbeXxX5gzMNGpppP/3P2YROGkONlsxbd1UxrEN62r6yQBF" +
                    "i9hTFF0FCqDM63UiLxTt3ooTpj4iUx/3htvPJ2AlSW5TaoviQUjQFYdb+CB6xDi0LFp93V5289lEXdPOVCULUGesqDA==;";

    public static final String PUBLIC_KEY_1;
    static {
        String tmp = "";
        try {
            tmp = IOUtils.toString(TestFixtures.class.getResourceAsStream("/keys/fake_publickey.pem"), Charset.defaultCharset());
        } catch (IOException e) {
            e.printStackTrace();
        }
        PUBLIC_KEY_1 = tmp;
    }

    public static final String PRIVATE_KEY_1;
    static {
        String tmp = "";
        try {
            tmp = IOUtils.toString(TestFixtures.class.getResourceAsStream("/keys/fake_privatekey.pem"), Charset.defaultCharset());
        } catch (IOException e) {
            e.printStackTrace();
        }
        PRIVATE_KEY_1 = tmp;
    }

    public static final String PUBLIC_KEY_2;
    static {
        String tmp = "";
        try {
            tmp = IOUtils.toString(TestFixtures.class.getResourceAsStream("/keys/fake_publickey2.pem"), Charset.defaultCharset());
        } catch (IOException e) {
            e.printStackTrace();
        }
        PUBLIC_KEY_2 = tmp;
    }

    public static final String PRIVATE_KEY_2;
    static {
        String tmp = "";
        try {
            tmp = IOUtils.toString(TestFixtures.class.getResourceAsStream("/keys/fake_privatekey2.pem"), Charset.defaultCharset());
        } catch (IOException e) {
            e.printStackTrace();
        }
        PRIVATE_KEY_2 = tmp;
    }

    public static final byte[] BINARY_FILE_BODY;
    static {
        byte[] tmp = {};
        try {
            tmp = IOUtils.toByteArray(TestFixtures.class.getResourceAsStream("/blank.jpeg"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        BINARY_FILE_BODY = tmp;
    }

}
